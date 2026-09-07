/// <reference lib="webworker" />

self.__WB_DISABLE_DEV_LOGS = true;

self.importScripts('sw-runtime-resources-precache.js');
import { clientsClaim, cacheNames, WorkboxPlugin } from 'workbox-core';
import { matchPrecache, precacheAndRoute, getCacheKeyForURL } from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';
import { PrecacheEntry } from 'workbox-precaching/_types';
import { NetworkOnly, NetworkFirst } from 'workbox-strategies';

declare var self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: Array<PrecacheEntry>;
  __WB_DISABLE_DEV_LOGS: boolean;
  additionalManifestEntries?: Array<PrecacheEntry>;
};

self.skipWaiting();
clientsClaim();

declare var OFFLINE_PATH: string; // defined by Webpack/Vite

declare global {
  interface ImportMeta {
    readonly env: {
      readonly MODE: string;
      readonly BASE_URL: string;
      readonly PROD: boolean;
      readonly DEV: boolean;
      readonly SSR: boolean;
    } & Readonly<Record<string, string>>;
  }
}

// Combine manifest entries injected at compile-time by Webpack/Vite
// with ones that Flow injects at runtime through `sw-runtime-resources-precache.js`.
let manifestEntries: PrecacheEntry[] = self.__WB_MANIFEST || [];
// If injected entries contains element for root, then discard the one from Flow
// may only happen when running in development mode, but after a frontend build
let hasRootEntry = manifestEntries.findIndex((entry) => entry.url === '.') >= 0;
if (self.additionalManifestEntries?.length) {
  manifestEntries.push(...self.additionalManifestEntries.filter((entry) => entry.url !== '.' || !hasRootEntry));
}

const offlinePath = OFFLINE_PATH;

// Compute the registration scope path.
// Example: http://localhost:8888/scope-path/sw.js => /scope-path/
const scope = new URL(self.registration.scope);

// The offline stub served by PwaHandler, rendered by the Flow client within an
// <iframe> in the app shell when a server view cannot be reached.
const offlineStubPath = `${scope.pathname}offline-stub.html`;

/**
 * Replaces <base href> in pre-cached response HTML with the service worker’s
 * scope URL.
 *
 * @param response HTML response to modify
 * @returns modified response
 */
async function rewriteBaseHref(response: Response) {
  const html = await response.text();
  return new Response(html.replace(/<base\s+href=[^>]*>/, `<base href="${self.registration.scope}">`), response);
}

// Matches the frame-ancestors directive of a content security policy.
const frameAncestorsDirective = /^\s*frame-ancestors(\s|$)/i;

/**
 * Restricts framing of a document to the same origin, instead of denying it
 * altogether.
 *
 * The offline stub is rendered in a same-origin iframe, but the response served
 * for it carries the headers of the origin server, which are stored in the
 * pre-cache as well. A security filter, a reverse proxy or a CDN commonly adds
 * `X-Frame-Options: DENY` to every response, in which case the browser displays
 * its own error page instead of the offline stub. Downgrading the headers to
 * same-origin framing keeps a third party page from framing the stub.
 *
 * @param response response to serve for the offline stub
 * @returns response the app shell is allowed to render within an iframe
 */
async function allowSameOriginFraming(response: Response) {
  const frameOptions = response.headers.get('X-Frame-Options');
  const contentSecurityPolicy = response.headers.get('Content-Security-Policy');
  if (frameOptions === null && !contentSecurityPolicy?.includes('frame-ancestors')) {
    return response;
  }

  const headers = new Headers(response.headers);
  if (frameOptions !== null) {
    headers.set('X-Frame-Options', 'SAMEORIGIN');
  }
  if (contentSecurityPolicy !== null) {
    // Of a content security policy, only frame-ancestors prevents framing,
    // the other directives are served as they are.
    headers.set(
      'Content-Security-Policy',
      contentSecurityPolicy
        .split(',')
        .map((policy) =>
          policy
            .split(';')
            .map((directive) => (frameAncestorsDirective.test(directive) ? " frame-ancestors 'self'" : directive))
            .join(';')
        )
        .join(',')
    );
  }

  return new Response(await response.blob(), {
    status: response.status,
    statusText: response.statusText,
    headers
  });
}

/**
 * Returns true if the given URL is included in the manifest, otherwise false.
 */
function isManifestEntryURL(url: URL) {
  return manifestEntries.some((entry) => getCacheKeyForURL(entry.url) === getCacheKeyForURL(`${url}`));
}

/**
 * A workbox plugin that checks and updates the network connection status
 * on every fetch request.
 */
let connectionLost = false;
function checkConnectionPlugin(): WorkboxPlugin {
  return {
    async fetchDidFail() {
      connectionLost = true;
    },
    async fetchDidSucceed({ response }) {
      connectionLost = false;
      return response;
    }
  };
}

const networkOnly = new NetworkOnly({
  plugins: [checkConnectionPlugin()]
});
const networkFirst = new NetworkFirst({
  plugins: [checkConnectionPlugin()]
});

if (import.meta.env.DEV) {
  self.addEventListener('activate', (event) => {
    event.waitUntil(caches.delete(cacheNames.runtime));
  });

  // Cache /VAADIN/* resources in dev mode. Ensure the Vite specific URLs on another port are not handled to avoid excessive logging.
  registerRoute(
    ({ url }) => url.port === scope.port && url.pathname.startsWith(`${scope.pathname}VAADIN/`),
    networkFirst
  );
}

registerRoute(
  new NavigationRoute(async (context) => {
    async function serveOfflineFallback() {
      const response = await matchPrecache(offlinePath);
      return response ? rewriteBaseHref(response) : undefined;
    }

    function serveResourceFromCache() {
      // Always serve the offline fallback at the scope path.
      if (context.url.pathname === scope.pathname) {
        return serveOfflineFallback();
      }

      if (isManifestEntryURL(context.url)) {
        return matchPrecache(context.request);
      }

      return serveOfflineFallback();
    }

    async function serveNavigationRequest() {
      // Try to serve the resource from the cache when offline is detected.
      if (!self.navigator.onLine) {
        const response = await serveResourceFromCache();
        if (response) {
          return response;
        }
      }

      // Sometimes navigator.onLine is not reliable,
      // try to serve the resource from the cache also in the case of a network failure.
      try {
        return await networkOnly.handle(context);
      } catch (error) {
        const response = await serveResourceFromCache();
        if (response) {
          return response;
        }
        throw error;
      }
    }

    const response = await serveNavigationRequest();
    // Only the offline stub is served as frameable, so that a third party page
    // cannot frame the application itself.
    return context.url.pathname === offlineStubPath ? allowSameOriginFraming(response) : response;
  })
);

precacheAndRoute(manifestEntries);

self.addEventListener('message', (event) => {
  if (typeof event.data !== 'object' || !('method' in event.data)) {
    return;
  }

  // JSON-RPC request handler for ConnectionStateStore
  if (event.data.method === 'Vaadin.ServiceWorker.isConnectionLost' && 'id' in event.data) {
    event.source?.postMessage({ id: event.data.id, result: connectionLost }, []);
  }
});

// Handle web push

self.addEventListener('push', (e) => {
  const data = e.data?.json();
  if (data) {
    self.registration.showNotification(data.title, data.options);
  }
});

self.addEventListener('notificationclick', (e) => {
  e.notification.close();
  e.waitUntil(focusOrOpenWindow());
});

async function focusOrOpenWindow() {
  const url = new URL('/', self.location.origin).href;

  const allWindows = await self.clients.matchAll({
    type: 'window'
  });
  const appWindow = allWindows.find((w) => w.url === url);

  if (appWindow) {
    return appWindow.focus();
  } else {
    return self.clients.openWindow(url);
  }
}
