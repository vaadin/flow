// Beyond the Java suite: RequestResponseTracker has no Java test class in src/test/java or
// src/test-gwt/java, so every case here is beyond the Java suite.
import { testRegistry } from '../testRegistry';
import { expect } from '@open-wc/testing';
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
  const registry = testRegistry({
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
  return { registry, sends: () => sends };
}

describe('RequestResponseTracker', () => {
  it('tracks the active request and fires request-starting', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const started: string[] = [];
    tracker.addRequestStartingHandler(() => started.push('x'));

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

  it('endRequest clears the flag, fires response-handling-ended, and does not send when idle', () => {
    const { registry, sends } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const ended: string[] = [];
    tracker.addResponseHandlingEndedHandler(() => ended.push('x'));
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
    // A flush is pending, so ending request 1 sends request 2 before the ended
    // event of request 1 fires.
    let tracker: RequestResponseTracker | null = null;
    const { registry } = makeRegistry({ flushPending: true, onSend: () => tracker!.startRequest(2) });
    tracker = new RequestResponseTracker(registry);
    const events: string[] = [];
    tracker.addRequestStartingHandler((event) => events.push(`started ${event.requestId}`));
    tracker.addResponseHandlingStartedHandler((event) => events.push(`response ${event.requestId}`));
    tracker.addResponseHandlingEndedHandler((event) => events.push(`ended ${event.requestId}`));

    tracker.startRequest(1);
    tracker.fireResponseHandlingStarted(false);
    tracker.fireResponseHandlingStarted(true);
    tracker.endRequest();

    expect(events).to.deep.equal(['started 1', 'response -1', 'response 1', 'started 2', 'ended 1']);
  });

  it('fires response-handling-started and reconnection-attempt with the attempt count', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const events: unknown[] = [];
    tracker.addResponseHandlingStartedHandler(() => events.push('started'));
    // The handler receives the event object, which carries the attempt count.
    const remover = tracker.addReconnectionAttemptHandler((event) => events.push(event.getAttempt()));

    tracker.fireResponseHandlingStarted(true);
    tracker.fireReconnectionAttempt(3);
    expect(events).to.deep.equal(['started', 3]);

    // The remover detaches the handler.
    remover.remove();
    tracker.fireReconnectionAttempt(4);
    expect(events).to.deep.equal(['started', 3]);
  });
  // The handler list keeps SimpleEventBus's registration semantics, which a
  // Set would silently change.
  it('notifies a handler once per registration and detaches one at a time', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    let notified = 0;
    const handler = (): void => {
      notified += 1;
    };
    tracker.addResponseHandlingStartedHandler(handler);
    const second = tracker.addResponseHandlingStartedHandler(handler);

    tracker.fireResponseHandlingStarted(true);
    expect(notified).to.equal(2);

    // One removal detaches one registration, leaving the other in place.
    second.remove();
    tracker.fireResponseHandlingStarted(true);
    expect(notified).to.equal(3);
  });

  it('dispatches to the handlers registered when the event fired', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const order: string[] = [];
    let lateRemover: { remove(): void } | null = null;
    tracker.addResponseHandlingStartedHandler(() => {
      order.push('first');
      // Added during dispatch: not notified for this event.
      lateRemover = tracker.addResponseHandlingStartedHandler(() => order.push('late'));
    });
    const removedDuringDispatch = tracker.addResponseHandlingStartedHandler(() => order.push('second'));
    tracker.addResponseHandlingStartedHandler(() => {
      // Removed during dispatch: still notified for this event.
      removedDuringDispatch.remove();
      order.push('third');
    });

    tracker.fireResponseHandlingStarted(true);
    expect(order).to.deep.equal(['first', 'second', 'third']);

    // The handler added during the first dispatch is notified from the second
    // one on, and the one removed during it is gone.
    order.length = 0;
    tracker.fireResponseHandlingStarted(true);
    expect(order).to.deep.equal(['first', 'third', 'late']);
    lateRemover!.remove();
  });
});
