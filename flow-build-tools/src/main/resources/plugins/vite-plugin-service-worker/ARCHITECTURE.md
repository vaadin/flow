# `@PWA` and the offline service worker

This note lives next to the Vite plugin that builds the service worker, but
it covers the whole feature: the annotation, the server-side handlers, the
build and the service worker at runtime.

## Overview

An application puts `@PWA(name = "…", shortName = "…")` on its
`AppShellConfigurator`. That makes it an installable progressive web
application:

- the page links a web app manifest and a set of icons generated from one
  logo;
- the page registers a service worker, `sw.js`;
- the service worker precaches the application shell and its frontend bundle.
  When the server can't be reached, navigations get a cached page instead of
  the browser's error page.

When the network is down, a client-side view keeps working from the cache.
A server-side view can't work without the server, so it shows the offline
page or the "offline stub", and Flow re-renders it when the connection comes
back. The connection state that drives this is described in
[`flow-client/ARCHITECTURE.md`](../../../../../../flow-client/ARCHITECTURE.md#connection-state-and-loading-indicator).

With `@PWA(offline = false)`, the application keeps the manifest and the icons,
but no service worker is built, and an existing registration is removed.

## Design goals

- **Zero configuration.** One annotation produces the manifest, the icons
  for every platform, the offline page and the service worker. Each of these
  can be replaced by a static file of the same name, or by an own
  `frontend/sw.ts`.
- **Offline is about the shell, not the data.** The service worker caches
  what the browser needs to start the application (the bundle, the shell,
  the icons, the offline page). It never caches UIDL or endpoint responses.
- **The cache list is exact.** Each precached URL has a revision, so a new
  deployment replaces exactly what changed. URLs whose contents can change
  without a new revision are not precached, but served network-first.
- **Same behaviour in development.** The service worker is built and served
  in development mode too, and it is rebuilt and updated live when its
  sources change.

## The pieces

| Piece | Where | Role |
| ----- | ----- | ---- |
| `PWA`, `PwaConfiguration` | `flow-server/.../server/` | The annotation and its parsed values (paths, colors, `offline`, `offlinePath`, `offlineResources`). |
| `PwaRegistry` | same | One per servlet context. Builds the icons, the manifest JSON, the offline page HTML, and the runtime precache list. |
| `PwaHandler` | `flow-server/.../server/communication/` | Serves the icons, `manifest.webmanifest`, the offline page and `offline-stub.html`, and `sw-runtime-resources-precache.js`. |
| `BootstrapHandler.setupPwa` | `flow-server/.../server/` | Adds the manifest link, the icon links and the theme color meta tags to `index.html`. It also adds a script that registers `sw.js`, or unregisters it when offline is disabled. |
| `TaskGeneratePWAIcons`, `TaskGenerateServiceWorker` | `flow-build-tools/.../server/frontend/` | Generate the icon sizes at build time, and the default `sw.ts` into the build folder (unless `frontend/sw.ts` or `frontend/sw.js` exists). |
| `TaskUpdateSettingsFile`, `TaskUpdateVite` | same | Write `clientServiceWorkerSource`, `offlineEnabled` and `offlinePath` into the Vite settings, and enable this plugin in `vite.generated.ts` when offline is enabled. |
| `vite-plugin-service-worker` (this folder) | `flow-build-tools/src/main/resources/plugins/` | Builds `sw.js` from `sw.ts` in a separate Vite build, and injects the build-time precache list. |
| `sw.ts` | `flow-server/.../server/frontend/` | The service worker: precaching, the navigation route with the offline fallback, the connection state, and web push. |

## Two halves of the precache list

The list of what to precache comes from two places, because neither side
knows everything:

1. **Build time: the frontend bundle.** In production builds, this plugin
   runs a second Vite build for `sw.ts` after the main bundle is written
   (`closeBundle`). The `injectManifestToSWPlugin` globs the whole output
   folder with workbox's `getManifest`, except `*.br` files and `pwa-icons/`.
   It replaces `self.__WB_MANIFEST` with the result. `index.html` is
   rewritten to `.`, the app shell URL. The output is also brotli-compressed,
   like the rest of the bundle.
2. **Runtime: what only the server knows.** `sw.ts` first runs
   `importScripts('sw-runtime-resources-precache.js')`. `PwaHandler` answers
   that request with `PwaRegistry.getRuntimeServiceWorkerJs(request)`, which
   sets:
   - `self.additionalManifestEntries`: the cached icons, the offline page (or
     `.` in development mode, when there is no `offlinePath`), the offline
     stub, the manifest, the `offlineResources`, and in production mode the
     `@StyleSheet`s of the app shell with their content hash (#25047);
   - `self.additionalNetworkFirstUrls`: in development mode, the app shell
     stylesheets, which have no content hash.

`sw.ts` merges the two lists and drops a duplicate `.` entry, then calls
`precacheAndRoute`.

## Runtime

- **Install.** Workbox fetches and caches every entry. `skipWaiting()` and
  `clientsClaim()` make a new worker take over at once, without waiting for
  the open tabs to close.
- **Navigation requests** go through a `NavigationRoute`:
  - The network is always tried first (`NetworkOnly`), so the server stays the
    source of truth for every page.
  - When the browser is offline, or the fetch fails, the route serves the
    precached copy of that URL if there is one. Otherwise it serves the
    offline page (`OFFLINE_PATH`, defined at build time).
  - The `<base href>` of a cached page is rewritten to the service worker
    scope (`rewriteBaseHref`). That way the same cached shell works for every
    path in the scope (#9652).
  - `offline-stub.html` is the page `Flow.ts` shows in an iframe for a server
    route while offline. It is the only response made frameable by the same
    origin (`allowSameOriginFraming`), in case a proxy or security filter adds
    `X-Frame-Options: DENY` to everything (#25504).
- **Other requests** are served by the precache (cache-first), or
  network-first for `additionalNetworkFirstUrls`. In development mode, the
  same applies to `/VAADIN/*` on the server port.
- **Connection state.** A workbox plugin records whether the last network
  request failed. The page's `ConnectionStateStore` asks for it once with the
  `Vaadin.ServiceWorker.isConnectionLost` message, so a page served from the
  cache starts as "connection lost".
- **Web push.** `push` events show a notification, and `notificationclick`
  focuses or opens the application window.

## Development mode

- The plugin builds `sw.js` in memory at `buildStart` (`write: false`). It
  serves it through `resolveId` / `load` as `/sw.js`. `ViteHandler` forwards
  `/sw.js` from the server root to Vite.
- When a module that `sw.js` was built from changes, `hotUpdate` rebuilds it
  and sends the `sw-js-update` HMR event. `vite-devmode.ts` reacts by calling
  `registration.update()`, so the new worker is installed without a manual
  reload (#21524).
- The worker clears its runtime cache on `activate`, so stale `/VAADIN/*`
  responses from a previous dev server run are not served.

## Decisions and gotchas

- **`offlinePath` is compiled into `sw.js`** as `OFFLINE_PATH`. Changing it
  requires a new frontend bundle, so `BundleValidationUtil` compares it with
  the `pwaOfflinePath` recorded in `stats.json`. A missing value is treated as
  `'.'`, the value the platform default bundle uses.
- **Separate build, separate output.** `sw.js` is an IIFE with inlined
  dynamic imports, because a classic service worker can't load modules.
  `globalThis.document` is defined as `undefined`, so that code shared with
  the page, such as Hilla's endpoint client, takes its worker branch.
  `import.meta.env` replaces `process.env` (#21573).
- **Unreadable stylesheets are skipped.** Workbox installs all entries or
  none, and one 404 would disable offline support altogether. That is why
  `PwaRegistry` leaves out a stylesheet whose content it can't read.
- **Revisions.** The revision of an entry is a hash of its own contents
  (icons, stylesheets, offline page, manifest). The exception is
  `offlineResources`, which use the servlet context hash, so they refresh
  once per deployment.
- **Precached icons are served `no-cache`.** The service worker owns their
  caching, so the browser's HTTP cache must not serve stale copies.
- **Spring Security** needs to permit the PWA resources without login.
  `PwaResourcesRequestMatcher` in `vaadin-spring` lists them.

## Working on it

- Integration tests: `flow-tests/test-pwa` (`PwaTestIT`: the PWA resources
  online and offline, `offlinePath`, and the compressed `sw.js`) and
  `flow-tests/test-pwa-disabled-offline` (`MainIT`: no registration, and an
  existing registration is removed).
- `index.ts` here is copied into the application's build folder by
  `TaskInstallFrontendBuildPlugins`, only the files listed in `package.json`.
  To work on it in place, set `"update": false` in the copied `package.json`
  so it is not overwritten.
- `sw.ts` is a template that is copied into the application. An application
  with its own `frontend/sw.ts` does not get the changes.
