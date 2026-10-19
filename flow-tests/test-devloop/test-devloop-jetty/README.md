# Dev loop tests: servlet container

End-to-end tests for the dev loop against the *other* shape of Vaadin
application: a WAR with no entry point, run by the project's own build plugin.

`test-devloop-spring` covers the shape the loop was built for first — a Spring Boot
application with a `main` method, launched as `java -cp … <MainClass>`. This
module covers the shape a generated Vaadin WAR starter has:
`<packaging>war</packaging>`, `mvn jetty:run`, and **Jetty nowhere on the
project's classpath**, because the container is a build plugin rather than a
dependency.

## What is different, and therefore what is tested

The daemon runs the project's `jetty-ee10-maven-plugin:run` in the build's own
JVM (`deployMode=EMBED`, which is the plugin's default), with both `-javaagent`s
and the JVM flags in `MAVEN_OPTS` and the chosen JDK in `JAVA_HOME`. So:

- **The application JVM is Maven's JVM.** It is still a *direct child* of the
  daemon, which is what keeps exit codes meaningful and stops a kill orphaning
  anything. A forked deploy mode would break that, which is why the daemon warns
  about a pom that pins one.
- **The daemon does not own the classpath.** The webapp class loader is
  assembled by the plugin. A sibling module resolves to its `target/classes`
  only because the run is driven with `-pl :app -am compile`, so that module's
  compile phase ran in the same session — `DevLoopMultiModuleIT` is what
  holds that down.
- **Application classes are in a webapp class loader.** The connector redefines
  through `Instrumentation`, which acts on a loaded `Class` whatever loaded it;
  `DevLoopJettyApplyIT` is what says so in practice.
- **There is no dependency injection and no container-built proxy**, so a
  structural edit is limited only by what the JVM accepts. That is the honest
  baseline, and the Spring fixture cannot show it.
- **The application log starts with a build log.** Readiness is Jetty's own
  `Started ServerConnector@…{0.0.0.0:8898}`, and Maven's `[ERROR]` epilogue must
  not be counted as failures of its own.

## The pom is not required to change

`devloop-app/pom.xml` keeps `<scan>2</scan>`, exactly as a generated WAR
starter ships it. Maven gives that precedence over the
`-Djetty.scan=0` the daemon passes, and a rescanner left running redeploys the
webapp underneath an `apply` that has already reported a hot swap — measured.
Rather than requiring every project to change its pom, the daemon puts its own
jar on `maven.ext.class.path` and `DevLoopBuildExtension` rewrites the effective
configuration in memory. Keeping the competing value here is what proves that
override still works; `DevLoopJettyStartIT` asserts it.

## Layout

```
devloop-shared/   a sibling library, so the multi-module leg exists
devloop-app/      the WAR the ITs drive; no src/main/webapp and no web.xml,
                  matching the starter - the static file it serves comes from
                  the sibling jar's META-INF/resources, and Flow registers its
                  servlet from an annotated listener the container scans
```

Named, packaged and laid out exactly like `test-devloop-spring`'s two modules,
down to `TaskListView`, `TaskService`, `DueDateFormatter` and `task-list.css`.
That is not tidiness: the ITs that are about the loop rather than about a
servlet container are written once in `test-devloop-support` and merely
extended here, and they can only be written once if both fixtures have the same
files in the same places.

## What is tested here, and what is inherited

`test-devloop-support` carries the ITs both fixtures run - `DevLoopApplyIT`,
`DevLoopCssIT`, `DevLoopMultiModuleIT`, `DevLoopLifecycleIT`,
`DevLoopBrowserIT`, `DevLoopCliContractIT`, `DevLoopDaemonSurvivalIT`,
`DevLoopDeletionIT`, `DevLoopPomEditIT` and `DevLoopFrontendIT` - along with the
CLI driver and the source patcher. They are
not extended here: this module's pom points failsafe's `<dependenciesToScan>` at
that jar, so they run against this WAR under their own names. Notably the
browser tests run here too: neither fixture runs a dev server (`vaadin.frontend.hotdeploy=false`
in both), so what reaches an open page is the daemon's own push over the
dev-tools connection - and here that connection crosses a servlet container's
websocket handshake and a webapp class loader, which is where it can fail. The
`DevLoopJetty*IT` classes in this module extend only the common
`AbstractDevLoopIT` base, and hold only what is true because the runtime is a
build plugin: the chosen runtime's name, the HotswapAgent
configuration a webapp class loader needs, the rescanner override, the
application's own `WEB-INF/classes` stylesheet that no container serves, and a
method-body edit not being read as a frontend change.

## Running

```
mvn verify -pl flow-tests/test-devloop/test-devloop-jetty/devloop-app
```

There is no `jetty:start`/`jetty:stop` execution: the daemon owns the
application process, and a second launcher would fight it for the port. The ITs
drive `vaadin-dev start` themselves, so a failure to start is a test failure
with the daemon's own reason attached rather than a build error.

The port is 8898 — not 8080, and not `test-devloop-spring`'s 8899, so the two dev-loop
IT modules can never take each other's port.
