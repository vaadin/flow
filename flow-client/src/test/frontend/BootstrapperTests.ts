import { expect } from '@open-wc/testing';
import {
  deferStartApplication,
  registerCallback,
  startApplicationImmediately
} from '../../main/frontend/internal/Bootstrapper';

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
});
