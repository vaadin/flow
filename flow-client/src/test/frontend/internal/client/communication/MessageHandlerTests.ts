import { expect } from '@open-wc/testing';
import { MessageHandler, parseJson } from '../../../../../main/frontend/internal/client/communication/MessageHandler';

function makeRegistry() {
  const log = {
    constants: [] as unknown[],
    executed: [] as unknown[],
    endRequests: 0,
    stopLoadings: 0,
    states: [] as string[],
    clearedResources: [] as string[]
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
    getDependencyLoader: () => ({ loadDependencies: () => {} }),
    getSystemErrorHandler: () => ({ handleSessionExpiredError: () => {}, handleUnrecoverableError: () => {} }),
    getApplicationConfiguration: () => ({ getMaxMessageSuspendTimeout: () => 10000 }),
    getResourceLoader: () => ({ clearLoadedResourceById: (id: string) => log.clearedResources.push(id) })
  };
  return registry;
}

describe('MessageHandler', () => {
  it('parseJson parses JSON text, and reports unparseable or missing text as null', () => {
    expect(parseJson('{"a":1,"b":"x"}')).to.eql({ a: 1, b: 'x' });
    expect(parseJson('not json')).to.equal(null);
    expect(parseJson(null)).to.equal(null);
  });

  describe('class', () => {
    // The handler defers message processing through the eager-dependency
    // tracker, but the fake dependency loader starts no load, so the counter
    // stays at zero and the deferred work runs straight away.

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

    describe('beyond the Java suite', () => {
      it('removes the stylesheet elements a message lists, and clears them from the loader', () => {
        const link = document.createElement('link');
        link.setAttribute('data-id', 'dep-x');
        const style = document.createElement('style');
        style.setAttribute('data-id', 'dep-x');
        const keep = document.createElement('style');
        keep.setAttribute('data-id', 'dep-y');
        document.head.append(link, style, keep);

        const registry = makeRegistry();
        new MessageHandler(registry as never).handleMessage({ syncId: 0, stylesheetRemovals: ['dep-x'] });

        expect(document.querySelector('[data-id="dep-x"]')).to.equal(null);
        expect(document.querySelector('[data-id="dep-y"]')).to.not.equal(null);
        expect(registry.log.clearedResources).to.deep.equal(['dep-x']);
        keep.remove();
      });

      it('reports finite processing and bootstrap timings after a message', () => {
        const registry = makeRegistry();
        const handler = new MessageHandler(registry as never);
        handler.handleMessage({ syncId: 0 });

        // [lastProcessingTime, totalProcessingTime, ...serverTimings?, bootstrapTime].
        const profiling = handler.getProfilingData();
        expect(profiling.length).to.be.at.least(3);
        profiling.forEach((value) => expect(value).to.be.finite);
        expect(profiling[0]).to.be.at.least(0);
      });

      it('keeps processing a message whose stylesheetRemovals is null', () => {
        const registry = makeRegistry();
        const handler = new MessageHandler(registry as never);
        // Java early-returns on a null/empty array; iterating it here would throw
        // inside the processing try and skip everything after it.
        handler.handleMessage({ syncId: 0, stylesheetRemovals: null, constants: { c: 1 } });

        expect(registry.log.constants).to.deep.equal([{ c: 1 }]);
        expect(registry.log.endRequests).to.equal(1);
      });
    });
  });
});
