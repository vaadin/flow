/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import { clientsClaim } from 'workbox-core';
import { matchPrecache, precache } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { PrecacheEntry } from 'workbox-precaching/_types';
import { NetworkOnly, NetworkFirst } from 'workbox-strategies';

self.skipWaiting();
clientsClaim();

declare var self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: PrecacheEntry[];
  additionalManifestEntries?: PrecacheEntry[];
};

declare var OFFLINE_PATH: string; // defined by Webpack/Vite

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

// Combine manifest entries injected at compile-time by Webpack/Vite
// with entries that Flow injects at runtime through `sw-runtime-resources-precache.js`.
let manifestEntries: PrecacheEntry[] = self.__WB_MANIFEST || [];
if (self.additionalManifestEntries?.length) {
  manifestEntries.push(...self.additionalManifestEntries);
}
// Precache the resulting manifest entries.
precache(manifestEntries);

// Compute the scope path in case the app is run under a servlet context path.
const scopePath = new URL(self.registration.scope).pathname;

const networkOnly = new NetworkOnly();
const networkFirst = new NetworkFirst();

// Indicates whether the app is offline.
let connectionLost = false;

// @ts-ignore
if (process.env.NODE_ENV === 'development') {
  // Precache OFFLINE_PATH manually in dev mode in case it is not not been included
  // in the manifest which happens when OFFLINE_PATH is different from '.'.
  precache([OFFLINE_PATH]);

  // Don't cache ping/pong requests that Vite sends to determine the dev server's availability.
  // Otherwise, the page will fall into an infinite reload caused by the `@vite/client` module.
  registerRoute(
    ({ url }) => url.pathname.startsWith(`${scopePath}VAADIN/__vite_ping`),
    networkOnly
  );

  // Cache requests to the dev server.
  registerRoute(
    ({ url }) => url.pathname.startsWith(`${scopePath}VAADIN/`),
    networkFirst
  );
}

registerRoute(
  ({ url }) => url.pathname.startsWith(scopePath),
  async (context) => {
    async function serveResourceFromCache() {
      // serve any file in the manifest directly from cache
      const path = context.url.pathname;
      const pathRelativeToScope = path.substr(scopePath.length);
      if (manifestEntries.some(({ url }) => url === pathRelativeToScope)) {
        return await matchPrecache(pathRelativeToScope);
      }

      const offlinePathPrecachedResponse = await matchPrecache(OFFLINE_PATH);
      if (offlinePathPrecachedResponse) {
        return await rewriteBaseHref(offlinePathPrecachedResponse);
      }
      return undefined;
    };

    // When offline is detected, try to serve the resource from the cache.
    if (!self.navigator.onLine) {
      const precachedResponse = await serveResourceFromCache();
      if (precachedResponse) {
        return precachedResponse;
      }
    }

    // According to https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine,
    // `navigator.onLine` is not a reliable indicator of the online status due to browser differences.
    // So try to serve the resource from the cache also in the case of a network failure.
    try {
      const response = await networkOnly.handle(context);
      connectionLost = false;
      return response;
    } catch (error) {
      connectionLost = true;
      const precachedResponse = await serveResourceFromCache();
      if (precachedResponse) {
        return precachedResponse;
      }
      throw error;
    }
  }
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
