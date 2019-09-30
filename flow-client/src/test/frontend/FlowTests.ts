const { suite, test, beforeEach, afterEach } = intern.getInterface("tdd");
const { assert } = intern.getPlugin("chai");

// API to test
import { Flow } from "../../main/resources/META-INF/resources/frontend/Flow";
// Intern does not serve webpack chunks, adding deps here in order to
// produce one chunk, because dynamic imports in Flow.ts  will not work.
import "../../main/resources/META-INF/resources/frontend/FlowBootstrap";
import "../../main/resources/META-INF/resources/frontend/FlowClient";
// Mock XMLHttpRequest so as we don't need flow-server running for tests.
import mock from 'xhr-mock';

const $wnd = window as any;
const flowRoot = window.document.body as any;

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

function createInitResponse(appId: string, changes = '[]'): string {
  return `
      {
        "appConfig": {
          "heartbeatInterval" : 300,
          "contextRootUrl" : "../",
          "debug" : true,
          "v-uiId" : 0,
          "serviceUrl" : "//localhost:8080/flow/",
          "webComponentMode" : false,
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
      }
    `;
};

suite("Flow", () => {

  beforeEach(() => {
    mock.setup();
  });

  afterEach(() => {
    mock.teardown();
    delete $wnd.Vaadin;
    delete flowRoot.$;
    delete flowRoot.$server;
  });

  test("should accept a configuration object", () => {
    const flow = new Flow({imports: () => {}});
    assert.isDefined(flow.config);
    assert.isDefined(flow.config.imports);
  });

  test("should initialize Flow server navigation when calling start()", () => {
    assert.isUndefined($wnd.Vaadin);

    stubServerRemoteFunction('FooBar-12345');
    mockInitResponse('FooBar-12345', changesResponse);

    const flow = new Flow();
    return flow
      .start()
      .then(() => {
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);

        // Check that serverside routing is enabled
        assert.isFalse(flow.response.appConfig.webComponentMode);

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);

        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check server added a div content with `Foo` text
        assert.equal("Foo", document.body.lastElementChild.textContent);
      });
  });

  test("should initialize UI when calling start()", () => {
    assert.isUndefined($wnd.Vaadin);

    const initial = createInitResponse('FooBar-12345');
    $wnd.Vaadin = {Flow: {initial: JSON.parse(initial)}};

    const flow = new Flow();
    return flow
      .start()
      .then(() => {
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);

        // Check that serverside routing is enabled
        assert.isFalse(flow.response.appConfig.webComponentMode);

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);

        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());

        // Check that initial was removed
        assert.isUndefined($wnd.Vaadin.Flow.initial);
      });
  });

  test("should throw when an incorrect server response is received", () => {
    // Configure an invalid server response
    mock.get(/^VAADIN\/\?v-r=init&location=.+/, (req, res) => {
      assert.equal('GET', req.method());
      return res
        .status(500)
        .body(`Unexpected Server Error`);
    });

    return new Flow()
      .start()
      .then(() => {
        throw new Error('Should not happen');
      })
      .catch(error => {
        assert.match(error.toString(), /500/);
      });
  });

  test("should connect client and server on navigation", () => {
    stubServerRemoteFunction('foobar-1111111');
    mockInitResponse('foobar-1111111');

    const flow = new Flow();
    return flow
      .navigate({pathname: "Foo/Bar.baz"})
      .then(() => {
        // Check that flowInit() was called
        assert.isDefined(flow.response);
        assert.isDefined(flow.response.appConfig);
        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.foobar.isActive());

        // Assert that element was created amd put in flowRoot so as server can find it
        assert.isDefined(flowRoot.$);
        assert.isDefined(flowRoot.$['foobar-1111111']);
      });
  });

  test("should bind Flow navigate function to the flow context", () => {
    // A mock class for router
    class TestRouter {
      config: any;

      constructor(config: any) {
        this.config = config;
      }

      navigate(params: any) : Promise<HTMLElement> {
        return this.config.navigate(params);
      }
    }

    stubServerRemoteFunction('ROOT-12345');
    mockInitResponse('ROOT-12345');

    const flow = new Flow();
    const router = new TestRouter ({
      // we'd rather this API syntax instead of () => flow.navigate();
      navigate: flow.navigate
    });

    return router
      .navigate({pathname: 'another-route'})
      .then(elem => {
        assert.isDefined(elem);
      });
  });

  test("should reuse container element in flow navigation", () => {
    stubServerRemoteFunction('ROOT-12345');
    mockInitResponse('ROOT-12345');

    const flow = new Flow();
    return flow
      .navigate({pathname: "Foo"})
      .then(e1 => {
        return flow
          .navigate({pathname: "Bar"})
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

    const route = new Flow().route;

    return route.action({pathname: 'Foo/Bar.baz'})
      .then(async(elem) => {

        // Check that flowInit() was called
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        // Assert that flowRoot namespace was created
        assert.isDefined(flowRoot.$);
        // Assert that container was created and put in the flowRoot
        assert.isDefined(flowRoot.$['foobar-12345']);

        // Assert server side has not put anything in the container
        assert.equal(0, elem.children.length);

        // When using router API, it should expose the onBeforeEnter handler
        assert.isDefined(elem.onBeforeEnter);
        // @ts-ignore
        elem.onBeforeEnter({pathname: 'Foo/Bar.baz'}, {})

        // Assert server side has put content in the container
        assert.equal(1, elem.children.length);
      });
  });

  test("should be possible to cancel navigation when using router onBeforeEnter API", () => {
    // true means that server will prevent navigation
    stubServerRemoteFunction('foobar-12345', true);

    mockInitResponse('foobar-12345');

    const route = new Flow().route;

    return route.action({pathname: 'Foo/Bar.baz'})
      .then((elem) => {

        // Check that flowInit() was called
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
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
    return flow.route.action({pathname: 'Foo'})
      .then((elem: any) => {
        assert.isDefined(elem.onBeforeLeave);
        assert.equal('Foo', flow.pathname);

        elem.onBeforeEnter({pathname: 'Foo'}, {prevent: () => {
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

  test("onBeforeLeave should not cause double round-trip on `server->server` navigation", () => {
    // true to prevent navigation from server
    stubServerRemoteFunction('foobar-12345', true);
    mockInitResponse('foobar-12345');

    const flow = new Flow();
    return flow.route.action({pathname: 'Foo'})
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

});

function stubServerRemoteFunction(id: string, cancel: boolean = false) {
  let container : any;
  // Stub remote function exported in JavaScriptBootstrapUI.
  flowRoot.$server = {
    connectClient: (localName: string, elemId: string, route: string) => {
      assert.isDefined(localName);
      assert.isDefined(elemId);
      assert.isDefined(route);

      assert.equal(elemId, id);
      assert.equal(localName, `flow-container-${elemId.toLowerCase()}`);

      container = flowRoot.$[elemId];

      assert.isDefined(container);
      assert.isDefined(container.serverConnected);

      container.appendChild(document.createElement('div'));

      // Resolve the promise
      flowRoot.$[elemId].serverConnected(cancel);
    },
    leaveNavigation: () => {
      // Resolve the promise
      container.serverConnected(cancel);
    }
  };
}

function mockInitResponse(appId: string, changes = '[]') {
  // Configure a valid server initialization response
  mock.get(/^VAADIN\/\?v-r=init.*/, (req, res) => {
    assert.equal('GET', req.method());
    return res
      .status(200)
      .header("content-type","application/json")
      .body(createInitResponse(appId, changes));
  });
}
