import { expect } from '@open-wc/testing';
import { ApplicationConnection } from '../../main/frontend/internal/ApplicationConnection';

function makeRegistry(opts: { initialUidlHandled?: boolean; activeRequest?: boolean } = {}) {
  const log = {
    resynchronized: 0,
    startedRequests: 0,
    handled: [] as unknown[],
    polled: 0,
    events: [] as Array<{ nodeId: number; eventType: string; data: unknown }>
  };
  const registry = {
    log,
    getMessageSender: () => ({
      resynchronize: () => log.resynchronized++,
      sendUnloadBeacon: () => {}
    }),
    getRequestResponseTracker: () => ({
      startRequest: () => log.startedRequests++,
      hasActiveRequest: () => opts.activeRequest ?? false
    }),
    getMessageHandler: () => ({
      handleMessage: (json: unknown) => log.handled.push(json),
      isInitialUidlHandled: () => opts.initialUidlHandled ?? false
    }),
    getPoller: () => ({ poll: () => log.polled++ }),
    getURIResolver: () => ({ resolveVaadinUri: (uri: string) => `resolved:${uri}` }),
    getServerConnector: () => ({
      sendEventMessage: (nodeId: number, eventType: string, data: unknown) =>
        log.events.push({ nodeId, eventType, data })
    }),
    getApplicationConfiguration: () => ({ getUIId: () => 7 }),
    getStateTree: () => ({ getRootNode: () => ({ getId: () => 1, getDebugJson: () => ({ root: true }) }) })
  };
  return registry;
}

const idleScheduler = { hasWorkQueued: () => false };

describe('ApplicationConnection', () => {
  it('resynchronizes when there is no initial UIDL', () => {
    const registry = makeRegistry();
    new ApplicationConnection(registry as never, idleScheduler).start(null);
    expect(registry.log.resynchronized).to.equal(1);
    expect(registry.log.handled).to.deep.equal([]);
  });

  it('handles the initial UIDL (after starting a request) when provided', () => {
    const registry = makeRegistry();
    new ApplicationConnection(registry as never, idleScheduler).start({ syncId: 0 });
    expect(registry.log.startedRequests).to.equal(1);
    expect(registry.log.handled).to.deep.equal([{ syncId: 0 }]);
    expect(registry.log.resynchronized).to.equal(0);
  });

  it('isActive while the initial UIDL is not yet handled', () => {
    const connection = new ApplicationConnection(makeRegistry({ initialUidlHandled: false }) as never, idleScheduler);
    expect(connection.isActive()).to.be.true;
  });

  it('isActive while a request is active or deferred work is queued', () => {
    expect(
      new ApplicationConnection(
        makeRegistry({ initialUidlHandled: true, activeRequest: true }) as never,
        idleScheduler
      ).isActive()
    ).to.be.true;
    expect(
      new ApplicationConnection(makeRegistry({ initialUidlHandled: true }) as never, {
        hasWorkQueued: () => true
      }).isActive()
    ).to.be.true;
  });

  it('is idle when the initial UIDL is handled with no request or deferred work', () => {
    const connection = new ApplicationConnection(makeRegistry({ initialUidlHandled: true }) as never, idleScheduler);
    expect(connection.isActive()).to.be.false;
  });

  it('delegates poll, resolveUri, sendEventMessage, connectWebComponent, getUIId, debug', () => {
    const registry = makeRegistry();
    const connection = new ApplicationConnection(registry as never, idleScheduler);

    connection.poll();
    expect(registry.log.polled).to.equal(1);
    expect(connection.resolveUri('context://x')).to.equal('resolved:context://x');
    connection.sendEventMessage(2, 'click', { k: 1 });
    connection.connectWebComponent({ tag: 'my-el' });
    expect(registry.log.events).to.deep.equal([
      { nodeId: 2, eventType: 'click', data: { k: 1 } },
      { nodeId: 1, eventType: 'connect-web-component', data: { tag: 'my-el' } }
    ]);
    expect(connection.getUIId()).to.equal(7);
    expect(connection.debug()).to.deep.equal({ root: true });
  });
});
