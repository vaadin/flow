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

The cutover PR — the one that removes the Java client and, with it,
[`PORTING.md`](./PORTING.md) — owns this list and empties it.

An entry qualifies only if all three hold:

1. the current code is a **correct, faithful** port; if it is not, it is a review
   finding, not an improvement;
2. the better form **deviates from the Java shape**; if it does not, make the
   change now instead of filing it;
3. the benefit is **concrete** — a class of bug the compiler would catch, a cast
   removed, an API callers can no longer misuse, or code that disappears.

A convention belongs in [`PORTING.md`](./PORTING.md); ported code that a later rule
says is wrong belongs in that file's retrofit backlog; only the above belongs here.
[`PORTING-REVIEW.md`](./PORTING-REVIEW.md) §9 is the review step that fills this
file.

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
  rule 1 gives it its own module because Java gives it its own file.
- **What TypeScript could do**: nothing at all — every helper is a one-liner over a
  native array (`pushArray` → `push(...values)`, `clear` → `length = 0`,
  `isEmpty` → `length === 0`). Post-cutover the module can go and the call sites
  can inline the native operation.
- **Why it waits**: the module documents the Java→native mapping that reviewers
  check against, and rule 1 requires it while the Java file exists.
- **Spotted in**: #24933

### `ClientJsonCodec.decodeWithoutTypeInfo` / `encodeWithoutTypeInfo`

- **Site**: `client/flow/util/ClientJsonCodec.ts`
- **Java shape**: both are `public`, and both are no-ops on the compiled-JS path —
  the JVM-side conversions exist for tests only.
- **What TypeScript could do**: delete them and let the call sites use the value
  directly, once nothing has to line up with the Java public API.
- **Why it waits**: rule 4 requires the complete public API of the ported class.
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
