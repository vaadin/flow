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

