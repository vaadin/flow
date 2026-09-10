// Beyond the Java suite: MessageSender has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import type { EventRemover } from '../../../../../main/frontend/internal/EventRemover';
import { testRegistry } from '../testRegistry';
import {
  ReconnectionAttemptEvent,
  type ReconnectionAttemptEventHandler
} from '../../../../../main/frontend/internal/client/communication/ReconnectionAttemptEvent';
import { MessageSender } from '../../../../../main/frontend/internal/client/communication/MessageSender';
import { ResynchronizationState } from '../../../../../main/frontend/internal/client/communication/MessageSender';

function makeRegistry(opts: { pushEnabled?: boolean } = {}) {
  const log = {
    xhrSends: [] as Array<Record<string, unknown>>,
    startRequests: 0,
    loadingStarts: 0
  };
  let activeRequest = false;
  const reconnectionHandlers: ReconnectionAttemptEventHandler[] = [];
  return {
    log,
    reconnectionHandlers,
    setActiveRequest: (active: boolean) => {
      activeRequest = active;
    },
    registry: testRegistry({
      UILifecycle: { isRunning: () => true },
      RequestResponseTracker: {
        hasActiveRequest: () => activeRequest,
        startRequest: () => {
          activeRequest = true;
          log.startRequests++;
        },
        addReconnectionAttemptHandler: (handler: ReconnectionAttemptEventHandler): EventRemover => {
          reconnectionHandlers.push(handler);
          return { remove: () => reconnectionHandlers.splice(reconnectionHandlers.indexOf(handler), 1) };
        }
      },
      ServerRpcQueue: {
        isEmpty: () => true,
        toJson: () => [],
        clear: () => {},
        isFlushPending: () => false,
        flush: () => {}
      },
      LoadingIndicatorStateHandler: {
        startLoading: () => {
          log.loadingStarts++;
        }
      },
      MessageHandler: { getCsrfToken: () => 'init', getLastSeenServerSyncId: () => 42 },
      XhrConnection: {
        send: (payload: Record<string, unknown>) => {
          log.xhrSends.push(payload);
        },
        getUri: () => '/app?v-r=uidl'
      },
      ApplicationConfiguration: { getMaxMessageSuspendTimeout: () => 1000000 },
      PushConfiguration: { isPushEnabled: () => opts.pushEnabled ?? false }
    })
  };
}

describe('MessageSender (class)', () => {
  it('runs the resynchronization state machine', () => {
    const sender = new MessageSender(makeRegistry().registry);
    expect(sender.getResynchronizationState()).to.equal(ResynchronizationState.NOT_ACTIVE);
    expect(sender.requestResynchronize()).to.be.true;
    expect(sender.getResynchronizationState()).to.equal(ResynchronizationState.SEND_TO_SERVER);
    expect(sender.requestResynchronize()).to.be.true; // still needs sending
    sender.clearResynchronizationState();
    expect(sender.getResynchronizationState()).to.equal(ResynchronizationState.NOT_ACTIVE);
  });

  it('sends a payload over XHR, assigning sync and client ids', () => {
    const { registry, log } = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] });

    expect(log.xhrSends).to.have.length(1);
    const sent = log.xhrSends[0];
    expect(sent.syncId).to.equal(42);
    expect(sent.clientId).to.equal(0);
    expect(log.startRequests).to.equal(1);
    expect(sender.hasQueuedMessages()).to.be.true;
  });

  it('queues a second message while one is pending', () => {
    const { registry, log } = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] }); // sent, clientId 0
    sender.send({ rpc: ['second'] }); // queued, not sent
    expect(log.xhrSends).to.have.length(1);
    expect(sender.hasQueuedMessages()).to.be.true;
  });

  it('dequeues the acknowledged message on a matching client id', () => {
    const { registry } = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] }); // sent, clientId 0
    expect(sender.hasQueuedMessages()).to.be.true;

    // Server acknowledges client id 1 (it has seen message 0).
    sender.setClientToServerMessageId(1, false);
    expect(sender.hasQueuedMessages()).to.be.false;
  });

  it('reports the communication method and reflects an enabled push connection', () => {
    const { registry } = makeRegistry();
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
    const { registry } = makeRegistry();
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
    const { registry, log, reconnectionHandlers, setActiveRequest } = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] });
    expect(log.xhrSends).to.have.length(1);

    // Simulate the request finishing, then a reconnection attempt.
    setActiveRequest(false);
    reconnectionHandlers.forEach((handler) => handler(new ReconnectionAttemptEvent(1)));
    expect(log.xhrSends).to.have.length(2); // queued message resent
  });
});
