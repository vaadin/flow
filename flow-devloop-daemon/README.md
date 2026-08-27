# flow-devloop-daemon

A long-running daemon that owns one Vaadin application's edit-to-running-app
loop: background compilation, hot swap or restart, browser refresh, error
reporting. It answers, authoritatively, *"what is the state of my last
change?"* — an agent batches edits, runs one `apply`, and reads a verdict from
the exit code instead of rebuilding, relaunching and guessing from screenshots.

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

`install-dev-cli` puts the `vaadin-dev` scripts in that same directory, so
`.vaadin/` holds both committed tooling and per-run state. The token in the
handshake authorizes commands to the daemon, so the goal also installs
`.vaadin/.gitignore` naming `daemon.properties` — the one file there that must
not be shared.

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
REDEFINE a.b.C,a.b.D   ->  OK redefined=2 notLoaded=0 dupes=0 completed=true …
RESOURCES /abs/a.css   ->  OK resources=1 pushed=1 browserReload=false ms=3
FRONTEND               ->  OK frontend=up:57231
INFO                   ->  OK instrumentation=true redefineSupported=true …
PING                   ->  OK pong
anything that fails    ->  ERR kind=<kind> [class=…] message=<free text, last>
```

`message=` is always last and takes the rest of the line, because it carries the
JVM's own words and those contain spaces (`Connector.fields`).

### Files

Under `<app>/target/devloop/`: `daemon.log` (the daemon's stdout — which is why
`System.out` here is correct and SLF4J would not be), `app.log` (the app's stdout
and stderr, the only place a "Port 8080 was already in use" exists), `cp.txt`
(the resolved classpath), `cp.stamp` (a fingerprint of every pom in the reactor),
`jvm-args.txt` (the app JVM's argument file — a reactor classpath is well past
Windows' 32 kB command-line limit). Each in-loop module gets its own
`<module>/target/devloop/cp.txt`.

The HotswapAgent jar is *not* here: it is cached per machine under
`~/.vaadin/devloop/`, pinned by version and verified against a SHA-256, so one
download serves every application and a `mvn clean` does not throw it away.

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
`-Dvaadin.frontend.hotdeploy=true` reaches Vite.

| Property | Default | Effect |
|---|---|---|
| `vaadin.dev.mainClass` | discovered | the class to launch (see `MainClass`) |
| `vaadin.dev.reactorRoot` | discovered | when the reactor root is not an ancestor of the application |
| `vaadin.dev.modules` | auto | the edit loop by hand; `.` for the application alone |
| `vaadin.dev.maven` | wrapper, then `PATH` | which Maven resolves the classpath |
| `vaadin.dev.mavenArgs` | none | extra arguments for the resolve, e.g. `-P!some-profile` |
| `vaadin.dev.javaHome` | a JBR if present, else this JVM | which JVM runs the app |
| `vaadin.dev.hotswapAgentJar` | downloaded | an already-present HotswapAgent jar |
| `vaadin.dev.agentJar` | this jar | the javaagent, for a daemon run from an exploded build |
| `vaadin.dev.idleSeconds` | 1800 | shut down after this long idle with no app running |
| `vaadin.dev.startSettleMillis` | 15000 | how long a registered app has to report a listening server |
| `vaadin.dev.errorSettleMillis` | 400 | how long an apply follows the app log after a redefine |

## Known limits

Operational facts a maintainer would otherwise rediscover the hard way. None of
them is a bug in this module; all of them are why `apply` escalates or qualifies
its answer rather than claiming success.

- **JPA entity mappings do not hot-reload**, with or without HotswapAgent.
  Hibernate's metamodel and schema are fixed at startup. The connector reports
  the entity classes involved and `apply` refuses to call the change live.
- **A structural change to a proxied Spring bean must restart.** A method-body
  change inside a bean is fine — the proxy delegates and the new body runs. A
  change to the class's *shape* is not: the live proxy was generated against the
  old one. This includes Spring Data repositories, which are bare interfaces with
  no annotation to spot them by, so the connector keys on the loaded proxy
  instead.
- **Hot-swap coverage differs sharply between stock HotSpot and a JBR.** Only a
  JBR gets `-XX:+AllowEnhancedClassRedefinition`; on stock HotSpot a structural
  change is simply rejected and escalates.
- **A sibling module contributing routes, `@JsModule` or `@NpmPackage` needs a
  restart**, not an `apply`: those are read at startup.
- **HotswapAgent's `Vaadin`, `Spring` and `SpringBoot` plugins are disabled**
  (`Launch`, `-DdisabledPlugins=…`). The Vaadin one targets an older package and
  fires a competing full page reload; the Spring ones were measured to lose the
  Spring Data repository bean under repeated redefinitions, after which the app
  throws while the redefine still reported success. The property name is
  `disabledPlugins`, plural and unprefixed — a wrong name is accepted silently
  and disables nothing.

## Tests

`src/test/java` covers the pure logic only — `Json`, `Handshake`, `Reactor`
discovery against pom fixtures, `AppLog` failure-reason extraction,
`Launch.membership`, `Compile`'s per-module grouping (with a real javac), and
`MainClass` discovery. The end-to-end loop is tested in
`flow-tests/test-devloop`, which drives the installed CLI against a real
application.
