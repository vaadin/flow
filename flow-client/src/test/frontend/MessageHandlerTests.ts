import { expect } from '@open-wc/testing';
import { resetForTesting } from '../../main/frontend/internal/EagerDependencyTracker';
import {
  calculateBootstrapTime,
  callAfterServerUpdates,
  getFetchStartTime,
  MessageHandler,
  parseJSONResponse,
  removeStylesheetByIdFromDom
} from '../../main/frontend/internal/MessageHandler';

function makeRegistry() {
  const log = {
    constants: [] as unknown[],
    executed: [] as unknown[],
    endRequests: 0,
    stopLoadings: 0,
    states: [] as string[]
  };
  let state = 'INITIALIZING';
  const registry = {
    log,
    getState: () => state,
    getUILifecycle: () => ({
      getState: () => state,
      setState: (s: string) => {
        state = s;
        log.states.push(s);
      }
    }),
    getMessageSender: () => ({
      getResynchronizationState: () => 'NOT_ACTIVE',
      clearResynchronizationState: () => {},
      setClientToServerMessageId: () => {},
      requestResynchronize: () => true,
      resynchronize: () => {}
    }),
    getStateTree: () => ({ prepareForResync: () => {} }),
    getRequestResponseTracker: () => ({
      fireResponseHandlingStarted: () => {},
      endRequest: () => log.endRequests++,
      hasActiveRequest: () => false
    }),
    getLoadingIndicatorStateHandler: () => ({ stopLoading: () => log.stopLoadings++ }),
    getConstantPool: () => ({ importFromJson: (c: unknown) => log.constants.push(c) }),
    getExecuteJavaScriptProcessor: () => ({ execute: (c: unknown) => log.executed.push(c) }),
    getDependencyLoader: () => ({ loadDependencies: () => {}, requireHtmlImportsReady: () => {} }),
    getSystemErrorHandler: () => ({ handleSessionExpiredError: () => {}, handleUnrecoverableError: () => {} }),
    getApplicationConfiguration: () => ({ getMaxMessageSuspendTimeout: () => 10000 }),
    getResourceLoader: () => ({ clearLoadedResourceById: () => {} })
  };
  return registry;
}

describe('MessageHandler', () => {
  it('removeStylesheetByIdFromDom removes link and style elements by data-id', () => {
    const link = document.createElement('link');
    link.setAttribute('data-id', 'dep-x');
    const style = document.createElement('style');
    style.setAttribute('data-id', 'dep-x');
    const keep = document.createElement('style');
    keep.setAttribute('data-id', 'dep-y');
    document.head.append(link, style, keep);

    removeStylesheetByIdFromDom('dep-x');

    expect(document.querySelector('[data-id="dep-x"]')).to.equal(null);
    expect(document.querySelector('[data-id="dep-y"]')).to.not.equal(null);
    keep.remove();
  });

  it('callAfterServerUpdates invokes afterServerUpdate when present', () => {
    let called = false;
    const node = {
      afterServerUpdate: () => {
        called = true;
      }
    } as unknown as Node;
    callAfterServerUpdates(node);
    expect(called).to.be.true;
  });

  it('callAfterServerUpdates is a no-op without the callback', () => {
    expect(() => callAfterServerUpdates(document.createElement('div'))).to.not.throw();
  });

  it('parseJSONResponse parses JSON text', () => {
    expect(parseJSONResponse('{"a":1,"b":"x"}')).to.eql({ a: 1, b: 'x' });
  });

  it('calculateBootstrapTime and getFetchStartTime return numbers', () => {
    expect(calculateBootstrapTime()).to.be.a('number');
    expect(getFetchStartTime()).to.be.a('number');
  });

  describe('class', () => {
    beforeEach(() => resetForTesting());

    it('starts in an undefined sync-id state with default csrf and no push id', () => {
      const handler = new MessageHandler(makeRegistry() as never);
      expect(handler.getLastSeenServerSyncId()).to.equal(-1);
      expect(handler.getCsrfToken()).to.equal('init');
      expect(handler.getPushId()).to.equal(null);
      expect(handler.isInitialUidlHandled()).to.be.false;
    });

    it('starts the UI and applies an in-order message (constants, csrf, sync id, end request)', () => {
      const registry = makeRegistry();
      const handler = new MessageHandler(registry as never);
      handler.handleMessage({
        syncId: 0,
        'Vaadin-Security-Key': 'tok',
        constants: { c: 1 }
      });

      expect(registry.log.states).to.deep.equal(['RUNNING']);
      expect(registry.log.constants).to.deep.equal([{ c: 1 }]);
      expect(handler.getCsrfToken()).to.equal('tok');
      expect(handler.getLastSeenServerSyncId()).to.equal(0);
      expect(handler.isInitialUidlHandled()).to.be.true;
      expect(registry.log.endRequests).to.equal(1);
      expect(registry.log.stopLoadings).to.equal(1);
    });

    it('queues an out-of-order message without applying it', () => {
      const registry = makeRegistry();
      const handler = new MessageHandler(registry as never);
      handler.handleMessage({ syncId: 0, constants: { first: 1 } });
      expect(handler.getLastSeenServerSyncId()).to.equal(0);

      // syncId 5 while expecting 1 -> queued, not applied.
      handler.handleMessage({ syncId: 5, constants: { skipped: 1 } });
      expect(registry.log.constants).to.deep.equal([{ first: 1 }]); // second not imported
      expect(handler.getLastSeenServerSyncId()).to.equal(0);
    });

    it('ignores an already-seen (stale) message but still ends the request', () => {
      const registry = makeRegistry();
      const handler = new MessageHandler(registry as never);
      handler.handleMessage({ syncId: 0 });
      handler.handleMessage({ syncId: 1 });
      const endRequestsBefore = registry.log.endRequests;

      // syncId 0 again: already seen -> ignored, but the request is ended.
      handler.handleMessage({ syncId: 0, constants: { stale: 1 } });
      expect(registry.log.constants).to.deep.equal([]); // never applied any constants
      expect(registry.log.endRequests).to.equal(endRequestsBefore + 1);
    });

    it('runs a one-shot session-expired handler when set', () => {
      const registry = makeRegistry();
      const handler = new MessageHandler(registry as never);
      let expiredHandled = 0;
      handler.setNextResponseSessionExpiredHandler(() => expiredHandled++);
      handler.handleMessage({ syncId: 0, meta: { sessionExpired: true } });
      expect(expiredHandled).to.equal(1);
    });
  });
});
