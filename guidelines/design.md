# Design

Guidance for the shape of new public Java API in Vaadin Flow. Consult this
before starting any non-trivial new API — especially anything that wraps a
browser or JavaScript API, exposes observable state, or manages a resource with
a lifecycle.

This chapter covers the Java surface only. The rest of the material for a
browser-API wrapper lives in sibling chapters:

- [Browser Integration](browser-integration.md) — where the client-side code
  lives, `executeJs`, DOM event naming, bootstrap data, capability detection.
- [Documenting](documenting.md) — what the Javadoc of a wrapper has to explain.
- [Testing](testing.md) — mocking the browser API in an integration test.

## Before you start

- **Find the precedent.** Flow already has patterns for per-UI facades,
  reactive signals, sealed result hierarchies, Jackson wire records, and
  browser-API wrappers. Look at `Page`, `History`, `ExtendedClientDetails`,
  `VaadinSession.localeSignal`, `window.Vaadin.Flow.*` helpers, etc. — match
  the existing shape rather than inventing a new one.
- **Understand the blast radius.** See [Repository](repository.md) — changes to
  `StateNode`, `Element` or the codec layer ripple across the codebase.
- **Frontend ↔ server is a contract.** Any protocol or DOM-event change needs
  both sides updated in the same PR.

## API shape

### Per-UI facades

If a feature is UI-scoped (not session-scoped), expose it via
`UI.getXxx()` returning a single instance created once per UI. The facade
holds `private final UI ui`, and all `executeJs` calls go through
`ui.getElement()` or `ui.getPage()`. Follow the `Page` / `History`
pattern. Enforce single-instance creation in the constructor if needed.

If a feature hands out a stateful handle (e.g. `Geolocation.watchPosition()`
returning a `GeolocationWatcher`), make the handle's constructor
**package-private** so application code cannot bypass the entry point.

### Keep internal mutators off user-facing classes

When the framework needs to write a value that the application is meant
to read, don't hang the setter on the class users already use for reads
— even if it's annotated "for framework use only". Put the read surface
on the user-facing class or facade and the write surface on
`UIInternals` (or equivalent internal-only class). The opposite shape
is a DX hazard: for example, `setGeolocationAvailability` lives on
`UIInternals`, not on the user-facing `ExtendedClientDetails`.

### Options records

For tunable knobs (accuracy, timeout, cache age), prefer an immutable
Java record over a long parameter list:

- Compact constructor with validation (reject negative durations, etc.).
  NullAway reads `@Nullable` off the record components, so there is no
  need to spell out the full canonical constructor.
- Builder for ergonomics. Offer both `Duration` and `int`-ms overloads
  on time-related setters where the wire format is ms; applications get
  a fluent `Duration` API and the record stores the int.
- Record stays serialisable — implement `Serializable` on the Builder
  too if it's meant to be held in session state.

### Signals for reactive state

Expose observable state as a `Signal<T>` rather than a listener API. Name
accessors with a `Signal` suffix — `localeSignal()`, `availabilitySignal()`,
`valueSignal()`, `activeSignal()` — so callers see the shape at the call
site. Other precedents: `windowSizeSignal`, `validationStatusSignal`.

- Back the signal with a private `ValueSignal<T>`; expose it as read-only
  via `valueSignal.asReadonly()`.
- Cache the read-only wrapper in a field so every call returns the same
  instance. `asReadonly()` allocates a fresh lambda per call — identity is
  unstable and allocations add up.
- Seed the signal with a meaningful default, not `null`. If the initial
  state is "no data yet", use a sentinel enum value (e.g. `UNKNOWN`) or a
  dedicated record (e.g. `Pending`) that callers can handle in a normal
  pattern match without a `case null` arm.
- Inside a reactive context, callers use `.get()` (subscribes); outside,
  they use `.peek()` (snapshot). Document this in the accessor's Javadoc.

### Interfaces vs abstract classes

When introducing a new type that callers will receive or pass around
(e.g. `Trigger`, `Action`, `Argument`), pick *one* of: a single concrete /
abstract class named `Xyz`, or both an interface `Xyz` and an abstract
class `AbstractXyz`. Don't introduce one half speculatively.

- **Default to a single abstract (or concrete) class named `Xyz`.** Skip
  the interface unless you can demonstrate, today, a useful implementation
  that does *not* extend the abstract class. Introducing the interface
  later does not retroactively help: every existing method signature that
  accepts `AbstractXyz` will still reject interface-only implementations,
  so the migration is just as expensive as not having had an interface at
  all.
- **Only add an interface alongside an abstract class if you can ship a
  worked example of using the interface without the abstract class.** The
  interface is a commitment that it stands on its own today and keeps
  standing — not an option you keep open "in case". An interface that
  only ever has one valid implementation adds noise without value.
- **Name it `Xyz`, not `AbstractXyz`, unless an interface exists or is
  explicitly planned.** The class name appears in every method signature
  that accepts an instance (`doSomething(Trigger trigger)` reads better
  than `doSomething(AbstractTrigger trigger)`), and renaming later
  doesn't unblock interface adoption — see above.
- **Prefer an interface with default methods over an abstract class when
  there is no state.** Abstract classes earn their keep by holding
  fields. If today and foreseeably the type has none, an interface is
  the lighter choice.
- **Don't reach for multiple inheritance.** The usual motivation for
  splitting interface + abstract class is "what if someone needs to
  combine this with another base class?". In practice almost every such
  case is better served by a separate "view" class that exposes the same
  underlying state, or by composition. Design the view, then decide if
  the interface is actually needed.
- **Don't make a sealed interface that permits a single abstract class.**
  That combination has no callers it serves: it can't be implemented
  outside the package (sealed), it can't be subclassed except via the one
  permitted class, and it adds a type with no behaviour. Either keep just
  the abstract class, or open the interface up to genuinely independent
  implementations.

### Result types: sealed interfaces + pattern matching

For values that are "one of N things" (position-or-error,
pending-or-position-or-error), use a sealed hierarchy:

```java
public sealed interface Foo permits FooA, FooB, FooC {}
```

- Record subtypes are preferred over classes.
- If the same values appear in two contexts with different permitted sets,
  split into two sealed interfaces and let the narrower one `extends` and
  `permits` a subset (see `GeolocationResult` vs `GeolocationOutcome`).
  This lets a one-shot callback omit the "pending" arm that would be dead
  there.
- Exhaustive `switch` expressions over the sealed set are guaranteed
  complete at compile time; design for that, don't add `default:` arms.

### Nullability discipline

- Apply `@NullMarked` at the package level (JSpecify). Only `@Nullable`
  what genuinely may be null.
- Prefer sentinel values over nullable returns in the public API
  (`UNKNOWN` enum constant, `Pending` record).
- Jackson wire records (the record used to decode `executeJs` return
  values or DOM event payloads) are the legitimate exception — their
  fields may be `@Nullable` because the wire format permits omissions.
  Keep the wire record private and translate to a non-null public shape
  at the boundary.
- `@Nullable` belongs on the declared type — `ValueSignal<@Nullable X>` —
  and NullAway infers it for the constructor, so `new ValueSignal<>(null)`
  and `Signal.cached(...)` need no repeated type argument or type witness.

### Lifecycle and cleanup

Resources that outlive a single request — watches, DOM listeners, timers,
client-side subscriptions — must be tied to a component's lifecycle:

- Accept a `Component owner` at construction.
- Register a `DetachListener` that performs the cleanup.
- Also expose an explicit `stop()` (or similar) for mid-view cancellation.
- Make `stop()` idempotent — guard with an `active` flag or signal so
  detach-after-stop and double-stop are safe no-ops.
- If the API supports resuming, reset observable state on resume so
  subscribers re-render with the correct initial value.
- DOM listeners that must keep flowing while the UI is inert (e.g. a
  modal is open over the view but position updates should still
  accumulate) are registered with
  `addEventListener(...).addEventDetail().allowInert()`. Use sparingly
  — inert exists to prevent user actions while something else has
  focus; only bypass it for passive streams.

### Naming components that wrap HTML elements

A component class name in `flow-html-components` is public API: changing it
later costs a deprecation cycle and a find-and-replace in every application.
Decide it from the rules below rather than case by case.

- **Default to the element's own name.** `<div>` → `Div`, `<section>` →
  `Section`, `<table>` → the table component. Tags of one or two letters do
  not read as words, so spell those out: `<p>` → `Paragraph`, `<a>` →
  `Anchor`, `<em>` → `Emphasis`, `<img>` → `Image`. Longer shorthand tags
  keep the name they already have in the package — `Div`, `Nav`, `Pre`,
  `Abbr`, `Hr`, `Param` — expand one only if the short form would be
  meaningless on its own.
  Drop the `HTML` prefix and `Element` suffix that the DOM interface names
  carry — the class for `<tr>` is named after "table row", not after
  `HTMLTableRowElement`.

- **Add a prefix only for one of two reasons.**
  - *The name is taken.* A Vaadin component owns the plain name and both
    classes have to be usable in the same file — `Button` and
    `NativeButton`, `Details` and `NativeDetails`. A clash with a type that
    is imported everywhere anyway counts too: `<object>` cannot be `Object`,
    hence `HtmlObject`.
  - *The plain name invites a mistake that goes unnoticed.* `<label>` reads
    like the "short piece of text" widget of Swing and of Vaadin 8. Picking
    `Label` for that purpose compiles, looks right, and silently produces
    markup that misleads screen readers, so the class is `NativeLabel` and
    the plain name is deliberately hard to find.

  The second reason is narrow on purpose. It is about mistakes that stay
  invisible, not about a name a user might reach for and then immediately
  reject: someone who tries `Table` expecting Vaadin 8's data-bound table
  finds no items, no columns and no data provider, and goes looking for
  `Grid` within a minute. A confusable name that fails loudly is a Javadoc
  problem, not a naming problem.

- **The prefix is `Native`.** `HtmlObject` predates the convention and
  keeps its name; everything prefixed since uses `Native`, and one uniform
  prefix is worth more than a second, better-reasoned one alongside it. Do
  not introduce further `Html…` names.

- **Prefix the class that has the problem, not the whole family.** `<td>`
  and `<tr>` collide with nothing and mislead nobody; `NativeTableCell` and
  `NativeTableRow` are prefixed only because they arrived together with
  `NativeTable`. A new family of related elements should prefix the members
  that meet a criterion above and leave the rest alone.

- **These are not reasons to prefix:** the element is low-level; the plain
  name might be wanted for a component we may build one day (do not reserve
  names for components that do not exist); the sibling classes are
  prefixed; the prefix makes the class look more advanced. Every prefix
  costs discoverability for the users who do want the element — pay it only
  when one of the two reasons above applies.

- **Renaming an existing component is a breaking change.** Do not rename
  just to close the gap with this guidance. When a rename is worth it, add
  the new class, deprecate the old one with a `@deprecated` pointer
  explaining what to use instead, and remove it in the next major — the way
  `Label` was replaced by `NativeLabel` in 24.1 and removed in 25.

