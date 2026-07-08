import { expect } from '@open-wc/testing';
import { ApplicationConfiguration } from '../../main/frontend/internal/ApplicationConfiguration';
import {
  deferStartApplication,
  type JsoConfiguration,
  populateApplicationConfiguration,
  registerCallback,
  startApplicationImmediately
} from '../../main/frontend/internal/Bootstrapper';

// A JsoConfiguration backed by a plain values map.
function makeJso(values: Record<string, unknown>): JsoConfiguration {
  return {
    getConfigString: (name: string) => (values[name] === undefined ? null : (values[name] as string)),
    getConfigBoolean: (name: string) => !!values[name],
    getConfigInteger: (name: string) => (values[name] as number) ?? 0,
    getConfigStringArray: (name: string) => (values[name] as string[]) ?? [],
    getConfigError: (name: string) => values[name] ?? null,
    getVaadinVersion: () => (values.vaadinVersion as string) ?? '',
    getAtmosphereVersion: () => (values.atmosphereVersion as string) ?? '',
    getAtmosphereJSVersion: () => (values.atmosphereJSVersion as string) ?? ''
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

  it('deferStartApplication runs the callback on WebComponentsReady', () => {
    let ran = false;
    deferStartApplication(() => {
      ran = true;
    });
    expect(ran).to.be.false;
    window.dispatchEvent(new Event('WebComponentsReady'));
    expect(ran).to.be.true;
  });

  it('registerCallback forwards the widgetset name and callback to registerWidgetset', () => {
    const saved = win.Vaadin;
    try {
      const calls: Array<[string, (id: string) => void]> = [];
      win.Vaadin = { Flow: { registerWidgetset: (name: string, cb: (id: string) => void) => calls.push([name, cb]) } };
      const callback = (): void => {};
      registerCallback('com.example.Widgetset', callback);
      expect(calls).to.have.length(1);
      expect(calls[0][0]).to.equal('com.example.Widgetset');
      expect(calls[0][1]).to.equal(callback);
    } finally {
      win.Vaadin = saved;
    }
  });

  describe('populateApplicationConfiguration', () => {
    it('fills the configuration from the bootstrap JSO (with explicit service URL)', () => {
      const conf = new ApplicationConfiguration();
      populateApplicationConfiguration(
        conf,
        makeJso({
          serviceUrl: 'http://host/app/',
          contextRootUrl: '../',
          webComponentMode: false,
          'v-uiId': 7,
          heartbeatInterval: 300,
          maxMessageSuspendTimeout: 5000,
          vaadinVersion: '24.9',
          debug: true,
          webcomponents: ['my-el']
        })
      );
      expect(conf.getServiceUrl()).to.equal('http://host/app/');
      expect(conf.getContextRootUrl()).to.equal('http://host/'); // resolved http://host/app/../
      expect(conf.getUIId()).to.equal(7);
      expect(conf.getHeartbeatInterval()).to.equal(300);
      expect(conf.getMaxMessageSuspendTimeout()).to.equal(5000);
      expect(conf.getServletVersion()).to.equal('24.9');
      expect(conf.isProductionMode()).to.be.false; // debug=true -> not production
      expect(conf.getExportedWebComponents()).to.deep.equal(['my-el']);
    });

    it('falls back to the current location when no service URL is configured', () => {
      const conf = new ApplicationConfiguration();
      populateApplicationConfiguration(conf, makeJso({ contextRootUrl: '.', 'v-uiId': 1, debug: false }));
      // serviceUrl resolves "." against the test page; just assert it is absolute.
      expect(conf.getServiceUrl()).to.match(/^https?:\/\//);
      expect(conf.isProductionMode()).to.be.true; // debug=false -> production
    });
  });
});
