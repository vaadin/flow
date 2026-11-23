# Architecture

How the core pieces of Flow fit together, and what to keep in mind when
touching them.

The more involved features have their own architecture notes next to the code:

| Feature                               | Note                                                         |
| ------------------------------------- | ------------------------------------------------------------ |
| Client-side bootstrap and routing     | [`flow-client/ARCHITECTURE.md`](../flow-client/ARCHITECTURE.md) |
| Connection state and loading indicator | [`flow-client/ARCHITECTURE.md`](../flow-client/ARCHITECTURE.md#connection-state-and-loading-indicator) |
| React component adapter               | [`flow-react/ARCHITECTURE.md`](../flow-react/ARCHITECTURE.md) |
| `@PWA` and the offline service worker | [`vite-plugin-service-worker/ARCHITECTURE.md`](../flow-build-tools/src/main/resources/plugins/vite-plugin-service-worker/ARCHITECTURE.md) |
| Translations for Hilla views          | [`rollup-plugin-vaadin-i18n/ARCHITECTURE.md`](../flow-build-tools/src/main/resources/plugins/rollup-plugin-vaadin-i18n/ARCHITECTURE.md) |
| Spring Security for client-side views | [`vaadin-spring/.../security/ARCHITECTURE.md`](../vaadin-spring/src/main/java/com/vaadin/flow/spring/security/ARCHITECTURE.md) |
| Stateless authentication with JWT     | [`vaadin-spring/.../security/stateless/ARCHITECTURE.md`](../vaadin-spring/src/main/java/com/vaadin/flow/spring/security/stateless/ARCHITECTURE.md) |
| npm dependency pinning                | [`flow-build-tools/ARCHITECTURE.md`](../flow-build-tools/ARCHITECTURE.md#npm-dependency-pinning) |
| Tailwind CSS                          | [`flow-build-tools/ARCHITECTURE.md`](../flow-build-tools/ARCHITECTURE.md#tailwind-css) |
| `vaadin-dev` dev loop daemon          | [`flow-devloop-daemon/README.md`](../flow-devloop-daemon/README.md), and the [Dev Loop](dev-loop.md) chapter |

## State management

Flow keeps the server-side representation of the UI in a tree and synchronizes
changes to the client automatically:

- `StateNode` — the state of a single component or element on the server.
- `StateTree` — the whole application state tree for one UI.
- `NodeFeature` — an aspect of a node's state (properties, children, listeners,
  …). A node carries only the features it needs.

Changes made to the tree during a request are collected and sent to the client
at the end of the request. Nothing needs to be pushed manually.

## DOM abstraction

`Element` and `Node` are the server-side mirror of the browser DOM. Components
build on top of them:

- `Element` for low-level DOM manipulation.
- `@Synchronize` to synchronize a client-side property back to the server.
- `@DomEvent` to map a DOM event to a server-side component event.
- `@ClientCallable` to expose a server-side method to client-side code.

An element's `nodeId` is not a stable identifier — it is `-1` until the node is
attached. Use an attachment-independent identifier (e.g. a UUID) when you need
one.

## Client-server communication

`JacksonCodec` handles serialization between Java and JavaScript:

- Parameters: Java objects → JSON → JavaScript variables (`$0`, `$1`, …).
- Return values: JavaScript objects → JSON → Java records or beans.
- `Element` instances are sent as DOM references (or `null`).
- Arbitrary objects are supported via Jackson serialization.

Values that would otherwise be sent again and again — DOM event settings
shared by many state nodes, and what each `executeJs` invocation runs — are
sent once through the shared constant pool and referenced by id.

Always pass values to `executeJs()` as parameters, never concatenated into the
expression string. See [Browser Integration](browser-integration.md) for the
full rules on calling into the browser, and [Wire Protocol](wire-protocol.md)
for what the response actually contains and what that means for new code.

Push uses a WebSocket-based connection (`PushConnection`,
`AtmospherePushConnection`).

## Routing

`Router` and `RouteConfiguration` implement navigation, with a lifecycle of
before-leave, before-enter and after-navigation observers. Route resolution is
driven by `@Route` annotations, discovered at startup or registered
programmatically.

In the browser, the router of the application (React Router by default) owns
the URL, and server-side routes are its catch-all fallback. See
[Client-side bootstrap and routing](../flow-client/ARCHITECTURE.md).

## Instantiation and lookup

`Instantiator` creates the objects Flow needs (views, listeners, converters) so
that a DI container can take over. `Lookup` resolves the SPI implementations
available in the current environment. Prefer going through these rather than
constructing implementations directly.

## Frontend build

- Vite-based development mode with hot reload.
- Production bundling, with a pre-built default bundle when the application
  does not need a custom one.
- TypeScript support with generated type definitions.
- Lit and React template support.
- Theme system built on CSS custom properties.

## Accessing the UI

Prefer reaching the UI through the component (`getUI()`) or through an event
that provides it (e.g. `AttachEvent`). `UI.getCurrent()` is a last resort for
code that has no component instance available.

## Session lock and current instances

The rules are in the Session Lock and Current Instances section of
[`CONVENTIONS.md`](../CONVENTIONS.md#session-lock-and-current-instances).

`UI.getCurrent()` is a promise to the code that reads it: the UI is safe to
touch from this thread right now. Application code relies on that promise
without checking it, and so does the framework — a signal effect, for example,
treats a signal change made on a thread where its UI is current as a change
made inside that UI's locked work, and handles it without going through
`UI.access`. A current UI without
the lock breaks that promise silently: the code mutates the state tree
concurrently with the thread that does hold the lock, and nothing fails until
the data is corrupted.

The shape to follow is the one of `UI.access` and `VaadinSession.access`: lock
the session, set the current instances with `CurrentInstance.setCurrent`, run
the work, and restore the previous instances with
`CurrentInstance.restoreInstances` in a `finally` block. Restore them before
`unlock()` in new code; `accessSynchronously` restores right after it, which
is safe only because `unlock()` runs nothing but queued access tasks, and each
of those sets its own current instances under the lock. Request handlers that set the current UI while they hold the lock — finding
the UI of a UIDL or heartbeat request, bootstrapping a page — have to restore
it the same way when they unlock, because everything after that point in the
request (writing the response, the request end callbacks) runs without the
lock. Some handlers do not restore it yet, so the request end callbacks can
still see a current UI today; that is a bug to fix, not behaviour to rely on.

The current `VaadinSession` is a weaker promise. It is set for the whole
request, including phases that run without the lock, such as download and
upload handlers that transfer data while other requests use the session. Code
that gets a session from `VaadinSession.getCurrent()` or from a parameter
locks it, or goes through `access`, before it touches the session's state.

Whether a callback runs with the lock is part of its contract, and a contract
that depends on the request is no contract at all. A request end callback also
fires for requests that never used a session, so taking the lock only to fire
it would add lock contention to every request, and it would still have no lock
to take for the sessionless ones. Such a callback runs without the lock, and
code in it that needs the UI or the session uses `access`.
