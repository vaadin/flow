/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import {skipWaiting, clientsClaim} from 'workbox-core';
import {matchPrecache, precacheAndRoute} from 'workbox-precaching';
import {NavigationRoute, registerRoute} from 'workbox-routing';
import {PrecacheEntry} from 'workbox-precaching/_types';
import {NetworkOnly} from 'workbox-strategies';

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
const navigationFallback = new NavigationRoute(async (params: any) => {
  // Use offlinePath fallback if offline was detected
  if (!self.navigator.onLine) {
    const offlinePathPrecachedResponse = await matchPrecache(offlinePath);
    if (offlinePathPrecachedResponse) {
      return offlinePathPrecachedResponse;
    }
  }

  // Sometimes navigator.onLine is not reliable, use fallback to offlinePath
  // also in case of network failure
  try {
    return await networkOnly.handle(params);
  } catch (error) {
    return (await matchPrecache(offlinePath)) || error;
  }
});

registerRoute(navigationFallback);

// @ts-ignore: __WB_MANIFEST is injected by the InjectManifest plugin
let manifestEntries: Array<PrecacheEntry> = self.__WB_MANIFEST;
// @ts-ignore: additionalManifestEntries is defined in sw-runtime-resources-precache.js
const additionalManifestEntries: Array<PrecacheEntry> = self.additionalManifestEntries;
if (additionalManifestEntries && additionalManifestEntries.length) {
  manifestEntries = [...manifestEntries, ...additionalManifestEntries];
}

precacheAndRoute(manifestEntries);
