# Dev loop tests: Payara Server

A WAR whose application server is **unzipped, started and handed a deployed
copy of the application** by the build — the same shape as `test-devloop-cargo`,
`test-devloop-tomee`, `test-devloop-liberty` and `test-devloop-payara-micro`,
and not the shape `test-devloop-jetty` covers, where the container shares the
build's JVM and the webapp class loader reads `target/classes`.

It has no IT classes of its own: failsafe runs the shared suite from
`flow-test-devloop-support` against it through `<dependenciesToScan>`, which is
what answers the questions a unit test cannot for a forked container — did the
loop's agents reach the *server's* JVM, and did the goal run in one module.
`DevLoopCssIT` is excluded, as it is for `test-devloop-cargo`: the sibling's
stylesheet reaches the server inside a jar in the deployed WAR, which refreshing
`devloop-shared/target/classes` cannot reach. The application runs in dev-bundle
mode (`vaadin.frontend.hotdeploy=false`) like every other fixture, so that
nothing but the daemon pushes to the browser.

## What is different about Payara Server, and therefore what is checked here

- **`start`, never `dev`.** `payara-server:dev` forces `autoDeploy`,
  `liveReload`, `keepState`, `trimLog` and `aiAgent` on: a redeploying watcher
  that competes with every apply, a log rewriter on the very stream readiness is
  read from, and a prompt on standard input. `start` defaults all of them off.
- **The JVM-flag channel is `payara.javaCommandLineOptions`**, and a pom cannot
  take it away: that user property feeds a second list which the mojo appends
  to the `<javaCommandLineOptions>` the pom writes. It is a `List<String>`, so
  Maven splits it on commas and the mojo silently drops any element with no
  `=` — which is why the daemon hands the flags over as one value.
- **The run goal has to be kept to one module.** A goal named on a Maven command
  line runs on every project in the reactor, and the daemon names one with
  `-pl :app -am`. On the reactor root `start` would bring up a domain with
  nothing deployed and block. The daemon switches it off reactor-wide with
  `skip` and back on here through a forced `<skip>false</skip>`.
- **`vaadin-dev-server` is not `<optional>`.** `maven-war-plugin` leaves optional
  dependencies out of `WEB-INF/lib`, so the application would start, serve, and
  never register with the daemon.

## What the pom has to say, and why

Only what any Payara Server project writes: the server version, the root
context, and `-Dvaadin.frontend.hotdeploy=false` in `<javaCommandLineOptions>`.

The port is **8080**, and it is not the pom's to choose: it is `domain1`'s, and
`payara.http.port` only tells the plugin where to connect. No other dev-loop
fixture uses 8080.

## Layout

```
devloop-shared/   a sibling library, so the multi-module leg exists
devloop-app/      the WAR; no src/main/webapp and no web.xml, matching a
                  generated starter
```

Named and laid out exactly like the other fixtures, down to `TaskListView`,
`TaskService`, `DueDateFormatter` and `task-list.css`.

## Running

```
mvn verify -f flow-tests/test-devloop/test-devloop-payara/pom.xml
```

The first run downloads and unpacks Payara Server, which is far slower than any
later boot.

## Running it by hand

From `devloop-app`:

```bash
./.vaadin/vaadin-dev start
./.vaadin/vaadin-dev status        # must say: payara
```

```powershell
.\.vaadin\vaadin-dev.cmd start
.\.vaadin\vaadin-dev.cmd status
```

Then open <http://localhost:8080/> and apply an edit with:

```bash
./.vaadin/vaadin-dev apply
```

Open the page **before** testing a hot swap by hand: a redefine acts on classes
already loaded, and a class the application has not loaded yet still has its
pre-edit bytes from the deployed WAR.

There is no `payara-server:start`/`payara-server:stop` execution in the pom: the
daemon owns the application process, and a second launcher would fight it for
the port.
