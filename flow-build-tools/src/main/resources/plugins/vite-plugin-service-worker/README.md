# vite-plugin-service-worker

Internal Vite plugin that builds the service worker (`sw.js`) of an
application with `@PWA` offline support. It compiles `sw.ts` in a separate
Vite build, injects the precache list of the frontend bundle, and serves and
live-updates the service worker in development mode.

It is copied into the application's build folder by
`TaskInstallFrontendBuildPlugins` and enabled in `vite.generated.ts` by
`TaskUpdateVite` when offline support is enabled.

How the whole `@PWA` feature works is described in
[ARCHITECTURE.md](ARCHITECTURE.md).
