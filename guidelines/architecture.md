# Architecture

How the core pieces of Flow fit together, and what to keep in mind when
touching them.

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

Always pass values to `executeJs()` as parameters, never concatenated into the
expression string. See [Browser Integration](browser-integration.md) for the
full rules on calling into the browser.

Push uses a WebSocket-based connection (`PushConnection`,
`AtmospherePushConnection`).

## Routing

`Router` and `RouteConfiguration` implement navigation, with a lifecycle of
before-leave, before-enter and after-navigation observers. Route resolution is
driven by `@Route` annotations, discovered at startup or registered
programmatically.

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
