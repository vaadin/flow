import { expect } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../main/frontend/internal/ApplicationConfiguration';
import { ApplicationConnection } from '../../main/frontend/internal/ApplicationConnection';

function makeRegistry(opts: { initialUidlHandled?: boolean; activeRequest?: boolean } = {}) {
  const log = {
    resynchronized: 0,
    startedRequests: 0,
    handled: [] as unknown[],
    polled: 0,
    events: [] as Array<{ nodeId: number; eventType: string; data: unknown }>
  };
  const registry = {
    log,
    getMessageSender: () => ({
      resynchronize: () => log.resynchronized++,
      sendUnloadBeacon: () => {}
    }),
    getRequestResponseTracker: () => ({
      startRequest: () => log.startedRequests++,
      hasActiveRequest: () => opts.activeRequest ?? false
    }),
    getMessageHandler: () => ({
      handleMessage: (json: unknown) => log.handled.push(json),
      isInitialUidlHandled: () => opts.initialUidlHandled ?? false
    }),
    getPoller: () => ({ poll: () => log.polled++ }),
    getURIResolver: () => ({ resolveVaadinUri: (uri: string) => `resolved:${uri}` }),
    getServerConnector: () => ({
      sendEventMessage: (nodeId: number, eventType: string, data: unknown) =>
        log.events.push({ nodeId, eventType, data })
    }),
    getApplicationConfiguration: () => ({ getUIId: () => 7 }),
    getStateTree: () => ({ getRootNode: () => ({ getId: () => 1, getDebugJson: () => ({ root: true }) }) })
  };
  return registry;
}

const idleScheduler = { hasWorkQueued: () => false };

describe('ApplicationConnection', () => {
  it('resynchronizes when there is no initial UIDL', () => {
    const registry = makeRegistry();
    new ApplicationConnection(registry as never, idleScheduler).start(null);
    expect(registry.log.resynchronized).to.equal(1);
    expect(registry.log.handled).to.deep.equal([]);
  });

  it('handles the initial UIDL (after starting a request) when provided', () => {
    const registry = makeRegistry();
    new ApplicationConnection(registry as never, idleScheduler).start({ syncId: 0 });
    expect(registry.log.startedRequests).to.equal(1);
    expect(registry.log.handled).to.deep.equal([{ syncId: 0 }]);
    expect(registry.log.resynchronized).to.equal(0);
  });

  it('isActive while the initial UIDL is not yet handled', () => {
    const connection = new ApplicationConnection(makeRegistry({ initialUidlHandled: false }) as never, idleScheduler);
    expect(connection.isActive()).to.be.true;
  });

  it('isActive while a request is active or deferred work is queued', () => {
    expect(
      new ApplicationConnection(
        makeRegistry({ initialUidlHandled: true, activeRequest: true }) as never,
        idleScheduler
      ).isActive()
    ).to.be.true;
    expect(
      new ApplicationConnection(makeRegistry({ initialUidlHandled: true }) as never, {
        hasWorkQueued: () => true
      }).isActive()
    ).to.be.true;
  });

  it('is idle when the initial UIDL is handled with no request or deferred work', () => {
    const connection = new ApplicationConnection(makeRegistry({ initialUidlHandled: true }) as never, idleScheduler);
    expect(connection.isActive()).to.be.false;
  });

  it('delegates poll, resolveUri, sendEventMessage, connectWebComponent, getUIId, debug', () => {
    const registry = makeRegistry();
    const connection = new ApplicationConnection(registry as never, idleScheduler);

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
    expect(connection.debug()).to.deep.equal({ root: true });
  });

  describe('published client API', () => {
    // A state tree with one node (id 5) bound to a DOM element, carrying a
    // 'class' (JAVA_CLASS), 'visible' property and one style property.
    function makeRegistryWithNode() {
      const domNode = document.createElement('div');
      const properties: Record<number, Record<string, unknown>> = {
        0: { class: 'com.example.MyView', visible: false }, // ELEMENT_DATA
        12: { color: 'red' } // ELEMENT_STYLE_PROPERTIES
      };
      let domListener: ((node: unknown) => boolean) | null = null;
      const node = {
        getId: () => 5,
        getDomNode: () => domNode,
        getDebugJson: () => ({}),
        getMap: (feature: number) => ({
          getProperty: (key: string) => ({
            getValue: () => properties[feature]?.[key],
            getValueOrDefault: (d: unknown) => properties[feature]?.[key] ?? d
          }),
          getPropertyNames: () => Object.keys(properties[feature] ?? {})
        }),
        addDomNodeSetListener: (listener: (n: unknown) => boolean) => {
          domListener = listener;
        }
      };
      const tree = {
        getRootNode: () => node,
        getNode: (id: number) => (id === 5 ? node : null),
        getStateNodeForDomNode: (el: Node) => (el === domNode ? node : null)
      };
      return { tree, node, domNode, fireDomSet: () => domListener?.(node) };
    }

    it('getByNodeId / getNodeId resolve node<->element both ways', () => {
      const fake = makeRegistryWithNode();
      const registry = { ...makeRegistry(), getStateTree: () => fake.tree };
      const connection = new ApplicationConnection(registry as never, idleScheduler);
      expect(connection.getByNodeId(5)).to.equal(fake.domNode);
      expect(connection.getByNodeId(99)).to.equal(null);
      expect(connection.getNodeId(fake.domNode)).to.equal(5);
      expect(connection.getNodeId(document.createElement('span'))).to.equal(-1);
    });

    it('addDomBindingListener fires the callback when the matching node is bound', () => {
      const fake = makeRegistryWithNode();
      const registry = { ...makeRegistry(), getStateTree: () => fake.tree };
      const connection = new ApplicationConnection(registry as never, idleScheduler);
      let fired = 0;
      connection.addDomBindingListener(5, () => fired++);
      fake.fireDomSet();
      expect(fired).to.equal(1);
    });

    it('exposes javaClass, hidden-by-server and style properties for dev tools', () => {
      const fake = makeRegistryWithNode();
      const registry = { ...makeRegistry(), getStateTree: () => fake.tree };
      const connection = new ApplicationConnection(registry as never, idleScheduler);
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
  });
});
