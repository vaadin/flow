// The Java counterpart is GwtApplicationConnectionTest in src/test-gwt; its single
// case is ported at the end of this file. com.vaadin.client.ApplicationConnection
// has no JRE-side test class, so every other case here is beyond the Java suite.

import { testRegistry } from './testRegistry';
import { expect, waitUntil } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../../../main/frontend/internal/client/ApplicationConfiguration';
import { ApplicationConnection } from '../../../../main/frontend/internal/client/ApplicationConnection';
import { onModuleLoad } from '../../../../main/frontend/internal/client/bootstrap/Bootstrapper';
import { StateNode } from '../../../../main/frontend/internal/client/flow/StateNode';
import { StateTree } from '../../../../main/frontend/internal/client/flow/StateTree';
import { inertRegistry } from './flow/stateTreeTestRegistry';

// A registry whose services are fakes but whose state tree is real: the fakes
// are checked against the signatures the engine slices name, and the tree, node
// and property reads the client API makes go through the ported classes.
function makeRegistry(opts: { initialUidlHandled?: boolean; activeRequest?: boolean; tree?: StateTree } = {}) {
  const log = {
    resynchronized: 0,
    startedRequests: 0,
    handled: [] as unknown[],
    polled: 0,
    events: [] as Array<{ nodeId: number; eventType: string; data: unknown }>
  };
  const tree = opts.tree ?? new StateTree(inertRegistry());
  // Something on the root node, so debug() has to reach that node rather than
  // any empty one. 'tag' in feature 0 is what the server writes for an element.
  tree.getRootNode().getMap(0).getProperty('tag').setValue('body');
  const registry = testRegistry({
    MessageSender: {
      resynchronize: () => log.resynchronized++,
      sendUnloadBeacon: () => {}
    },
    RequestResponseTracker: {
      startRequest: () => log.startedRequests++,
      hasActiveRequest: () => opts.activeRequest ?? false
    },
    MessageHandler: {
      handleMessage: (json: unknown) => log.handled.push(json),
      isInitialUidlHandled: () => opts.initialUidlHandled ?? false,
      getProfilingData: () => [1, 2]
    },
    Poller: { poll: () => log.polled++ },
    URIResolver: { resolveVaadinUri: (uri: string) => `resolved:${uri}` },
    ServerConnector: {
      sendEventMessage: (nodeId: number, eventType: string, data: unknown) =>
        log.events.push({ nodeId, eventType, data })
    },
    ApplicationConfiguration: { getUIId: () => 7 },
    StateTree: tree
  });
  return { registry, log, tree };
}

const idleScheduler = { hasWorkQueued: () => false };

// Records the event types registered on a target, the way the Java suite's
// addEventsObserver monkey-patches addEventListener. Returns the undo function.
function observeAddedEvents(target: EventTarget, into: string[]): () => void {
  const original = target.addEventListener;
  target.addEventListener = function observed(this: EventTarget, type: string, ...rest: unknown[]) {
    into.push(type);
    return (original as (...args: unknown[]) => void).call(this, type, ...rest);
  } as typeof target.addEventListener;
  return () => {
    target.addEventListener = original;
  };
}

describe('ApplicationConnection', () => {
  it('resynchronizes when there is no initial UIDL', () => {
    const registry = makeRegistry();
    new ApplicationConnection(registry.registry, idleScheduler).start(null);
    expect(registry.log.resynchronized).to.equal(1);
    expect(registry.log.handled).to.deep.equal([]);
  });

  it('handles the initial UIDL (after starting a request) when provided', () => {
    const registry = makeRegistry();
    new ApplicationConnection(registry.registry, idleScheduler).start({ syncId: 0 });
    expect(registry.log.startedRequests).to.equal(1);
    expect(registry.log.handled).to.deep.equal([{ syncId: 0 }]);
    expect(registry.log.resynchronized).to.equal(0);
  });

  it('isActive while the initial UIDL is not yet handled', () => {
    const connection = new ApplicationConnection(makeRegistry({ initialUidlHandled: false }).registry, idleScheduler);
    expect(connection.isActive()).to.be.true;
  });

  it('isActive while a request is active or deferred work is queued', () => {
    expect(
      new ApplicationConnection(
        makeRegistry({ initialUidlHandled: true, activeRequest: true }).registry,
        idleScheduler
      ).isActive()
    ).to.be.true;
    expect(
      new ApplicationConnection(makeRegistry({ initialUidlHandled: true }).registry, {
        hasWorkQueued: () => true
      }).isActive()
    ).to.be.true;
  });

  it('is idle when the initial UIDL is handled with no request or deferred work', () => {
    const connection = new ApplicationConnection(makeRegistry({ initialUidlHandled: true }).registry, idleScheduler);
    expect(connection.isActive()).to.be.false;
  });

  it('delegates poll, resolveUri, sendEventMessage, connectWebComponent, getUIId, debug', () => {
    const registry = makeRegistry();
    const connection = new ApplicationConnection(registry.registry, idleScheduler);

    connection.poll();
    expect(registry.log.polled).to.equal(1);
    expect(connection.resolveUri('context://x')).to.equal('resolved:context://x');
    connection.sendEventMessage(2, 'click', { k: 1 });
    connection.connectWebComponent({ tag: 'my-el' });
    expect(registry.log.events).to.deep.equal([
      { nodeId: 2, eventType: 'click', data: { k: 1 } },
      { nodeId: 1, eventType: 'connect-web-component', data: { tag: 'my-el' } }
    ]);
    expect(connection.getUIId()).to.equal(7);
    expect(connection.getProfilingData()).to.deep.equal([1, 2]);
    expect(connection.debug()).to.deep.equal({ elementData: { tag: 'body' } });
  });

  describe('published client API', () => {
    // A state tree with one node (id 5) bound to a DOM element, carrying the
    // 'jc' (NodeProperties.JAVA_CLASS) and 'visible' properties and one style
    // property. The keys are the wire names the server actually writes, so a
    // wrong constant in the engine shows up here as a missing value.
    function makeRegistryWithNode() {
      const tree = new StateTree(inertRegistry());
      const node = new StateNode(5, tree);
      tree.registerNode(node);
      // Written with the feature ids and property keys the server actually puts
      // on the wire (ELEMENT_DATA = 0, ELEMENT_STYLE_PROPERTIES = 12), not with
      // the engine's own constants: a wrong constant in the engine then shows up
      // here as a missing value instead of cancelling out.
      const elementData = node.getMap(0);
      elementData.getProperty('jc').setValue('com.example.MyView');
      elementData.getProperty('visible').setValue(false);
      node.getMap(12).getProperty('color').setValue('red');
      const domNode = document.createElement('div');
      const { registry } = makeRegistry({ tree });
      // The DOM node is attached on demand: setDomNode is what notifies the
      // dom-set listeners, and a state node only takes one.
      return { registry, node, domNode, attach: () => node.setDomNode(domNode) };
    }

    it('getByNodeId / getNodeId resolve node<->element both ways', () => {
      const fixture = makeRegistryWithNode();
      fixture.attach();
      const connection = new ApplicationConnection(fixture.registry, idleScheduler);
      expect(connection.getByNodeId(5)).to.equal(fixture.domNode);
      expect(connection.getByNodeId(99)).to.equal(null);
      expect(connection.getNodeId(fixture.domNode)).to.equal(5);
      expect(connection.getNodeId(document.createElement('span'))).to.equal(-1);
    });

    it('addDomBindingListener fires the callback when the matching node is bound', () => {
      const fixture = makeRegistryWithNode();
      const connection = new ApplicationConnection(fixture.registry, idleScheduler);
      let fired = 0;
      connection.addDomBindingListener(5, () => fired++);
      fixture.attach();
      expect(fired).to.equal(1);
    });

    it('exposes javaClass, hidden-by-server and style properties for dev tools', () => {
      const connection = new ApplicationConnection(makeRegistryWithNode().registry, idleScheduler);
      expect(connection.getJavaClass(5)).to.equal('com.example.MyView');
      expect(connection.isHiddenByServer(5)).to.be.true; // visible=false
      expect(connection.getElementStyleProperties(5)).to.deep.equal({ color: 'red' });
    });
  });

  describe('create', () => {
    it('assembles the registry, binds the root node, and publishes the client', () => {
      const savedVaadin = (window as { Vaadin?: unknown }).Vaadin;
      (window as { Vaadin?: unknown }).Vaadin = { Flow: { clients: {} }, connectionState: { state: '' } };
      try {
        const config = new ApplicationConfiguration();
        config.setApplicationId('ROOT-2147483647');
        config.setServiceUrl('/app');
        config.setUIId(0);
        config.setHeartbeatInterval(-1);

        const rootElement = document.createElement('div');
        const connection = ApplicationConnection.create(config, rootElement);

        expect(connection).to.be.instanceOf(ApplicationConnection);
        // The client API is published under the suffix-stripped application id.
        const clients = (window as unknown as { Vaadin: { Flow: { clients: Record<string, unknown> } } }).Vaadin.Flow
          .clients;
        expect(clients.ROOT).to.not.equal(undefined);
        // No UIDL handled yet -> the application still has work to do.
        expect(connection.isActive()).to.be.true;
      } finally {
        (window as { Vaadin?: unknown }).Vaadin = savedVaadin;
      }
    });

    it('installs one uncaught-error listener however many connections are created', () => {
      // GWT's uncaught exception handler was a single replaceable slot, so a
      // second application did not make every error be reported twice.
      const savedVaadin = (window as { Vaadin?: unknown }).Vaadin;
      const registeredTypes: string[] = [];
      const restoreWindow = observeAddedEvents(window, registeredTypes);
      try {
        (window as { Vaadin?: unknown }).Vaadin = { Flow: { clients: {} }, connectionState: { state: '' } };
        const config = new ApplicationConfiguration();
        config.setApplicationId('ROOT-1');
        config.setServiceUrl('/app');
        config.setUIId(0);
        config.setHeartbeatInterval(-1);

        ApplicationConnection.create(config, document.createElement('div'));
        ApplicationConnection.create(config, document.createElement('div'));

        // One at most: zero when an earlier case already installed the listener.
        expect(registeredTypes.filter((type) => type === 'error')).to.have.length.at.most(1);
      } finally {
        restoreWindow();
        (window as { Vaadin?: unknown }).Vaadin = savedVaadin;
      }
    });
  });

  // This has to stay the last block in the file: the case below starts a real
  // application through the bootstrap, which binds the root state node to the
  // shared document.body and has no shutdown path, so anything running after it
  // would inherit that body.
  describe('bootstrap', () => {
    it('does not add navigation events for web components', async () => {
      // Ported from GwtApplicationConnectionTest.test_should_not_addNavigationEvents_forWebComponents.
      const savedVaadin = (window as { Vaadin?: unknown }).Vaadin;
      const windowEvents: string[] = [];
      const bodyEvents: string[] = [];
      const restoreWindow = observeAddedEvents(window, windowEvents);
      const restoreBody = observeAddedEvents(document.body, bodyEvents);
      try {
        // Mirrors mockFlowBootstrapScript(true) and createDummyConnectionState from
        // the Java test base. The initial UIDL is the one addition: the GWT test
        // leaves it out, so the application resynchronizes over XHR against a
        // service URL nothing answers, which a browser test cannot leave pending.
        const configuration: Record<string, unknown> = {
          heartbeatInterval: 300,
          maxMessageSuspendTimeout: 5000,
          contextRootUrl: '../',
          debug: true,
          'v-uiId': 0,
          serviceUrl: '//localhost:8080/flow/',
          webComponentMode: true,
          uidl: { syncId: 0, changes: [] }
        };
        (window as { Vaadin?: unknown }).Vaadin = {
          Flow: {
            clients: {},
            registerWidgetset: (name: string, callback: (applicationId: string) => void) => callback(name),
            getApp: () => ({ getConfig: (key: string) => configuration[key] })
          },
          connectionState: {
            state: 'connected',
            requestCount: 0,
            setState(state: string) {
              this.state = state;
            },
            loadingStarted() {},
            loadingFinished() {},
            loadingFailed() {}
          }
        };

        onModuleLoad();

        // The widgetset callback runs immediately, but the application starts on a
        // deferred command and the engine module is imported asynchronously; the
        // published client is the signal that it has started.
        const clients = (window as unknown as { Vaadin: { Flow: { clients: Record<string, unknown> } } }).Vaadin.Flow
          .clients;
        await waitUntil(() => clients.client !== undefined, 'the application was never started');

        // The engine's own window listener first: absent navigation events only
        // mean anything once the observer is known to have seen the application
        // start. The Java case relies on delayTestFinish for the same reason.
        expect(windowEvents).to.contain('pagehide');
        expect(windowEvents).to.not.contain('popstate');
        expect(bodyEvents).to.not.contain('click');
      } finally {
        restoreBody();
        restoreWindow();
        // The application keeps running, so leave the globals it reads in place
        // rather than restoring an undefined Vaadin: a late callback into
        // ConnectionIndicator dereferences window.Vaadin without a guard.
        if (savedVaadin !== undefined) {
          (window as { Vaadin?: unknown }).Vaadin = savedVaadin;
        }
      }
    });
  });
});
