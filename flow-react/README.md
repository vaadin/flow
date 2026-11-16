# Flow React

React integration for Flow:

- `ReactAdapterComponent`, the Java base class for wrapping a React component
  as a Flow component, together with the `ReactAdapterElement` web component
  base class it pairs with.
- `ReactRouterOutlet`, which renders client-side React views inside a Flow
  `@Layout`.

How the adapter works is described in [ARCHITECTURE.md](ARCHITECTURE.md).
