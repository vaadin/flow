# Migration plan: empty `src/main/java`, drop GWT

This document is the roadmap. Companion to [`MIGRATION.md`](MIGRATION.md),
which explains the per-class patterns. This file says **what to do in what
order, in commits small enough to review, with the tests that prove each
step is safe**.

## Goal

After the plan completes:

- `src/main/java` is empty.
- `src/test/java` is empty.
- `src/test-gwt/` is gone.
- The GWT plugin and its dependencies are out of `pom.xml`.
- `Flow.ts` imports the migrated TypeScript directly; the
  `window.Vaadin.Flow.internal` bridge namespace and `FlowClient.js`
  wrapper are removed.
- `flow-tests` integration suite still passes end-to-end.

## Constraints

1. **Small PRs.** Each commit is reviewable on its own and targets a single
   logical scope (typically one Java package, sometimes one class).
2. **Drop GWT in the end** — including the GwtTest suite and the JUnit
   suite that exercises pure-Java logic.
3. **Each commit must work.** Build green, all surviving tests green. Tests
   covering the behaviour being migrated must already exist in mocha *in
   the same commit* as the migration.

These rule out an atomic tear-down PR. They also rule out the
"facade-with-JVM-fallback" pattern (Strategy B in the discussion that led
to this plan) — those facades only exist to keep JUnit alive for code
that's about to be deleted, and adding them just to delete them later is
make-work.

The chosen strategy: **drop the JUnit tests one-by-one as their target
code migrates, port their coverage to mocha first.** Each Java class
becomes a pure `@JsType(isNative = true)` declaration pointing at the new
TS module — no JVM fallback body, because no JUnit test exercises a
migrated class anymore.

## Strategy in one paragraph

For every Java class X, in dependency order from leaves to roots:

1. Port the relevant tests of X (the parts of `Gwt*Test` and `XTest.java`
   that exercise behaviour worth preserving) to mocha tests sitting under
   `src/test/frontend/`. Run them against the *current* Java implementation
   wrapped behind a temporary `@JsType` shim if necessary, to validate the
   port faithfully reproduces the original assertions.
2. Translate X to TypeScript under `src/main/frontend/internal/<pkg>/X.ts`.
   Run the mocha tests against the TS implementation.
3. Replace `X.java` with a `@JsType(isNative = true, namespace =
   "Vaadin.Flow.internal.<pkg>", name = "X")` declaration so Java callers
   keep compiling and dispatch to the TS at runtime. Add the registration
   to `internal/bridge.ts`.
4. Delete `XTest.java` (its coverage now lives in mocha).
5. Delete `GwtXTest.java` (or strip out the parts that exercised X, if it
   covers multiple classes that haven't migrated yet).

When every package has been through this, one **tear-down commit** drops
the GWT plugin, the JsType shims, the bridge namespace, and the test
infrastructure that no longer has anything to test.

## Pre-work

Three commits, no functional change to flow-client output:

### P1 — Inventory the test coverage we're committing to preserve

Walk through `src/test-gwt/` and `src/test/java/`. For each test method,
mark one of:

- **PORT**: covers behaviour that needs a mocha equivalent.
- **DROP**: only exists because the Java code currently exists (e.g.
  `JreArrayTest`, `JreMapTest` — testing JVM fallbacks that disappear);
  or covers a constructor with `@since` checks; or is asserting a quirk
  of GWT-specific behaviour that isn't real.
- **INTEGRATION**: high-level end-to-end behaviour where the flow-tests
  integration suite already covers it adequately.

Output: a checklist file under `flow-client/`, one row per test method.
Reviewers can challenge classifications before any migration begins.

### P2 — Mocha test harness for state-tree-like scenarios

The current mocha suite only exercises the `Flow` API and individual
migrated modules. Most ports will need a small harness that:

- Builds JSON UIDL change arrays the way the server would.
- Sets up a `StateTree` instance and asserts post-change DOM state.
- Captures messages sent to the server (sinon spies on the bridge).

Add this harness as `src/test/frontend/harness.ts` so subsequent ports can
reuse it.

### P3 — Confirm GwtTest pipeline status on real CI

Today GwtTests are env-blocked in this sandbox (`libXtst.so.6` missing).
Before relying on them as a safety net during the migration, verify the
upstream CI actually runs and passes them on the current `client-to-ts`
branch. If they're skipped in CI too, the migration plan needs adjusting
— probably by porting *all* GwtTests up-front in P1's classification
work, with no INTEGRATION shortcut.

## Migration order — leaves up

Each step below is one PR. Skip steps for classes already migrated in
earlier commits on this branch (see [`MIGRATION.md`](MIGRATION.md)
"Status"). Refer to MIGRATION.md for the per-class mechanics.

### Tier 0 — already migrated (24 classes)

`Console`, `LitUtils`, `ReactUtils`, `ConnectionIndicator`, `ElementUtil`,
`WidgetUtil`, `PolymerUtils`, `BrowserInfo`, `ResourceLoader`,
`SystemErrorHandler`, `ExecuteJavaScriptElementUtils`, `ExistingElementMap`
(stateful), `ApplicationConnection`, `Profiler`, `bootstrap.Bootstrapper`,
`communication.{MessageHandler, MessageSender, XhrConnection,
AtmospherePushConnection}`, `flow.ExecuteJavaScriptProcessor`,
`flow.binding.SimpleElementBindingStrategy`,
`flow.util.ClientJsonCodec`, `flow.model.UpdatableModelProperties`.

These still have a JVM fallback in their Java facades (from the in-flight
migration). Each one needs a follow-up cleanup commit during its tier to:

- Delete the JVM fallback (turn the Java class into a pure `@JsType`
  native shim).
- Port and delete its JUnit test if one exists.

### Tier 1 — `flow.reactive` (8 files, ~600 LOC)

`ReactiveValue`, `FlushListener`, `InvalidateListener`,
`ReactiveValueChangeListener`, `InvalidateEvent`,
`ReactiveValueChangeEvent`, `Computation`, `Reactive`,
`ReactiveEventRouter`.

Test ports: `ReactiveTest`, `ComputationTest`, `TestReactiveEventRouter`,
`CountingComputation`, `CodeTest` (the parts touching reactive).

This is the leaf — nothing inside `flow-client` depends on something
below it that isn't already native (`flow.collection.*`, `flow.util.*`
remain Java but are already `@JsType(isNative=true)` aliases of global
types, so they pass through transparently).

### Tier 2 — `flow.nodefeature` (13 files, ~800 LOC)

`NodeFeature` (interface), `NodeMap`, `NodeList`, `MapProperty`,
`MapPropertyAddEvent`, `MapPropertyAddListener`, `MapPropertyChangeEvent`,
`MapPropertyChangeListener`, `ListSpliceEvent`, `ListSpliceListener`.

Depends on Tier 1.

Test ports: `NodeListTest`, `NodeMapTest`, `MapPropertyTest`.

### Tier 3 — `flow.dom` (5 files, ~400 LOC)

`DomApi`, `DomApiImpl`, `DomElement`, `DomNode`, `PolymerDomApiImpl`.

Self-contained Polymer/native DOM wrappers, no deps on Tier 1-2.

Test ports: `DomApiAbstractionUsageTest`, `GwtDomApiTest`,
`GwtPolymerApiImplTest`.

### Tier 4 — `flow.*` root: state tree (6 files, ~2k LOC)

`StateNode`, `StateTree`, `ConstantPool`, `TreeChangeProcessor`,
`NodeUnregisterEvent`, `NodeUnregisterListener`. Finish
`ExecuteJavaScriptProcessor` (drop its remaining JVM fallback).

Depends on Tiers 1-3.

Test ports: `StateNodeTest`, `StateTreeTest`, `TreeChangeProcessorTest`,
`ExecuteJavaScriptProcessorTest`, `GwtStateNodeTest`, `GwtStateTreeTest`,
`GwtTreeChangeProcessorTest`, `GwtMultipleBindingTest`,
`GwtClientJsonCodecTest`. **Highest-stakes tier; protocol correctness
hinges on these tests.**

### Tier 5 — `flow.binding` (8 files, ~2.5k LOC)

`Binder`, `BinderContext`, `BindingStrategy`,
`SimpleElementBindingStrategy` (finish; ~1.5k LOC by itself),
`TextBindingStrategy`, `ServerEventHandlerBinder`, `ServerEventObject`,
`Debouncer`.

Depends on Tiers 1-4.

Test ports: `GwtBasicElementBinderTest`, `GwtPropertyElementBinderTest`,
`GwtEventHandlerTest`, `GwtErrotHandlerTest`. Split into 2-3 PRs because
SimpleElementBindingStrategy alone is a significant translation.

### Tier 6 — `flow.util.ClientJsonCodec` finish + `flow.model` final cleanup

Drop the remaining JVM fallbacks. Port and remove any leftover JUnit
tests. Small commit.

### Tier 7 — Top-level `com.vaadin.client` utility finishes

Drop JVM fallbacks from `Console`, `LitUtils`, `ReactUtils`,
`ConnectionIndicator`, `ElementUtil`, `WidgetUtil`, `PolymerUtils`,
`BrowserInfo`, `ResourceLoader`, `SystemErrorHandler`,
`ExecuteJavaScriptElementUtils`, `ExistingElementMap`,
`UpdatableModelProperties`, `Profiler`. Add any missing mocha test
coverage. Port + delete any JUnit tests still alive against these.

Also covered in this tier: `Command` (functional interface; becomes a TS
function type) and `TrackingScheduler` (GWT scheduler wrapper — likely
replaced with `setTimeout`/`queueMicrotask` and the class deleted
outright).

### Tier 8 — `Registry` + application kernel (~1.5k LOC)

`Registry` (interface), `DefaultRegistry`, `ApplicationConfiguration`,
`UILifecycle`, `InitialPropertiesHandler`, `DependencyLoader`.

Depends on Tiers 1-7.

Test ports: `RegistryTest`, `DependencyLoaderTest`,
`DependencyTestHelper`, `UILifecycleTest`,
`AbstractConfigurationTest`, `InitialPropertiesHandlerTest`,
`GwtDependencyLoaderTest`.

### Tier 9 — `communication` package (~3k LOC)

Approximately 22 files. Likely split into 3 PRs:

- **Tier 9a** — XHR + message flow: finish `MessageHandler`,
  `MessageSender`, `XhrConnection`, `Xhr`, `XhrConnectionError`,
  `ServerRpcQueue`, `ServerConnector`, the three `Request*Event` /
  `ResponseHandling*Event` types. Test ports: `GwtMessageHandlerTest`,
  `GwtApplicationConnectionTest`.

- **Tier 9b** — heartbeat / poll / reconnect: `Heartbeat`, `Poller`,
  `PollConfigurator`, `RequestResponseTracker`, `ReconnectConfiguration`,
  `ReconnectionAttemptEvent`, `ConnectionStateHandler` (interface),
  `DefaultConnectionStateHandler`, `LoadingIndicatorConfigurator`,
  `LoadingIndicatorStateHandler`. Test ports: `PollConfiguratorTest`,
  `ReconnectConfigurationTest`, `GwtDefaultConnectionStateHandlerTest`,
  `GwtLoadingIndicatorStateHandlerTest`.

- **Tier 9c** — push: `PushConnection` (interface),
  `PushConnectionFactory`, `PushConfiguration`, finish
  `AtmospherePushConnection` (the JSO subclasses
  `AtmosphereConfiguration` and `AtmosphereResponse` become plain TS
  objects). Test ports: `GwtAtmospherePushConnectionTest`.

### Tier 10 — `ApplicationConnection` finish + `bootstrap` (~500 LOC)

`ApplicationConnection.Styles` (JSO becomes plain TS object), the
constructor wiring (rest of the class body). `bootstrap.ErrorMessage`
(JSO), `bootstrap.JsoConfiguration` (JSO). Finish
`bootstrap.Bootstrapper`. Test ports: nothing left that hasn't already
been ported.

### Tier 11 — `ValueMap` and remaining JSOs

`ValueMap` is used by `MessageHandler` and friends as a typed wrapper
around UIDL JSON. After Tier 9 it has no remaining consumers; delete it.
Same for `Profiler.GwtStatsEvent` (already eliminated by Tier 7 finish).

### Tier 12 — Vendored `gwt/**` and the collection family

`com.vaadin.client.gwt.com.google.web.bindery.event.shared.SimpleEventBus`
— used by tests of the reactive system; either inline the small bit
that's needed into the relevant mocha harness or replace with a 20-line
TS implementation.

`com.vaadin.client.gwt.elemental.js.util.Xhr` — used by
`XhrConnection`; either inline or replace with `fetch`/`XMLHttpRequest`
directly in TS.

`flow.collection.*` (`JsArray`, `JsMap`, `JsSet`, `JsWeakMap`,
`JsCollections`) — by this point every Java consumer is migrated, so
nothing references them. Delete the Java files. The `Jre*` fallback
classes go with them.

### Tier 13 — tear-down (one commit)

At this point `src/main/java/com/vaadin/client` is empty.

Sequence in the commit:

1. Restructure `Flow.ts`:
   - Drop `import { installGwtBridge } from './internal/bridge'` and its
     calls.
   - Drop the dynamic import of `FlowClient.js`.
   - Drop the `clientMod.init()` / `getClients()` indirection — call the
     migrated TS `ApplicationConnection` directly.
2. Delete:
   - `src/main/java/com/vaadin/client/` (now empty, drop the directory).
   - `src/main/java/com/vaadin/flow/linker/ClientEngineLinker.java` and
     `src/main/resources/com/vaadin/flow/linker/ClientEngineSingleScriptTemplate.js`.
   - `src/test/java/` (no Java to test).
   - `src/test-gwt/` (its content is in mocha now).
   - `scripts/client.js` (wrapped FlowClient.js — gone).
   - `src/main/frontend/FlowClient.js`, `FlowClient.d.ts`,
     `FlowBootstrap.js`, `FlowBootstrap.d.ts`.
   - `src/main/frontend/internal/bridge.ts`,
     `src/main/frontend/internal/registry.ts` — no remaining consumers
     because every reader of the namespace is also gone.
3. Optionally promote `src/main/frontend/internal/client/**` up so the
   directory tree isn't named "internal" anymore; rewrite the imports
   accordingly.
4. `pom.xml` surgery:
   - Drop `gwt-maven-plugin`, all `com.google.gwt:*` deps, the
     `org.eclipse.platform.*` test deps, `org.apache.httpcomponents`,
     `org.ow2.asm`, `osgi.core` / `osgi.cmpn` (verify each, some may
     still be needed).
   - Drop the `build-helper-maven-plugin` `add-test-source` execution
     for `src/test-gwt/java`.
   - Drop the surefire `Gwt*` excludes (nothing matches anymore).
   - Drop the `exec-maven-plugin` `npm-install`/`install-esbuild`
     executions and keep only `typescript-compile` + `run-tests` (now
     the whole build cycle).
   - Drop the `sdm` and `slow-tests` profiles.
   - `maven.compiler.release` / `source` / `target` can stay at 8 (or
     bump — no longer constrained by GWT).
5. `package.json`: scripts cleanup; drop `webpack`/`webpack.tests.config.js`
   references if no longer reachable.
6. `README.md`: rewrite. No more GWT debugging instructions, no more
   `gwt.module.style=DETAILED`. Pure TS build.
7. Run `flow-tests` integration suite end-to-end (the only remaining
   safety net against subtle protocol regressions introduced anywhere in
   Tiers 1-12).
8. Grep across the parent repo for `ClientEngineLinker`,
   `flow-client-*.jar` paths that might still expect the GWT artefact at
   `META-INF/resources/VAADIN/static/client/*.cache.js`, and the
   `gwt-maven-plugin` itself. Anything that breaks gets fixed in this
   commit or in a follow-up to the calling module.

## Cross-cutting concerns

### Test infrastructure

- New mocha tests live under `src/test/frontend/`, file name ending
  `Tests.ts` (matches the web-test-runner glob).
- Shared harness goes in `src/test/frontend/harness.ts` (created in P2).
- For each migration commit, the mocha tests for the migrated class
  must exist and pass *before* the Java implementation is removed. The
  commit may rely on Java assertions being equivalent (cross-checked by
  the test author during the port), but `npm test` must be green at the
  end of the commit.

### `ClientEngineTestBase.installMigratedBridgeStubs`

This currently provides JS stubs of migrated TS modules so the surviving
GwtTests can keep passing. As classes migrate:

- A migration whose GwtTest gets fully ported to mocha **deletes** the
  GwtTest *and* the corresponding stub entry.
- A migration whose GwtTest covers other classes that haven't migrated
  yet **updates** the stub to match the current TS API (or, more
  ergonomic, has the stub literally import the published bridge value).

The whole `installMigratedBridgeStubs` function disappears in Tier 13.

### Bridge namespace

`window.Vaadin.Flow.internal.*` is an internal contract between the Java
shims and the TS modules. As Java shims disappear, the consumers of the
namespace disappear, but TS modules still register themselves through
`installGwtBridge()` for harmless reasons. Tier 13 deletes both the
registration and the namespace.

### Order of dependency vs. order of effort

The leaves-up order means heavier classes (`StateTree`, the binding
package) come in the middle of the plan. That's the highest-risk window
— the state-tree protocol is non-trivial and any translation error
manifests as flaky integration tests. Resist the temptation to skip the
mocha port of `GwtStateTreeTest` / `GwtTreeChangeProcessorTest` because
they look long; they are the safety net.

## Effort estimate

Each tier is a focused unit of work:

| Tier | Class count | LOC | Estimated days | Notes |
|---|---|---|---|---|
| P1-P3 | — | — | 1-2 | Inventory + harness + CI confirmation |
| 1 | 8 | 600 | 1-2 | Reactive — needs care |
| 2 | 13 | 800 | 1-2 | NodeMap/NodeList/MapProperty |
| 3 | 5 | 400 | 1 | DOM wrappers |
| 4 | 6 | 2000 | 3-4 | **Highest risk** — state tree |
| 5 | 8 | 2500 | 3-4 | Binding strategy is huge |
| 6 | finishes | small | 0.5 | Cleanup |
| 7 | finishes | small | 1 | JVM fallback drops |
| 8 | 6 | 1500 | 2 | Application kernel |
| 9a/9b/9c | 22 | 3000 | 4-5 | Communication layer in 3 PRs |
| 10 | finishes | 500 | 1 | App connection + bootstrap |
| 11 | 1 | 100 | 0.5 | ValueMap delete |
| 12 | 7 | 800 | 1-2 | Vendored + collection cleanup |
| 13 | — | — | 1-2 | Tear-down + flow-tests verification + parent-repo cross-checks |
| **Total** | | | **~21-30 days** | ~25-40 commits |

Risk multipliers:

- If GwtTest pipeline isn't running on real CI today (P3), add 5-10 days
  for porting all GwtTests up-front rather than relying on them.
- If `flow-tests` integration suite turns up regressions during Tier 4
  or 5, debugging the translation can add several days per incident.

## What's blocking starting Tier 1

1. P1 inventory done and reviewed.
2. P2 mocha harness landed.
3. P3 CI status confirmed.

These three pre-work items unblock the entire plan and should be the
next commits on this branch.

## Open questions

- Are there flow-server tests or flow-tests scenarios that mock or stub
  `com.vaadin.client.*` types? A grep across the parent repo should
  catch this before Tier 13.
- Does any external add-on or Vaadin module reference the
  `META-INF/resources/VAADIN/static/client/*.cache.js` GWT bundle path?
  If yes, the tear-down needs a deprecation path.
- Should the migrated TS modules be exposed as a published npm package
  (e.g. `@vaadin/flow-client`) once the GWT plugin is gone? If yes,
  Tier 13 also expands the `package.json` `files` and `main` entries.
