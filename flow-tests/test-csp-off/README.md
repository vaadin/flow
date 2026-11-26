# test-csp-off

Home for integration tests of the APIs that send JavaScript to the browser as a
string:

- `Element.executeJs(String, ...)` and `Page.executeJs(String, ...)`, and the
  values they return
- `JsFunction.of(String, ...)`
- `Page.addDynamicImport(String)`
- `Element.addJsInitializer(String, ...)`
- element listener filters and event data given as an expression rather than
  a property path: `setFilter(String)`, `addEventData(String)` and
  `addEventDataElement(String)`
- `Shortcuts.setShortcutListenOnElement(String, ...)`, a shortcut locator given
  as a string

`@EventData` values and `@DomEvent(filter = ...)` are JavaScript in
annotations too, but the build puts them into the bundle, so they work in the
strict mode and are tested in the other modules.

All other `flow-tests` modules run with `contentSecurityPolicy=strict`: the `vaadin.contentSecurityPolicy` property
of the `flow-tests` POM sets it, and the parent passes it to the application as
a system property. In that mode these APIs throw a `ContentSecurityPolicyViolationException`. This
module passes `off` to the application instead, a fixed value that
`-Dvaadin.contentSecurityPolicy=...` does not change, so it is the one place where they still
run.

The rest of the setup is the one of [test-default](../test-default/README.md):
Spring Boot, dev mode with frontend hotdeploy, and JUnit 6 (Jupiter).

## Adding a test

Only add a test here if it exercises one of the string-based APIs above. A test
that only uses such an API as a helper belongs in its feature module, with the
JavaScript declared in a `@JsDefinition` interface instead.

1. Add a `@Route` view under `src/main/java/com/vaadin/flow/test/cspoff/...`.
2. Add an IT under `src/test/java/...` that extends
   [`AbstractCspOffIT`](src/test/java/com/vaadin/flow/test/cspoff/AbstractCspOffIT.java),
   is annotated with `@TestFor(view)`, and uses `@BrowserTest` methods.
