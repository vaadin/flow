import { wiredRegistryBase } from '../flow/stateTreeTestRegistry';
import { testRegistry } from '../testRegistry';
import type {
  ResourceLoadEvent,
  ResourceLoadListener
} from '../../../../../main/frontend/internal/client/ResourceRegistry';
import { expect } from '@open-wc/testing';
import { MessageHandler, parseJson } from '../../../../../main/frontend/internal/client/communication/MessageHandler';
import { DependencyLoader } from '../../../../../main/frontend/internal/client/DependencyLoader';
import { StateNode } from '../../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../../main/frontend/internal/client/flow/StateTree';
import { UILifecycle, UIState } from '../../../../../main/frontend/internal/client/UILifecycle';
import { NodeFeatures } from '../../../../../main/frontend/internal/flow/internal/nodefeature/NodeFeatures';

function makeRegistry(maxMessageSuspendTimeout = 10000) {
  const log = {
    constants: [] as unknown[],
    executed: [] as unknown[],
    endRequests: 0,
    stopLoadings: 0,
    states: [] as string[],
    clearedResources: [] as string[],
    resynchronized: false,
    sessionExpiredHandled: false,
    unrecoverableErrorHandled: false
  };
  let state: UIState = UIState.INITIALIZING;
  // A response normally arrives while the request that triggered it is still
  // active, so the tracker starts out with one; ending it clears the flag, as
  // the real tracker does.
  let activeRequest = true;
  return {
    log,
    getState: () => state,
    startRequest: () => {
      activeRequest = true;
    },
    registry: testRegistry({
      UILifecycle: {
        getState: () => state,
        setState: (s: UIState) => {
          state = s;
          log.states.push(s);
        }
      },
      MessageSender: {
        getResynchronizationState: () => 'NOT_ACTIVE',
        clearResynchronizationState: () => {},
        setClientToServerMessageId: () => {},
        requestResynchronize: () => true,
        resynchronize: () => {
          log.resynchronized = true;
        }
      },
      StateTree: { prepareForResync: () => {} },
      RequestResponseTracker: {
        fireResponseHandlingStarted: () => {},
        endRequest: () => {
          // The real tracker throws when there is nothing to end.
          if (!activeRequest) {
            throw new Error('endRequest called when no request is active');
          }
          log.endRequests++;
          activeRequest = false;
        },
        hasActiveRequest: () => activeRequest
      },
      LoadingIndicatorStateHandler: { stopLoading: () => log.stopLoadings++ },
      ConstantPool: { importFromJson: (c: unknown) => log.constants.push(c) },
      ExecuteJavaScriptProcessor: { execute: (c: unknown) => log.executed.push(c) },
      DependencyLoader: { loadDependencies: () => {} },
      SystemErrorHandler: {
        handleSessionExpiredError: () => {
          log.sessionExpiredHandled = true;
        },
        handleUnrecoverableError: () => {
          log.unrecoverableErrorHandled = true;
        }
      },
      ApplicationConfiguration: { getMaxMessageSuspendTimeout: () => maxMessageSuspendTimeout },
      ResourceLoader: { clearLoadedResourceById: (id: string) => log.clearedResources.push(id) }
    })
  };
}

// handleJSON is protected, and the Gwt test reaches it through Java package
// access; a subclass is the TypeScript equivalent, with no widening in the port.
class TestMessageHandler extends MessageHandler {
  callHandleJSON(json: Record<string, unknown>): void {
    this.handleJSON(json);
  }
}

// The dependency-ordering cases assert that a dependency finishes loading before
// tree changes are applied, so they need the real DependencyLoader and StateTree
// wired into one registry the way the Java suite assembles its Registry. Both
// record into a shared list — the Java suite subclasses ResourceLoader and
// StateTree for exactly that — so the order can be compared.
function makeWiredRegistry() {
  const order: string[] = [];
  const scriptUrls: string[] = [];

  const uiLifecycle = new UILifecycle();
  uiLifecycle.setState(UIState.RUNNING);
  const registry = wiredRegistryBase(uiLifecycle);
  registry.register('ServerConnector', {
    sendEventMessage: () => {},
    sendNodeSyncMessage: () => {},
    sendTemplateEventMessage: () => {},
    sendExistingElementAttachToServer: () => {},
    sendExistingElementWithIdAttachToServer: () => {},
    sendReturnChannelMessage: () => {}
  });

  // Records when changes reach the tree, as the Java suite's TestStateTree does.
  class RecordingStateTree extends StateTree {
    override setUpdateInProgress(updateInProgress: boolean): void {
      if (updateInProgress) {
        order.push('StateTree');
      }
      super.setUpdateInProgress(updateInProgress);
    }
  }
  const tree = new RecordingStateTree(registry);
  registry.register('StateTree', tree);

  // Records the load, as the Java suite's TestResourceLoader does, and completes
  // it on a later task: a real load is asynchronous, and only then does the
  // eager-dependency gate — the thing these cases assert — decide the order.
  // loadDynamicImport runs the expression, which is what the real loader does
  // and what the dynamic-import case asserts on.
  // Only the loader identity is read from the event, and nothing under test reads
  // it, so it stands in for the one a real load would carry.
  const event = { getResourceLoader: () => null, getResourceData: () => '' } as unknown as ResourceLoadEvent;
  // The Java fake records the load when it is requested; recording it when it
  // completes is what makes "handled before applying changes" checkable, since a
  // real load finishes on a later task.
  const complete = (listener: ResourceLoadListener | null) => {
    setTimeout(() => {
      order.push('ResourceLoader');
      listener?.onLoad(event);
    }, 0);
  };
  const recordScript = (url: string, listener: ResourceLoadListener | null) => {
    scriptUrls.push(url);
    complete(listener);
  };
  registry
    .register('ResourceLoader', {
      loadJsModule: recordScript,
      loadScript: recordScript,
      loadDynamicImport: (expression: string, listener: ResourceLoadListener) => {
        setTimeout(() => {
          new Function(expression)();
          order.push('ResourceLoader');
          listener.onLoad(event);
        }, 0);
      },
      clearLoadedResourceById: () => {}
    })
    .register('URIResolver', { resolveVaadinUri: (uri: string) => uri })
    .register('RequestResponseTracker', {
      fireResponseHandlingStarted: () => {},
      // The Java suite's TestRequestResponseTracker makes endRequest a no-op.
      endRequest: () => {},
      hasActiveRequest: () => true
    })
    .register('LoadingIndicatorStateHandler', { stopLoading: () => {} })
    .register('MessageSender', {
      getResynchronizationState: () => 'NOT_ACTIVE',
      clearResynchronizationState: () => {},
      setClientToServerMessageId: () => {},
      requestResynchronize: () => true,
      resynchronize: () => {}
    })
    .register('ExecuteJavaScriptProcessor', { execute: () => {} })
    .register('SystemErrorHandler', { handleSessionExpiredError: () => {}, handleUnrecoverableError: () => {} });
  registry.register('DependencyLoader', new DependencyLoader(registry));

  const messageHandler = new TestMessageHandler(registry);
  return { order, scriptUrls, tree, messageHandler };
}

// The handler defers dependency loading, change processing and the after-update
// callbacks; these await them.
const afterDeferred = (): Promise<void> =>
  new Promise((resolve) => {
    setTimeout(resolve, 50);
  });

describe('MessageHandler', () => {
  it('parseJson parses JSON text, and reports unparseable or missing text as null', () => {
    // Beyond the Java suite: GwtMessageHandlerTest exercises parseJson only through handleJSON, so its null paths have no Java case.
    expect(parseJson('{"a":1,"b":"x"}')).to.eql({ a: 1, b: 'x' });
    expect(parseJson('not json')).to.equal(null);
    expect(parseJson(null)).to.equal(null);
  });

  describe('class', () => {
    // The handler defers message processing through the eager-dependency
    // tracker, but the fake dependency loader starts no load, so the counter
    // stays at zero and the deferred work runs straight away.

    it('starts in an undefined sync-id state with default csrf and no push id', () => {
      // Beyond the Java suite: No Java case asserts the initial state.
      const handler = new MessageHandler(makeRegistry().registry);
      expect(handler.getLastSeenServerSyncId()).to.equal(-1);
      expect(handler.getCsrfToken()).to.equal('init');
      expect(handler.getPushId()).to.equal(null);
      expect(handler.isInitialUidlHandled()).to.be.false;
    });

    it('starts the UI and applies an in-order message (constants, csrf, sync id, end request)', () => {
      // Beyond the Java suite: No Java case walks a full in-order message.
      const registry = makeRegistry();
      const handler = new MessageHandler(registry.registry);
      handler.handleMessage({
        syncId: 0,
        'Vaadin-Security-Key': 'tok',
        constants: { c: 1 }
      });

      expect(registry.log.states).to.deep.equal(['RUNNING']);
      expect(registry.log.constants).to.deep.equal([{ c: 1 }]);
      expect(handler.getCsrfToken()).to.equal('tok');
      expect(handler.getLastSeenServerSyncId()).to.equal(0);
      expect(handler.isInitialUidlHandled()).to.be.true;
      expect(registry.log.endRequests).to.equal(1);
      expect(registry.log.stopLoadings).to.equal(1);
    });

    it('queues an out-of-order message without applying it', () => {
      // Beyond the Java suite: The Gwt suite covers the forced resync, not the queueing itself.
      const registry = makeRegistry();
      const handler = new MessageHandler(registry.registry);
      handler.handleMessage({ syncId: 0, constants: { first: 1 } });
      expect(handler.getLastSeenServerSyncId()).to.equal(0);

      // syncId 5 while expecting 1 -> queued, not applied.
      handler.handleMessage({ syncId: 5, constants: { skipped: 1 } });
      expect(registry.log.constants).to.deep.equal([{ first: 1 }]); // second not imported
      expect(handler.getLastSeenServerSyncId()).to.equal(0);
    });

    it('ignores an already-seen (stale) message but still ends the request', () => {
      // Beyond the Java suite: No Java case covers a stale re-send.
      const registry = makeRegistry();
      const handler = new MessageHandler(registry.registry);
      handler.handleMessage({ syncId: 0 });
      registry.startRequest();
      handler.handleMessage({ syncId: 1 });
      const endRequestsBefore = registry.log.endRequests;

      // syncId 0 again: already seen -> ignored, but the request it responds to
      // is ended.
      registry.startRequest();
      handler.handleMessage({ syncId: 0, constants: { stale: 1 } });
      expect(registry.log.constants).to.deep.equal([]); // never applied any constants
      expect(registry.log.endRequests).to.equal(endRequestsBefore + 1);
    });

    it('ignores a duplicate response arriving with no active request instead of throwing', () => {
      // Beyond the Java suite: No Java case covers a re-sent response.
      const registry = makeRegistry();
      const handler = new MessageHandler(registry.registry);
      handler.handleMessage({ syncId: 0 });
      expect(registry.log.endRequests).to.equal(1);

      // The same response once more, with the request already ended by the
      // first copy: ignored without ending a request that is not active.
      handler.handleMessage({ syncId: 0 });
      expect(registry.log.endRequests).to.equal(1);
      // The loading indicator is still stopped for the duplicate.
      expect(registry.log.stopLoadings).to.equal(2);
    });

    it('runs a one-shot session-expired handler when set', () => {
      // Beyond the Java suite: No Java case covers setNextResponseSessionExpiredHandler.
      const registry = makeRegistry();
      const handler = new MessageHandler(registry.registry);
      let expiredHandled = 0;
      handler.setNextResponseSessionExpiredHandler(() => expiredHandled++);
      handler.handleMessage({ syncId: 0, meta: { sessionExpired: true } });
      expect(expiredHandled).to.equal(1);
    });

    // The session-expired handler runs after a delay, so these await it.
    const afterSessionExpiredDelay = async (): Promise<void> =>
      new Promise((resolve) => {
        setTimeout(resolve, 300);
      });

    it('handles a module dependency before applying changes to the tree', async () => {
      // Ported from testMessageProcessing_moduleDependencyIsHandledBeforeApplyingChangesToTree.
      const wired = makeWiredRegistry();
      // An empty changes list still starts change processing, which must happen
      // after the dependency has loaded.
      wired.messageHandler.callHandleJSON({
        syncId: 0,
        changes: [],
        EAGER: [{ type: 'JS_MODULE', url: 'foo' }]
      });
      await afterDeferred();

      expect(wired.scriptUrls).to.contain('foo');
      expect(wired.order.length).to.be.at.least(2);
      expect(wired.order[0]).to.equal('ResourceLoader');
      expect(wired.order[1]).to.equal('StateTree');
    });

    it('handles a dynamic dependency before applying changes to the tree', async () => {
      // Ported from testMessageProcessing_dynamicDependencyIsHandledBeforeApplyingChangesToTree.
      const wired = makeWiredRegistry();
      (window as { testEvents?: string[] }).testEvents = [];
      wired.messageHandler.callHandleJSON({
        syncId: 0,
        changes: [],
        LAZY: [{ type: 'DYNAMIC_IMPORT', url: "window.testEvents.push('test-dependency');" }]
      });
      await afterDeferred();

      // The dependency's own script ran, and it did so before the changes
      // reached the tree — the Java case asserts the same two events in order.
      const events = (window as { testEvents?: string[] }).testEvents!;
      expect(events[0]).to.equal('test-dependency');
      expect(wired.order).to.deep.equal(['ResourceLoader', 'StateTree']);
      delete (window as { testEvents?: string[] }).testEvents;
    });

    it('requests a resync when an out-of-order message is not resolved in time', async () => {
      // Ported from testForceHandleMessage_resyncIsRequested. The configuration
      // allows 200 ms of message suspension.
      const registry = makeRegistry(200);
      const handler = new MessageHandler(registry.registry);

      handler.handleMessage({ syncId: 1 });
      handler.handleMessage({ syncId: 3 });

      expect(registry.log.resynchronized).to.be.false;
      await new Promise((resolve) => {
        setTimeout(resolve, 300);
      });
      expect(registry.log.resynchronized).to.be.true;
    });

    it('shows no session-expired message when the UI is already terminated', async () => {
      // Ported from testHandleJSON_uiTerminated_sessionExpiredMessageNotShown.
      const registry = makeRegistry();
      const handler = new TestMessageHandler(registry.registry);
      // The UI has been terminated, for instance by the redirect JS that
      // Page::setLocation causes.
      registry.registry.getUILifecycle().setState(UIState.RUNNING);
      registry.registry.getUILifecycle().setState(UIState.TERMINATED);

      handler.callHandleJSON({ meta: { sessionExpired: true } });
      await afterSessionExpiredDelay();

      expect(registry.log.sessionExpiredHandled).to.be.false;
      expect(registry.log.unrecoverableErrorHandled).to.be.false;
      expect(registry.getState()).to.equal('TERMINATED');
    });

    it('shows no unrecoverable-error message when the UI is already terminated', async () => {
      // Ported from testHandleJSON_uiTerminated_unrecoverableErrorMessageNotShown.
      const registry = makeRegistry();
      const handler = new TestMessageHandler(registry.registry);
      registry.registry.getUILifecycle().setState(UIState.RUNNING);
      registry.registry.getUILifecycle().setState(UIState.TERMINATED);

      handler.callHandleJSON({ meta: { appError: true } });
      await afterSessionExpiredDelay();

      expect(registry.log.sessionExpiredHandled).to.be.false;
      expect(registry.log.unrecoverableErrorHandled).to.be.false;
      expect(registry.getState()).to.equal('TERMINATED');
    });

    it('shows the session-expired message and terminates a running UI', async () => {
      // Ported from testHandleJSON_sessionExpiredAndUIRunning_sessionExpiredMessageShown.
      const registry = makeRegistry();
      const handler = new TestMessageHandler(registry.registry);
      registry.registry.getUILifecycle().setState(UIState.RUNNING);

      handler.callHandleJSON({ meta: { sessionExpired: true } });
      await afterSessionExpiredDelay();

      expect(registry.log.sessionExpiredHandled).to.be.true;
      expect(registry.log.unrecoverableErrorHandled).to.be.false;
      expect(registry.getState()).to.equal('TERMINATED');
    });

    it('shows the unrecoverable-error message and terminates a running UI', async () => {
      // Ported from testHandleJSON_unrecoverableErrorAndUIRunning_unrecoverableErrorMessageShown.
      const registry = makeRegistry();
      const handler = new TestMessageHandler(registry.registry);
      registry.registry.getUILifecycle().setState(UIState.RUNNING);

      handler.callHandleJSON({ meta: { appError: { caption: 'error', message: 'oops' } } });
      await afterSessionExpiredDelay();

      expect(registry.log.unrecoverableErrorHandled).to.be.true;
      expect(registry.log.sessionExpiredHandled).to.be.false;
      expect(registry.getState()).to.equal('TERMINATED');
    });

    it('calls afterServerUpdate on the DOM node of an updated state node', async () => {
      // Beyond the Java suite: GwtMessageHandlerTest asserts the ordering of
      // dependency loading and tree updates, not the after-update callback.
      const wired = makeWiredRegistry();
      const node = new StateNode(2, wired.tree);
      wired.tree.registerNode(node);
      const element = document.createElement('div');
      let called = 0;
      (element as unknown as { afterServerUpdate: () => void }).afterServerUpdate = () => {
        called += 1;
      };
      node.setDomNode(element);

      wired.messageHandler.callHandleJSON({
        syncId: 0,
        changes: [{ node: 2, type: 'put', feat: NodeFeatures.ELEMENT_PROPERTIES, key: 'foo', value: 'bar' }]
      });
      await afterDeferred();

      expect(called).to.equal(1);
    });

    describe('beyond the Java suite', () => {
      it('removes the stylesheet elements a message lists, and clears them from the loader', () => {
        const link = document.createElement('link');
        link.setAttribute('data-id', 'dep-x');
        const style = document.createElement('style');
        style.setAttribute('data-id', 'dep-x');
        const keep = document.createElement('style');
        keep.setAttribute('data-id', 'dep-y');
        document.head.append(link, style, keep);

        const registry = makeRegistry();
        new MessageHandler(registry.registry).handleMessage({ syncId: 0, stylesheetRemovals: ['dep-x'] });

        expect(document.querySelector('[data-id="dep-x"]')).to.equal(null);
        expect(document.querySelector('[data-id="dep-y"]')).to.not.equal(null);
        expect(registry.log.clearedResources).to.deep.equal(['dep-x']);
        keep.remove();
      });

      it('reports finite processing and bootstrap timings after a message', () => {
        const registry = makeRegistry();
        const handler = new MessageHandler(registry.registry);
        handler.handleMessage({ syncId: 0 });

        // [lastProcessingTime, totalProcessingTime, ...serverTimings?, bootstrapTime].
        const profiling = handler.getProfilingData();
        expect(profiling.length).to.be.at.least(3);
        profiling.forEach((value) => expect(value).to.be.finite);
        expect(profiling[0]).to.be.at.least(0);
      });

      it('keeps processing a message whose stylesheetRemovals is null', () => {
        const registry = makeRegistry();
        const handler = new MessageHandler(registry.registry);
        // Java early-returns on a null/empty array; iterating it here would throw
        // inside the processing try and skip everything after it.
        handler.handleMessage({ syncId: 0, stylesheetRemovals: null, constants: { c: 1 } });

        expect(registry.log.constants).to.deep.equal([{ c: 1 }]);
        expect(registry.log.endRequests).to.equal(1);
      });
    });
  });
});
