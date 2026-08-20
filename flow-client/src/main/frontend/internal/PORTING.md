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

1. **One Java class → one TypeScript module.** Do not collapse multiple Java
   classes into a single `.ts` file. This applies to production code **and to
   tests** (one Java `*Test` → one `*Tests.ts`). Example: `NodeFeatures` and
   `NodeProperties` are separate Java classes, so they are `NodeFeatures.ts`
   and `NodeProperties.ts`, not one shared file.
   - A closely-related nested type declared *inside* its owner in Java (e.g. a
     class's own change-event/listener) stays in the same module as its owner,
     matching the Java file it lives in.
2. **Mirror the Java package path** under `internal/` (e.g.
   `com.vaadin.client.flow.reactive` → `internal/reactive`).

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

6. **Carry Javadoc over verbatim**, including *member* Javadoc on constants —
   class descriptions, `@param`/`@returns`, `@deprecated`, `@see` as
   `{@link …}`. Preserve the source wording (including typos); do not silently
   reword. Convert `<p>` → blank lines and `<code>`/`{@code}` → backticks.
7. **Do not carry `@since` or `@author`.**
8. Document any deviation from the original at the site where it occurs.

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

13. Tests mirror the Java test cases 1:1 (same coverage, same assertions).
