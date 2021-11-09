importScripts("https://storage.googleapis.com/workbox-cdn/releases/3.6.3/workbox-sw.js");

workbox.precaching.suppressWarnings();

// self. _ _ WB_MANIFEST is default injection point
workbox.precaching.precacheAndRoute(self.__WB_MANIFEST)
