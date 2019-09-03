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
const flowRoot = (window.document.body as any);

suite("Flow", () => {

  beforeEach(() => {
    mock.setup();
  });

  afterEach(() => {
    mock.teardown();
    delete flowRoot.$;
    delete flowRoot.$server;
  });

  test("should accept a configuration object", () => {
    const flow = new Flow({imports: () => {}});
    assert.isDefined(flow.config);
    assert.isDefined(flow.config.imports);
  });

  test("should initialize Flow client when calling start()", () => {
    const $wnd = window as any;
    assert.isUndefined($wnd.Vaadin);

    mockInitResponse('FooBar-12345');
    return new Flow()
      .start()
      .then(response => {
        assert.isDefined(response);
        assert.isDefined(response.appConfig);
        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.FooBar.isActive());
      });
  });

  test("should throw when an incorrect server response is received", () => {
    // Configure an invalid server response
    mock.get('VAADIN/?v-r=init', (req, res) => {
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

    return new Flow()
      .navigate({pathname: "Foo/Bar.baz"})
      .then(() => {
        // Check that flowInit() was called
        assert.isDefined((window as any).Vaadin.Flow.resolveUri);

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
        assert.isDefined((window as any).Vaadin.Flow.resolveUri);
        // Assert that flowRoot namespace was created
        assert.isDefined(flowRoot.$);
        // Assert that container was created and put in the flowRoot
        assert.isDefined(flowRoot.$['foobar-12345']);

        // Assert server side has not put anything in the container
        assert.equal(0, elem.children.length);

        // When using router API, it should expose the onBeforeEnter handler
        assert.isDefined(elem.onBeforeEnter);
        elem.onBeforeEnter && elem.onBeforeEnter({pathname: 'Foo/Bar.baz'}, {prevent: () => {}})

        // Assert server side has put content in the container
        assert.equal(1, elem.children.length);
      });
  });

  test("should be possible to cancel navigation when using router API", () => {
    stubServerRemoteFunction('foobar-12345', true);
    mockInitResponse('foobar-12345');

    const route = new Flow().route;

    return route.action({pathname: 'Foo/Bar.baz'})
      .then(async(elem) => {

        // Check that flowInit() was called
        assert.isDefined((window as any).Vaadin.Flow.resolveUri);
        // Assert that flowRoot namespace was created
        assert.isDefined(flowRoot.$);
        // Assert that container was created and put in the flowRoot
        assert.isDefined(flowRoot.$['foobar-12345']);

        // Assert server side has not put anything in the container
        assert.equal(0, elem.children.length);

        // When using router API, it should expose the onBeforeEnter handler
        assert.isDefined(elem.onBeforeEnter);

        // @ts-ignore
        const promise = elem.onBeforeEnter({pathname: 'Foo/Bar.baz'}, {prevent: () => {
          return {cancel: true};
        }});

        promise.then(obj => assert.isTrue(obj.cancel));
      });
  });
});

function stubServerRemoteFunction(id: string, cancel: boolean = false) {
  // Stub remote function exported in JavaScriptBootstrapUI.
  flowRoot.$server = {
    connectClient: (localName: string, elemId: string, route: string) => {
      assert.isDefined(localName);
      assert.isDefined(elemId);
      assert.isDefined(route);

      assert.equal(elemId, id);
      assert.equal(localName, `flow-container-${elemId.toLowerCase()}`);

      assert.isDefined(flowRoot.$[elemId]);
      assert.isDefined(flowRoot.$[elemId].serverConnected);

      flowRoot.$[elemId].appendChild(document.createElement('div'));

      // Resolve the promise
      flowRoot.$[elemId].serverConnected(cancel);
    }
  };
}

function mockInitResponse(appId: string) {
  // Configure a valid server initialization response
  mock.get('VAADIN/?v-r=init', (req, res) => {
    assert.equal('GET', req.method());
    return res
      .status(200)
      .header("content-type","application/json")
      .body(`
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
            "changes": [],
            "timings": [],
            "Vaadin-Security-Key": "119a6005-e663-4a4c-a882-bbfa8bd0c304",
            "Vaadin-Push-ID": "4b915ffb-4e0a-484c-9995-09500fe9fa3a"
          }
        }
      }
    `);
  });
}
