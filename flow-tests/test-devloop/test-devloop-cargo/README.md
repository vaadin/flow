# Dev loop tests: Cargo

End-to-end tests for the dev loop against a WAR whose servlet container is
**installed, started and handed a deployed copy of the application** by the
build, rather than one that serves the module's own output in place.

`test-devloop-jetty` covers the same packaging with `mvn jetty:run` in
`EMBED` mode, where the container shares the build's JVM and the webapp class
loader reads `target/classes`. This module covers what a generated WAR starter
actually does in production shape: Apache Tomcat 11, installed under `target/`
by [Codehaus Cargo](https://codehaus-cargo.github.io/), running as a **process
of its own**, with the packaged WAR copied into its `webapps`.

Cargo rather than a Tomcat plugin because there is no other option: Apache's
`tomcat7-maven-plugin` was last released in 2013 and runs a `javax.servlet`
container that a Jakarta EE application cannot be deployed to at all. One entry
for Cargo also covers every other container it drives.

## What is different, and therefore what is tested

- **The container is a fork.** The application is a *grandchild* of the daemon,
  so its own exit code is lost - the code the daemon waits on is Maven's -
  and stopping it has to reach a process Maven owns. `AppProcess` ends a launch
  descendants-first; `DevLoopCargoRestartIT` is what says that holds.
- **The JVM flags cannot travel in `MAVEN_OPTS`.** Nothing on Maven's command
  line and nothing in its environment reaches a JVM Maven forked, and no
  parameter of `cargo:run` that could carry them has a user property. They go on
  the model as the project property `cargo.jvmargs`, which only
  `DevLoopBuildExtension` can set, so this is the one runtime that does not work
  without that extension at all. `DevLoopCargoStartIT` asserts the agents
  arrived by hot swapping through them.
- **The run goal has to be kept to one module.** A goal named on a Maven command
  line runs on every project in the reactor, and the daemon names one with
  `-pl :app -am`. Jetty's mojo supports `war` packaging alone and skips the
  rest; Cargo's fails the build on the first module that is not a WAR. The
  daemon switches the goal off reactor-wide with `cargo.maven.skip` and switches
  it back on here through a forced `<skip>false</skip>`.
- **The webapp class loader reads a deployed copy.** A hot swap is unaffected,
  because a redefine acts on a loaded class whatever loaded it. A *resource* is
  not: refreshing `target/classes` is the whole of what an apply can do for one,
  and the container is reading a WAR it was handed. See below.
- **`vaadin-dev-server` is not `<optional>`.** `maven-war-plugin` leaves optional
  dependencies out of `WEB-INF/lib`, so with the Jetty fixture's declaration the
  application starts, serves, and never registers with the daemon.

## The pom is not required to change

The daemon needs `cargo:run` to be switched on for this module while it is
switched off for the rest of the reactor, and it needs a JVM-flags property that
no command line can set. Neither is written here: the daemon puts its own jar on
`maven.ext.class.path` and `DevLoopBuildExtension` edits the effective model in
memory. Nothing in this pom exists for the dev loop except the container
configuration any Cargo project would write anyway.

## Layout

```
devloop-shared/   a sibling library, so the multi-module leg exists
devloop-app/      the WAR the ITs drive; no src/main/webapp and no web.xml,
                  matching the starter - the static file it serves comes from
                  the sibling jar's META-INF/resources, and Flow registers its
                  servlet from an annotated listener the container scans
```

Named, packaged and laid out exactly like `test-devloop-jetty`'s two modules,
down to `TaskListView`, `TaskService`, `DueDateFormatter` and `task-list.css`.
That is not tidiness: the ITs that are about the loop rather than about a
container are written once in `test-devloop-support` and merely run here, and
they can only be written once if every fixture has the same files in the same
places.

## What is tested here, and what is inherited

`test-devloop-support` carries the ITs every fixture runs - `DevLoopApplyIT`,
`DevLoopMultiModuleIT`, `DevLoopLifecycleIT`, `DevLoopBrowserIT`,
`DevLoopCliContractIT`, `DevLoopDaemonSurvivalIT`, `DevLoopDeletionIT`,
`DevLoopPomEditIT` and `DevLoopFrontendIT` - along with the CLI driver and the
source patcher. They are not extended here: this module's pom points failsafe's
`<dependenciesToScan>` at that jar, so they run against this WAR under their own
names. The `DevLoopCargo*IT` classes extend only the common `AbstractDevLoopIT`
and hold what is true because the container is a fork reading a deployed WAR.

**`DevLoopCssIT` is excluded**, and `DevLoopCargoResourceIT` replaces it. That
test fetches the sibling module's stylesheet over HTTP after an apply, which
works wherever the webapp class path *is* the modules' own output. Here the
sibling arrives inside a jar in a WAR that Cargo copied into Tomcat's `webapps`,
so refreshing `devloop-shared/target/classes` never reaches what the container
serves; only a restart, which rebuilds and redeploys the WAR, can. That is a
property of deploying a packaged WAR and it holds for WildFly and TomEE too, so
it is asserted for what it is rather than worked around.

## Running

```
mvn verify -pl flow-tests/test-devloop/test-devloop-cargo/devloop-app
```

Nothing else is needed: `VaadinDevCli` pins the daemon's reactor root to this
fixture, so the loop spans `devloop-shared` and `devloop-app` and nothing else -
without that it would resolve the whole Flow repository, which is both slow and
a different test.

The first run downloads and unpacks Tomcat under `target/cargo/installs`; later
ones reuse it.

There is no `cargo:start`/`cargo:stop` execution: the daemon owns the
application process, and a second launcher would fight it for the port. The ITs
drive `vaadin-dev start` themselves, so a failure to start is a test failure
with the daemon's own reason attached rather than a build error.

The port is 8897 - not 8080, and not `test-devloop-jetty`'s 8898 or
`test-devloop-spring`'s 8899, so no two dev-loop IT modules can take each
other's port.

## Running it by hand

From `devloop-app`, with the same reactor root the ITs pin:

```bash
export VAADIN_DEV_DAEMON_OPTS='-Dvaadin.dev.reactorRoot=..'
./.vaadin/vaadin-dev start
./.vaadin/vaadin-dev status
```

```powershell
$env:VAADIN_DEV_DAEMON_OPTS = '-Dvaadin.dev.reactorRoot=..'
.\.vaadin\vaadin-dev.cmd start
.\.vaadin\vaadin-dev.cmd status
```

Then open <http://localhost:8897/>, and apply an edit with:

```bash
./.vaadin/vaadin-dev apply
```

Without `reactorRoot` the daemon walks up to the Flow repository's own root and
takes all of it into the loop. That is occasionally what you want - editing
`flow-server` and seeing it live - but it is far slower, and inside this
repository it also needs

```bash
export VAADIN_DEV_DAEMON_OPTS='-Dvaadin.dev.mavenArgs=-DskipTsTests=true'
```

because a WAR fixture builds with `package`, and `flow-client` runs its karma
tests in the `test` phase through `exec-maven-plugin`, which
`-Dmaven.test.skip` does not govern. A user project needs none of this.

Either way the first start is slow: Tomcat has to be downloaded and unpacked,
and the WAR packaged and deployed.

Open the page **before** testing a hot swap by hand: a redefine acts on classes
already loaded, and a class the application has not loaded yet still has its
pre-edit bytes from the deployed WAR.
