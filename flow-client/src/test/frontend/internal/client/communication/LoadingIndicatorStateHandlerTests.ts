import { expect } from '@open-wc/testing';
import { CONNECTED, getState, LOADING } from '../../../../../main/frontend/internal/client/ConnectionIndicator';
import { JsonConstants } from '../../../../../main/frontend/internal/flow/shared/JsonConstants';
import { LoadingIndicatorStateHandler } from '../../../../../main/frontend/internal/client/communication/LoadingIndicatorStateHandler';

type Win = { Vaadin?: { connectionState?: unknown } };

// Stands in for the connection-state web component the Java suite installs: it
// counts the requests in flight and reports the state the indicator shows.
function makeHandler(hasActiveRequest = false) {
  const tracker = { requestCount: 0 };
  const connectionState = {
    state: CONNECTED,
    loadingStarted: () => {
      tracker.requestCount += 1;
      connectionState.state = LOADING;
    },
    loadingFinished: () => {
      tracker.requestCount = Math.max(0, tracker.requestCount - 1);
      if (tracker.requestCount === 0) {
        connectionState.state = CONNECTED;
      }
    }
  };
  (window as Win).Vaadin = { connectionState };
  const registry = { getRequestResponseTracker: () => ({ hasActiveRequest: () => hasActiveRequest }) };
  return { handler: new LoadingIndicatorStateHandler(registry), tracker };
}

// stopLoading defers its update through the scheduler.
const afterDeferred = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

describe('LoadingIndicatorStateHandler', () => {
  afterEach(() => {
    delete (window as Win).Vaadin;
  });

  it('stays muted without a preceding message', async () => {
    // Ported from test_default_loadingMuted.
    const { handler, tracker } = makeHandler();
    expect(tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);

    handler.startLoading();

    expect(tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);

    handler.stopLoading();
    await afterDeferred();

    expect(tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);
  });

  it('shows loading for a navigation message', async () => {
    // Ported from test_navigationFlow_loadingVisible.
    const { handler, tracker } = makeHandler();
    expect(getState()).to.equal(CONNECTED);

    handler.processMessage(JsonConstants.RPC_TYPE_NAVIGATION, null);
    handler.startLoading();

    expect(tracker.requestCount).to.equal(1);
    expect(getState()).to.equal(LOADING);

    handler.stopLoading();
    await afterDeferred();

    expect(tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);
  });

  it('shows loading for a regular UI event', async () => {
    // Ported from test_regularUiEventFlow_loadingVisible.
    for (const event of ['click', 'change', 'submit']) {
      const { handler } = makeHandler();
      handler.processMessage(JsonConstants.RPC_TYPE_EVENT, event);
      handler.startLoading();

      expect(getState(), event).to.equal(LOADING);

      handler.stopLoading();
      await afterDeferred();

      expect(getState(), event).to.equal(CONNECTED);
    }
  });

  it('stays muted for a high-frequency UI event', async () => {
    // Ported from test_mutedUiEventFlow_loadingMuted.
    const mutedEvents = ['mousemove', 'touchmove', 'drag', 'keydown', 'keyup', 'keypress', 'wheel', 'scroll', 'input'];
    for (const event of mutedEvents) {
      const { handler } = makeHandler();
      handler.processMessage(JsonConstants.RPC_TYPE_EVENT, event);
      handler.startLoading();

      expect(getState(), event).to.equal(CONNECTED);

      handler.stopLoading();
      await afterDeferred();

      expect(getState(), event).to.equal(CONNECTED);
    }
  });

  describe('beyond the Java suite', () => {
    it('does not stop loading while a request is still active', async () => {
      const { handler, tracker } = makeHandler(true);
      handler.processMessage(JsonConstants.RPC_TYPE_MAP_SYNC, null);
      handler.startLoading();
      handler.stopLoading();
      await afterDeferred();
      expect(tracker.requestCount).to.equal(1);
      expect(getState()).to.equal(LOADING);
    });

    it('shows loading for a non-event request type', () => {
      const { handler, tracker } = makeHandler();
      handler.processMessage(JsonConstants.RPC_TYPE_MAP_SYNC, null);
      handler.startLoading();
      expect(tracker.requestCount).to.equal(1);
    });
  });
});
