import { expect } from '@open-wc/testing';
import { TrackingScheduler } from '../../main/frontend/internal/TrackingScheduler';

const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

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
});
