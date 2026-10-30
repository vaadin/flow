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

The rules for new public API are in the Public API, Naming, Nullability and
`toString()` sections of [`CONVENTIONS.md`](../CONVENTIONS.md). This chapter
explains them and covers the design decisions they leave open.

## Before you start

- **Find the precedent.** The shapes to match are in `Page`, `History`,
  `ExtendedClientDetails`, `VaadinSession.localeSignal` and the
  `window.Vaadin.Flow.*` helpers.
- **Understand the blast radius.** See [Repository](repository.md) — changes to
  `StateNode`, `Element` or the codec layer ripple across the codebase.

## API shape

### Per-UI facades

If a feature is UI-scoped (not session-scoped), expose it via
`UI.getXxx()` returning a single instance created once per UI. The facade
holds `private final UI ui`, and all `executeJs` calls go through
`ui.getElement()` or `ui.getPage()`. Follow the `Page` / `History`
pattern. Enforce single-instance creation in the constructor if needed.

A stateful handle the facade hands out (e.g. `Geolocation.watchPosition()`
returning a `GeolocationWatcher`) is only valid when created through that
entry point, which is why its constructor stays package-private.

### Keep internal mutators off user-facing classes

A setter on the class users already read from shows up in their IDE
completion, and a "for framework use only" note does not stop anyone from
calling it. That is why the write surface goes on `UIInternals` (or an
equivalent internal-only class): `setGeolocationAvailability` lives on
`UIInternals`, not on the user-facing `ExtendedClientDetails`.

### Options records

How to build the options record for tunable knobs (accuracy, timeout, cache
age):

- NullAway reads `@Nullable` off the record components, so the compact
  constructor that validates (rejecting negative durations, etc.) is enough;
  there is no need to spell out the full canonical constructor.
- Offer both `Duration` and `int`-ms overloads on time-related builder
  setters where the wire format is ms; applications get a fluent `Duration`
  API and the record stores the int.
- The record stays serialisable — implement `Serializable` on the Builder
  too if it's meant to be held in session state.

### Signals for reactive state

The `Signal` suffix lets callers see the shape at the call site. Precedents:
`localeSignal()`, `availabilitySignal()`, `valueSignal()`, `activeSignal()`,
`windowSizeSignal`, `validationStatusSignal`.

- Back the signal with a private `ValueSignal<T>` and expose it read-only via
  `valueSignal.asReadonly()`. Since `asReadonly()` allocates a fresh lambda
  per call, an uncached wrapper would also add up in allocations.
- A sentinel default such as `UNKNOWN` or `Pending` lets callers handle the
  "no data yet" state in a normal pattern match, without a `case null` arm.
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

A sealed hierarchy with record subtypes models values that are "one of N
things" (position-or-error, pending-or-position-or-error):

```java
public sealed interface Foo permits FooA, FooB, FooC {}
```

- If the same values appear in two contexts with different permitted sets,
  split into two sealed interfaces and let the narrower one `extends` and
  `permits` a subset (see `GeolocationResult` vs `GeolocationOutcome`).
  This lets a one-shot callback omit the "pending" arm that would be dead
  there.
- The compiler checks an exhaustive `switch` over the sealed set for
  completeness, so adding a subtype later shows every place that has to
  handle it. A `default:` arm turns that check off.

### Nullability discipline

The reasoning behind the rules in the Nullability section of
[`CONVENTIONS.md`](../CONVENTIONS.md#nullability):

- **Empty collections.** Callers can iterate without a null check, and "no
  values" has a single spelling.
- **`@Nullable T` instead of `Optional<T>`.** The getter matches the setter
  that accepts `null` to clear the value. NullAway already makes callers
  handle the `null` case, so `Optional` adds an allocation and a second style
  without adding safety. Existing `Optional`-returning methods such as
  `Component.getUI()` stay as they are for compatibility.
- **No stand-in values.** A `""`, `-1` or `0` meaning "not set" can not be
  told apart from a value the application actually set.
- **Named states for signals and result types.** Unlike a getter, a signal or
  a "one of N things" value has a real "no data yet" state, and a named value
  lets callers handle it in an exhaustive `switch` — see *Signals for reactive
  state* above.
- **Wire records.** The record used to decode `executeJs` return values or
  DOM event payloads may have `@Nullable` fields because the wire format
  permits omissions; translating it at the boundary keeps those nulls out of
  the public API.

### Lifecycle and cleanup

Resources that outlive a single request — watches, DOM listeners, timers,
client-side subscriptions — are tied to an owner component with a
`DetachListener`, plus an explicit `stop()` for mid-view cancellation. Beyond
that:

- Guard `stop()` with an `active` flag or signal, so detach-after-stop and
  double-stop are safe no-ops.
- If the API supports resuming, reset observable state on resume so
  subscribers re-render with the correct initial value.
- DOM listeners that must keep flowing while the UI is inert (e.g. a
  modal is open over the view but position updates should still
  accumulate) are registered with
  `addEventListener(...).addEventDetail().allowInert()`. Use sparingly
  — inert exists to prevent user actions while something else has
  focus; only bypass it for passive streams.

### Extending an existing entry point

Overloading an existing method, rather than adding a name for a second way to
do the same thing, is a discoverability decision: the user who knows one form
finds the other among the overloads of what they already call, while a separate
name has to be found on its own and reads as a different feature.

The cost lands in the Javadoc. Overloads that do the same thing differently
have to open with the same sentence about what the method is for and then say
which is which — "the version that takes a string" against "the version that
takes an interface". That is cheaper than a name nobody finds.

### Accepting a type the application writes

A type the application declares and the framework interprets — an annotated
interface, a class following a convention — is validated where it is accepted.
The judgment call that leaves open is what happens to a member the mechanism
does not interpret.

Do not let that case through unspoken. When some members of such a type are
interpreted and others are ordinary Java, the type carries two kinds of member
with different rules and the user has to know which is which. That can be the
right design, but then the line between them is a rule stated in one sentence
in the Javadoc and enforced for everything on the interpreted side. What must
never happen is a member that quietly does nothing because it fell between the
two.

### Naming boolean properties

Name a boolean property after its positive state: `isEnabled()` /
`setEnabled(boolean)`, `isVisible()`, `isRequired()` — not `isDisabled()`,
`isHidden()` or `setNotRequired(boolean)`. A negative name turns every
negated check into a double negative: `!isDisabled()` makes the reader undo
two negations to learn that the thing is enabled, and `setHidden(false)` is
harder to follow than `setVisible(true)`.

- **The getter and the setter share one polarity.** `setEnabled(false)` is
  read back with `isEnabled()`, never with a separate `isDisabled()`.
- **Flip the polarity at the boundary, not in the API.** HTML boolean
  attributes name the non-default state (`disabled`, `hidden`) because they
  work by presence and absence; Java has no such constraint. The wrapper maps
  `setEnabled(false)` to the `disabled` attribute so that application code
  never sees the negative form.
- **Words that are positive concepts are fine.** `isEmpty()`, `isInert()`,
  `isDisposed()` and `isReadOnly()` name a state of their own, not the
  negation of another name in the same API. The rule is about `not`, `no`,
  `dis-`, `un-` and `hidden`-style names whose opposite is the state callers
  actually ask about.
- **Existing negative names stay, and new API matches them.** Renaming
  public API is a breaking change; do not rename only to follow this rule.
  When new API joins a family that already uses a negative name, consistency
  with the family wins: a field implements `HasValidation` with
  `setInvalid(boolean)` / `isInvalid()`, so a range field that adds a flag for
  one of its parts names it `isStartInvalid()`, not `isStartValid()`.

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

- **Do not rename just to close the gap with this guidance.** A rename is a
  breaking change and goes through a deprecation cycle, the way `Label` was
  replaced by `NativeLabel` in 24.1 and removed in 25.

### `toString()`

The reasoning behind the rules in the `toString()` section of
[`CONVENTIONS.md`](../CONVENTIONS.md#tostring), and the cases they leave
open. `toString()` is for people: loggers, IDEs and string concatenation call
it on their own, at moments the class does not choose, so it has to say enough
to tell what an instance is and must never get in the way of the code that
called it.

- **Named methods for strings the program needs.** Existing examples are
  `ScrollIntoViewOption.Behavior.getValue()`,
  `ScreenOrientationType.getClientValue()` and `Direction.getClientName()`;
  `toString()` may delegate to them. Call the named method, not `toString()`
  — `element.getOuterHTML()` rather than `element.toString()`,
  `message.toJson()` rather than `message.toString()`. Enums such as
  `HasText.WhiteSpace`, `WebStorage.Storage` and `Unit` predate the rule and
  return their client value from `toString()`; they stay that way for
  compatibility, but do not copy the pattern. Relying on the `toString()` of a
  JDK or library type is fine when its Javadoc documents the format, such as
  `Path` or `URI`.
- **Why it must be cheap and never throw.** It runs on log paths, possibly
  per request, and inside the construction of an error message, where an
  exception from `toString()` hides the error that was being reported (which
  is why `StateNode.describe()` catches one from a component). Do not serialize
  a subtree, walk a large structure or load data to build it. Be careful with
  values the application supplied: `size()` on the collection behind a
  `ListDataProvider` can be slow or initialise a lazy JPA collection, which is
  only acceptable as a deliberate choice.
- **Why `peek()`.** Reading a signal with `get()` inside an effect or a
  computed signal registers a dependency, so merely logging it would change
  what the effect reacts to (`ValueSignal`, `SharedListSignal`, …).
- **Why the runtime class name.** A subclass then reports its own type
  (`Range`, `StringLengthValidator`,
  `AbstractField.ComponentValueChangeEvent`). The signals and
  `RegexpValidator` have the right shape but write the class name as a
  literal, so a subclass such as `EmailValidator` prints `RegexpValidator[…]`;
  do not copy that part. Data providers keep `super.toString()` because many
  instances wrap application data and the identity hash tells two apart.
- **Filling in the shape.** Inside the brackets, use `name=value` pairs the
  way records print them when the values need a name
  (`Name[width=100, height=50]`), and bare values when they do not
  (`StringLengthValidator[1, 10]`). Show what identifies the instance or
  explains its behaviour, not every field.
- **Label numbers that can mislead.** If a value can be read two ways, say
  which one it is: `ListDataProvider` prints `250 backend items, filtered`,
  because a bare `250 items` suggests the data is fine when a filter hides all
  of it.
- **Wrappers include what they wrap.** `DataProviderWrapper` and
  `ConfigurableFilterDataProviderWrapper` print the wrapped provider, so the
  chain is visible in one line.
- **Application content.** Besides credentials, a description written into a
  server log on every request should name structure (class, id, tag), not the
  user's data. `ProxyConfig.Proxy` says that authentication is used instead of
  printing the username and password (its id-first format is not an example
  of the shape).
- **Records already have one.** Do not override the generated `toString()` of
  a record unless it has to hide a field or the components alone do not
  describe it.

Changing the text of an existing `toString()` is not an API break, but tests
in other repositories and applications sometimes compare it, so mention it as a
behaviour change in the pull request.
