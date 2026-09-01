// Every case here is beyond the Java suite: com.vaadin.client.bootstrap.Bootstrapper
// has no test class of its own. The one Java-side case that drives it,
// GwtApplicationConnectionTest.test_should_not_addNavigationEvents_forWebComponents,
// is ported in ApplicationConnectionTests, next to the class it is written against.

import { expect } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../../../../main/frontend/internal/client/ApplicationConfiguration';
import {
  deferStartApplication,
  onModuleLoad,
  populateApplicationConfiguration,
  registerCallback,
  startApplication,
  startApplicationImmediately
} from '../../../../../main/frontend/internal/client/bootstrap/Bootstrapper';
import type { ConfigObject } from '../../../../../main/frontend/internal/client/bootstrap/JsoConfiguration';

// The bootstrap configuration object the server writes into the page: the values
// are read through the ported JsoConfiguration accessors, as in production.
function makeJso(values: Record<string, unknown>): ConfigObject {
  return {
    getConfig: (name: string) =>
      name === 'versionInfo'
        ? { vaadinVersion: values.vaadinVersion, atmosphereVersion: values.atmosphereVersion }
        : values[name]
  };
}

describe('Bootstrapper', () => {
  const win = window as unknown as { WebComponents?: unknown; Vaadin?: unknown };

  it('startApplicationImmediately is true with no WebComponents polyfill', () => {
    const saved = win.WebComponents;
    try {
      win.WebComponents = undefined;
      expect(startApplicationImmediately()).to.be.true;
      win.WebComponents = { ready: false };
      expect(startApplicationImmediately()).to.be.false;
      win.WebComponents = { ready: true };
      expect(startApplicationImmediately()).to.be.true;
    } finally {
      win.WebComponents = saved;
    }
  });

  it('deferStartApplication waits for WebComponentsReady before starting', () => {
    // Java defers the start itself rather than taking a callback, so what is
    // observable here is the listener it registers, not a flag it sets.
    const registered: string[] = [];
    const original = window.addEventListener;
    window.addEventListener = function observed(this: Window, type: string, ...rest: unknown[]) {
      registered.push(type);
      return (original as (...args: unknown[]) => void).call(this, type, ...rest);
    } as typeof window.addEventListener;
    try {
      deferStartApplication('app-1');
      expect(registered).to.contain('WebComponentsReady');
    } finally {
      window.addEventListener = original;
    }
  });

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

  describe('populateApplicationConfiguration', () => {
    const sessionExpiredError = { caption: 'Session Expired', message: 'Take note of any unsaved data' };
    // Java unboxes these three, so a configuration without them is a bootstrap
    // error rather than a defaulted value; every case supplies them.
    const requiredIntegers = { 'v-uiId': 1, heartbeatInterval: 300, maxMessageSuspendTimeout: 5000 };

    it('fills the configuration from the bootstrap JSO (with explicit service URL)', () => {
      const conf = new ApplicationConfiguration();
      populateApplicationConfiguration(
        conf,
        makeJso({
          serviceUrl: 'http://host/app/',
          contextRootUrl: '../',
          webComponentMode: true,
          'v-uiId': 7,
          heartbeatInterval: 300,
          maxMessageSuspendTimeout: 5000,
          vaadinVersion: '24.9',
          atmosphereVersion: '2.4.0',
          sessExpMsg: sessionExpiredError,
          debug: true,
          requestTiming: true,
          webcomponents: ['my-el'],
          devToolsEnabled: true,
          liveReloadUrl: 'http://host/live',
          liveReloadBackend: 'SPRING_BOOT_DEVTOOLS',
          springBootLiveReloadPort: '35729'
        })
      );
      expect(conf.getServiceUrl()).to.equal('http://host/app/');
      expect(conf.getContextRootUrl()).to.equal('http://host/'); // resolved http://host/app/../
      expect(conf.isWebComponentMode()).to.be.true;
      expect(conf.getUIId()).to.equal(7);
      expect(conf.getHeartbeatInterval()).to.equal(300);
      expect(conf.getMaxMessageSuspendTimeout()).to.equal(5000);
      expect(conf.getServletVersion()).to.equal('24.9');
      expect(conf.getAtmosphereVersion()).to.equal('2.4.0');
      // The Atmosphere JS version is read off the loaded push library, not the
      // configuration, so it is empty until vaadinPush.js has loaded.
      expect(conf.getAtmosphereJSVersion()).to.equal('');
      expect(conf.getSessionExpiredError()).to.equal(sessionExpiredError);
      expect(conf.isProductionMode()).to.be.false; // debug=true -> not production
      expect(conf.isRequestTiming()).to.be.true;
      expect(conf.getExportedWebComponents()).to.deep.equal(['my-el']);
      expect(conf.isDevToolsEnabled()).to.be.true;
      expect(conf.getLiveReloadUrl()).to.equal('http://host/live');
      expect(conf.getLiveReloadBackend()).to.equal('SPRING_BOOT_DEVTOOLS');
      expect(conf.getSpringBootLiveReloadPort()).to.equal('35729');
    });

    it('leaves the live-reload strings empty when the bootstrap omits them', () => {
      // Java stores them as null; the ported configuration takes strings, so the
      // bootstrap maps a missing value to the empty string.
      const conf = new ApplicationConfiguration();
      populateApplicationConfiguration(conf, makeJso({ ...requiredIntegers, contextRootUrl: './' }));
      expect(conf.getLiveReloadUrl()).to.equal('');
      expect(conf.getLiveReloadBackend()).to.equal('');
      expect(conf.getSpringBootLiveReloadPort()).to.equal('');
      expect(conf.getSessionExpiredError()).to.be.null;
      expect(conf.isDevToolsEnabled()).to.be.false;
      expect(conf.isRequestTiming()).to.be.false;
    });

    it('fails when the bootstrap omits an integer Java unboxes', () => {
      // Beyond the Java suite, which cannot observe an NPE from the client.
      expect(() =>
        populateApplicationConfiguration(new ApplicationConfiguration(), makeJso({ contextRootUrl: './' }))
      ).to.throw('v-uiId');
    });

    it('falls back to the current location when no service URL is configured', () => {
      const conf = new ApplicationConfiguration();
      populateApplicationConfiguration(conf, makeJso({ ...requiredIntegers, contextRootUrl: '.', debug: false }));
      // serviceUrl resolves "." against the test page; just assert it is absolute.
      expect(conf.getServiceUrl()).to.match(/^https?:\/\//);
      expect(conf.isProductionMode()).to.be.true; // debug=false -> production
    });
  });
});
