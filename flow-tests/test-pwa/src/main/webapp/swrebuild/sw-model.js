/*
 * Model service worker for the vaadin/flow#24800 reproduction IT.
 *
 * It mirrors the *policy* of Flow's generated service worker
 * (flow-server/src/main/resources/com/vaadin/flow/server/frontend/sw.ts):
 * precache the build's assets, serve them precache-first and fall through to
 * the network on a miss, and purge outdated precaches on activate (as workbox
 * does). Whether a freshly installed worker takes over an already-open page
 * mid-session - i.e. whether sw.ts calls self.skipWaiting() + clientsClaim() -
 * is supplied by the IT via the `takeover` query parameter, which the IT
 * derives from the real generated sw.js. That keeps this model faithful to the
 * actual platform behavior: the reproduction turns green automatically once
 * sw.ts stops taking over mid-session.
 *
 * The build identity (OLD/NEW) comes from the `build` query parameter:
 *   sw-model.js?build=OLD&takeover=...   an already-installed PWA
 *   sw-model.js?build=NEW&takeover=...   the worker shipped by a rebuild
 *
 * After a NEW build the old-hash bundle (bundle-old.js) is, like a redeployed
 * server, no longer available: a network fall-through for it yields 404.
 */
const params = new URL(self.location).searchParams;
const BUILD = (params.get('build') || 'OLD').toUpperCase();
const TAKEOVER = params.get('takeover') === 'true';

const CACHE = 'swrebuild-precache-' + BUILD;
const SCOPE = self.registration.scope; // .../swrebuild/
const bundle = BUILD === 'NEW' ? 'bundle-new.js' : 'bundle-old.js';
const shell = BUILD === 'NEW' ? 'shell-new.html' : 'shell-old.html';
const MANIFEST = [SCOPE + shell, SCOPE + bundle, SCOPE + 'common.js'];

self.addEventListener('install', (e) => {
  e.waitUntil(
    (async () => {
      if (TAKEOVER) {
        // Activate immediately instead of waiting for open pages to close.
        self.skipWaiting();
      }
      await caches.open(CACHE).then((c) => c.addAll(MANIFEST));
    })()
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    (async () => {
      // Drop outdated precaches, exactly like a new workbox precache revision.
      for (const k of await caches.keys()) {
        if (k.startsWith('swrebuild-precache-') && k !== CACHE) {
          await caches.delete(k);
        }
      }
      if (TAKEOVER) {
        // Take control of already-open pages mid-session.
        await self.clients.claim();
      }
    })()
  );
});

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);
  // Only handle this reproduction's own resources.
  if (!url.pathname.includes('/swrebuild/')) {
    return;
  }
  e.respondWith(
    (async () => {
      const cache = await caches.open(CACHE);
      const hit = await cache.match(e.request, { ignoreSearch: true });
      if (hit) {
        return hit;
      }
      // Network fall-through. After the rebuild the redeployed server no
      // longer has the old-hash bundle -> 404 (issue steps 6-8).
      if (BUILD === 'NEW' && url.pathname.endsWith('bundle-old.js')) {
        return new Response('gone', { status: 404 });
      }
      return fetch(e.request);
    })()
  );
});
