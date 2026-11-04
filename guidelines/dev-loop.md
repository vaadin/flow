# Dev Loop

The `vaadin-dev` dev loop owns one application's edit-to-running-app loop:
background compilation, hot swap or restart, browser refresh and error
reporting. It answers, authoritatively, *"what is the state of my last
change?"* — an agent batches edits, runs one `apply`, and reads a verdict from
the exit code instead of rebuilding, relaunching and guessing from screenshots.

This chapter explains the decisions every change to the loop has to keep; the
rules themselves are in the Dev Loop section of
[`CONVENTIONS.md`](../CONVENTIONS.md). How each part is implemented — the
connector protocol, the files under `target/devloop/`, how each server runtime
is launched, which JVM runs the app, the frontend leg, compiling with Maven and
the known limits — is in
[`flow-devloop-daemon/README.md`](../flow-devloop-daemon/README.md). What users
are told is the `vaadin-devloop` skill and the help text of the `vaadin-dev`
launchers, all under
[`flow-plugins/flow-plugin-base/src/main/resources/vaadin-dev-cli/`](../flow-plugins/flow-plugin-base/src/main/resources/vaadin-dev-cli/).
A behavior none of these describe is not a decision anyone made, and a change
that relies on it should write it down.

## The pieces

- **The launchers** — `vaadin-dev` (bash), `vaadin-dev.ps1` (PowerShell) and
  the `vaadin-dev.cmd` shim that runs the PowerShell one. `mvn
  vaadin:install-dev-cli` installs them into the application's `.vaadin/`,
  together with the skill. Both launchers carry the same contract, and their
  usage text is the same word for word.
- **The daemon** — `flow-devloop-daemon`, one per application. Its jar is also
  the javaagent loaded into the application JVM.
- **The connector** — `com.vaadin.base.devserver.devloop` in
  `vaadin-dev-server`, running inside the application. It holds one socket to
  the daemon for the application's lifetime, so its close is the signal that
  the application is gone.
- **The build extension** — `DevLoopBuildExtension`, loaded into the Maven runs
  the daemon starts. It records the effective model and rewrites the
  configuration of a server plugin for the loop's run.

The whole module is internal: it may be renamed or removed in a future
release, and the only supported surface is the CLI.

## Zero dependencies

The daemon depends on the JDK and nothing else — not SLF4J, not Jackson. That is
why it is a module of its own rather than a package inside `vaadin-dev-server`:
a module boundary can be enforced, and a `maven-enforcer-plugin`
`RestrictImports` execution in its `pom.xml` does enforce it.

Two consequences follow, and both are deliberate:

- The daemon starts in tens of milliseconds, which is what makes
  `vaadin-dev status` cost milliseconds rather than a JVM boot.
- It can never drag the application's classpath into the daemon JVM. The daemon
  reads the app's class files (`MainClass`), compiles its sources (`Compile`) and
  redefines its classes through the in-app connector — but never loads one.

So `Json` hand-rolls its JSON escaping, `Reactor` reads poms with the JDK's own
XML parser, and `MainClass` walks a class file's constant pool instead of
calling `Class.forName`. None of those is an oversight, and none should be
replaced by a library.

The build plugin does not depend on the daemon either: `install-dev-cli` runs
code out of the daemon jar the project resolves, reflectively, over a class
loader of its own, as the Build & Dependencies rules require of any dev-only
jar.

## One daemon, outliving the command that started it

The loop holding across commands is the one thing the daemon exists to do, and
three decisions protect it.

**The handshake lives outside `target/`.**
`<app>/.vaadin/daemon.properties` records the daemon's pid, process start time,
port and auth token. `mvn clean` must not orphan a running daemon and leave the
next command spawning a second one to fight for port 8080. The recorded start
time is what stops a recycled pid from passing for a live daemon. The token
authorizes commands, so `install-dev-cli` also installs a `.vaadin/.gitignore`
naming the handshake: `.vaadin/` holds both committed tooling and per-run
state.

**A lock makes the first start one step.** Beside the handshake,
`.vaadin/daemon.lock` is held as an OS file lock for as long as the daemon runs.
Reading the record, binding a port and writing the record back is three steps,
and two simultaneous first invocations would otherwise each get through them
and each start an application on the same port. The kernel releases the lock if
the daemon dies, so there is nothing to reap.

**The daemon runs in a session of its own** — on Unix `setsid`, or perl's
`POSIX::setsid` where no `setsid` binary ships — and not merely with SIGHUP
ignored. A `nohup`'d child stays in the process group of the shell that spawned
it, and the runners this CLI is driven from (agent sandboxes, CI steps,
`timeout --kill-after`) end a command by killing that group: the daemon, and
the app it owns as its child, would die with the very command that started
them. The next command would then find a handshake naming a dead pid, spawn a
second daemon and report the app as stopped. A daemon that shuts down deletes
its own record, so a handshake left behind by a dead pid is evidence of exactly
that failure, and the CLI reports it rather than quietly reaping it.

## The command contract

A request is one line, `<token> <verb> <args...>`. The reply is zero or more
`> text` progress lines followed by exactly one `EXIT <code>`, which becomes
the CLI's exit status. Progress-then-code is the shape `apply` needs, so every
verb uses it. The verbs and their arguments are listed in the daemon README.

The exit code is what an agent acts on, so it is derived from
`TransactionEngine.Outcome` rather than parsed out of text, and its meanings do
not change:

| Outcome      | Exit | Means                                           |
| ------------ | ---- | ----------------------------------------------- |
| `STABLE`     | 0    | the change is live and the app is consistent    |
| `COMPILED`   | 0    | compiled, but nothing is running to apply it to |
| `NO_CHANGES` | 0    | nothing changed on disk since the last apply    |
| `FAILED`     | 1    | could not complete; the reason names the phase  |
| `SUPERSEDED` | 4    | cancelled by a newer apply                      |

The CLI adds `64` (usage), `70` (internal, or the daemon is unreachable) and
`77` (unauthorized) of its own.

Two rules make the question answerable at all: **at most one transaction in
flight**, and **supersede rather than queue**. A new `apply` cancels the
in-flight one and proceeds with the accumulated change-set, because only the
latest bytes on disk matter. The superseded caller still gets a terminal
answer, so no caller is left waiting.

## Settings are fixed when the daemon is spawned

Everything that steers the loop is a system property of the daemon JVM, given
as `VAADIN_DEV_DAEMON_OPTS="-Dfoo=bar"`, and the application JVM inherits the
environment of the daemon. The CLI reads `VAADIN_DEV_DAEMON_OPTS` only when it
*spawns* a daemon; every later command reuses the daemon that is already
running and ignores the variable, so setting it in front of `restart` changes
nothing. Changing a setting therefore takes a `shutdown` first:

```
.vaadin/vaadin-dev shutdown
VAADIN_DEV_DAEMON_OPTS="-Dspring.profiles.active=dev" .vaadin/vaadin-dev start
```

A setting given this way lives as long as the daemon and appears in no file, so
it is for steering one local run. What the project always needs belongs in its
own properties files, where the rest of the team can see it. A value that has
to change between launches without a `shutdown` is a command option instead,
the way `start --jvm-args` carries JVM flags for the application.

## The loop is the only thing that restarts the application

The transaction model only holds while nothing else rebuilds, redeploys or
reloads the application on a schedule of its own: an `apply` that reported a
clean hot swap while a server plugin redeployed the webapp underneath it has
told the agent something false. So every runtime the loop supports has its own
watchers switched off for the loop's run — Spring Boot devtools restarts,
Jetty's `scan`, TomEE's `reloadOnUpdate`, Liberty's application monitor (by
deploying a packaged WAR instead of a loose application), and the `dev` goals of
the Payara and Liberty plugins, which are never run. The HotswapAgent plugins
that fire a competing page reload or act on Spring beans behind the loop's back
are disabled for the same reason. The daemon README lists each one and how it is switched off.

## Ask the build, do not guess

Whether a profile is active, what a module inherits from a parent outside the
checkout, and which configuration a plugin ends up with are Maven's to decide,
and they cannot be worked out from the poms. The daemon therefore reads them
from what the build itself recorded: the effective model that
`DevLoopBuildExtension` writes to `model.properties` while the classpath is
resolved, the frontend folder that Flow wrote to `flow-build-info.json`, the
release level of the compiled class files. A guess that matches the common
project is wrong for the one with a parent outside the checkout, and that one
fails with no clear reason.

## Say what happened, not what was hoped

An `apply` that reports `Stable` for a change that is not live is the worst
answer the loop can give, because the agent then debugs code that is correct.
So a change the running JVM cannot take — a JPA mapping, a structural change to
a Spring proxy, a new bean, a frontend annotation, a resource read only at
startup, a deleted class — escalates to a restart, and a failure the loop finds
out about fails the apply with the phase and the reason. Where the loop cannot
tell, it errs toward the restart: treating every non-theme frontend file as
bundled over-restarts, which is the honest error to make. Where the loop can
ask, it asks rather than overhears — a Vite compile error is found by having
the connector fetch the changed module, not by hoping a browser re-fetched it
while the daemon watched the log. The daemon README's Known limits section
lists what the loop cannot make live and why.

## Verifying a change

The daemon's unit tests cover what is decidable without a running application,
and the loop itself is tested by the fixtures in `flow-tests/test-devloop`: an
entry point and a WAR under its own build plugin, over shared ITs. The forked
containers — WildFly, TomEE, both Payaras, Liberty and Cargo — have no fixture,
and the one question that matters for them, whether the agents reached the
server's JVM, is answered by hand. Do that against a reactor and not a single
module: a goal named on a command line runs on every module in the reactor, and
a sibling left at `compile` reaches the WAR as an empty directory, and a project
with no sibling shows neither. The daemon README's Tests section has the
procedure.
