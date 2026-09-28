# test-devloop

End-to-end tests for the `vaadin-dev` CLI and the dev-loop daemon
(`flow-devloop-daemon`, plus the in-app connector in
`vaadin-dev-server/.../devserver/devloop/`).

One fixture per shape of Vaadin application, because the shape is the thing the
daemon has to tell apart — and almost everything that has broken here broke on
one shape while the other stayed green:

| Module | The application it stands in for |
|---|---|
| [test-devloop-spring](test-devloop-spring/README.md) | an application with an entry point of its own: Spring Boot, launched as `java -cp … <MainClass>` |
| [test-devloop-jetty](test-devloop-jetty/README.md) | a WAR with no entry point, run by the project's own build plugin, with the servlet container nowhere on its classpath |
| test-devloop-support | everything both of them run: the CLI driver (`VaadinDevCli`), the source patcher (`SourcePatch`), and the ITs themselves, published as a test-jar |

Each fixture is its own multi-module reactor with a sibling library beside the
application, because the case worth testing — an edit in a sibling reaching the
running page — only exists if there is a sibling. The two are laid out
identically, down to the module directory names and the fixture files
(`TaskListView`, `TaskService`, `DueDateFormatter`, `task-list.css`), so that
the tests which are about the loop rather than about a container can be written
once and run twice. Those ITs are ordinary classes in `test-devloop-support` -
`DevLoopApplyIT`, `DevLoopCssIT`, `DevLoopMultiModuleIT`, `DevLoopLifecycleIT`,
`DevLoopBrowserIT`, `DevLoopCliContractIT`, `DevLoopDaemonSurvivalIT`,
`DevLoopDeletionIT`, `DevLoopPomEditIT`, `DevLoopFrontendIT` - test sources
there, published as a test-jar, and each
fixture's failsafe runs them straight out of it with `<dependenciesToScan>`,
against its own application. No module declares a subclass of them, and the
support module runs none of them itself (`skipITs`): it has no application to
drive. What is left in a fixture is only what is true of its own
shape, in a `DevLoopSpring*IT` or `DevLoopJetty*IT` class of its own. The two run on different ports
(8899 and 8898) so that neither can take the other's, and neither uses the usual
`spring-boot:start` / `jetty:start` IT lifecycle: the daemon owns the
application process, and a second launcher would fight it for the port.

`test-devloop-support` is listed among the modules `flow-tests` builds
regardless of `-DskipTests`, rather than in this aggregator's `<modules>`. That
is how CI gets it into the local repository: the IT jobs build no test module at
all, so anything they resolve has to have been installed by the workspace build,
which runs with `-DskipTests`.

## Running

From the repository root, after one `mvn install -DskipTests`:

```bash
# both fixtures
mvn -o -pl flow-tests/test-devloop/test-devloop-spring/devloop-app,flow-tests/test-devloop/test-devloop-jetty/devloop-app verify
```

Each fixture's README has the rest: what it pins and why, how to drive the loop
by hand, and the patch-and-revert rule the ITs follow so that a failed run never
leaves the working tree dirty.
