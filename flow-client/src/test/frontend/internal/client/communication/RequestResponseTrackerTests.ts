// Beyond the Java suite: RequestResponseTracker has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { testRegistry } from '../testRegistry';
import { expect } from '@open-wc/testing';
import { EventBus } from '../../../../../main/frontend/internal/client/EventBus';
import { RequestResponseTracker } from '../../../../../main/frontend/internal/client/communication/RequestResponseTracker';
import { ResynchronizationState } from '../../../../../main/frontend/internal/client/communication/MessageSender';

function makeRegistry(
  opts: {
    running?: boolean;
    flushPending?: boolean;
    resync?: ResynchronizationState;
    queued?: boolean;
    onSend?: () => void;
  } = {}
) {
  let sends = 0;
  const eventBus = new EventBus();
  const registry = testRegistry({
    EventBus: eventBus,
    UILifecycle: { isRunning: () => opts.running ?? true },
    ServerRpcQueue: { isFlushPending: () => opts.flushPending ?? false },
    MessageSender: {
      getResynchronizationState: () => opts.resync ?? ResynchronizationState.NOT_ACTIVE,
      hasQueuedMessages: () => opts.queued ?? false,
      sendInvocationsToServer: () => {
        sends++;
        opts.onSend?.();
      }
    }
  });
  return { registry, eventBus, sends: () => sends };
}

describe('RequestResponseTracker', () => {
  it('tracks the active request and fires request-start', () => {
    const { registry, eventBus } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const started: string[] = [];
    eventBus.addEventListener('vaadin-request-start', () => started.push('x'));

    expect(tracker.hasActiveRequest()).to.be.false;
    tracker.startRequest(0);
    expect(tracker.hasActiveRequest()).to.be.true;
    expect(started).to.have.length(1);
  });

  it('throws on a double start or an end without an active request', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    expect(() => tracker.endRequest()).to.throw('no request is active');
    tracker.startRequest(0);
    expect(() => tracker.startRequest(0)).to.throw('another is active');
  });

  it('endRequest clears the flag, fires request-end, and does not send when idle', () => {
    const { registry, eventBus, sends } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const ended: string[] = [];
    eventBus.addEventListener('vaadin-request-end', () => ended.push('x'));
    tracker.startRequest(0);
    tracker.endRequest();
    expect(tracker.hasActiveRequest()).to.be.false;
    expect(ended).to.have.length(1);
    expect(sends()).to.equal(0);
  });

  it('endRequest sends pending invocations when a flush is pending', () => {
    const { registry, sends } = makeRegistry({ flushPending: true });
    const tracker = new RequestResponseTracker(registry);
    tracker.startRequest(0);
    tracker.endRequest();
    expect(sends()).to.equal(1);
  });

  it('endRequest sends on a pending resync or queued messages', () => {
    const resync = makeRegistry({ resync: ResynchronizationState.SEND_TO_SERVER });
    const t1 = new RequestResponseTracker(resync.registry);
    t1.startRequest(0);
    t1.endRequest();
    expect(resync.sends()).to.equal(1);

    const queued = makeRegistry({ queued: true });
    const t2 = new RequestResponseTracker(queued.registry);
    t2.startRequest(0);
    t2.endRequest();
    expect(queued.sends()).to.equal(1);
  });

  it('reports the id of the request its events are about', () => {
    // A flush is pending, so ending request 1 sends request 2 before the end
    // of request 1 is reported.
    let tracker: RequestResponseTracker | null = null;
    const { registry, eventBus } = makeRegistry({ flushPending: true, onSend: () => tracker!.startRequest(2) });
    tracker = new RequestResponseTracker(registry);
    const events: string[] = [];
    for (const type of ['vaadin-request-start', 'vaadin-response-start', 'vaadin-request-end'] as const) {
      eventBus.addEventListener(type, (event) => events.push(`${type} ${event.detail.requestId}`));
    }

    tracker.startRequest(1);
    // A message the server sent on its own is not about request 1.
    tracker.fireResponseHandlingStarted(false);
    tracker.fireResponseHandlingStarted(true);
    tracker.endRequest();

    expect(events).to.deep.equal([
      'vaadin-request-start 1',
      'vaadin-response-start -1',
      'vaadin-response-start 1',
      'vaadin-request-start 2',
      'vaadin-request-end 1'
    ]);
  });

  it('fires response-start and reconnection-attempt with the attempt count', () => {
    const { registry, eventBus } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const events: unknown[] = [];
    eventBus.addEventListener('vaadin-response-start', () => events.push('started'));
    eventBus.addEventListener('vaadin-reconnection-attempt', (event) => events.push(event.detail.attempt));

    tracker.fireResponseHandlingStarted(true);
    tracker.fireReconnectionAttempt(3);
    expect(events).to.deep.equal(['started', 3]);
  });
});
