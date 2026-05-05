# Flow Tests

Integration and end-to-end test modules for the Vaadin Flow framework.

## Always-Built Modules

These modules are built regardless of `-DskipTests`.

| Module | Description |
|--------|-------------|
| **test-resources** | Shared JAR providing static frontend and Java resources (JS modules, Flow bootstrap files, test component classes) reused across multiple test modules. Contains no runnable tests. |
| **test-common** | Shared JAR providing common base classes and utilities for UI integration tests, such as `AbstractDivView`, servlet helpers, and dependency-loading base views. |
| **test-lumo** | Test-only Lumo theme implementation (`AbstractTheme`) pulling in Vaadin Lumo npm packages and JS modules. Consumed by other test modules needing Lumo support. |
| **test-express-build** | Parent POM aggregating tests for Vaadin's "express build" feature (pre-built dev bundles and production bundles that skip full Vite/npm builds). Sub-modules cover dev bundle creation, production bundle validation, add-on scenarios, reusable themes, and embedding. |

## Shared Modules (`it-shared-modules` profile)

Built with `-DsharedModules` so they can be installed before running tests separately (e.g. in CI).

| Module | Description |
|--------|-------------|
| **test-root-context** | Main broad integration test WAR covering a large range of Flow features deployed at root context in NPM dev mode: UI components, routing, push, event handling, Lit and Polymer templates, error handling, serialization, and more. |
| **test-embedding** | Tests for embedding Flow applications as web components into non-Vaadin host pages. Sub-modules cover generic embedding, theme variants, production-mode embedding, application themes, and reusable themes. See [test-embedding/README.md](test-embedding/README.md). |
| **test-frontend** | Parent POM aggregating tests for the frontend build pipeline (Vite, npm, pnpm, optionally bun). Sub-modules test Vite basics, production builds, context paths, PWA support, embedded web component resync, and package-manager-specific builds. |
| **test-application-theme** | Tests for Flow's application theme system: reusable themes as a parent theme, live reload of application and component theme CSS changes via Vite, and theme inheritance scenarios. |
| **test-multi-war** | Tests deploying two separate Flow WAR applications simultaneously in the same server to verify they function independently, including views and embedded web components working side by side. |
| **vaadin-spring-tests** | Large aggregator of integration test modules covering Flow's Spring and Spring Boot integration: Spring Boot (plain, WAR, JAR, contextpath, reverse proxy), Spring MVC, Spring Security (access control, URL mapping, WebSocket, route access checkers), classpath scanning, and live reload performance. |

## Test Modules (`it-test-modules` profile)

Active when `-DskipTests` is **not** set (the default).

### Core Framework

| Module | Description |
|--------|-------------|
| **test-dev-mode** | Tests for development-mode-specific behavior: dependency loading order, debug window/error overlay, dev tools plugins, Vite communication channel, stream resources, exported JS functions, and Vite websocket logout handling. |
| **test-servlet** | Tests that the Flow servlet registers and deploys correctly, verifying a basic navigation target is accessible and renders content via the standard servlet mechanism. |
| **test-misc** | Catch-all module for miscellaneous integration tests: compressed resource serving, exception logging, i18n/translation, `@PreserveOnRefresh`, production-mode config, partial route matching, and themed component rendering. |
| **test-eager-bootstrap** | Tests the "eager bootstrap" (eager server load) feature where the initial UIDL is embedded in the first HTTP response, verifying the page includes the UIDL payload and basic view rendering works. |
| **test-custom-route-registry** | Tests using a custom `RouteRegistry` implementation in place of the default `ApplicationRouteRegistry`, verifying custom routes, error handling, and not-found views resolve through the user-provided registry. |
| **test-client-queue** | Tests the client-side message queue under adverse server conditions: slow responses, missing responses, and re-sync loops. Verifies the client queues requests correctly and avoids duplicate processing. |
| **test-commercial-banner** | Tests that when a commercial Vaadin add-on is present without a valid license, the commercial licensing banner is correctly displayed in the browser's shadow DOM. |

### Routing

| Module | Description |
|--------|-------------|
| **test-vaadin-router** | Integration tests for client-side routing using `vaadin-router`: navigation lifecycle events, forwarding/redirecting, history manipulation, query parameters, back-navigation, and postponed navigation. Also has a production-mode build variant. |
| **test-react-router** | Same routing test suite as `test-vaadin-router` but executed against a React Router-based setup, ensuring parity between the two routing strategies. Also has a production-mode build variant. |
| **test-react-adapter** | Tests the `flow-react` adapter bridging server-side Flow components with client-side React components: bidirectional state synchronization and embedding Flow server views inside React component trees. Also has a production-mode build variant. |
| **test-router-custom-context** | Tests that the Flow Router and dependency injection work correctly when deployed under a non-root servlet context path (`/custom-context-router`). Also tests encoded URL parameters and offline behavior. |
| **test-router-custom-context-encoded** | Same tests as `test-router-custom-context` but deployed under a context path with URL-encodable and regex-special characters (spaces, `$`, `{`, `}`), verifying correct location computation in dev mode. |
| **test-router-custom-context-encoded-prod** | Identical to `test-router-custom-context-encoded` but in production mode, ensuring encoded context path handling works with a production-compiled frontend bundle. |
| **test-ccdm** | Tests the Client-side Development Mode (CCDM) setup — TypeScript/Lit client router with server-side Java views — covering index HTML handling, server-side navigation lifecycle, push, app theme, and exception handling. Also has a production-mode build variant. |
| **test-ccdm-flow-navigation** | Tests bidirectional navigation between TypeScript client-side views and Java server-side Flow views in a CCDM app under a non-root context path, including special-character routes and service worker behavior. Also has a production-mode build variant. |

### Themes

| Module | Description |
|--------|-------------|
| **test-themes** | Tests Flow's application theme support in development (hot-deploy) mode: CSS loading, theme application to components, and hot-reloading of theme changes. Also has production-mode and devbundle build variants. |
| **test-no-theme** | Tests that a Flow application configured with `@NoTheme` correctly suppresses default theming, verifying standard browser default styles are applied instead of Lumo. (Tests disabled; included for Maven version plugin.) |
| **test-theme-no-polymer** | Tests that a custom application theme (Lumo utility class expansion) works in a project without Polymer components, ensuring theme utility CSS classes apply without requiring Polymer. |

### PWA

| Module | Description |
|--------|-------------|
| **test-pwa** | Tests the `@PWA` annotation end-to-end: correct PWA meta tags, web manifest, icons, service worker registration, and offline resources. Also has a production-mode build variant. |
| **test-pwa-disabled-offline** | Tests the `@PWA` annotation when offline mode is disabled, verifying no service worker is registered and no related errors are logged. Also has a production-mode build variant. |

### Build & Frontend Pipeline

| Module | Description |
|--------|-------------|
| **test-npm-only-features** | Parent module grouping sub-tests for NPM-specific features: external JS module loading, init listeners, builds without the build mojo, custom frontend directories, bytecode scanning, and startup performance checks. |
| **test-custom-frontend-directory** | Tests that Flow applications work correctly with a custom (non-default) frontend directory, verifying themes, CSS loading, and generated TypeScript resolve from the alternate directory. |
| **test-legacy-frontend** | Tests that custom Vaadin themes placed in the legacy `frontend/` folder (pre-24.x convention) still work, verifying Lumo utility class names resolve and apply correct CSS styles. |

### Live Reload & Redeployment

| Module | Description |
|--------|-------------|
| **test-live-reload** | Tests live-reload in dev mode for a single-module project: Java class change, frontend file change, theme changes, `@PreserveOnRefresh` interaction, and enable/disable via Dev Tools UI. |
| **test-live-reload-multimodule** | Tests live reload in a multi-module Maven project (library + UI + theme submodules) with hot-deploy enabled, verifying frontend changes from a dependency library trigger a live reload. |
| **test-live-reload-multimodule-devbundle** | Same multi-module scenario but with hot-deploy disabled (dev bundle mode), validating live reload works through the dev bundle pipeline. |
| **test-redeployment** | Tests Spring Boot DevTools-triggered redeployment: session preservation across reloads, dev-mode class cache picking up newly compiled views, and theme switching live-reload after redeployment. |
| **test-redeployment-no-cache** | Tests that when the dev-mode class cache is explicitly disabled, the cache is not populated, ensuring the no-cache configuration is respected during Spring Boot dev-mode restarts. |

### Servlet Containers & Embedding

| Module | Description |
|--------|-------------|
| **servlet-containers** | Aggregator that runs integration tests on servlet containers other than the default Jetty. Contains a `tomcat10` submodule deploying the root-context WAR onto Apache Tomcat 10 via the Cargo plugin. |

## Nightly Profile

| Module | Description |
|--------|-------------|
| **test-webpush** | Integration tests for Flow's Web Push notification feature (`flow-webpush`): subscribing, unsubscribing, permission checking, and sending push notifications. |
