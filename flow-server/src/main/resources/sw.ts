/// <reference lib="webworker" />

importScripts('sw-runtime-resources-precache.js');
import {skipWaiting, clientsClaim} from 'workbox-core';
import {precacheAndRoute} from 'workbox-precaching';
import {NavigationRoute, registerRoute} from 'workbox-routing';
import {PrecacheEntry} from 'workbox-precaching/_types';
import {NetworkFirst} from 'workbox-strategies';

skipWaiting();
clientsClaim();

// @ts-ignore: __WB_MANIFEST is injected by the InjectManifest plugin
let manifestEntries: Array<PrecacheEntry> = self.__WB_MANIFEST;
// @ts-ignore: additionalManifestEntries is defined in sw-runtime-resources-precache.js
const additionalManifestEntries: Array<PrecacheEntry> = self.additionalManifestEntries;
if (additionalManifestEntries && additionalManifestEntries.length) {
    manifestEntries = [...manifestEntries, ...additionalManifestEntries];
}

precacheAndRoute(manifestEntries);

const appShellCacheKey = '/';
const navigationFallback = new NavigationRoute(new NetworkFirst({
    plugins: [
        // Always use app shell cache key instead of original request URL
        { cacheKeyWillBeUsed: async () => appShellCacheKey },
    ]
}));
registerRoute(navigationFallback);
