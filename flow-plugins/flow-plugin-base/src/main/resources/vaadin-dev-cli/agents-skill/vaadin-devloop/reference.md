# `vaadin-dev` reference

Detail behind [SKILL.md](SKILL.md). Read the section you need.

## Reading `apply`

```
hmr: 1 resource(s) copied, pushed 1 stylesheet(s) in place    ← CSS, pushed into the open page
hot-reload: redefineClasses(1); onHotswap completed=true      ← Java, hot-swapped, UI refreshed
  → live, but no Vaadin component was redefined ...            ← bytes are live; the page still shows the old render
compiling → runtime → restarting → Stable                     ← app restarted; reload the page
restart: classpath changed (removed h2-2.3.232.jar)           ← a pom edit; a JVM cannot be given a new classpath
no changes   (../shared-lib/pom.xml changed; nothing to recompile or restart)
                                                              ← the pom edit was seen; the app is stable
compiling → Failed
  Foo.java:51:53  error: cannot find method bar() in class Foo
  shared-lib/Bar.java:12:9  error: ...   ← a file outside this module is named by its module
  → check the name, or add the missing member/import          ← fix, re-apply; app keeps last good bytes
app log: 1 error(s) since the change; see target/devloop/app.log
  ERROR ... : There was an exception while trying to navigate  ← the change is live and the app threw
```

A `no changes (... pom.xml changed; nothing to recompile or restart)` line is a *positive*
answer, not a shrug: the pom edit was noticed, Maven re-resolved, and neither any module's
compile classpath nor the app's runtime classpath moved — so the running app is already what the
poms describe. A bare `no changes` with no parenthesis means nothing was examined at all.

A `→ live, but no Vaadin component was redefined` line means the redefine worked and Flow had
nothing to refresh: `onHotswap` re-creates components and route targets, and a Grid's cells were
rendered on the server and pushed once. The change is real — interact with the view (anything
that refreshes the data provider) or reload the page to see it. Do not re-apply; there is
nothing left to compile.

An `app log:` line means the app logged an error while the change went live — the bytes are
live, the code did something wrong. `Stable` with this line under it is not a green result:
read the error before reporting the change as working. `status` shows the same for errors
logged since the last apply, which is where a failure that only appears when someone uses
the app turns up.

`--json` gives `outcome`, `classification`, `changeSet`, `diagnostics[]`
(`file`/`line`/`column`/`message`/`hint`), `logErrors[]`, `timings`, `nextAction`.

## Which edits need a page reload

| Edit | Browser |
|---|---|
| CSS/icons under `META-INF/resources/` | updates in place, **no reload** |
| Java in a **component/view** class (method bodies, string literals, most view code) | updates in place, **no reload** |
| Java in a **plain class** (formatter, mapper, helper) called from a renderer | live immediately, but already-rendered output keeps its old values — `apply` says so and tells you to interact with the view or reload |
| Structural Java (new fields/beans, new repository methods, changed routes or annotations) | restart → **reload the page** |
| A JPA entity's mapping | never hot-reloads; Hibernate fixes its metamodel and schema at startup, so `apply` escalates to a restart |
| `application.properties` | copied only — run `restart` to take effect |
| Hand-written `src/main/frontend/` files | not watched; start with `VAADIN_DEV_DAEMON_OPTS="-Dvaadin.frontend.hotdeploy=true" .vaadin/vaadin-dev start` |
| Java or CSS in a sibling library module | same as the application's — `changeSet` shows it as `../<module>/...` |
| A sibling module's new route, `@JsModule` or `@NpmPackage` | read at startup — needs `restart`, not `apply` |
| A `pom.xml` anywhere in the reactor | the next `apply` re-resolves through Maven (a few seconds), then **recompiles whole** every module whose compile classpath moved — so removing a dependency the code still uses `Failed`s with real diagnostics instead of breaking at runtime. If the **app's** classpath moved (a dependency added or removed) it also **restarts** and names what moved. A pom edit that changes neither stays `no changes`. A pom that does not resolve fails the apply and names the artifact |

## Verifying in the browser

Use whatever browser automation this agent has — a Playwright/browser MCP server, a
built-in browser tool, or a headless Playwright/Selenium script. The rules are the same
whichever it is:

- **Navigate once, keep the page open across applies.** CSS pushes and Java hot-swaps land in
  an already-open page; re-navigating hides what you are testing. Reload only after a restart.
- **The first snapshot after navigating is usually empty** — Vaadin renders client-side. Wait
  for a known element or re-snapshot before asserting.
- **CSS: assert computed style**, not screenshots —
  `getComputedStyle(document.querySelector('.app-name')).fontSize`
- **Java: assert the rendered DOM** — component text is in the light DOM:
  `[...document.querySelectorAll('vaadin-button')].map(b => b.textContent.trim())`
- Read the browser console after a change (a `/favicon.ico` 404 is normal noise).
- Dev mode injects Vaadin's dev-tools/Copilot toolbar — ignore those nodes, never assert on them.

With no browser at hand, `curl http://localhost:8080` only proves the app serves the shell —
say so rather than reporting the UI as verified, and fall back to `./mvnw test` for the parts a
test can cover.

## When it goes wrong

- `compiling → Failed` — diagnostics name file, line, column. Fix and re-apply.
- `frontend-down` — Vite stopped answering: `restart`.
- App failed to start (a taken port, a bad config) → `start` exits `1` and names the reason
  from the app's own log, with the tail printed under it; `status` repeats the reason. The
  whole log is the target application's `target/devloop/app.log`. Daemon wedged → `shutdown`,
  then any command respawns it.
- `this project does not depend on the dev-loop daemon` — the application's `pom.xml` is
  missing `com.vaadin:vaadin-dev` (declare it `<optional>true</optional>`, as a generated
  starter does).
- Hot-swap coverage depends on the JVM: only a JetBrains Runtime gets enhanced class
  redefinition, so on a stock JDK more edits escalate to a restart. Nothing is wrong when they
  do — the restart is the honest answer.

## Environment

```
VAADIN_DEV_HOME          a directory holding flow-devloop-daemon.jar, for an air-gapped setup.
                         Otherwise the jar is resolved once from the project's own dependencies
                         and the answer cached under target/devloop/
VAADIN_DEV_APP           the application to act on; the --app option wins over it
VAADIN_DEV_PROGRESS      auto (default) | never | always
VAADIN_DEV_DAEMON_OPTS   JVM options for the daemon, e.g. -Dvaadin.frontend.hotdeploy=true,
                         -Dvaadin.dev.idleSeconds=60, -Dvaadin.dev.reactorRoot=<dir>,
                         -Dvaadin.dev.modules=<dirs>, -Dvaadin.dev.maven=<path>,
                         -Dvaadin.dev.mavenArgs=<args>, -Dvaadin.dev.mainClass=<class>,
                         -Dvaadin.dev.daemonJar=<path>
```

`.vaadin/vaadin-dev --help` lists the rest, including the `redefine <a.b.C,...>` diagnostic.

## Also

- The target application's `./mvnw test` for unit + UI tests.
- If a **Vaadin MCP server** is available (`search_vaadin_docs`, `get_component_java_api`,
  `get_component_styling`, `get_theme_css_properties`), use it instead of recalling API from
  memory; otherwise check the Vaadin version in the application's `pom.xml` and read
  vaadin.com/docs for that version. Prefer theme CSS properties (`--vaadin-*`, `--aura-*`)
  over hard-coded values.
- Browser verification needs a browser automation tool. Nothing installs or configures one for
  you: register a Playwright MCP server (or the equivalent for your agent) yourself, and the
  Vaadin docs MCP server alongside it.
