# test-csp-strict

Integration tests for the `csp=strict` setting: a production-mode Spring Boot
application where Vaadin sends its nonce-based `Content-Security-Policy`
header, the browser enforces it, and the string-based JavaScript APIs throw.

One view per scenario, in the package `com.vaadin.flow.test.cspstrict.<feature>`.
A scenario is done when its IT opens the view, does the ordinary thing the
view is about, and the browser reports no policy violation. The violations are
collected by the inline script that `CspViolationCollector` adds to every page
(nonced by Vaadin like every other script), and read back with
`assertNoCspViolations()`.

ITs whose scenario does not work yet are `@Disabled` with the issue that blocks
them. Remove the annotation when the issue is fixed. To run them anyway, for
example to see how far a fix gets, deactivate the condition:

```
mvn verify -pl flow-tests/test-csp-strict \
  -Djunit.jupiter.conditions.deactivate=org.junit.jupiter.engine.extension.DisabledCondition
```

Today no page renders in strict mode at all: the constructor of `Page`
registers event data for the browser details, the strict mode rejects it, and
creating the `UI` fails for every request. So every IT that opens a view is
disabled with [#26152](https://github.com/vaadin/flow/issues/26152) until the
property paths of event data are sent without JavaScript. The header checks run.

| Scenario | View | IT | State |
|---|---|---|---|
| The policy header, a nonce on every script, a new nonce per response | `csp.PolicyView` | `PolicyIT` | header checks enabled, the view check blocked by [#26152](https://github.com/vaadin/flow/issues/26152) |
| JavaScript run through a `@JsDefinition` | `js.DeclaredJsView` | `DeclaredJsIT` | blocked by [#26152](https://github.com/vaadin/flow/issues/26152) (no UI) |
| `executeJs(String)` throws in Java, trace leads to the caller | `js.StringJsView` | `StringJsIT` | blocked by [#26152](https://github.com/vaadin/flow/issues/26152) (no UI) |
| Framework JavaScript: focus, page title | `dom.FocusView` | `FocusIT` | blocked by [#26152](https://github.com/vaadin/flow/issues/26152) (no UI) |
| Navigation through a router link | `routing.NavigationView` | `NavigationIT` | blocked by [#26152](https://github.com/vaadin/flow/issues/26152) (no UI) |
| Click listener with event data, element listener with an expression | `dom.ClickView` | `ClickIT` | blocked by [#26152](https://github.com/vaadin/flow/issues/26152) |
| Keyboard shortcut | `components.ShortcutView` | `ShortcutIT` | blocked by [#26666](https://github.com/vaadin/flow/issues/26666) |
