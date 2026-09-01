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

Reviewing a port against these rules has its own procedure — enumeration,
evidence and verification requirements — in
[`PORTING-REVIEW.md`](./PORTING-REVIEW.md). A review that does not follow it is
not a review of this document.

A rule added after the series started carries the branch it appeared at
(`_Introduced during #NNNNN._`). Ported code written before that branch is graded
against the rules that existed for it, and any retrofit still owed is listed in
the [retrofit backlog](#retrofit-backlog) at the end of this file.

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
   - A type that is genuinely nested *inside* its owner's `.java` file (a Java
     inner/nested class) has no file of its own, so it stays in the owner's
     module. None of the ported reactive / node-feature event/listener types are
     nested this way, so none of them is affected.
     - **Unless keeping it there would form a load-time cycle.** A nested type
       gets its own module when the owner cannot hold it:
       `DefaultConnectionStateHandler.Type` is `ConnectionMessageType.ts`,
       because `ReconnectStateMachine` compares these priorities at runtime while
       `DefaultConnectionStateHandler` constructs the state machine, so an enum in
       the owner would make the two modules circular at load time. Name the cycle
       at the site, port the nested type's members in full — `isMessage`,
       `isHigherPriorityThan` and the ordinal priorities are all there — and give
       the module its own suite, as rule 1's mapping requires. This is the
       module-layout twin of rule 12's `import type` escape for the same problem.
       _Introduced during #24951._ (False positive this prevents: a review
       reporting a rule-1 violation against a split whose alternative — a runtime
       cycle — is not available.)
   - **Suites may be organised by feature area.** The one-`*Test`-to-one-`*Tests.ts`
     mapping above assumes a module's Java coverage lives in a single test class.
     Where it is spread across several, the port may group its suites by what is
     being exercised instead, provided **each suite names the Java classes it draws
     from** in its header, so rule 13.9's counterpart question stays answerable.
     `SimpleElementBindingStrategy` is the case in point: its attribute, children,
     property, styling, visibility, shadow-root, virtual-children, event-listener,
     ready-callback and model-handler suites draw between them from
     `GwtBasicElementBinderTest`, `GwtMultipleBindingTest`, `GwtPolymerModelTest`
     and `GwtPropertyElementBinderTest`, none of which is organised by feature.
     Rule 13's case-level rules apply unchanged to the union of those classes.
     _Introduced during #24949._ (False positive this prevents: a review reporting
     ten rule-1 violations against a layout that is deliberate, and that the
     alternative — six suites each mixing unrelated features because Java groups by
     binder class — would make worse.)
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
     a new subdirectory needs no lint configuration: the eslint project service
     walks up from each file to `src/test/frontend/tsconfig.json`, and
     `allowDefaultProject` in `eslint.config.mjs` now lists only the root-level
     suites that predate the port. _(This bullet used to say every test
     subdirectory had to be listed there; that stopped being true when the test
     tree got its own `tsconfig.json`.)_ This mapping fixes where the suite **lives**; which Java
     test classes it must cover is rule 13.9, and there is often more than one.

## Visibility parity

3. Java `public`/`protected` → `export` / class member; Java `private` →
   non-exported module-local function or a JS-native `#`-private class member
   (use `#`, not the TypeScript `private` keyword).
   - **Do not widen visibility for tests.** Never add an `export` (or promote a
     `#`-private member to public) just so a test can reach a helper that is
     `private` in Java — that breaks visibility parity. Test a private helper
     through the existing public surface that already exercises it, mirroring how
     the Java `*Test` covers it. A Java method that is *itself* public "for
     testing purposes" (e.g. `TreeChangeProcessor.processChange`) is genuine
     public API and is exported as such — that is not a test-only export.
     (Regression this prevents: `ClientJsonCodec`'s `applyCaptures` and
     `createReturnChannelCallback`, both `private static native` in Java, were
     exported only so the test could call them directly; they are covered instead
     through the public `decodeWithTypeInfo` `@v-fn` / `@v-return` paths.)

## Completeness & ordering

4. **Complete public API per module** — an absent member means "not in Java",
   never "forgotten". A deliberate subset (e.g. `NodeProperties` currently
   ports only the keys the ported client needs) must say so in the module
   Javadoc, and missing entries are added as later ports require them.
5. **Member order follows the Java declaration order**, including for constant
   registries. In a module of exported functions this covers the **export order**
   too: the sequence of `export function` declarations follows the order of the
   Java methods they port. (Regression this prevents: `TreeChangeProcessor.ts`
   exported `processChange` before `processChanges` while
   `TreeChangeProcessor.java` declares `processChanges` first, and a review that
   checked class members marked the module `pass` on every rule.)

## Javadoc / TSDoc

6. **Carry Javadoc over verbatim**, including *member* Javadoc on constants and
   *constructor* Javadoc — class descriptions, `@param`/`@returns`,
   `@deprecated`, `@see` as `{@link …}`. Every parameter documented in the Java
   source keeps its `@param` in the port; a constructor is not exempt. Preserve
   the source wording (including typos); do not silently reword. Convert `<p>` →
   blank lines and `<code>`/`{@code}` → backticks.
   - Tags belong to the members that are **ported**. An unported private Java
     helper's `@param`/`@return` are out of scope — `TreeChangeProcessor`'s
     `jsonArrayAsJsArray` has no TypeScript counterpart, so its tags are not
     missing — and a parameter that exists only in the port gets its own `@param`.
     A tag count that differs from Java for one of those two reasons is not a
     finding.
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
    symbol (e.g. `{@link NodeProperties.VISIBLE}`). A cross-*module* reference
    needs the symbol in scope for the link to resolve, so add a doc-only
    `import type { X } from '…'` (or `import type * as X` for a module of
    functions) alongside it. Such an import does **not** trip `noUnusedLocals`:
    TypeScript counts a `{@link}` reference as a use, so the link and the import
    keep each other alive. Do not settle for a plain code span — that was the
    earlier workaround and it loses the link. The cross-module half of this rule
    was _introduced during #24949_; a code span standing in for a cross-module
    link in an earlier branch is `n/a` per `PORTING-REVIEW.md` §8.6, not a
    finding. (A code span for a symbol that is not ported at all stays correct
    under rule 11, whichever branch it is in.)

## References to symbols the port does not have

11. When Javadoc references a Java identifier that is **not yet ported**, keep
    the reference as a code span (e.g. `` `ErrorMessage` ``) rather than a
    dangling `{@link}`, and add a `TODO(flow-client-ts)` note in the module so
    it is restored to a real `{@link …}` (with an import) in the follow-up PR
    that ports the referenced class.
    - **A class outside the port's scope never gets that note.** This migration
      covers the GWT client (`com.vaadin.client.*`) and the shared constants it
      reads, so a reference into `flow-server` — `com.vaadin.flow.internal.JacksonCodec`,
      `com.vaadin.flow.dom.DebouncePhase`, the node-feature classes such as
      `ElementData` — will never resolve to a ported symbol. Its code span is
      **permanent**: state that at the site, naming the class, and add no
      `TODO(flow-client-ts)` and no retrofit-backlog row, because there is no
      follow-up PR to wait for. Server-side constant *values* the port has to
      duplicate rather than import (`ApplicationConstants`' `v-r` and `uiId` in
      `SystemErrorHandler.ts`) are the same case: group them under a comment
      naming the Java class. _Introduced during #24950._ (The distinction matters
      because a `TODO` is a promise: four sites carried one for classes nobody
      will ever port, so each review re-read them as open work.)
12. Where the port needs a slice of a not-yet-ported class, declare a minimal
    TypeScript `interface` contract (documented as a port deviation) that the
    future ported class will satisfy at cutover — as `StateTree.ts` did for
    `com.vaadin.client.Registry` and its server-communication layer until #24952
    ported them. A slice is only for a class that is **not yet ported**, never a
    permanent decoupling from one that is: once the referenced class lands,
    replace the slice with a real `import` of the ported type and delete the
    interface. (E.g. `StateNode` and `ClientJsonCodec` import the real
    `StateTree` rather than re-declaring a `getNode` / `getRegistry` slice; and
    once `StateTree`/`StateNode`/`NodeMap` landed in #24948, `MapProperty` and
    `NodeFeature` dropped their `MapPropertyTree` / `MapPropertyNode` /
    `MapPropertyOwner` / `NodeFeatureNode` slices for the real types. Where the
    real class would create a runtime import cycle — `NodeFeature` is the base
    class of `NodeList`/`NodeMap`, so a *value* import of `StateNode` for an
    `instanceof` would form a circular `extends` — import the type with
    `import type` and keep a structural runtime check, documented at the site.
    Where a real consumer needs a member the slice omitted — such as
    `ServerConnector.sendReturnChannelMessage` — extend the still-unported slice
    to cover it.) If the PR that ports the class cannot collapse a slice in the
    same change, it files a retrofit-backlog row naming the slice: a slice never
    outlives its port silently.
    - **A service takes the registry, not a slice of it.** `Registry` declares the
      24 typed getters `Registry.java` declares, so a module's constructor takes
      `Registry` and no module re-declares the getters it happens to call. There
      is no registry slice left to keep in step with anything.
      _First stated during #24951, when the container was still local and each
      getter's return type was narrowed to `Pick<PortedClass, 'membersUsed'>`; that
      sweep found six places where a slice and its class had drifted apart (five
      slices looser than the real signature, and
      `sendExistingElementWithIdAttachToServer` declaring a non-null `id` the
      binding layer already calls it with as null) plus one member,
      `handlePropertyUpdate`, that `StateTree`'s slice omitted although the tree
      calls it. #24952 then ported `DefaultRegistry` and dropped all 23 local
      contracts for the real class, which also removed the last four `as never`
      casts the loose slices had needed._
    - **A suite builds a real registry.** Because `Registry.set` is protected, as
      in Java, a suite registers its services through the `TestRegistry` subclass
      in `src/test/frontend/internal/client/testRegistry.ts` — `testRegistry({ StateTree: tree })`
      — rather than casting an object literal into the registry type. Services a
      suite does not register stay unregistered, so a lookup the code under test
      should not make throws instead of silently returning a stub.

## Tests

13. **Tests mirror the Java test cases 1:1** — same set, same granularity, same
    assertions. This is the rule most often broken in this stack (missing
    cases, merged cases, under-asserting cases); the sub-rules below spell out
    what "1:1" means so the mismatches don't recur.
    1. **One Java `@Test` → at least one TypeScript `it()`.** Every `@Test`
       method in **every** Java counterpart (13.9) is accounted for by an
       `it()`. This is a floor, not a ceiling: a module may carry *extra* cases
       beyond the Java set (see 13.6). Verify before opening the PR that no Java
       case is missing.
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
       `@Test` methods. Where suites are organised by feature area (rule 1), the
       order holds *within* each suite: the cases it ports keep their relative
       order from the Java class they came from.
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
    9. **A module can have more than one Java counterpart — find them all.**
       Rule 2's path mirror says where a `*Tests.ts` *lives*; it does not say
       where its Java counterpart lives. Before counting cases, inventory every
       Java test class that exercises the ported class, across all source roots
       and naming variants:
       - `flow-client/src/test/java/**/XTest.java` — the JRE-side unit test;
       - `flow-client/src/test-gwt/java/**/GwtXTest.java` — the GWT/browser test
         (`GwtJsArrayTest` for `JsArray`, `GwtStateTreeTest` for `StateTree`);
       - `flow-client/src/test/java/**/JreXTest.java` — the JRE-fallback test
         (`JreArrayTest` for `JsArray`);
       - `flow-server/src/test/java/**/XTest(s).java` — for a class ported from
         `com.vaadin.flow.*` (`BrowserDetailsTest`, `SharedUtilTests`).

       Rules 13.1–13.8 then apply to the **union** of their `@Test` methods, and
       a suite that declares itself to have no Java counterpart must have checked
       all four locations first. List the counterparts you found in the review, so
       the next round does not have to re-derive the set. _Introduced during
       #24948._ (Regression this prevents: eight `@Test` methods across
       `GwtStateTreeTest`, `GwtStateNodeTest`, `GwtTreeChangeProcessorTest` and
       `GwtClientJsonCodecTest` had no verdict in any grid, and
       `ClientJsonCodecTests` stated that `ClientJsonCodec` "has no `*Test.java`
       counterpart" while `GwtClientJsonCodecTest` existed with two cases.)

## Language mapping

14. **Settled mappings — cite these, do not re-derive or reopen them.** Each was
    decided once for the whole series; a review may point at the rule number, and
    may reopen one only with new evidence. _Introduced during #24948._
    1. **Value comparison uses strict `===`.** Java `Objects.equals` becomes
       `===`, accepting that `undefined` and `null` stay distinct where GWT's
       compiled `==` treated them alike. This is deliberate: the stricter
       behaviour is preferred, and the consequences are handled where they
       surface.
    2. **`Optional<T>` becomes `T | undefined`**, with `Optional.ofNullable`
       mapping to a `null` → `undefined` normalisation at the setter (see
       `MapProperty.setPreviousDomValue`).
    3. **A Java `Class<T>` token becomes the JS constructor function** — see the
       `nodeData` map in `StateNode`.
    4. **Java `assert` becomes the always-on `assert()` helper**, including the
       structural and consistency assertions, not only the null checks. Drop only
       an assertion that TypeScript's non-null types make unreachable, and say so
       at the site. (Regression this prevents: eight structural asserts were
       dropped from `StateTree` / `TreeChangeProcessor` while the
       null-precondition ones were correctly dropped, so the omission read as
       deliberate.) One further exception, and it needs the same note at the site:
       where the asserted invariant is one the running client can legitimately
       break — and Java, whose assertions production strips, therefore continues
       and recovers — the check stays as a `Console.warn` rather than a throw.
       `MessageHandler.processMessage` is the case in point: it runs deferred
       until the message's eager dependencies load, and `forceMessageHandling`
       can clear the response locks and handle a newer message meanwhile, moving
       the last seen server id past the one being processed. _Introduced during
       #24951._
    5. **A GWT-compiler-only construct has no port** — `crazyJsCast`,
       `crazyJsoCast`, deferred binding — and its absence is documented at the
       site that would have called it.
    6. **Non-null access mirrors Java's — use `!`, not `?.`.** Where Java
       dereferences a reference its own types treat as non-null — a plain
       `x.foo()` with no guarding `assert` that would throw
       `NullPointerException` if `x` were null — the port mirrors it with a
       non-null assertion (`x!.foo()`), **not** optional chaining (`x?.foo()`).
       Strict parity is preferred: optional chaining silently yields `undefined`
       exactly on the input Java rejects, quietly diverging behaviour instead of
       failing as Java does. (Regression this prevents:
       `ClientJsonCodec.decodeWithTypeInfo`'s `@v-node` branch used
       `tree.getNode(id)?.getDomNode()`, returning `undefined` for a missing node
       where Java's `tree.getNode(id).getDomNode()` throws.) The opposite case —
       normalising a JS `undefined` to `null` for a *value* Java also treats as
       nullable, e.g. `map.get(...) ?? null` mirroring a Java `Map.get` that
       returns `null` — is faithful and stays, because it restores Java's
       contract rather than deviating from it. Where Java *does* guard the access
       with an `assert`, port the assert (rule 14.4) rather than a bare `!`. If
       `eslint-config-vaadin`'s `@typescript-eslint/no-non-null-assertion` fires
       on the `!`, disable it at that line with a note (as rule 8 does for
       `max-params`) rather than reshaping the access into a silent `?.`.
    7. **Side effects and identity are part of the port.** Mirror what the Java
       method mutates and what it returns, not just the shape of the result: a
       helper that mutates its argument in place and returns that same instance is
       ported the same way, not as build-a-copy-and-return. (Deviation this names:
       `ClientJsonCodec.decodeObjectWithTypeInfo` builds a new object, where
       `ClientJsonCodec.java:306` writes the decoded values back into the incoming
       `JsonObject` and returns it — observationally different for a caller that
       holds a reference to the input.)

## Comments in ported code

15. **Do not cite these porting conventions from ported code.** A comment in a
    `.ts` module — production or test — explains what the code does and why on its
    own terms; it must **not** reference `PORTING.md` or a rule number (`rule 14.6`,
    `13.9`, `PORTING.md 13.6`, …). These conventions exist to govern the review of
    the port while the migration series is in progress; once the port is finished
    this document goes away, and a comment that points at "rule 13.6" becomes a
    dangling reference to a file the reader no longer has. Keep the *substance* —
    the reason a deviation exists, that a case has no Java equivalent, why a `!` is
    used instead of `?.` — as a self-contained explanation, and drop the citation.
    (A review still cites rule numbers; that lives in the PR discussion and the
    review grid, not in the source tree.) _Introduced during #24948._ (Regression
    this prevents: production and test modules across the stack carried
    `// See PORTING.md rule 14.6` and `// beyond the Java suite (PORTING.md 13.6)`
    comments that would outlive the review process.)
16. **Record TypeScript improvements; do not make them.** Where the port is
    faithful but TypeScript could express the same thing better — a type guard
    instead of an unchecked type parameter, a literal union instead of `number`, a
    generic instead of an overload set — leave the code as it is and file the idea
    in [`PORTING-IMPROVEMENTS.md`](./PORTING-IMPROVEMENTS.md) against that file's
    admission test. The port stays literal until the migration is complete, because
    a reviewer's only leverage is reading a module against its Java original; the
    cutover PR owns that list and empties it. A review grades such a site `pass` —
    the port *is* faithful — and files the entry instead of reporting a finding;
    `PORTING-REVIEW.md` §9 is the step that looks for them. _Introduced during
    #24949._ (Regression this prevents: `BinderContext.getStrategies` invited a
    mid-series signature change that would have diverged from `BinderContext.java`
    while both trees were still being reviewed against each other.)
17. **Carry the implementation comments, not only the Javadoc.** The in-body
    `//` comments of a Java method are part of what is ported: they record why a
    branch exists, what the server does that forces an ordering, which bug a line
    works around, why a wait is where it is. Carry each one to the statement it
    annotates, at the same point in the method and in the source wording — a port
    that keeps every Javadoc block but drops the body comments loses precisely the
    knowledge that cannot be re-derived from the code. Rewrapping to the
    TypeScript print width is fine, and two Java comments may merge into one
    sentence where the port merges the statements they annotate; dropping the
    substance is not.
    - **The exceptions are the ones the code itself has.** A comment explaining a
      construct the port does not have is not missing: `// prevent direct
      instantiation` / `// Only static stuff in this class` above a private
      utility constructor (a module of functions has none), `// JSO Constructor`,
      `// $entry not needed as function is not exported`, `// Crazy cast since
      otherwise SDM fails`, a comment on an unported private helper. Where the
      port *replaces* the mechanic rather than dropping it, the comment is
      replaced too — say what the TypeScript does instead, at that site.
    - **A carried Java `TODO` stays a plain `TODO`,** with its original text and
      ticket link — `DefaultConnectionStateHandler.pushClientTimeout` keeps
      `// TODO Reconnect, allowing client timeout to be set` and its
      `dev.vaadin.com/ticket/18429` link. It is the Java author's open item, not
      porting debt, so it takes neither the rule-11 `TODO(flow-client-ts)` marker
      nor a retrofit-backlog row.
    - *Verification:* compare the comment-line counts per module
      (`grep -c '^[ \t]\+//'` over the `.java` and its `.ts`) and read the Java
      comments against the port wherever the port has fewer. The count only
      points at modules to read: a port that condenses three Java lines into one
      sentence is complete at a lower count, and one that adds port-specific
      notes can sit at a higher count while still having dropped something.
      Indentation-scoped counts mislead badly — measuring only 8-space Java
      against 4-space TypeScript comments read `SimpleElementBindingStrategy` as
      short by 49 lines when it is short by 2, and reported no gap at all for
      `JsoConfiguration`, whose port carries none of its original three.
    _Introduced during #24951._ (Regression this prevents:
    `DefaultConnectionStateHandler` carried 12 of its original's 50 comment lines,
    losing among others why `scheduleReconnect` fires the first retry outside the
    timer, why 4xx status codes are treated as possibly temporary, and why a
    bidirectional transport pushes pending changes immediately on reconnect; the
    sweep it prompted found comments missing in eight of this PR's modules and
    eleven more in the layers below.)

## Retrofit backlog

Rules added mid-series that earlier ported code does not satisfy yet — **parity
debts**, whose fixes make the port *more* literal. An improvement that would
deviate from the Java shape is not a row here; it goes to
[`PORTING-IMPROVEMENTS.md`](./PORTING-IMPROVEMENTS.md) (rule 16). A row is
removed when the retrofit lands; see [`PORTING-REVIEW.md`](./PORTING-REVIEW.md)
§8 for how rows get here and where a retrofit is allowed to land.

| Rule | Affected modules | Retrofit lands in | Status |
| --- | --- | --- | --- |
| 13.1 | `ExecuteJavaScriptProcessorTests` has no `it()` for the five `execute_*` and seven `isBound_*` cases of `ExecuteJavaScriptProcessorTest`. `invoke`/`isBound` are `protected` again, so the Java approach - a subclass that overrides them - now ports directly; what remains is building the state nodes each case needs | a follow-up on the support-services layer | open |
| 17 | Eleven base-layer modules measure short of their Java original's comment lines: `Console` (2/7), `SharedUtil` (10/13), `TreeChangeProcessor` (4/6), `JsArray` (0/2), `BrowserDetails` (34/36), `ExistingElementMap` (1/2), `MapProperty` (14/15), `ClientJsonCodec` (9/10), `SimpleElementBindingStrategy` (107/109), `ServerEventHandlerBinder` (0/1), `NodeFeatures` (0/1). Each needs the read-through the count only points at, since part of the gap is the GWT mechanics the rule exempts (`Console`'s `$entry` deferral, `JsArray`'s private constructor) and part is genuinely dropped (`ClientJsonCodec` has none of the five `// Check for @v-…` branch markers (`@v-node` twice, `@v-return`, `@v-fn`, and the unknown-`@v-` fallback); `MapProperty` carries one of the two `// mark as server update is in progress` sites) | a follow-up on the state-tree and support-services layers | open |

The rule-12 row for `Registry`'s typed getters was closed in #24952: the getters
moved from `DefaultRegistry` to `Registry`, where `Registry.java` has them, all
23 local registry contracts were deleted, and the suites that used to cast an
object literal into a registry now build a real one through `TestRegistry`.
`getApplicationConnection` is registered by `DefaultRegistry.setApplicationConnection`,
since the port assembles the registry before the connection that Java passes into
its constructor exists.

The virtual-child rows are blocked rather than overlooked: both cases assert
that `InitialPropertiesHandler` reverts a deferred element's properties on
flush, and that class is not ported yet, so the assertion has nothing to bind
to. The suite records the deferral at its head.

The rule-12 slices that stood in for `StateNode` / `StateTree` / `NodeMap`
(`MapPropertyTree` / `MapPropertyNode` / `MapPropertyOwner` in
`MapProperty.ts`, `NodeFeatureNode` in `NodeFeature.ts`) were collapsed to the
real types in #24948, which also rewrote the base-layer `MapPropertyTests` /
`NodeMapTests` / `NodeListTests` mocks into real instances. The
`serverEventObjectAccess` slice in `StateTree` was collapsed in #24949 when
`ServerEventObject` landed.
