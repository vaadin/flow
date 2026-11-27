# test-csp-strict-spring-security

Integration tests for the `contentSecurityPolicy=strict` setting in an application that also
uses Spring Security, where the `Content-Security-Policy` of the application is
configured in Spring Security.

Spring Security creates a nonce for each request and puts it in place of the
`{nonce}` placeholder of the policy. Vaadin puts that nonce on the scripts of a
bootstrap page and sends no policy of its own. The tests pin what each response
carries:

| Response | Policy |
|---|---|
| A bootstrap page | The policy of Spring Security, with the nonce that every script of the page has. |
| A bootstrap page of a path the policy of Spring Security is not sent for | The policy of Vaadin, with the nonce that every script of the page has. |
| A file of the production bundle, `VAADIN/build/*.js` | The policy of Spring Security. |
| A download served by a `DownloadHandler` | The policy of Spring Security. |
| A response that Spring Security writes itself, such as a 403 | The policy of Spring Security. |

A directive added through `IndexHtmlResponse.getContentSecurityPolicy()` in an
`IndexHtmlRequestListener` is not sent: that policy is only there when Vaadin
sends it.

The tests read the response headers and need no browser. The browser
scenarios of the `strict` mode are in `test-csp-strict`; the policy of a
bootstrap page is the same here, so they are not repeated.
