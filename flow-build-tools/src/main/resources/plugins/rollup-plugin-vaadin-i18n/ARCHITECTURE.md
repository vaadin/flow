# Translations for Hilla views

This note lives next to the Rollup plugin that splits the translation keys
by chunk, but it covers the whole Flow side of i18n for client-side views:
the build plugin, the translation endpoint and the provider behind it. The
browser runtime, `@vaadin/hilla-react-i18n`, is described in
`packages/ts/react-i18n/ARCHITECTURE.md` in the Hilla repository.

## Overview

Flow and Hilla views share one set of translations: the
`vaadin-i18n/translations*.properties` files on the classpath, read through
the application's `I18NProvider`. A Flow view calls `getTranslation(key)` on
the server. A Hilla view calls `translate(key`…`)` in the browser, and the
browser loads translations from the server with a request:

```
GET ./?v-r=i18n&langtag=de-DE[&chunks=<chunk file>…][&keys=<key>…]
```

`TranslationFileRequestHandler` answers with a flat JSON object of keys and
translated strings.

What the browser asks for differs between development and production:

- **In development**, it asks for the whole language: no `chunks`, no `keys`.
  All translations of the locale are returned, and editing a properties file
  sends a `translations-update` HMR event that makes the browser reload them.
- **In production**, a large application should not ship every string to
  every page. The build records which keys each JS chunk uses, and when a lazy
  chunk loads, the browser asks only for that chunk's keys.

## Design goals

- **One source of translations** for both kinds of views, through
  `I18NProvider`. A custom provider works for Hilla views too.
- **Load only what the page uses** in production, without asking the
  application to declare anything. The split follows the bundler's own
  chunks.
- **Nothing to do in development.** No build step is needed, and edits show
  up live.
- **No session for translations.** The request is served by a
  `SessionlessRequestHandler`, so loading a login page in another language
  does not create a session.

## The pieces

| Piece | Where | Role |
| ----- | ----- | ---- |
| `rollup-plugin-vaadin-i18n.js` (this folder) | `flow-build-tools/src/main/resources/plugins/` | Production-only Vite plugin. Collects keys per chunk, injects `i18n.registerChunk(<chunk file>)` into each chunk that uses keys, and writes `i18n.json`. |
| `vite.generated.ts` | `flow-server/src/main/resources/` | Enables the plugin only when `productionMode`, with the output folder `settings.i18nOutput`. |
| `TaskUpdateSettingsFile` | `flow-build-tools/.../server/frontend/` | Sets `i18nOutput` to `<build>/classes/vaadin-i18n`, so `i18n.json` ends up on the classpath next to the properties files. |
| `TranslationFileRequestHandler` | `flow-server/.../flow/i18n/` | Handles `?v-r=i18n`: resolves `chunks` to keys through `vaadin-i18n/i18n.json`, adds the requested `keys`, and asks the provider. |
| `I18NProvider` | same | `getAllTranslations(locale)` and `getTranslations(keys, locale)`, used only by this handler. The defaults return nothing and translate key by key. |
| `DefaultI18NProvider`, `I18NUtil` | same | The provider created automatically when `vaadin-i18n/translations*.properties` exist (`DefaultInstantiator`). It reads `ResourceBundle`s with a no-fallback control. |
| `DefaultTranslationsHotswapper` | `vaadin-dev-server` | On a change to `vaadin-i18n/*.properties`, clears the bundle cache and sends `translations-update`. |

## Build: keys per chunk

The plugin works in three Rollup hooks:

1. **`transform`, per module.** It parses modules that import from
   `@vaadin/hilla-react-i18n`:
   - A ``key`…` `` tag inside a `translate(…)` call is recorded as a key of
     that module (in the module's `meta.vaadinI18n.keys`).
   - A ``key`…` `` anywhere else, typically a view's `ViewConfig` title, is
     recorded for the main chunk. The menu needs it before any view chunk has
     loaded.
   - A module with keys gets `await i18n.registerChunk('<marker>')` prepended
     (and the `i18n` import, if missing). The real chunk name isn't known
     yet at this point.
2. **`renderChunk`, per output chunk.** It keeps the first `registerChunk`
   call of the chunk with the marker replaced by the chunk's file name, and
   removes the rest. One call loads the keys of every module in the chunk.
   This happens here, not in `generateBundle`, so that the bundler composes
   the source maps.
3. **`generateBundle`.** It collects the keys of all modules in each chunk.
   The main-chunk keys are added to the chunk whose name contains
   `indexhtml`. It writes `{ "chunks": { "<file>": { "keys": [...] } } }` as
   `i18n.json`.

## Runtime: serving translations

`TranslationFileRequestHandler`:

1. parses `langtag` (`de-DE` or `de_DE`) into a `Locale`;
2. turns each `chunks` value into its keys from `i18n.json` (read once,
   lazily, through the class loader), and adds the `keys` values;
3. calls `getAllTranslations(locale)` when no key was requested (development
   mode), otherwise `getTranslations(keys, locale)`;
4. answers with the JSON and an `X-Vaadin-Retrieved-Locale` header, or with
   404 when there is nothing to return. Without an `I18NProvider`, it answers
   501 in development mode (with a message) and 404 in production mode.

Choosing the bundle for a locale is up to the provider. `DefaultI18NProvider`
uses `ResourceBundle` lookup with a no-fallback control: exact match, then
language, then the root `translations.properties`, but never the bundle of
the JVM default locale. A missing key is returned as `!<language>: <key>`,
the same marker the browser runtime uses.

## Decisions and gotchas

- **Production only.** In development, the plugin isn't added and no
  `i18n.json` is written (#21568). A stale `i18n.json` left on the classpath
  from an earlier production build doesn't matter, because development
  requests carry no `chunks`.
- **Matching is literal.** Only ``key`literal` `` with no interpolation is
  recognised, and only when `key` and `translate` are imported from
  `@vaadin/hilla-react-i18n` (renamed imports are followed).
  `translateDynamic` with a computed key is not, so the browser loads such a
  key on demand with `keys=` and logs a warning.
- **The main chunk is found by name** (`indexhtml`). Renaming the entry chunk
  in the Vite config would drop the `ViewConfig` keys.
- **The response header echoes the request.** Since the handler delegates
  to `I18NProvider` (#21636), it no longer knows which bundle answered.
  `X-Vaadin-Retrieved-Locale` carries the requested locale.
- **The class loader matters.** The provider and the handler use the
  service's class loader. In Spring Boot, `DefaultI18NProviderFactory` uses
  the thread context class loader, so Spring Boot devtools restarts pick up
  changed translations (#19791).
- **Dependencies.** The plugin's npm dependencies (`transform-ast`,
  `magic-string`) are not installed from its `package.json`. They are listed
  with the Vite build dependencies in
  `flow-server/src/main/resources/com/vaadin/flow/server/frontend/dependencies/vite/package.json`
  (#21574).

## Working on it

- Unit tests: `TranslationFileRequestHandlerTest`, `I18NUtilTest`,
  `DefaultInstantiatorI18NTest` in `flow-server`.
- Integration test: `I18nChunkIT` in `flow-tests/test-frontend/vite-production`.
- End-to-end tests with Hilla views are in the Hilla repository
  (`packages/java/tests/spring/react-i18n`).
- The `?v-r=i18n` contract (`langtag`, `chunks`, `keys`, the header) is shared
  with `DefaultBackend` in `@vaadin/hilla-react-i18n`. Change both together.
