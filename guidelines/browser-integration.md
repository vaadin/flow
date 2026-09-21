# Browser Integration

How to wrap a browser or JavaScript API in Java, where the client-side code
lives, and how the two sides talk to each other.

For the Javadoc expectations that come with a browser-API wrapper, see
[Documenting](documenting.md).

## Supported browsers

Only write client code targeting these. **No fallbacks, no polyfills** for
anything else:

- Chrome (evergreen)
- Firefox (evergreen)
- Firefox Extended Support Release (ESR)
- Safari 17 or newer (latest minor version in each major series)
- Edge (Chromium, evergreen)

## JavaScript location and globals

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

## `executeJs` parameter passing

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

## DOM event naming

- **Prefix custom DOM events with `vaadin-`** — e.g.
  `vaadin-geolocation-position`, not `geolocation-position`. This keeps
  the event namespace distinct and grepable.
- Event payloads travel as Jackson-annotated records. Keep the wire shape
  faithful to what the browser produces (e.g. `long timestamp` not
  `Instant`) and provide convenience accessors on the public type.

## Server ↔ client signalling patterns

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

## What goes on the wire

- **No server-side identifiers.** A Java class or method name in a UIDL
  response tells the browser about the application without helping it run.
  Key what the client looks up by a hash of the content it runs: renaming a
  class or a method then changes nothing the browser holds, and two call sites
  that run the same JavaScript share one entry.
- **Debug information is development-only and belongs in the generated
  frontend file.** One readable string —
  `com.vaadin.flow.component.Focusable$FocusJs.focus/1` — next to the function
  it identifies makes a client-side error message useful. Repeating it in every
  response is payload that production does not need.
- **Reuse the client's constant pool instead of adding a second cache.** A
  payload the server sends more than once — an expression, a function
  identifier — goes out once as a constant and is referenced afterwards. Keep
  the message shape identical for the old and the new path so one code path on
  the client resolves both, and put arriving constants in the pool before
  anything resolves a reference to them.
- **Nothing the receiver can derive.** A key the client ignores, or a count it
  can read off the payload it already has, is size on every response and one
  more thing that can disagree with reality.
- **Assume the two sides can disagree.** A browser that reconnects after a
  server restart, without reloading the page, still holds the bundle from
  before. Compare what arrives with what the local function expects and report
  a mismatch through the error channel of the call, so a
  `PendingJavaScriptResult` that can never run completes instead of hanging.
  Binding the parameters one slot off — an element that lands where an argument
  was expected — is the failure mode to design out, because it is silent.

## Generated frontend files

A file the build generates from Java into `frontend/generated/` is part of the
bundle contract, and three things follow from that:

- **A bundle that predates the generated content has to be rebuilt.** Hash the
  file into the stats that bundle validation compares, the way the commercial
  banner is compared, so the first build after the feature lands rebuilds once.
  Never skip a rebuild the application needs: a bundle without the content is a
  broken application, so either rebuild or fail with a message that names what
  is missing.
- **Regeneration in development cannot rely on the dev loop alone.** The
  `vaadin-dev` CLI refines the frontend on its own terms, but a class the IDE
  recompiles never passes through it. Implement a `VaadinHotswapper` for the
  change as well and push the result to the browser with `sendHmrEvent(…)`, so
  a new declaration shows up without a restart.
- **The task that generates the file owns reading and patching it.** Callers
  ask it to add what is missing; parsing and merging stay private. See
  [Design](design.md).

## Bootstrap-time data

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

## Feature-capability detection

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

