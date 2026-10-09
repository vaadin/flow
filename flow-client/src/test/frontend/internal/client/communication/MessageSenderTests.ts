// Beyond the Java suite: MessageSender has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { testRegistry } from '../testRegistry';
import { EventBus } from '../../../../../main/frontend/internal/client/EventBus';
import { MessageSender } from '../../../../../main/frontend/internal/client/communication/MessageSender';
import { ResynchronizationState } from '../../../../../main/frontend/internal/client/communication/MessageSender';
import type { VaadinRequest } from '../../../../../main/frontend/internal/client/communication/VaadinRequest';
import { UILifecycle, UIState } from '../../../../../main/frontend/internal/client/UILifecycle';

function makeRegistry(opts: { pushEnabled?: boolean } = {}) {
  const log = {
    xhrSends: [] as Array<Record<string, unknown>>,
    startRequests: 0,
    loadingStarts: 0
  };
  let activeRequest = false;
  let invocations: unknown[] = [];
  const lifecycle = new UILifecycle();
  lifecycle.setState(UIState.RUNNING);
  const eventBus = new EventBus();
  return {
    log,
    eventBus,
    setActiveRequest: (active: boolean) => {
      activeRequest = active;
    },
    queueInvocation: (invocation: unknown) => invocations.push(invocation),
    terminate: () => lifecycle.setState(UIState.TERMINATED),
    registry: testRegistry({
      UILifecycle: lifecycle,
      RequestResponseTracker: {
        hasActiveRequest: () => activeRequest,
        startRequest: () => {
          activeRequest = true;
          log.startRequests++;
        }
      },
      EventBus: eventBus,
      ServerRpcQueue: {
        isEmpty: () => invocations.length === 0,
        toJson: () => invocations,
        clear: () => {
          invocations = [];
        },
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
    const { registry, log, setActiveRequest } = makeRegistry();
    const sender = new MessageSender(registry);
    sender.send({ rpc: [] });
    expect(log.xhrSends).to.have.length(1);

    // Simulate the request finishing, then a reconnection attempt.
    setActiveRequest(false);
    sender.resendQueuedMessages(1);
    expect(log.xhrSends).to.have.length(2); // queued message resent
  });

  describe('requests', () => {
    // Records the requests announced on the bus, and the events they dispatch
    // with the state each event comes with.
    function recordRequests(eventBus: EventBus) {
      const requests: VaadinRequest[] = [];
      const events: string[] = [];
      eventBus.addEventListener('vaadin-request', ({ detail: request }) => {
        const index = requests.push(request) - 1;
        request.addEventListener('sent', () => events.push(`${index} sent ${request.attempt}`));
        request.addEventListener('error', () => events.push(`${index} error ${request.failure!.reason}`));
        request.addEventListener('end', () => events.push(`${index} end ${request.outcome}`));
      });
      return { requests, events };
    }

    it('announces a request for queued invocations and tracks it through the send', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const { requests, events } = recordRequests(context.eventBus);

      context.queueInvocation({ type: 'event' });
      sender.openRequest();
      sender.openRequest(); // a second invocation joins the same request
      expect(requests).to.have.length(1);
      expect(requests[0].attempt).to.equal(0);

      sender.sendInvocationsToServer();
      expect(context.log.xhrSends).to.have.length(1);
      expect(events).to.deep.equal(['0 sent 1']);
    });

    it('announces a request without invocations right before sending it', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const { requests, events } = recordRequests(context.eventBus);

      sender.resynchronize();
      expect(context.log.xhrSends).to.have.length(1);
      expect(requests).to.have.length(1);
      expect(events).to.deep.equal(['0 sent 1']);
    });

    it('counts an XHR reconnection resend as the next attempt of the same request', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const { requests, events } = recordRequests(context.eventBus);
      sender.send({ rpc: [] });

      context.setActiveRequest(false);
      sender.resendQueuedMessages(1);
      expect(context.log.xhrSends).to.have.length(2);
      expect(requests).to.have.length(1);
      expect(events).to.deep.equal(['0 sent 1', '0 sent 2']);
    });

    it('counts a resend by the timer as the next attempt of the same request', () => {
      const clock = sinon.useFakeTimers();
      try {
        const context = makeRegistry();
        const sender = new MessageSender(context.registry);
        const { requests, events } = recordRequests(context.eventBus);
        sender.send({ rpc: [] });

        context.setActiveRequest(false);
        // The configured suspend timeout plus the margin the sender adds.
        clock.tick(1000000 + 500);
        expect(context.log.xhrSends).to.have.length(2);
        expect(requests).to.have.length(1);
        expect(events).to.deep.equal(['0 sent 1', '0 error timeout', '0 sent 2']);
      } finally {
        clock.restore();
      }
    });

    it('counts a WebSocket resend after a reconnection as the next attempt of the same request', () => {
      const context = makeRegistry();
      const pushed: unknown[] = [];
      const push = {
        isActive: () => true,
        isBidirectional: () => true,
        push: (payload: unknown) => pushed.push(payload),
        disconnect: () => {},
        getTransportType: () => 'websocket'
      };
      const sender = new MessageSender(context.registry, () => push);
      sender.setPushEnabled(true);
      const { requests, events } = recordRequests(context.eventBus);
      sender.send({ rpc: [] });

      // The connection is lost, and once it is back the request it ended has
      // the pending message pushed again.
      sender.failPushAttempt({ reason: 'network' });
      context.setActiveRequest(false);
      sender.sendInvocationsToServer();
      expect(pushed).to.have.length(2);
      expect(pushed[1]).to.equal(pushed[0]);
      expect(requests).to.have.length(1);
      expect(events).to.deep.equal(['0 sent 1', '0 error network', '0 sent 2']);
    });

    it('finds the request a reply over push answers by the client id it carries', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const { requests } = recordRequests(context.eventBus);
      sender.send({ rpc: [] }); // client id 0

      expect(sender.findRequest(1)).to.equal(requests[0]);
      expect(sender.findRequest(undefined)).to.equal(requests[0]);
      expect(sender.findRequest(2)).to.be.undefined;
    });

    it('ends a request once, when the server confirms it', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const { events } = recordRequests(context.eventBus);
      sender.send({ rpc: [] });

      sender.setClientToServerMessageId(1, false);
      sender.setClientToServerMessageId(1, false);
      expect(events).to.deep.equal(['0 sent 1', '0 end acknowledged']);
    });

    it('ends the requests it drops as discarded, and reports nothing for them afterwards', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const { events } = recordRequests(context.eventBus);

      // A forced client id update drops the sent and the queued request.
      sender.send({ rpc: [] });
      sender.send({ rpc: ['queued'] });
      sender.setClientToServerMessageId(5, true);
      expect(events).to.deep.equal(['0 sent 1', '0 end discarded', '1 end discarded']);
      events.length = 0;

      // Giving up on the connection ends what was sent, even though the
      // payload is still sent again later.
      context.setActiveRequest(false);
      sender.send({ rpc: [] });
      sender.discardSentRequests();
      context.setActiveRequest(false);
      sender.resendQueuedMessages(1);
      expect(events).to.deep.equal(['2 sent 1', '2 end discarded']);
      events.length = 0;

      // Stopping the application ends what was sent and what is still queued.
      context.setActiveRequest(false);
      sender.setClientToServerMessageId(7, true);
      sender.send({ rpc: [] });
      context.queueInvocation({ type: 'event' });
      sender.openRequest();
      context.terminate();
      expect(events).to.deep.equal(['3 sent 1', '3 end discarded', '4 end discarded']);
    });

    it('keeps sending when a listener throws', () => {
      const context = makeRegistry();
      const sender = new MessageSender(context.registry);
      const reported: unknown[] = [];
      // The browser reports a listener's error as uncaught; keep it from
      // failing the case.
      const onerror = window.onerror;
      window.onerror = (...args) => {
        reported.push(args[4]);
        return true;
      };
      try {
        const sent: number[] = [];
        context.eventBus.addEventListener('vaadin-request', ({ detail: request }) => {
          request.addEventListener('sent', () => {
            throw new Error('listener failed');
          });
          request.addEventListener('sent', () => sent.push(request.attempt));
          throw new Error('listener failed');
        });
        sender.send({ rpc: [] });
        expect(context.log.xhrSends).to.have.length(1);
        expect(sent).to.deep.equal([1]);
        expect(reported).to.have.length(2);
      } finally {
        window.onerror = onerror;
      }
    });
  });
});
