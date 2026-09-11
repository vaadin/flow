<!--
 Copyright 2000-2026 Vaadin Ltd.

 Licensed under the Apache License, Version 2.0 (the "License"); you may not
 use this file except in compliance with the License. You may obtain a copy of
 the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 License for the specific language governing permissions and limitations under
 the License.
-->

# Deferred TypeScript improvements

Places where the port is faithful and correct, but TypeScript could express the
same thing better. They are **recorded here and deliberately not acted on** while
the migration is in progress: the port's value during review is that every module
can be read line-by-line against its Java original, and an improvement that
deviates from the Java shape destroys that for the sake of a change that can just
as well happen later.

The cutover PR — the one that removes the Java client — owns this list and
empties it.

An entry qualifies only if all three hold:

1. the current code is a **correct, faithful** port; if it is not, it is a review
   finding, not an improvement;
2. the better form **deviates from the Java shape**; if it does not, make the
   change now instead of filing it;
3. the benefit is **concrete** — a class of bug the compiler would catch, a cast
   removed, an API callers can no longer misuse, or code that disappears.

The porting conventions the series was written against (`PORTING.md`) and the
procedure a port was reviewed with (`PORTING-REVIEW.md`) were both removed once
the porting series had landed and the retrofit backlog they tracked was empty,
so this file is the only one of the three left. A parity debt is therefore an
ordinary bug report now, not a backlog row, and only entries meeting the tests
above belong here.

## Stronger typing

### `BinderContext.getStrategies` — infer the strategy type from a type guard

- **Site**: `client/flow/binding/BinderContext.ts`, `client/flow/binding/Binder.ts`
- **Java shape**:
  `<T extends BindingStrategy<?>> JsArray<T> getStrategies(Predicate<BindingStrategy<?>>)`.
  `T` is caller-declared and unchecked, and `Binder` needs
  `@SuppressWarnings({"unchecked", "rawtypes"})` plus a `(T) strategy` cast. The
  Javadoc says the predicate exists only because `Class<T>#isInstance()` is
  unavailable in GWT, and that avoiding a `ClassCastException` is "the developer
  responsibility".
- **What TypeScript could do**: take a type guard, so the caller cannot mismatch —
  ```ts
  getStrategies<T extends BindingStrategy<Node>>(
    predicate: (strategy: BindingStrategy<Node>) => strategy is T
  ): T[];
  ```
  `T` is then inferred from the predicate, a wrong guard fails to compile at the
  guard itself, and the `as T[]` in `Binder` disappears — `filter` already returns
  `T[]`. The port would need no cast where Java needs two suppressions.
- **Why it waits**: the current form is a faithful port of the Java signature, and
  changing it diverges from `BinderContext.java` while both trees are still
  reviewed against each other.
- **Spotted in**: #24949

### Feature and property ids as literal unions instead of `number` / `string`

- **Site**: `flow/internal/nodefeature/NodeFeatures.ts`, `NodeProperties.ts`, and
  the `number` parameters of `StateNode.getMap` / `getList` / `hasFeature`
- **Java shape**: `NodeFeatures` is a class of `public static final int`, so every
  feature id is an `int` and Java has no way to constrain the parameter further.
- **What TypeScript could do**: derive a union from the ported registry —
  ```ts
  type FeatureId = (typeof NodeFeatures)[keyof typeof NodeFeatures];
  getMap(id: FeatureId): NodeMap;
  ```
  Passing a property id, an unrelated constant or an arbitrary integer then fails
  to compile instead of producing an empty feature at run time.
- **Why it waits**: the `number` signatures mirror Java's `int`, and narrowing them
  changes the ported API shape.
- **Spotted in**: #24947, #24948

### `MapProperty.getValueOrDefault` — one generic instead of three overloads

- **Site**: `client/flow/nodefeature/MapProperty.ts`
- **Java shape**: three overloads — `int`, `boolean`, `String` — because Java
  cannot express "returns whatever type the default is".
- **What TypeScript could do**:
  ```ts
  getValueOrDefault<T extends number | boolean | string>(defaultValue: T): T;
  ```
  One signature states the contract, and the implementation loses its
  `number | boolean | string` widening and the casts that go with it.
- **Why it waits**: the overload set is the faithful rendering of three Java
  methods, and collapsing it drops that correspondence.
- **Spotted in**: #24947

### `EventRemover` — a cleanup function instead of a one-method interface

- **Site**: `EventRemover.ts` and every registration site that returns one
- **Java shape**: mirrors `elemental.events.EventRemover`, an interface with a
  single `remove()`.
- **What TypeScript could do**: return `() => void` (or a `Disposable`), which is
  the idiomatic shape and removes a wrapper object allocation at every
  `addListener` call. Best done as one sweep, since it touches the whole tree.
- **Why it waits**: the interface is the faithful port of an elemental type that
  ported modules still reference.
- **Spotted in**: #24933

### `MapProperty.getValue` — a value type instead of `unknown`

- **Site**: `client/flow/nodefeature/MapProperty.ts`; casts at its consumers, e.g.
  `client/flow/binding/TextBindingStrategy.ts:53`,
  `client/flow/binding/SimpleElementBindingStrategy.ts:303`, `:667`, `:910`, `:917`,
  `client/PolymerUtils.ts:174`
- **Java shape**: `Object getValue()`, so every consumer casts — nine
  `getValue() as …` sites in the ported binding layer alone.
- **What TypeScript could do**: carry the type on the property, either as a generic
  `MapProperty<T>` or by typing the value as the union a feature can actually hold
  (`StateNode | JsonValue`). The type decision then lives in the one place that
  knows it, and the casts at the consumers disappear.
- **Why it waits**: `unknown` is the faithful rendering of `Object`, and the
  binding layer is still reviewed against Java code that casts in the same places.
- **Spotted in**: #24947, measured in #24949

### `ReactiveEventRouter` — drop the `wrapper` / `dispatcher` callbacks

- **Site**: `client/flow/reactive/ReactiveEventRouter.ts`, and its three
  construction sites `MapProperty.ts:49-50`, `NodeMap.ts:44-46`,
  `NodeList.ts:46-48`
- **Java shape**: `ReactiveEventRouter` is abstract with
  `protected abstract L wrap(ReactiveValueChangeListener)` and
  `protected abstract void dispatchEvent(L, E)`, because a Java listener is an
  interface instance that has to be adapted.
- **What TypeScript could do**: nothing — a listener already *is* a function type,
  so all three sites pass the identity (`(listener) => listener`) and an application
  (`(listener, event) => listener(event)`). The constructor can take just the
  reactive value, dropping two parameters and a layer of indirection from the
  reactive core.
- **Why it waits**: the callbacks are the faithful rendering of the two abstract
  methods, and the router is reviewed against the Java class that needs them.
- **Spotted in**: #24947

## Simplifications available once the constraint is gone

### `NodeFeature.isStateNode` — a real `instanceof StateNode`

- **Site**: `client/flow/nodefeature/NodeFeature.ts`
- **Java shape**: `getAsDebugJson` tests `value instanceof StateNode`.
- **What TypeScript could do**: the same `instanceof`, once it is possible. Today a
  *value* import of `StateNode` into `NodeFeature` would close a
  `NodeFeature → StateNode → NodeList/NodeMap → NodeFeature` class-initialisation
  cycle, so the import is type-only and the check is structural (documented at the
  site). A settled module graph after cutover makes the real check available, and
  it is stricter than the structural one.
- **Why it waits**: not a deviation anyone chose — the ESM cycle forces it.
- **Spotted in**: #24948

### The `JsArray` module — delete it and use native arrays

- **Site**: `client/flow/collection/JsArray.ts` and its call sites
- **Java shape**: `JsArray<T>` is a JS-array wrapper with `@JsOverlay` helpers, and
  it gets its own module because the port maps one Java source file to one
  TypeScript module.
- **What TypeScript could do**: nothing at all — every helper is a one-liner over a
  native array (`pushArray` → `push(...values)`, `clear` → `length = 0`,
  `isEmpty` → `length === 0`). Post-cutover the module can go and the call sites
  can inline the native operation.
- **Why it waits**: the module documents the Java→native mapping that reviewers
  check against, and one-module-per-Java-source-file keeps it while the Java file
  exists.
- **Spotted in**: #24933

### `ClientJsonCodec.decodeWithoutTypeInfo` / `encodeWithoutTypeInfo`

- **Site**: `client/flow/util/ClientJsonCodec.ts`
- **Java shape**: both are `public`, and both are no-ops on the compiled-JS path —
  the JVM-side conversions exist for tests only.
- **What TypeScript could do**: delete them and let the call sites use the value
  directly, once nothing has to line up with the Java public API.
- **Why it waits**: the port carries the complete public API of the ported class,
  so a member is absent only when Java has none.
- **Spotted in**: #24948

### Always-on `assert` and runtime `Profiler` enablement

- **Site**: `assert.ts`, `client/Profiler.ts`
- **Java shape**: GWT strips `assert` from production builds and enables the
  profiler through deferred binding on `vaadin.profiler`, both compile-time
  mechanisms with no TypeScript equivalent, so the port keeps assertions always on
  and swaps deferred binding for a runtime `setEnabled`.
- **What TypeScript could do**: gate both behind a build-time flag the bundler can
  fold, so assertions and the profiler compile out of production bundles the way
  they did in GWT.
- **Why it waits**: it needs a bundler-level decision and touches how the engine is
  built, not just how a module is written.
- **Spotted in**: #24933

### `Reactive`'s static state — an instance instead of module-level `let`s

- **Site**: `client/flow/reactive/Reactive.ts:27-31`, and the `reset()` below it
- **Java shape**: five `private static` fields on `Reactive`, plus a `reset()`
  documented as *"Intended for test cases … Should never be called from non-test
  code!"* — Java has no cheaper way to give the flush cycle a scope.
- **What TypeScript could do**: make the reactive scope an instance the engine owns
  and a test constructs per case. That removes a test-only member from the public
  API and the cross-test coupling that currently makes `Reactive.reset()` mandatory
  in every `beforeEach`.
- **Why it waits**: module-level state is the faithful rendering of Java statics,
  and every ported consumer reaches `Reactive` as a namespace.
- **Spotted in**: #24947

### `BrowserDetails` / `BrowserInfo` — parse the user agent with a library

- **Site**: `flow/shared/BrowserDetails.ts` (~850 lines), `client/BrowserInfo.ts`
- **Java shape**: hand-written user-agent sniffing, and the Java class is
  `@Deprecated` — `BrowserInfo.ts` carries that deprecation on 15 members, each
  saying to *"use a parsing library like ua-parser-js to parse the user agent"*.
- **What TypeScript could do**: exactly what the deprecation asks. The pair reduces
  to a thin adapter over a parser, and the version/OS matrices the suite pins today
  become the library's problem.
- **Why it waits**: the port carries the complete public API of the ported class
  while the Java class exists, deprecated or not.
- **Spotted in**: #24933

### `Profiler` — drop the GWT stats-event protocol

- **Site**: `client/Profiler.ts` — 33 references to `__gwtStatsEvent` /
  `GwtStatsEvent` across 629 lines
- **Java shape**: the profiler reports through GWT's `__gwtStatsEvent` hook so GWT
  dev tooling can consume it, and the port keeps the payload shape including a
  `MODULE_NAME = ''` placeholder, since `GWT.getModuleName()` has no analogue.
- **What TypeScript could do**: delete the protocol once GWT is gone — nothing
  listens to it — leaving the timing tree and its consumer callback.
- **Why it waits**: it is the faithful port of the Java class, and the hook is still
  live while the GWT client is the running engine.
- **Spotted in**: #24933

## Test-side ergonomics

### `stateTreeTestRegistry` — a `registryWith(overrides)` instead of whole literals

- **Site**: `src/test/frontend/internal/client/flow/stateTreeTestRegistry.ts`, plus
  the four places that still build a `Registry` literal of their own —
  `client/flow/StateTreeTests.ts`, `client/flow/util/ClientJsonCodecTests.ts`,
  `client/flow/binding/BinderTests.ts`, `client/flow/bindingTestHelpers.ts`
- **Java shape**: none. The fakes stand in for the ported `Registry` slice, and the
  GWT tests build their own registry per test class as well.
- **What TypeScript could do**: let a suite state only the member it cares about —
  ```ts
  const registry = registryWith({ getServerConnector: () => ({ sendReturnChannelMessage: record }) });
  ```
  merging over `inertRegistry()`. Today one recording member costs a suite all five
  `Registry` members and, if the member is on the connector, all six of its methods.
  That is why the helper's own promise — *"a member added to `Registry` is filled in
  once"* — does not hold: adding one still means editing four more files.
- **Why it waits**: not the Java shape — **this entry fails admission test 2** and
  could land at any time. It waits only because two of the four sites belong to
  branches below, and editing them is churn while those are read against Java. The
  next PR to touch these suites should simply do it rather than inherit the note.
- **Spotted in**: #24949

## Deviations that fix a Java defect

Places where the port deliberately does **not** mirror the Java behaviour because
the Java behaviour is a bug. Each is documented at the site; they are listed here
so a review does not read them as porting slips.

- **`Debouncer.flushAll` guards the intermediate branch** — an intermediate-only
  debouncer has no idle timer and clears its buffered command on every tick, so
  between two ticks `flushAll` reaches the intermediate branch with a null
  command and a null command map. `Debouncer.java:288` dereferences both, so the
  `NullPointerException` aborts the flush for every remaining debouncer too. The
  port skips that debouncer instead. Spotted in #24949.

## Rejected candidates

Shapes that look like entries but are not, recorded so a later sweep does not
re-propose them.

- **`TreeChangeProcessor`'s non-null access** — no redundant `node!` exists: the
  `assert` helper is declared `asserts condition`, so `assert(child !== null, …)`
  narrows and the following statements need no assertion. Already idiomatic.
- **`WidgetUtil`'s `Record<string, unknown>` property helpers** — dynamic property
  access is the purpose of `getJsProperty` / `setJsProperty` / `deleteJsProperty`, so
  a typed alternative would not be more correct. Fails admission test 3.
- **`StateNode`'s `#nodeData: Map<unknown, unknown>`** — `getNodeData<T>(clazz)` is
  already type-safe at the boundary; tightening the private map changes nothing
  observable. Fails admission test 3.
