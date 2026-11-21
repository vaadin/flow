import { expect } from '@open-wc/testing';
import { TrackingScheduler } from '../../../../main/frontend/internal/client/TrackingScheduler';
import { ACTIVITY_CHANGED_EVENT } from '../../../../main/frontend/internal/client/ActivityNotifier';

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

// Beyond the Java suite: com.vaadin.client.TrackingScheduler has no test class of
// its own.
describe('TrackingScheduler', () => {
  it('reports no work queued initially', () => {
    expect(new TrackingScheduler().hasWorkQueued()).to.be.false;
  });

  it('tracks a deferred command as queued until it has run', async () => {
    const scheduler = new TrackingScheduler();
    let ran = false;
    scheduler.scheduleDeferred(() => {
      ran = true;
    });
    expect(scheduler.hasWorkQueued()).to.be.true; // queued, not yet run
    expect(ran).to.be.false;

    await settle();
    expect(ran).to.be.true;
    expect(scheduler.hasWorkQueued()).to.be.false;
  });

  it('tracks multiple deferred commands and clears once all have run', async () => {
    const scheduler = new TrackingScheduler();
    const order: number[] = [];
    scheduler.scheduleDeferred(() => order.push(1));
    scheduler.scheduleDeferred(() => order.push(2));
    expect(scheduler.hasWorkQueued()).to.be.true;

    await settle();
    expect(order).to.deep.equal([1, 2]);
    expect(scheduler.hasWorkQueued()).to.be.false;
  });

  it('notifies once all deferred commands have run', async () => {
    const scheduler = new TrackingScheduler();
    const queuedWhenNotified: boolean[] = [];
    const listener = () => queuedWhenNotified.push(scheduler.hasWorkQueued());
    window.addEventListener(ACTIVITY_CHANGED_EVENT, listener);
    try {
      // The second command schedules a third one before the notification
      // from an early decrement would be dispatched, so such a notification
      // would see work still queued
      scheduler.scheduleDeferred(() => {});
      scheduler.scheduleDeferred(() => scheduler.scheduleDeferred(() => {}));

      for (let i = 0; i < 5; i++) {
        await settle();
      }
      expect(queuedWhenNotified).to.deep.equal([false]);
    } finally {
      window.removeEventListener(ACTIVITY_CHANGED_EVENT, listener);
    }
  });
});
