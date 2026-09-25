# Dev loop tests: Open Liberty

A WAR whose servlet container is **installed, started and handed a deployed
copy of the application** by the build — the same shape as `test-devloop-cargo`,
`test-devloop-payara` and `test-devloop-payara-micro`, and not the shape
`test-devloop-jetty` covers, where the container shares the build's JVM and the
webapp class loader reads `target/classes`.

It has no IT classes of its own: failsafe runs the shared suite from
`flow-test-devloop-support` against it through `<dependenciesToScan>`, which is
what answers the questions a unit test cannot for a forked container — did the
loop's agents reach the *server's* JVM, and did the goal run in one module.
`DevLoopCssIT` is excluded, as it is for `test-devloop-cargo`: the sibling's
stylesheet reaches the server inside a jar in the deployed WAR, which refreshing
`devloop-shared/target/classes` cannot reach. The application runs in dev-bundle
mode (`vaadin.frontend.hotdeploy=false`) like every other fixture, so that
nothing but the daemon pushes to the browser.

## What is different about Liberty, and therefore what is checked here

- **The JVM-flag channel carries one flag per property.** Every Maven property
  named `liberty.jvm.<key>` becomes one *line* of the server's generated
  `jvm.options`, and a line is one JVM argument — so the flags cannot be joined
  the way WildFly's `wildfly.javaOpts` and TomEE's `tomee-plugin.args` are. The
  daemon sends `-Dliberty.jvm.devloop0=…`, `devloop1=…`, one per flag.
  `target/liberty/wlp/usr/servers/defaultServer/jvm.options` is where to check
  that they arrived.
- **`looseApplication` is forced off.** Liberty's default deploys a
  loose-application XML pointing straight at `target/classes`, and Liberty's own
  `applicationMonitor` defaults to `updateTrigger="polled"` — so the server
  would restart the application under every apply. That monitor lives in
  `server.xml`, where the daemon cannot reach it, so the deployment shape is
  what changes instead: a packaged WAR does not change between restarts.
- **`embedded` is forced off** for the same reason it is listed as competing: an
  embedded server runs in Maven's JVM, which reads no `jvm.options` at all.
- **The run goal needs no phase and no skip.** `liberty:run` runs `resources`,
  `compiler:compile` and `war:war` itself, and it keeps itself to the farthest
  downstream project through the session's `ProjectDependencyGraph` — so unlike
  Cargo and both Payaras it needs none of the switch-the-goal-off machinery.
- **`vaadin-dev-server` is not `<optional>`.** `maven-war-plugin` leaves optional
  dependencies out of `WEB-INF/lib`, so the application would start, serve, and
  never register with the daemon.

## What the pom has to say, and why

Only what any Liberty project writes: the runtime artifact, the HTTP port and
the context root. Liberty's port lives in `server.xml` rather than in a plugin
parameter, so it goes in through `<bootstrapProperties>` and
`src/main/liberty/config/server.xml` reads it as `${httpPort}`.

The context root goes the same way, from the pom's `devloop.context.path`, and
it is `/devloop` rather than `/`: Liberty answers `getContextPath()` with `"/"`
for a root-context application instead of the `""` the servlet spec asks for,
and Flow then builds its `VAADIN` paths with a doubled slash. This is the one
fixture that is not served from `/`, and `AbstractDevLoopIT` reads the same pom
property, so the ITs follow it.

`-Dvaadin.frontend.hotdeploy=false` goes in through `<jvmOptions>`, which the
plugin writes into `jvm.options` after the loop's own lines. The dev loop takes `liberty.jvm.*` for its agents and
leaves `liberty.bootstrap.*` alone, so that channel is still the project's.

`server.xml` enables `servlet-6.0` and `websocket-2.1` and nothing else, and
declares one `httpEndpoint` — a second one would log a second "web application
available" line, which is what readiness is read from.

## Layout

```
devloop-shared/   a sibling library, so the multi-module leg exists
devloop-app/      the WAR; no src/main/webapp and no web.xml, matching a
                  generated starter
```

Named and laid out exactly like the other fixtures, down to `TaskListView`,
`TaskService`, `DueDateFormatter` and `task-list.css`.

## Running it by hand

From `devloop-app`:

```bash
./.vaadin/vaadin-dev start
./.vaadin/vaadin-dev status        # must say: liberty
```

```powershell
.\.vaadin\vaadin-dev.cmd start
.\.vaadin\vaadin-dev.cmd status
```

Then open <http://localhost:8894/devloop/> and apply an edit with:

```bash
./.vaadin/vaadin-dev apply
```

The first start downloads and unpacks Open Liberty under `target/liberty`, and
creates and configures a server in it, which is far slower than any later boot.

Open the page **before** testing a hot swap by hand: a redefine acts on classes
already loaded, and a class the application has not loaded yet still has its
pre-edit bytes from the deployed WAR.

There is no `liberty:start`/`liberty:stop` execution in the pom: the daemon owns
the application process, and a second launcher would fight it for the port.
