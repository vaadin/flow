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

If the facade hands out a stateful handle (e.g. `Geolocation.track()`
returning a `GeolocationTracker`), make the handle's constructor
**package-private** so application code cannot bypass the facade.

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

- Public canonical constructor with `@Nullable` params and validation
  (reject negative durations, etc.) — NullAway needs the canonical ctor
  spelled out explicitly rather than the record-derived one.
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
- DOM listeners that must keep flowing while the UI is inert (e.g. a
  modal is open over the view but position updates should still
  accumulate) are registered with
  `addEventListener(...).addEventDetail().allowInert()`. Use sparingly
  — inert exists to prevent user actions while something else has
  focus; only bypass it for passive streams.

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

- **Non-trivial JS goes in its own file**, never inlined beyond a
  one-liner in a Java string. Two valid homes:
  - `flow-client/src/main/frontend/Xxx.ts` imported from `Flow.ts`
    (`import './Xxx';`). Use this for platform-level features that
    need to be available before the bootstrap handshake (anything
    referenced from `collectBrowserDetails`, anything that must attach
    `document` / `window` listeners before the first user interaction).
    Precedents: `Geolocation.ts`, `PageVisibility.ts`. Prefer TypeScript
    here — new files should not be `.js`.
  - `META-INF/frontend/xxx.js` loaded via `@JsModule("./xxx.js")` on
    `UI.java` or a component. Use this when the script is tied to a
    specific Java API surface and does not need to run at bootstrap
    time.
- **Global state and helper functions live under `window.Vaadin.Flow`**
  (e.g. `window.Vaadin.Flow.geolocation`,
  `window.Vaadin.Flow.pageVisibility`,
  `window.Vaadin.Flow.componentSizeObserver`). Use annotations on
  `UI.java` for scripts that need to run globally.
- **`init(element)` installers must be idempotent.** A facade may call
  `window.Vaadin.Flow.xxx.init(this)` more than once per UI element
  (lazy (re)arming from a signal accessor, navigation to a view that
  re-subscribes, etc.). Track installations per element (WeakMap) and
  dispose the previous set of listeners before attaching new ones so
  the element never carries duplicates.

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
- **Log `executeJs` client-side errors at `DEBUG`, not WARN/ERROR.** A
  failed JS call usually means the feature is unavailable (user denied
  permission, API missing, insecure context) — not a server bug. The
  pattern is `.then(ok -> {}, err -> LOGGER.debug("X failed: {}", err))`.

### DOM event naming

- **Prefix custom DOM events with `vaadin-`** — e.g.
  `vaadin-geolocation-position`, not `geolocation-position`. This keeps
  the event namespace distinct and grepable.
- Event payloads travel as Jackson-annotated records. Keep the wire shape
  faithful to what the browser produces (e.g. `long timestamp` not
  `Instant`) and provide convenience accessors on the public type.

### Server ↔ client signalling patterns

For streaming and state-change wiring, keep DOM events as **transport**
and `Signal` as **state**. Applications should subscribe to the signal;
the DOM events are an implementation detail of the facade.

- **Event-to-Signal bridging.** The client dispatches a
  `vaadin-xxx-position` / `vaadin-xxx-error` CustomEvent per update; the
  server-side facade has a DOM listener that pulls the detail record
  and writes it to the private `ValueSignal`. Applications subscribe to
  the signal.
- **Client-initiated state-change bridge-back.** For state that changes
  without a server-initiated request (permission change, network
  online/offline, window resize), the client dispatches a
  `vaadin-xxx-change` event on `document.body` (which is the UI's root
  element on the server). The facade constructor registers a listener
  on `ui.getElement()` and forwards the detail into the same
  `UIInternals` signal the bootstrap path seeds. No polling required.
- **Stable client-side keys for async browser handles.** When the
  browser API returns an opaque id asynchronously (e.g.
  `watchPosition()`), don't try to round-trip it back to the server to
  later cancel. Pre-generate a UUID on the server, pass it as an
  `executeJs` parameter, and have the client's wrapper store its own
  `Map<key, browserId>`. Both sides then use the same key for
  subsequent operations (`clearWatch(key)` on the client looks up the
  browser-assigned id).

### Bootstrap-time data

If a feature needs an initial value before the first user interaction,
thread it through the bootstrap handshake rather than waiting for a
round-trip:

- Client collects the value in `collectBrowserDetails` (make that
  function async if needed) and appends it to the init request as a
  `v-xxx` parameter. The TS that produces the value must be imported
  from `Flow.ts` so it is loaded when `collectBrowserDetails` runs —
  `@JsModule` on `UI.java` loads too late for this path.
- Server reads it in `ExtendedClientDetails.fromJson` and seeds the
  appropriate `UIInternals` field / signal.
- The public Java signal picks up the value on UI attach — no
  additional round-trip required.
- Seed the server-side signal with a sentinel (`UNKNOWN`, `Pending`, …)
  so the brief window between attach and handshake completion is
  distinguishable from a genuine reading. Precedents:
  `GeolocationAvailability.UNKNOWN`.

### Feature-capability detection

Probe for feature availability **without calling the feature itself**
— calling it usually triggers a permission prompt, which defeats the
point of probing. Useful primitives:

- `window.isSecureContext` — HTTPS or `localhost`. Most sensitive
  browser APIs require this.
- `document.featurePolicy?.allowsFeature("xxx")` — Chromium-only;
  Firefox and Safari don't expose a feature-policy introspection API.
  Absence of the API should be treated as "allowed", not "unsupported".
- `navigator.permissions.query({ name: "xxx" })` — returns a
  `PermissionStatus` whose `.state` is `"granted" | "denied" |
  "prompt"` and which also emits a `change` event. Safari may reject
  with a TypeError for specific permission names; catch and fall back
  to an `UNKNOWN` sentinel.
- Expose the result to the server via the bootstrap param pattern
  above, plus a `vaadin-xxx-availability-change` event for subsequent
  changes.

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
  `flow-tests/test-root-context/` that mocks the relevant browser API
  and exercises both happy-path and error branches. Use an option
  value to trigger errors deterministically (for geolocation,
  `maximumAge === -1` works). If the API streams updates (e.g.
  `watchPosition`), use `setInterval` to simulate them and verify
  that the matching cancel call (e.g. `clearWatch`) actually stops
  them.
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
  production-code fixes; `feat:` is for new features.
- When opening a PR, mark it as draft. Remind the author to self-review
  before marking it ready.
