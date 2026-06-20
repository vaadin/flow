import { expect } from '@open-wc/testing';
import { init } from '../../main/frontend/FlowClient';
import { jsInteropProbe } from '../../main/frontend/internal/gwtExports';

const $wnd = window as any;

describe('GWT JsInterop exports', () => {
  before(() => {
    // Loading and running the GWT bundle (init()) registers the JsInterop
    // exports as a side effect. The Bootstrapper entry point otherwise tries to
    // start an application: it bails unless window.Vaadin.Flow exists and calls
    // registerWidgetset. Provide a no-op so only the exports get registered,
    // without starting a real application.
    $wnd.Vaadin = $wnd.Vaadin || {};
    $wnd.Vaadin.Flow = $wnd.Vaadin.Flow || {};
    if (!$wnd.Vaadin.Flow.registerWidgetset) {
      $wnd.Vaadin.Flow.registerWidgetset = () => {};
    }
    init();
  });

  it('publishes exported classes under window.Vaadin.Flow.internal', () => {
    expect($wnd.Vaadin.Flow.internal).to.not.be.undefined;
    expect($wnd.Vaadin.Flow.internal.JsInteropProbe).to.not.be.undefined;
  });

  it('exported static methods are callable from TypeScript', () => {
    expect(jsInteropProbe().echo('ping')).to.equal('flow-client:ping');
  });
});
