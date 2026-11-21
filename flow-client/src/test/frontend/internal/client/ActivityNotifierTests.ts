import { expect } from '@open-wc/testing';
import {
  ACTIVITY_CHANGED_EVENT,
  notifyActivityChanged
} from '../../../../main/frontend/internal/client/ActivityNotifier';

const nextTask = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('ActivityNotifier', () => {
  let active: boolean;
  let activeWhenNotified: boolean[];
  const listener = () => activeWhenNotified.push(active);

  beforeEach(() => {
    active = true;
    activeWhenNotified = [];
    window.addEventListener(ACTIVITY_CHANGED_EVENT, listener);
  });

  afterEach(() => {
    window.removeEventListener(ACTIVITY_CHANGED_EVENT, listener);
  });

  it('coalesces notifications made before the event is dispatched', async () => {
    active = false;
    notifyActivityChanged();
    notifyActivityChanged();

    await nextTask();
    expect(activeWhenNotified).to.deep.equal([false]);
  });

  it('does not report a momentary idle state within a promise chain', async () => {
    // e.g. Flow finishing its initialization and then navigating to the
    // first route
    await Promise.resolve()
      .then(() => {
        active = false;
        notifyActivityChanged();
      })
      .then(() => {
        active = true;
      });

    await nextTask();
    expect(activeWhenNotified).to.deep.equal([true]);
  });
});
