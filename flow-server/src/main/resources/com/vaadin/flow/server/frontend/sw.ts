/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import {
  clientsClaim,
  RouteHandlerCallbackOptions,
  skipWaiting
} from 'workbox-core';
import {matchPrecache, precacheAndRoute} from 'workbox-precaching';
import {NavigationRoute, registerRoute} from 'workbox-routing';
import {PrecacheEntry} from 'workbox-precaching/_types';
import {NetworkOnly} from 'workbox-strategies';

declare var self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: Array<PrecacheEntry>,
  additionalManifestEntries?: Array<PrecacheEntry>
};

skipWaiting();
clientsClaim();

declare var OFFLINE_PATH_ENABLED: boolean; // defined by webpack.generated.js
declare var OFFLINE_PATH: string; // defined by webpack.generated.js

/**
 * Replaces <base href> in pre-cached response HTML with the service worker’s
 * scope URL.
 *
 * @param response HTML response to modify
 * @returns modified response
 */
const rewriteBaseHref = async (response: Response) => {
  const html = await response.text();
  return new Response(
    html.replace(
      /<base\s+href=[^>]*>/,
      `<base href="${self.registration.scope}">`
    ),
    response
  );
};

const appShellPath = '.';
const offlinePath = OFFLINE_PATH_ENABLED ? OFFLINE_PATH : appShellPath;
const networkOnly = new NetworkOnly();
let connectionLost = false;
const navigationFallback = new NavigationRoute(async (context: RouteHandlerCallbackOptions) => {
  // Use offlinePath fallback if offline was detected
  if (!self.navigator.onLine) {
    const offlinePathPrecachedResponse = await matchPrecache(offlinePath);
    if (offlinePathPrecachedResponse) {
      return rewriteBaseHref(offlinePathPrecachedResponse);
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
    const precachedResponse = await matchPrecache(offlinePath);
    return precachedResponse ? await rewriteBaseHref(precachedResponse) : error;
  }
});

registerRoute(navigationFallback);

let manifestEntries = self.__WB_MANIFEST;
if (self.additionalManifestEntries && self.additionalManifestEntries.length) {
  manifestEntries = [...manifestEntries, ...self.additionalManifestEntries];
}

precacheAndRoute(manifestEntries);

self.addEventListener('message', event => {
  if (typeof event.data !== 'object' || !('method' in event.data)) {
    return;
  }

  // JSON-RPC request handler for ConnectionStateStore
  if (event.data.method === 'Vaadin.ServiceWorker.isConnectionLost' && 'id' in event.data) {
    event.source?.postMessage({id: event.data.id, result: connectionLost}, []);
  }
});
