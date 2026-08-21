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

# Java → TypeScript porting conventions

These conventions apply to the incremental port of the GWT client
(`com.vaadin.client.*`) and its shared server constants
(`com.vaadin.flow.internal.*`) into `flow-client/src/main/frontend/internal`.
They consolidate the review feedback from the migration PR stack (#24933,
#24947, …) so follow-up PRs stay consistent.

## Module layout

1. **One Java *source file* → one TypeScript module.** The split follows the
   Java *file* boundary, not the logical grouping: every top-level Java class,
   interface or `@FunctionalInterface` that lives in its own `.java` file
   becomes its own `.ts` module. Do not collapse several `.java` files into a
   single `.ts` file — this applies to production code **and to tests** (one
   Java `*Test` → one `*Tests.ts`; shared test helpers such as
   `CountingComputation` / `TestReactiveEventRouter` are their own helper
   modules too, and the test runner treats any non-`*Tests.ts` file under
   `src/test/frontend` as a helper rather than a suite). Examples:
   - `NodeFeatures` and `NodeProperties` are separate `.java` files, so they
     are `NodeFeatures.ts` and `NodeProperties.ts`.
   - The `com.vaadin.client.flow.reactive` package (`Reactive`, `Computation`,
     `ReactiveEventRouter`, `ReactiveValue`, `ReactiveValueChangeEvent`,
     `ReactiveValueChangeListener`, `InvalidateEvent`, `InvalidateListener`,
     `FlushListener`) is nine `.java` files, so it is nine `.ts` modules — not
     one `reactive.ts`.
   - A change-event / listener pair (e.g. `ListSpliceEvent` /
     `ListSpliceListener`, `MapPropertyChangeEvent` /
     `MapPropertyChangeListener`, `MapPropertyAddEvent` /
     `MapPropertyAddListener`) is a *separate* `.java` file from its owner
     feature in this codebase, so each gets its own module — it is **not**
     merged into the owner's module.
   - The **only** exception is a type that is genuinely nested *inside* its
     owner's `.java` file (a Java inner/nested class): it has no file of its
     own, so it stays in the owner's module. None of the currently ported
     reactive / node-feature event/listener types are nested this way.
2. **Mirror the Java package path verbatim** under `internal/`, stripping only
   the `com.vaadin.` prefix:

   ```
   com.vaadin.<rest>.ClassName  ->  internal/<rest with dots as slashes>/ClassName.ts
   ```

   | Java class | TypeScript module |
   | --- | --- |
   | `com.vaadin.client.BrowserInfo` | `internal/client/BrowserInfo.ts` |
   | `com.vaadin.client.flow.collection.JsArray` | `internal/client/flow/collection/JsArray.ts` |
   | `com.vaadin.client.flow.reactive.Reactive` | `internal/client/flow/reactive/Reactive.ts` |
   | `com.vaadin.client.flow.nodefeature.NodeMap` | `internal/client/flow/nodefeature/NodeMap.ts` |
   | `com.vaadin.flow.shared.BrowserDetails` | `internal/flow/shared/BrowserDetails.ts` |
   | `com.vaadin.flow.shared.util.SharedUtil` | `internal/flow/shared/util/SharedUtil.ts` |
   | `com.vaadin.flow.internal.nodefeature.NodeFeatures` | `internal/flow/internal/nodefeature/NodeFeatures.ts` |

   No package segment is collapsed or renamed: `client` and `flow` are real
   segments and stay in the path, and `com.vaadin.flow.internal` keeps its own
   `internal` segment inside the frontend `internal/` root.
   - A type with **no `com.vaadin` counterpart** has no package to mirror. One
     that comes from the GWT `elemental` library (e.g. `EventRemover`,
     mirroring `elemental.events.EventRemover`) and the local `assert` helper
     live at the `internal/` root.
   - A module that is a **TypeScript-only split** of a Java class (e.g.
     `ResourceRegistry` out of `ResourceLoader`, `MessageOrdering` out of
     `MessageHandler`) lives in the package of the class it was split from.
   - **Tests mirror this layout too.** A `*Tests.ts` (and its helper modules)
     lives under `src/test/frontend/` at the same path — `internal/` prefix
     included — as the module under test does under `src/main/frontend/`, e.g.
     `com.vaadin.client.flow.reactive.ComputationTest` →
     `src/test/frontend/internal/client/flow/reactive/ComputationTests.ts`, and
     `…flow.nodefeature.MapPropertyTest` →
     `src/test/frontend/internal/client/flow/nodefeature/MapPropertyTests.ts`. The test
     runner discovers suites recursively (`src/test/frontend/**/*Tests.ts`), and
     `eslint.config.mjs` lists each test subdirectory in
     `projectService.allowDefaultProject` (its globs do not support the `**`
     multi-level wildcard, so add one entry per level when introducing a new
     test subdirectory).

## Visibility parity

3. Java `public`/`protected` → `export` / class member; Java `private` →
   non-exported module-local function or a JS-native `#`-private class member
   (use `#`, not the TypeScript `private` keyword).

## Completeness & ordering

4. **Complete public API per module** — an absent member means "not in Java",
   never "forgotten". A deliberate subset (e.g. `NodeProperties` currently
   ports only the keys the ported client needs) must say so in the module
   Javadoc, and missing entries are added as later ports require them.
5. **Member order follows the Java declaration order**, including for constant
   registries.

## Javadoc / TSDoc

6. **Carry Javadoc over verbatim**, including *member* Javadoc on constants and
   *constructor* Javadoc — class descriptions, `@param`/`@returns`,
   `@deprecated`, `@see` as `{@link …}`. Every parameter documented in the Java
   source keeps its `@param` in the port; a constructor is not exempt. Preserve
   the source wording (including typos); do not silently reword. Convert `<p>` →
   blank lines and `<code>`/`{@code}` → backticks.
7. **Do not carry `@since` or `@author`.**
8. **Match the Java API — including constructor signatures.** Do **not** deviate
   from the Java parameter list; in particular, do not bundle several positional
   Java parameters into a single object parameter. A Java constructor such as
   `ListSpliceEvent(source, index, remove, add, clear)` ports to the same
   positional parameters `constructor(source, index, remove, add, clear)`, not a
   `constructor(source, details)` object, and each parameter keeps its own
   `@param` with the original wording. (This reverses an earlier, now-forbidden
   deviation that folded `ListSpliceEvent`'s four positional params into a
   `details` object.) Where a deviation is genuinely unavoidable because the
   language leaves no faithful equivalent — e.g. merging Java method overloads
   into one method with an optional parameter, as in `NodeList.splice` — keep it
   minimal and document it at the site where it occurs. Prefer matching the Java
   API over deviating. When a lint rule fires on a signature that faithfully
   mirrors the Java one — e.g. `@typescript-eslint/max-params` on a constructor
   with more than the allowed number of positional parameters — disable the rule
   at that line with an `eslint-disable-next-line` comment and a note that the
   parameters deliberately match the Java original, rather than reshaping the API
   to satisfy the linter.

## Cross-referencing constants (no magic values)

9. **Import and reference constants; never hard-code their values.** Constants
   that originate from `NodeFeatures` / `NodeProperties` (feature ids, property
   keys) must be imported and used by name — e.g. `NodeMap` uses
   `NodeFeatures.ELEMENT_PROPERTIES`, not a local `= 1`. This holds in tests
   too (mirror the Java test: hard-coded where the Java test hard-codes,
   `NodeFeatures.X` where the Java test uses the constant).
10. Cross-link intra-module references with a real `{@link …}` to the ported
    symbol (e.g. `{@link NodeProperties.VISIBLE}`).

## References to not-yet-ported symbols

11. When Javadoc references a Java identifier that is **not yet ported**, keep
    the reference as a code span (e.g. `` `ElementData` ``) rather than a
    dangling `{@link}`, and add a `TODO(flow-client-ts)` note in the module so
    it is restored to a real `{@link …}` (with an import) in the follow-up PR
    that ports the referenced class.
12. Where the port needs a slice of a not-yet-ported class, declare a minimal
    TypeScript `interface` contract (documented as a port deviation) that the
    future ported class will satisfy at cutover — see `MapPropertyTree` /
    `MapPropertyNode` / `MapPropertyOwner` in `MapProperty.ts`.

## Tests

13. **Tests mirror the Java test cases 1:1** — same set, same granularity, same
    assertions. This is the rule most often broken in this stack (missing
    cases, merged cases, under-asserting cases); the sub-rules below spell out
    what "1:1" means so the mismatches don't recur.
    1. **One Java `@Test` → at least one TypeScript `it()`.** Every `@Test`
       method in the Java counterpart is accounted for by an `it()`. This is a
       floor, not a ceiling: a module may carry *extra* cases beyond the Java
       set (see 13.6). Verify before opening the PR that no Java case is
       missing.
    2. **Never drop a case.** Port *every* `@Test`, including the intricate
       ones. A missing `it()` means "not in the Java test", never "skipped for
       brevity". (Regression this prevents: `MapPropertyTest.java`'s 9
       `syncToServer` / server-update-lifecycle cases were originally omitted,
       leaving the most complex ported logic untested.)
    3. **Never merge cases.** Do not fold several `@Test` methods into one
       `it()`, even when they share setup — one Java case that asserts three
       things about `hasPropertyValue`, or two that split
       invoked-during-flush vs removed-after-flush, stays as that many separate
       `it()` blocks. Merging loses the finer-grained failure attribution the
       Java suite provides (a regression in one branch would be masked by the
       other's assertions). If cases share setup, repeat it (or use a
       `beforeEach`/helper) rather than combining the assertions.
    4. **Do not split a ported case.** Do not expand one `@Test` into multiple
       `it()` blocks — that loses the mapping back to the Java case. Adding a
       genuinely new case is different, and is covered by 13.6.
    5. **Match assertion strength — do not under-assert.** Port *all* of a
       case's assertions, not just the easy one. If the Java test asserts four
       things (e.g. property not sent **and** a change event fired **and**
       `getNewValue()` is null **and** a registered flush listener ran), the
       `it()` asserts the same four. Dropping assertions silently narrows
       coverage of exactly the behavior the production port implements.
    6. **Extra cases are allowed where Java has no equivalent.** Several
       ported modules have thin or absent Java coverage — `GwtWidgetUtilTest`
       has a single case for the whole of `WidgetUtil`, and `BrowserInfo`,
       `Console`, `Profiler` and `StorageUtil` have no Java test at all.
       Holding those to a strict count would mean deleting real coverage, so
       additional `it()` blocks are welcome there. Group them under a
       `describe('beyond the Java suite')` (or mark them individually) so the
       ported-vs-added split stays obvious and 13.1 remains checkable. Rules
       13.2, 13.3 and 13.5 still apply in full to the cases that *are* ported.
    7. **Keep the Java case's name and order.** Name each `it()` after the Java
       method it ports (a readable rephrasing is fine — keep the mapping
       obvious, e.g. `setValue_updateFromServerIsApplied_syncToServerUpdatesValue`
       → `"setValue: update from server is applied, syncToServer updates
       value"`), and keep the `it()` blocks in the same order as the Java
       `@Test` methods.
    8. **Import shared test helpers — never re-declare them.** When a Java test
       reuses a helper from another test (e.g. `MapPropertyTest`, `NodeMapTest`
       and `NodeListTest` all `import
       com.vaadin.client.flow.reactive.CountingComputation`), the port imports
       the same helper module (`import { countingComputation } from
       '../reactive/CountingComputation'`) rather than copying an inline
       equivalent into each suite. A helper lives in one module — the one that
       mirrors its Java package (rule 1) — and every suite that needs it imports
       it from there, even across package directories. (Regression this
       prevents: `MapPropertyTests`, `NodeMapTests` and `NodeListTests` each
       carried their own `countingComputation` copy instead of the shared one.)
