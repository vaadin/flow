// Every case here is beyond the Java suite: com.vaadin.client.bootstrap.Bootstrapper
// has no test class of its own. The one Java-side case that drives it,
// GwtApplicationConnectionTest.test_should_not_addNavigationEvents_forWebComponents,
// is ported in ApplicationConnectionTests, next to the class it is written against.
//
// The module exports what Bootstrapper.java makes public - onModuleLoad,
// startApplication and registerCallback - so the cases drive those. The
// configuration reader is private, as in Java, and is therefore covered through
// what a started application publishes: the ui id, the production and
// request-timing flags, the exported web components, the servlet version and the
// resolved context root. The values no published method exposes (the heartbeat
// and suspend intervals, the session-expired error, the Atmosphere versions, the
// dev-tools and live-reload settings) are read by the same key-for-key block and
// are not asserted. Neither is the assert that a missing v-uiId, heartbeat
// interval or suspend timeout raises: it fires inside a deferred command, where
// a browser reports the throw rather than handing it to the caller.

import { expect, waitUntil } from '@open-wc/testing';
import {
  onModuleLoad,
  registerCallback,
  startApplication
} from '../../../../../main/frontend/internal/client/bootstrap/Bootstrapper';
import type { ConfigObject } from '../../../../../main/frontend/internal/client/bootstrap/JsoConfiguration';

// The bootstrap configuration object the server writes into the page, read
// through the ported JsoConfiguration accessors as in production.
function makeJso(values: Record<string, unknown>): ConfigObject {
  return {
    getConfig: (name: string) =>
      name === 'versionInfo'
        ? { vaadinVersion: values.vaadinVersion, atmosphereVersion: values.atmosphereVersion }
        : values[name]
  };
}

// A configuration the whole bootstrap can run on: absolute URLs so the resolved
// context root is predictable, an initial UIDL so the started application handles
// a message instead of resynchronizing over XHR, and no heartbeat.
function bootstrapConfiguration(values: Record<string, unknown> = {}): Record<string, unknown> {
  return {
    serviceUrl: 'http://host/app/',
    contextRootUrl: '../',
    'v-uiId': 7,
    heartbeatInterval: -1,
    maxMessageSuspendTimeout: 5000,
    vaadinVersion: '24.9',
    debug: true,
    requestTiming: true,
    webcomponents: ['my-el'],
    uidl: { syncId: 0, changes: [] },
    ...values
  };
}

// The published client, as the browser sees it.
interface PublishedClient {
  productionMode: boolean;
  exportedWebComponents: string[];
  getUIId(): number;
  resolveUri(uri: string): string;
  getVersionInfo?(parameter?: unknown): { flow: string };
  getProfilingData?(): number[];
  getNodeInfo?(nodeId: number): unknown;
}

describe('Bootstrapper', () => {
  const win = window as unknown as { WebComponents?: unknown; Vaadin?: unknown };

  // Stands in for vaadinBootstrap.js: it holds the clients the engine publishes
  // into and answers the configuration lookup.
  function installBootstrapScript(configuration: Record<string, unknown>) {
    const clients: Record<string, unknown> = {};
    const lookedUp: string[] = [];
    win.Vaadin = {
      Flow: {
        clients,
        registerWidgetset: (_name: string, callback: (applicationId: string) => void) => callback('registered'),
        getApp: (appId: string) => {
          lookedUp.push(appId);
          return makeJso(configuration);
        }
      },
      connectionState: {
        state: 'connected',
        setState(state: string) {
          this.state = state;
        },
        loadingStarted() {},
        loadingFinished() {},
        loadingFailed() {}
      }
    };
    const started = async (appId: string): Promise<PublishedClient> => {
      await waitUntil(() => clients[appId] !== undefined, `the application ${appId} was never started`);
      return clients[appId] as PublishedClient;
    };
    return { clients, lookedUp, started };
  }

  it('registerCallback registers startApplication under the widgetset name', () => {
    const saved = win.Vaadin;
    try {
      const calls: Array<[string, (id: string) => void]> = [];
      win.Vaadin = { Flow: { registerWidgetset: (name: string, cb: (id: string) => void) => calls.push([name, cb]) } };
      registerCallback('com.example.Widgetset');
      expect(calls).to.have.length(1);
      expect(calls[0][0]).to.equal('com.example.Widgetset');
      // Java registers Bootstrapper::startApplication, not a caller-supplied one.
      expect(calls[0][1]).to.equal(startApplication);
    } finally {
      win.Vaadin = saved;
    }
  });

  describe('onModuleLoad', () => {
    let saved: unknown;

    beforeEach(() => {
      saved = win.Vaadin;
    });

    afterEach(() => {
      win.Vaadin = saved;
    });

    it('does nothing when vaadinBootstrap.js was not loaded', () => {
      win.Vaadin = {};
      expect(() => onModuleLoad()).to.not.throw();
    });

    it('registers the widgetset callback, and only once per bootstrap context', () => {
      const registered: string[] = [];
      const flow = { registerWidgetset: (name: string) => registered.push(name) };
      win.Vaadin = { Flow: flow };

      onModuleLoad();
      expect(registered).to.deep.equal(['client']);

      // Second call in the same bootstrap context is a no-op.
      onModuleLoad();
      expect(registered).to.deep.equal(['client']);

      // A replaced window.Vaadin.Flow is a new context, so it registers again.
      win.Vaadin = { Flow: { registerWidgetset: (name: string) => registered.push(name) } };
      onModuleLoad();
      expect(registered).to.deep.equal(['client', 'client']);
    });
  });

  // Each case below starts a real application, which binds a root state node to
  // the shared document.body and has no shutdown path; binding an empty root node
  // installs nothing on the body, and each application publishes under its own id.
  describe('startApplication', () => {
    let savedVaadin: unknown;
    let savedWebComponents: unknown;

    beforeEach(() => {
      savedVaadin = win.Vaadin;
      savedWebComponents = win.WebComponents;
    });

    afterEach(() => {
      win.Vaadin = savedVaadin;
      win.WebComponents = savedWebComponents;
    });

    it('reads the configuration from the DOM into the published client', async () => {
      win.WebComponents = undefined;
      const bootstrap = installBootstrapScript(bootstrapConfiguration());

      startApplication('devapp');
      const client = await bootstrap.started('devapp');

      expect(bootstrap.lookedUp).to.contain('devapp');
      expect(client.getUIId()).to.equal(7); // v-uiId
      expect(client.productionMode).to.be.false; // debug: true
      expect(client.exportedWebComponents).to.deep.equal(['my-el']); // webcomponents
      // serviceUrl + contextRootUrl, resolved: http://host/app/../ -> http://host/
      expect(client.resolveUri('context://style.css')).to.equal('http://host/style.css');
      // The development-mode block, and the version the parameter is ignored for.
      expect(client.getVersionInfo?.()).to.deep.equal({ flow: '24.9' }); // vaadinVersion
      expect(client.getNodeInfo).to.be.a('function');
      expect(client.getProfilingData).to.be.a('function'); // requestTiming: true
    });

    it('falls back to the current location when no service URL is configured', async () => {
      win.WebComponents = undefined;
      const configuration = bootstrapConfiguration({ contextRootUrl: './', debug: false });
      delete configuration.serviceUrl;
      const bootstrap = installBootstrapScript(configuration);

      startApplication('rootedapp');
      const client = await bootstrap.started('rootedapp');

      // The context root resolves against the test page rather than against a
      // service URL, so it is the page's own directory.
      const anchor = document.createElement('a');
      anchor.href = './style.css';
      expect(client.resolveUri('context://style.css')).to.equal(anchor.href);
      expect(client.productionMode).to.be.true; // debug: false
    });

    it('waits for the WebComponents polyfill before starting', async () => {
      win.WebComponents = { ready: false };
      const bootstrap = installBootstrapScript(bootstrapConfiguration());
      // The listener the bootstrap registers is anonymous and permanent, so it is
      // captured as it is added and removed afterwards: this page outlives the
      // case, and a later dispatch would start the application again.
      let listener: EventListenerOrEventListenerObject | null = null;
      const originalAddEventListener = window.addEventListener;
      window.addEventListener = function observed(this: Window, type: string, ...rest: unknown[]) {
        if (type === 'WebComponentsReady') {
          listener = rest[0] as EventListenerOrEventListenerObject;
        }
        return (originalAddEventListener as (...args: unknown[]) => void).call(this, type, ...rest);
      } as typeof window.addEventListener;

      try {
        startApplication('deferredapp');
        // The start is deferred, so nothing has read the configuration yet.
        await new Promise((resolve) => {
          setTimeout(resolve, 0);
        });
        expect(bootstrap.lookedUp).to.deep.equal([]);

        win.WebComponents = { ready: true };
        window.dispatchEvent(new Event('WebComponentsReady'));
        const client = await bootstrap.started('deferredapp');
        expect(client.getUIId()).to.equal(7);
      } finally {
        window.addEventListener = originalAddEventListener;
        if (listener !== null) {
          window.removeEventListener('WebComponentsReady', listener);
        }
      }
    });

    it('starts immediately once the WebComponents polyfill is ready', async () => {
      win.WebComponents = { ready: true };
      const bootstrap = installBootstrapScript(bootstrapConfiguration());

      startApplication('readyapp');
      const client = await bootstrap.started('readyapp');
      expect(client.getUIId()).to.equal(7);
    });
  });
});
