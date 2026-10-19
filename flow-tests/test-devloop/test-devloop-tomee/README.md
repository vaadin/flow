# Dev loop tests: Apache TomEE

A WAR whose servlet container is **unzipped, started and handed a deployed copy
of the application** by the build — the same shape as `test-devloop-cargo`,
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

## What is different about TomEE, and therefore what is checked here

- **Its run goal cannot be named on a Maven command line**, which is the whole
  reason for the fixture — and it takes a reactor to see it. A named goal runs
  in *every* project in the reactor, and `tomee:run` has no escape: no `skip`
  to switch it off (`skipCurrentProject` guards `copyWar` alone) and no
  packaging check, so `AbstractTomEEMojo.execute` unzips a TomEE under the
  reactor root's `target/`, starts it and blocks — before `devloop-app` is
  built, with nothing deployed, and without failing, so the daemon just waits
  out its start window.

  So the daemon names `package` and no goal, and `DevLoopBuildExtension` binds
  `run` to that phase in `devloop-app` alone. What to check in
  `target/devloop/app.log`: the launch line ends in `package` with no
  `org.apache.tomee.maven:…:run` after it and carries
  `-Dvaadin.devloop.ext.bind=package:run`, and the build shows **one**
  `--- tomee:…:run (vaadin-devloop-run) @ flow-test-devloop-tomee-app ---`,
  after `war:war` and in no other module. Two of them, or one against
  `flow-test-devloop-tomee`, is the failure this fixture exists to catch.
- **The flags are parsed the way a shell would parse them.** `tomee-plugin.args`
  goes through TomEE's own `Args.parse`, where a backslash escapes whatever
  follows it and is then dropped — measured on Windows, a
  `C:\Users\…\hotswap-agent.jar` reached the JVM as `C:Users…hotswap-agent.jar`
  and the JVM refused to start. `ServerPlugin.shellEscapedFlags` is what doubles
  them, and this is the only entry that needs it.
- **`reloadOnUpdate` is forced off.** It is TomEE's own redeploy-on-change, the
  second driver of restarts that `jetty.scan` is for Jetty. The pom here leaves
  it at its default, so the forced value only shows as a `[vaadin-dev]` line.
- **`vaadin-dev-server` is not `<optional>`.** `maven-war-plugin` leaves optional
  dependencies out of `WEB-INF/lib`, so the application would start, serve, and
  never register with the daemon.

## What the pom has to say, and why

Only what any TomEE project writes: the version, the classifier, the HTTP port,
the context and `vaadin.frontend.hotdeploy=false` as a system variable. `tomeeVersion` is pinned because the plugin's default of `-1`
resolves to whatever OpenEJB version the plugin was built from, and `context` is
`ROOT` so the view is at the root of the server rather than under the module's
`finalName`.

There are **no `<executions>`**: the daemon owns the application process, and
the execution that runs it is added to the effective model for the duration of
the run and never written here.

## Layout

```
devloop-shared/   a sibling library, so the multi-module leg exists
devloop-app/      the WAR; no src/main/webapp and no web.xml, matching a
                  generated starter
```

Named and laid out exactly like the other fixtures, down to `TaskListView`,
`TaskService`, `DueDateFormatter` and `task-list.css`.

## Running it by hand

From the fixture root, once, so the sibling is installed and the CLI is
generated:

```bash
mvn -q -o install -DskipTests
```

Then from `devloop-app`:

```bash
./.vaadin/vaadin-dev start
./.vaadin/vaadin-dev status        # must say: tomee
```

```powershell
.\.vaadin\vaadin-dev.cmd start
.\.vaadin\vaadin-dev.cmd status
```

Then open <http://localhost:8892/> and apply an edit with:

```bash
./.vaadin/vaadin-dev apply
```

Or run the shared suite against it, from `devloop-app`:

```bash
mvn -o verify
```

The first start resolves and unzips a TomEE under `target/apache-tomee`, which
is far slower than any later boot.

Open the page **before** testing a hot swap by hand: a redefine acts on classes
already loaded, and a class the application has not loaded yet still has its
pre-edit bytes from the deployed WAR.
