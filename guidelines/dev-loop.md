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

## Why the loop exists

A human developer gets a feedback loop from the IDE without asking for one: it
compiles in the background, applies the change to the running application,
refreshes the browser and shows the errors. A coding agent that edits the
sources from outside the IDE gets none of that
([#25197](https://github.com/vaadin/flow/issues/25197)):

- There is no authoritative answer to "is my last change live in the running
  app?", so the agent guesses, waits on timers and retries.
- Compile and runtime failures are invisible, or found only indirectly.
- A change that spans Java and generated frontend artifacts has no point at
  which it is known to be consistent across both.
- A human and an agent working on the same running application cannot observe
  one shared, unambiguous state.

The cost is slower iteration, wasted tokens, and agents drawing conclusions
about an application they cannot actually see. The loop is measured against
exactly that: with it, an agent should finish the same task faster and with
fewer tokens, and the code it writes should be no worse.

## What the loop is and is not

The daemon launches, coordinates and observes; it does not replace anything.
Compilers, HotswapAgent, Flow's `Hotswapper`, the Vite dev server and the
browser's live reload all keep doing their own jobs, and the daemon calls them
and reports what they did. Out of scope, on purpose:

- **Checking what rendered.** Loading a route and asserting what is on the page
  is the agent's job, with its own tools such as Playwright. The daemon's job
  ends when the change is live in the running application; it carries no
  browser automation or UI-testing API.
- **Production builds**, code-generation quality and visual design tooling.
- **An MCP server.** See [The CLI is the agent surface](#the-cli-is-the-agent-surface).
- **Shared write-coordination** — an agent compiling while an IDE owns the JVM.

## The CLI is the agent surface

Agents reach the loop through one surface, the `vaadin-dev` CLI, a thin client
over the daemon's loopback RPC. The choice over an MCP server is deliberate:

- **Token cost.** An MCP tool schema sits in the agent's context on every turn.
  A CLI is learned once, from a line in the skill or `--help`, and afterwards
  costs only the command and its output. That is why the default output is
  terse — a success is about two lines — full diagnostics are printed only on
  failure, and structured output is the opt-in `--json`.
- **Composability.** `apply && …`, piping `--json` to `jq`, scripts.
- **Human and CI parity.** A developer or a CI job runs the very same command.
- **No special client.** Any agent with a shell can use it.

An MCP server would be one more client over the same RPC, so it can be added
later without changing the contract; until then a new capability is a new verb
or option on the CLI.

The RPC binds to the loopback interface only and every request carries the
token from the handshake, because whoever can reach it can make the daemon
compile, redefine and restart the application.

## Who owns the application

Whoever launches the application owns compiling and reloading it for that
session. The daemon is always the one that aggregates state and answers
questions about it, and never a second actor competing with the owner: two
owners both compiling, both redefining over one JVM or both restarting the
application diverge, and the answer to "what is the state of my last change?"
is lost again.

The loop implements the agent-led mode: the daemon launches the application,
compiles, redefines and restarts it, and `status` reports `owner=daemon`. An
IDE-led mode, where a human launched the application from the IDE, is planned
as observe-only: the daemon would relay the IDE's state through the IDE
plugins' existing Copilot REST service rather than inventing its own
coordination, and it must then neither run its own redefiner nor launch a JVM
with its own HotswapAgent beside the IDE's.

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
calling `Class.forName`. None of those is an oversight.

The build plugin does not depend on the daemon either: `install-dev-cli` runs
code out of the daemon jar the project resolves, reflectively, over a class
loader of its own, as the Build & Dependencies rules require of any dev-only
jar.

## One daemon, outliving the command that started it

Nobody starts the daemon by hand. The CLI spawns it when a command needs one
and every later command, from any agent or terminal, reuses it — the pattern of
the Gradle daemon and of language servers. There is exactly one per application
root, because two daemons would bring back two answers to "what is the current
state?". It ends on an explicit `shutdown`, after an idle timeout with no
application running (`vaadin.dev.idleSeconds`), or when a newly spawned daemon
finds its predecessor dead. Whether a daemon or an application is alive is
decided by its process, never by probing a socket: a raw probe of the
application's JDWP port leaves the JDWP handshake half-open and disables the
listener for the rest of the session.

So `start`, `stop` and `restart` are about the application, and the daemon's
own lifecycle is automatic. Shutting the daemon down stops the application it
launched, because it owns it.

The loop holding across commands is the one thing the daemon exists to do, and
three decisions protect it.

**The handshake lives outside `target/`.**
`<app>/.vaadin/daemon.properties` records the daemon's pid, process start time,
port and auth token. `mvn clean` must not orphan a running daemon and leave the
next command spawning a second one to fight for port 8080. The recorded start
time is what stops a recycled pid from passing for a live daemon. The token
authorizes commands, so `install-dev-cli` also installs a `.vaadin/.gitignore`
naming the per-run files: `.vaadin/` holds both committed tooling and per-run
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
command uses it; `register` is the connector's long-lived connection rather
than a command, and sends no `EXIT`. The verbs and their arguments are listed
in the daemon README.

The exit code is what an agent acts on, which is why an `apply`'s code is
derived from `TransactionEngine.Outcome` rather than parsed out of text, and
why changing what a code means breaks every agent that branches on it:

| Outcome      | Exit | Means                                           |
| ------------ | ---- | ----------------------------------------------- |
| `STABLE`     | 0    | the change is live and the app is consistent    |
| `COMPILED`   | 0    | compiled, but nothing is running to apply it to |
| `NO_CHANGES` | 0    | nothing changed on disk since the last apply    |
| `FAILED`     | 1    | could not complete; the reason names the phase  |
| `SUPERSEDED` | 4    | cancelled by a newer apply                      |

The CLI adds `64` (usage), `70` (internal, or the daemon is unreachable) and
`77` (unauthorized) of its own.

## The transaction

A transaction is what the daemon tracks: one logical source change carried
through to a running application that is stable again. State, diagnostics and
the outcome an agent reads all hang off it. Two edited files, the class files
they compile to and the redefinition that applies them are one transaction with
one outcome, not three events to correlate.

The agent makes its edits with its own tools and then commits them with
`apply`; there is no debounce guessing when an agent is done typing. `apply`
blocks until the transaction reaches a terminal outcome, printing each phase it
enters, and the wait lives in the daemon, gated on what actually happened.
An agent never sleeps or polls to learn whether its change is live.

"What is the state of my last change?" only has an answer while one
transaction at a time decides it. A newer `apply` supersedes the running one
rather than queueing behind it because only the latest bytes on disk matter:
a queued apply would spend its time on a change-set that is already out of
date. The superseded caller still needs an answer, or it waits for ever.

When the application dies, the connector's socket closes, and how it closed
tells a `stop` or a normal exit from a crash. A transaction in flight then
fails — it cannot become stable without a running application — and `status`
reports the application as stopped or crashed, with the exit code when there
is one. The daemon stays up and does not restart the application on its own:
it reports, and the agent relaunches it with `start` or `restart`. An `apply`
with nothing running compiles and says so (`COMPILED`, with `start` as the next
step) rather than launching the application behind the agent's back.
A loop that quietly relaunched a crashing application would hide the very
failure the agent needs to see.

## Settings are fixed when the daemon is spawned

Everything that steers the loop is a system property of the daemon JVM, given
as `VAADIN_DEV_DAEMON_OPTS="-Dfoo=bar"`, and the application JVM inherits the
environment of the daemon. The CLI reads `VAADIN_DEV_DAEMON_OPTS` only when it
*spawns* a daemon; every later command reuses the daemon that is already
running and ignores the variable, so setting it in front of `restart` changes
nothing; changing a setting takes a `shutdown` first. The daemon README's
Knobs section has the commands and the list of settings.

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
are disabled for the same reason. The daemon README lists each one and how it
is switched off.

## One atomic redefinition, then ask Flow to refresh

Each transaction redefines all of its changed classes in one
`Instrumentation.redefineClasses` call, made in the application JVM by the
connector. The JVM's enhanced class redefinition and HotswapAgent's plugins
are used, but never HotswapAgent's own delivery: its file watcher turns one
edit into an event per generated class file (lambdas, inner classes), and
Flow's route registry update, which is not thread-safe, then races itself. One
batch per transaction is what avoids that, and it is also why the daemon is the
only redefiner in the JVM.

Redefining bytecode is not enough to show the change: re-rendering the open
view in place takes Flow's `Hotswapper.onHotswap`, which resolves classes in
the application's class loader and so runs in-process, in the connector, after
the redefinition. Whether the classes were redefined changes what it does, so
the connector tells it the truth, and a Java change is Stable only once the
classes were redefined and `onHotswap` ran.

Applying is attempt-then-escalate. What a change will need — a frontend hot
module replacement for CSS, a hot reload for a method body, a restart for a
structural change — can be predicted, but the prediction is not the answer:
the loop tries, escalates to a restart when the JVM refuses, and reports what
actually happened and why.

## The browser is told, not driven

The daemon never talks to a browser. A refresh rides Flow's existing live
reload channel (`BrowserLiveReload` over the debug window connection), which the
connector triggers. That channel is a fire-and-forget broadcast with no
per-browser acknowledgement, so the loop reports what it knows — whether a
reload was sent and whether the channel is connected — and nothing about what a
browser rendered. `apply` does not wait for a browser either: an agent-led
session often has none open.

## Reuse Flow's machinery

Where Flow already decides something, the loop lets Flow decide it rather
than growing a parallel tracker: a stale dev bundle is rebuilt by Flow's own
startup path (`NodeTasks`, `BundleValidationUtil.needsBuild`), theme CSS goes
through `ThemeLiveUpdater.push` as on a save, `Hotswapper` and its
`VaadinHotswapper` implementations react to redefined classes, and
`BrowserLiveReload` reaches the browser. A second tracker that decides the same
thing differently is one more way for the loop and the running application to
disagree. For the same reason the loop never assumes it owns the project
directory alone: `.vaadin-node-tasks.lock` expects short-lived holders, and an
IDE or a CI build may be running Flow's tasks at the same time.

An apply compiles its change-set itself — with javac in the daemon, or with
Maven's `compiler:compile` goal — and does not compile through the `compile`
phase. The phase copies resources again, which can trigger reloads nobody asked
for, and runs `prepare-frontend` and whatever else the project binds before
`compile` on every apply. Resolving the classpath again after a pom edit is a
different step and does name `compile`, so that Maven's reactor reader hands
back `target/classes` rather than a stale jar; the daemon README explains it.

## Ask the build, do not guess

Whether a profile is active, what a module inherits from a parent outside the
checkout, and which configuration a plugin ends up with are Maven's to decide,
and they cannot be worked out from the poms. A guess that matches the common
project is wrong for the one with a parent outside the checkout, and that one
fails with no clear reason. So the daemon reads such answers from what the
build recorded: the effective model that `DevLoopBuildExtension` writes to
`model.properties` while the classpath is resolved, and the frontend folder that
Flow wrote to `flow-build-info.json`.

Reading a pom is still fine for a value the pom states for itself, as long as
there is a fallback for when it is inherited instead. The Java release level is
the example: `Reactor.requiredRelease()` reads the compiler configuration out of
the poms, and when a `spring-boot-starter-parent` project leaves it to the
parent, `Jvm` falls back to the version of the compiled class files.

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

The daemon's unit tests cover what is decidable without a running application.
The loop itself is tested end to end in `flow-tests/test-devloop`, with one
fixture per shape of application — an entry point, a WAR under Jetty's plugin,
and one per forked container. Each fixture is a multi-module reactor with a
sibling library, because two failures only exist there: a goal named on a
command line runs on every module in the reactor, and a sibling left at
`compile` reaches a deployed WAR as an empty directory. The ITs that are about
the loop rather than a container live once in `test-devloop-support` and every
fixture runs them, so a fix verified on one shape is checked on all of them. A
shape that genuinely cannot answer a shared question excludes that IT: the
forked servers exclude `DevLoopCssIT`, because a container reading a deployed
WAR cannot see a resource refreshed in the module's output, and Cargo checks the
same thing its own way in `DevLoopCargoResourceIT`.
`flow-tests/test-devloop/README.md` describes the layout.
