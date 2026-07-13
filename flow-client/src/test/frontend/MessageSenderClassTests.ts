import { expect } from '@open-wc/testing';
import { MessageSender } from '../../main/frontend/internal/communication/MessageSender';
import { ResynchronizationState } from '../../main/frontend/internal/communication/ResynchronizationState';

function makeRegistry(opts: { pushEnabled?: boolean } = {}) {
  const log = {
    xhrSends: [] as any[],
    startRequests: 0,
    loadingStarts: 0
  };
  let activeRequest = false;
  const reconnectionHandlers: Array<(attempt: number) => void> = [];
  const registry: any = {
    log,
    reconnectionHandlers,
    setActiveRequest: (v: boolean) => {
      activeRequest = v;
    },
    getUILifecycle: () => ({ isRunning: () => true }),
    getRequestResponseTracker: () => ({
      hasActiveRequest: () => activeRequest,
      startRequest: () => {
        activeRequest = true;
        log.startRequests++;
      },
      addReconnectionAttemptHandler: (h: (attempt: number) => void) => reconnectionHandlers.push(h)
    }),
    getServerRpcQueue: () => ({
      isEmpty: () => true,
      toJson: () => [],
      clear: () => {},
      isFlushPending: () => false,
      flush: () => {}
    }),
    getLoadingIndicatorStateHandler: () => ({
      startLoading: () => {
        log.loadingStarts++;
      }
    }),
    getMessageHandler: () => ({ getCsrfToken: () => 'init', getLastSeenServerSyncId: () => 42 }),
    getXhrConnection: () => ({ send: (payload: any) => log.xhrSends.push(payload), getUri: () => '/app?v-r=uidl' }),
    getApplicationConfiguration: () => ({ getMaxMessageSuspendTimeout: () => 1000000 }),
    getPushConfiguration: () => ({ isPushEnabled: () => opts.pushEnabled ?? false })
  };
  return registry;
}

describe('MessageSender (class)', () => {
  it('runs the resynchronization state machine', () => {
    const sender = new MessageSender(makeRegistry());
    expect(sender.getResynchronizationState()).to.equal(ResynchronizationState.NOT_ACTIVE);
    expect(sender.requestResynchronize()).to.be.true;
    expect(sender.getResynchronizationState()).to.equal(ResynchronizationState.SEND_TO_SERVER);
    expect(sender.requestResynchronize()).to.be.true; // still needs sending
    sender.clearResynchronizationState();
    expect(sender.getResynchronizationState()).to.equal(ResynchronizationState.NOT_ACTIVE);
  });

  it('sends a payload over XHR, assigning sync and client ids', () => {
    const registry = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] });

    expect(registry.log.xhrSends).to.have.length(1);
    const sent = registry.log.xhrSends[0];
    expect(sent.syncId).to.equal(42);
    expect(sent.clientId).to.equal(0);
    expect(registry.log.startRequests).to.equal(1);
    expect(sender.hasQueuedMessages()).to.be.true;
  });

  it('queues a second message while one is pending', () => {
    const registry = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] }); // sent, clientId 0
    sender.send({ rpc: ['second'] }); // queued, not sent
    expect(registry.log.xhrSends).to.have.length(1);
    expect(sender.hasQueuedMessages()).to.be.true;
  });

  it('dequeues the acknowledged message on a matching client id', () => {
    const registry = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] }); // sent, clientId 0
    expect(sender.hasQueuedMessages()).to.be.true;

    // Server acknowledges client id 1 (it has seen message 0).
    sender.setClientToServerMessageId(1, false);
    expect(sender.hasQueuedMessages()).to.be.false;
  });

  it('reports the communication method and reflects an enabled push connection', () => {
    const registry = makeRegistry();
    const push = {
      isActive: () => true,
      isBidirectional: () => true,
      push: () => {},
      disconnect: (cb: () => void) => cb(),
      getTransportType: () => 'WEBSOCKET'
    };
    const sender = new MessageSender(registry, () => push);
    expect(sender.getCommunicationMethodName()).to.contain('XHR');

    sender.setPushEnabled(true);
    expect(sender.getCommunicationMethodName()).to.equal('Client to server: WEBSOCKET, server to client: WEBSOCKET');
  });

  it('sends an unload beacon with the UNLOAD flag', () => {
    const registry = makeRegistry();
    const beacons: Array<{ url: string; payload: string }> = [];
    const original = navigator.sendBeacon;
    Object.defineProperty(navigator, 'sendBeacon', {
      value: (url: string, payload: string) => {
        beacons.push({ url, payload });
        return true;
      },
      configurable: true
    });
    try {
      new MessageSender(registry).sendUnloadBeacon();
    } finally {
      Object.defineProperty(navigator, 'sendBeacon', { value: original, configurable: true });
    }

    expect(beacons).to.have.length(1);
    expect(beacons[0].url).to.equal('/app?v-r=uidl');
    expect(JSON.parse(beacons[0].payload).UNLOAD).to.be.true;
  });

  it('resends queued messages on a reconnection attempt', () => {
    const registry = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] });
    expect(registry.log.xhrSends).to.have.length(1);

    // Simulate the request finishing, then a reconnection attempt.
    registry.setActiveRequest(false);
    registry.reconnectionHandlers.forEach((h: (attempt: number) => void) => h(1));
    expect(registry.log.xhrSends).to.have.length(2); // queued message resent
  });
});
