# P1 — Test coverage inventory

> Per [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) step P1. Classifies every
> `@Test` method in `src/test/java` and every `public void test*` /
> `public void <camel>` method in `src/test-gwt` as one of:
>
> - **PORT** — covers behaviour worth preserving; needs a mocha
>   equivalent before the underlying Java class can be removed.
> - **DROP** — tests JVM-fallback code, GWT-specific build quirks, or
>   language-primitive behaviour that the JS engine itself tests. Goes
>   away with the Java.
> - **INTEGRATION** — high-level scenario already covered by the
>   `flow-tests` integration suite; loss is acceptable.
>
> Counts: **176** test methods total (49 test files). After classification:
> ~**130 PORT**, ~**40 DROP**, ~**6 INTEGRATION**.
>
> Progress (as of 2026-05-19): **34 DROP done** (Jre*Test ×4, AssertionTest,
> CodeTest, DomApiAbstractionUsageTest, plus inventory adjustments), **9
> PORT done** (ReactiveTest → ReactiveTests.ts).
>
> This is a starting classification — reviewers should challenge specific
> rows before the corresponding migration tier starts. Once a row turns
> green (mocha equivalent merged or row dropped), it's no longer a
> blocker for its tier.

Legend: ☐ pending · ✅ mocha-ported or dropped

## `src/test/java` — JVM JUnit suite (~106 methods, 30 files)

### Pure dead-code candidates (DROP)

These exercise the `Jre*` fallback collections that disappear when the
Java side uses native JS types directly (Tier 12 of the plan).

| File | Tests | Class | Status |
|---|---:|---|---|
| `flow/collection/JreArrayTest` | 14 | DROP — fallback for JsArray | ✅ deleted |
| `flow/collection/JreMapTest` | 3 | DROP — fallback for JsMap | ✅ deleted |
| `flow/collection/JreSetTest` | 4 | DROP — fallback for JsSet | ✅ deleted |
| `flow/collection/JreWeakMapTest` | 2 | DROP — fallback for JsWeakMap | ✅ deleted |
| `AssertionTest.testAssertionsAreEnabled` | 1 | DROP — verifies `-ea` JVM flag, irrelevant in TS | ✅ deleted |
| `CodeTest.gwtGenerics` | 1 | DROP — verifies GWT compiler quirk | ✅ deleted |
| `DomApiAbstractionUsageTest.testDomApiCodeNotUsed` | 1 | DROP — Java-source static analysis; loses meaning when Java is gone | ✅ deleted |
| `flow/ClientEngineSizeIT.testClientEngineSize` | 1 | DROP — GWT bundle size budget; bundle goes away in T13 | ☐ defer to T13 |
| `communication/AbstractConfigurationTest` | 0 | DROP — abstract base, no tests of its own | ☐ goes away with its concrete subclasses |

### PORT — pure-Java logic that needs a mocha equivalent

Each row gets ported when its target class enters the corresponding tier
of [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md).

| File | Tier | Tests | Status | Notes |
|---|---|---:|---|---|
| `ExistingElementMapTest` | T7 | 2 | ☐ | Already migrated; tests still pass against the JVM fallback. Port to mocha + delete in T7. |
| `RegistryTest` | T8 | 3 | ☐ | setAndGet, getUndefined, setAndGetCustom |
| `UILifecycleTest` | T8 | 7 | ☐ | State machine transitions + listeners |
| `DependencyLoaderTest` | T8 | 5 | ☐ | loadStylesheet, loadScript, loadMultiple, eager ordering, inline ordering |
| `InitialPropertiesHandlerTest` | T8 | 3 | ☐ | flushPropertyUpdates_* — interaction with state tree, NOT covered by flow-tests at this granularity |
| `flow/ExecuteJavaScriptProcessorTest` | T4 | 12 | ☐ | execute_*, isBound_* — node-parameter handling + bound-state walks |
| `flow/StateNodeTest` | T4 | 4 | ☐ | testDefaultNoFeatures, testGetListFeature, testGetMapFeature, setNodeData_getNodeData_retrievedInstanceIsTheSame |
| `flow/StateTreeTest` | T4 | 23 | ☐ | The state-tree protocol: id mappings, register/unregister, property-sync gating, visibility cascade, prepareForResync. **High-priority port.** |
| `flow/TreeChangeProcessorTest` | T4 | 14 | ☐ | put / mapRemove / mapReAdd / splice / detach / parent linkage. **High-priority port.** |
| `flow/nodefeature/MapPropertyTest` | T2 | 19 | ☐ | Reactive change listeners, syncToServer gating, alwaysUpdate strategy. **Critical for binding correctness.** |
| `flow/nodefeature/NodeListTest` | T2 | 5 | ☐ | initialEmpty, splice, spliceEvents, reactive, forEach |
| `flow/nodefeature/NodeMapTest` | T2 | 9 | ☐ | newMapEmpty, propertyCreation, property change listeners |
| `flow/reactive/ComputationTest` | T1 | 8 | ☐ | Computation lifecycle, dependencies, recompute |
| `flow/reactive/ReactiveTest` | T1 | 9 | ✅ ported | → `src/test/frontend/ReactiveTests.ts`. JUnit class deleted. |
| `communication/PollConfiguratorTest` | T9b | 1 | ☐ | listensToProperty |
| `communication/ReconnectConfigurationTest` | T9b | 7 | ☐ | defaults, setGet*, reactsToChanges, changesReportedInOneBatch |

## `src/test-gwt` — GWT browser test suite (~74 methods, 24 files)

### DROP — collection / language primitive tests

GWT-side counterparts of the Jre tests. They test that `JsArray` /
`JsMap` / `JsSet` / `JsWeakMap` work as @JsType bindings to the JS
globals. The JS engine itself guarantees the underlying behaviour.

| File | Tests | Class |
|---|---:|---|
| `flow/collection/GwtJsArrayTest` | 14 | DROP — language primitive |
| `flow/collection/GwtJsMapTest` | 4 | DROP — language primitive |
| `flow/collection/GwtJsSetTest` | 5 | DROP — language primitive |
| `flow/collection/GwtJsWeakMapTest` | 3 | DROP — language primitive |
| `flow/util/GwtNativeFunctionTest` | 3 | DROP — tests the `new Function(...)` helper which becomes a TS-side detail |
| `flow/dom/GwtDomApiTest.testPolymerMicroDependencyLoaded` | 1 | DROP — tests dependency-loading scaffolding gone after migration |
| `flow/dom/GwtDomApiTest.testPolymerFullDependencyLoaded` | 1 | DROP — same |
| `flow/dom/GwtDomApiTest.scheduleDeferred` | 1 | DROP — helper, not a test |
| `flow/dom/GwtPolymerApiImplTest.testPolymerMicroLoaded` | 1 | DROP — Polymer-availability check |
| `flow/dom/GwtPolymerApiImplTest.testPolymer2` | 1 | DROP — Polymer-availability check |

### INTEGRATION — covered well enough by `flow-tests`

| File | Test | Class |
|---|---|---|
| `GwtApplicationConnectionTest.test_should_not_addNavigationEvents_forWebComponents` | INTEGRATION | Web component embedding is covered by `flow-tests/test-embedding` |
| `GwtDefaultConnectionStateHandlerTest.test_browserEvents_stopsHeartbeats` | INTEGRATION | Reconnect flow has dedicated `flow-tests/test-router-ui` scenarios |
| `GwtDefaultConnectionStateHandlerTest.test_onlineEventFollowedByOffline_connectionLost` | INTEGRATION | same |
| `GwtDefaultConnectionStateHandlerTest.test_onlineEventHeartbeatSucceeds_connected` | INTEGRATION | same |
| `GwtDefaultConnectionStateHandlerTest.test_onlineEventButHeartbeatFails_continuesReconnectingAndFinallyGivesUp` | INTEGRATION | same |
| `flow/GwtErrotHandlerTest.testhandleUnrecoverableError_textContentIsSetInDivsNotInnerHtml` | INTEGRATION | UI-visible error rendering |

### PORT — protocol / behaviour-critical

| File | Tier | Tests | Notes |
|---|---|---:|---|
| `GwtDependencyLoaderTest` | T8 | 3 | testAllEagerDependenciesAreLoadedFirst, testEnsureLazyDependenciesLoadedInOrder, testDependenciesWithAllLoadModesAreProcessed |
| `GwtExecuteJavaScriptElementUtilsTest` | T4 | 11 | testAttachExistingElement_* (5), testPopulateModelProperties_* (5), testReturnChannel_passedToExecJavaScript_messageSentToServer |
| `GwtMessageHandlerTest` | T9a | 7 | Module/dynamic dependency ordering vs tree changes; resync; session expired / unrecoverable error gating |
| `GwtWidgetUtilTest.testgetAbsoluteUrl` | T7 | 1 | URL resolution behaviour |
| `communication/GwtAtmospherePushConnectionTest.testDisconnect_disconnectUrlIsSameAsInConnect` | T9c | 1 | Push lifecycle |
| `communication/GwtLoadingIndicatorStateHandlerTest` | T9b | 6 | Loading state machine; UI-visible muting rules |
| `flow/GwtBasicElementBinderTest` | T5 | 71 | **The single biggest port.** Property/attribute binding, children, virtual children, events, classes, styles, visibility, shadow-DOM ready callbacks. Split across 3-4 mocha files. |
| `flow/GwtEventHandlerTest` | T5 | 6 | sendTemplateEventToServer, server-event handler dispatch, client-callable promises, add/remove of server-event handlers |
| `flow/GwtMultipleBindingTest` | T5 | 11 | Double-bind regression scenarios for each binding aspect |
| `flow/GwtPolymerModelTest` | T5 | 21 | Polymer property + list propagation; updatable-property gating |
| `flow/GwtPropertyElementBinderTest` | T5 | 8 | sendEventToServer, sendNodePropertySyncToServer, sendExistingElementWithIdAttachToServer, clearSynchronizedProperties, DOM listener synchronisation, flush-on-DOM-event |
| `flow/GwtStateNodeTest.testNodeData_getNodeData_sameInstance` | T4 | 1 | NodeData identity |
| `flow/GwtStateTreeTest` | T4 | 5 | sendTemplateEventMessage, sendTemplateEventToServer_delegateToServerConnector, deferredTemplateMessage_isIgnored, prepareForResync_unregistersDescendantsAndClearsRootChildren, prepareForResync_rejectsPendingPromise |
| `flow/GwtTreeChangeProcessorTest.testPrimitiveSplice` | T4 | 1 | Splice into a primitive list — small but important |
| `flow/util/GwtClientJsonCodecTest` | T6 | 13 | decode/encode round-trips, @v-node / @v-return / @v-type sentinels, deeply nested recursive decoding |

## How to use this inventory

1. Before starting a tier (per `MIGRATION_PLAN.md`), grep this file for
   the tier identifier (T1, T2, …). Each PORT row in that tier needs a
   mocha test in the same commit as the class migration. Each DROP row
   in that tier can be deleted in the same commit. INTEGRATION rows are
   deleted without porting; flow-tests is the safety net.
2. When a mocha port lands, change the `☐` glyph in the row to `✅` and
   add the mocha file path so the trail is visible.
3. When the count of remaining `☐` rows in a tier reaches zero, the tier
   is ready for the Java-side migration commit.

## Open questions raised by the inventory

- `DomApiAbstractionUsageTest` checks that `DomApi.wrap()` is used
  everywhere instead of direct `elemental.dom.Element` casts. After the
  Java code is gone this test is meaningless, but the *intent* (don't
  bypass the polymer-aware wrapper) is still valuable in TS. Consider
  encoding the rule as an eslint rule in T3 (`flow.dom` migration)
  instead of porting the test.
- `ClientEngineSizeIT` enforces a 800 KB budget on the GWT bundle. The
  TS bundle has its own size profile; agree the new budget before T13.
- `GwtPolymerModelTest` (21 tests) and `GwtBasicElementBinderTest` (71
  tests) together cover most of the binding subsystem. Splitting them
  into manageable mocha files during T5 needs a concrete plan; suggest
  one mocha file per "feature area" (properties, attributes, children,
  events, classes, styles, visibility, virtual children, polymer
  hooks, polymer-list propagation).
- The current GwtTest suite is env-blocked locally (`libXtst.so.6`).
  Need to confirm with P3 whether real CI runs the suite *today* before
  trusting the existing assertions as the reference.
