# flow-client core rewrite — plan & status

Living plan for migrating the remaining Java `flow-client` to TypeScript, on the
long-lived branch `flow-client-jsinterop-exports` (one PR). See
[MIGRATION_STRATEGY.md](MIGRATION_STRATEGY.md) for the original analysis and
[DESIGN_GUIDELINES.md](DESIGN_GUIDELINES.md) for API conventions.

## Approach: build-alongside, then cutover

The reactive + state-tree core is too interconnected (and its JVM tests break en
masse the moment any of it goes browser-only) to migrate class-by-class while
keeping the shared branch green. So, with the no-duplicate rule **temporarily
suspended** for this rewrite, we:

1. Build each layer as **new TypeScript modules under
   `src/main/frontend/internal/`**, mocha-tested in isolation, leaving the Java
   live. The branch stays green because nothing Java changes.
2. Express not-yet-ported dependencies as **TS contracts (interfaces)** that
   later layers — and ultimately the real classes — satisfy.
3. At the end, **cut over**: point the engine bootstrap at the TS modules,
   delete the Java client, and port remaining JVM tests to mocha.

Per-commit gate (all must pass before push): `npm run lint`, `npx tsc --noEmit`,
`npx web-test-runner --node-resolve --playwright`. Then push.

CI note: while a module is **build-alongside** (not yet imported/registered, not
in the `build-gwt-test-internals` bundle), nothing in the GWT client, the
production bundle, the GwtTests or the ITs executes it — the only CI steps that
touch it are `tsc` + `lint` (compile phase) and mocha (test phase), i.e. exactly
the local gate above. So the local gate is authoritative for these commits;
don't gate on "Flow Validation" green per commit (it mostly re-runs unrelated
Java/IT suites and exposes unrelated flakes). Re-enable CI gating the moment a
module is wired into `registerInternals` or the gwt-test-internals bundle (then
ES5/HtmlUnit GwtTests and real-browser ITs give signal mocha can't), and gate on
green decisively at **cutover**.

## Two migration modes (important)

Not every remaining class is a clean build-alongside candidate. Some binding
classes were **already partially migrated earlier via the import-direction
pattern** — their JS logic lives in `src/main/frontend/internal/*` and the Java
class delegates to `window.Vaadin.Flow.internal.*` via JSNI.

- **Pure-Java classes** (no existing TS internal) → **build-alongside** (new TS
  module + mocha tests), as above.
- **Hybrid classes** (already have a TS internal) → **reconcile/complete the
  existing internal** by folding remaining Java logic into it. Do NOT build a
  parallel TS class — that would duplicate the existing internal.

Known hybrid classes (verified — they reference `Vaadin.Flow.internal` in Java):
- `binding/ServerEventObject.java` ↔ existing `internal/ServerEventObject.ts`
- `binding/SimpleElementBindingStrategy.java` ↔ existing internals

Check before porting any further class: `grep -l "Vaadin.Flow.internal"
src/main/java/.../<Class>.java` and `find src/main/frontend -iname "*<Class>*"`.

## Status — done (TypeScript, mocha-tested, CI-green)

Foundation (build-alongside, pure-Java; ~17 classes, 69 mocha tests):

| Module | Location | Tests |
|---|---|---|
| Reactive core: `Reactive`, `Computation`, `ReactiveEventRouter`, events/listeners/value types | `internal/reactive/reactive.ts` | 17 |
| `MapProperty` (+ change event/listener) | `internal/nodefeature/MapProperty.ts` | 11 |
| `NodeFeature`, `NodeMap`, `NodeList` (+ add/splice events & listeners) | `internal/nodefeature/{NodeFeature,NodeMap,NodeList}.ts` | 14 |
| `StateNode` (+ unregister event/listener) | `internal/StateNode.ts` | 4 |
| `StateTree` | `internal/StateTree.ts` | 14 |
| `NodeFeatures` / `NodeProperties` constants | `internal/nodefeature/NodeFeatures.ts` | — |
| `ConstantPool` | `internal/ConstantPool.ts` | 3 |
| `Debouncer` | `internal/binding/Debouncer.ts` | 3 |
| Binding contracts: `BindingStrategy`, `BinderContext` | `internal/binding/BindingStrategy.ts` | — (interfaces) |
| `TextBindingStrategy` | `internal/binding/TextBindingStrategy.ts` | 3 |
| `ServerEventObject` (hybrid; full `$server` lifecycle, `defineMethod`, `getEventData` + expression eval) | `internal/ServerEventObject.ts` | 15 |
| `ServerEventHandlerBinder` (binds CLIENT_DELEGATE_HANDLERS names onto `$server`, splice-synced) | `internal/binding/ServerEventHandlerBinder.ts` | 6 |
| `SimpleElementBindingStrategy` **complete** (slices 1–16) — all binding methods + `bind()` orchestrator + `class SimpleElementBindingStrategy implements BindingStrategy<Element>` | `internal/binding/SimpleElementBindingStrategy.ts` | 69 |
| `Binder` (entry point: strategy list + `BinderContext` impl + `bind`) — **binding layer complete** | `internal/binding/Binder.ts` | 3 |
| `ServerConnector` (builds + enqueues RPC messages) + `JsonConstants` (RPC subset) + `ClientJsonCodec.encodeWithoutTypeInfo` | `internal/communication/ServerConnector.ts`, `internal/JsonConstants.ts` | 5 |
| `ServerRpcQueue` (accumulates + deferred-flushes RPC invocations via MessageSender) | `internal/communication/ServerRpcQueue.ts` | 5 |
| `LoadingIndicatorStateHandler` (loading indicator from RPC activity, mutes high-frequency events) | `internal/communication/LoadingIndicatorStateHandler.ts` | 5 |
| `RequestResponseTracker` (single-active-request + lifecycle events) + `ResynchronizationState` | `internal/communication/RequestResponseTracker.ts`, `ResynchronizationState.ts` | 6 |
| `SharedUtil.addGetParameter`/`addGetParameters` (URL query helpers) | `internal/SharedUtil.ts` | 5 |
| `Heartbeat` (periodic heartbeat POST, interval scheduling, terminate-on-lifecycle) | `internal/communication/Heartbeat.ts` | 4 |
| `Poller` (interval polling: sends `ui-poll` on the root node, terminate-on-lifecycle) | `internal/communication/Poller.ts` | 3 |
| `PollConfigurator` + `LoadingIndicatorConfigurator` (bind node-feature config → Poller / ConnectionIndicator) | `internal/communication/{PollConfigurator,LoadingIndicatorConfigurator}.ts` | 2 |
| `TreeChangeProcessor` (applies server UIDL changes to the state tree) + `ClientJsonCodec.decodeWithoutTypeInfo` + `CHANGE_*` constants | `internal/TreeChangeProcessor.ts` | 6 |
| `ReconnectConfiguration` (reconnect config from root node + reactive bind) + `XhrConnectionError` (XHR error data holder) | `internal/communication/{ReconnectConfiguration,XhrConnectionError}.ts` | 5 |
| `MessageSender` (UIDL send orchestrator: client-id, resync state machine, message queue, resend timer; XHR/push/handler contracted) | `internal/communication/MessageSender.ts` | 7 |
| `MessageOrdering` (MessageHandler's sync-id ordering kernel: server-id/resync extraction + pending-message queue) | `internal/communication/MessageOrdering.ts` | 6 |
| `XhrConnection` class (hybrid; UIDL XHR send + getUri + success/fail response routing to MessageHandler/ConnectionStateHandler) | `internal/XhrConnection.ts` | 5 |
| `ConnectionStateHandler` interface (heartbeat/xhr/push success-failure + reconnect-config contract) | `internal/communication/ConnectionStateHandler.ts` | type-only |
| `PushConfiguration` (push config from root node; drives MessageSender.setPushEnabled on push-mode change) | `internal/communication/PushConfiguration.ts` | 4 |
| `PushConnection` interface + `PushConnectionFactory` (extracted from MessageSender; deduped) | `internal/communication/PushConnection.ts` | type-only |
| `ConnectionMessageType` (DefaultConnectionStateHandler.Type kernel: heartbeat/push/xhr priority + isMessage) | `internal/communication/ConnectionMessageType.ts` | 3 |
| `Registry` (DI container mechanism: set/setResettable/get/has/reset, token-keyed) | `internal/Registry.ts` | 4 |
| `UILifecycle` (forward-only INITIALIZING→RUNNING→TERMINATED state machine + change events) | `internal/UILifecycle.ts` | 4 |
| `URIResolver` (+ VaadinUriResolver.resolveVaadinUri: context://, base:// + base-relative) | `internal/URIResolver.ts` | 6 |
| `ApplicationConfiguration` (per-UI bootstrap config holder, ~20 getters/setters) | `internal/ApplicationConfiguration.ts` | 3 |
| `ClientJsonCodec.decodeStateNode` (resolve `@v-node` element ref → state node; unblocks ExecuteJavaScriptProcessor) | `internal/ClientJsonCodec.ts` | 3 |
| `ClientJsonCodec.decodeWithTypeInfo` (full type-info decoder: `@v-node`/`@v-return`/`@v-fn`/object/array) | `internal/ClientJsonCodec.ts` | 6 |
| `ExecuteJavaScriptProcessor.invokeJavaScript` (manifest + apply server-JS expression with context/params + error handling) | `internal/ExecuteJavaScriptProcessor.ts` | 3 |
| `EagerDependencyTracker` (DependencyLoader's eager-load gate: count + run-when-loaded) | `internal/EagerDependencyTracker.ts` | 4 |
| `TrackingScheduler` (deferred-command scheduling + pending-work / idle tracking) | `internal/TrackingScheduler.ts` | 3 |
| `ResourceRegistry` (ResourceLoader's dedup + listener-fanout kernel: addListener/fireLoad/fireError/clearById) | `internal/ResourceRegistry.ts` | 4 |
| `ResourceLoader` class — script/inline-script/dynamic-import loaders + DOM init + clear (composes ResourceRegistry); stylesheet/HTML loaders deferred | `internal/ResourceLoader.ts` | 4 |
| `BrowserInfo` family probes (`isSafari`/`isSafariOrIOS`/`isOpera`/`isWebkit`) + `ResourceLoader` stylesheet loaders (`loadStylesheet`/`inlineStyleSheet` + head-comment insertion, Safari/Opera quirks) | `internal/BrowserInfo.ts`, `internal/ResourceLoader.ts` | 8 |
| `DependencyLoader` body (load deps by mode/type → ResourceLoader; eager-before-lazy; URI resolution) | `internal/DependencyLoader.ts` | 6 |
| `ExecuteJavaScriptElementUtils` initializer registry (`registerInitializer`/`disposeInitializer`/drain-on-unregister) | `internal/ExecuteJavaScriptElementUtils.ts` | 4 |
| `ExecuteJavaScriptElementUtils.registerUpdatableModelProperties` (store UpdatableModelProperties node data) | `internal/ExecuteJavaScriptElementUtils.ts` | 2 |
| `ExecuteJavaScriptElementUtils.populateModelProperties` + `PolymerUtils.getTag` (model-prop sync / set-null, retry-when-defined) | `internal/ExecuteJavaScriptElementUtils.ts`, `internal/PolymerUtils.ts` | 3 |
| `ExecuteJavaScriptElementUtils.attachExistingElement` (find existing child by tag, resolve/register id, report to server) — **ExecuteJavaScriptElementUtils complete** | `internal/ExecuteJavaScriptElementUtils.ts` | 2 |
| `ExecuteJavaScriptProcessor` body (`execute`/`handleInvocation` param decode + node-bound deferral + `invoke` via context callbacks) — **complete** | `internal/ExecuteJavaScriptProcessor.ts` | 3 |
| `SystemErrorHandler` class (handleError/handleErrorObject, isWebComponentMode, recreateWebComponents) | `internal/SystemErrorHandler.ts` | 4 |
| `MessageHandler` body (UIDL handle in sync-id order + apply: constants/changes/executeJs/deps/session-expired) — composes MessageOrdering/TreeChangeProcessor/etc. | `internal/MessageHandler.ts` | 5 |
| `ReconnectStateMachine` (DefaultConnectionStateHandler reconnect-decision core: handleRecoverableError/resolveTemporaryError/giveUp) | `internal/communication/ReconnectStateMachine.ts` | 5 |
| `FragmentedMessage` (AtmospherePushConnection websocket message-fragmentation kernel) | `internal/AtmospherePushConnection.ts` | 3 |
| `ApplicationConnection` engine API (start/isActive/poll/resolveUri/sendEventMessage/connectWebComponent/getUIId/debug) | `internal/ApplicationConnection.ts` | 6 |
| `Bootstrapper.populateApplicationConfiguration` (DOM bootstrap config → ApplicationConfiguration) | `internal/Bootstrapper.ts` | 2 |
| `DefaultConnectionStateHandler` full class (implements ConnectionStateHandler; composes ReconnectStateMachine + retry/handlers/online-offline) | `internal/communication/DefaultConnectionStateHandler.ts` | 6 |
| `DefaultRegistry` assembly (wires all ported services in dependency order + typed getters) — **cutover §3** | `internal/DefaultRegistry.ts` | 3 |
| `ApplicationConnection.create()` cutover entry (assemble registry + observe config + bind root node + publishClient) — **cutover §4 (non-destructive)** | `internal/ApplicationConnection.ts` | 1 |
| `AtmospherePushConnection` full `PushConnection` class (connect/state-machine/push-fragmentation/onMessage/disconnect + push-js load) + factory wired into DefaultRegistry | `internal/AtmospherePushConnection.ts` | 7 |
| `WidgetUtil.updateAttribute` (added to the existing hybrid; not registered — Java keeps its own) | `internal/WidgetUtil.ts` | +1 |
| `createModelTree` subsystem (StateNode/MapProperty → Polymer model tree + reactive change handlers + notification paths) | `internal/PolymerModelTree.ts` | 3 |
| Attach sub-deps: `PolymerUtils.addReadyListener`/`fireReadyEvent`/`getCustomElement` + `ReactUtils.isInitialized` (build-alongside; not registered) | `internal/PolymerUtils.ts`, `internal/ReactUtils.ts` | 4 |
| DOM abstraction: `DomApi` (`wrap`/`updateApiImplementation`/`isPolymerMicroLoaded`) + `DomNode`/`DomElement`/`DomTokenList` types + Polymer impl | `internal/dom/DomApi.ts` | 5 |
| `UpdatableModelProperties` (StateNode-stored set of server-updatable model props) | `internal/model/UpdatableModelProperties.ts` | 2 |
| `ExistingElementMap` (bidirectional server-node-id ↔ existing element map) | `internal/ExistingElementMap.ts` | 3 |
| `InitialPropertiesHandler` (queues/​resets new-node property updates vs server initial values) | `internal/InitialPropertiesHandler.ts` | 3 |

Two prep refactors also landed (green): `Computation` and `ReactiveEventRouter`
decoupled from inheritance (abstract → callbacks / composition) in the Java
before porting.

### TS contracts awaiting their real implementation
Declared in the modules above; satisfied by the real classes at cutover:
- `StateNode` → `StateTree` contract (`getNode`, `getFeatureDebugName`,
  `isActive`, `sendNodePropertySyncToServer`) — already satisfied by TS
  `StateTree`.
- `StateTree` → `Registry`, `ServerConnector`, `InitialPropertiesHandler`
  contracts, and an injected `ServerEventObjectAccess` (resync promise reject).
- `MapProperty`/node features → `MapPropertyTree`/`MapPropertyNode`/
  `NodeFeatureNode` — satisfied by TS `StateNode`/`NodeMap`.

### Translation notes / decisions
- GWT `JsMap`/`JsSet`/`JsArray` → native `Map`/`Set`/`Array`.
- `Class<?>`-keyed `StateNode.nodeData` → map keyed by JS constructor function
  (`object.constructor`; `getNodeData(clazz)`).
- GWT `elemental.util.Timer` → `setTimeout` (one-shot) / `setInterval`
  (repeating) (`Debouncer`).
- `elemental.json` JSON → loose `unknown` / `Record<string, unknown>`.
- Dev-only Java `assert`s are dropped in the TS ports.
- Tests are mocha (`@open-wc/testing`) under `src/test/frontend/*Tests.ts`; timer
  behavior tested with short real timeouts + `async`.

## Remaining work (ordered)

1. **Binding layer — COMPLETE.**
   - Pure-Java (build-alongside): `ServerEventHandlerBinder` **done**, `Binder`
     **done** (`internal/binding/Binder.ts` — strategy list
     [`SimpleElementBindingStrategy`, `TextBindingStrategy`], the `BinderContext`
     impl, and the `bind(stateNode, domNode)` entry point).
   - Hybrid (reconcile existing internals): `ServerEventObject` **done** — the
     existing partial internal now carries the whole class (the `$server`
     `get`/`getIfPresent` lifecycle, `defineMethod`, and `getEventData` with its
     event/Polymer-model expression evaluation), operating on the TS
     `StateNode`/`StateTree`/`ConstantPool` contracts and mocha-tested in
     isolation. The Java version stays live; the new exports are wired into
     `registerInternals` at cutover (a local `getConstantPool` slice on the
     Registry contract is satisfied by the real Registry then).
     Hybrid `SimpleElementBindingStrategy` (~1530 lines — the largest class) is
     **complete**: ported in 16 build-alongside slices into
     `internal/binding/SimpleElementBindingStrategy.ts`, ending with the `bind()`
     orchestrator and the `class SimpleElementBindingStrategy implements
     BindingStrategy<Element>`. Its supporting deps (`DomApi`,
     `UpdatableModelProperties`, `ExistingElementMap`, `InitialPropertiesHandler`,
     `PolymerModelTree.createModelTree`, the `PolymerUtils`/`ReactUtils` attach
     helpers) are ported too; `ApplicationConfiguration` is consumed only via a
     thin contract slice (satisfied by the real Registry at cutover). The
     per-slice node contracts are intentionally narrow and unified at the class
     by casting the concrete `StateNode`.
     - Slice 16 **done**: `bind()` + the strategy class (see above).
     - Slice 1 **done**: the self-contained DOM-event-data resolution helpers
       (`getOrCreateExpression`, `resolveFilters`, `resolveDebounces`) used by
       `handleDomEvent`, on top of the ported `Debouncer`. Note: the strategy
       class lives in `internal/binding/SimpleElementBindingStrategy.ts`; the
       window-registered Polymer model bridge stays in
       `internal/SimpleElementBindingStrategy.ts` (same basename, different dir)
       and is imported by the class at cutover.
     - Slice 15 **done**: visibility binding — `bindVisibility`/`updateVisibility`
       (records the bound state, hides invisible nodes, rebinds an initially
       invisible node once visible, re-applying on VISIBLE changes), wiring the
       slice-6 helpers + `remove`/`doBind`. **Only `bind()` itself + the class
       assembly remain** — every binding method is now ported. Next: write the
       `bind()` orchestrator and assemble `class SimpleElementBindingStrategy
       implements BindingStrategy<Element>` (casting the concrete StateNode to the
       per-slice contracts), wiring `bindPolymerModelProperties` (the
       window-registered helper) with the ported `handlePropertiesChanged`/
       `fireReadyEvent`/`handleListItemPropertyChange` callbacks.
     - Slice 14 **done**: virtual children / attach-existing — `bindVirtualChildren`,
       `appendVirtualChild` (in-memory / inject-by-id / inject-by-name /
       template-in-template), `doAppendVirtualChild`, `handleInjectId`,
       `handleTemplateInTemplate`, `verifyAttachRequest`, `verifyAttachedElement`,
       `getPayload`. Extends the tree contract with
       `getInitialPropertiesHandler`/`sendExistingElementWithIdAttachToServer` and
       the node with `hasFeature`. **All bind() dependencies are now ported** —
       remaining: `bindVisibility`/`updateVisibility` (wires slice-6 + remove/doBind),
       `bind()` itself, then assemble `class SimpleElementBindingStrategy
       implements BindingStrategy<Element>` (unifying the per-slice node contracts).
     - Attach sub-deps **done**: `PolymerUtils.addReadyListener`/`fireReadyEvent`
       (ready-listener registry), `PolymerUtils.getCustomElement`/
       `getChildIgnoringStyles` (custom-element-by-path), and
       `ReactUtils.isInitialized`. These unblock the virtual-children/attach
       machinery (`bindVirtualChildren`/`appendVirtualChild`/`doAppendVirtualChild`/
       `verifyAttach*`/`handleInjectId`/`handleTemplateInTemplate`), which is next.
     - Slice 13 **done**: bind lifecycle — `doBind` (re-fire dom-node-set +
       rebind), `scheduleInitialExecution` (deferred initial property update),
       `remove` (teardown: stop computations, remove listeners), and the
       `boundNodes` weak set. Remaining for bind(): the virtual-children/attach
       machinery (needs `ReactUtils.isInitialized`/`PolymerUtils.addReadyListener`/
       `getCustomElement`/`getChildIgnoringStyles`), `bindVisibility`/
       `updateVisibility`, and `bind()` itself — at which point the per-slice node
       contracts get unified and the class is assembled.
     - Slice 12 **done**: shadow-root binding — `bindShadowRoot`/`attachShadow`
       (attaches/reuses the open shadow root and binds the shadow node's children
       via the ported `bindChildren`; the `NativeFunction` shadow-attach becomes a
       direct `attachShadow` call). Remaining for bind(): virtual children/attach
       (needs `ReactUtils.isInitialized`, `PolymerUtils.addReadyListener`/
       `getCustomElement`/`getChildIgnoringStyles` ported first),
       `bindVisibility`/`updateVisibility`, `doBind`, `scheduleInitialExecution`,
       `remove`, `bind()` + class assembly.
     - Slice 11 **done**: light-DOM children binding — `bindChildren`,
       `handleChildrenSplice`, `addChildren`, `removeAllChildren`,
       `getFirstNodeMappedAsStateNode`, `getPreviousSibling` (creates/inserts or
       adopts child elements via the binder context + DomApi, syncing on splice).
       Uses the ported `ExistingElementMap`. `ElementUtil` is already a TS hybrid.
       Remaining: virtual children/attach (`bindVirtualChildren`/`appendVirtualChild`/
       `verifyAttach*`/`handleInjectId`/`handleTemplateInTemplate`), `bindShadowRoot`/
       `attachShadow`, `bindVisibility`/`updateVisibility`, `doBind`,
       `scheduleInitialExecution`, `remove`, `bind()` + class assembly.
     - Slice 10 **done**: element property binding — `updateProperty` (applies a
       map property to the element's JS property via `createModelTree`, guarding
       against clobbering a user edit with the previous DOM value; deletes/clears
       when unset). With this, all the `bindMap` property users
       (`updateStyleProperty`/`updateAttribute`/`updateProperty`) are ported.
     - `createModelTree` subsystem **done** (in `internal/PolymerModelTree.ts`,
       kept out of the registered `PolymerUtils.ts`): recursive StateNode/
       MapProperty → Polymer model-tree conversion with nodeId tagging, plus the
       reactive change handlers and notification-path resolution. This unblocks
       SES `updateProperty`.
     - Slice 9 **done**: Polymer model handlers — `InitialPropertyUpdate` (the
       deferred-initial-update holder), `handlePropertiesChanged`/
       `handlePropertyChange` (gated by the node's `UpdatableModelProperties`)
       and `handleListItemPropertyChange`. The `checkParent` assert is dropped
       (dev-only). Remaining: `updateProperty` (needs `createModelTree`),
       children/attach, shadow root, `bindVisibility`/`updateVisibility`,
       `doBind`, `scheduleInitialExecution`, `remove`, `bind()` + class assembly.
     - Slice 8 **done**: generic map/property binding — `bindMap`,
       `bindProperty`, `createComputations` (reactively binds a NodeMap's
       properties to a "property user" callback; existing properties applied
       eagerly, added ones on flush). The property users themselves are the
       slice-3/4 `updateStyleProperty`/`updateAttribute` and the not-yet-ported
       `updateProperty` (which needs `PolymerUtils.createModelTree`).
     - Slice 7 **done**: DOM event listeners — the `BindingContext` (node, DOM
       node, binder context, per-type listener bookkeeping),
       `bindDomEventListeners` and `handleDomEvent` (collecting event data,
       synchronized properties and mapped state nodes, then resolving
       filters/debounces and sending to the server), tying together the slice-1
       filter/debounce and slice-2 closest-node helpers. Remaining for the
       bind() core: `bindChildren`/`bindVirtualChildren`/attach, `bindShadowRoot`,
       `bindMap`/`bindProperty`/`createComputations`, `updateProperty` (+ the
       `PolymerUtils.createModelTree` model subsystem), the Polymer model
       handlers, `doBind`, `bindVisibility`/`updateVisibility`, and `bind()`
       itself — then assemble the `BindingStrategy<Element>` class.
     - Slice 6 **done**: visibility binding — `storeInitialHiddenAttribute`,
       `restoreInitialHiddenAttribute`, `setElementInvisible` and
       `applyStructuralAttributes` (the `slot`). The `bindVisibility`/
       `updateVisibility` wiring needs `BindingContext`/`remove`/`doBind` and
       lands with the bind() core.
     - Slice 5 **done**: creation & identity — `create` (element creation with
       node/parent namespace), `getTag`, `getNamespace`, `isApplicable`,
       `hasSameTag`, `needsRebind`, `isVisible`. These are the
       BindingContext-free leaf methods; they get assembled into the
       `BindingStrategy<Element>` class once `bind()` is ported.
     - Slice 4 **done**: the attribute binding (`updateAttribute`,
       `updateAttributeValue`), resolving a `uri` model object against the
       application configuration (web-component mode). This also added
       `WidgetUtil.updateAttribute` (ported on top of `DomApi`). The property
       binder (`updateProperty`) still waits on `PolymerUtils.createModelTree`;
       the `bind()`/`handleDomEvent` core still needs the `BindingContext` type
       and the remaining DOM-structure/visibility/event-listener methods.
     - Slice 3 **done**: the styling binding (`updateStyleProperty` and
       `bindClassList`), on native CSS and the ported `DomApi`.
     - Slice 2 **done**: the closest-state-node lookups
       (`getClosestStateNodeIdToEventTarget`, `getStateNodeForElement`,
       `getClosestStateNodeIdToDomNode`) that map an event target / a DOM node to
       the nearest bound state node id. DOM parents are walked with native
       `parentNode`/`isSameNode`; `DomApi`'s shadow-tree-aware wrapping (not
       ported yet) replaces the raw traversal at cutover.
2. **Communication layer** — `ServerConnector` **done**
   (`internal/communication/ServerConnector.ts` — builds the RPC messages as
   plain JS objects and enqueues them via the registry's `ServerRpcQueue`;
   satisfies `StateTree`'s `ServerConnector` contract). Added a shared
   `JsonConstants` (RPC subset) and `ClientJsonCodec.encodeWithoutTypeInfo`.
   `ServerRpcQueue` **done** (`internal/communication/ServerRpcQueue.ts` —
   accumulates invocations and deferred-flushes them via `MessageSender`).
   `LoadingIndicatorStateHandler` **done**
   (`internal/communication/LoadingIndicatorStateHandler.ts`).
   `RequestResponseTracker` **done** (single-active-request guard + the
   request/response lifecycle + reconnection-attempt events, GWT EventBus →
   per-type listener sets; `ResynchronizationState` extracted to its own module).
   `Heartbeat` **done** (`internal/communication/Heartbeat.ts` — interval
   scheduling, terminate-on-lifecycle, heartbeat POST). Remaining:
   `Poller` **done** (`internal/communication/Poller.ts`). `PollConfigurator` +
   `LoadingIndicatorConfigurator` **done** (bind the POLL_CONFIGURATION /
   LOADING_INDICATOR_CONFIGURATION node features to the poller / connection
   indicator). `TreeChangeProcessor` **done** (`internal/TreeChangeProcessor.ts` —
   applies server UIDL attach/detach/put/remove/splice/clear changes to the state
   tree; pure logic, tested end-to-end on a real StateTree; the core of what
   MessageHandler applies). `ReconnectConfiguration` + `XhrConnectionError`
   **done**. `MessageSender` **done** (`internal/communication/MessageSender.ts` —
   the UIDL send orchestrator: client-to-server message-id counter, the
   resynchronization state machine, the outgoing message queue + ack/dequeue
   logic, the resend timer, push enable/disable; XhrConnection / PushConnection
   (via an injected factory) / MessageHandler are contracted, and the logic is
   mocha-tested with fakes).
   `MessageHandler` **in progress**: its pure-logic kernel — the server-/sync-id
   message **ordering** (`getServerId`/`isResynchronize` + `PendingMessageQueue`
   with expected-id / next-handlable / drop-old logic) — is ported and tested as
   `internal/communication/MessageOrdering.ts`. The full MessageHandler is an
   851-line integration hub (UIDL parse → ConstantPool import →
   TreeChangeProcessor.processChanges → executeJs → dependency loading →
   session-expired/error handling) needing ~8 more contracts (DependencyLoader,
   ConstantPool, ExecuteJavaScriptProcessor, SystemErrorHandler, ResourceLoader,
   UILifecycle states, Profiler, StateTree.prepareForResync); it composes the
   MessageOrdering kernel and the 5 existing JSNI helpers, and its real
   validation is the cutover ITs.
   `XhrConnection` **done** (class folded into the hybrid
   `internal/XhrConnection.ts` alongside `resendRequest`: getUri via SharedUtil,
   XHR send, success/fail routing to MessageHandler / ConnectionStateHandler;
   routing + getUri mocha-tested, XHR round-trip integration-validated).
   The `ConnectionStateHandler` **interface** is ported
   (`internal/communication/ConnectionStateHandler.ts`, type-only) — consolidates
   the heartbeat/xhr/push success-failure + reconnect-config contract previously
   inlined across Heartbeat/XhrConnection/MessageSender.
   `PushConfiguration` **done** (`internal/communication/PushConfiguration.ts` —
   exposes the UI_PUSHCONFIGURATION settings and drives
   MessageSender.setPushEnabled on push-mode change, deferred to a flush
   listener; mocha-tested). The GWT event classes (`RequestStartingEvent`,
   `ResponseHandling{Started,Ended}Event`, `ReconnectionAttemptEvent`) are
   **subsumed** by RequestResponseTracker's per-type listener sets — no separate
   port needed.
   `PushConnection` interface + `PushConnectionFactory` **done** (extracted to
   `internal/communication/PushConnection.ts` as the canonical contracts;
   MessageSender's inlined copies removed and ConnectionStateHandler re-pointed —
   deduped; type-only). Remaining:
   the full `MessageHandler` body, `AtmospherePushConnection` (~800 LOC,
   Atmosphere/library-bound), the full `DefaultConnectionStateHandler` body
   (596-line reconnect state machine). `DefaultConnectionStateHandler` **in
   progress**: its `Type` kernel — `ConnectionMessageType` (heartbeat/push/xhr
   priority + isMessage, used to resolve competing recoverable errors) — is
   ported and tested (`internal/communication/ConnectionMessageType.ts`); the
   surrounding reconnect/dialog/error orchestration is an integration hub
   validated by the cutover ITs. NOTE: this network
   core is heavily interconnected and browser/network-bound — a poorer fit for
   the isolated-slice + mocha-fake rhythm; much of its real validation is the
   cutover ITs. Leaf pieces (done: LoadingIndicatorStateHandler;
   candidates: Heartbeat, Poller, the request lifecycle event classes) are still
   sliceable.
3. **`Registry`** and the engine `init()` / bootstrap. `UILifecycle` **done**
   (`internal/UILifecycle.ts` — the forward-only INITIALIZING→RUNNING→TERMINATED
   state machine + change events, GWT EventBus → listener set; mocha-tested).
   `URIResolver` **done** (`internal/URIResolver.ts` — folds in
   VaadinUriResolver.resolveVaadinUri: context:// → context root, base:// →
   base, other protocols pass through, plus base-relative location helpers;
   mocha-tested). `ApplicationConfiguration` **done**
   (`internal/ApplicationConfiguration.ts` — the per-UI bootstrap config holder,
   ~20 getters/setters; in Java a @JsType exported as
   window.Vaadin.Flow.internal.ApplicationConfiguration, populated by the
   bootstrap; mocha-tested). The Registry **container
   mechanism** is ported (`internal/Registry.ts` — token-keyed
   set/setResettable/get/has/reset, mocha-tested); the concrete typed getters
   (`getMessageSender`, …) and the registration of every service belong to the
   cutover subclass, once all services exist.
4. **Cutover** — point the bootstrap at the TS modules, delete the Java
   `flow-client` packages, and port remaining JVM tests (`*Test.java`,
   `Gwt*Test`) to mocha.

This is a multi-session effort: the binding + communication half is the bulk and
includes the codebase's largest classes. "Delete the Java" happens only at step
4, once consumers no longer reference the Java classes.

## CI / push mechanics (for the maintainer's tooling)
Push via the GitHub App (id 3960142) using `github.pem`; poll the "Flow
Validation" workflow for the pushed SHA. A `maven-metadata.xml`
"epilog non whitespace" build failure is a known runner-cache flake — re-run by
pushing again (the App lacks workflow re-run permission).
