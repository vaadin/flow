import { expect } from '@open-wc/testing';

// API to test
import { ConnectionState, ConnectionStateStore } from '../../main/frontend/ConnectionState';
import { Flow, type NavigationParameters } from '../../main/frontend/Flow';
// Intern does not serve webpack chunks, adding deps here in order to
// produce one chunk, because dynamic imports in Flow.ts  will not work.
import '../../main/frontend/FlowBootstrap';
import '../../main/frontend/FlowClient';
// Mock XMLHttpRequest so as we don't need flow-server running for tests.
import { type MockXhrServer, newServer } from 'mock-xmlhttprequest';
import sinon from 'sinon';

const $wnd = window as any;
const flowRoot = window.document.body as any;

const stubVaadinPushSrc = '/src/test/frontend/stubVaadinPush.js';
let server: MockXhrServer;
// The event that stubServerRemoteFunction fails with an "Internal error", which stops the UI
let failingEvent: string | undefined;
// A `changes` array that adds a div with 'Foo' text to body
const changesResponse = `[
  {
    "node":1,
    "type":"put",
    "key":"tag",
    "feat":0,
    "value":"body"
  },
  {
    "node":1,
    "type":"splice",
    "feat":2,
    "index":0,
    "addNodes":[
      2
    ]
  },
  {
    "node":2,
    "type":"attach"
  },
  {
    "node":2,
    "type":"put",
    "key":"tag",
    "feat":0,
    "value":"div"
  },
  {
    "node":2,
    "type":"splice",
    "feat":2,
    "index":0,
    "addNodes":[
      3
    ]
  },
  {
    "node":3,
    "type":"attach"
  },
  {
    "node":3,
    "type":"put",
    "key":"text",
    "feat":7,
    "value":"Foo"
  }
]`;

function createInitResponse(appId: string, changes = '[]', pushScript?: string): string {
  return `
      {
        "appConfig": {
          "heartbeatInterval" : 300,
          "maxMessageSuspendTimeout": 5000,
          "contextRootUrl" : "../",
          "debug" : true,
          "v-uiId" : 0,
          "serviceUrl" : "//localhost:8080/flow/",
          "productionMode": false,
          "appId": "${appId}",
          "uidl": {
            "syncId": 0,
            "clientId": 0,
            "timings": [],
            "Vaadin-Security-Key": "119a6005-e663-4a4c-a882-bbfa8bd0c304",
            "Vaadin-Push-ID": "4b915ffb-4e0a-484c-9995-09500fe9fa3a",
            "changes": ${changes}
          }
        }
        ${pushScript !== undefined ? `, "pushScript": "${pushScript}"` : ''}
      }
    `;
}

describe('Flow', () => {
  before(() => {
    // keep track of all event listeners added by Flow client to window for removal between tests
    $wnd.originalAddEventListener = $wnd.addEventListener;
  });

  after(() => {
    $wnd.addEventListener = $wnd.originalAddEventListener;
  });

  let listeners: Array<{ type: string; listener: any }> = [];

  beforeEach(() => {
    delete $wnd.Vaadin;
    $wnd.Vaadin = {
      connectionState: new ConnectionStateStore(ConnectionState.CONNECTED)
    };
    const indicator = $wnd.document.body.querySelector('vaadin-connection-indicator');
    if (indicator) {
      indicator.remove();
    }
    Array.from(document.body.children)
      .filter((e) => e.tagName.toLowerCase().startsWith('flow-container'))
      .forEach((e) => e.remove());

    $wnd.addEventListener = (type, listener) => {
      listeners.push({ type: type, listener: listener });
      $wnd.originalAddEventListener(type, listener);
    };

    server = newServer({});
    server.install();
  });

  afterEach(() => {
    // Every spy and stub below is created on the default sandbox, so one
    // restore covers them all and keeps a case that throws mid-way from
    // leaking a replaced function (e.g. console.error) into later cases.
    sinon.restore();
    server.remove();
    failingEvent = undefined;
    delete $wnd.Vaadin;
    delete flowRoot.$;
    if (flowRoot.timers) {
      // clear timers started in stubServerRemoteFunction
      flowRoot.timers.forEach(clearTimeout);
      delete flowRoot.timers;
    }
    listeners.forEach((recorded: any) => {
      $wnd.removeEventListener(recorded.type, recorded.listener);
    });
    listeners = [];
  });

  it('should accept a configuration object', () => {
    const flow = new Flow({ imports: () => {} });
    expect(flow.config).not.to.be.undefined;
    expect(flow.config.imports).not.to.be.undefined;
  });

  it('should initialize window.Flow object', () => {
    new Flow({ imports: () => {} });

    expect($wnd.Vaadin).not.to.be.undefined;
    expect($wnd.Vaadin.Flow).not.to.be.undefined;
  });

  it('should initialize a flow loading indicator', async () => {
    new Flow({ imports: () => {} });
    await $wnd.Vaadin.connectionIndicator.updateComplete;
    expect($wnd.Vaadin.connectionIndicator.isConnected).to.be.true;
    const indicator = $wnd.document.querySelector('.v-loading-indicator') as HTMLElement;
    const styles = $wnd.document.querySelector('style#css-loading-indicator') as HTMLElement;
    expect(indicator).not.to.be.null;
    expect(styles).not.to.be.null;
    expect(indicator.getAttribute('style')).to.equal('display: none');
  });

  it('should throw when an incorrect server response is received', () => {
    // Configure an invalid server response
    server.addHandler('GET', /^.*\?v-r=init&location=.*/, (req) => {
      expect('GET').to.equal(req.method);
      return req.respond(500, {}, `Unexpected Server Error`);
    });

    return (new Flow() as any)
      .flowInit(true)
      .then(() => {
        throw new Error('Should not happen');
      })
      .catch((error) => {
        expect(error.toString()).to.match(/500/);
      });
  });

  it('should connect client and server on route action', async () => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111');

    const flow = new Flow();
    // Check that the Flow puts a client object for TypeScript
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive).not.to.be.undefined;
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;

    const route = flow.serverSideRoutes[0];

    sinon.spy(flow, 'loadingStarted');
    sinon.spy(flow, 'loadingFinished');

    return route.action({ pathname: 'Foo/Bar.baz', search: '' }).then(() => {
      // Check that flowInit() was called
      if (!flow.response) {
        expect.fail('Response should be defined');
      }

      expect(flow.response.appConfig).not.to.be.undefined;
      // Check that bootstrap was initialized
      expect($wnd.Vaadin.Flow.initApplication).not.to.be.undefined;
      expect($wnd.Vaadin.Flow.registerWidgetset).not.to.be.undefined;
      // Check that flowClient was initialized
      expect($wnd.Vaadin.Flow.clients.foobar.resolveUri).not.to.be.undefined;
      expect($wnd.Vaadin.Flow.clients.foobar.isActive()).to.be.false;

      // Check that pushScript is not initialized
      expect($wnd.vaadinPush).to.be.undefined;

      // Assert that element was created amd put in flowRoot so as server can find it
      expect(flowRoot.$).not.to.be.undefined;
      expect(flowRoot.$['foobar-1111111']).not.to.be.undefined;

      // Check that `loadingStarted` and `loadingFinished` pair was called
      sinon.assert.calledOnce(flow.loadingStarted);
      sinon.assert.calledOnce(flow.loadingFinished);

      // Check that `isActive` flag is set to false after the action
      expect($wnd.Vaadin.Flow.clients.foobar.isActive()).to.be.false;
    });
  });

  it('loadingStarted and loadingFinished should update isActive and connection indicator', async () => {
    const flow = new Flow();
    sinon.spy($wnd.Vaadin.connectionState, 'loadingStarted');
    sinon.spy($wnd.Vaadin.connectionState, 'loadingFinished');

    flow.loadingStarted();
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.true;
    sinon.assert.calledOnce($wnd.Vaadin.connectionState.loadingStarted);
    sinon.assert.notCalled($wnd.Vaadin.connectionState.loadingFinished);

    flow.loadingFinished();
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;
    sinon.assert.calledOnce($wnd.Vaadin.connectionState.loadingStarted);
    sinon.assert.calledOnce($wnd.Vaadin.connectionState.loadingFinished);
  });

  it('should remove context-path in request', () => {
    stubServerRemoteFunction('foobar-1111111', false, new RegExp('^Foo/Bar.baz$'));
    mockInitResponse('foobar-1111111');

    const flow = new Flow();
    flow['baseRegex'] = /^\/foo\//;
    const route = flow.serverSideRoutes[0];

    return route.action({ pathname: '/foo/Foo/Bar.baz' }).then(() => {
      if (!flow.response) {
        expect.fail('Response should be defined');
      }
    });
  });

  it('should bind Flow serverSideRoutes function to the flow context', () => {
    // A mock class for router
    class TestRouter {
      routes: [];
    }

    stubServerRemoteFunction('ROOT-12345');
    mockInitResponse('ROOT-12345');

    const router = new TestRouter();
    router.routes = new Flow().serverSideRoutes;

    return router.routes[0].action({ pathname: 'another-route' }).then((elem) => {
      expect(elem).not.to.be.undefined;
    });
  });

  it('should reuse container element in flow navigation', () => {
    stubServerRemoteFunction('ROOT-12345');
    mockInitResponse('ROOT-12345');

    const route = new Flow().serverSideRoutes[0];

    return route.action({ pathname: 'Foo' }).then((e1) => {
      return route.action({ pathname: 'Bar' }).then((e2) => {
        expect(1).to.equal(Object.keys(flowRoot.$).length);
        expect(e1).to.equal(e2);
        expect(e1.id).to.equal(e2.id);
      });
    });
  });

  it('navigation should be delayed to onBeforeEnter when using router API', () => {
    stubServerRemoteFunction('foobar-12345');
    mockInitResponse('foobar-12345');

    const route = new Flow().serverSideRoutes[0];

    return route.action({ pathname: 'Foo/Bar.baz' }).then(async (elem) => {
      // Check that flowInit() was called
      expect($wnd.Vaadin.Flow.clients.foobar.resolveUri).not.to.be.undefined;
      // Assert that flowRoot namespace was created
      expect(flowRoot.$).not.to.be.undefined;
      // Assert that container was created and put in the flowRoot
      expect(flowRoot.$['foobar-12345']).not.to.be.undefined;

      // Assert server side has not put anything in the container
      expect(0).to.equal(elem.children.length);

      // When using router API, it should expose the onBeforeEnter handler
      expect(elem.onBeforeEnter).not.to.be.undefined;

      // after action TB isActive flag should be false
      expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;

      // Store `isActive` flag when the onBeforeEnter is being executed
      let wasActive = false;
      setTimeout(() => (wasActive = wasActive || $wnd.Vaadin.Flow.clients.TypeScript.isActive()), 5);
      // @ts-ignore
      await elem.onBeforeEnter({ pathname: 'Foo/Bar.baz' }, {});
      // TB should be informed when the server call was in progress and when it is finished
      expect(wasActive).to.be.true;
      expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;

      // Assert server side has put content in the container
      expect(1).to.equal(elem.children.length);
    });
  });

  it('should be possible to cancel navigation when using router onBeforeEnter API', () => {
    // true means that server will prevent navigation
    stubServerRemoteFunction('foobar-12345', true);

    mockInitResponse('foobar-12345');

    const route = new Flow().serverSideRoutes[0];

    return route.action({ pathname: 'Foo/Bar.baz' }).then((elem) => {
      // Check that flowInit() was called
      expect($wnd.Vaadin.Flow.clients.foobar.resolveUri).not.to.be.undefined;
      // Assert that flowRoot namespace was created
      expect(flowRoot.$).not.to.be.undefined;
      // Assert that container was created and put in the flowRoot
      expect(flowRoot.$['foobar-12345']).not.to.be.undefined;

      // Assert server side has not put anything in the container
      expect(0).to.equal(elem.children.length);

      // When using router API, it should expose the onBeforeEnter handler
      expect(elem.onBeforeEnter).not.to.be.undefined;

      // @ts-ignore
      elem
        .onBeforeEnter(
          { pathname: 'Foo/Bar.baz' },
          {
            prevent: () => {
              return { cancel: true };
            }
          }
        )
        .then((obj) => expect(obj.cancel).to.be.true);
    });
  });

  it('onBeforeLeave should cancel `server->client` navigation', () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', true);
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = flow.serverSideRoutes[0];

    return route.action({ pathname: 'Foo' }).then((elem: any) => {
      expect(elem.onBeforeLeave).not.to.be.undefined;
      expect('Foo').to.equal(flow.pathname);

      return elem
        .onBeforeEnter(
          { pathname: 'Foo' },
          {
            prevent: () => {
              // set cancel to false even though server is cancelling
              return { cancel: false };
            }
          }
        )
        .then((result: any) => {
          // view content was set
          expect(result.cancel).to.be.false;
          expect(1).to.equal(elem.children.length);

          return elem
            .onBeforeLeave(
              { pathname: 'Lorem' },
              {
                prevent: () => {
                  // set cancel to true
                  return { cancel: true };
                }
              }
            )
            .then((result: any) => {
              // Navigation cancelled onBeforeLeave
              expect(result.cancel).to.be.true;
            });
        });
    });
  });

  it('should cancel a server navigation when the UI stops before the server answers it', async () => {
    failingEvent = 'ui-navigate';
    const loadPage = sinon.stub(Flow.prototype as any, 'loadPage');
    stubServerRemoteFunction('foobar-12345');
    mockInitResponse('foobar-12345');
    const elem: any = await new Flow().serverSideRoutes[0].action({ pathname: 'Foo' });
    const cmd = { prevent: () => 'prevented' };

    expect(await settledSoon(elem.onBeforeEnter({ pathname: 'Foo' }, cmd))).to.equal('prevented');
    // The error notification stays on screen
    expect(loadPage.called).to.be.false;
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;
    document.querySelectorAll('.v-system-error').forEach((e) => e.remove());
  });

  it('should keep navigating on the server when only an exported web component on the page stops', async () => {
    stubServerRemoteFunction('foobar-12345');
    mockInitResponse('foobar-12345');
    let embeddedTerminated = false;
    const embedded = {
      // Busy until the main client is published, so that the router waits for it
      isActive: () => !$wnd.Vaadin.Flow.clients.foobar,
      isTerminated: () => embeddedTerminated,
      sendEventMessage: () => {},
      events: new EventTarget()
    };
    const flow = new Flow();
    $wnd.Vaadin.Flow.clients.embedded = embedded;
    const elem: any = await flow.serverSideRoutes[0].action({ pathname: 'Foo' });
    const cmd = { prevent: () => 'prevented' };
    // The view itself is not compared, as chai cannot print it when it differs
    const shown = (result: unknown) => (result === elem ? 'view' : String(result));

    // Stops while the navigation waits for the server
    const navigation = elem.onBeforeEnter({ pathname: 'Foo' }, cmd);
    embeddedTerminated = true;
    embedded.events.dispatchEvent(new CustomEvent('vaadin-ui-terminated'));
    expect(shown(await settledSoon(navigation))).to.equal('view');

    // Has stopped before the next navigation starts
    const leave = { prevent: () => 'prevented', continue: () => 'continued' };
    expect(await settledSoon(elem.onBeforeLeave({ pathname: 'Lorem' }, leave))).to.equal('continued');
  });

  it('should load other routes as a new page when the UI stopped while the client started', async () => {
    const loadPage = sinon.stub(Flow.prototype as any, 'loadPage');
    stubServerRemoteFunction('foobar-12345');
    server.addHandler('GET', /^.*\?v-r=init.*/, (req) => {
      req.respond(
        200,
        { 'content-type': 'application/json' },
        createInitResponse('foobar-12345').replace('"timings": [],', `"timings": [], "meta": ${internalErrorMeta},`)
      );
    });
    const { pathname, search } = window.location;
    const elem: any = await new Flow().serverSideRoutes[0].action({ pathname, search });
    const cmd = { prevent: () => 'prevented' };

    // The initial navigation to the page already loaded is cancelled, as
    // loading it again could stop the same way
    expect(await settledSoon(elem.onBeforeEnter({ pathname, search }, cmd))).to.equal('prevented');
    expect(loadPage.called).to.be.false;
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;

    // Also when the browser already shows the target, as on back and forward
    const navigation = { pathname: '/Foo', search: '?a=1', hash: '#b' };
    window.history.pushState(null, '', '/Foo?a=1#b');
    try {
      expect(await settledSoon(elem.onBeforeEnter(navigation, cmd))).to.equal('prevented');
    } finally {
      window.history.replaceState(null, '', pathname + search);
    }
    expect(loadPage.calledOnceWith('/Foo?a=1#b')).to.be.true;
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;
    document.querySelectorAll('.v-system-error').forEach((e) => e.remove());
  });

  it('should load the page on the app origin for the route paths that Flow.tsx passes when the UI stopped while starting', async () => {
    const loadPage = sinon.stub(Flow.prototype as any, 'loadPage');
    stubServerRemoteFunction('foobar-12345');
    server.addHandler('GET', /^.*\?v-r=init.*/, (req) => {
      req.respond(
        200,
        { 'content-type': 'application/json' },
        createInitResponse('foobar-12345').replace('"timings": [],', `"timings": [], "meta": ${internalErrorMeta},`)
      );
    });
    const { pathname, search } = window.location;
    const elem: any = await new Flow().serverSideRoutes[0].action({ pathname, search });
    const cmd = { prevent: () => 'prevented' };
    // Flow.tsx enters a route with `basename + location.pathname`, and the
    // basename is '/' for an app deployed at the root context path
    const basename = '/';

    expect(await settledSoon(elem.onBeforeEnter({ pathname: basename + pathname, search }, cmd))).to.equal('prevented');
    expect(loadPage.called).to.be.false;

    // '//Foo' as a URL would be the host 'Foo'
    expect(await settledSoon(elem.onBeforeEnter({ pathname: basename + '/Foo', search: '?a=1' }, cmd))).to.equal(
      'prevented'
    );
    expect(loadPage.args).to.deep.equal([['/Foo?a=1']]);
    document.querySelectorAll('.v-system-error').forEach((e) => e.remove());
  });

  it('should load the page under the context path when the UI stopped while starting', async () => {
    const loadPage = sinon.stub(Flow.prototype as any, 'loadPage');
    stubServerRemoteFunction('foobar-12345');
    server.addHandler('GET', /^.*\?v-r=init.*/, (req) => {
      req.respond(
        200,
        { 'content-type': 'application/json' },
        createInitResponse('foobar-12345').replace('"timings": [],', `"timings": [], "meta": ${internalErrorMeta},`)
      );
    });
    const base = document.createElement('base');
    base.href = '/app/';
    document.head.prepend(base);
    try {
      const elem: any = await new Flow().serverSideRoutes[0].action({ pathname: '/app/Bar' });
      // Flow.tsx passes '/app/Foo' for an app at the context path '/app'
      const cmd = { prevent: () => 'prevented' };
      expect(await settledSoon(elem.onBeforeEnter({ pathname: '/app/Foo' }, cmd))).to.equal('prevented');
      // A colon in the first segment does not make it a URL scheme
      expect(await settledSoon(elem.onBeforeEnter({ pathname: '/app/user:42' }, cmd))).to.equal('prevented');
    } finally {
      base.remove();
    }
    expect(loadPage.args).to.deep.equal([['/app/Foo'], ['/app/user:42']]);
    document.querySelectorAll('.v-system-error').forEach((e) => e.remove());
  });

  it('should let a `server->client` navigation leave when the UI stops before the server answers it', async () => {
    failingEvent = 'ui-leave-navigation';
    stubServerRemoteFunction('foobar-12345');
    mockInitResponse('foobar-12345');
    const elem: any = await new Flow().serverSideRoutes[0].action({ pathname: 'Foo' });
    await elem.onBeforeEnter({ pathname: 'Foo' }, {});
    const cmd = { prevent: () => 'prevented', continue: () => 'continued' };

    expect(await settledSoon(elem.onBeforeLeave({ pathname: 'Lorem' }, cmd))).to.equal('continued');
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;

    // A later navigation leaves without waiting for the stopped UI
    expect(await settledSoon(elem.onBeforeLeave({ pathname: 'Ipsum' }, cmd))).to.deep.equal({});
    expect($wnd.Vaadin.Flow.clients.TypeScript.isActive()).to.be.false;
    document.querySelectorAll('.v-system-error').forEach((e) => e.remove());
  });

  it('onBeforeEnter should handle forwardTo `server->client` navigation', () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', false, undefined, { pathname: 'Lorem', search: '' });
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = flow.serverSideRoutes[0];

    return route.action({ pathname: 'Foo' }).then((elem: any) => {
      return elem
        .onBeforeEnter(
          { pathname: 'Foo' },
          {
            redirect: (context: any) => {
              return { redirectContext: context };
            }
          }
        )
        .then((result: any) => {
          // Navigate to expect destination
          expect('Lorem').to.equal(result.redirectContext);
        });
    });
  });

  it('onBeforeLeave should not cause double round-trip on `server->server` navigation', () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', true);
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = new Flow().serverSideRoutes[0];

    return route.action({ pathname: 'Foo' }).then((elem: any) => {
      return elem
        .onBeforeEnter(
          { pathname: 'Foo' },
          {
            prevent: () => {
              // set cancel to false even though server is cancelling
              return { cancel: false };
            }
          }
        )
        .then(() => {
          return elem
            .onBeforeLeave(
              { pathname: 'Foo' },
              {
                prevent: () => {
                  // set cancel to true
                  return { cancel: true };
                }
              }
            )
            .then((result: any) => {
              // since server call is skipped, prevent() above is not executed
              // checking that cancel was not set demonstrates that there
              // were no double round-trip
              expect(result.cancel).to.be.undefined;
            });
        });
    });
  });

  it('should load pushScript on init', async () => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111', undefined, stubVaadinPushSrc);

    const flow = new Flow();

    const route = flow.serverSideRoutes[0];
    await route.action({ pathname: 'Foo/Bar.baz' });

    expect($wnd.vaadinPush).not.to.be.undefined;
    expect($wnd.vaadinPush.isStub).to.be.true;
  });

  it('should load pushScript on route action', async () => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111', undefined, stubVaadinPushSrc);

    const flow = new Flow();

    const route = flow.serverSideRoutes[0];
    await route.action({ pathname: 'Foo/Bar.baz', search: '' });

    expect($wnd.vaadinPush).not.to.be.undefined;
    expect($wnd.vaadinPush.isStub).to.be.true;
  });

  it('should not throw error when response header content type has charset', async () => {
    stubServerRemoteFunction('foobar-1111112');
    mockInitResponse('foobar-1111113', undefined, stubVaadinPushSrc, true);
    const flow = new Flow();
    const route = flow.serverSideRoutes[0];
    await route.action({ pathname: 'Foo/Bar.baz', search: '' });
  });

  it('should not throw error when response header content type has no charset', async () => {
    stubServerRemoteFunction('foobar-1111113');
    mockInitResponse('foobar-1111113', undefined, stubVaadinPushSrc);
    const flow = new Flow();
    const route = flow.serverSideRoutes[0];
    await route.action({ pathname: 'Foo/Bar.baz', search: '' });
  });

  it('should show stub when navigating to server view offline', async () => {
    stubServerRemoteFunction('foobar-123');
    $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTION_LOST;
    const consoleError = sinon.stub(console, 'error');
    const flow = new Flow();
    const route = flow.serverSideRoutes[0];
    const params: NavigationParameters = {
      pathname: 'Foo/Bar.baz',
      search: ''
    };
    const view = await route.action(params);
    expect(view.localName).to.equal('iframe');
    expect(view.getAttribute('src')).to.equal('./offline-stub.html');
    // Nothing was attempted, so there is nothing to report. Staying silent
    // here is what makes the log of the initialization failure below a
    // reliable way to tell the two routes to the stub apart.
    sinon.assert.notCalled(consoleError);

    // @ts-ignore
    let onBeforeEnterReturns = view.onBeforeEnter(params, {});
    expect(onBeforeEnterReturns).to.equal(undefined);

    // @ts-ignore
    let onBeforeLeaveReturns = view.onBeforeLeave(params, {});
    expect(onBeforeLeaveReturns).to.equal(undefined);
  });

  [
    { description: 'without a content type', headers: {}, body: 'unable to connect' },
    {
      description: 'with a JSON body',
      headers: { 'content-type': 'application/json' },
      body: '{"status":500,"error":"Internal Server Error","path":"/"}'
    }
  ].forEach(({ description, headers, body }) => {
    it(`should show stub when navigating to server view and Flow initialization fails with an HTTP error ${description}`, async () => {
      server.addHandler('GET', /^.*\?v-r=init.*/, (req) => {
        req.respond(500, headers, body);
      });
      const flow = new Flow();
      const route = flow.serverSideRoutes[0];
      const params: NavigationParameters = {
        pathname: 'Foo/Bar.baz',
        search: ''
      };

      await $wnd.Vaadin.connectionIndicator.updateComplete;
      const indicator = $wnd.document.querySelector('.v-loading-indicator');

      const consoleError = sinon.stub(console, 'error');
      const view = await route.action(params);
      expect(view).not.to.be.null;
      expect(view.localName).to.equal('iframe');
      expect(view.getAttribute('src')).to.equal('./offline-stub.html');

      expect(indicator.getAttribute('style')).to.equal('display: none');

      // The stub replaces the view without a trace of why, so the failure has
      // to reach the console: the reported error carries the status and body of
      // the failed init request, which is the only clue to what went wrong.
      sinon.assert.calledOnce(consoleError);
      const reportedError = consoleError.getCall(0).args[1];
      expect(reportedError).to.be.instanceOf(Error);
      expect(reportedError.message).to.contain('500');
      expect(reportedError.message).to.contain(body);

      // @ts-ignore
      let onBeforeEnterReturns = view.onBeforeEnter(params, {});
      expect(onBeforeEnterReturns).to.equal(undefined);

      // @ts-ignore
      let onBeforeLeaveReturns = view.onBeforeLeave(params, {});
      expect(onBeforeLeaveReturns).to.deep.equal(undefined);
    });
  });

  it('should retry navigation when back online', async () => {
    stubServerRemoteFunction('foobar-123');
    $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTION_LOST;
    const flow = new Flow();
    const route = flow.serverSideRoutes[0];
    const params: NavigationParameters = {
      pathname: 'Foo/Bar.baz',
      search: ''
    };
    const clientSideRouter = { render: sinon.spy() };
    const view = await route.action(params);
    await view.onBeforeEnter(params, {}, clientSideRouter);

    $wnd.Vaadin.connectionState.state = ConnectionState.CONNECTED;
    sinon.assert.calledOnce(clientSideRouter.render);
    sinon.assert.calledWithExactly(clientSideRouter.render.getCall(0), params, false);
  });

  it("when no Flow client loaded, should transition to CONNECTED when receiving 'offline' and then 'online' events and connection is reestablished", async () => {
    server.addHandler('HEAD', /^.*sw.js/, (req) => {
      req.respond(200);
    });
    new Flow();
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTED);
    $wnd.dispatchEvent(new Event('offline')); // caught by Flow.ts
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTION_LOST);
    $wnd.dispatchEvent(new Event('online')); // caught by Flow.ts
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.RECONNECTING);
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTED);
  });

  it("when no Flow client loaded, should transition to CONNECTION_LOST when receiving 'offline' and then 'online' events and connection is not reestablished", async () => {
    server.addHandler('HEAD', /^.*sw.js/, (req) => {
      req.setNetworkError();
    });

    new Flow();
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTED);
    $wnd.dispatchEvent(new Event('offline')); // caught by Flow.ts
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTION_LOST);
    $wnd.dispatchEvent(new Event('online')); // caught by Flow.ts
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.RECONNECTING);
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTION_LOST);
  });

  it("when Flow client loaded, should transition to RECONNECTING on receiving 'offline' and then 'online' events", async () => {
    stubServerRemoteFunction('FooBar-12345');
    mockInitResponse('FooBar-12345', undefined, stubVaadinPushSrc);
    const flow = new Flow();
    await (flow as any).flowInit(true);
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTED);
    $wnd.dispatchEvent(new Event('offline')); // caught by DefaultConnectionStateHandler
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.CONNECTION_LOST);
    $wnd.dispatchEvent(new Event('online')); // caught by DefaultConnectionStateHandler
    expect($wnd.Vaadin.connectionState.state).to.equal(ConnectionState.RECONNECTING);
  });

  it('should pre-attach container element on every navigation', async () => {
    stubServerRemoteFunction('foobar-12345');
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = flow.serverSideRoutes[0];

    const flowRouteParams = { pathname: 'Foo', search: '' };
    const otherRouteParams = { pathname: 'Lorem', search: '' };

    // Initial navigation
    const container = await route.action(flowRouteParams);
    expect(container.isConnected).to.be.true;
    expect(container.style.display).to.equal('none');

    // @ts-ignore
    await container.onBeforeEnter(flowRouteParams, {});
    expect(container.isConnected).to.be.true;
    expect(container.style.display).to.equal('');

    // Leave

    // @ts-ignore
    await container.onBeforeLeave(otherRouteParams, {});
    // The router detaches the container, possibly because it renders a client-side view
    container.parentNode!.removeChild(container);

    await route.action(flowRouteParams);
    expect(container.isConnected).to.be.true;
    expect(container.style.display).to.equal('none');

    // @ts-ignore
    await container.onBeforeEnter(flowRouteParams, {});
    expect(container.isConnected).to.be.true;
    expect(container.style.display).to.equal('');
  });
});

// The meta of an "Internal error" reply, which stops the UI
const internalErrorMeta =
  '{"appError":{"caption":"Internal error","url":null,"message":"Please notify the administrator.","details":null,"querySelector":null}}';

// What the promise resolves to, or 'pending' if it does not settle soon
function settledSoon<T>(promise: Promise<T>): Promise<T | 'pending'> {
  return Promise.race([promise, new Promise<'pending'>((resolve) => setTimeout(() => resolve('pending'), 500))]);
}

function stubServerRemoteFunction(
  id: string,
  cancel: boolean = false,
  routeRegex?: RegExp,
  url?: NavigationParameters
) {
  let container: any;

  flowRoot.timers = [];
  const handlers = {
    connectClient: (route: string) => {
      expect(route).not.to.be.undefined;
      if (routeRegex) {
        expect(route).to.match(routeRegex);
      }

      container = flowRoot.$[id];

      expect(container).not.to.be.undefined;
      expect(container.serverConnected).not.to.be.undefined;

      // When appending elements container should be attached and hidden
      expect(container.isConnected).to.be.true;
      expect('none').to.equal(container.style.display);

      container.appendChild(document.createElement('div'));

      // asynchronously resolve the remote server call
      const timer = setTimeout(() => {
        container.serverConnected(cancel, url);
        // container should be visible when not cancelled or not has redirect server-client
        expect(cancel || url ? 'none' : '').to.equal(container.style.display);
      }, 10);
      flowRoot.timers.push(timer);
    },
    leaveNavigation: () => {
      // asynchronously resolve the promise
      const timer = setTimeout(() => container.serverConnected(cancel, url), 10);
      flowRoot.timers.push(timer);
    }
  };

  flowRoot.timers = [];
  server.addHandler('POST', /^.*\?v-r=uidl.*/, (req) => {
    const payload = JSON.parse(req.body);
    if (payload.rpc && payload.rpc[0].type === 'event') {
      const rpc = payload.rpc[0];
      if (rpc.event === failingEvent) {
        // Like VaadinService.handleExceptionDuringRequest: no client id
        req.respond(
          200,
          { 'content-type': 'application/json' },
          `{"changes":{},"resources":{},"locales":{},"meta":${internalErrorMeta},"syncId":-1}`
        );
        return;
      } else if (rpc.event === 'ui-navigate') {
        handlers.connectClient(rpc.data.route);
      } else if (rpc.event === 'ui-leave-navigation') {
        handlers.leaveNavigation();
      }
    }
    req.respond(
      200,
      { 'content-type': 'application/json' },
      '{"syncId":' + (payload['syncId'] + 1) + ',"clientId":' + (payload['clientId'] + 1) + '}'
    );
  });
}

function mockInitResponse(appId: string, changes = '[]', pushScript?: string, withCharset?: boolean) {
  // Configure a valid server initialization response

  server.addHandler('GET', /^.*\?v-r=init.*/, (req) => {
    req.respond(
      200,
      { 'content-type': 'application/json' + (withCharset ? ';charset=ISO-8859-1' : '') },
      createInitResponse(appId, changes, pushScript)
    );
  });
}
