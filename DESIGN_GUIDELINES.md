# DESIGN_GUIDELINES.md

Guidance for designing new features in Vaadin Flow. Consult this before
starting any non-trivial new API — especially anything that wraps a browser
or JavaScript API, exposes observable state, or manages a resource with a
lifecycle.

For day-to-day commands, repo structure, and testing workflow, see
[CLAUDE.md](CLAUDE.md).

## Before you start

- **Find the precedent.** Flow already has patterns for per-UI facades,
  reactive signals, sealed result hierarchies, Jackson wire records, and
  browser-API wrappers. Look at `Page`, `History`, `ExtendedClientDetails`,
  `VaadinSession.localeSignal`, `window.Vaadin.Flow.*` helpers, etc. — match
  the existing shape rather than inventing a new one.
- **Understand the blast radius.** Changes to `StateNode`, `Element`, or the
  codec layer ripple across the codebase. If your design needs to touch
  them, plan for full test runs and a longer review cycle.
- **Frontend ↔ server is a contract.** Any protocol or DOM-event change
  needs both sides updated in the same PR.

## API shape

### Per-UI facades

If a feature is UI-scoped (not session-scoped), expose it via
`UI.getXxx()` returning a single instance created once per UI. The facade
holds `private final UI ui`, and all `executeJs` calls go through
`ui.getElement()` or `ui.getPage()`. Follow the `Page` / `History`
pattern. Enforce single-instance creation in the constructor if needed.

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
- NullAway requires `@Nullable` on constructor type arguments for
  nullable-parameterised signals:
  `new ValueSignal<@Nullable X>(null)`.

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

## Browser / JavaScript integration

### Wrap thin; document thick

Java API wrapping a browser/JS API must be written for Java developers who
do not know the underlying JS API. Explain: what the method does in Java
terms; when to call it; what the parameters and return value mean;
threading and lifecycle expectations; any browser-specific caveats
(e.g. "Safari always returns UNKNOWN"). Do not assume the reader will
read the W3C spec or the `.ts` source.

### Supported browsers

Only write client code targeting these. **No fallbacks, no polyfills** for
anything else:

- Chrome (evergreen)
- Firefox (evergreen)
- Firefox Extended Support Release (ESR)
- Safari 17 or newer (latest minor version in each major series)
- Edge (Chromium, evergreen)

### JavaScript location and globals

- **Non-trivial JS goes in its own `.js` or `.ts` file** under
  `META-INF/frontend/` (Java side) or `flow-client/src/main/frontend/`
  (client module), loaded via `@JsModule` on `UI.java`. Don't inline
  anything beyond a one-liner in a Java string.
- **Global state and helper functions live under `window.Vaadin.Flow`**
  (e.g. `window.Vaadin.Flow.geolocation`,
  `window.Vaadin.Flow.componentSizeObserver`). Use annotations on
  `UI.java` for scripts that need to run globally.

### `executeJs` parameter passing

- **Never** concatenate values into the expression string. Always pass
  them as parameters and reference them positionally (`$0`, `$1`, ...).
  String concatenation is a prompt for injection bugs and quoting
  nightmares.
- **Never build JSON manually by string concatenation.** Use Jackson 3 for
  construction.
- Element parameters arrive on the client as DOM references (or `null`);
  plan for that on both sides.
- Return values from JS can be deserialised to Java records automatically;
  use a private record for the wire shape.

### DOM event naming

- **Prefix custom DOM events with `vaadin-`** — e.g.
  `vaadin-geolocation-position`, not `geolocation-position`. This keeps
  the event namespace distinct and grepable.
- Event payloads travel as Jackson-annotated records. Keep the wire shape
  faithful to what the browser produces (e.g. `long timestamp` not
  `Instant`) and provide convenience accessors on the public type.

### Bootstrap-time data

If a feature needs an initial value before the first user interaction,
thread it through the bootstrap handshake rather than waiting for a
round-trip:

- Client collects the value in `collectBrowserDetails` (make that
  function async if needed) and appends it to the init request as a
  `v-xxx` parameter.
- Server reads it in `ExtendedClientDetails.fromJson` and seeds the
  appropriate `UIInternals` field / signal.
- The public Java signal picks up the value on UI attach — no
  additional round-trip required.

## Documentation

- Javadoc for public API explains the *why* and the caveats, not just the
  type signature.
- Call out reliability concerns prominently — if a value is best-effort,
  say so, and enumerate the browsers where it degrades.
- Prefer short, runnable examples in the class-level Javadoc. Keep
  examples consistent with the real platform APIs (e.g. if you show
  `map.setCenter(...)`, use the real Vaadin Map `Coordinate(longitude,
  latitude)` shape).
- **Do not add `@since` tags.**
- Javadoc describes the code today, not what changed. Change history
  belongs in commit messages.

## Testing new features

- **Write the tests that should pass first.** If they expose problems in
  the implementation, fix the implementation afterwards — don't rewrite
  the tests to match a broken implementation.
- Keep the unit test count minimal — only the essential cases. More tests
  are not better; focused tests are.
- For browser-facing features, add an IT view under
  `flow-tests/test-root-context/` that mocks `navigator.geolocation` (or
  the relevant browser API) and exercises both happy-path and error
  branches. Use an option value to trigger errors deterministically
  (e.g. `maximumAge === -1`). Use `setInterval` to simulate a stream of
  updates, and verify that `clearWatch` actually stops them.
- ITs assert concrete outputs, not just "not null". If floating-point
  arithmetic would make assertions brittle, simplify the mock to emit
  stable values (different timestamps suffice for uniqueness).
- Add a short settle pause before snapshotting counts after a
  stop-like action — an in-flight event can still land right after the
  stop marker appears in the DOM.
- When an IT fails, debug with Playwright before guessing — see what the
  browser is actually doing.

## Commit / PR hygiene

- When a commit resolves an issue in this repo, add `Fixes #issuenumber`
  to the message.
- Use `test:` prefix for commits that only touch tests; `fix:` is for
  production-code fixes.
- When opening a PR, mark it as draft. Remind the author to self-review
  before marking it ready.
