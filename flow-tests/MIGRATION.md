# flow-tests restructuring plan

Status: **draft / in progress.** This is the working plan for consolidating the
~45 `flow-tests` modules (110+ buildable test apps, 521 ITs) into a small set of
modules organized along two axes.

## 1. Goal

Reduce the number of test modules and make it obvious *what* each IT tests and
*why* it needs the module it lives in. Today most modules exist only to pin one
build/deploy setting; the actual test content is overwhelmingly concentrated in
`test-root-context` (279 of 521 ITs).

## 2. The two axes

Every IT is categorized along two independent axes:

- **Settings → module.** The build/deploy configuration an IT requires
  (deployment/DI container, build mode, package manager, router engine, context
  path, theme, security, …). This is the *only* legitimate reason to split a
  module. Settings are largely **orthogonal dimensions**; most ITs deviate from
  default in exactly one.
- **Feature → package.** The major framework feature the IT exercises (routing,
  dom, components, templates, push, …). This drives the Java package, *within* a
  module.

Key finding from the IT scan: ~250 of `test-root-context`'s 279 ITs run on pure
**default** settings (dev mode, root context, default Lumo, npm). Push transport
(websocket vs long-polling) is chosen **per-test** via `@CustomPush` on the view,
not by module config, so push tests are "default" too. The dominant module is
therefore differentiated *only by feature*.

### Module sizing principle

Each module pins **one specific setting** (or, when a feature only makes sense as
a combination, one specific *set* of settings). Two consequences:

- **Specialized modules are minimal.** A specialized module contains *only* the
  tests that actually exercise its pinned setting — not copies of generic feature
  tests. Generic features are tested once, in `test-default`. Example:
  `test-vaadin-router` holds only the routing tests that behave differently with
  the legacy router, not the whole `routing` feature.
- **Modes are covered once, not per feature.** `dev hotdeploy` is the default for
  essentially every module, because for most tests the build mode is irrelevant.
  The *other* modes therefore need only a single module each to exercise their
  code path: one `test-dev-bundle` and one `test-production`. We do **not** create
  dev-bundle/production variants of every feature.

### Placement & permutation policy

Two invariants keep placement unambiguous *and* prevent both duplication and
silent coverage loss:

- **Source home (unique).** Every test's *source* lives in exactly one module +
  package — its home. That is the only place the test is ever added or edited.
  This gives "one place to add a test" and "no duplication".
- **A permutation is a reuse-run, never a copy.** Running the same test under a
  second setting (router engine, production mode, context path, …) is done by the
  specialized module **reusing the home's `test-jar`** (`dependenciesToScan` /
  `pom-*.xml`), not by copying source. A permutation is a build/run concern, not a
  second source location.
- **Mode modules are not homes.** `test-production` and `test-dev-bundle` are
  reuse-runners. You never *add* a new test there unless the behavior exists
  *only* in that mode. Default-mode features live in `test-default` and are reused
  into a mode module only when a permutation is worth keeping.

Consequence for "no permutations lost": for every permutation that exists today we
must explicitly **retain it** (as a reuse-run) or **drop it on purpose** (recorded
below). Dropping is a deliberate, reviewable decision — never a side effect of
moving a test.

#### Permutation ledger (retain vs drop — filled in during migration)

| Permutation that exists today | Decision |
|---|---|
| routing × {React, vaadin-router} — `test-react-router` ↔ `test-vaadin-router` (currently *duplicated* source) | collapse to one home (`test-default` routing) + **reuse-run** under `reactEnable=false` in `test-vaadin-router` |
| {ccdm, router, …} × production (`pom-production.xml` variants) | **retain** only where prod-specific behavior is asserted (reuse into `test-production`); **drop** the mode-irrelevant ones — decide per case |
| context path × {dev, prod, encoded} | one home (`test-contextpath`) + `pom-*.xml`/profile variants |
| feature × dev-bundle | **dropped on purpose** — one representative `test-dev-bundle` covers the dev-bundle code path; build mode is irrelevant to the feature behavior |

Every source module's migration PR updates this ledger so each retain/drop is
explicit.

## 3. Package naming convention

The **settings are encoded as a parent package segment**, so the same feature in
two modules has distinct fully-qualified packages:

```
com.vaadin.flow.test[.<settingsKey>].<feature>[.<subfeature>]
```

- The **default** module omits `<settingsKey>` (its base is
  `com.vaadin.flow.test`). The *absence* of a settings segment signals "default".
- **Specialized** modules add a `<settingsKey>` naming the pinned setting:
  `vaadinrouter`, `production`, `contextpath`, `pnpm`, `springsecurity`,
  `notheme`, …
- The **feature** is the next segment(s).

Examples:

| Module (setting) | Feature | Package |
|---|---|---|
| default | routing | `com.vaadin.flow.test.routing` |
| vaadin-router (`reactEnable=false`) | routing | `com.vaadin.flow.test.vaadinrouter.routing` |
| production (`productionMode=true`) | routing | `com.vaadin.flow.test.production.routing` |
| pnpm | frontend | `com.vaadin.flow.test.pnpm.frontend` |
| default | lit templates | `com.vaadin.flow.test.templates.lit` |
| default | polymer templates | `com.vaadin.flow.test.templates.polymer` |

A view and its IT share the same package (across `src/main` and `src/test`), as
in the already-migrated `RemoveAddVisibilityView` / `RemoveAddVisibilityIT`.

## 4. Feature packages (the `<feature>` taxonomy)

One per major feature; applied within whichever module pins the relevant setting.

| Feature package | Covers | ~Total ITs |
|---|---|---|
| `routing` | router, navigation, route/query params, location, redirect/forward, route-not-found | ~78 |
| `templates.lit` | LitTemplate API | ~8 |
| `templates.polymer` | PolymerTemplate: binding, injection, composition, collections, events, dom | ~50 |
| `dom` | Element API, dom events/listeners, properties, shadow root, focus/blur, visibility | ~35 |
| `components` | HTML components, DnD, dialog+shortcut, upload, stream/download resources | ~30 |
| `lifecycle` | attach/detach, preserve-on-refresh, value-change-mode, UI init, polling | ~27 |
| `push` | server push (transport selected per-test via `@CustomPush`) | ~27 |
| `frontend` | package manager / bundler / frontend-directory behavior | ~63 |
| `dependencies` | `executeJs`, return values, `@JsModule`/`@StyleSheet`/inline loading | ~19 |
| `embedding` | web component export & embedding | ~17 |
| `theming` | themes, css loading, lumo, styles | ~16 |
| `security` | Spring Security auth, method security, route-path access | ~21 |
| `errorhandling` | exceptions, error views, fault tolerance | ~12 |
| `di` | Spring DI / bean scopes | ~10 |
| `devmode` | dev-server feature tests (devtools, exception stacktraces, Vite comms) — default settings, go to test-default. Settings-bearing ones (live-reload, redeployment, dev-bundle, eager-bootstrap, client-queue) split to their own modules | ~28 |
| `scroll` | scroll restoration | 6 |
| `signals` | signal bindings | 5 |
| `pwa` | PWA, offline, web push | 3 |
| `i18n` | translations / locale | ~3 |
| `bootstrap` | init listeners, client details, location parsing, misc infra | ~20 |

`templates` is deliberately **split** into `templates.lit` and
`templates.polymer` (different APIs); both live in the **same** module.

## 5. Target modules (the `<settingsKey>` set)

From ~45 directories (110+ buildable apps) down to **one large `test-default` plus
~20 small single-setting modules** (and ~5 irreducible special-infra ones).
Per the sizing principle, each specialized module is *small*: the "Collapses from"
column lists where its setting lives today, but only the **setting-relevant** ITs
move into it — generic feature tests stay in `test-default`. Settings-only
permutations of one source are built multiple ways via `pom-*.xml` variants and
test-jar reuse (the patterns already used by `servlet-containers` and the
encoded-context module).

| Module | Setting pinned | Deployment | Base package | Holds (setting-relevant tests only) |
|---|---|---|---|---|
| **test-default** | React, dev hotdeploy, root context, default Lumo, npm | Spring Boot, JUnit 6 | `com.vaadin.flow.test` | bulk of test-root-context (~250); **test-dev-mode** (dev mode *is* default); **test-react-router** (React *is* default → react routing = default routing); **test-react-adapter** (add `flow-react` dep); Spring DI/scope ITs (test-default is Spring Boot); generic ITs scattered elsewhere |
| **test-plain-servlet** | plain `VaadinServlet` / custom servlet service (no Spring Boot) | plain Jetty | `…test.servlet` | test-servlet, test-custom-route-registry, test-client-queue, `SyncError*` & custom-servlet ITs |
| **test-production** | `productionMode=true` | build-frontend | `…test.production` | prod-only ITs (e.g. RouteNotFoundProdMode), prod-bundle |
| **test-vaadin-router** | `reactEnable=false` (legacy client router) | as default | `…test.vaadinrouter` | test-vaadin-router, test-ccdm |
| **test-contextpath** | custom / encoded context (+prod profile) | Jetty contextPath | `…test.contextpath` | test-router-custom-context (+encoded, +encoded-prod) |
| **test-dev-bundle** | dev bundle (`vaadin.frontend.hotdeploy=false`) — the dev mode *without* hotdeploy that contrasts with test-default | dev, prebuilt bundle | `…test.devbundle` | a **representative subset** that proves the dev-bundle code path (from test-express-build dev-bundle parts); not a per-feature re-run |
| **test-custom-frontend-directory** | custom / legacy `frontendDirectory` | dev/prod | `…test.frontenddir` | test-custom-frontend-directory, test-legacy-frontend |
| **test-pnpm** | pnpm package manager | as default | `…test.pnpm` | minimal build/run smoke proving pnpm |
| **test-bun** | bun package manager | as default | `…test.bun` | minimal build/run smoke proving bun |
| **test-themes** | custom application theme (incl. reusable parent themes, Aura, no-Polymer) | dev/prod | `…test.theme` | test-themes, test-application-theme, test-theme-no-polymer |
| **test-no-theme** | `@NoTheme` (theming suppressed) | dev | `…test.notheme` | test-no-theme |
| **test-tailwind** | Tailwind CSS feature flag | dev | `…test.tailwind` | test-tailwindcss |
| **test-embedding** | web component embedding | dev/prod | `…test.embedding` | test-embedding |
| **test-plain-spring** | Spring **without** Boot (Spring MVC) | Spring MVC on Jetty | `…test.plainspring` | test-mvc-without-endpoints, the non-Boot bits of test-spring-common |
| **test-spring-security** | security + (urlmapping \| contextpath \| methodsec \| routepath \| websocket) via profiles | Spring Boot | `…test.springsecurity` | the 14 security modules → ~4 |
| **test-livereload** | live reload (+ multimodule, dev-bundle variants) | Jetty/Spring | `…test.livereload` | test-live-reload (+multimodule, +devbundle) |
| **test-redeployment** | Spring DevTools restart (+no-cache profile) | Spring Boot jar | `…test.redeployment` | test-redeployment (+no-cache) |
| **test-eager-bootstrap** | `eagerServerLoad=true` | as default | `…test.eagerbootstrap` | test-eager-bootstrap |
| **test-pwa** | PWA (+offline variants, webpush) | dev/prod | `…test.pwa` | test-pwa, test-pwa-disabled-offline, test-webpush |
| **test-cdi** (future) | CDI container | CDI | `…test.cdi` | vaadin-cdi (when merged) |

**Package managers**: npm is the default (test-default). `test-pnpm` and
`test-bun` are **separate modules**, each a minimal build/run smoke that proves
its package manager. No `test-frontend` source module is needed — its frontend-dir
setting is `test-custom-frontend-directory`, and its PWA/embedding tests move to
`test-pwa` / `test-embedding`.

**Stay as separate modules (unique infra, can't collapse):**
`servlet-containers` (Cargo/Tomcat), `test-multi-war` (two WARs in one Jetty),
`test-commercial-banner` (fake `user.home` license hack), a fault-tolerance
module for proxy-based tests (`NetworkInterruptionIT` uses
`ChromeBrowserTestWithProxy`), and `test-npm-performance-regression` (heavy
payload deps).

**Why several proposed modules were dropped:**
- **No `test-react`** — React is the default engine (`reactEnable` defaults to
  true); only `flow-react` distinguishes the react modules, which is a dependency
  not a setting. React routing tests are default routing; react-adapter tests
  live in test-default with `flow-react` added.
- **No `test-devmode`** — `test-dev-mode` pins no settings (dev mode is the
  default); its ITs are default and move to test-default. Only the genuine
  settings around dev mode stay separate: `test-eager-bootstrap`,
  `test-livereload`, `test-dev-bundle`, `test-redeployment`.
- **No `test-frontend`** — see package-manager note above.
- **`test-plain-spring`, not `test-spring`** — test-default already *is* Spring
  Boot, so Spring DI/scope tests are default; only non-Boot (Spring MVC) needs
  its own module.

## 6. Placement rule for special-infra ITs

An IT that needs special infrastructure goes into the **default** (or relevant
existing) module **if that infra is already present there**; otherwise it gets
its **own module**. Examples:

- Custom `@WebServlet` / `createServletService` overrides (`SyncError*`,
  custom route registry, custom UIDL handler) → **test-plain-servlet**
  (Spring Boot changes servlet registration, so they can't be in test-default).
- Proxy-driven network-interruption tests → own module (no proxy infra in
  default).
- Slow/scalability and `@Ignore`d tests → carry their `@Category`/disable status
  across; don't let them block a feature package.

## 7. Migration mechanics

For each IT moved into a module:

1. **View**: copy `src/main` view verbatim; change only the package (to the
   module's `<settingsKey>.<feature>` package) and, where helpful, the `@Route`
   value to a clean slug.
2. **IT**: rewrite from JUnit 4 (`ChromeBrowserTest`) to **JUnit 6 (Jupiter)**:
   - extend `AbstractDefaultIT` (or the module's equivalent base),
   - `@Test`→`@BrowserTest`, `@Before/@After`→`@BeforeEach/@AfterEach`,
     `Assert.*`→`Assertions.*`, drop `@Category`,
   - annotate with `@TestFor(TheView.class)` instead of overriding
     `getTestPath()` (the base derives the path via `RouteUtil.getRoutePath`).
3. **Remove** the original view + IT from the source module (atomic move).
4. **Settings-variant reuse**: when a specialized module runs the *same* ITs as
   another under different settings (router engine, context path, prod mode,
   security permutation), share one source via a **test-jar dependency +
   `dependenciesToScan`** or a **`pom-*.xml` profile**, rather than copying — as
   `servlet-containers` and `test-router-custom-context-encoded` already do.

### Established module conventions (already in place in `test-default`)

- **Spring Boot** deployment, frontend **hotdeploy** (Vite), no
  `flow-maven-plugin`.
- ITs on the **JUnit Platform** via the `failsafe.provider.artifactId` override
  (the `flow-tests` parent exposes this property; default stays
  `surefire-junit47`).
- `AbstractDefaultIT` supplies a headless Chrome driver via TestBench
  `DriverSupplier` (`--headless=new --no-sandbox --disable-dev-shm-usage …`).
- `@TestFor(view)` + default `getTestPath()`.

## 8. CI

No CI changes are required to add modules: `scripts/computeMatrix.js` discovers
modules from the poms and ITs by `*IT.java`, and each module self-selects its
failsafe provider. As `test-default` grows, consider adding it to `moduleSplits`
in `computeMatrix.js` so its tests fan out across parallel slices.

## 9. Suggested sequence

1. **(done)** Scaffold `test-default` (Spring Boot, JUnit 6, hotdeploy),
   make the failsafe provider overridable, add `@TestFor`, migrate the first IT
   (`RemoveAddVisibility`).
2. Establish the feature packages in `test-default` and migrate the
   default-settings ITs from `test-root-context` feature-by-feature
   (`dom`, `components`, `routing`, `lifecycle`, `dependencies`, `scroll`,
   `signals`, `templates.lit`, `templates.polymer`, `push`, `errorhandling`,
   `bootstrap`, `i18n`).
3. Add **test-plain-servlet** for the custom-servlet ITs.
4. Collapse the **spring-security** 14→~4 and the **context-path** 3→1.
5. Split the theme modules (**test-themes**, **test-no-theme**, **test-tailwind**)
   and add **test-pnpm** / **test-bun**; keep **test-custom-frontend-directory**
   for the frontend-dir setting.
6. Fold the router-engine (`test-vaadin-router`) and production variants into
   shared-source builds where behavior matches.
7. Retire emptied modules (and delete the placeholder `test-embedded-jetty-12`).
8. Add **test-cdi** when vaadin-cdi merges.

## 10. Open questions (not set in stone)

- Default module: omit the `<settingsKey>` segment (recommended, ergonomic) vs.
  always include one (`defaultconfig`) for uniformity.
- Whether router-engine and production variants should be separate modules or
  pure `pom-*.xml` builds over `test-default`'s source.
- Exact `<settingsKey>` names.
- How finely to split `templates.polymer` (single package vs. sub-packages for
  binding/injection/collections/…).
