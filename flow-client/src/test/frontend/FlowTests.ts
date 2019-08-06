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
  });

  afterEach(() => {
    mock.teardown();
  });

  test("should accept a configuration object", () => {
    const flow = new Flow({imports: () => {}});
    assert.isDefined(flow.config);
    assert.isDefined(flow.config.imports);
  });

  test("should have the start() method in the API", () => {
    return new Flow()
      .start()
      .then(response => {
        assert.isDefined(response);
        assert.isDefined(response.appConfig);
        const $wnd = window as any;
        // Check that bootstrap was initialized
        assert.isDefined($wnd.Vaadin.Flow.initApplication);
        assert.isDefined($wnd.Vaadin.Flow.registerWidgetset);
        // Check that flowClient was initialized
        assert.isDefined($wnd.Vaadin.Flow.resolveUri);
        assert.isFalse($wnd.Vaadin.Flow.clients.foobar.isActive());
      });
  });

  test("should have the navigate() method in the API", () => {
    return new Flow().navigate();
  });
});
