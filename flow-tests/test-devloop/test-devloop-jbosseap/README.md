# Dev loop tests: Red Hat JBoss EAP

A WAR whose application server is **provisioned, started and handed a deployed
copy of the application** by the build — the same shape as `test-devloop-cargo`,
`test-devloop-liberty` and both Payara fixtures, and not the shape
`test-devloop-jetty` covers, where the container shares the build's JVM and the
webapp class loader reads `target/classes`.

The server is **Red Hat JBoss EAP 8.1**, which the Vaadin platform release notes
list among the supported application servers, driven by the same
`wildfly-maven-plugin` entry the dev loop uses for community WildFly.

It has no IT classes of its own: failsafe runs the shared suite from
`flow-test-devloop-support` against it through `<dependenciesToScan>`, which is
what answers the questions a unit test cannot for a forked container — did the
loop's agents reach the *server's* JVM, and did the goal run in one module.
`DevLoopCssIT` is excluded, as it is for `test-devloop-cargo`: the sibling's
stylesheet reaches the server inside a jar in the deployed WAR, which refreshing
`devloop-shared/target/classes` cannot reach. The application runs in dev-bundle
mode (`vaadin.frontend.hotdeploy=false`) like every other fixture, so that
nothing but the daemon pushes to the browser.

## What is different about EAP, and therefore what is checked here

- **EAP is provisioned, not downloaded.** The zip installer needs a Red Hat
  customer portal login; the Galleon feature pack does not. So the pom declares
  `org.jboss.eap:wildfly-ee-galleon-pack` with no version and pins it through the
  `org.jboss.eap.channels:eap-8.1` channel, resolved from
  `https://maven.repository.redhat.com/ga/`, which is how Red Hat documents EAP
  8.1 provisioning.
- **`wildfly:run` cannot provision it.** That goal has no `<channels>` or
  `<feature-packs>` parameter at all — it only knows
  `<feature-pack-location>`, which points at the community universe. So
  `wildfly:provision` is bound to `prepare-package` and `run` finds the server
  in `<provisioning-dir>`. `wildfly:run` forks the lifecycle up to `package`,
  so a `vaadin-dev start` runs that provisioning itself, once.
- **The JVM-flag channel is `wildfly.javaOpts`**, whose value the mojo's own
  `setJavaOpts` splits on whitespace.
  `target/devloop/app.log` is where to check that the agents arrived: the
  `flags:` line holds the whole value handed to the plugin.
- **The pom must not write `<javaOpts>`.** A `<configuration>` value beats the
  user property, so the agents would be dropped in silence. That is why the HTTP
  port goes in through `<server-args>` instead, which reaches the server's
  command line as a system property and is what `standalone.xml` reads
  `${jboss.http.port}` from.
- **The run goal has to be kept to one module.** A goal named on a Maven command
  line runs on every project in the reactor, and the daemon names one with
  `-pl :app -am`. `RunMojo` resolves the deployment from the project's own
  packaging and fails outright when there is no such file, so the aggregator pom
  ends the build with `The deployment
  'target/flow-test-devloop-jbosseap-25.4-SNAPSHOT.pom' could not be found`
  before the server is ever started. The daemon switches the goal off
  reactor-wide with `wildfly.skip` and switches it back on here through a forced
  `<skip>false</skip>` — the same inversion Cargo and both Payaras need. A
  single-module WildFly project never shows it.
- **The reactor has to reach `package`, not `compile`.** `wildfly:run` forks the
  lifecycle of the *application's* module alone, so a sibling stops at whatever
  phase the command line names. At `compile` it has no jar, Maven substitutes
  its `target/classes` directory for the dependency, and `maven-war-plugin`
  writes that directory into `WEB-INF/lib` under the jar's name — a zero-length
  `…-shared.jar/` entry in the WAR, and `ClassNotFoundException:
  …DueDateFormatter` on the first render. The second thing a single-module
  project cannot show.
- **EAP 8.1 needs a `jboss-deployment-structure.xml`, and the release notes do
  not say so.** See below.
- **`vaadin-dev-server` is not `<optional>`.** `maven-war-plugin` leaves optional
  dependencies out of `WEB-INF/lib`, so the application would start, serve, and
  never register with the daemon.

## What EAP 8.1 needs that is not the dev loop's doing

`src/main/webapp/WEB-INF/jboss-deployment-structure.xml` is the one file here
that no other fixture has, and it has nothing to do with the loop: without it
every UIDL request fails with

```
java.lang.NoSuchFieldError: Class com.fasterxml.jackson.annotation.JsonFormat$Shape
does not have member field 'com.fasterxml.jackson.annotation.JsonFormat$Shape POJO'
```

EAP 8.1 Update 8.0 still ships Jackson 2, and its `jaxrs` subsystem puts the
server's own `com.fasterxml.jackson.core.jackson-annotations` module on the
deployment's class path ahead of the Jackson 3 in `WEB-INF/lib`.

The Vaadin platform release notes for 25.3.0 list **RedHat JBoss EAP 8.1 with no
workaround at all** — the `jaxrs` exclusion is given for EAP 8.0 and WildFly 36,
and the `jackson-annotations` exclusion for WildFly 37+. On EAP 8.1 Update 8.0
the module exclusion **alone is not enough**: the deployment still resolves the
server's annotations. This file carries both. That the notes say EAP 8.1 needs
neither is a documentation gap rather than a Flow one.

It also means this fixture has a `src/main/webapp`, which the others do not.

## What the pom has to say, and why

Only what any EAP project writes: the feature pack, the channel, the Red Hat
repository the two are resolved from, the `provision` execution that installs
the server, the deployment name that puts the application at the root context,
and the HTTP port. Nothing in it exists for the dev loop.

`<name>ROOT.war</name>` is the root context: WildFly and EAP read a deployment's
context path off its name.

## Layout

```
devloop-shared/   a sibling library, so the multi-module leg exists
devloop-app/      the WAR; no web.xml, matching a generated starter, and the
                  only src/main/webapp among the fixtures - it holds nothing
                  but the jboss-deployment-structure.xml above
```

Named and laid out exactly like the other fixtures, down to `TaskListView`,
`TaskService`, `DueDateFormatter` and `task-list.css`.

## Running it by hand

From `devloop-app`:

```bash
./.vaadin/vaadin-dev start
./.vaadin/vaadin-dev status        # must say: wildfly
```

```powershell
.\.vaadin\vaadin-dev.cmd start
.\.vaadin\vaadin-dev.cmd status
```

Then open <http://localhost:8893/> and apply an edit with:

```bash
./.vaadin/vaadin-dev apply
```

The first start provisions EAP under `target/eap-server`, which is several
hundred megabytes off Red Hat's repository and far slower than any later boot.
A provisioned server is reused.

Open the page **before** testing a hot swap by hand: a redefine acts on classes
already loaded, and a class the application has not loaded yet still has its
pre-edit bytes from the deployed WAR.

There is no `wildfly:start`/`wildfly:shutdown` execution in the pom: the daemon
owns the application process, and a second launcher would fight it for the port.
