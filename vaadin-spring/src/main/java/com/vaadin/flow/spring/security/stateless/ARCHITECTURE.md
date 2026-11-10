# Stateless authentication with JWT

## Overview

By default, Spring Security keeps the authenticated user in the HTTP session.
Stateless authentication keeps it in the browser instead, as a signed JSON
Web Token (JWT) in cookies. Every request carries the token, and the server
rebuilds the user from it without looking anything up. The main audience is
Hilla applications that run without sessions, behind a load balancer without
sticky sessions or in front of several server instances. Flow views still use
a `VaadinSession` for their UI state, so stateless authentication doesn't make
them stateless, but it works with them too.

An application enables it next to its regular Vaadin security setup:

```java
http.with(new VaadinStatelessSecurityConfigurer<>(), cfg -> cfg
        .withSecretKey().secretKey(new SecretKeySpec(key, JwsAlgorithms.HS256))
        .and().issuer("com.example.app"));
```

The story of a user:

1. The user logs in through the login view, with a form post or with Hilla's
   `login()`. Spring Security authenticates as usual.
2. While the response is written, the user is encoded as a JWT: the name as
   `sub`, the roles as `roles`, and `iat`, `exp`, `iss`. It is signed with the
   secret key and sent as two cookies.
3. On the next request, the cookies are joined and verified, and the user is
   rebuilt from the claims. The response carries a fresh token with a new
   expiry.
4. The token expires after `expiresIn` (30 minutes by default) without
   requests, or the user logs out and the cookies are removed.

## Design goals

- **No server-side state for authentication.** Any instance with the key can
  authenticate any request.
- **The token can't be stolen by scripts, but the client can still read
  it.** The JWT is split into two cookies. `jwt.headerAndPayload` is readable
  from JavaScript, so a client can see who is logged in and when the token
  expires. `jwt.signature` is `HttpOnly`, so a script can't take the whole,
  valid token.
- **A sliding session.** The token is re-issued on every response, so an
  active user stays logged in without a refresh token.
- **CSRF without a session.** The CSRF token moves from the session into a
  cookie (the double-submit cookie pattern).
- **Plug into Spring Security**, not around it. Everything is a standard
  extension point: a `SecurityContextRepository`, a `CsrfTokenRepository` and
  a `RequestCache`.

## The pieces

All in this package, except where noted:

| Piece | Role |
| ----- | ---- |
| `VaadinStatelessSecurityConfigurer` | Public entry point (`expiresIn`, `issuer`, `withSecretKey`). Wires the classes below into `HttpSecurity`. |
| `JwtSecurityContextRepository` | `SecurityContextRepository`. `loadContext` decodes and verifies the JWT, and `saveContext` encodes a new one, or removes it for an anonymous user. |
| `SerializedJwtSplitCookieRepository` | Splits the serialized JWT into the two cookies and joins them back. |
| `UpdateJwtCookiesFilter` (nested) | Saves the current context into the cookies when the response is committed, or at the end of the request. |
| `CookieCsrfTokenRepository` (Spring) | CSRF token in the `XSRF-TOKEN` cookie, readable from JavaScript. |
| `CookieRequestCache` (Spring) | The request that was saved before login, kept in a cookie. It is the delegate of `VaadinDefaultRequestCache`. |
| `VaadinSavedRequestAwareAuthenticationSuccessHandler` | In the parent package. After a TypeScript login, it issues the new CSRF cookie and returns the token in headers. See [`../ARCHITECTURE.md`](../ARCHITECTURE.md). |

## How it is wired

`init()`:

- The security context repository becomes a
  `DelegatingSecurityContextRepository` of the JWT repository and a
  `RequestAttributeSecurityContextRepository`. The request attribute keeps the
  context for forwards and error dispatches within the same request.
- If CSRF is enabled, the CSRF token repository becomes
  `CookieCsrfTokenRepository.withHttpOnlyFalse()`. The request handler is
  `XorCsrfTokenRequestAttributeHandler::handle`, which is Spring's recipe for
  JavaScript clients. The token is still loaded per request, but the value
  that comes back in the header is the plain cookie value, not a masked one.
- The CSRF session strategy becomes `NullAuthenticatedSessionStrategy`.
  Spring renews the CSRF token whenever a user authenticates. Here, *every*
  request authenticates from the JWT, so the default would replace the token
  on every request.
- The same CSRF repository is handed to the success handler, so that a
  TypeScript login can return the new token.

`configure()` passes the key, the algorithm, the issuer and `expiresIn` to the
repository, and adds `UpdateJwtCookiesFilter` before `HeaderWriterFilter`. If
the request cache is `VaadinDefaultRequestCache`, it sets `CookieRequestCache`
as its delegate.

## A request

1. **Load.** `SecurityContextHolderFilter` asks the repository.
   `JwtSecurityContextRepository.loadContext` joins the cookies and verifies
   them with a `NimbusJwtDecoder`: the signature with the configured key,
   `exp`/`nbf`, and `iss` when an issuer is set. Spring's
   `JwtAuthenticationConverter` then turns the claims into a
   `JwtAuthenticationToken`, with `ROLE_<role>` authorities from `roles`. A
   missing, expired or invalid token gives an empty context (logged at trace
   level).
2. **Handle.** Authorization and the application run as usual.
3. **Save.** When the response is about to commit,
   `UpdateJWTCookieOnCommitResponseWrapper` saves the current context. An
   authenticated user gets a new token, with `exp = now + expiresIn` and both
   cookies with `Max-Age = expiresIn - 1`. An anonymous context removes the
   cookies, but only if the request had them. When a `jwt.headerAndPayload`
   cookie is already being set on the response, the filter leaves it alone.
   The cookies are `Secure` on HTTPS, and their path is the context path.

Logging in is the same save, with the context that the login just
authenticated. Logging out clears the context, so the save removes the
cookies.

## Decisions and gotchas

- **The principal changes after the first request.** At login, the principal
  is whatever the authentication provider returned, for example a
  `UserDetails`. On later requests, it is rebuilt from the token: a
  `JwtAuthenticationToken` whose name is `sub`. Code that casts the principal
  to `UserDetails` works right after login and fails afterwards. Read the
  user name, or look the user up by name.
- **Only roles survive.** The token carries the authorities with the `ROLE_`
  prefix, without that prefix, in `roles`. Other authorities and any custom
  details are lost on the next request.
- **HMAC keys only.** `withSecretKey()` builds an `OctetSequenceKey`, so the
  algorithm is one of the `MacAlgorithm`s. With no algorithm given, it is
  taken from the key's algorithm name. Every instance needs the same key, and
  changing the key logs everyone out.
- **Logging out doesn't revoke the token.** Removing the cookies ends the
  session in that browser, but a copied token stays valid until it expires.
  That is the price of not keeping state. Keep `expiresIn` short.
- **The cookies are not `SameSite`-restricted.** Spring's `Cookie` API used
  here sets no `SameSite` attribute, so the browser default applies. CSRF
  protection comes from the CSRF token, not from the cookie attributes.
- **History.** The cookies used to be updated only by the login. Since #18994
  they are re-issued on every request. The `RequestAttributeSecurityContextRepository`
  delegate came with #15804, and the XOR request handler with #16521.

## Working on it

- Unit tests in `vaadin-spring/src/test/java/com/vaadin/flow/spring/security/stateless/`:
  `JwtSecurityContextRepositoryTest`, `SerializedJwtSplitCookieRepositoryTest`
  and `JwtStatelessAuthenticationTest` (the whole filter chain).
- End-to-end tests with a client are in the Hilla repository:
  `packages/java/tests/spring/security-jwt`.
- How a Hilla client logs in and keeps CSRF up to date is described in
  `packages/ts/frontend/ARCHITECTURE.md` in the Hilla repository.
