# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add new, unreleased items here. -->

## [0.27.15] (2019-01-10)
* Add `--allow-origin` flag which sets the `Access-Control-Allow-Origin` header.

## [0.27.14] (2018-12-19)
* Fix `compile-cache` cli option not being respected.

## [0.27.13] (2018-10-15)
* Allow file requests to specify `?nocompile` to prevent any compilation

## [0.27.12] (2018-06-28)
* Updated dependencies.

## [0.27.11] (2018-05-08)
* Updated dependencies.

## [0.27.10] (2018-05-03)
* The transform for `import.meta` now uses the special `"meta"` dependency
  provided by
  [@polymer/esm-amd-loader](https://github.com/Polymer/tools/tree/master/packages/esm-amd-loader),
  instead of injecting a static path. As a result, it is now always coupled with
  the AMD transform, and cannot be enabled independently.
* Fixed issue where the package name was only a substring of a request path was
  being considered a root path request. e.g. a package of `@foo` with a request
  path os `/@foo/bar` is considered a root path request, but a request path of
  `/@foobar/baz` is not but was being considered one before.
* The [regenerator runtime](https://github.com/facebook/regenerator) is now
  injected into projects that are compiling to ES5.

## [0.27.9] (2018-05-01)
* Dropped support for node v6. This is a soft break, as we aren't
  making any changes that are known to break node v6, but we're no longer testing against it. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support)
  for details.
* Fixed issue where resources would be cached after restarting polyserve with
  different compilation/transformation options. We've turned off most
  browser-side caching. As a reminder: do not use polyserve in production,
  it is designed for development.
* Add "webcomponents-bundle.js" to the heuristic used to determine when to
  inject the Custom Elements ES5 Adapter.
* Replaced RequireJS AMD loader with
  [@polymer/esm-amd-loader](https://github.com/Polymer/tools/tree/master/packages/esm-amd-loader),
  which is smaller and better emulates the behavior of ES modules.

## [0.27.8] (2018-04-25)
* `@babel/preset-es2015` has been replaced with a manually-created version so that `@babel/plugin-transform-classes` can be pinned to v7.0.0-beta.35 to avoid a bug where the polyfilled HTMLElement constructor is not called. (https://github.com/babel/babel/issues/7506)


## [0.27.7] (2018-04-23)
* Stricter requirements for determining when a browser supports modules, and
  hence when to automatically transform modules to AMD. We now require support
  for dynamic import and import.meta.

## [0.27.6] (2018-04-18)
* Fix node module resolution for the case where the root package is served from
  the components/ directory and imports a module from its own package using a
  path.

## [0.27.5] (2018-04-17)
* Update --npm flag description.

## [0.27.4] (2018-04-11)
* Fix AMD transform bug where if an HTML document had multiple type=module scripts, and any of them (apart from the first) had any kind of import, then that import was not accessible (because it was mapped to the wrong module callback function argument).

## [0.27.2] (2018-04-10)
* Bring in latest polymer-analyzer and polymer-build fixes to node resolution
  and babel helpers.

## [0.27.0](https://github.com/PolymerLabs/polyserve/tree/0.26.0) (2018-04-03)
* Upgrade polymer-build to 3.0.0-pre.7

## [0.26.0](https://github.com/PolymerLabs/polyserve/tree/0.26.0) (2018-03-28)

* ES > AMD module transformation
  * AMD loader will now be injected as a minified inline script, instead of as
    an external script.
  * AMD modules will now execute in document script order.
* Babel helpers will now be injected as a single minified inline script into
  the HTML document, instead of into every JS script.
* Phantom `<html>`, `<body>`, and `<head>` elements will no longer be
  introduced into HTML.

## [0.25.3](https://github.com/PolymerLabs/polyserve/tree/0.25.3) (2018-03-26)

* Node module specifier rewriter will now resolve paths according to the node
  module resolution algorithm, not just bare specifiers (e.g. "./foo" resolves
  to "./foo.js").

## [0.25.2](https://github.com/PolymerLabs/polyserve/tree/0.25.1) (2018-03-21)

* Another fix for import specifier rewriting when given a relative componentDir.

## [0.25.1](https://github.com/PolymerLabs/polyserve/tree/0.25.1) (2018-03-21)

* Fix import specifier rewriting when given a relative componentDir.

## [0.25.0](https://github.com/PolymerLabs/polyserve/tree/0.25.0) (2018-03-20)

* Module bare specifier rewriting Babel plugin now works on Windows, does not rewrite fully qualified URLs, and will follow the "module" or "jsnext:main" fields when a package.json uses them instead of "main".
* Add exponentiation, async/await, and async generator support when compiling to ES5.
* Fix import specifier rewriting when importing a dependency from a top-level module.

## [0.24.0](https://github.com/PolymerLabs/polyserve/tree/0.24.0) (2018-02-23)

* Honor `.bowerrc` for bower component directory and variants
* Root directories can now be relative paths
* Added --module-resolution flag to allow rewriting of "bare" module specifiers to web-compatible paths.

## [0.23.0](https://github.com/PolymerLabs/polyserve/tree/0.23.0) (2017-10-02)

* Added ability to modify and/or substitute generated express apps for when calling startServers, to support more flexibility in request handler ordering and middleware scenarios.
* Fix issue with module scripts being compiled when a nomodule script exists.
* Updated polymer-build to 2.1.0 which includes polymer-build 3.1.0 which inlines external stylesheet links in templates.

## [0.22.1](https://github.com/PolymerLabs/polyserve/tree/v0.22.1) (2017-09-19)

* Fixes issue with the babel compile cache where different compilation options were using the same namespace. ([#216](https://github.com/Polymer/polyserve/issues/216)).

## [0.22.0](https://github.com/PolymerLabs/polyserve/tree/v0.22.0) (2017-09-18)

* Identify wct-browser-legacy as a web-component-tester client package, to add a hook for deferring mocha execution in support of requirejs.
* Change `x(...args)` syntax to `x.apply(undefined, arguments)` to support IE11.

## [0.21.9](https://github.com/PolymerLabs/polyserve/tree/v0.21.9) (2017-09-15)

* Fix issue where requirejs is installed somewhere other than in polyserve's own node_modules subfolder.
* Fix issue with `.npmignore` file and npm pack logic change in recent npm release.

## [0.21.0](https://github.com/PolymerLabs/polyserve/tree/v0.21.0) (2017-09-13)

* Auto-compile now includes transformation of ES modules to AMD modules with RequireJS for browsers that do not support ES modules. Includes special handling for WCT.
* Fix #48 where we would incorrectly inject the ES5 adapter for browsers that supported ES2015 but not modules.

## [0.20.0](https://github.com/PolymerLabs/polyserve/tree/v0.20.0) (2017-08-08)

* Update Polymer Build to v2.0.0
* Allow package names to include a '/'
* Added --npm flag which sets component folder to node_modules and reads package name from package.json.

## [0.19.1](https://github.com/PolymerLabs/polyserve/tree/v0.19.1) (2017-05-22)

* Allow webcomponentsjs polyfill *tests* to be compiled.

## [0.19.0](https://github.com/PolymerLabs/polyserve/tree/v0.19.0) (2017-05-08)

* Add auto-compile support for the Mobile Safari browser.
* Add auto-compile support for the Vivaldi browser.
* Fixed issue when serve with --push-manifest ([#168](https://github.com/Polymer/polyserve/issues/168))
* Add gzip and deflate HTTP response compression.

## [0.18.0](https://github.com/PolymerLabs/polyserve/tree/v0.18.0) (2017-04-18)

* When compiling to ES5, inject the Custom Elements ES5 Adapter into any HTML
  file where the web components polyfill is included (typically the entry
  point). This adapter is needed when serving ES5 to browsers that support the
  native Custom Elements API
  ([#164](https://github.com/Polymer/polyserve/issues/164)).
* A server can now be started with a custom entry point; previously index.html
  was always assumed ([#161](https://github.com/Polymer/polyserve/issues/161)).

## [0.17.0](https://github.com/PolymerLabs/polyserve/tree/v0.17.0) (2017-04-13)

* Add auto-compile support for the Chromium browser.
* Add auto-compile support for the Opera browser.
* Require ES5 compilation for Edge < 40 ([#161](https://github.com/Polymer/polyserve/issues/161)).
* Fixed issue where directory paths redirected to the file system ([#96](https://github.com/Polymer/polyserve/issues/96)).
* [Breaking] Remove Node v4 support: Node v4 is no longer in Active LTS, so as per the [Polymer Tools Node.js Support Policy](https://www.polymer-project.org/2.0/docs/tools/node-support) polyserve will not support Node v4. Please update to Node v6 or later to continue using the latest verisons of Polymer tooling.

## [0.16.0](https://github.com/PolymerLabs/polyserve/tree/v0.16.0) (2017-02-14)

### Added
* `-u`/`--component-url` option to support expressing different url to
  fetch components from than `components`.

### Fixed
* When no port is given, do a better job of finding an available port.
 * Uses the list of [sauce-legal](https://wiki.saucelabs.com/display/DOCS/Sauce+Connect+Proxy+FAQS#SauceConnectProxyFAQS-CanIAccessApplicationsonlocalhost?) ports.

## [0.15.0](https://github.com/PolymerLabs/polyserve/tree/v0.15.0) (2016-11-28)

### Added

* Compile JavaScript with Babel for browsers that don't support ES6. A new
`--compile` flag controls compilation, with valid values of 'always', 'never',
and 'auto'; and a default of 'auto'.
* If the root dir contains directories that look like `bower_components-${foo}`,
they will be treated as `dependency variants`. In that case, polyserve will
start one server for each variant, enabling testing and development of your code
against each set of dependencies.

## [0.14.0](https://github.com/PolymerLabs/polyserve/tree/v0.14.0) (2016-11-17)

* Added support for HTTP2 and HTTP2 Push. PR #98
* TypeScript declaration fixes

## [0.13.0](https://github.com/PolymerLabs/polyserve/tree/v0.13.0) (2016-09-30)

### Fixed
* Return a 404 for missing resources with known file extensions rather than serving out `index.html`. PR #102

## [0.12.0](https://github.com/PolymerLabs/polyserve/tree/v0.12.0) (2016-05-17)

### Fixed
* The `openPath` option should override the component path. PR #95

## [0.11.0](https://github.com/PolymerLabs/polyserve/tree/v0.11.0) (2016-05-12)

### Added
* Option to customize the URL top open the browser to. #81

### Fixed
* Don't serve out the `index.html` shell file for paths ending in `.html`.
* Make the browser opening behavior more configurable and predictable. #81
