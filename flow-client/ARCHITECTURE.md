# Client-side bootstrap, routing and connection state

## Overview

A Flow application today is a single-page application whose router runs in
the browser. The server-side views are one kind of route in that router, next
to any client-side views the application has (for example, Hilla React views).

This is what happens when a user opens `/orders`:

1. `IndexHtmlRequestHandler` answers with the application shell, `index.html`.
   That is an empty `#outlet` plus the Vite bundle.
2. The bundle starts React Router with the routes in `routes.tsx`. If the
   application has no client views, the only route there is Flow's catch-all,
   `serverSideRoutes`, which renders the `Flow` component from `Flow.tsx`.
3. When it renders for the first time, the `Flow` component asks the `Flow`
   class in `Flow.ts` to start Flow. That class creates the server-side `UI`
   with a `?v-r=init` request, loads the client engine (`FlowClient.js`), and
   sends a `ui-navigate` event with the path.
4. The server runs its router as usual (`BeforeEnterObserver`s, the
   `@Route`, layouts), renders the view into the UI's container element
   (`<flow-container-…>`), and confirms with `serverConnected`. The `Flow`
   component then moves the container into the outlet.

From then on, React Router owns the URL. Every location change goes through
it. When the new location is a server route, `Flow` makes a round trip to the
server. When the user leaves a server view for a client view, `Flow` first
asks the server whether the view may be left.

The server-side half of the story (`UI`, `Router`, the navigation
lifecycle) is described under Routing in
[`guidelines/architecture.md`](../guidelines/architecture.md#routing). This
note covers the browser side and how the two halves talk to each other.

## Design goals

- **One router, one history.** The browser router owns the address bar and
  `history`. In React mode the server never pushes history entries behind the
  router's back. Instead, `UI.navigate` and `History.pushState` ask the router
  to navigate (see [Navigating](#navigating)). In vaadin-router mode the
  server still writes `history` itself.
- **Server views are a fallback route.** Client routes are matched first, and
  everything they don't match goes to Flow. A pure Flow application and a
  hybrid one use the same mechanism. The only difference is which
  `routes.tsx` is generated.
- **The server keeps its veto.** The router's navigation guards
  (`useBlocker` in React Router, `onBeforeEnter` / `onBeforeLeave` in
  vaadin-router) are mapped onto Flow's `BeforeEnter` / `BeforeLeave`
  lifecycle, including postponing.
- **Lazy server start.** No UI is created until a server route is actually
  rendered. A visit that only touches client views does not create a `UI`.
  The application can opt into an eager start instead
  ([Eager server load](#eager-server-load)).
- **One engine for both routers.** `Flow.ts` speaks the route contract of
  vaadin-router (`action`, `onBeforeEnter`, `onBeforeLeave`). `Flow.tsx`
  adapts React Router to that contract, so the engine has no React
  dependency.
- **No surprises from infrastructure.** The bootstrap request has to pass
  web application firewalls, CSP nonces, context paths and service workers.

## The pieces

| Piece | Where | Role |
| ----- | ----- | ---- |
| `index.html` | `flow-server/src/main/resources/com/vaadin/flow/server/frontend/` | Default shell: `#outlet` and the iOS standalone height fix. The entry script is injected by Vite. |
| `index-react.tsx` / `index.ts` | same | Default entry point. In React mode (the default), it renders `RouterProvider` with the router from `routes.tsx`. In vaadin-router mode (`reactEnable=false`), it sets up `@vaadin/router` with `...serverSideRoutes` as the last route. |
| `routes-flow.tsx`, `routes.tsx` | same | Default `routes.tsx`. `routes-flow.tsx` is only `serverSideRoutes`. The Hilla variant uses `RouterConfigurationBuilder` with `withFallback(Flow)`. |
| `vaadin-react.tsx` | same | Publishes the routes as `window.Vaadin.routesConfig` and registers the global click handler. |
| `Flow.tsx` | same | The `Flow` React component, `serverSideRoutes`, queued navigation, the portals that host React adapters inside server views, and the web component helpers `reactElement` / `loadComponentScript`. |
| `Flow.ts` | `flow-client/src/main/frontend/` | `Flow` class: lazy UI creation, loading of the engine, the vaadin-router-style route (`action`, `onBeforeEnter`, `onBeforeLeave`), the offline stub, and the connection indicator. |
| `FlowBootstrap.js`, `FlowClient.js` | same | The bootstrap that registers the application from the `?v-r=init` response, and the client engine (state tree, UIDL, push). |
| `IndexHtmlRequestHandler` | `flow-server/.../server/communication/` | Serves `index.html`, adds the base href, `lang`, feature flags, app shell settings, the dev tools, the Spring CSRF meta tags, and optionally the initial UIDL. |
| `JavaScriptBootstrapHandler` | same | Handles `?v-r=init`: creates the `UI` and returns the application config as JSON. |
| `UI.browserNavigate`, `UI.leaveNavigation` | `flow-server/.../component/UI.java` | Server side of the `ui-navigate` and `ui-leave-navigation` events. They answer through `serverConnected(cancel)` / `serverPaused()` on the container element. |
| `TaskGenerateReactFiles`, `TaskGenerateIndexTs` | `flow-build-tools/.../server/frontend/` | Write the templates above into `src/main/frontend/generated/`. They check that a custom `routes.tsx` still includes the server routes, and write `layouts.json`. |

`Flow.ts` and the engine reach the application through `jar-resources`
(`Frontend/generated/jar-resources/Flow.js`). `Flow.tsx` and the other
templates are copied into `src/main/frontend/generated/flow/` on every build,
so the application always runs the version that matches its Flow jar. An
application can replace `index.html`, `index.tsx` and `routes.tsx` with its
own copies in `src/main/frontend/`, but not `Flow.tsx`.

## Page load, step by step

1. **Shell.** `IndexHtmlRequestHandler` reads `index.html` (cached in
   production mode) and decorates it. No `UI` is created here unless the
   initial UIDL is included (see
   [Eager server load](#eager-server-load)).
2. **Client router.** `index.tsx` renders the router. React Router matches the
   path against `routes.tsx`. For a server route, `/*` matches the `Flow`
   component.
3. **UI creation.** The `Flow` component calls the route's `action`, which
   `flowInit()` serves:
   - `flowInitUi()` uses `window.Vaadin.TypeScript.initial` when the server
     inlined it. Otherwise it sends
     `GET ?v-r=init&location=…&query=…&<browser details>`. The browser
     details are separate query parameters, not one JSON value, because
     percent-encoded JSON in the URL is blocked by some web application
     firewalls.
   - `JavaScriptBootstrapHandler` validates the location (400 for invalid
     paths), creates the `UI`, and returns `appConfig` (and the push script
     URL, if `@Push` is used).
4. **Container and engine.** `Flow.ts` creates `<flow-container-<appid>>`
   before calling `FlowBootstrap.init`, because the initial UIDL may already
   refer to it. It runs the generated imports (`generated-flow-imports.js`,
   the `@JsModule`s), then imports `vaadin-flow-client`, and polls until the
   engine is idle. The container is attached to `<body>`, hidden, right away:
   components created from the server may expect to be in the DOM before the
   router puts the container in place.
5. **Navigation.** The route's `onBeforeEnter` sends `ui-navigate` with the
   route, the query, the app shell title, the history state and the trigger
   (`link`, `client` or `history`, from the click listener in
   `loadingFinished`). On the first navigation, `UI.browserNavigate` renders
   the route (unless it was rendered eagerly) and installs the history
   handler. Later navigations go through that handler.
6. **Ack.** The server calls `serverConnected(cancel)` on the container.
   `Flow.ts` resolves the guard, and `Flow.tsx` moves the container into the
   outlet and fires `vaadin-navigated`.

### Eager server load

With `eagerServerLoad`, or a custom `BootstrapInitialPredicate`,
`IndexHtmlRequestHandler` creates the UI and renders the route while serving
`index.html`. It inlines the response as `window.Vaadin.TypeScript.initial`
and puts the container into `#outlet` already. This saves one round trip on
the first view, but every page load then creates a UI, including the ones that
end on a client view.

## Navigating

Every location change goes through the router, and the `Flow` component
decides from the target what has to happen:

| From → to | What happens |
| --------- | ------------ |
| client → server | `/*` matches. `Flow` mounts and runs the page load flow above (or only `ui-navigate`, if the UI exists). |
| server → server | `useBlocker` blocks the transition, and `onBeforeEnter` sends `ui-navigate`. The server can continue, prevent, redirect or postpone. The blocker is proceeded or reset to match. |
| server → client | `onBeforeLeave` sends `ui-leave-navigation`. The server navigates to a `ClientViewPlaceholder`, so `BeforeLeaveObserver`s run, and then acks or cancels. A postponed leave keeps the blocker until `serverConnected` arrives. |
| server-initiated | In React mode, `UI.navigate` and `History.pushState` / `replaceState` dispatch a `vaadin-navigate` window event (`HistoryJs.navigatePushing` / `navigateReplacing`). `Flow.tsx` turns it into a router navigation through `useQueuedNavigate`. `callback: false` means the server already rendered the view, so no round trip follows. |
| link click | `Flow.tsx` catches clicks on same-origin `<a>` elements (except those with `router-ignore`, `download` or a target) and navigates with the router. Before any `Flow` component has mounted, `registerGlobalClickHandler` from `vaadin-react.tsx` does the same with `pushState` and a `popstate` event. |

`useQueuedNavigate` exists because React Router updates history
asynchronously. Several `navigate()` calls in one task (a redirect inside a
navigation, `UI.navigate` from a listener) used to overwrite each other or
append paths (#19985, #20446, #20673, #22021, #25910). The queue runs one
navigation at a time, after the current one has finished its round trip.

## Layouts and mixed views

- **Flow layout around client views.** `@Layout` classes are written to
  `generated/layouts.json` by `TaskGenerateReactFiles` (and by the route
  hotswapper at runtime). Hilla's file router reads it and puts the matching
  client routes under a `Flow` layout route (`flowLayout`). See
  `packages/ts/file-router/ARCHITECTURE.md` in the Hilla repository.
- **Client views inside a Flow layout.** The server renders the layout, and
  `ReactRouterOutlet` (a React adapter in `flow-react`) renders React Router's
  `<Outlet/>` inside it.
- **React inside server views.** React adapter elements in the container
  render through `flow-portal-add` events into the `Flow` component's own
  React tree, so they share React Router and context with the rest of the
  application. See [`flow-react/ARCHITECTURE.md`](../flow-react/ARCHITECTURE.md).

## Decisions and gotchas

- **The blocker can go stale.** Round trips resolve the blocker
  asynchronously, and by then React Router may have reset it, for example
  after a queued navigation completed. Always resolve the *current* blocker
  through `blockerRef` (`proceedBlocker` / `resetBlocker`), never the one
  captured by the effect. Otherwise React Router throws "Invalid blocker state
  transition" (#25861).
- **Recognising server routes.** `Flow.tsx` decides "server → server" by
  matching the target against `window.Vaadin.routesConfig` and checking
  whether the matched element's type is `Flow`. The routes go through
  `window` rather than an import, so that `Flow.tsx` does not import the
  application's `routes.tsx`, which imports `Flow.tsx` (#19666).
  `Flow.type = 'FlowContainer'` is set for Copilot.
- **Portal removal is registered synchronously.** The `flow-portal-remove`
  listener is added when the portal is added, not in an effect, and it only
  reacts to its own element. Otherwise a detach and re-attach in the same task
  produces a duplicate portal, and a nested adapter's removal drops its parent
  (#20770, #24661).
- **HMR reloads the page.** `Flow.tsx` imports `generated-flow-imports.js`,
  so any change in an `@JsModule` hot-updates `Flow.tsx`. Those modules can't
  be swapped in place, so the module accepts the update by reloading.
- **Bare `vaadin-flow-client` import.** The Vite config aliases it to
  `FlowClient.js` and pre-bundles it. A relative import would make the dev
  server serve the engine's ~90 modules one by one.
- **TestBench hooks.** `window.Vaadin.Flow.clients.TypeScript.isActive()` is
  `true` while a server navigation is in progress, so TestBench waits for it
  like it waits for the engine.
- **`@PreserveOnRefresh` needs `window.name`.** It is set in the `Flow`
  constructor, before anything can navigate.
- **Offline.** When the connection is down or `?v-r=init` fails, the route
  renders the offline stub (`offline-stub.html` in an iframe) instead of the
  container, and re-renders when the connection comes back. See
  [Connection state and loading indicator](#connection-state-and-loading-indicator)
  and, for the service worker, the
  [`@PWA` note](../flow-build-tools/src/main/resources/plugins/vite-plugin-service-worker/ARCHITECTURE.md).

## Working on it

- `Flow.tsx` and the other templates are plain resources that are only
  compiled as part of an application's build. Try a change in one of the test
  modules below, not only in the unit tests.
- Unit tests for `Flow.ts`: `flow-client/src/test/frontend/FlowTests.ts`
  (`npm test` in `flow-client`).
- Integration tests: the routing ITs in `flow-tests/test-default`
  (`com.vaadin.flow.test.routing`: `NavigationIT`, `BackNavIT`, `StateIT` and
  others, under React Router), `flow-tests/test-vaadin-router`
  (vaadin-router mode), `flow-tests/test-ccdm` and
  `flow-tests/test-ccdm-flow-navigation` (bootstrap and navigation with
  client views), `flow-tests/test-react-adapter` (portals).
- A change to the `ui-navigate` / `ui-leave-navigation` / `serverConnected`
  contract changes both `UI.java` and `Flow.ts` / `Flow.tsx`. Update them in
  the same change.
- Related: [Browser Integration](../guidelines/browser-integration.md),
  [Wire Protocol](../guidelines/wire-protocol.md),
  [Frontend Hotswap](../guidelines/frontend-hotswap.md) (for `layouts.json`).

## Connection state and loading indicator

### Overview

The thin bar at the top of a Vaadin application and the "Connection lost"
popup are one element, `<vaadin-connection-indicator>`. It shows one shared
value: the connection state of the browser tab. Several parties write that
value, and none of them know about each other:

- the Flow router in `Flow.ts`, while a server navigation is in progress;
- the Flow engine, while a UIDL request is in flight, and while it
  reconnects;
- Hilla's `ConnectClient`, while an endpoint call is in flight;
- the service worker, which may know that the server was unreachable before
  the page even loaded.

When the user clicks a button, the engine announces loading. If the response
takes longer than `firstDelay`, the bar appears, and it gets more prominent
after `secondDelay` and `thirdDelay`. If the request fails, the engine
switches to reconnecting and retries. It ends in connected again, or in
connection lost once it gives up.

### Design goals

- **One state for the whole tab.** Flow and Hilla requests show the same
  indicator, and an offline page looks the same in a Flow and a Hilla view.
- **Loading is counted, not set.** Each party starts and finishes its own
  loading. The indicator goes back to connected only when every party is done,
  so a finishing endpoint call doesn't hide the bar while a UIDL request is
  still running.
- **No noise from high-frequency events.** A request triggered only by
  `mousemove`, `keydown`, `input`, `scroll` and similar events does not show
  loading. These requests are frequent and short, and a flickering bar would
  only distract.
- **Configurable from Java**, without custom client code: the delays and the
  default theme (`UI.getLoadingIndicatorConfiguration()`), and the reconnect
  texts, attempts and interval (`UI.getReconnectDialogConfiguration()`).

### The pieces

| Piece | Where | Role |
| ----- | ----- | ---- |
| `ConnectionStateStore`, `ConnectionState` | `flow-client/src/main/frontend/ConnectionState.ts` | The shared store at `window.Vaadin.connectionState`. Created on first import, starting as connected (always on localhost) or as connection lost when `navigator.onLine` is false. On a service worker message saying the server was unreachable, it switches to connection lost. |
| `ConnectionIndicator` (element) | `flow-client/src/main/frontend/ConnectionIndicator.ts` | `<vaadin-connection-indicator>`, created when the module loads, which the engine bootstrap (`Bootstrapper.ts`) and `Flow.ts` both import. Renders the loading bar with the three delays, and the reconnecting and offline messages. Exposed as `window.Vaadin.connectionIndicator`. |
| `internal/client/ConnectionIndicator.ts` | `flow-client/src/main/frontend/` | The engine's access to the store and the element. Every call is a no-op when they are missing. |
| `LoadingIndicatorStateHandler` | `internal/client/communication/` | Decides when a UIDL request announces loading. |
| `LoadingIndicatorConfigurator` | same | Binds the `LOADING_INDICATOR_CONFIGURATION` node feature of the UI to the element properties. |
| `DefaultConnectionStateHandler`, `ReconnectStateMachine`, `ReconnectConfiguration` | same | Reconnect after XHR, push or heartbeat errors, and the `online` / `offline` events, once the engine has loaded. |
| `Flow.ts` | `flow-client/src/main/frontend/` | Announces loading for server navigations. Before the engine has loaded, it handles `online` / `offline` itself and shows the offline stub. |
| `sw.ts` | `flow-server/.../server/frontend/` | Answers `Vaadin.ServiceWorker.isConnectionLost` from the outcome of its last network request. |

### Loading

The store has a counter. `loadingStarted()` sets the state to loading and adds
one. `loadingFinished()` and `loadingFailed()` subtract one, and when the
count reaches zero, they set connected or connection lost. Any direct state
change (`state = …`) resets the counter to zero.

In the engine, the decision is made per request:

1. `ServerConnector` passes each queued RPC to
   `LoadingIndicatorStateHandler.processMessage`. Any message that is not an
   event from the silent list marks the next request as one that shows
   loading.
2. When `MessageSender` sends the request, `startLoading()` announces it,
   unless every message in it was silent.
3. When `MessageHandler` has processed the response, `stopLoading()` finishes
   it. This happens only when no other request is active, and is deferred
   through the `TrackingScheduler`, so a follow-up request keeps the bar
   without a flicker. TestBench sees the deferred update as activity.

The handler always uses `loadingStarted` / `loadingFinished`, never sets the
state directly, so it does not cancel what the router or Hilla announced. It
remembers whether its own loading is still counted (`#announced`). A state
change by anyone, such as reconnecting, clears that, so the handler never
finishes a loading that the store has already dropped.

### Reconnecting

Once the engine has loaded, `DefaultConnectionStateHandler` owns
reconnection:

- A recoverable XHR, push or heartbeat error goes to `ReconnectStateMachine`.
  The machine keeps the highest-priority cause, sets reconnecting, and counts
  attempts. The first attempt runs immediately, and the rest run every
  `reconnectInterval`. An attempt re-sends the queued messages or sends a
  heartbeat.
- After `reconnectAttempts`, the machine gives up: connection lost, and
  heartbeats stop.
- A successful response resolves the error. `restoreLoading()` then
  re-announces a loading that was still ongoing (#26452), or shows connected
  (#26308).
- The `offline` event gives up at once. The `online` event resumes heartbeats,
  and the next heartbeat decides.
- A 403 heartbeat means the session expired, and a 404 means the UI is gone.
  Neither triggers a reconnect.

Before the engine has loaded, `Flow.ts` handles `online` with a `HEAD sw.js`
request (any HTTP answer counts as reachable) and `offline` by setting
connection lost. A server route shows the offline stub until the state is
online again.

### Decisions and gotchas

- **Silent events are a fixed list** in `LoadingIndicatorStateHandler`. The
  decision is per request, not per message: one non-silent message is enough
  for the whole request to show loading (#24230).
- **Don't set the state to connected to end loading.** It would also end
  everyone else's loading. Use `loadingFinished()` or `loadingFailed()`.
- **Others reach the store and the element through `window.Vaadin`**, not
  through imports: Hilla and the engine read `window.Vaadin.connectionState`.
  The modules create them only when they are not there yet, so loading a
  module twice keeps the first instance.
- **The element uses standard decorators** (`accessor` fields), so the
  `flow-client` tsconfig must not enable `experimentalDecorators`, and the
  test runner lowers them with an explicit esbuild target.
- **The element's texts come from `ReconnectDialogConfiguration`**, applied in
  `DefaultConnectionStateHandler.configurationUpdated()`. The name is
  historical: there is no reconnect dialog any more, the indicator took its
  place (#9481).

### Working on it

- Unit tests: `flow-client/src/test/frontend/internal/client/communication/`
  (`LoadingIndicatorStateHandlerTests.ts`, `ReconnectStateMachineTests.ts`,
  `DefaultConnectionStateHandlerTests.ts` and others), and
  `ConnectionStateTests.ts` and `ConnectionIndicatorTests.ts` for the store
  and the element.
- Integration tests: `LoadingIndicatorIT` in `flow-tests/test-default`,
  `ConnectionIndicatorIT` and `LoadingIndicatorNavigationIT` in
  `flow-tests/test-ccdm-flow-navigation`.
