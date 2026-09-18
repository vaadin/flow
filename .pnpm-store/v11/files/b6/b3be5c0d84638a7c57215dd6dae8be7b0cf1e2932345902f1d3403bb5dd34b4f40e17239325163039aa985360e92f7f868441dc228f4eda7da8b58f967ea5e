# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add new, unreleased items here. -->

## 6.9.2 - 2018-12-23
* Fix gulpfile to actually build browser.js.

## 6.9.1 - 2018-12-19
* Update WCT dependencies to reduce vulnerabilities in npm security audit.

## 6.9.0 - 2018-10-25
* Official release with `wct-mocha` support after verifying it in active projects.

## 6.9.0-pre.1 - 2018-10-15
* Added support for `wct-mocha` as an npm package.
* The `wctPackageName` config option is now optional as `wct` will scan for appropriate installed client-side package (`wct-mocha`, `wct-browser-legacy` or `web-component-tester`).

## 6.8.0 - 2018-08-15
* Fixed client-side memory leak.
* Updated wct-sauce to v2.1.0, which changes the set of browsers when testing
  on Travis CI.

## 6.7.1 - 2018-06-25
* Converted from promisify-node to native Node version of promisify.  Officially breaks support for node v6.

## 6.7.0 - 2018-06-19
* Updated Mocha to v5
* Updated wct-st to *not* force-exit process after `runSauceTunnel()` promise is rejected/resolved. 
* Fixed incorrect CLI arguments

## 6.6.0 - 2018-05-08
* Dropped support for node v6. This is a soft break, as we aren't
  making any changes that are known to break node v6, but we're no longer testing against it. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support)
  for details.
* Fix path delimiter bug which broke Windows support.
* Tests can define `window.uncaughtErrorFilter`, a function to filter out
  expected uncaught errors. The function receives `ErrorEvent` objects from the
  `error` event on window, and if it returns true, the error will not be logged
  and will not cause tests to fail.
* Change the default value of --module-resolution to "node".
* Updated dependencies.

## 6.6.0-pre.5 - 2018-04-12
* Restore missing dependency 'findup-sync'.

## 6.6.0-pre.4 - 2018-04-11
* Upgrade polyserve to pick up recent fixes.

## 6.6.0-pre.3 - 2018-03-21
* Upgrade polyserve to ^0.25.2 and add the --module-resolution flag
* Fix #488 - Support .bowerrc directory name override of bower_components, including variants
* Injected libraries are now resolved to correct paths while using the runner. Note: simply serving the test file and running it will attempt to find the files, but they may load the wrong versions (e.g. lodash 4 might load instead of 3)
* Not published: 6.6.0-pre.1 & 6.6.0-pre.2

## 6.5.0 - 2018-01-17
* Upgrade wct-local to 2.1.0 to get support for headless Chrome.

## 6.4.3 - 2018-01-11
* web-component-tester: no longer injects `a11ySuite.js` script in `--npm` mode.
* wct-browser-legacy: `a11ySuite.js` now exports a `a11ySuite` reference. Import this reference direction to use `a11ySuite()` in npm.

## 6.4.2 - 2018-01-09
* Upgrade wct-sauce to 2.0.0 to get updated browsers lists to include Safari 11 and Edge 15.
* Fixed #523 WCT ignores the webserver hostname
* Remove `overflow-y: auto` from test runner styling to increase performance.

## 6.4.1 - 2017-11-20
* Ensure that WCT is installed with compatible versions of wct-local and wct-sauce. This fixes a bug where – if incompatible versions are installed – they aren't able to coordinate shutdown, so WCT hangs after successfully completing a test run.

## 6.4.0 - 2017-10-31 🎃

* Updated package.json:
  * Upgraded dependencies async, chai, cleankill, findup-sync, sinon, and socket.io.
  * Upgraded devDependencies update-notifier
* Added `define:webserver` hook to enable substitution of the generated express app for the webserver through a callback, to support use cases where a plugin might want to inject handlers or middleware in front of polyserve.
* Added support for `proxy: {path: string, target: string}` config which is forwarded to `polyserve`.

## 6.3.0 - 2017-10-02

* Updated wct-browser-legacy to use a module version of a11ySuite to get access to Polymer.dom.flush.
* Updated generated index for webserver to use a11ySuite as a module.
* Updated polyserve to get support for on-the-fly module compilation and `<script type=module>` conversion for browsers that don't support modules.

## 6.2.0 - 2017-09-19

* Updated the browser.js file for npm case to use test-fixture as JS module instead of html import.
* Updated the integration tests to support running on Sauce via wct-sauce plugin.
* Updated polyserve to 0.22.1 for better compilation support and ES modules in HTML script tags and bug fixes.
* Updated bower.json to reference newly published test-fixture version 3.0.0.
* No longer require `?npm=true` url parameter, as `web-component-tester` and `wct-browser-legacy` versions of `browser.js` now contain explicit npm/non-npm directives of the form `window.__wctUseNpm` as a first step towards splitting out the client-side environment-specific config and behavior.
* Added support for scoped package names under npm.

## 6.1.5 - 2017-08-31

* Removed reliance on `document.currentScript` in browser.js because IE11 doesn't have it. (Second reference found)

## 6.1.4 - 2017-08-31

* Removed reliance on `document.currentScript` in browser.js because IE11 doesn't have it.
* To use WCT tests with NPM in browser without WCT (or polymer-cli) running on the commandline, you must include an `?npm=true` or `&npm=true` parameter in the URL for the test document; WCT does this automatically when opening its own browsers.

## 6.1.3 - 2017-08-26

* Added `sinon` as dependency of `sinon-chai` in web context to suppress the npm installation warning/error of unmet peer dependency, even though `@polymer/sinonjs` fulfills the runtime dependency and `sinon` will be unused.
* Set `@polymer/test-fixture` back to ^0.0.3 because of dependency install errors related to yarn's "flat".

## 6.1.2 - 2017-08-22

* Updated npm browser dependency on `@polymer/test-fixture` v3.0.0-pre.1

## 6.1.1 - 2017-08-21

* Updated browser dependency to a browser-ready sinon npm package for `wct --npm` option.

## 6.1.0 - 2017-08-17

* Added an *experimental* `--npm` flag to support web packages which are installed in `node_modules` by npm (instead of installed in `bower_components` by Bower.
* Builds a `wct-browser-legacy` package directory used to publish npm-specific client support package to npm, suitable for use with devDependencies of custom elements.
* Added version flag to CLI. Available using `--version` or `-V`.

## 6.0.1 - 2017-08-08

* Updated package.json dependencies:
  * Upgraded @types/gulp
  * Moved all @types to devDependencies
  * Removed PolymerElements/test-fixture from npm dependencies (is already installed by bower)
* Undo fix for #505, as https://bugs.chromium.org/p/chromium/issues/detail?id=701601 has been fixed and shipped in M58 stable.

## 6.0.0 - 2017-05-15

* Major changes:
  * In an effort to reduce magical behavior and make `wct` easier to understand, it no longer will automatically serve some resources from its own `npm` dependencies and some resources from the project under test. Instead, all resources are served out of the project under test. This gives the project under test control over its testing dependencies and their versions.
    * As part of this, wct will also require that the project under test have an installation of the client side web-component-tester bower package.
    * This release unifies the behavior of `wct` and `polyserve`, so if your code works without warnings in one it should work in the other.
  * `webserver.webRunnerPath`, `webserver.webRunnerContent`, and `webserver.urlPrefix`, `webserver.staticContent` were internal properties that were exposed on the `config` object. They have been refactored and their replacement has been prefixed with an underscore to clarify that they're internal implementation details.
  * Dropped support for node v4, added support for node v8. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support) for details.

<details>
  <summary>Click for full details</summary>

## New in 6.0.0 stable

* Update wct-local to remove deprecation warnings on install.
* Remove warning when running wct if it's not installed into `node_modules`.

## 6.0.0-prerelease.9 - 2017-04-19

* The default `waitFor` doesn't rely on `HTMLImports.whenReady` or `Polymer.whenReady` timings, but only waits for the `WebComponentsReady` event to be fired. For a different wait time, set `WCT = { waitFor: function(cb){ cb(); }}`.

## 6.0.0-prerelease.8 - 2017-04-13

* [BREAKING] Dropped support for node v4, added support for node v8. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support) for details.

## 6.0.0-prerelease.7 - 2017-03-15

* Fixed #505 – Work around an issue in Chrome 57 where dynamically inserted HTML Imports did not block subsequent script execution. See https://bugs.chromium.org/p/chromium/issues/detail?id=701601

## 6.0.0-prerelease.1 through 6.0.0-prerelease.6

### Breaking change

* In an effort to reduce magical behavior and make `wct` easier to understand, it no longer will automatically serve some resources from its own `npm` dependencies and some resources from the project under test. Instead, all resources are served out of the project under test. This gives the project under test control over its testing dependencies and their versions.
  * As part of this, wct will also require that the project under test have an installation of the client side web-component-tester bower package. We recommend that all projects also have a dependency on the npm web-component-tester node module, and in a future release will will require it. This is to makes results more reproducible, and ensures that they'll be protected from future breaking changes.
  * This release also unifies the behavior of `wct` and `polyserve`, so if your code works without warnings in one it should work in the other.
  * Calling `replace(...)` will use sinon to stub `document.importNode` until `teardown` is called.

### Added

* Polymer.dom.flush() call in a11ySuite to ensure lazy dom is loaded
* Added beforeEach parameter to a11ySuite
* Added first pass of _variants_. Variants different configurations of testing the same code.
  * Add support for _variant dependencies_.
    * wct already supports loading dependencies from your `bower_components` directory, mapping them to `../` in your code. You can now add variant dependency directories named like `bower_components-foo`. When these are detected, tests will then run separately for each such dependency directory, mapping `../` appropriately. See README for more details.

### Removed

* `webserver.webRunnerPath`, `webserver.webRunnerContent`, and `webserver.urlPrefix`, `webserver.staticContent` were internal properties that were exposed on the `config` object. They have been refactored and their replacement has been prefixed with an underscore to clarify that they're internal implementation details.

### Fixed

* Fixed #392
* Fixed #373 and #383 which were caused by `emitHook` not handling arguments correctly.
* Fixed error log message for loading WCT config

</details>

## 5.0.1

* Backport of fix for #505 – Work around an issue in Chrome 57 where dynamically inserted HTML Imports did not block subsequent script execution. See https://bugs.chromium.org/p/chromium/issues/detail?id=701601

## 5.0.0
* Mocha upgraded to `v3.1.2`. This shouldn't require any new code, but make sure your tests still pass as there were some more subtle changes made to Mocha behavior for v3 (Add IE7 support, update dependencies). See https://github.com/mochajs/mocha/pull/2350 for more info.

## 4.2.2
* Update bower dependencies to match node dependencies
* Update rollup to 0.25
* Update README to point to webcomponents-lite.js

## 4.2.1
* Fix `grep` for upstream mocha bug

## 4.2.0
* Add `httpbin` functionality to check `POST` requests
  * `POST` to `/httpbin`, response will be contents of `POST`

## 4.1.0
* Add `ignoreRules` option to `a11ySuite`
    * Array of a11ySuite rules to ignore for that suite
    * Example: https://github.com/PolymerElements/paper-tooltip/commit/bf22b1dfaf7f47312ddb7f5415f75ae81fa467bf

## 4.0.3
* Fix npm 3 serving for lodash and sinon

## 4.0.2
* Fix serving from `node_modules` for npm 3

## 4.0.1
* Fix Polymer 0.5 testing

## 4.0.0
* Remove `bower` as a dependency, serve testing files out of `node_modules`
* Upgrade to `wct-local` 2.0, which needs node 0.12+ for `launchpad` 0.5
* Replace esperanto with rollup for building browser bundle

# 3.x

## 3.4.0
* Integrate [test-fixture](https://github.com/PolymerElements/test-fixture)

## 3.3.0
* Add ability to cancel running tests from library

## 3.2.0
* Add accessibility testing with `a11ySuite` and
    [accessibility-developer-tools](https://github.com/GoogleChrome/accessibility-developer-tools)

## 3.1.3

* `.bowerrc` included in the package to ensure that packages don't get placed in
  unexpected locations.

## 3.1.2

* `--verbose` now includes logging from [`serve-waterfall`](https://github.com/PolymerLabs/serve-waterfall).

## 3.1.1

* WCT now depends on `wct-sauce ^1.5.0`

## 3.1.0

* WCT proper no longer periodically executes webdriver commands to ensure remote
  keepalive. Logic has moved into `wct-sauce`.

* Fix for verbose mode breaking IE10.

## 3.0.7

* Mixing TDD & BDD Mocha interfaces is now an error.

* Calls to `console.error` now generate an error.

* Errors that occur during WCT's initialization are more reliably reported.

* WCT now treats dependencies installed into `bower_components/` as if they are
  siblings of the current repo (much like polyserve).

* Browser libraries are no longer bundled with WCT.

  * They are now bower-managed, and by default installed to `bower_components/`
    within `web-component-tester`.

  * The libraries loaded can be configured via `WCT = {environmentScripts: []}`.

  * Massive overhaul of `browser.js` to support this & `environment.js` no
    longer exists.

* Support for newer versions of webcomponents.js (also Polymer 0.8).

* Mocha configuration can be specified by the `mochaOptions` key in client
  options (i.e. `<script>WCT = {mochaOptions: {}};</script>`).

* Browser options can be specified in `wct.conf.js` via the `clientOptions` key.

* WCT now always generates an index when run via the command line.

* `wct.conf.json` can be used as an alternative to `wct.conf.js`.

## 3.0.0-3.0.6

Yanked. See `3.0.7` for rolled up notes.


# 2.x

There were changes made, and @nevir failed to annotate them. What a slacker.


# 1.x

What are you, an archaeologist?
