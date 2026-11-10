# Spring Security for client-side views and endpoints

This note covers what `VaadinSecurityConfigurer` sets up so that a browser
client, not only the Flow engine, can work with Spring Security: CSRF, login
from TypeScript, and how unauthenticated requests are answered. Stateless
authentication with JWT has its own note,
[`stateless/ARCHITECTURE.md`](stateless/ARCHITECTURE.md). Access control for
Flow views (`NavigationAccessControl` and the access checkers) is not covered
here.

## Overview

A Flow UI talks to the server only through Vaadin's own requests (UIDL, push,
heartbeat). Those requests carry Vaadin's CSRF token and redirect through
UIDL. A Hilla client, or any custom JavaScript, makes plain HTTP requests
instead: endpoint calls, the login form post, the logout post. Those are
subject to Spring Security like any other request.

So the configurer has two jobs at once:

- **Keep Spring Security out of Flow's way.** Framework-internal requests skip
  Spring's CSRF check, internal and static resources are permitted, and the
  login page can be posted to without a token.
- **Make Spring Security usable from a script.** The CSRF token is put where
  a script can read it, the login answers with headers instead of a redirect,
  and a denied endpoint call gets a `401` instead of a login page.

## Design goals

- **One configuration for Flow and Hilla.** `http.with(VaadinSecurityConfigurer.vaadin(), …)`
  sets up both. The Hilla parts are only added when Hilla is on the classpath
  (`EndpointRequestUtil.isHillaAvailable()`).
- **Spring's CSRF protection stays on** for everything except Flow's own
  requests, which Vaadin protects itself.
- **A script can log in without parsing HTML or following redirects.**
- **Status codes, not pages, for scripts.** A request that can't show a login
  view gets a status the caller can handle.

## CSRF

| Requests | Protected by |
| -------- | ------------ |
| Flow's own: UIDL, push, heartbeat (`RequestUtil.isFrameworkInternalRequest`) | Vaadin's CSRF token, inside the UIDL. Spring's check is skipped (`customizeCsrf`). |
| The login page path (`formLoginPage`) | Nothing. The page is posted to before the user has a session token. |
| Everything else, including Hilla endpoints, login and logout posts | Spring Security's CSRF token. |

For a script to send Spring's token, the token has to reach the page:

- `IndexHtmlRequestHandler` in `flow-server` calls
  `SpringCsrfTokenUtil.addTokenAsMetaTagsToHeadIfPresentInRequest`. It writes
  the `_csrf`, `_csrf_header` and `_csrf_parameter` meta tags into
  `index.html`, from the token Spring puts in the request attribute.
- With a cookie-based token repository (stateless authentication), the token
  also lives in the `XSRF-TOKEN` cookie and may change after the page is
  served. So `SpringCsrfTokenUtil` also adds a small `formdata` listener
  script, which replaces the CSRF field of any posted form with the current
  cookie value (hilla#910).
- When Spring is not protecting the request, Hilla uses its own double-submit
  `csrfToken` cookie (`CsrfIndexHtmlRequestListener`, `CsrfChecker` in Hilla).

## Login from TypeScript

`VaadinSavedRequestAwareAuthenticationSuccessHandler` is the success handler
of form login and OAuth2 login. A request with the `source: typescript`
header, which Hilla's `login()` sends, is treated as a script login:

| Response header | Content |
| --------------- | ------- |
| `Result` | `success`, set by the handler's `RedirectStrategy` instead of a `302` |
| `Default-url` | where to go when there is no saved URL |
| `Saved-url` | the URL the user tried to open before logging in: the saved request, the URL stored by Flow's navigation access control, or the result of a `SuccessUrlResolver` |
| `Spring-CSRF-header`, `Spring-CSRF-token` | the CSRF token to use from now on |

The CSRF token changes at login: Spring renews it when the user
authenticates. A page that keeps running after a script login would
otherwise keep sending the old one. With a session, the handler reads the
renewed token from the request. With a cookie repository (stateless), it
generates and saves a new token itself. For a regular form login with a
cookie repository, it removes the token instead, so that a new one is issued
on the next request.

A failed login is Spring's normal failure handling. The client sees that the
response has no `Result: success`.

## Unauthenticated and denied requests

`customizeExceptionHandling` decides what an unauthenticated request gets:

- **Hilla endpoint calls** (`RequestUtil.isEndpointRequest`) get `401`. A
  `CsrfException` on an endpoint call also gets `401`, not `403`, so that
  Hilla's `InvalidSessionMiddleware` can treat an expired session and a stale
  token the same way: log in again and retry.
- **Sub-resource requests** from the browser (scripts, styles, images, as
  told by `HandlerHelper.isNonHtmlInitiatedRequest`) get `401` when a form
  login page is configured. Redirecting them to the login page would loop,
  and would hide which resource was denied.
- **Everything else** is redirected to the login page.

Access to client-side views follows the same idea: with Hilla, file routes
are authorized from their `ViewConfig` (`RequestUtil.isSecuredHillaRoute`,
`authorizeHillaRoute`), and endpoints require authentication unless they are
anonymous.

## The saved request

`VaadinDefaultRequestCache` saves only the requests worth returning to after
login: routes, not internal requests, or what the service worker fetched while
the user was logged out. The success handler reads
from it. With stateless authentication, its delegate is a
`CookieRequestCache`, so the saved URL survives without a session.

## Logout

`LogoutFilter` runs Spring's logout handlers. The success handler redirects
through `UidlRedirectStrategy`. When the logout was triggered by a Flow
request, the strategy sets the page location through the current `UI`.
Otherwise it sends a regular redirect. The
handlers are also passed to `AuthenticationContext`, so `logout()` from Java
runs the same chain. A Hilla client logs out with a plain post, and reads the
new CSRF token from the page it is redirected to.

## Working on it

- Unit tests: `VaadinSecurityConfigurerTest`,
  `VaadinSavedRequestAwareAuthenticationSuccessHandlerTest`, `RequestUtilTest`
  in `vaadin-spring`.
- End-to-end tests of script login, CSRF and `401` handling are in the Hilla
  repository (`packages/java/tests/spring/security`,
  `packages/java/tests/spring/security-jwt`, `packages/java/tests/csrf`).
- The response headers of a TypeScript login are a contract with `login()` in
  `@vaadin/hilla-frontend`. Change both together.
