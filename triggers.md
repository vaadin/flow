# Trigger API — design plan

Server-side API for wiring **client-side triggers** (DOM events, shortcuts,
…) to **client-side actions** (clipboard copy, set property, run JS, …)
reading values from **outputs** (DOM properties, literals, JS expressions,
…), without a server round-trip when not needed. The motivating constraint
is the class of browser APIs that must run inside the user-gesture DOM
event handler that produced the event — clipboard, fullscreen, file
download, web share, …

The API is in `com.vaadin.flow.component.trigger` and is designed for
extension: apps and add-ons add new trigger / action / output types by
extending the abstract bases and pairing them with a JS handler registered
on `window.Vaadin.Flow.triggers`.

## Motivation in a nutshell

The Vaadin 8 trigger API had a clean three-shape model — `Trigger`,
`Action`, `Output<T>` — that solved this class of problems and the
adjacent class of "disable-on-click for a button triggered by a keyboard
shortcut" (the `dev.vaadin.com/ticket/8484` case): the click is destined
for the server, but the visual / state change has to happen *immediately*
on the client so a second click during the round-trip latency window
becomes impossible.

Flow has every primitive needed to rebuild that model — `Element.executeJs`,
`@JsModule`, server-side `Signal<T>`, `NodeFeature` for per-node storage,
the `window.Vaadin.Flow.<feature>` namespacing pattern — without the
GWT-era `TriggerSupport`-as-connector indirection.

## Confirmed design decisions

| Question | Decision |
| --- | --- |
| How are `Output<T>` values resolved when a trigger fires? | **Snapshot at trigger time.** The dispatcher walks outputs synchronously inside the DOM event handler and passes resolved values to actions. Signals are not in the v0 client runtime; a `SignalOutput<T>` adapter is deferred. |
| Where do trigger/action/output records live in the state tree? | **Per-host-element.** Each `Element` has a lazy `TriggerSupport` server-side feature holding its own id-keyed pools. Cross-element wiring works by carrying the target element as an `executeJs` parameter; the snapshot references it by index. |
| What use cases must the v0 slices validate end-to-end? | Clipboard copy, disable-on-shortcut, inline JS escape hatch, server-side `Runnable` action. |
| `Trigger.triggers(SerializableRunnable)` ergonomics? | **Keep the lambda overload** on `Trigger`. Internally it constructs a `ServerCallbackAction(handler)`. |
| Server-state mirroring of actions? | **Eager.** Actions that have a server-observable effect (e.g. `SetEnabledAction`) override `applyServerSideEffect()`. The mirror runs at the **start** of the same server cycle that processes the trigger event, **before** user-attached DOM event listeners fire — so listener code sees the post-action state and a second click during the latency window is a no-op locally. |
| Public API host type? | **`Component` and `Element` overloads everywhere.** The Element constructor is canonical; the Component overload delegates via `host.getElement()`. |

## Architecture

### Server

- Public package: `com.vaadin.flow.component.trigger`.
  - Interfaces: `Trigger`, `Action`, `Output<T>`.
  - Abstract bases (extension points): `AbstractTrigger`, `AbstractAction`,
    `AbstractOutput<T>`. Each carries a **namespaced type id**
    (`flow:click`, `vaadin:notification`, `myapp:double-tap`). Subclasses
    override `buildClientConfig(ConfigContext)` to ship JSON config and
    `applyServerSideEffect()` for the server mirror.

- Internal package: `com.vaadin.flow.component.trigger.internal`.
  - `TriggerSupport extends ServerSideFeature`: the per-element store.
    Holds three id-keyed pools (triggers, actions, outputs) plus a list of
    bindings `{triggerId, [actionIds…]}`. Implements `ConfigContext` so
    subclasses can ask it to register outputs / element references by id.
  - `ConfigContext`: tiny interface passed to `buildClientConfig` —
    `registerOutput(Output)` and `referenceElement(Element|Component)`.
  - `ServerCallbackAction`: the sugar target for `Trigger.triggers(Runnable)`.

- Three small framework registrations (same pattern as `SignalBindingFeature`):
  - `NodeFeatures.TRIGGER_SUPPORT` constant.
  - Factory registered in `NodeFeatureRegistry`.
  - Slot added to `BasicElementStateProvider.features`.

### Client

- `flow-client/src/main/frontend/Triggers.ts` — imported from `Flow.ts`
  so it ships in the boot bundle.
- Self-registers `window.Vaadin.Flow.triggers` with the public API
  `registerTrigger / registerAction / registerOutput / bind / unbind`.
- Built-in factories registered at module load. Add-on `@JsModule`s
  register their own custom types against the same registry.
- `bind(host, snapshot, extraElements?)` is **idempotent** — tracked per
  host via a `WeakMap`; a second `bind` disposes the previous installation
  before wiring the new one. (Per `DESIGN_GUIDELINES.md`.)
- The dispatcher walks bindings synchronously inside the DOM event handler
  so user-gesture-only browser APIs (clipboard, fullscreen, share, …)
  remain callable from actions.

### Wire format

A single JSON snapshot per host, sent through `Element.executeJs`:

```jsonc
{
  "triggers": { "0": { "type": "flow:click",          "config": { /* … */ } } },
  "actions":  { "0": { "type": "flow:clipboard-copy", "config": { "textOutput": 0 } } },
  "outputs":  { "0": { "type": "flow:property",       "config": { "property": "value", "element": 1 } } },
  "bindings": [ { "trigger": 0, "actions": [0] } ]
}
```

The executeJs call passes the host element as `this` and the snapshot as
`$0`. Secondary elements referenced by outputs/actions are passed as
extra positional parameters and appear in the snapshot as parameter
indices (`element: 1` means the second extra element, `0` means the
host itself).

The snapshot is re-emitted on:
- Every binding mutation (collapsed via `StateTree.beforeClientResponse`
  to one call per request cycle).
- Every host (re-)attach, so re-attaching to the DOM after a detach
  doesn't drop the bindings.

## Public API surface (sketch)

```java
public interface Trigger extends Serializable {
    Trigger triggers(Action... actions);
    Trigger triggers(SerializableRunnable serverHandler);
    void remove();
}

public interface Action extends Serializable {}
public interface Output<T> extends Serializable {}

public abstract class AbstractTrigger implements Trigger {
    protected AbstractTrigger(String typeId, Element host);
    protected AbstractTrigger(String typeId, Component host);
    public ObjectNode buildClientConfig(ConfigContext context);
    public final Element getHost();
    public final String getTypeId();
}

public abstract class AbstractAction implements Action {
    protected AbstractAction(String typeId);
    public ObjectNode buildClientConfig(ConfigContext context);
    public void applyServerSideEffect();
}

public abstract class AbstractOutput<T> implements Output<T> {
    protected AbstractOutput(String typeId, Class<T> valueType);
    public ObjectNode buildClientConfig(ConfigContext context);
}
```

## Slice plan

Each slice is one PR-sized chunk: built-ins + unit tests + one IT.

### Slice 1 — Clipboard copy (DONE)

- **Triggers**: `ClickTrigger` (`flow:click`).
- **Outputs**:  `PropertyOutput<T>` (`flow:property`).
- **Actions**:  `ClipboardCopyAction` (`flow:clipboard-copy`).
- **Server stub**: `ServerCallbackAction` (`flow:server-callback`) — class
  in place so `Trigger.triggers(Runnable)` compiles; client handler ships
  in slice 4.
- **IT**: `TriggerClipboardCopyIT` — button click copies textfield value;
  stubs `navigator.clipboard.writeText` to avoid permission flakiness.
- **Validates**: Per-host snapshot + executeJs wire + idempotent bind +
  user-gesture preservation + cross-element output reference.

### Slice 2 — Disable-on-shortcut (DONE)

- **Triggers**: `ShortcutTrigger` (`flow:shortcut`, `Key` + `KeyModifier…`,
  scoped to host via a capturing `keydown` listener).
- **Actions**:
  - `ClickAction` (`flow:click`) — `element.click()` on a target.
  - `SetEnabledAction` (`flow:set-enabled`, boolean) — toggles the
    `disabled` attribute locally **and** mirrors server-side via
    `applyServerSideEffect()` calling `Element.setEnabled(boolean)`.
- **Mirror plumbing**: `TriggerSupport` lazily registers a single
  `ReturnChannelRegistration` per host (with
  `DisabledUpdateMode.ALWAYS`); the bind call passes it to the client as
  the last `executeJs` parameter, where it arrives as a callable
  function. Each action factory receives a `notifyServer` callback that
  closes over its own id. When `SetEnabledAction.run()` finishes its
  local DOM change it invokes `notifyServer()`, which delivers
  `[actionId]` to `dispatchMirror` on the server, which looks up the
  action and calls `applyServerSideEffect()`.
- **Ordering note**: the mirror notification is queued through Flow's
  outbound channel infrastructure inside the same synchronous DOM event
  handler. With the user wiring actions as `(disable, click)` the
  notification is queued before the click event Flow emits as a result
  of `target.click()`, so the server processes the mirror first and
  the user's `ClickListener` observes the post-action enabled state.
  This is best-effort and depends on user-chosen action order; the
  stronger guarantee (mirror-before-listener regardless of order)
  remains deferred.
- **IT**: `Enter` shortcut on a `Div` form → disable + click on a submit
  button. Asserts both client-side (`disabled` attribute set) and
  server-side (`"clicked, enabled=false"` text rendered by the
  `ClickListener`).
- **Validates**: trigger on one component / action on another;
  end-to-end server mirroring via the return channel; the
  disable-during-latency-window pattern.

### Slice 3 — Inline JS escape hatch (DONE)

- **Triggers**: `JsTrigger` (`flow:js`) — expression evaluated with the
  host element as {@code this} and {@code trigger} as the fire helper;
  may return a cleanup function used on uninstall.
- **Actions**:  `JsAction` (`flow:js`, varargs of `Output<?>`) —
  expression evaluated with {@code output(i)} resolving to the i-th
  declared output's current value at fire time. Declared outputs go
  through the shared `ConfigContext.registerOutput(...)` path so they
  dedupe with built-in outputs.
- **Outputs**:  `JsOutput<T>` (`flow:js`, valueType, expression) —
  expression evaluated at the moment a trigger fires; its return value
  is the output.
- **Client wiring**: all three use `new Function(...)` (not `eval`) and
  swallow compile/runtime exceptions to a `console.debug` so a broken
  expression doesn't break the rest of the dispatch.
- **IT**: button click → `JsTrigger` fires → `JsAction` reads
  `JsOutput`'s value and writes it into a span. Pure JS round trip with
  no custom Java action class and no custom TS module.
- **Validates**: that add-on authors can ship a working custom type
  without writing a TS module first.

### Slice 4 — Server callback as an action (DONE)

- **Action**: wire the existing `ServerCallbackAction` (`flow:server-callback`)
  stubbed in slice 1.
- **Mechanism**: piggybacks on the per-host `ReturnChannelRegistration`
  already in place from slice 2's mirror plumbing — no new round-trip
  path, no UI-scoped helper. `ServerCallbackAction.applyServerSideEffect()`
  invokes the wrapped `SerializableRunnable`; the client factory's
  `run()` is a one-liner calling `notifyServer()`. The existing
  `TriggerSupport.dispatchMirror` looks the action up by id and calls
  `applyServerSideEffect()`.
- **No outputs to the `Runnable`** in v0. `Trigger.triggers(Runnable)` is
  no-arg sugar; if a callback needs values, use a `@ClientCallable` on
  a component directly, build a custom `Action` subclass, or carry the
  context through server-side state.
- **IT**: button → `Trigger.triggers(() -> result.setText("server fired"))`
  → IT asserts the result text updates.
- **Validates**: the degrade-to-round-trip path; the lambda overload
  `Trigger.triggers(Runnable)`; that the per-host channel handles both
  mirror notifications (slice 2) and server callbacks (slice 4)
  without protocol changes.

### Slice 5 — `SignalOutput<T>` (planned)

- **Output**: `SignalOutput<T>(Signal<T>)` (`flow:signal-value`). Reads
  a server-side `Signal<T>` and exposes its current value to trigger
  actions, snapshot-style.
- **Mechanism**: at snapshot-build time the server reads
  `signal.peek()` and ships the value directly inside the output's
  config (`{"value": <jsonValue>}`). On construction the output
  subscribes to its `Signal` and on every change schedules the host's
  next `beforeClientResponse` flush — same path `TriggerSupport`
  already uses for `.triggers(…)` mutations. The client factory is
  trivial: `read()` returns the value lifted from `config.value`.
- **Cleanup**: the signal subscription is anchored on the host's
  state-node detach hook so it doesn't outlive its UI.
- **Use cases**: action needs a value from a `ValueSignal`,
  `ComputedSignal`, or `SharedValueSignal` that isn't naturally
  represented as a DOM property on any element (session state,
  derived computations, multi-user collaborative state).
- **Why a dedicated type instead of "bind signal → property, then
  `PropertyOutput`"**: ergonomic — saves creating a ghost property
  whose only purpose is to relay the signal value into a trigger, and
  makes the dependency explicit at the call site.
- **Snapshot semantics, not reactive composition.** This slice does
  not introduce `FormatOutput`, `Output.not`, `ComputedSignal`-of-
  outputs etc. — those remain in "deferred". `SignalOutput` is a
  thin reader, not a graph builder.
- **Tradeoff**: every signal change re-emits the snapshot for the
  host. Fine for low-frequency UI/session signals; users binding a
  rapidly-changing signal (mouse position, etc.) should reach for
  `JsOutput` or a property binding instead.
- **IT**: button → `ClipboardCopyAction` reading a
  `SignalOutput<String>(sessionLocaleSignal)`. Mutate the signal,
  click, assert clipboard receives the new value.
- **Validates**: signal value flows into snapshot; signal change
  triggers re-emit; cleanup on detach.

## Extending the API (apps and add-ons)

1. Write a server class extending `AbstractTrigger` / `AbstractAction` /
   `AbstractOutput<T>` with a namespaced type id (`myapp:foo`). Override
   `buildClientConfig(ConfigContext)` to ship config. If the action has
   a server-observable effect, override `applyServerSideEffect()`.
2. Add `@JsModule("./my-trigger.js")` (or annotate `UI`) for a TS/JS
   file that calls `window.Vaadin.Flow.triggers.registerAction(
   "myapp:foo", factory)` at module load.
3. That's it. The framework's snapshot pipeline and dispatcher pick up
   the new type by id.

A `JsAction` / `JsOutput<T>` (slice 3) covers the case where the add-on
author doesn't want a TS file at all.

## Deferred / explicitly out of scope for v0

- **Output composition** (`FormatOutput`, `Output.not`, `ConditionalAction`,
  `Output.combine`, …). Straightforward to add once the core API is
  proven; deliberately omitted from v0 so the public surface stays small.
- **Fluent shorthands on built-in components** (`button.triggerByPress(…)`,
  `myButton.createDisableAction()`, `myButton.setShortcutKey(KeyCode.ENTER)`,
  …). One-line sugar on top of the low-level API. Defer until the
  low-level API stabilises so the shorthands don't ossify a wrong shape.
- **Touching `flow-client` GWT.** The original Vaadin 8 architecture
  used GWT connectors for triggers/actions; the modern Flow client mixes
  GWT Java (state-tree binding) with TypeScript modules
  (`Geolocation.ts`, `PageVisibility.ts`). The trigger runtime lives in
  the TS half, leaving the GWT side untouched.
- **Public access to the wire format.** Snapshot shape lives in the
  internal package and can change without API breakage.

## Lifecycle & ordering details

- `TriggerSupport` is a `ServerSideFeature` — server-only, never serialises
  itself through the standard `NodeChange` machinery. Wire updates go
  through `executeJs`, not the state-tree sync.
- `Element.executeJs` defers until the element is attached, so the first
  `bind` call after registration arrives at the right time without
  manual attach handling.
- For **re-attach**, `TriggerSupport` adds a one-shot
  `addAttachListener` on first use that re-emits the snapshot. The
  client's `bind` is idempotent (disposes previous handlers first).
- Mutations are coalesced via
  `StateTree.beforeClientResponse(stateNode, …)` so a burst of
  `.triggers(…)` calls in one request produces a single `executeJs`
  emit.
- Action server-side effects (slice 2+) run **before** user-attached
  `DomEventListener`s in the same server cycle. This is the
  load-bearing ordering for `SetEnabledAction` etc.: the server's view
  of the component already reflects the action by the time application
  code runs.

## File map

```
flow-server/src/main/java/com/vaadin/flow/component/trigger/
  Trigger.java                 — interface
  Action.java                  — interface
  Output.java                  — interface (generic)
  AbstractTrigger.java         — base + Element/Component constructors
  AbstractAction.java          — base + applyServerSideEffect hook
  AbstractOutput.java          — base
  ClickTrigger.java            — flow:click  (slice 1)
  PropertyOutput.java          — flow:property (slice 1)
  ClipboardCopyAction.java     — flow:clipboard-copy (slice 1)
  ShortcutTrigger.java         — flow:shortcut       (slice 2)
  ClickAction.java             — flow:click          (slice 2)
  SetEnabledAction.java        — flow:set-enabled    (slice 2)
  JsTrigger.java               — flow:js             (slice 3)
  JsAction.java                — flow:js             (slice 3)
  JsOutput.java                — flow:js             (slice 3)
  internal/
    TriggerSupport.java        — per-element ServerSideFeature
    ConfigContext.java         — passed to buildClientConfig
    ServerCallbackAction.java  — flow:server-callback (slice 4)
    TriggerCallbackHandler.java — UI-scoped @ClientCallable host (slice 4)

flow-client/src/main/frontend/
  Triggers.ts                  — window.Vaadin.Flow.triggers runtime

flow-tests/test-root-context/src/main/java/com/vaadin/flow/uitest/ui/
  TriggerClipboardCopyView.java       (slice 1)
  TriggerShortcutDisableView.java     (slice 2)
  TriggerJsEscapeHatchView.java       (slice 3)
  TriggerServerCallbackView.java      (slice 4)
```
