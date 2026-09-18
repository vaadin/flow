# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add new, unreleased changes here. -->

## [1.0.4] - 2018-12-06
* Load AMD modules with the same crossorigin attribute as
  document.currentScript, defaulting to `crossorigin=anonymous` to match
  native module script behavior.

## [1.0.3] - 2018-09-18
* Fix incorrect resolution of absolute path URLs (e.g. loading `/foo.js` from
  `/bar/baz.js`).

## [1.0.2] - 2018-06-28
* Update minimized version for latest minifier changes.

## [1.0.1] - 2018-06-11
* `import.meta.url` is now correct for scripts defined in HTML imports.
* Executing the loader twice will no longer invalidate its global state.

## [1.0.0] - 2018-05-08
* Fix bug where the ordering of module execution was not strict enough.
  Standard ES modules are loaded as they're encountered, but they execute
  strictly in the order that they're first imported.
* Support cyclical dependencies.

## [0.1.1] - 2018-05-03
* Fix bug where the `import.meta.url` of a top-level module had an artificial
  anchor suffix (e.g. "http://example.com/foo#3").
* Fix a bug relating to base URL in IE 11 which was causing invalid URLs to be
  loaded, and incorrect import.meta.url to be provided.

## [0.1.0] - 2018-05-01
* Initial release.
