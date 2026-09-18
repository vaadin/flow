# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add new, unreleased changes here. -->

## [3.1.4] - 2019-04-26
* Fixed an issue where bare specifiers were not rewritten for dynamic imports after transforming them to AMD require statements.

## [3.1.3] - 2019-04-02
* Disabled `babel-plugin-minify-builtins` and `babel-plugin-minify-remove-undefined` due to errors in `babel-preset-minify`.

## [3.1.2] - 2019-03-01
* Added support for `treeshake` option to remove dead JavaScript code when bundling.

## [3.1.1] - 2018-11-12
* Fix issue with deadcode removal in babel-minify, by turning it off, as it can be error prone and result in removal of code which should not be removed and cause hard to debug errors. https://github.com/Polymer/tools/issues/724

## [3.1.0] - 2018-10-15
* Added `wct-mocha` to the set of recognized WCT client-side packages in `htmlTransform` when using [@polymer/esm-amd-loader](https://github.com/Polymer/tools/tree/master/packages/esm-amd-loader).

## [3.0.4] - 2018-06-28
* Fix NPM audit warnings.

## [3.0.3] - 2018-06-25
* Service Worker generation uses a consistent spacing for anonymous
  functions (i.e. space between keyword and parentheses `function ()`)
  ensuring Node 8 and 10 output are identical.

## [3.0.2] - 2018-06-19
* Fix incorrect relative paths to the component directory in push manifests.
* Fix push manifest generation crash with ES module projects.

## [3.0.1] - 2018-05-14
* Pin dependency babel-plugin-minify-guarded-expressions of
  babel-preset-minify to known working version 0.4.1.

## [3.0.0] - 2018-05-08
* Updated dependencies.

## [3.0.0-pre.17] - 2018-05-03
* The transform for `import.meta` now uses the special `"meta"` dependency
  provided by
  [@polymer/esm-amd-loader](https://github.com/Polymer/tools/tree/master/packages/esm-amd-loader),
  instead of injecting a static path. As a result, it is now always coupled with
  the AMD transform, and cannot be enabled independently.
* The [regenerator runtime](https://github.com/facebook/regenerator) can now
  be injected either inline each time it is used by `jsTransform`, or inline
  into the HTML document by `htmlTransform`.
* Added es5, es2015, es2016, es2017, and es2018 compile targets to `jsTransform`
  to allow fine grained control over what features to compile.

## [3.0.0-pre.16] - 2018-05-01
* Disable the `simplify` babel plugin when minifying javascript. See
  https://github.com/babel/minify/issues/824
* Disable the `mangle` babel plugin as well. See
  https://github.com/Polymer/tools/issues/261
* Add "webcomponents-bundle.js" to the heuristic used to determine when to
  inject the Custom Elements ES5 Adapter.
* Dropped support for node v6. This is a soft break, as we aren't
  making any changes that are known to break node v6, but we're no longer testing against it. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support)
  for details.
* Replaced RequireJS AMD loader with
  [@polymer/esm-amd-loader](https://github.com/Polymer/tools/tree/master/packages/esm-amd-loader),
  which is smaller and better emulates the behavior of ES modules.

## [3.0.0-pre.15] - 2018-04-25
* `@babel/preset-es2015` has been replaced with a manually-created version so that `@babel/plugin-transform-classes` can be pinned to v7.0.0-beta.35 to avoid a bug where the polyfilled HTMLElement constructor is not called. (https://github.com/babel/babel/issues/7506)

## [3.0.0-pre.14] - 2018-04-23
* Disable Babel `minify-constant-folding` plugin when minifying. This plugin has a bug that breaks when a constant is exported from a module (https://github.com/babel/minify/issues/820).

## [3.0.0-pre.13] - 2018-04-18
* Fix node module resolution for the case where the root package is served from
  the components/ directory and imports a module from its own package using a
  path.

## [3.0.0-pre.12] - 2018-04-17
* The Babel helpers script now includes all Babel helpers that could be used by the ES5 compilation and AMD transforms.
* The `jsTransform` function's `injectBabelHelpers` option now has 3 possible values: `none` (default), `full`, and `amd`.
* Inline JavaScript will now only be transformed to AMD modules if they have type=module.
* External JavaScript files will now only be transformed to AMD modules if they contain module import/export syntax.

## [3.0.0-pre.11] - 2018-04-11
* Fix AMD transform bug where if an HTML document had multiple type=module scripts, and any of them (apart from the first) had any kind of import, then that import was not accessible (because it was mapped to the wrong module callback function argument).

## [3.0.0-pre.10] - 2018-04-09
* [breaking] The `jsTransform` function will now inline any required Babel helper functions by default. Previously they were always omitted. Added `externalHelpers` option to disable inlining. Note that the behavior of the build transformers from `getOptimizeStreams` continue to never inline the Babel helpers.

## [3.0.0-pre.9] - 2018-04-05
* Fix bug with node module resolution where specifiers in dynamic import() were rewritten.
* Fix bug with dynamic import rewriting where imported require function could not be called.

## [3.0.0-pre.8] - 2018-04-04
* Fix bug where not all percent-encoded characters in URIs would be decoded (in particular, `%40` -> `@` which is important for scoped NPM packages).

## [3.0.0-pre.7] - 2018-04-03
* [breaking] Rename `jsTransform` and `getOptimizeStreams` option from `transformEsModuleToAmd` to `transformModuleToAmd`.
* [breaking] The `transformModulesToAmd` option in `jsTransform` now automatically implies `transformImportMeta`, and throws if `transformImportMeta` is set to false.
* [breaking] The `JsTransform` class now takes a full `OptimizeOptions` instead of just a `JsOptimizeOptions`.

## [3.0.0-pre.6] - 2018-04-02
* Add an `import.meta` transform to `jsTransform` with the `transformImportMeta` option.
* Add a dynamic import() transform to AMD require()

## [3.0.0-pre.5] - 2018-03-28
* AMD loader will now only be injected into an HTML document if it contains at least one `type=module` script.
* Added `softSyntaxError` option to `jsTransform`. If set, Babel parse errors will no longer throw. Instead, a console error will be logged, and the original JS returned.
* Expose `htmlTransform` from main package index.

## [3.0.0-pre.4] - 2018-03-28
* ES to AMD module transformation is now supported by `getOptimizeStreams` and `htmlTransform`. Additionally:
  * Will now inject an inline minified RequireJS AMD loader, and the Babel helpers.
  * Phantom `<html>`, `<head>`, and `<body>` tags injected by parse5 are now removed.

## [3.0.0-pre.3] - 2018-03-28
* Upgraded to `polymer-bundler@4.0.0-pre.3` which brings ES6 module bundling to the build pipeline!  Upgraded to `polymer-analyzer@3.0.0-pre.18`.

## [3.0.0-pre.2] - 2018-03-26
* Add `htmlTransform` function, factored out of the Polyserve `compile-middleware` module.
* Add ordered execution to the ES to AMD module transformation in `htmlTransform`.
* Node module specifier rewriter will now resolve paths according to the node module resolution algorithm, not just bare specifiers (e.g. "./foo" resolves to "./foo.js").

## [3.0.0-pre.1] - 2018-03-21
* Upgraded to `polymer-analyzer@3.0.0-pre.17` and `polymer-bundler@4.0.0-pre.2`.
* Converted from `PackageUrlResolver` to `FsUrlResolver` as more appropriate to application build context.
* Ignore `not-loadable` warnings, as polymer-build should not load remote
  resources.

## [2.5.0] - 2018-03-21
* Add `packageName` option to `jsTransform()` function. Option is required when
  `isComponentRequest` option is true.

## [2.4.1] - 2018-03-20
* Fix import specifier rewriting when importing a dependency from a top-level module.

## [2.4.0] - 2018-03-19
* Fix dependency specification for `babel-core`
* Add `jsTransform` function, factored out of `optimize-streams` module (so that it can be shared with Polyserve).
* Renamed `jsTransform` option from `compile` to `compileToEs5` to clarify its behavior.
* Added `transformEsModulesToAmd` option to `jsTransform` and JS stream transformer.
* Add exponentiation, async/await, and async generator syntax/transform support to `jsTransform`.

## [2.3.3] - 2018-03-14
* Don't run Babel at all if there are no meaningful changes to make.

## [2.3.2] - 2018-03-13
* Fix bug where JS compilation/minification would ignore the "excludes" field.

## [2.3.1] - 2018-03-12
* Restore ES5 template literal uniquifying feature from https://github.com/Polymer/polymer-cli/pull/962 that was missed in earlier code migration.
* Allow "none" moduleResolution option in JS compile transform.

## [2.3.0] - 2018-03-12
* JS compile and other "optimize streams" build transformers have been moved from polyer-cli into this package.
* JS compile build transformer wil now rewrite bare module specifiers to paths.
* Module bare specifier rewriting Babel plugin has been moved from polyserve into this package.
* Module bare specifier rewriting Babel plugin now works on Windows, does not rewrite fully qualified URLs, and will follow the "module" or "jsnext:main" fields when a package.json uses them instead of "main".

## [2.2.0] - 2018-02-23
* Fixed issue where the build silently fails when several source dependencies are missing
* If the ProjectConfig given to BuildAnalyzer has a `componentDir`, pass a PackageUrlResolver using that `componentDir` to the underlying Analyzer.
* Warnings indicated in the `lint.warningsToIgnore` ProjectConfig option are now ignored.

## [2.1.1] - 2017-10-23
* Updated `polymer-bundler` to 3.1.1, to fix an issue with deprecated CSS imports being inlined into the wrong templates.

## [2.1.0] - 2017-10-02
* Updated `polymer-bundler` to 3.1.0, which inlines external stylesheet links in templates.

## [2.0.0] - 2017-07-18
* [Breaking] Upgraded `polymer-bundler` to 3.x, which includes new default behavior around the `rewriteUrlsInTemplates` option in support of Polymer 2.x defaults.  Polymer 1.x project developers should set the option `rewriteUrlsInTemplates: true`.  See [using polymer-bundler programmatically](https://github.com/polymer/polymer-bundler#using-polymer-bundler-programmatically) for more information.

## [1.6.0] - 2017-06-29
* Automatically recognize any lazy-imports encountered as fragments when generating push-manifest.
* The `addPushManifest` feature now honors the laziness of html-imports and excludes them from the set of their importers' pushed assets.
* Upgraded Polymer Bundler to 2.2.0, which updated the shell strategy so that the shell is no longer linked to from other bundles. See [Bundler issue #471](https://github.com/Polymer/polymer-bundler/issues/471) for more details.

## [1.5.1] - 2017-06-02
* Prefetch links are now only added for transitive dependencies.

## [1.5.0] - 2017-05-23
* Service Worker generator now generates relative URLs for pre-cached assets instead of absolute. This makes it possible to cache assets when the app is served from a non-root path without re-mapping all URLs. Since server workers fetch relative to their own URL, there is no effective change for service workers served from the root path.
* Service Worker generator now better caches the entrypoint by setting navigateFallback and related options.

## [1.4.2] - 2017-05-19
* Updated the AddPrefetchLinks transform to not insert html root element tags like `<html>`, `<head>` or `<body>`.

## [1.4.1] - 2017-05-18
* Updated dependency on `polymer-bundler` to use official 2.0.0 release and enable unmediated semver upgrades.
* Fixed issue with push manifest URLs being a mix of relative and absolute URLs (now always relative), and a double-delimiter issue when using basePath.

## [1.4.0] - 2017-05-18

* Added `PolymerProject.addPrefetchLinks()` transform.
* Added `PolymerProject.addBabelHelpersInEntrypoint()` transform.

## [1.3.1] - 2017-05-16

* Updated polymer-project-config dependency.

## [1.3.0] - 2017-05-15

* A prefix can now be passed to `addPushManifest()`, which will be prepended to all push manifest resource paths.
* A basePath can now be passed to the service worker generator, where it will be used as the sw-precache replacePrefix.
* Upgrade to Polymer Analyzer that changes many errors to warnings for more robust builds.

## [1.2.5] - 2017-05-15

* Updated `polymer-analyzer` dependency to `^2.0.0` now that it is out of alpha.

## [1.2.4] - 2017-05-11

* Simplify addCustomElementsEs5Adapter() adapter injection method.
* Bundler stream no longer emits any CSS or Javascript files which have been inlined into bundles.

## [1.2.3] - 2017-05-10

* Dependency updates. Fixes issue with `polymer-bundler`'s handling of `lazy-import` links in `dom-modules`.

## [1.2.2] - 2017-05-09

* Dependency updates. Update to `sw-precache@5` to prevent "corrupted data" errors on Firefox 52 and Chrome 59 when using `addServiceWorker()`.  Upgraded `polymer-bundler` and `polymer-analyzer` to address `lazy-import` bugs.

## [1.2.1] - 2017-05-03

* Dependency updates. Upgraded to new `polymer-bundler`.  Most important update is a fix to bug whereby `lazy-import` links were being moved out of their `<dom-module>` containers.

## [1.2.0] - 2017-05-02

* Dependency updates.  Upgraded to new `polymer-bundler`, `polymer-analyzer` and `dom5` versions.
* Fixed bug where `<html>`, `<head>` and `<body>` were added to documents mutated by `HtmlSplitter` and the `CustomElementsES5AdapterInjector`.
* Print build-time warnings and errors with full location information, and precise underlines of the place where the problem was identified.
* Fixed issue where two copies of entrypoint files with same name are emitted by bundler stream: an un-bundled one, followed by bundled one.
* Fixed issue where html imports were emitted by bundler as individual files even though they were bundled.
* Added an options argument to `PolymerProject#bundler()` to support users configuring the bundler.  Options include all `Bundler#constructor` options, `analyzer`, `excludes`, `inlineCss`, `inlineScripts`, `rewriteUrlsInTemplates`, `sourcemaps`, `stripComments`, as well as `strategy` and `urlMapper` which are used on call to `Bundler#generateManifest`.

## [1.1.0] - 2017-04-14

* Add `addCustomElementsEs5Adapter()` method to PolymerProject. Provides an adapter needed when serving ES5 to browsers that support the native Custom Elements API.

## [1.0.0] - 2017-04-11

* [Breaking] Remove Node v4 support: Node v4 is no longer in Active LTS, so as per the [Polymer Tools Node.js Support Policy](https://www.polymer-project.org/2.0/docs/tools/node-support) polymer-build will not support Node v4. Please update to Node v6 or later to continue using the latest verisons of Polymer tooling.
* New Feature: add automatic [HTTP/2 push manifest](https://github.com/GoogleChrome/http2-push-manifest) generation for HTTP/2 Push-enabled servers.

## [0.9.1] - 2017-03-20

* Fixed issue with Service Worker generation in Windows environment where full paths were put into its precacheConfig instead of relative paths.
* Bundled files always use canonical platform separators in their paths now.  Previously, files might have either back-slashes or forward-slashes on Windows, depending on how they arrived at the Bundler and this caused the Analyzer to treat files as missing when mapping them by path.

## [0.9.0] - 2017-03-15

* [breaking] PolymerProject's `bundler` property is now a `bundler()` method, returning a new BuildBundler stream on each call.  This is to support parallel pipelines using bundler.

## [0.8.4] - 2017-03-04

* Build now delegates authority to the Analyzer for what urls are package external instead of local heuristics.
* Bundling now processes files coming in from streams, to support things like js-minification before bundling.

## [0.8.3] - 2017-03-03

* Dependency updates.

## [0.8.2] - 2017-02-24

* Dependency updates

## [0.8.1] - 2017-02-17

* Update the version of `polymer-bundler` to fix several bugs:
 * Fix regressions in url attribute updating (src, href, assetpath).
 * Added support for `<base>` href and target attribute emulation on bundled output.
 * Whitespace-agnostic license comment deduplication.
 * Server-side includes no longer stripped as comments.

## [0.8.0] - 2017-02-14

* `project.splitHtml()` & `project.rejoinHtml()` methods have been pulled off of `PolymerProject` so that multiple streams can split/rejoin in parallel. See [the new README section on `HTMLSplitter`](https://github.com/Polymer/polymer-build#handling-inlined-cssjs) for updated instructions on how to split/rejoin inline scripts and styles in your build stream.
* Completed the migration away from `hydrolysis` to `polymer-analyzer` and `vulcanize` to `polymer-bundler`.
* Do a better job of only listing warnings for code in the package being built.

## [0.7.1] - 2017-02-03

* Fix issue where larger projects would cause the sources stream to hang.

## [0.7.0] - 2017-01-31

* `BuildAnalyzer.sources` & `BuildAnalyzer.dependencies` are now `BuildAnalyzer.sources()` & `BuildAnalyzer.dependencies()`, respectively. This change only affects uses who are importing and/or using the `BuildAnalyzer` class directly.
* Fix issue where files were being loaded immediately, before the build stream was started.


## [0.6.0] - 2017-01-04

* Fix issue where missing source files were causing silent stream failures.
* **Interface Update!** `PolymerProject.analyzer` is no longer a required step in your build pipeline. Instead, analysis happens automatically while it fills the `project.sources()` and `project.dependencies()` streams with your project files. See [the README](/README.md) for updated examples of what build streams look like without the analyzer.
* **[`merge-stream`](https://www.npmjs.com/package/merge-stream) users:** Update to v1.0.1 or later if you are using `merge-stream` with `polymer-build`. Stream errors do not propagate properly in previous versions of the library, and your build task may silently fail as a result.
* `StreamAnalyzer` is now `BuildAnalyzer` (since it is no longer a stream). This change only affects uses who are importing and/or using `StreamAnalyzer` directly from the `polymer-build` module.
* Fix issue where behaviors were not being analyzed properly.
* Update the version of polymer-analyzer we use, fixing a number of errors.

<details>
  <summary><strong>v0.6.0 Pre-Release Changelog</strong></summary><p>

### [0.6.0-alpha.3] - 2016-12-19

* Actually update the version of polymer-analyzer we use, fixing a number of errors. (alpha.2 was accidentally a noop release).

### [0.6.0-alpha.2] - 2016-12-19

* Update the version of polymer-analyzer we use, fixing a number of errors.

### [0.6.0-alpha.1] - 2016-12-13

* **Interface Update!** `PolymerProject.analyzer` is no longer a required step in your build pipeline. Instead, analysis happens automatically while it fills the `project.sources()` and `project.dependencies()` streams with your project files. See [the README](/README.md) for updated examples of what build streams look like without the analyzer.
* **[`merge-stream`](https://www.npmjs.com/package/merge-stream) users:** Update to v1.0.1 or later if you are using `merge-stream` with `polymer-build`. Stream errors do not propagate properly in previous versions of the library, and your build task may silently fail as a result.
* `StreamAnalyzer` is now `BuildAnalyzer` (since it is no longer a stream). This change only affects uses who are importing and/or using `StreamAnalyzer` directly from the `polymer-build` module.
* Fix issue where behaviors were not being analyzed properly.

</p></details>

## [0.5.1] - 2016-12-02

* Updated polymer-analyzer to `2.0.0-alpha.18`

## [0.5.0] - 2016-11-01

* **New Analyzer!** Should fix most reported bugs that were caused by bad analysis, but may introduce new ones. Be sure to test your build after upgrading to confirm that your build is still functioning. See [`polymer-analyzer`](https://github.com/Polymer/polymer-analyzer) for more information.
  * Fixed silent failures during build analysis.
  * Added warning printing during build analysis (#54).
* Added support for relative `root` paths.
* Renamed two `AddServiceWorkerOptions` properties:
 * `serviceWorkerPath` was renamed to `path`.
 * `swConfig` was renamed to `swPrecacheConfig`.
 * Old names are deprecated, and support for them will be removed in future versions.
* polymer.json configuration now managed by [`polymer-project-config`](https://github.com/Polymer/polymer-project-config)
* Upgrade outdated dependencies:
  * `sw-precache@4.2.0` generates a new kind of service-worker that will require all users to repopulate their cache. Otherwise it continues to behave the same as before.

## [0.4.1] - 2016-08-24

### Fixed
* No longer modifies the object passed to `generateServiceWorker()` – https://github.com/Polymer/polymer-build/pull/27

## [0.4.0] - 2016-08-05

### Fixed
* Don't halt building when encountering imports of absolute URLs (i.e. https://example.com/font.css).

### Added
* Generate `.d.ts` files for typescript users.
