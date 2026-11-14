# test-csp-strict-spring-security

Integration tests for the `csp=strict` setting in an application that also
uses Spring Security, where Spring Security is configured with a
`Content-Security-Policy` of its own.

Both Vaadin and Spring Security want to send the header. The tests pin what
each response carries:

| Response | Policy |
|---|---|
| A bootstrap page | Only the nonce-based policy of Vaadin. The policy of Spring Security is not sent and not merged in. |
| A bootstrap page, with a directive added through `IndexHtmlResponse.getContentSecurityPolicy()` in an `IndexHtmlRequestListener` | The policy of Vaadin with that directive. This is how an application adds what it would otherwise configure in Spring Security. |
| A file of the production bundle, `VAADIN/build/*.js` | The policy of Spring Security. |
| A download served by a `DownloadHandler` | The policy of Spring Security. |
| A response that Spring Security writes itself, such as a 403 | The policy of Spring Security. |

The tests read the response headers and need no browser. The browser
scenarios of the `strict` mode are in `test-csp-strict`; the policy of a
bootstrap page is the same here, so they are not repeated.
