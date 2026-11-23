# Dev loop tests: Payara Micro

A WAR that the build **starts a Payara Micro for and hands a deployed copy of
the application** — the same shape as `test-devloop-cargo`,
`test-devloop-tomee`, `test-devloop-liberty` and `test-devloop-payara`, and not
the shape `test-devloop-jetty` covers, where the container shares the build's
JVM and the webapp class loader reads `target/classes`.

It has no IT classes of its own: failsafe runs the shared suite from
`flow-test-devloop-support` against it through `<dependenciesToScan>`, which is
what answers the questions a unit test cannot for a forked container — did the
loop's agents reach the *server's* JVM, and did the goal run in one module.
`DevLoopCssIT` is excluded, as it is for `test-devloop-cargo`: the sibling's
stylesheet reaches the server inside a jar in the deployed WAR, which refreshing
`devloop-shared/target/classes` cannot reach. The application runs in dev-bundle
mode (`vaadin.frontend.hotdeploy=false`) like every other fixture, so that
nothing but the daemon pushes to the browser.

## What is different about Payara Micro, and therefore what is checked here

- **The JVM-flag channel is `exec.args`.** No parameter of
  `payara-micro:start` that could carry the flags has a user property -
  `<javaCommandLineOptions>` is settable from a pom alone - but the mojo reads
  `exec.args` straight off the session's user properties and splices its tokens
  in ahead of `-jar`. The name is undocumented and not Payara's own, so a
  release that dropped it would leave the server starting with no agents; the
  launch line in `target/devloop/app.log` is where to look first.
- **`deployWar` is forced on.** It defaults to `false` on `start` and only `dev`
  turns it on, so without it the server would come up with nothing deployed.
- **The run goal has to be kept to one module**, exactly as for Payara Server:
  `payara.skip` switches it off reactor-wide and a forced `<skip>false</skip>`
  switches it back on here.
- **`vaadin-dev-server` is not `<optional>`.** `maven-war-plugin` leaves optional
  dependencies out of `WEB-INF/lib`, so the application would start, serve, and
  never register with the daemon.

## What the pom has to say, and why

Only what any Payara Micro project writes: the version, the port and
`--nocluster` in `<commandLineOptions>` (the plugin has no parameter for the
port, and a single-instance fixture has no use for Hazelcast), the root context,
and `-Dvaadin.frontend.hotdeploy=false` in `<javaCommandLineOptions>`.

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
mvn verify -f flow-tests/test-devloop/test-devloop-payara-micro/pom.xml
```

## Running it by hand

From `devloop-app`:

```bash
./.vaadin/vaadin-dev start
./.vaadin/vaadin-dev status        # must say: payara-micro
```

```powershell
.\.vaadin\vaadin-dev.cmd start
.\.vaadin\vaadin-dev.cmd status
```

Then open <http://localhost:8896/> and apply an edit with:

```bash
./.vaadin/vaadin-dev apply
```

Open the page **before** testing a hot swap by hand: a redefine acts on classes
already loaded, and a class the application has not loaded yet still has its
pre-edit bytes from the deployed WAR.

There is no `payara-micro:start`/`payara-micro:stop` execution in the pom: the
daemon owns the application process, and a second launcher would fight it for
the port.
