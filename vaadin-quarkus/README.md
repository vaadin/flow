# vaadin-quarkus

A Quarkus extension that adds support for Vaadin Flow. It is built and
released as part of Flow and shares its version number, from Vaadin 25.4
onwards. Before it was imported into this repository it lived in
[vaadin/quarkus](https://github.com/vaadin/quarkus), which still serves the
earlier Vaadin versions:

* `main`, at 3.2-SNAPSHOT, for Vaadin 25.2 and 25.3, with Quarkus 3.33
* `3.1` for Vaadin 25.1 and Quarkus 3.32
* `3.0` for Vaadin 25.0 and Quarkus 3.32
* `2.2` for Vaadin 24 and Quarkus 3.20
* `1.1` for Vaadin 23 and Quarkus 2

The minimum supported Quarkus version is the one this branch's `quarkus.version`
property names in the root POM — currently 3.33 (LTS). Vaadin 25.0 raised it
from 3.27 because Flow depends on Jackson 3.1.x and Jackson Annotations 2.21.x.

The module is split the way a Quarkus extension has to be:

* `runtime` — the extension itself, published as `com.vaadin:vaadin-quarkus`,
  along with the codestart a generated project starts from
* `deployment` — the build steps Quarkus runs at augmentation time, published
  as `com.vaadin:vaadin-quarkus-deployment`

The integration tests live in `flow-tests/vaadin-quarkus-tests`.

To try the extension out, start from
https://github.com/vaadin/base-starter-flow-quarkus/.

## devUI URL

After executing the quarkus project with dev profile `mvn quarkus:dev`, the
devUI can be accessed (with not overwritten configuration) at URL:
`http://localhost:8080/q/dev-ui/extensions`

## Push dispatch

The extension sets `quarkus.websocket.dispatch-to-worker=true` as a default.
This routes inbound Vaadin Push websocket frames through the Quarkus worker
thread pool instead of the Vert.x event loop.

**The Quarkus default (`false`) is unsafe for Vaadin applications.** Vaadin's
`PushHandler` acquires the session lock before dispatching, and application
code is free to block while holding that lock (e.g. synchronous REST calls or
database operations inside `BeforeEnterObserver` / `AfterNavigationListener`).
With the Quarkus default, that blocking happens on the same Vert.x event loop
that Push uses — deadlocking the loop and, if the REST client's response is
pinned to the same loop, hanging the request indefinitely.

Override to `false` only if you fully control all session-locked code paths and
have a reason to prefer event-loop dispatch (e.g. measurable latency-sensitive
Push patterns with strictly non-blocking handlers).
