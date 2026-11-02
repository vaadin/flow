# React component adapter

## Overview

The adapter lets a Flow application use a React component as if it were a
Java component. It has two halves that come in a pair:

- a **Java class** that extends `ReactAdapterComponent`. It has a `@Tag` and a
  `@JsModule`, and gives the component its Java API;
- a **web component** that extends `ReactAdapterElement`, in the module named
  by `@JsModule`. It renders the React component in `render(hooks)`.

The test application has the smallest example, a text input
(`flow-tests/test-react-adapter`):

```tsx
class ReactInput extends ReactAdapterElement {
  protected override render(hooks: RenderHooks) {
    const [value, setValue] = hooks.useState<string>('value');
    return <input value={value} onChange={(e) => setValue(e.target.value)} />;
  }
}
customElements.define('react-input', ReactInput);
```

```java
@Tag("react-input") @JsModule("./ReactInput.tsx")
public class ReactInput extends ReactAdapterComponent {
    public void setValue(String value) { setState("value", value); }
    public String getValue() { return getState("value", String.class); }
    public void addValueChangeListener(SerializableConsumer<String> l) {
        addStateChangeListener("value", String.class, l);
    }
}
```

The two halves talk only through standard Flow element features: a named
**state** is an element property, a change from the browser is a
`<name>-changed` DOM event, and server-side children go into named
**content** slots. There is no protocol of its own.

## Design goals

- **No new wire protocol.** State goes over element properties and DOM
  events, so it is serialized with Jackson like any other property and
  follows the same rules for synchronization and security.
- **React components stay React.** `render()` is a real React render
  function, so hooks, context and libraries work as usual. The adapter adds
  three hooks and nothing else.
- **One React tree per page.** Inside a React Router application, every
  adapter renders into the tree of the `Flow` route component through a
  portal. So it shares React Router and any context providers with the rest
  of the application, which is what makes `ReactRouterOutlet` possible.
- **Works without React Router, too.** When no `Flow` component claims the
  element (vaadin-router mode, exported web components), it creates its own
  React root.
- **Flow children inside React.** A server-side component can be placed into
  a slot in the React output (`useContent` / `getContentElement`).

## The pieces

| Piece | Where | Role |
| ----- | ----- | ---- |
| `ReactAdapterComponent` | `flow-react/src/main/java/com/vaadin/flow/component/react/` | Java base class: `setState` / `getState` / `addStateChangeListener`, `getContentElement`, Jackson conversion helpers. |
| `ReactAdapter.template` | `flow-react/src/main/resources/com/vaadin/flow/server/frontend/` | Source of `ReactAdapterElement` and `RenderHooks`. Written to `src/main/frontend/generated/flow/ReactAdapter.tsx` by `TaskGenerateReactFiles`, with `{{VAADIN_VERSION}}` replaced for usage statistics. |
| `ReactRouterOutlet` + `ReactRouterOutletElement.tsx` | `flow-react` | An adapter that renders React Router's `<Outlet/>`, so a Flow `@Layout` can host client-side views. |
| `Flow.tsx` | `flow-server/.../server/frontend/` | Handles `flow-portal-add` / `flow-portal-remove` and renders the portals. See [`flow-client/ARCHITECTURE.md`](../flow-client/ARCHITECTURE.md). |
| `SimpleElementBindingStrategy`, `ReactUtils.ts` | `flow-client/src/main/frontend/internal/client/` | Bind `INJECT_BY_NAME` virtual children, waiting for the adapter's ready callback. |
| `NodeUpdater`, `FrontendBuildUtils.isReactModuleAvailable` | `flow-build-tools/.../server/frontend/` | Add React and React Router whenever React is enabled. The React flavour of the Vaadin component versions (`versions.json`, `mode: react`) is pinned only when `flow-react` is on the classpath. |

`ReactAdapter.tsx` is only written when the `flow-react` jar is on the
classpath, because `TaskGenerateReactFiles` looks the template up there.

## State

A state is an element property with the same name.

- **Server → browser.** `setState(name, value)` writes the value with Jackson
  into the element property (`setPropertyJson`). On the element,
  `useState(name)` has replaced the property with a getter and setter on its
  first render. The setter stores the value and dispatches to a `useReducer`
  in the wrapper component, which re-renders.
- **Browser → server.** The `set` function returned by `useState` stores the
  value, fires `<name>-changed`, and re-renders. On the server,
  `addStateChangeListener(name, type, listener)` is
  `addPropertyChangeListener(name, name + "-changed", …)`. That both
  synchronizes the property on that event and decodes the value into `type`.
- **Initial value.** The first `useState(name, initial)` call takes the value
  the property already has (set from Java before the element rendered), and
  falls back to `initial` only when there is none.

`useState` is called during render, like a React hook, and keeps one setter
per name. The state lives on the element, not in React, so it survives
re-renders and portal moves.

## Events

`useCustomEvent(type, options)` returns a function that dispatches a
`CustomEvent` on the element. On the server, it is an ordinary DOM event:
`getElement().addEventListener(type, …)`, or a `@DomEvent` component event.

## Content slots

`getContentElement(name)` creates a `<flow-content-container>` element on the
server and attaches it as a virtual child with `INJECT_BY_NAME`. On the
client, `useContent(name)` renders
`<flow-content-container name=… style="display: contents">`. The client binds
the server element to it by name.

React renders asynchronously, so the slot usually does not exist yet when the
binding arrives. In that case the binding registers a ready callback
(`addReadyCallback`), and `useContent` calls it from an effect once the slot
is in the DOM. If the render never outputs the slot, the binding can't find
the element and fails.

## Mounting and unmounting

1. `connectedCallback` dispatches a cancelable, bubbling `flow-portal-add`
   event with the element and its React content.
2. Inside the `Flow` route component, `Flow.tsx` cancels the event and adds a
   portal into the element to its own React tree.
3. If nobody cancels the event, the element creates a React root of its own
   (`createRoot(this)`).
4. `disconnectedCallback` either dispatches `flow-portal-remove`, or unmounts
   its own root after a microtask.

## Decisions and gotchas

- **Quick detach and re-attach.** Flow often detaches and re-attaches an
  element in the same task, for example when it is moved.
  - Unmounting an own root waits a microtask and is skipped if the element is
    connected again by then.
  - `connectedCallback` waits for a pending unmount and never creates a
    second root (#26060).
  - On the `Flow.tsx` side, the removal listener is registered synchronously
    and reacts only to its own element (#20770, #24661).
- **A state is not synchronized without a listener.** The browser only sends
  `<name>-changed` updates of a property that the server listens to. Without
  `addStateChangeListener`, `getState` returns the last value the server set,
  not what the user typed.
- **`render()` follows the rules of hooks.** The three hooks are bound to the
  element, but `useContent` uses `useEffect`, and `render()` runs inside a
  React function component. Call the hooks unconditionally and in the same
  order.
- **React dependencies do not need Hilla.** `NodeUpdater` adds React and
  React Router whenever React is enabled (#18625). Before that, a Flow-only
  application with an adapter had no React installed.

## Working on it

- Integration tests: `flow-tests/test-react-adapter` (`ReactAdapterIT` for
  state in both directions, `null` state and quick re-attach;
  `FlowInReactComponentIT` for content slots) and the routing ITs in
  `flow-tests/test-default` (adapters inside the `Flow` route).
- A change in `ReactAdapter.template` reaches applications through
  `TaskGenerateReactFiles`. Check it in a test module, because the template
  is not compiled in this module.
- The portal handling is shared with `Flow.tsx`. A change to the
  `flow-portal-add` / `flow-portal-remove` events updates both files.
