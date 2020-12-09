const { suite, test, beforeEach, afterEach } = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import {Flow, NavigationParameters} from "../../main/resources/META-INF/resources/frontend/Flow";
// Intern does not serve webpack chunks, adding deps here in order to
// produce one chunk, because dynamic imports in Flow.ts  will not work.
import "../../main/resources/META-INF/resources/frontend/FlowBootstrap";
import "../../main/resources/META-INF/resources/frontend/FlowClient";
// Mock XMLHttpRequest so as we don't need flow-server running for tests.
import mock from 'xhr-mock';

const $wnd = window as any;
const flowRoot = window.document.body as any;

const stubVaadinPushSrc = '/src/test/frontend/stubVaadinPush.js';

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
          "clientRouting" : false,
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
};

suite("Flow", () => {

  beforeEach(() => {
    delete $wnd.Vaadin;
    mock.setup();
    const indicator = $wnd.document.body.querySelector('.v-loading-indicator');
    if (indicator) {
      $wnd.document.body.removeChild(indicator);
    }
  });

  afterEach(() => {
    mock.teardown();
    delete $wnd.Vaadin;
    delete flowRoot.$;
    delete flowRoot.$server;
  });

  test("should accept a configuration object", () => {
    assert.isUndefined($wnd.Vaadin);
    const flow = new Flow({imports: () => {}});
    assert.isDefined(flow.config);
    assert.isDefined(flow.config.imports);
  });

  test("should initialize window.Flow object", () => {
    assert.isUndefined($wnd.Vaadin);
    new Flow({imports: () => {}});

    assert.isDefined($wnd.Vaadin);
    assert.isDefined($wnd.Vaadin.Flow);
  });

  test("should initialize a flow loading indicator", async () => {
    new Flow({imports: () => {}});
    const indicator = $wnd.document.querySelector('.v-loading-indicator') as HTMLElement;
    const styles = $wnd.document.querySelector('style#css-loading-indicator') as HTMLElement;
    assert.isNotNull(indicator);
    assert.isNotNull(styles);

    assert.isFunction($wnd.Vaadin.Flow.loading);
    assert.equal('none', indicator.getAttribute('style'));

    $wnd.Vaadin.Flow.loading(true);
    assert.isNull(indicator.getAttribute('style'));
    assert.isFalse(indicator.classList.contains('second'));
    assert.isFalse(indicator.classList.contains('third'));

    await new Promise(resolve => setTimeout(resolve, 1500));
    assert.isTrue(indicator.classList.contains('second'));

    await new Promise(resolve => setTimeout(resolve, 3500));
    assert.isTrue(indicator.classList.contains('third'));

    $wnd.Vaadin.Flow.loading(false);
    assert.equal('none', indicator.getAttribute('style'));
    assert.isFalse(indicator.classList.contains('second'));
    assert.isFalse(indicator.classList.contains('third'));
  });

  test("loading should stack isActive", async () => {
    const flow = new Flow({imports: () => {}});
    assert.isFalse(flow.isActive);
    $wnd.Vaadin.Flow.loading(true);
    assert.isTrue(flow.isActive);
    $wnd.Vaadin.Flow.loading(true);
    assert.isTrue(flow.isActive);
    $wnd.Vaadin.Flow.loading(false);
    assert.isTrue(flow.isActive);
    $wnd.Vaadin.Flow.loading(false);
    assert.isFalse(flow.isActive);

  });

  test("should initialize Flow server navigation when calling flowInit(true)", () => {
    assert.isUndefined($wnd.Vaadin);

    stubServerRemoteFunction('FooBar-12345');
    mockInitResponse('FooBar-12345', changesResponse);

    const flow = new Flow();
    return (flow as any).flowInit(true)
      .then(() => {
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);

        // Check that serverside routing is enabled
        assert.isFalse(flow.response.appConfig.clientRouting);

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.clients.FooBar.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check that pushScript is not initialized
        assert.isUndefined($wnd.vaadinPush);

        // Check server added a div content with `Foo` text
        assert.equal("Foo", document.body.lastElementChild.textContent);
      });
  });

  test("should initialize UI when calling flowInit(true)", () => {
    assert.isUndefined($wnd.Vaadin);

    const initial = createInitResponse('FooBar-12345');
    $wnd.Vaadin = {TypeScript: {initial: JSON.parse(initial)}};

    const flow = new Flow();
    return (flow as any).flowInit(true)
      .then(() => {
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);

        // Check that serverside routing is enabled
        assert.isFalse(flow.response.appConfig.clientRouting);

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.clients.FooBar.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check that pushScript is not initialized
        assert.isUndefined($wnd.vaadinPush);

        // Check that Flow.ts doesn't inject appId script if config.imports is undefined
        const appIdScript = document.querySelector('script[type="module"][data-app-id]');
        assert.isNull(appIdScript);

        // Check that initial was removed
        assert.isUndefined($wnd.Vaadin.Flow.initial);
      });
  });

  test("should inject appId script when calling flowInit(true) with custom config.imports", () => {
    assert.isUndefined($wnd.Vaadin);

    const initial = createInitResponse('FooBar-12345');
    $wnd.Vaadin = {TypeScript: {initial: JSON.parse(initial)}};

    const flow = new Flow({
      imports: () => {}
    });
    return (flow as any).flowInit(true)
      .then(() => {
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);

        // Check that serverside routing is enabled
        assert.isFalse(flow.response.appConfig.clientRouting);

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.clients.FooBar.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check that pushScript is not initialized
        assert.isUndefined($wnd.vaadinPush);

        // Check that Flow.ts inject appId script
        const appIdScript = document.body.querySelector('script[type="module"][data-app-id]');
        assert.isDefined(appIdScript);
        const injectedAppId = appIdScript.getAttribute('data-app-id');
        assert.isTrue(flow.response.appConfig.appId.startsWith(injectedAppId));

        // Check that initial was removed
        assert.isUndefined($wnd.Vaadin.Flow.initial);
      });
  });

  test("should throw when an incorrect server response is received", () => {
    // Configure an invalid server response
    mock.get(/^.*\?v-r=init&location=.*/, (req, res) => {
      assert.equal('GET', req.method());
      return res
        .status(500)
        .body(`Unexpected Server Error`);
    });

    return (new Flow() as any).flowInit(true)
      .then(() => {
        throw new Error('Should not happen');
      })
      .catch(error => {
        assert.match(error.toString(), /500/);
      });
  });

  test("should connect client and server on route action", () => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111');

    const flow = new Flow();
    // Check that the Flow puts a client object for TypeScript
    assert.isDefined($wnd.Vaadin.Flow.clients.TypeScript.isActive);
    assert.isFalse($wnd.Vaadin.Flow.clients.TypeScript.isActive());

    const route = flow.serverSideRoutes[0];
    const indicator = $wnd.document.querySelector('.v-loading-indicator');

    let wasActive = false;
    let style = '';
    setTimeout(() => {
    // Check the `isActive` flag and indicator.style at the time the action is being executed
    wasActive = $wnd.Vaadin.Flow.clients.TypeScript.isActive();
      style = indicator.getAttribute('style');
    }, 5);

    return route
      .action({pathname: "Foo/Bar.baz"})
      .then(() => {
        // Check that flowInit() was called
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);
        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.clients.foobar.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.foobar.isActive());
        assert.equal('none', indicator.getAttribute('style'));

        // Check that pushScript is not initialized
        assert.isUndefined($wnd.vaadinPush);

        // Assert that element was created amd put in flowRoot so as server can find it
        assert.isDefined(flowRoot.$);
        assert.isDefined(flowRoot.$['foobar-1111111']);

        // Check that `isActive` flag was active during the action
        assert.isTrue(wasActive);
        // Check that indicator was visible during the action
        assert.isNull(style);
        // Check that `isActive` flag is set to false after the action
        assert.isFalse($wnd.Vaadin.Flow.clients.foobar.isActive());
      });
  });

  test("should remove context-path in request", () => {
    stubServerRemoteFunction('foobar-1111111', false, new RegExp('^Foo/Bar.baz$'));
    mockInitResponse('foobar-1111111');

    const flow = new Flow();
    flow['baseRegex'] = /^\/foo\//;
    const route = flow.serverSideRoutes[0];

    return route
      .action({pathname: "/foo/Foo/Bar.baz"})
      .then(() => {
        assert.isDefined(flow.response);
      });
  });

  test("should bind Flow serverSideRoutes function to the flow context", () => {
    // A mock class for router
    class TestRouter {
      routes: []
    }

    stubServerRemoteFunction('ROOT-12345');
    mockInitResponse('ROOT-12345');

    const router = new TestRouter ();
    router.routes = new Flow().serverSideRoutes;

    return router
      .routes[0].action({pathname: 'another-route'})
      .then(elem => {
        assert.isDefined(elem);
      });
  });

  test("should reuse container element in flow navigation", () => {
    stubServerRemoteFunction('ROOT-12345');
    mockInitResponse('ROOT-12345');

    const route = new Flow().serverSideRoutes[0];

    return route
      .action({pathname: "Foo"})
      .then(e1 => {
        return route
          .action({pathname: "Bar"})
          .then(e2 => {
            assert.equal(1, Object.keys(flowRoot.$).length);
            assert.equal(e1, e2);
            assert.equal(e1.id, e2.id);
          });
      });
  });

  test("navigation should be delayed to onBeforeEnter when using router API", () => {
    stubServerRemoteFunction('foobar-12345');
    mockInitResponse('foobar-12345');

    const route = new Flow().serverSideRoutes[0];

    return route.action({pathname: 'Foo/Bar.baz'})
      .then(async(elem) => {

        // Check that flowInit() was called
        assert.isDefined($wnd.Vaadin.Flow.clients.foobar.resolveUri);
        // Assert that flowRoot namespace was created
        assert.isDefined(flowRoot.$);
        // Assert that container was created and put in the flowRoot
        assert.isDefined(flowRoot.$['foobar-12345']);

        // Assert server side has not put anything in the container
        assert.equal(0, elem.children.length);

        // When using router API, it should expose the onBeforeEnter handler
        assert.isDefined(elem.onBeforeEnter);

        // after action TB isActive flag should be false
        assert.isFalse($wnd.Vaadin.Flow.clients.TypeScript.isActive());

        // Store `isAcive` flag when the onBeforeEnter is being executed
        let wasActive = false;
        setTimeout(() => wasActive = $wnd.Vaadin.Flow.clients.TypeScript.isActive(), 5);
        // @ts-ignore
        await elem.onBeforeEnter({pathname: 'Foo/Bar.baz'}, {});
        // TB should be informed when the server call was in progress and when it is finished
        assert.isTrue(wasActive);
        assert.isFalse($wnd.Vaadin.Flow.clients.TypeScript.isActive());

        // Assert server side has put content in the container
        assert.equal(1, elem.children.length);
      });
  });

  test("should be possible to cancel navigation when using router onBeforeEnter API", () => {
    // true means that server will prevent navigation
    stubServerRemoteFunction('foobar-12345', true);

    mockInitResponse('foobar-12345');

    const route = new Flow().serverSideRoutes[0];

    return route.action({pathname: 'Foo/Bar.baz'})
      .then((elem) => {

        // Check that flowInit() was called
        assert.isDefined($wnd.Vaadin.Flow.clients.foobar.resolveUri);
        // Assert that flowRoot namespace was created
        assert.isDefined(flowRoot.$);
        // Assert that container was created and put in the flowRoot
        assert.isDefined(flowRoot.$['foobar-12345']);

        // Assert server side has not put anything in the container
        assert.equal(0, elem.children.length);

        // When using router API, it should expose the onBeforeEnter handler
        assert.isDefined(elem.onBeforeEnter);

        // @ts-ignore
        elem.onBeforeEnter({pathname: 'Foo/Bar.baz'}, {prevent: () => {
            return {cancel: true};
          }})
          .then(obj => assert.isTrue(obj.cancel));

      });
  });

  test("onBeforeLeave should cancel `server->client` navigation", () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', true);
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = flow.serverSideRoutes[0];

    return route.action({pathname: 'Foo'})
      .then((elem: any) => {
        assert.isDefined(elem.onBeforeLeave);
        assert.equal('Foo', flow.pathname);

        return elem.onBeforeEnter({pathname: 'Foo'}, {prevent: () => {
            // set cancel to false even though server is cancelling
            return {cancel: false};
          }})
          .then((result: any) => {
            // view content was set
            assert.isFalse(result.cancel);
            assert.equal(1, elem.children.length);

            return elem.onBeforeLeave({pathname: 'Lorem'}, {prevent: () => {
                // set cancel to true
                return {cancel: true};
              }})
              .then((result: any) => {
                // Navigation cancelled onBeforeLeave
                assert.isTrue(result.cancel);
              });
          });
      });
  });

  test("onBeforeEnter should handle forwardTo `server->client` navigation", () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', false, undefined, {pathname: 'Lorem', search: ''});
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = flow.serverSideRoutes[0];

    return route.action({pathname: 'Foo'})
      .then((elem: any) => {
        return elem.onBeforeEnter({pathname: 'Foo'}, {redirect: (context: any) => {
            return {redirectContext: context}
          }, })
          .then((result: any) => {
            // Navigate to expect destination
            assert.equal('Lorem', result.redirectContext);
          });
      });
  });

  test("onBeforeLeave should not cause double round-trip on `server->server` navigation", () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', true);
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    const route = new Flow().serverSideRoutes[0];

    return route.action({pathname: 'Foo'})
      .then((elem: any) => {
        return elem.onBeforeEnter({pathname: 'Foo'}, {prevent: () => {
            // set cancel to false even though server is cancelling
            return {cancel: false};
          }})
          .then(() => {
            return elem.onBeforeLeave({pathname: 'Foo'}, {prevent: () => {
                // set cancel to true
                return {cancel: true};
              }})
              .then((result: any) => {
                // since server call is skipped, prevent() above is not executed
                // checking that cancel was not set demonstrates that there
                // were no double round-trip
                assert.isUndefined(result.cancel);
              });
          });
      });
  });

  test("should load pushScript on init", async() => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111', undefined, stubVaadinPushSrc);

    const flow = new Flow();

    const route = flow.serverSideRoutes[0];
    await route.action({pathname: "Foo/Bar.baz"});

    assert.isDefined($wnd.vaadinPush);
    assert.isTrue($wnd.vaadinPush.isStub);
  });

  test("should load pushScript on flowInit(true) with initial response", async() => {
    const initial = createInitResponse('FooBar-12345');
    $wnd.Vaadin = {TypeScript: {initial: JSON.parse(initial)}};
    $wnd.Vaadin.TypeScript.initial.pushScript = stubVaadinPushSrc;

    const flow = new Flow();
    await (flow as any).flowInit(true);

    assert.isDefined($wnd.vaadinPush);
    assert.isTrue($wnd.vaadinPush.isStub);
  });

  test("should load pushScript on flowInit(true) with server response", async() => {
    stubServerRemoteFunction('FooBar-12345');
    mockInitResponse('FooBar-12345', undefined, stubVaadinPushSrc);

    const flow = new Flow();
    await (flow as any).flowInit(true);

    assert.isDefined($wnd.vaadinPush);
    assert.isTrue($wnd.vaadinPush.isStub);
  });

  test("should load pushScript on route action", async() => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111', undefined, stubVaadinPushSrc);

    const flow = new Flow();

    const route = flow.serverSideRoutes[0];
    await route.action({pathname: "Foo/Bar.baz", search:""});

    assert.isDefined($wnd.vaadinPush);
    assert.isTrue($wnd.vaadinPush.isStub);
  });

  test("should not throw error when response header content type has charset", async () => {
    stubServerRemoteFunction('foobar-1111112');
    mockInitResponse('foobar-1111113', undefined, stubVaadinPushSrc, true);
    const flow = new Flow();
    const route = flow.serverSideRoutes[0];
    await route.action({pathname: "Foo/Bar.baz", search:""});
  });

  test("should not throw error when response header content type has no charset", async () => {
    stubServerRemoteFunction('foobar-1111113');
    mockInitResponse('foobar-1111113', undefined, stubVaadinPushSrc);
    const flow = new Flow();
    const route = flow.serverSideRoutes[0];
    await route.action({pathname: "Foo/Bar.baz", search:""});
  });
});

function stubServerRemoteFunction(id: string, cancel: boolean = false, routeRegex?: RegExp,
                                  url?: NavigationParameters) {
  let container : any;

  // Stub remote function exported in JavaScriptBootstrapUI.
  flowRoot.$server = {
    connectClient: (localName: string, elemId: string, route: string) => {

      assert.isDefined(localName);
      assert.isDefined(elemId);
      assert.isDefined(route);
      if (routeRegex) {
        assert.match(route, routeRegex);
      }

      assert.equal(elemId, id);
      assert.equal(localName, `flow-container-${elemId.toLowerCase()}`);

      container = flowRoot.$[elemId];

      assert.isDefined(container);
      assert.isDefined(container.serverConnected);

      // When appending elements container should be attached and hidden
      assert.isTrue(container.isConnected);
      assert.equal('none', container.style.display);

      container.appendChild(document.createElement('div'));

      // asynchronously resolve the remote server call
      setTimeout(() => {
        container.serverConnected(cancel, url);
        // container should be visible when not cancelled or not has redirect server-client
        assert.equal(cancel || url ? 'none' : '', container.style.display);
      }, 10);
    },
    leaveNavigation: () => {
      // asynchronously resolve the promise
      setTimeout(() => container.serverConnected(cancel, url), 10);
    }
  };
}

function mockInitResponse(appId: string, changes = '[]', pushScript?: string, withCharset?: boolean) {
  // Configure a valid server initialization response
  mock.get(/^.*\?v-r=init.*/, (req, res) => {
    assert.equal('GET', req.method());
    return res
      .status(200)
      .header("content-type", "application/json" + (withCharset ? ";charset=ISO-8859-1" : ""))
      .body(createInitResponse(appId, changes, pushScript));
  });
}
