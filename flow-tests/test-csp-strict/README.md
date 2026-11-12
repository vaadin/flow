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

Every scenario below runs. An IT for a scenario that does not work yet is added
`@Disabled` with the issue that blocks it, and the annotation is removed when
the issue is fixed. To run a disabled IT anyway, for example to see how far a
fix gets, deactivate the condition:

```
mvn verify -pl flow-tests/test-csp-strict \
  -Djunit.jupiter.conditions.deactivate=org.junit.jupiter.engine.extension.DisabledCondition
```

| Scenario | View | IT |
|---|---|---|
| The policy header, a nonce on every script, a new nonce per response, a view rendered under the policy | `csp.PolicyView` | `PolicyIT` |
| JavaScript run through a `@JsDefinition` | `js.DeclaredJsView` | `DeclaredJsIT` |
| `executeJs(String)` throws in Java, trace leads to the caller | `js.StringJsView` | `StringJsIT` |
| Framework JavaScript: focus, page title | `dom.FocusView` | `FocusIT` |
| Navigation through a router link | `routing.NavigationView` | `NavigationIT` |
| Click listener with event data, element listener with an event data property path | `dom.ClickView` | `ClickIT` |
| Keyboard shortcut | `components.ShortcutView` | `ShortcutIT` |
| Server push from a background thread over WebSocket, the default transport, with no fallback | `push.PushView` | `PushIT` |
| Server push from a background thread over long polling | `push.LongPollingPushView` | `LongPollingPushIT` |
| Navigation to a view whose frontend module is in a lazy chunk | `chunk.LazyChunkView` | `LazyChunkIT` |
