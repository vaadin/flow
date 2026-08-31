# `vaadin-dev` reference

Detail behind [SKILL.md](SKILL.md). Read the section you need.

## Reading `apply`

```
hmr: 1 resource(s) copied, pushed 1 stylesheet(s) in place    ← CSS, pushed into the open page
hmr: 2 theme file(s) pushed in place                          ← theme CSS, pushed; no reload
hmr: 1 frontend file(s) served live, browser reloaded         ← index.html or a theme asset
hmr: 3 frontend file(s), applied by Vite (dev server up:57231)
                                                              ← Vite mode: Vite applied them on save
hot-reload: redefineClasses(1); onHotswap completed=true      ← Java, hot-swapped, UI refreshed
  → live, but no Vaadin component was redefined ...            ← bytes are live; the page still shows the old render
compiling → runtime → restarting → Stable                     ← app restarted; reload the page
restarting → Stable                                           ← restarted with nothing compiled
restart: classpath changed (removed h2-2.3.232.jar)           ← a pom edit; a JVM cannot be given a new classpath
restart: frontend changed (dev bundle rebuild)                ← a bundled frontend edit; the restart rebuilds
restart: 2 frontend file(s) removed (dev bundle rebuild)
restart: theme.json changed (dev bundle rebuild)
restart: frontend imports changed (MyView): @JsModule and friends are read at startup (dev bundle rebuild)
no changes   (../shared-lib/pom.xml changed; nothing to recompile or restart)
                                                              ← the pom edit was seen; the app is stable
compiling → Failed
  Foo.java:51:53  error: cannot find method bar() in class Foo
  shared-lib/Bar.java:12:9  error: ...   ← a file outside this module is named by its module
  → check the name, or add the missing member/import          ← fix, re-apply; app keeps last good bytes
app log: 1 error(s) since the change; see target/devloop/app.log
  There was an exception while trying to navigate              ← the change is live and the app threw
frontend → Failed                                             ← Vite mode: a TypeScript error
dev server: [vite] Internal server error: Transform failed with 1 error: | [PARSE_ERROR] Unexpected token | src/main/frontend/greeting.ts:1:26
                                                              ← what and where, without opening the log
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

In **Vite mode** a TypeScript or JavaScript compile error reaches you as a `frontend → Failed`
with `dev server:` under it, and `apply` exits `1`. Vite compiles on save rather than on apply,
so its errors are in the log before `apply` runs and before the browser shows its red overlay;
`apply` carries them into its own window and fails on them rather than answering a clean
`Stable` over a file the browser is refusing to load. This is the one failure the daemon
reports without escalating — a restart cannot compile a broken module. Fix the file and
re-apply — the next apply is clean. A Java edit that arrived in the same change-set is already
live; the dev server's error is about the frontend half.

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
| `application.properties`, or any resource outside `META-INF/resources/`, `static/`, `public/` and `resources/` | read while the app started and never re-read, so copying it changes nothing — `apply` **restarts** |
| `src/main/frontend/themes/<theme>/**.css` | pushed in place, **no reload** — `apply` combines the theme and sends it |
| `src/main/frontend/index.html`, theme assets (images, fonts) | already served from disk; `apply` **reloads the page** |
| Any other `src/main/frontend/` file — `.ts`, `.tsx`, `.js`, `.css`, `theme.json` | in **dev-bundle mode** only a Vite build can fold it into the bundle, so `apply` **restarts**, and the restart rebuilds the bundle (slow — tens of seconds). In **Vite mode** Vite applied it when you saved; `apply` says so and does nothing — unless Vite could not compile it, in which case `apply` **fails** and quotes what Vite said |
| A deleted `src/main/frontend/` file | same as an edit: restart in dev-bundle mode, nothing in Vite mode |
| `src/main/frontend/generated/` | never in the change-set; the build owns it |
| `package.json`, `vite.config.ts`, `tsconfig.json` at the project root | **not tracked** — they need `npm install` or a Vite restart, so edit them and run `restart`, not `apply` |
| Java or CSS in a sibling library module | same as the application's — `changeSet` shows it as `../<module>/...` |
| A sibling module's new route, `@JsModule` or `@NpmPackage` | read at startup — needs `restart`, not `apply` |
| Adding, changing or removing `@JsModule`, `@JavaScript`, `@CssImport`, `@NpmPackage` or `@Theme` on a Java class | the class redefines cleanly, but those are read by the build — they go into `generated-flow-imports.js` at startup and reach the browser through a bundle chunk. `apply` **restarts** rather than reporting a hot-reload that is not live. `@StyleSheet` is the exception: it is live already, no restart |
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
- `dev server: [vite] ...` — Vite refused to compile the change. Fix the file it names and
  re-apply; do not `restart`, which cannot compile a broken module either.
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
-Dvaadin.dev.frontend    the frontend folder, when it is neither what the build recorded
                         nor src/main/frontend or frontend/ (a daemon option, so it goes
                         in VAADIN_DEV_DAEMON_OPTS). `apply` prints the folder it settled
                         on, and where it got it, into target/devloop/daemon.log
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
