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
  } = {}
) {
  let sends = 0;
  let webkitClears = 0;
  const registry = testRegistry({
    XhrConnection: {
      clearWebkitMaybeIgnoringRequests: () => {
        webkitClears++;
      }
    },
    UILifecycle: { isRunning: () => opts.running ?? true },
    ServerRpcQueue: { isFlushPending: () => opts.flushPending ?? false },
    MessageSender: {
      getResynchronizationState: () => opts.resync ?? ResynchronizationState.NOT_ACTIVE,
      hasQueuedMessages: () => opts.queued ?? false,
      sendInvocationsToServer: () => {
        sends++;
      }
    }
  });
  return { registry, sends: () => sends, webkitClears: () => webkitClears };
}

describe('RequestResponseTracker', () => {
  it('tracks the active request', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    expect(tracker.hasActiveRequest()).to.be.false;
    tracker.startRequest();
    expect(tracker.hasActiveRequest()).to.be.true;
  });

  it('throws on a double start or an end without an active request', () => {
    const { registry } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    expect(() => tracker.endRequest()).to.throw('no request is active');
    tracker.startRequest();
    expect(() => tracker.startRequest()).to.throw('another is active');
  });

  it('endRequest clears the flag and the WebKit retry, and does not send when idle', () => {
    const { registry, sends, webkitClears } = makeRegistry();
    const tracker = new RequestResponseTracker(registry);
    tracker.startRequest();
    tracker.endRequest();
    expect(tracker.hasActiveRequest()).to.be.false;
    expect(webkitClears()).to.equal(1);
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
});
