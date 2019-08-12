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

suite("Flow", () => {

  beforeEach(() => {
    mock.setup();
  });

  afterEach(() => {
    mock.teardown();
  });

  test("should accept a configuration object", () => {
    const flow = new Flow({imports: () => {}});
    assert.isDefined(flow.config);
    assert.isDefined(flow.config.imports);
  });

  test("should initialize Flow client when calling start()", () => {
    const $wnd = window as any;
    assert.isUndefined($wnd.Vaadin);

    mockInitResponse();
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
        assert.isFalse($wnd.Vaadin.Flow.clients.foobar.isActive());
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
    const flowRoot = (window.document.body as any);

    flowRoot.$server = {
      connectClient: () => {
        // Resolve the promise
        flowRoot.$['flow-foo-bar-baz-0'].serverConnected();
      }
    };

    mockInitResponse();
    return new Flow()
      .navigate({path: "Foo/Bar.baz"})
      .then(() => {
        // Check that start() was called
        assert.isDefined((window as any).Vaadin.Flow.resolveUri);

        // Assert that element was created amd put in flowRoot so as server can find it
        assert.equal(1, flowRoot.$.counter);
        assert.isDefined(flowRoot.$['flow-foo-bar-baz-0']);
      });
  });
});

function mockInitResponse() {
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
          "appId": "foobar-1111111",
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
