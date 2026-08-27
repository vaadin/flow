# test-devloop

End-to-end tests for the `vaadin-dev` CLI and the dev-loop daemon
(`flow-devloop-daemon`, plus the in-app connector in
`vaadin-dev-server/.../devserver/devloop/`).

## Why this module has its own reactor

`flow-tests/README.md` says most tests belong in `test-default`, and that modules
keep their own only for irreducible special infrastructure. A dev loop qualifies
twice over:

- **The interesting case is multi-module.** An edit in a sibling library has to
  reach the running page, and that only exists if there *is* a sibling — so this
  aggregator is the reactor under test, with `devloop-shared` beside
  `devloop-app`.
- **The daemon owns the application process.** So the usual
  `spring-boot-maven-plugin start`/`stop` (or `jetty:start`/`stop`) IT lifecycle
  cannot be used: a second launcher would fight the daemon for the HTTP port.

`devloop-app` inherits `flow-tests` for infrastructure but carries its own Spring
Boot configuration, because it stands in for a user application. Its build runs
`flow:install-dev-cli`, which rewrites the payload whenever it differs, so the CLI
the ITs drive is the one the goal installs — a change that breaks the installed layout breaks the ITs.

That is also why `.vaadin/`, `.agents/` and `.claude/` are git-ignored *here* and
committed in a real project: regenerated every build, they would otherwise leave
a dirty working tree after every edit to the shipped resources.

## How the lifecycle differs

| Every other flow-tests module | Here |
|---|---|
| a Maven plugin starts the container at `pre-integration-test` | the ITs run `vaadin-dev start` themselves |
| a Maven plugin stops it at `post-integration-test` | `vaadin-dev shutdown`, from a JVM shutdown hook and again before the next run |
| a failed start is a build error | a failed start is a test failure carrying the daemon's own reason |

The daemon is deliberately *not* restarted between tests: surviving is its whole
value, and a cold start is about thirty seconds. It *is* restarted between runs,
though. `VaadinDevCli` shuts down any daemon it finds a handshake for before the
first `start`, because the exit hook is best-effort: a test JVM that exits while
a transaction is in flight leaves the daemon running, and the next run's first
apply then races the previous run's - reported as `superseded`, exit 4, which
looks like a failure of whatever test happened to be first. A run guarantees its
own preconditions rather than trusting the last one to have cleaned up. The cost
is one cold start per `mvn verify`, which is the price of a run meaning the same
thing every time.

Three things the ITs must pin, and all are set in `VaadinDevCli`:

- **`-Dvaadin.dev.reactorRoot`** — this application sits inside the Flow
  repository, whose own root aggregates it as well, so reactor discovery would
  otherwise pick the Flow root and resolve the classpath by building all of Flow.
- **`-Dvaadin.dev.mavenArgs=-P!install-git-hooks`** — the Flow root pom activates
  that profile whenever `${maven.multiModuleProjectDirectory}/.husky/_` is
  missing, and runs a script it locates the same way. Rooted here, both paths
  point at this reactor and the resolve fails; the daemon reports it as
  `classpath: ...`, which is honest but not useful.
- **`VAADIN_DEV_PROGRESS=never`** — the output is read by assertions, not by a
  person.

## Running them

These are ordinary ITs: no tag, no profile, and CI runs them like any other
module under `flow-tests/`. The whole suite takes about two minutes, most of it
the two cold JVM starts that `DevLoopPomEditIT` and `DevLoopRestartIT` need.

From the repository root:

```bash
# once, so the SNAPSHOT parents, the daemon jar and devloop-shared are installed
mvn install -DskipTests

# the ITs in this module, and nothing else
mvn -o -pl flow-tests/test-devloop/devloop-app verify
```

**Do not add `-am` to the second command.** It would pull most of Flow into the
reactor and run every upstream module's tests first. Build the upstream modules
with the first command, and keep the reactor of the second one to this module
alone. After changing the daemon or the connector, reinstall just what changed
rather than reaching for `-am`:

```bash
mvn -o -pl flow-devloop-daemon,vaadin-dev-server install -DskipTests
mvn -o -pl flow-tests/test-devloop/devloop-shared install -DskipTests
```

A single class or method, same rule about the reactor:

```bash
mvn -o -pl flow-tests/test-devloop/devloop-app verify \
    -Dit.test=DevLoopCliContractIT
mvn -o -pl flow-tests/test-devloop/devloop-app verify \
    -Dit.test='DevLoopApplyIT#methodBodyEdit_isHotSwappedRatherThanRestarted'
```

Targeting the `flow-tests/test-devloop` aggregator instead needs one more flag:
it runs failsafe too and owns no tests, so `-Dit.test` fails there before
reaching `devloop-app` with `No tests matching pattern ... were executed`. Add
`-Dfailsafe.failIfNoSpecifiedTests=false`, or just name `devloop-app` as above.

`DevLoopBrowserIT` needs Chrome; the rest do not.

## The patch-and-revert rule

These ITs mutate real sources, because that is the subject: a dev loop is only
testable by changing what is on disk. Two rules keep it safe.

1. **Only files that exist to be edited.** Java changes are confined to the
   `…app.mutable` package and to `devloop-shared`'s `DueDateFormatter`; the
   stylesheet and the pom are named explicitly by the tests that edit them.
2. **Every edit is reverted from the original bytes.** `SourcePatch` remembers
   what it read and writes it back in `@AfterEach`, so a failed run never leaves
   the working tree dirty for the next one. If a run is killed mid-test, `git
   checkout` the module — nothing else is needed.

## Driving the loop by hand

```bash
cd flow-tests/test-devloop/devloop-app
mvn flow:install-dev-cli                  # already run by the build

export VAADIN_DEV_DAEMON_OPTS="-Dvaadin.dev.reactorRoot=$(cd .. && pwd) -Dvaadin.dev.mavenArgs=-P!install-git-hooks"
.vaadin/vaadin-dev status                # expect: stopped, in milliseconds
.vaadin/vaadin-dev start                 # blocks until serving or failed
#   edit src/main/java/.../mutable/TaskListView.java
#   and ../devloop-shared/src/main/resources/META-INF/resources/task-list.css
.vaadin/vaadin-dev apply                 # expect exit 0 + "hot-reload:" / "hmr:"
.vaadin/vaadin-dev status --json
.vaadin/vaadin-dev shutdown
```

The application serves on **http://localhost:8899** — not 8080, because a
developer running this very likely has something there already, and the daemon
would then report a port clash rather than the test telling you anything. The
number lives in `pom.xml` (`<server.port>`) and in
`src/main/resources/application.properties`, and the two have to agree.

On Windows use `.\.vaadin\vaadin-dev.cmd` with the same arguments: every verb and
every exit code is meant to be identical. Nothing checks that automatically -
comparing the two launchers takes a machine that can run both, and CI is Linux -
so run the sequence above through the `.cmd` launcher after changing either
script.
