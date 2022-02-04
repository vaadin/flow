/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import '@vite/env';
import { clientsClaim } from 'workbox-core';
import { matchPrecache, precache } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { PrecacheEntry } from 'workbox-precaching/_types';
import { NetworkOnly, NetworkFirst } from 'workbox-strategies';

declare var self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: PrecacheEntry[];
  additionalManifestEntries?: PrecacheEntry[];
};

declare var OFFLINE_PATH: string; // defined by webpack.generated.js

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

// Combine the manifest entries provided at compile-time with provided by Flow in `sw-runtime-resources-precache.js`.
let manifestEntries: PrecacheEntry[] = self.__WB_MANIFEST || [];
if (self.additionalManifestEntries?.length) {
  manifestEntries.push(...self.additionalManifestEntries);
}
// Precache the manifest entries
precache(manifestEntries);

// Compute the scope path for apps run with a servlet context path.
const scopePath = new URL(self.registration.scope).pathname;

const networkOnly = new NetworkOnly();
const networkFirst = new NetworkFirst();

let connectionLost = false;

// @ts-ignore
if (process.env.NODE_ENV === 'development') {
  // Precache OFFLINE_PATH manually in dev mode in case it has not been included in the manifest
  // (e.g when OFFLINE_PATH === '.').
  precache([OFFLINE_PATH]);

  // Cancel `__vite_ping` requests to prevent Vite from doing infinite page reload.
  registerRoute(
    ({ url }) => url.pathname.startsWith(`${scopePath}VAADIN/__vite_ping`),
    async (context) => {
      return self.navigator.onLine
        ? networkOnly.handle(context)
        : Response.error();
    }
  );

  // Cache requests sent to the dev server.
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

    // Use offlinePath fallback if offline was detected
    if (!self.navigator.onLine) {
      const precachedResponse = await serveResourceFromCache();
      if (precachedResponse) {
        return precachedResponse;
      }
    }

    // Sometimes navigator.onLine is not reliable, use fallback to offlinePath
    // also in case of network failure
    try {
      const response = await networkOnly.handle(context);
      connectionLost = false;
      return response;
    } catch (error) {
      connectionLost = true;
      const precachedResponse = await serveResourceFromCache();
      return precachedResponse || error
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

self.skipWaiting();
clientsClaim();
