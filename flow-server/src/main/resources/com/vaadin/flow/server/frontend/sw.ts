/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import { clientsClaim, cacheNames, WorkboxPlugin } from 'workbox-core';
import { matchPrecache, precache } from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';
import { PrecacheEntry } from 'workbox-precaching/_types';
import { NetworkOnly, NetworkFirst } from 'workbox-strategies';

declare var self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: PrecacheEntry[];
  additionalManifestEntries?: PrecacheEntry[];
};

declare var OFFLINE_PATH: string; // defined by Webpack/Vite

// Combine manifest entries injected at compile-time by Webpack/Vite
// with ones that Flow injects at runtime through `sw-runtime-resources-precache.js`.
let manifestEntries: PrecacheEntry[] = self.__WB_MANIFEST || [];
if (self.additionalManifestEntries?.length) {
  manifestEntries.push(...self.additionalManifestEntries);
}
// Precache the resulting manifest entries.
precache(manifestEntries);

// Compute the registration scope path.
const scopePath = new URL(self.registration.scope).pathname;

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
};

/**
 * Returns true if the given URL is included in the manifest, otherwise false.
 */
function isManifestEntryURL(url: URL) {
  const pathRelativeToScope = url.pathname.substring(scopePath.length);
  return manifestEntries.some((entry) => entry.url === pathRelativeToScope);
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
      return response
    }
  }
}

const networkOnly = new NetworkOnly({
  plugins: [checkConnectionPlugin()]
});
const networkFirst = new NetworkFirst({
  plugins: [checkConnectionPlugin()]
});

if (process.env.NODE_ENV === 'development') {
  self.addEventListener('activate', (event) => {
    event.waitUntil(caches.delete(cacheNames.runtime));
  });

  registerRoute(
    ({ url }) => url.pathname.startsWith(`${scopePath}VAADIN/__vite_ping`),
    networkOnly
  );

  registerRoute(
    ({ url }) => url.pathname.startsWith(`${scopePath}VAADIN/`),
    networkFirst
  );

  if (OFFLINE_PATH === '.') {
    registerRoute(
      ({ url }) => !isManifestEntryURL(url),
      async ({ event }) => {
        const response = await networkFirst.handle({
          request: new Request(OFFLINE_PATH),
          event
        });
        return rewriteBaseHref(response);
      }
    )
  }
}

/**
 * Handle requests to manifest entries.
 */
registerRoute(
  ({ url }) => isManifestEntryURL(url),
  async (context) => {
    if (!navigator.onLine) {
      const response = await matchPrecache(context.request)
      if (response) {
        return response;
      }
    }

    try {
      return await networkOnly.handle(context);
    } catch (error) {
      const response = await matchPrecache(context.request);
      if (response) {
        return response;
      }
      throw error;
    }
  }
);

/**
 * Handle other requests.
 */
registerRoute(
  new NavigationRoute(async (context) => {
    if (!navigator.onLine) {
      const response = await matchPrecache(OFFLINE_PATH);
      if (response) {
        return rewriteBaseHref(response);
      }
    }

    try {
      return await networkOnly.handle(context);
    } catch (error) {
      const response = await matchPrecache(OFFLINE_PATH);
      if (response) {
        return rewriteBaseHref(response);
      }
      throw error;
    }
  })
);

self.addEventListener('message', (event) => {
  if (typeof event.data !== 'object' || !('method' in event.data)) {
    return;
  }

  // JSON-RPC request handler for ConnectionStateStore
  if (event.data.method === 'Vaadin.ServiceWorker.isConnectionLost' && 'id' in event.data) {
    event.source?.postMessage({ id: event.data.id, result: connectionLost }, []);
  }
});

self.skipWaiting();
clientsClaim();
