import { wiredRegistryBase } from '../flow/stateTreeTestRegistry';
import { testRegistry } from '../testRegistry';
import { expect } from '@open-wc/testing';
import { CONNECTED, getState, LOADING } from '../../../../../main/frontend/internal/client/ConnectionIndicator';
import { JsonConstants } from '../../../../../main/frontend/internal/flow/shared/JsonConstants';
import { LoadingIndicatorStateHandler } from '../../../../../main/frontend/internal/client/communication/LoadingIndicatorStateHandler';
import { MessageHandler } from '../../../../../main/frontend/internal/client/communication/MessageHandler';
import { MessageSender } from '../../../../../main/frontend/internal/client/communication/MessageSender';
import { RequestResponseTracker } from '../../../../../main/frontend/internal/client/communication/RequestResponseTracker';
import { ServerConnector } from '../../../../../main/frontend/internal/client/communication/ServerConnector';
import { ServerRpcQueue } from '../../../../../main/frontend/internal/client/communication/ServerRpcQueue';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { UILifecycle, UIState } from '../../../../../main/frontend/internal/client/UILifecycle';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';

type Win = { Vaadin?: { connectionState?: unknown } };

// Stands in for the connection-state web component the Java suite installs: it
// counts the requests in flight and reports the state the indicator shows.
function installConnectionState() {
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
  return tracker;
}

function makeHandler(hasActiveRequest = false) {
  const tracker = installConnectionState();
  const registry = testRegistry({ RequestResponseTracker: { hasActiveRequest: () => hasActiveRequest } });
  return { handler: new LoadingIndicatorStateHandler(registry), tracker };
}

// stopLoading defers its update through the scheduler.
const afterDeferred = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, 0));

// The two RPC round-trip cases need the services the message actually travels
// through, so they are wired into one registry the way the Java suite assembles
// its Registry: a real StateTree, ServerConnector, ServerRpcQueue,
// RequestResponseTracker, LoadingIndicatorStateHandler and MessageHandler, with
// MessageSender.send() reduced to starting the request (as the Java suite's
// TestMessageSender does) so no request leaves the browser.
function makeWiredRegistry() {
  const tracker = installConnectionState();

  const uiLifecycle = new UILifecycle();
  uiLifecycle.setState(UIState.RUNNING);
  const registry = wiredRegistryBase(uiLifecycle);
  registry.getApplicationConfiguration().setServiceUrl('');
  registry.getApplicationConfiguration().setContextRootUrl('/');

  const tree = new StateTree(registry);
  registry.register('StateTree', tree);
  registry.register('RequestResponseTracker', new RequestResponseTracker(registry));
  registry.register('LoadingIndicatorStateHandler', new LoadingIndicatorStateHandler(registry));
  registry.register('ServerRpcQueue', new ServerRpcQueue(registry));
  registry.register('ServerConnector', new ServerConnector(registry));
  const responseTracker = registry.getRequestResponseTracker();

  class TestMessageSender extends MessageSender {
    override send(): void {
      if (!responseTracker.hasActiveRequest()) {
        responseTracker.startRequest();
      }
    }
  }
  registry.register('MessageSender', new TestMessageSender(registry));

  class TestMessageHandler extends MessageHandler {
    simulateResponse(): void {
      this.handleJSON({ syncId: this.getLastSeenServerSyncId() + 1 });
    }
  }
  const messageHandler = new TestMessageHandler(registry);
  registry.register('MessageHandler', messageHandler);

  // Reached only by paths these two cases do not take.
  registry
    .register('ExecuteJavaScriptProcessor', { execute: () => {} })
    .register('DependencyLoader', { loadDependencies: () => {} })
    .register('SystemErrorHandler', { handleSessionExpiredError: () => {}, handleUnrecoverableError: () => {} })
    .register('ResourceLoader', { clearLoadedResourceById: () => {} });

  const node = new StateNode(2, tree);
  tree.registerNode(node);
  node.getMap(NodeFeatures.ELEMENT_DATA);

  return { tracker, tree, node, messageHandler };
}

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

  it('shows loading for a click event RPC', async () => {
    // Ported from test_clickEventRpc_loadingVisible. The Java suite installs a
    // scheduler that runs deferred work synchronously; here the queue flush and
    // the indicator update are awaited instead.
    const wired = makeWiredRegistry();
    wired.tree.sendEventToServer(wired.node, 'click', {});
    await afterDeferred();

    expect(wired.tracker.requestCount).to.equal(1);
    expect(getState()).to.equal(LOADING);

    wired.messageHandler.simulateResponse();
    await afterDeferred();

    expect(wired.tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);
  });

  it('stays muted for a mousemove event RPC', async () => {
    // Ported from test_mousemoveEventRpc_loadingMuted.
    const wired = makeWiredRegistry();
    wired.tree.sendEventToServer(wired.node, 'mousemove', {});
    await afterDeferred();

    expect(wired.tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);

    wired.messageHandler.simulateResponse();
    await afterDeferred();

    expect(wired.tracker.requestCount).to.equal(0);
    expect(getState()).to.equal(CONNECTED);
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
  });
});
