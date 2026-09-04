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

Candidates are every directory under `~/.jdks` plus `JAVA_HOME` and `JDK_HOME`, and
each one's version and vendor are read from its own `release` file
(`IMPLEMENTOR="JetBrains s.r.o."` is what makes it a JBR) rather than guessed from its
directory name — which is how `jbr-9` used to outrank `jbr-21`. The JBR closest above
the requirement wins; failing that, the closest JDK, and the log says what that cost.

## The frontend leg

`Frontend` owns the whole of it: where the frontend folder is, and what a change under it
means. It lives there and not in `TransactionEngine` for a practical reason — `Frontend` is
constructible from a directory and a string, so every rule is unit-testable, while
`TransactionEngine` needs a `Launch` and a running app and is only reachable from an IT.
What is left in `TransactionEngine` is plumbing.

**Finding the folder.** The zero-dependency rule means `FrontendUtils` is out of reach, so the
precedence is `-Dvaadin.dev.frontend`, then `-Dvaadin.frontend.folder` (which `Launch` already
forwards to the app, so the two agree by construction), then `frontendFolder` out of
`target/classes/META-INF/VAADIN/config/flow-build-info.json`, then `src/main/frontend` and
`frontend/`. The token is preferred over the convention because Flow wrote it after resolving
both the legacy fallback and the plugin's `<frontendDirectory>`, which convention-matching
cannot see — and it is readable before the app has ever started, because `prepare-frontend`
runs at `process-resources` and the daemon's own resolve runs `compile`. Only
`frontendFolder` is read from that file: it also records `frontend.hotdeploy`, and that one
describes the *build*, not the mode the app is running in, which can be set in
`application.properties` the build never read.

**Deciding what to do.** Which mode the app is in is the whole question, and only the app can
answer it, so `FRONTEND` is asked once per apply and reused by every leg.

- **Vite mode** — Vite's root *is* the frontend folder, so it applied the edit when the file
  was saved and the daemon cannot suspend it the way it suspends Flow's own watchers. Nothing
  is pushed and nothing escalates; `apply` names the files and says Vite did it. Pretending
  otherwise would be the one thing this daemon exists not to do — which is also why a Vite
  compile error is a `FAILED` apply and not a footnote under `Stable`. That error exists only
  in the app log: the push succeeded, the app is running and no class failed to redefine, so
  every other signal the daemon has says the change is live while the browser shows a red
  overlay. `devServerFailure` is the rule, reading both the settled window and the errors
  carried across from save time, and it does not escalate — a restart cannot compile a broken
  module.
- **Dev-bundle mode** — theme CSS is pushed through `ThemeLiveUpdater.push`, the same call
  Flow's own watcher makes on save (and which the connector suspends, as it does
  `PublicResourcesLiveUpdater`). `index.html` and theme assets are already served from the
  folder, so they need only a reload. Everything else is in the bundle, and only a Vite build
  can fold it in — so `apply` restarts, and the app's own startup path
  (`NodeTasks` → `BundleValidationUtil.needsBuild` → `TaskRunDevBundleBuild`) rebuilds. The
  restart is the mechanism here, not a fallback.

**A frontend annotation on a Java class escalates too.** `@JsModule`, `@JavaScript`,
`@CssImport`, `@NpmPackage` and `@Theme` are read by the build, not at runtime: they end up in
`generated-flow-imports.js`, which `TaskUpdateImports` writes during startup, and the browser
reaches them through a bundle chunk keyed by class name. The redefine succeeds and the class
really does carry the new annotation, so nothing looks wrong - but the import is in no chunk the
client can load. `REDEFINE` therefore reports `frontendImports=<classes>`, compared before and
after through `AnnotationReader` (a `Class` discards its cached annotation data when
`classRedefinedCount` moves, so the comparison is real), and `blockedReason` escalates. Note
that no file under the frontend folder need have changed for this - which is why the frontend
tree alone cannot catch it. `@StyleSheet` is deliberately excluded: those are live already
through `StyleSheetHotswapper`, and restarting for one would be a regression.

**Every non-theme frontend file is treated as bundled**, whether or not anything imports it.
The daemon cannot read `stats.json` — that would be a dependency — so it cannot know, and
over-restarting is the honest error to make: the alternative is reporting a change live when
it is not.

Deletions are tracked for frontend files and not for Java sources, because `frontendNotified`
is a complete inventory and there is no artifact to clean up. A removed module the bundle
still imports breaks the next build, so it has to escalate.

The baseline is re-seeded on every registration (`seedFromDisk` → `seedFrontend`), and that is
load-bearing rather than tidiness: a bundled edit escalates to a restart, the restart
re-registers, and without the re-seed the same file would be offered again after the restart
that already folded it into the bundle — restarting the app for ever.

**A Vite compile error is found by asking the dev server, not by overhearing it.** Vite compiles
a module when something *requests* it - not on save, and not when `apply` runs. So the log holds a
report only if a browser happened to re-fetch while the daemon was watching, and a page already
showing the error overlay does not re-fetch at all; every other signal then says the change went
fine, and the apply reports a clean `Stable` over a file the page cannot load. So the frontend leg
asks. `FRONTEND_CHECK <paths>` has the connector fetch each changed file through
`DevModeHandler.prepareConnection`, on the base Vite was actually launched with
(`ViteHandler.getPathToVaadin()`, so an app on a context path works too). A `500` is a refusal and
carries Vite's own message; a `200` means the module compiles. Only `500` counts - a `404` means
the dev server does not serve that path at all.

A `200` is not the whole story, though, because it answers only the question it was asked: can
this module be *served*. Types are stripped without being checked, so a type error - and a
stray `>` in JSX, which oxc tolerates and `tsc` does not - comes back `200`, runs, and is still
wrong. That failure exists only in the log, where `vite-plugin-checker` writes it
(`CHECKER_ERROR`, told apart from `DEV_SERVER_ERROR` for exactly this reason). It type-checks
within about a hundred milliseconds of the *save*, so its report is already there by the time
anyone runs `apply` - which is why the checker's errors are carried across `Watch.mark()` like
the dev server's, and why not carrying them let a broken `.tsx` through as `Stable`. Fatal only
when the change-set touched a frontend file, so a Java-only edit is never failed by a type
error somebody else left behind.

The answer is authoritative **in both directions**, which is the point. A refusal fails the apply
(`Transaction.devServerRefusal`) and leaves the file unmarked, so the next apply asks again rather
than reporting `no changes` over a module that never compiled. And a clean answer *overrules the
log* (`Transaction.devServerAsked`): once the file is fixed, the report still in the log describes
the version before the fix - the daemon's own request for the broken one is among the things that
put it there - so trusting the log would fail the very apply that repaired the problem. That
overruling is scoped to transform errors, the only ones the fetch answers for; the checker's
verdict is read from the log either way. The log stays the whole fallback for a dev server that
cannot be asked: an app too old for the command, or one
whose dev server has gone away. It takes finding, because Flow pipes Vite's output through
`DevServerOutputTracker` at `INFO` - the level says `INFO`, "error" is lower case, and
`[PARSE_ERROR]` has no word boundary before `ERROR` - so `AppLog` matches those openers separately
(`DEV_SERVER_ERROR`). An error Vite logged on save is already there when `apply` starts, so
dev-server errors are carried across `Watch.mark()` when a frontend file actually changed
(`Transaction.carriedLogErrors`); one logged mid-apply is caught by settling, as in the redefine
leg.

Either way the report is compacted to three parts and **wrapped, not truncated**. The opening
line, the line naming the error and the source position are kept; the excerpt, caret diagram and
JavaScript stack are dropped (`REPORT_DECORATION`), because the excerpt is the developer's own
code and a caret diagram cannot line up inside an indented summary. `AppLog.report` does this for
a report that arrives whole from the connector and `Watch` line by line for one read out of the
log, which is why the connector joins its lines with the separator rather than flattening them to
spaces. The parts are joined with a unit separator rather than a `|`, because an ASCII excerpt is
drawn as `1 | export function ...` and splitting on that lost the gutter; it never leaves the
daemon, as `--json` and the one-line reason both put ` | ` back. `quote` and `reasonRows` then wrap
at 100 columns, letting a long path overflow a row rather than be cut in half - a compiler message
is the one output whose tail matters as much as its head, and truncating a refusal at 160
characters cut off "Unterminated string constant" on a message whose head is an absolute path.

**Vite mode is verified by hand**, because `hotdeploy` is baked into the app JVM from the
daemon's own system properties and `flow-tests/test-devloop` deliberately shares one
long-lived daemon; switching modes there means a shutdown, a cold start and a pnpm install.
The decision logic is covered by `FrontendTest` in both modes. To check it end to end:

```bash
cd <app>
.vaadin/vaadin-dev shutdown
#   the variable is read only when a daemon is spawned, so a running one
#   would ignore it - see Knobs
VAADIN_DEV_DAEMON_OPTS="-Dvaadin.frontend.hotdeploy=true" .vaadin/vaadin-dev start
#   edit src/main/frontend/<something>.ts
.vaadin/vaadin-dev apply
#   expect: hmr: N frontend file(s), applied by Vite (dev server up:<port>)

#   then break the same file - a missing brace is enough - and, with nothing
#   open in a browser, so the log stays silent about it:
.vaadin/vaadin-dev apply
#   expect: exit 1, "dev server: <file>: Transform failed ...", and the same
#   answer again on a repeat apply and after re-saving the file still broken
#   fix the file
.vaadin/vaadin-dev apply
#   expect: exit 0, Stable - the report left in the log must not fail this one
```

## Known limits

Operational facts a maintainer would otherwise rediscover the hard way. None of
them is a bug in this module; all of them are why `apply` escalates or qualifies
its answer rather than claiming success.

- **JPA entity mappings do not hot-reload**, with or without HotswapAgent.
  Hibernate's metamodel and schema are fixed at startup. The connector reports
  the entity classes involved and `apply` refuses to call the change live. That
  includes a class that is only now being given `@Entity`, and the annotation is
  read out of the compiled bytes for it rather than off the class once it is
  loaded: on a JVM with enhanced class redefinition the class is replaced rather
  than edited in place, and its reflective view is refreshed by HotswapAgent's
  own cache clearing, which is not ordered against the reply. Measured, that
  made the answer depend on another thread's timing — the bytes say the same
  thing on every JVM.
- **A structural change to a proxied Spring bean must restart.** A method-body
  change inside a bean is fine — the proxy delegates and the new body runs. A
  change to the class's *shape* is not: the live proxy was generated against the
  old one. This includes Spring Data repositories, which are bare interfaces with
  no annotation to spot them by, so the connector keys on the loaded proxy
  instead.
- **Hot-swap coverage differs sharply between stock HotSpot and a JBR.** Only a
  JBR gets `-XX:+AllowEnhancedClassRedefinition`; on stock HotSpot a structural
  change is simply rejected and escalates. A project needing a Java version no
  installed JBR provides therefore runs on a stock JDK for the whole session —
  the launch log line says so when it happens.
- **A sibling module contributing routes, `@JsModule` or `@NpmPackage` needs a
  restart**, not an `apply`: those are read at startup.
- **Only resources under a public root can be made live.** `src/main/resources`
  is split by who reads the file: `META-INF/resources/`, `static/`, `public/`
  and `resources/` are served from the classpath per request, so the copy into
  `target/classes` plus a `RESOURCES` push is the whole of the work. Everything
  else — `application.properties` first among them — was read while the app was
  starting and is never re-read, so the copy alone would leave the running JVM
  on the old values. `apply` copies it anyway, to keep the classpath honest, and
  then escalates to a restart rather than returning `Stable`.
- **A deleted resource has to be un-copied.** A walk only sees what is there, so
  the deletion is found against the fingerprint inventory instead — every
  resource on disk at the last seed is a key in it. `target/classes` is what the
  application actually reads, so the copy is removed; a public one then gets a
  `RESOURCES` call of its own, which has nothing to push and therefore reloads
  the page, and a startup one escalates exactly as an edit to it would. The
  inventory is also what bounds this: a resource created and deleted without the
  application restarting in between was never seeded, so its copy is left for
  the next build to clear.
- **A deleted Java source has to be un-compiled, and then restarted.** Found the
  same way a deleted resource is — against the fingerprint inventory, since a
  walk sees only what is there — and it is the change with the loudest failure
  mode if it is missed: a removed route, bean or entity goes on answering out of
  its stale `.class`, and `apply` reports `no changes` with exit 0 over it. The
  artifact is removed (`Foo.class` and the `Foo$…class` files javac names after
  it; a second top-level class in the same file is left for the next full
  build), and then the apply escalates unconditionally — a JVM cannot un-define
  a class it has loaded, so the type is live until the application starts again,
  whatever else the change-set carries. Removing the artifact is what stops the
  restart loading it straight back. Unlike a live resource the deletion is not
  forgotten when it is acted on: it stays in the change-set until a restart
  re-seeds the inventory from disk.
- **An edit that changes what a class promises its callers is not supported.**
  `Compile` passes only the change-set to javac, so callers nobody edited keep
  the bytecode they were compiled with. Rename or re-sign a method, move a
  supertype, or change a `static final` constant, and those callers are stale —
  a `NoSuchMethodError` at runtime, or for an inlined constant no error at all
  and the old value — while `apply` reports `Stable`. The restart does not fix
  it either: it loads the same class files. Recompile with Maven after such an
  edit. A dependency-aware compile (or a whole-module recompile when the
  compiled API changes) is the fix, and is not implemented.
- **Annotation processors are not run.** The compile passes `-proc:none`, so a
  Lombok-, MapStruct- or Dagger-backed source recompiled here loses every member
  and every class the processor would have generated, and the redefine or the
  restart then works from bytecode a normal Maven build would never have
  produced. Such a project needs `mvn compile` rather than `apply`; honouring
  the module's `proc` and `annotationProcessorPaths` configuration is not
  implemented.
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
