/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import {skipWaiting, clientsClaim} from 'workbox-core';
import {matchPrecache, precacheAndRoute} from 'workbox-precaching';
import {NavigationRoute, registerRoute} from 'workbox-routing';
import {PrecacheEntry} from 'workbox-precaching/_types';
import {NetworkOnly} from 'workbox-strategies';

skipWaiting();
clientsClaim();

const appShellPath = '/';
// @ts-ignore: OFFLINE_PATH_ENABLED and OFFLINE_PATH are defined by webpack.generated.js
const offlinePath = OFFLINE_PATH_ENABLED ? '/' + OFFLINE_PATH : appShellPath;
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
