importScripts('sw-runtime-resources-precache.js');
import {skipWaiting, clientsClaim} from 'workbox-core';
import {precacheAndRoute} from 'workbox-precaching';

skipWaiting();
clientsClaim();

// __WB_MANIFEST is injected by the InjectManifest plugin
let manifestEntries = self.__WB_MANIFEST;
// additionalManifestEntries is defined in sw-runtime-resources-precache.js
if (self.additionalManifestEntries && self.additionalManifestEntries.length) {
    manifestEntries = [...manifestEntries, ...self.additionalManifestEntries];
}

precacheAndRoute(manifestEntries);
