# Migrating flow-client from GWT to TypeScript

This document is the working plan and status for converting `flow-client` from
GWT/Java to TypeScript, incrementally, on the long-lived branch
`flow-client-jsinterop-exports` (one PR).

> **Core rule: at no point is the same logic implemented twice.** Each commit
> migrates one piece to TypeScript and deletes the Java body for that piece in
> the same commit. Existing tests (GwtTests, ITs) plus a new `*Tests.ts` mocha
> test prove it still works.

## Direction: import (TS implementation, Java delegates)

An earlier revision of this file proposed the *export* direction (TS calls into
`@JsType`-exported Java, migrating top-down). We did **not** take that path. The
adopted approach is the **import direction**, because it lets us move code one
small leaf at a time and clean up as we go:

- Each leaf's logic moves to a TypeScript module under
  `src/main/frontend/internal/<Name>.ts`.
- The functions are registered on `window.Vaadin.Flow.internal.<Name>` by
  `src/main/frontend/internal/registerInternals.ts`, which runs at the very
  start of the engine's `init()` (see `FlowClient.js` / `scripts/client.js`),
  so the implementations exist before any GWT code runs, in every bootstrap
  path.
- The Java class keeps its method signatures, but each migrated method becomes
  a `native` JSNI one-liner that delegates, e.g.

  ```java
  public static native boolean hasTag(Node node, String tag)
  /*-{
      return $wnd.Vaadin.Flow.internal.ElementUtil.hasTag(node, tag);
  }-*/;
  ```

The boundary stays unidirectional per call (GWT -> TS). Java callbacks that must
fire are passed *down* as plain JS functions, wrapped in `$entry` on the Java
side (see the rules below).

## The GwtTest harness

GwtTests run the GWT-compiled engine in HtmlUnit and never call the engine's
`init()`, so the `window.Vaadin.Flow.internal.*` implementations would be
missing. To avoid duplicating them:

- `src/test/frontend/gwtTestInternals.ts` is an esbuild entry that imports and
  calls `registerInternals()`.
- `npm run build-gwt-test-internals` (wired into the pom `process-test-classes`
  phase) bundles it and transpiles it to **ES5**, into
  `target/test-classes/.../gwtTestInternals.js`.
- `ClientEngineTestBase` embeds that bundle via a GWT `ClientBundle` and
  `eval`s it in `gwtSetUp()`, so every GwtTest registers the same TypeScript
  implementations production uses.

### Why ES5, and other HtmlUnit constraints

The (old) HtmlUnit used by GwtTests parses only ES5. esbuild **cannot** lower
`const` to ES5, so the build esbuild-bundles (resolving imports) and then runs
`tsc -p tsconfig.gwt-test.json` (`target: es5`) over the bundle. Consequences
for migrated TS:

- **No unicode regex flag** (`/.../u`) -- HtmlUnit rejects it. Use `/.../`.
- Avoid syntax tsc/esbuild can't reduce to ES5-safe output. Runtime ES2015 APIs
  (`Array.from`, `String.includes`, `Promise`) are fine -- HtmlUnit FF38 has
  them; `Promise` is polyfilled in `gwtSetUp`.

## Rules learned the hard way

- **`$entry` only on top-level callback entry points** (event handlers, promise
  `then` callbacks, `onSuccess`/`onError`). Do **not** `$entry`-wrap a
  supplier/function whose exception is *expected and caught* by surrounding
  logic: `$entry` also reports it to GWT's uncaught-exception handler, so it is
  logged twice. mocha can't catch this; a CI IT did
  (`ResourceLoader.runPromiseExpression` / `DynamicDependencyIT`).
- **Stays in Java**: GWT compiler casts (`crazyJsCast`), `GWT.isScript()`
  branches, pure-Java helpers (e.g. `Supplier` null checks), and native
  `@JsType(isNative=true)` interfaces.
- **Each migration gets a `src/test/frontend/<Name>Tests.ts`** mocha test for
  its logic.
- **`$wnd` vs the GWT module window in GwtTests.** The bundle is eval'd in
  `$wnd` (host window), so bundle code resolves globals (`Promise`, `Set`,
  `Map`, `customElements`, ...) from `$wnd`. `ClientEngineTestBase`'s JSNI
  polyfills install onto `window` (the GWT *module/iframe* window). Any global a
  migrated function relies on must therefore also be set up on `$wnd` in
  `installPolyfills()` (collections already are; `$wnd.Promise = window.Promise`
  mirrors the synchronous test Promise so deferred bundle `.then` callbacks run
  immediately, as the engine's JSNI did when it ran in the module window). When
  a migration starts depending on a new global, check the harness covers `$wnd`.

## Validation

Per leaf, before committing:

1. `npm run lint` (the **full** lint, as CI runs it -- per-file eslint misses
   project-wide rules such as the default-project file cap).
2. `npx tsc --noEmit`.
3. `npm run build-gwt-test-internals`, then mocha
   (`npx web-test-runner --node-resolve --playwright`).
4. A GwtTest as an ES5 **parse check** of the bundle, e.g.
   `mvn -o -pl flow-client test-compile gwt:test -Dgwt.test.pattern='**/GwtWidgetUtilTest.java'`.

Then push and wait for the **Flow Validation** CI to go green (unit-tests +
it-tests). CI is watched via a GitHub App (key `github.pem`, app `3960142`,
installation `137938441`): mint a JWT -> installation token, push
`HEAD:flow-client-jsinterop-exports`, poll `actions/runs?head_sha=`. The App can
push (contents) but **cannot** re-run workflows (`actions: write` missing) -- ask
a human to re-run flaky jobs.

### Local validation limit: push GwtTests, and the bundle-rebuild trap

`mvn` (offline **and** online, locally) cannot compile the *push* GwtTests --
it fails with `Rebind result 'AtmospherePushConnection$Factory' could not be
found`. This is a local-environment artifact; CI compiles them fine. So for
push-coupled classes, validate logic with mocha and rely on CI. Validate the
ES5 bundle parses with a non-push GwtTest (`GwtWidgetUtilTest`).

The *binding/Polymer* GwtTests (`GwtBasicElementBinderTest`,
`GwtPolymerModelTest`) **do** compile and run locally and are worth running for
binding changes -- but note: `mvn test-compile gwt:test` only re-runs the
`build-gwt-test-internals` npm step when *test sources* change, so editing
`src/main/frontend` alone leaves a **stale** bundle in
`target/test-classes/.../gwtTestInternals.js`. Before each local GwtTest run,
explicitly `npm run build-gwt-test-internals` and verify the deployed bundle
matches your edit. (`gwt:test` without `test-compile` does not run the tests.)
HtmlUnit does not surface `console.log`; isolate behaviour with discriminating
experiments (force/bypass a code path) rather than logging.

## Status

### Done (migrated to TS, CI-green)

`GwtTest harness` (ClientBundle + eval) · `ElementUtil` · `WidgetUtil` (URL,
JS-property, JSON helpers) · `LitUtils` · `ReactUtils` · `ConnectionIndicator` ·
`BrowserInfo` (the three environment probes; browser/OS parsing stays in the
shared `BrowserDetails` Java class) · `XhrConnection.resendRequest` ·
`ExecuteJavaScriptElementUtils.isPropertyDefined` · `SystemErrorHandler`
(recreateNodes, showPopover, getShadowRootElement) · `ClientJsonCodec`
(createReturnChannelCallback, applyCaptures) · `ResourceLoader` (5 helpers) ·
`MessageHandler` (removeStylesheetByIdFromDom, callAfterServerUpdates,
calculateBootstrapTime, parseJSONResponse, getFetchStartTime) · `PolymerUtils`
(DOM probes, shadow-root helpers, **and** model-data writers
setListValueByIndex/splice/storeNodeId/setProperty) · `AtmospherePushConnection`
(isAtmosphereLoaded, doPush, doDisconnect, **doConnect**, **createConfig**) ·
`ExecuteJavaScriptProcessor.getContextExecutionObject` (Java builds the
`$entry`-wrapped `getNode` + lifecycle callbacks, TS assembles the executeJs
context object) · `StorageUtil` (local/session get+set) ·
`MessageSender.sendBeacon` · `SimpleElementBindingStrategy`
(bindPolymerModelProperties + hookUpPolymerElement -- the Polymer
`_propertiesChanged`/`ready`/dom-repeat monkey-patching; Java builds the
`$entry` node/tree-capturing callbacks, TS does the DOM wiring).

**The real-logic leaf phase is complete.** Every JSNI block that contained
actual JS *logic* now has that logic in TypeScript, with the Java side reduced
to a thin `native` delegation (plus, where callbacks fire back into GWT, the
`$entry` wrappers that must stay Java-side). What remains is the GWT-runtime
substrate -- see below.

### Remaining: structural tier (needs bottom-up caller migration, NOT leaf delegation)

These cannot be advanced by the import-direction delegation pattern: they have
no JS *logic* to move (a JSO `this[key]` accessor or a GWT compiler intrinsic),
so delegating them would leave the JSNI in place while adding indirection --
churn without progress. They become plain TypeScript only once their Java
*callers* are themselves TS, i.e. by flipping direction and migrating the
consumer subsystems (the binding layer, message handling, bootstrap) top-down.
That is a larger architectural effort than the leaf phase and should be planned
as such.

- **JSO overlay types** (`extends JavaScriptObject`, trivial `this[key]`
  accessors): `ValueMap`, `JsoConfiguration`, `ErrorMessage`, `ServerEventObject`,
  `AtmospherePushConnection`'s `AbstractConfiguration` get/set accessors,
  `ApplicationConnection.Styles`, and `Profiler`'s `GwtStatsEvent` (so `Profiler`
  waits too; it is also dev-only and untested).
- **GWT compiler intrinsics / pure-Java**: `WidgetUtil.crazyJsCast`/`crazyJsoCast`,
  `ApplicationConnection.getProfilingData` (reads `MessageHandler` private
  fields), `Xhr.create` (a single elemental `new window.XMLHttpRequest()` with an
  awkward `Window` param).
- **Hot collection abstractions**: `JsArray`, `JsCollections` (per-call
  indirection on hot paths).
- **`DomApi`**: a Polymer-1.x compatibility shim of native `@JsType` interfaces
  consumed by ~11 Java callers; the interfaces can't move until the callers do.
- **`Console`**: every method branches on `GWT.isScript()` and it owns the
  uncaught-exception-handler machinery.
- **`Bootstrapper`**: bootstrap-timing -- `vaadinBootstrapLoaded()` checks
  `$wnd.Vaadin.Flow`, which `registerInternals` itself creates, so a delegating
  version would always return true. Needs a different approach.

## How to resume

The leaf phase is done, so resuming means starting the **structural tier**,
which is a different kind of work: instead of moving a leaf's body to TS and
delegating, pick a consumer subsystem (e.g. the message-handling path, the
binding layer, or bootstrap) and migrate it together with the GWT-substrate
types it uses (`JsArray`/`JsCollections`, the JSO overlays, `DomApi`), so those
types become plain TS rather than `@JsType`/`JavaScriptObject` shims. This
breaks the "one tiny no-duplicate commit per leaf" cadence -- scope and sequence
it deliberately (which subsystem first, how to keep the build green across the
larger change) before starting. The import-direction pattern and the
validation/CI steps above still apply to whatever leaves fall out of each step.

## Why the import-direction pattern has reached its limit

The import-direction pattern (TS implementation + thin Java `native`
delegation) works only for classes whose method **bodies are JS logic** wrapped
in JSNI -- concrete static/instance methods that take and return
JS-representable values. Every such class is now migrated (plus `ErrorMessage`
removed and `LocationParser` ported and deleted, the first Java file gone).

What remains is ~78 **pure-Java** files (no JSNI) that form the GWT client core:
`StateNode`/`StateTree`, the `nodefeature` package, the `binding` package, the
`reactive` package, `Debouncer`, `DomApi`, and the collection/event/listener
types. These cannot use the import-direction pattern, for concrete reasons
verified in the code:

- **Abstract classes with Java subclasses.** `Computation` is `abstract`
  (`doRecompute()`); `ReactiveEventRouter<L,E>` is `abstract` + generic and is
  extended by `MapProperty`, `NodeMap` and `NodeList`. A TS class cannot be the
  superclass of a Java `abstract`-method subclass, so the class and all its
  subclasses must move together.
- **Interfaces implemented by Java.** `ReactiveValue`, `FlushListener`,
  `InvalidateListener`, `ReactiveValueChangeListener` are implemented by Java
  lambdas/classes throughout; the interface can't move without its implementers.
- **Mutual recursion / shared state.** `Reactive` (static coordinator) and
  `Computation` call each other and share the flush queue + current-computation
  state; they are one unit.
- **Generics, Java type identity, and `JsArray`/`JsSet` of Java objects** thread
  through all of the above.

Consequence: the reactive cluster alone (`Reactive`, `Computation`,
`ReactiveEventRouter`, `ReactiveValue`, the 3 listener interfaces, the 2 events)
plus its consumers (`MapProperty`/`NodeMap`/`NodeList`, ~14 files using
`Reactive`, ~9 using `ReactiveValue`) is a single **all-or-nothing connected
component of ~20-30 files**. There is no partial change that compiles, so it
cannot be done as incremental green commits -- it is a wholesale rewrite of a
core component.

## Recommended approach for the core rewrite (a project, not a loop)

This is a multi-session project, sequenced bottom-up by connected component:

1. **Collections + base types first** (`JsArray`, `JsSet`, `JsMap`,
   `JsWeakMap`, `JsCollections`): make them genuine TS modules/types that both
   TS and the remaining Java can use. They are the substrate everything else
   needs.
2. **Reactive component** (`Reactive`, `Computation`, `ReactiveEventRouter`,
   `ReactiveValue`, listeners, events) as one unit, re-expressing the abstract
   class / generic-router relationships in TS (composition + callbacks instead
   of `abstract` inheritance).
3. **Node features** (`nodefeature` package) on top of the TS reactive layer.
4. **State tree** (`StateNode`, `StateTree`), then **binding**, then the
   remaining consumers.

Each stage: convert the component to TS, replace the Java consumers' usages
(the consumers move in the same or the next stage -- top-down within the stage),
and **port the component's JVM unit tests** (e.g. `ReactiveTest`,
`ComputationTest`, the `nodefeature`/`binding` `Gwt*`/JUnit tests) to mocha. The
GWT engine's `init()` would shrink to a TS bootstrap as the last consumers move.
Expect red intermediate states within a stage; keep each *stage* (not each
commit) green.

## Build-alongside execution (in progress)

To keep the shared branch green throughout the core rewrite, the TS
implementation is built **alongside** the Java (temporarily duplicated), each
layer mocha-tested in isolation, until a final cutover wires the Java consumers
to the TS modules and deletes the Java. The not-yet-ported classes a layer
depends on are declared as TS contracts that later layers (and ultimately the
real classes) satisfy.

Layers landed so far (`src/main/frontend/internal/`), all green:

1. **Reactive core** -- `reactive/reactive.ts`: `Reactive`, `Computation`,
   `ReactiveEventRouter` and the event/listener/value types. Tests:
   `ReactiveCoreTests.ts`.
2. **`MapProperty`** -- `nodefeature/MapProperty.ts` (+ change event/listener).
   Tests: `MapPropertyTests.ts`.
3. **Node features** -- `nodefeature/NodeFeature.ts`, `NodeMap.ts`, `NodeList.ts`
   (+ property-add / splice events and listeners). Tests: `NodeMapTests.ts`,
   `NodeListTests.ts`.
4. **`StateNode`** -- `StateNode.ts` (+ node-unregister event/listener; the Java
   `Class<?>`-keyed `nodeData` becomes a map keyed by JS constructor function).
   Tests: `StateNodeTests.ts`.

Remaining: `StateTree` (the `Registry`/server-communication seam), the binding
layer, then the cutover.
