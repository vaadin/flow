import { expect } from '@open-wc/testing';
import { RequestResponseTracker } from '../../main/frontend/internal/communication/RequestResponseTracker';
import { ResynchronizationState } from '../../main/frontend/internal/communication/ResynchronizationState';

function makeRegistry(
  opts: {
    running?: boolean;
    flushPending?: boolean;
    resync?: ResynchronizationState;
    queued?: boolean;
  } = {}
) {
  let sends = 0;
  const registry = {
    getUILifecycle: () => ({ isRunning: () => opts.running ?? true }),
    getServerRpcQueue: () => ({ isFlushPending: () => opts.flushPending ?? false }),
    getMessageSender: () => ({
      getResynchronizationState: () => opts.resync ?? ResynchronizationState.NOT_ACTIVE,
      hasQueuedMessages: () => opts.queued ?? false,
      sendInvocationsToServer: () => {
        sends++;
      }
    })
  };
  return { registry, sends: () => sends };
}

describe('RequestResponseTracker', () => {
  it('tracks the active request and fires request-starting', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const started: string[] = [];
    tracker.addRequestStartingHandler(() => started.push('x'));

    expect(tracker.hasActiveRequest()).to.be.false;
    tracker.startRequest();
    expect(tracker.hasActiveRequest()).to.be.true;
    expect(started).to.have.length(1);
  });

  it('throws on a double start or an end without an active request', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    expect(() => tracker.endRequest()).to.throw('no request is active');
    tracker.startRequest();
    expect(() => tracker.startRequest()).to.throw('another is active');
  });

  it('endRequest clears the flag, fires response-handling-ended, and does not send when idle', () => {
    const { registry, sends } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const ended: string[] = [];
    tracker.addResponseHandlingEndedHandler(() => ended.push('x'));
    tracker.startRequest();
    tracker.endRequest();
    expect(tracker.hasActiveRequest()).to.be.false;
    expect(ended).to.have.length(1);
    expect(sends()).to.equal(0);
  });

  it('endRequest sends pending invocations when a flush is pending', () => {
    const { registry, sends } = makeRegistry({ flushPending: true });
    const tracker = new RequestResponseTracker(registry);
    tracker.startRequest();
    tracker.endRequest();
    expect(sends()).to.equal(1);
  });

  it('endRequest sends on a pending resync or queued messages', () => {
    const resync = makeRegistry({ resync: ResynchronizationState.SEND_TO_SERVER });
    const t1 = new RequestResponseTracker(resync.registry);
    t1.startRequest();
    t1.endRequest();
    expect(resync.sends()).to.equal(1);

    const queued = makeRegistry({ queued: true });
    const t2 = new RequestResponseTracker(queued.registry);
    t2.startRequest();
    t2.endRequest();
    expect(queued.sends()).to.equal(1);
  });

  it('fires response-handling-started and reconnection-attempt with the attempt count', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    const events: unknown[] = [];
    tracker.addResponseHandlingStartedHandler(() => events.push('started'));
    const remover = tracker.addReconnectionAttemptHandler((attempt) => events.push(attempt));

    tracker.fireResponseHandlingStarted();
    tracker.fireReconnectionAttempt(3);
    expect(events).to.deep.equal(['started', 3]);

    // The remover detaches the handler.
    remover.remove();
    tracker.fireReconnectionAttempt(4);
    expect(events).to.deep.equal(['started', 3]);
  });
});
