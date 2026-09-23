# flow-devloop-daemon

A long-running daemon that owns one Vaadin application's edit-to-running-app
loop: background compilation, hot swap or restart, browser refresh, error
reporting. It answers, authoritatively, *"what is the state of my last
change?"* — an agent batches edits, runs one `apply`, and reads a verdict from
the exit code instead of rebuilding, relaunching and guessing from screenshots.

Everything in this module is for internal use only. May be renamed or removed
in a future release.

The user-facing documentation is the `vaadin-devloop` skill installed by
`mvn vaadin:install-dev-cli` (its resources live in
`flow-plugins/flow-plugin-base/src/main/resources/vaadin-dev-cli/`). This file is
for someone who has to change the code here.

## The one rule

**Zero dependencies.** Not SLF4J, not Jackson, nothing. That is why this is its
own module rather than a package inside `vaadin-dev-server`: a module boundary
can be enforced, and a `maven-enforcer-plugin` `RestrictImports` execution in
`pom.xml` does enforce it.

Two consequences follow, and both are deliberate:

- The daemon starts in tens of milliseconds, which is what makes
  `vaadin-dev status` cost milliseconds rather than a JVM boot.
- It can never drag the application's classpath into the daemon JVM. The daemon
  reads the app's class files (`MainClass`), compiles its sources (`Compile`) and
  redefines its classes through the in-app connector — but never loads one.

So `Json` hand-rolls 40 lines of JSON escaping, `Reactor` reads poms with the
JDK's own XML parser, and `MainClass` walks a class file's constant pool instead
of calling `Class.forName`. None of those is an oversight.

The jar is also the javaagent: the manifest carries `Premain-Class` /
`Agent-Class` for `com.vaadin.flow.devloop.agent.DevLoopAgent` alongside its own
`Main-Class`, so the app JVM is launched with `-javaagent:<this jar>` and there is
one artifact to resolve rather than two to keep in step. Only `DevLoopAgent`
loads over there; it publishes the JVM's `Instrumentation` handle and does
nothing else.

## Three communication channels

| Channel | Carries |
|---|---|
| **CLI ↔ daemon**, loopback TCP | one request line per command, found through the handshake file |
| **daemon ↔ in-app connector**, one long-lived socket | the redefine/resource commands, and the app-liveness signal |
| **files under `target/devloop/`** | logs, the classpath cache, the pom stamp, the JVM arg file |

### CLI ↔ daemon

`<app>/.vaadin/daemon.properties` (`Handshake`) records the daemon's pid, process
start time, port and auth token. It is **outside `target/`** on purpose: `mvn
clean` must not orphan a running daemon and leave the next command spawning a
second one to fight for port 8080. The recorded start time is what stops a
recycled pid from passing for a live daemon.

Beside it, `.vaadin/daemon.lock` is held as an OS file lock for as long as the
daemon runs. Reading the record, binding a port and writing the record back is
three steps, and two simultaneous first invocations would otherwise each get
through them and each start an application on the same port. The lock makes it
one step, and the kernel releases it if the daemon dies, so there is nothing to
reap.

`install-dev-cli` puts the `vaadin-dev` scripts in that same directory, so
`.vaadin/` holds both committed tooling and per-run state. The token in the
handshake authorizes commands to the daemon, so the goal also installs
`.vaadin/.gitignore` naming `daemon.properties` — the one file there that must
not be shared.

The CLI spawns the daemon into a **session of its own** — `setsid`, or perl's
`POSIX::setsid` where no `setsid` binary ships — and not merely with SIGHUP
ignored. A `nohup`'d child stays in the process group of the shell that spawned
it, and the runners this CLI is driven from (agent sandboxes, CI steps, `timeout
--kill-after`) end a command by killing that group: the daemon, and the app it
owns as its child, would die with the very command that started them. The next
command would then find a handshake naming a dead pid, spawn a second daemon and
report the app as stopped — the loop failing to hold across two commands, which
is the one thing the daemon exists to do. A handshake left behind by a dead pid
is evidence of exactly that, because a daemon that shuts down deletes its own
record, so the CLI reports it rather than quietly reaping it.

A request is one line, `<token> <verb> <args...>`. The reply is zero or more
`> text` progress lines followed by exactly one `EXIT <code>`, which becomes the
CLI's exit status. Progress-then-code is the shape `apply` needs, so every verb
uses it.

Verbs: `ping`, `status [--json]`, `start`, `stop`, `restart`, `shutdown`,
`apply [--json] [--no-restart]`, `redefine <a.b.C,...>` (diagnostic — pushes
named classes with none of `apply`'s escalation policy), and `register`, which is
the connector's own connection rather than a command.

### daemon ↔ in-app connector

The connector (`com.vaadin.base.devserver.devloop` in `vaadin-dev-server`) opens
one socket and holds it for the app's lifetime, so its close *is* the signal that
the app is gone — no polling, no port probing. The daemon sends commands down the
same connection and reads one reply line:

```
REDEFINE a.b.C,a.b.D   ->  OK redefined=2 notLoaded=0 dupes=0 completed=true frontendImports=MyView …
RESOURCES /abs/a.css   ->  OK resources=1 pushed=1 browserReload=false ms=3
THEME /abs/styles.css  ->  OK themes=1 pushed=1 reloaded=false ms=7
RELOAD                 ->  OK reloaded=true
FRONTEND [/abs/dir]    ->  OK frontend=up:57231 mode=DEVELOPMENT_BUNDLE themes=my-theme agree=true
INFO                   ->  OK instrumentation=true redefineSupported=true …
PING                   ->  OK pong
anything that fails    ->  ERR kind=<kind> [class=…] message=<free text, last>
```

`FRONTEND` takes the frontend folder the daemon settled on as an *argument* rather than
returning it as a field, and for the same reason `message=` is last: a reply is split on
whitespace and a Windows path can contain a space. The app answers `agree=true|false`, and a
`false` is the first thing to look at when an edit is not being seen. `frontend=` keeps its
original meaning and position, so the parts of the daemon that read only that field were
untouched when the other fields were added.

`message=` is always last and takes the rest of the line, because it carries the
JVM's own words and those contain spaces (`Connector.fields`).

### Files

Under `<app>/target/devloop/`: `daemon.log` (the daemon's stdout — which is why
`System.out` here is correct and SLF4J would not be), `app.log` (the app's stdout
and stderr, the only place a "Port 8080 was already in use" exists), `cp.txt`
(the resolved classpath), `cp.stamp` (a fingerprint of every pom in the reactor),
`jvm-args.txt` (the app JVM's argument file — a reactor classpath is well past
Windows' 32 kB command-line limit). Each in-loop module gets its own
`<module>/target/devloop/cp.txt` and `model.properties`.

**`model.properties` is how the daemon knows what starts the application**,
written by `DevLoopBuildExtension` as each resolve reads the projects and read
back by `EffectiveModel`. Whether a profile is active, and what a module
inherits from a parent outside the checkout, are Maven's to decide and cannot
be worked out from the poms — so they are not guessed at: the extension writes
down the effective `<build><plugins>`, the packaging and the active profiles,
and that is the only source `Reactor.plugin` reads. A `<pluginManagement>`
version is absent from it by construction, and so is a plugin from a profile
that did not run. It is believed only while it is newer than the poms it was
built from, and `compose` resolves before it asks, so the answer is always
there by the time anything is launched. Its path is relative to the module and
never to `${project.build.directory}`: the daemon has no Maven to ask where a
project moved that to, so a moved build directory would leave the file written
in one place and looked for in another.

The HotswapAgent jar is *not* here: it is cached per machine under
`~/.vaadin/devloop/`, pinned by version and verified against a SHA-256, so one
download serves every application and a `mvn clean` does not throw it away.
`HotswapAgentJar` owns all three facts and is the only code that downloads
anything.

It is provisioned by `mvn flow:install-dev-cli`, not by the first `start`, so a
machine set up while it had network access can run the loop afterwards with
none — which is what a container image or a sandboxed agent environment needs.
The goal runs `HotswapAgentJar` out of the daemon jar the project resolves,
reflectively, over a class loader of its own: the plugin must not carry the
daemon into every build it runs in, and the version worth pre-downloading is the
one the daemon that will actually run has pinned. So a project whose daemon
predates this only gets a warning, and the goal and the `start` can never
disagree about the version. The daemon still provisions on demand through the
same code and into the same cache, so a project that never ran the goal is
unaffected and one that did never downloads twice.
`-Dvaadin.devcli.skipHotswapAgent=true` installs the CLI without it; dropping
the release asset into `~/.vaadin/devloop/` by hand works too, since what is
there is checksum-verified either way.

## Outcomes and exit codes

`TransactionEngine.Outcome` is the whole vocabulary, and the exit code is derived
from it rather than parsed out of text:

| Outcome | Exit | Means |
|---|---|---|
| `STABLE` | 0 | the change is live and the app is consistent |
| `COMPILED` | 0 | compiled, but nothing is running to apply it to |
| `NO_CHANGES` | 0 | nothing changed on disk since the last apply |
| `FAILED` | 1 | could not complete; the reason names the phase |
| `SUPERSEDED` | 4 | cancelled by a newer apply |

The CLI adds `64` (usage), `70` (internal / daemon unreachable) and `77`
(unauthorized) of its own.

Two rules make the question answerable at all: **at most one transaction in
flight**, and **supersede rather than queue** — a new `apply` cancels the
in-flight one and proceeds with the accumulated change-set, because only the
latest bytes on disk matter. The superseded caller still gets a terminal answer.

## Knobs

All are system properties on the daemon JVM
(`VAADIN_DEV_DAEMON_OPTS="-Dfoo=bar"` from the CLI). Any `vaadin.*` property the
daemon was started with is also forwarded to the app JVM, which is how
`-Dvaadin.frontend.hotdeploy=true` reaches Vite. So is every `spring.*`
property — the app is launched with no program arguments, so this is the only
way to hand it one:

```
.vaadin/vaadin-dev shutdown
VAADIN_DEV_DAEMON_OPTS="-Dspring.profiles.active=dev" .vaadin/vaadin-dev start
```

The `shutdown` is not optional. `VAADIN_DEV_DAEMON_OPTS` is read by the CLI only
when it *spawns* a daemon; every later command reuses the daemon that is already
running and ignores the variable, so setting it in front of `restart` changes
nothing. That applies to every property in this table, not only these.

Three are held back because the loop sets them itself, and a forwarded copy
would override rather than repeat them (a later `-D` wins):
`spring.devtools.restart.enabled`, `vaadin.launch-browser` and
`vaadin.devloop.classes`. Devtools is the one that matters — two things
restarting the application on their own schedules is what the transaction model
exists to prevent.

Anything set this way lives as long as the daemon and appears in no file, so it
is for steering one local run. What the project always needs belongs in its own
properties files, where the rest of the team can see it (see
`Launch.forwardedToApp`).

| Property | Default | Effect |
|---|---|---|
| `vaadin.dev.mainClass` | discovered | the class to launch (see `MainClass`) |
| `vaadin.dev.runtime` | discovered | how the app is started: `main`, `jetty-ee10`, `jetty-ee11`, `wildfly`, `tomee`, `payara`, `payara-micro`, `cargo` (see `AppRuntime`) |
| `vaadin.dev.reactorRoot` | discovered | when the reactor root is not an ancestor of the application |
| `vaadin.dev.modules` | auto | the edit loop by hand; `.` for the application alone |
| `vaadin.dev.frontend` | discovered | the frontend folder, when it is neither what the build recorded nor a conventional location (see `Frontend`) |
| `vaadin.dev.maven` | wrapper, then `PATH` | which Maven resolves the classpath |
| `vaadin.dev.mavenArgs` | none | extra arguments for the resolve, e.g. `-P!some-profile` |
| `vaadin.dev.javaHome` | the best JBR for the project (see `Jvm`) | which JVM runs the app |
| `vaadin.dev.hotswapAgentJar` | downloaded | an already-present HotswapAgent jar |
| `vaadin.dev.agentJar` | this jar | the javaagent, for a daemon run from an exploded build |
| `vaadin.dev.idleSeconds` | 1800 | shut down after this long idle with no app running |
| `vaadin.dev.startSettleMillis` | 15000 | how long a registered app has to report a listening server |
| `vaadin.dev.errorSettleMillis` | 400 | how long an apply follows the app log after a redefine |

## How the application is started

`AppRuntime` decides which of two shapes this project is, and for a WAR
which of the containers in `ServerPlugin.KNOWN` its build runs.

**An entry point** — a Spring Boot class, or any `public static void main` — is
launched directly as `java -cp <classpath> <MainClass>` (`MainClassRuntime`).

**A WAR** has no entry point, and its servlet container is a build plugin rather
than a dependency, so only the build knows how to start it. `MavenGoalRuntime`
asks it to. Jetty runs the application in the build's own JVM; WildFly, TomEE,
both Payaras and Cargo cannot, and the section after this one is about the
difference.

### A container that runs in the build's JVM

```
MAVEN_OPTS=<agents, opens, -XX:+AllowEnhancedClassRedefinition, -DdisabledPlugins>
JAVA_HOME=<the JDK Jvm chose>
  mvnw -B -ntp -nsu [-f <root>/pom.xml -pl :<app> -am] -Dmaven.test.skip=true
       [compile] <plugin>:<version>:run -Djetty.deployMode=EMBED -Djetty.scan=0
       <-D settings the application reads>
```

Four parts are load-bearing. `EMBED` (the plugin's default) runs the server in
the build's JVM, so the application is still a **direct child** of the daemon -
a fork would lose its exit code and orphan it. `MAVEN_OPTS` and `JAVA_HOME` are
the only way in, because the plugin's own `jvmArgs` applies to a fork only.
`-pl :app -am compile` is what makes a sibling module resolve to its
`target/classes` rather than an installed jar. And `-Dmaven.test.skip=true`
keeps the goal's forked `test-compile` from building tests the loop has no use
for.

The `-D` settings split in two: JVM flags can only be given to a starting JVM,
so they go in the environment, while settings the application reads go on the
Maven command line, where each is its own argument and a value with a space in
it survives. `disabledPlugins` is the exception - HotswapAgent reads it in
`premain`, before Maven sets anything - so it has to be a real JVM flag.

Which runtime a project gets, in order: `-Dvaadin.dev.runtime`; an entry point
the build *names* (a jar manifest `Start-Class`, or `@SpringBootApplication`);
a server plugin from `ServerPlugin.KNOWN`; then any `public static void main`.
The order matters both ways - a Spring Boot app can be packaged as a WAR, and a
WAR can carry an unrelated main method. Another container later is an entry in
that table.

### A container that forks

WildFly, TomEE, both Payaras and Cargo have no embedded mode: `wildfly:run`,
`tomee:run`, `payara-server:start`, `payara-micro:start` and `cargo:run` each
provision a server and start it as a **process of its own**.
Nothing on Maven's command line, and nothing in its environment, reaches a JVM
that Maven forked, so the agents, the JVM flags and the settings the application
reads all travel together in the one parameter each plugin hands on to that
process:

```
JAVA_HOME=<the JDK Jvm chose>
  mvnw -B -ntp -nsu [-f <root>/pom.xml -pl :<app> -am] -Dmaven.test.skip=true
       org.wildfly.plugins:wildfly-maven-plugin:<version>:run
       -Dwildfly.javaOpts="<agents, opens, -XX:...> <settings the app reads>"
```

All three deploy the packaged WAR rather than the module's own output, so one
has to exist. WildFly's goal declares `@Execute(phase = PACKAGE)` and forks the
packaging itself, which is why the command above names no phase; TomEE's and
Cargo's fork nothing, so `package` goes on their command line instead — and
naming it for WildFly too would only build the WAR twice. The parameter is
`wildfly.javaOpts`
for WildFly, whose mojo splits the value on whitespace, and `tomee-plugin.args`
for TomEE, which parses it the way a shell would — `javaagents` would read
better and carries no user property, so no command line can set it. WildFly's
`javaHome` defaults to `${java.home}`, so the server runs on the JVM Maven runs
on and the JBR carries over unasked; Cargo's `cargo.java.home` defaults the same
way.

### Apache Tomcat, through Cargo

Tomcat has no Maven plugin of its own a Vaadin project could use: Apache's
`tomcat7-maven-plugin` was last released in 2013 and runs a `javax.servlet`
container, which a Jakarta EE application cannot be deployed to at all. What the
ecosystem uses instead — this repository's own `flow-tests/servlet-containers`
included — is Codehaus Cargo, so the table entry is named `cargo` after the
plugin rather than after Tomcat, and it covers every other container Cargo
drives as well.

Cargo forks like the other two, but its flags cannot travel the same way. Every
parameter of `cargo:run` that could carry them — `<container>`,
`<configuration>` — is a nested element with no user property behind it, so no
`-D` reaches one. What Cargo *does* read is any **Maven project property** named
`cargo.*`, which it injects as a container configuration property, after the
pom's own `<properties>`. So the daemon asks the build extension to put one on
the model:

```
  mvnw ... -Dmaven.ext.class.path=<daemon jar>
       -Dvaadin.devloop.ext.property.cargo.jvmargs="<agents, -XX:...> <settings>"
       package org.codehaus.cargo:cargo-maven3-plugin:<version>:run
```

`cargo.jvmargs` and not `cargo.start.jvmargs`, which is what this said until a
container that is not Tomcat was tried. Cargo appends both to the command line
of the JVM it launches, and for Tomcat that JVM *is* the container, so either
name works and the start-only one leaves a project's own heap settings alone.
Cargo's GlassFish family is not that shape: there the JVM Cargo launches is the
`asadmin` client and the server is a process asadmin starts in turn, so
`AbstractGlassFishInstalledLocalContainer.startInternal` takes `cargo.jvmargs`
away from asadmin on purpose (CARGO-1255) and the standalone configuration
writes it into the domain's `domain.xml` as `<jvm-options>` instead.
`cargo.start.jvmargs` gets no such treatment and reaches the asadmin client
alone — so the agents were loaded into a command-line tool that exits, the
application ran without them, and every apply restarted with nothing to say why.
One name is correct for both families and this is it.

What that costs is the thing the other name avoided: `cargo.jvmargs` is
something a project may well have written for itself. So `DevLoopBuildExtension`
**adds to** a project property it finds rather than replacing it, the loop's
flags last — the same precedence `mavenOpts` applies to `MAVEN_OPTS`, and for
the same reason. A heap size the pom asks for is honoured, and the agents cannot
be switched off by one.

The value needs no backslash escaping: Cargo parses it with its own copy of
Ant's `translateCommandline`, where quotes group and a backslash is an ordinary
character.

The consequence is that this one runtime **needs** the extension rather than
merely benefiting from it. A daemon running from an exploded build directory has
no jar to point Maven at, so there is no channel at all and the server would
start without the agents; `MavenGoalRuntime.warnings` says so rather than
letting every apply quietly restart.

Readiness is read off Cargo's own line, `<name> started on port [<port>]`, and
not off the container's. That is what lets one entry answer for every container
Cargo drives, and it still works for a project that sends the container's output
to a file with `<container><output>`.

### Payara, both of them

Payara ships two runtimes and a project declares one or the other, so there are
two entries: `payara` for `fish.payara.maven.plugins:payara-server-maven-plugin`
and `payara-micro` for its `payara-micro-maven-plugin`. Both run the `start`
goal, neither declares an `@Execute`, so both name `package` themselves, and
both block on the forked process the way the loop needs.

**Never the `dev` goal.** Both plugins' `DevMojo` forces `autoDeploy` on, and
that is a watcher which re-invokes Maven and redeploys — the same competing
rebuilder `jetty.scan` and TomEE's `reloadOnUpdate` are switched off for. Payara
Server's `dev` also turns on `trimLog`, which rewrites every line the server
logs and so every line readiness is read from, and `aiAgent`, which turns the
process into a prompt reading standard input. `start` defaults all of them off;
the table's `Competing` entries only hold a pom that asks for them back.

Neither channel can be taken away by a pom, which is new — for WildFly and TomEE
a `<javaOpts>` or `<args>` in the pom beats the command line and the daemon can
only warn. Payara Server's `payara.javaCommandLineOptions` feeds a *second*
field that the mojo appends to the pom's list, and Payara Micro's `exec.args` is
read straight off the session's user properties, where a pom cannot reach at
all.

Two things about those channels are worth knowing before changing either.

**Payara Server drops any option with no `=` in it.** The mojo makes a key and a
value of each element at the first `=` and adds nothing when there is no value —
silently. That would take `-XX:+AllowEnhancedClassRedefinition` with it, and
with it enhanced class redefinition. It survives because the flags are handed
over as *one* whitespace-separated value: the split at the first `=` and the
`key=value` that rebuilds it are exact inverses, so the value round-trips byte
for byte and the server's own `JavaUtils.parseParameters` tokenizes it back at
the far end. The requirement is only that the value contain an `=` somewhere,
which the loop's settings always do.

**And Maven splits that same value on commas**, the parameter being declared
`List<String>` — a bare comma split in Plexus's converter, with no escaping
available. Combined with the rule above, a comma does not divide a flag, it
deletes most of it: `-DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty` would
arrive as `-DdisabledPlugins=Vaadin`. So `ServerPlugin.commaSplitFlags` marks
that channel, and `MavenGoalRuntime` moves every comma-bearing flag into a JVM
argument file under `target/devloop/payara-args.txt`, passing `@<file>` in its
place — the JVM expands that itself, and an argument file quotes a comma without
trouble. The flags that need no file stay inline, where the launch line shows
them.

Payara Micro has neither problem: `exec.args` is a plain string, split on
whitespace and nothing else. It has no escaping either, so a Windows path passes
through as it stands and one containing a space cannot be passed at all —
`MavenGoalRuntime.unsplittable` says so. It is also **undocumented**: there is
no constant behind it, the literal name is inline in the mojo, and a Payara
release that dropped it would leave the server starting with no agents and every
apply restarting. The launch line the daemon logs is where to look first.

Readiness differs between the two, and not arbitrarily. Payara Micro logs the
kernel's own `Network Listener http-listener started in: 49ms - bound to
[/0.0.0.0:8080]`, which is one line and carries the port; the listener name is
matched literally so an HTTPS one cannot answer for it, and the elapsed time is
stepped over because `MessageFormat` groups it by locale. Payara Server's domain
is started with no `--verbose`, so that line may go only to the domain's own
`server.log` and never reach the daemon — readiness there is the *plugin's*
line, `<name> application deployed successfully : http://host:port/ctx`, which
is logged only after the admin endpoint has answered and the deployment has
succeeded. That is a stronger signal than a bound socket, and it is the same
reasoning Cargo's entry uses: read the plugin, not the container.

One consequence for a test fixture, and it is why there is none: **Payara
Server's HTTP port cannot be chosen from the command line.** `payara.http.port`
only tells the plugin which port to talk to; the listener itself comes from the
domain's `domain.xml`. Payara Micro's can — `-Dexec.args=-Dpayaramicro.port=<n>`
— because Micro reads its own configuration from system properties.

### What a deployed WAR costs a hot swap

All three forked containers deploy a *copy* of the WAR, and the webapp class
loader reads that copy rather than the module's `target/classes`. A hot swap
acts on classes already loaded, so it is unaffected — but a class the
application has not loaded *yet* still loads its pre-edit bytes from the
deployed copy, until the next restart rebuilds and redeploys the WAR. Measured
against Tomcat: an `apply` on a view's service class reported
`redefineClasses(0)` because the page had never been opened, and the value that
then rendered was the deployed one. Once the page had been opened, the same edit
hot-swapped in under a second and the new value rendered without a reload.
Jetty never shows this, because there the webapp class loader *is*
`target/classes`.

Module options go in as `--add-opens=<module>/<package>=<target>`, one token
rather than two. WildFly sorts module options apart from the rest before it
builds the server's command line, and the two-token form comes apart in the
sorting — measured, seven `--add-opens` arrived ahead of their seven values and
the JVM refused to start at all. `MavenGoalRuntime.singleToken` folds them.

Three consequences. The application is a **grandchild** of the daemon: stopping
it is still reliable, because `AppProcess` ends a launch descendants-first, but
its own exit code is lost — the code the daemon waits on is Maven's. A pom that
pins `<javaOpts>` or `<args>` in `<configuration>` beats the command line and
would silently drop the agents; that one the build extension cannot rewrite for
you, because the value it would have to write is composed per launch, so the
daemon warns about it instead. And the **first** `start` on a WildFly project
provisions a server under `target/` before it can start one, and a Cargo project
downloads and unpacks one, which is far slower than any boot — so
`AppRuntime.startupTimeout` is a runtime's own to declare, and a forked container
asks for twenty minutes where an embedded one gets five.

## Which JVM runs the app

`Jvm` owns it, and the answer is not simply "a JBR". Enhanced class redefinition is a
JVM feature, so a JBR is what makes a structural change hot-swappable at all — but a
JBR too old for the project cannot run the application, and that is the worse failure.
So the project's required version is worked out first and the JBR is chosen against it.

Where the requirement comes from, in order:

1. **The poms** — `maven-compiler-plugin`'s `<configuration>`, then
   `maven.compiler.release` / `target` / `source`, then `java.version`; the application
   module first, the reactor root second. `Reactor.requiredRelease()`.
2. **The compiled bytecode** — the major version of the first class under
   `target/classes`. This is the answer for a project that inherits its level from a
   parent outside the checkout, which is every `spring-boot-starter-parent` project
   that leaves `java.version` alone. The poms cannot see that; the class files can.

**Java 21 is a floor, not a preference.** Flow requires it, so a JVM below 21 is
dropped during discovery and never ranked, and a project declaring 17 is still run on
21 or above. The floor applies to the JVM only — javac is still told the project's own
release, because compiling a 17-target project at 21 would let code through the dev
loop that Maven then rejects.

Candidates are every directory under `~/.jdks` and under `~/.vaadin/jdk`, plus
`JAVA_HOME` and `JDK_HOME`. The second of those is where the Vaadin plugins for
IntelliJ IDEA, VS Code and Eclipse install the JetBrains Runtime they offer to
download, so a developer who took that offer already has the JVM this loop wants —
without it the whole session would run on a stock JDK with a JBR sitting on disk. Each
candidate's version and vendor are read from its own `release` file
(`IMPLEMENTOR="JetBrains s.r.o."` is what makes it a JBR) rather than guessed from its
directory name — which is how `jbr-9` used to outrank `jbr-21`. The JBR closest above
the requirement wins; failing that, the closest JDK, and the log says what that cost.

## The frontend leg

`Frontend` owns it: where the frontend folder is, and what a change under it
means. The rules live there rather than in `TransactionEngine` because
`Frontend` is constructible from a directory and a string, so every one of them
is unit-testable.

**Finding the folder**, in order: `-Dvaadin.dev.frontend`,
`-Dvaadin.frontend.folder` (which `Launch` forwards to the app, so the two
agree by construction), `frontendFolder` out of
`target/classes/META-INF/VAADIN/config/flow-build-info.json`, then
`src/main/frontend` and `frontend/`. The build-info token beats the convention
because Flow wrote it after resolving both the legacy fallback and the plugin's
`<frontendDirectory>`, and it is readable before the app has ever started.
Nothing else is read from that file: it also records `frontend.hotdeploy`,
which describes the build rather than the mode the app is running in.

**Which mode the app is in decides everything else**, and only the app can
answer it, so `FRONTEND` is asked once per apply and reused by every leg.

- **Vite mode** — Vite's root *is* the frontend folder, so it applied the edit
  on save and the daemon cannot suspend it as it suspends Flow's own watchers.
  Nothing is pushed and nothing escalates; `apply` names the files and says
  Vite did it. A Vite compile error is therefore a `FAILED` apply rather than a
  footnote under `Stable` (`devServerFailure`), and does not escalate — a
  restart cannot compile a broken module.
- **Dev-bundle mode** — theme CSS goes through `ThemeLiveUpdater.push`, the
  call Flow's own watcher makes on save; `index.html` and theme assets are
  served from the folder and need only a reload. Everything else is in the
  bundle, so `apply` restarts and the startup path (`NodeTasks` →
  `BundleValidationUtil.needsBuild` → `TaskRunDevBundleBuild`) rebuilds it. The
  restart is the mechanism here, not a fallback.

**A frontend annotation on a Java class escalates too.** `@JsModule`,
`@JavaScript`, `@CssImport`, `@NpmPackage` and `@Theme` are read by the build
into `generated-flow-imports.js`, and the browser reaches them through a bundle
chunk keyed by class name — so the redefine succeeds, the class really does
carry the new annotation, and the import is in no chunk the client can load.
`REDEFINE` reports `frontendImports=<classes>`, compared before and after
through `AnnotationReader`, and `blockedReason` escalates. No file under the
frontend folder need have changed, which is why the tree alone cannot catch it.
`@StyleSheet` is excluded: those are already live through
`StyleSheetHotswapper`.

**Every non-theme frontend file is treated as bundled**, imported or not:
reading `stats.json` would be a dependency, so over-restarting is the honest
error to make. Deletions are tracked here and not for Java sources,
`frontendNotified` being a complete inventory, and a removed module the bundle
still imports breaks the next build. The baseline is re-seeded on every
registration (`seedFromDisk` → `seedFrontend`), without which the file that
caused a restart would be offered again after it, for ever.

**A Vite compile error is found by asking the dev server, not by overhearing
it.** Vite compiles a module when something *requests* it, so the log holds a
report only if a browser happened to re-fetch while the daemon was watching —
and a page already showing the overlay does not re-fetch at all. So
`FRONTEND_CHECK <paths>` has the connector fetch each changed file through
`DevModeHandler.prepareConnection`, on the base Vite was launched with
(`ViteHandler.getPathToVaadin()`, so a context path works too). A `500` is a
refusal carrying Vite's own message, a `200` means the module compiles, and a
`404` means the path is not served at all and counts for nothing. Paths are
joined with the unit separator (`U+001F`), a comma being legal in a Unix path.
Each probe makes Vite compile on demand, so a timeout or a reset comes back
*inconclusive* rather than as "served" and the verdict falls back to the log.

A `200` answers only what it was asked — can this module be *served*. Types are
stripped without being checked, so a type error, or a stray `>` in JSX that oxc
tolerates and `tsc` does not, comes back `200` and is still wrong. That failure
exists only in the log, where `vite-plugin-checker` writes it (`CHECKER_ERROR`,
told apart from `DEV_SERVER_ERROR` for exactly this reason) within about a
hundred milliseconds of the *save* — which is why its errors are carried across
`Watch.mark()` like the dev server's. Fatal only when the change-set touched a
frontend file, so a Java-only edit is never failed by a type error somebody
else left behind.

The answer is authoritative **in both directions**: a refusal fails the apply
(`Transaction.devServerRefusal`) and leaves the file unmarked, so the next
apply asks again instead of reporting `no changes` over a module that never
compiled, and a clean answer *overrules the log*
(`Transaction.devServerAsked`), because the report still sitting there
describes the version before the fix. The overruling is scoped to transform
errors, the only ones the fetch answers for. The log stays the fallback for a
dev server that cannot be asked, and it takes finding: Flow pipes Vite's output
through `DevServerOutputTracker` at `INFO`, so `AppLog` matches those openers
separately (`DEV_SERVER_ERROR`), and errors logged on save are carried across
`Watch.mark()` (`Transaction.carriedLogErrors`).

Either way the report is compacted to three parts — the opening line, the line
naming the error, the source position — and **wrapped, not truncated**, the
excerpt, caret diagram and JavaScript stack being dropped
(`REPORT_DECORATION`). The parts are joined with a unit separator rather than a
`|`, which an ASCII excerpt draws as its own gutter; `--json` and the one-line
reason put ` | ` back. `quote` and `reasonRows` wrap at 100 columns, letting a
long path overflow a row rather than be cut in half.

**Vite mode is verified by hand**, because `hotdeploy` is baked into the app
JVM from the daemon's own system properties and `flow-tests/test-devloop`
shares one long-lived daemon; the decision logic itself is covered by
`FrontendTest` in both modes.

```bash
cd <app>
.vaadin/vaadin-dev shutdown
#   read only when a daemon is spawned, so a running one would ignore it
VAADIN_DEV_DAEMON_OPTS="-Dvaadin.frontend.hotdeploy=true" .vaadin/vaadin-dev start
#   edit src/main/frontend/<something>.ts, then:
.vaadin/vaadin-dev apply
#   expect: hmr: N frontend file(s), applied by Vite (dev server up:<port>)

#   break the same file - a missing brace is enough - with nothing open in a
#   browser, so the log stays silent about it:
.vaadin/vaadin-dev apply
#   expect: exit 1, "dev server: <file>: Transform failed ...", and the same
#   answer on a repeat apply and after re-saving the file still broken
#   then fix it:
.vaadin/vaadin-dev apply
#   expect: exit 0, Stable - the report left in the log must not fail this one
```

## Deletions

A walk sees only what is there, so a deletion is found against the fingerprint
inventory instead; a file created and deleted without a restart in between was
never seeded, so its output is left for the next build to clear.

**A deleted resource is un-copied** from `target/classes`. A public one then
gets a `RESOURCES` call with nothing to push, which reloads the page; a startup
one escalates as an edit to it would.

**A deleted Java source is un-compiled, and then restarted** — a JVM cannot
un-define a class it has loaded, and removing `Foo.class` and the `Foo$…class`
files javac named after it is what stops the restart loading them straight
back. Missed, this is the loudest failure in the change-set: a removed route,
bean or entity goes on answering out of its stale `.class` under a `no changes`
exit 0. The deletion stays in the change-set until a restart re-seeds the
inventory.

## HotswapAgent plugins

`Vaadin`, `Spring`, `SpringBoot` and `Jetty` are disabled
(`Launch.DISABLED_HOTSWAP_PLUGINS`), and nothing else: the Vaadin one fires a
competing full page reload, the Spring ones were measured to lose the Spring
Data repository bean under repeated redefinitions, and Jetty's hooks name
`org.eclipse.jetty.webapp` and `org.mortbay` classes, which match nothing under
Jetty 12.

**Disabling has to reach the right class loader**, because HotswapAgent reads
`hotswap-agent.properties` off the class's *own* loader — so
`-DdisabledPlugins` never reached a webapp loader inside a build plugin's
realm. `MavenGoalRuntime` therefore also writes the list into the app's
`target/classes`, and with it an `extraClasspath` naming the HotswapAgent jar,
without which that loader cannot see the agent itself; honouring the setting
also needs `java.base/java.net` and `java.base/jdk.internal.loader` opened. The
key is `disabledPlugins`, plural and unprefixed; a wrong one is accepted
silently.

## Under a build-plugin runtime

A WAR runs inside a Maven build rather than beside one, so every restart costs
a Maven invocation — seconds rather than a fraction, partly paid back because
the daemon has already compiled into `target/classes`. Three things follow.

**A pom's `<scan>` or `<deployMode>` would beat the dev loop, so the daemon
rewrites them** — the same extension that records the effective model, in its
other job. A `<configuration>` value wins over the user property the same
parameter exposes, so `-Djetty.scan=0` does nothing to the `<scan>2</scan>` a
generated WAR starter writes, and the plugin was measured redeploying the
webapp underneath an `apply` that had reported a clean hot swap. The daemon
puts its own jar on `maven.ext.class.path` and `DevLoopBuildExtension` edits
the effective model in memory before any mojo runs; nothing is written to the
project, and the override is announced in the app log. From an exploded build
directory there is no jar to point Maven at, and it can only warn.

**The build's class loaders are in the application's JVM**, so a second copy of
every class the Vaadin Maven plugin scans is live in it. That loader is kept
out of HotswapAgent (`MavenGoalRuntime.extraJvmFlags`), and the connector
compares a class to its own previous state by `Class` identity rather than by
name — keyed by name, one copy's "before" met the other's "after". The
duplicates are still redefined, which is what
`N duplicate class copy/copies also redefined` reports.

**The daemon does not own the classpath**: the webapp loader is the plugin's to
assemble, so `Launch.assemble`'s removal of a superseded jar does not apply,
and a sibling module resolves to `target/classes` only because of the
`-pl :app -am compile` in the command. JVM flags reach the app through
`MAVEN_OPTS` alone, which Maven expands unquoted, so a path with a space is
split by the shell — `MavenGoalRuntime.unsplittable` names the flag at launch
rather than leaving a JVM that will not start.

## Known limits

Why `apply` escalates or qualifies its answer rather than claiming success.
None of them is a bug in this module.

### In the running JVM

- **JPA entity mappings do not hot-reload**, with or without HotswapAgent:
  Hibernate's metamodel and schema are fixed at startup. The connector reports
  the entity classes and `apply` refuses to call the change live, reading
  `@Entity` out of the compiled bytes rather than off the loaded class, whose
  reflective view HotswapAgent refreshes on a schedule of its own.
- **A structural change to a proxied Spring bean must restart** — the live
  proxy was generated against the old shape, though a method-body change is
  fine. That includes Spring Data repositories, bare interfaces with no
  annotation to spot them by, so the connector keys on the loaded proxy
  instead.
- **A bean or an entity the application has never seen must restart too.**
  Component scanning runs once, at startup, and the HotswapAgent plugins that
  would rescan are disabled (above), so a class only now being given
  `@Component`, `@Service`, `@Repository`, `@Controller`, `@RestController`,
  `@ControllerAdvice`, `@RestControllerAdvice` or `@Configuration` gets no bean
  definition and fails at the first injection point with
  `NoSuchBeanDefinitionException`; a brand-new `@Entity` is in the same
  position against a metamodel fixed at startup. Nothing was redefined, so it
  takes both sides to spot: `REDEFINE` reports which classes carry a stereotype
  (`stereotypes=`) and the daemon's own inventory which of them the app never
  had. The known gap is a stereotype composed through a project's own
  meta-annotation — only the custom annotation is in the constant pool, so that
  one is a restart to ask for by hand.
- **Hot-swap coverage differs sharply between stock HotSpot and a JBR.** Only a
  JBR gets `-XX:+AllowEnhancedClassRedefinition`; on stock HotSpot a structural
  change is simply rejected and escalates. A project needing a Java version no
  installed JBR provides runs on a stock JDK for the whole session, and the
  launch log says so.
- **A sibling module is in the loop only while the application depends on it**,
  and its classes then follow the rules above: `@JsModule` and `@NpmPackage`
  escalate as frontend annotations do anywhere, a never-loaded class as any
  other new bean or entity would.

### In the in-loop compile

- **An edit that changes what a class promises its callers is not supported.**
  Only the change-set goes to javac, so callers nobody edited keep the bytecode
  they were compiled with: rename or re-sign a method, move a supertype, or
  change a `static final` constant, and they are stale — a `NoSuchMethodError`,
  or for an inlined constant the old value and no error at all — while `apply`
  reports `Stable` and the restart loads the same class files. Recompile with
  Maven; a dependency-aware compile is not implemented.
- **Annotation processors are not run** (`-proc:none`), so a Lombok-,
  MapStruct- or Dagger-backed source recompiled here loses every member and
  every class the processor would have generated, and what runs is bytecode a
  normal build would never have produced. Such a project needs `mvn compile`
  rather than `apply`.
- **The compiler plugin's configuration is not read**, except for the release
  level. The option list is fixed — `--release`, `-encoding UTF-8`, `-nowarn`,
  `-proc:none`, `-parameters`, `-g` — so `<compilerArgs>`, `--enable-preview`
  and `-Werror` are not honoured. The last two are unconditional because a
  normal build has both on and both live in a parent outside the checkout;
  without `-parameters` a recompiled Spring Data repository throws about
  missing parameter names, from code nobody edited.
- **`target/classes` is shared with Maven, and the daemon writes into it
  last.** A class newer than its source makes `mvn compile` a no-op, so
  `mvn verify` after a session tests whatever the in-loop compile did
  differently. `mvn clean` is the recovery.

### Outside the change-set

- **Only resources under a public root can be made live.**
  `META-INF/resources/`, `static/`, `public/` and `resources/` are served from
  the classpath per request, so the copy into `target/classes` plus a
  `RESOURCES` push is the whole of the work. Everything else —
  `application.properties` first among them — was read while the app was
  starting and is never re-read, so `apply` copies it, to keep the classpath
  honest, and then escalates.
- **`src/main/webapp` is not watched.** A container serves it off disk, so an
  edit there is already live but `apply` neither mentions it nor reloads the
  page, and a `WEB-INF/web.xml` edit needs a `restart` asked for by hand. A
  modern Vaadin WAR keeps static files under `META-INF/resources`, which is
  tracked normally.

## Tests

`src/test/java` covers what is decidable without a running application: `Json`,
`Handshake`, `Reactor` discovery against pom fixtures, `AppRuntime`, `AppLog`,
`AppProcess`, `Launch.membership`, `Compile`'s per-module grouping (with a real
javac), `Frontend` in both frontend modes, `Jvm`, `HotswapAgentJar`,
`MainClass`, and the verdicts `TransactionEngine` reaches without an app. The
loop itself is tested in `flow-tests/test-devloop`: `test-devloop-spring` for
an entry point and `test-devloop-jetty` for a WAR under its own build plugin,
over the shared ITs in `test-devloop-support`.

**WildFly, TomEE and both Payaras have no fixture**, only the table's own unit
tests. What those cannot answer is the one question that matters for a forked
container — did the agents reach the server's JVM — so that is verified by hand
against a WAR whose pom runs the plugin in question:

```bash
.vaadin/vaadin-dev start
.vaadin/vaadin-dev status     # the runtime named here must be the intended one
#   target/devloop/app.log holds the readiness line the entry's pattern expects,
#   and the "flags:" line holds the whole value handed to the plugin - for
#   payara, check target/devloop/payara-args.txt exists and holds the flags
#   with commas in them
#   edit a method body in a class the running page has already loaded, then:
.vaadin/vaadin-dev apply
#   expect: exit 0 and hot-reload, NOT "restarting". A restart here is the
#   symptom of every way this can go wrong - the flags dropped, split on a
#   comma, or sent to a JVM that was not the server's.
```

A structural edit — adding a method — escalating to a restart *on a JBR* is the
particular symptom of `-XX:+AllowEnhancedClassRedefinition` having been dropped,
which is the failure the one-value trick above exists to prevent.
