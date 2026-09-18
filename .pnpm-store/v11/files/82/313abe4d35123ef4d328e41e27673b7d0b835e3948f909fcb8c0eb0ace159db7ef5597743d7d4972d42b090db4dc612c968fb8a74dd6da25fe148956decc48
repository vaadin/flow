# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add new, unreleased changes here. -->

## [4.0.3] - 2019-03-01
* Added support for `treeshake` option to remove dead JavaScript code when bundling.

## [4.0.2] - 2018-06-28
* Add `modules` to the browser capabilities of the `uncompiled-bundled` and 
  `uncompiled-unbundled` build presets.
* Fix NPM audit warnings.

## [4.0.1] - 2018-05-09
* Fix names of `uncompiled-bundled` and `uncompiled-unbundled` build presets.

## [4.0.0] - 2018-05-08
* Change the default value of `--module-resolution` to "node".

## [3.14.0] - 2018-05-03
* Dropped support for node v6. This is a soft break, as we aren't
  making any changes that are known to break node v6, but we're no longer testing against it. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support)
  for details.
* Removed the `transformImportMeta` option, because it is now always coupled
  with the `transformModulesToAmd` option.
* The `import.meta` transform now depends on the special `"meta"` dependency
  provided by
  [@polymer/esm-amd-loader](https://github.com/Polymer/tools/tree/master/packages/esm-amd-loader),
  instead of injecting a static path. As a result, it is now always coupled with
  the AMD transform, and cannot be enabled independently.
* Added es5, es2015, es2016, es2017, and es2018 compile targets to the
  `js.compile` option, to allow fine grained control over which JavaScript
  features to compile.

## [3.13.0] - 2018-04-03
* [breaking] Rename `build` option from `js.transformEsModulesToAmd` to `js.transformModulesToAmd`.
* Add new `build` option: `js.transformImportMeta`.

## [3.12.0] - 2018-03-28
* Added new `build` option: `js.transformEsModulesToAmd`.

## [3.11.1] - 2018-03-27
* `ProjectConfig.initializeAnalyzerFromDirectory` should use the given
  directory as a root dir, even if there is no `polymer.json` file there.

## [3.11.0] - 2018-03-27
* JSON serialization will now include `npm`, `componentDir`, and `moduleResolution` fields.
* Add a method to initialize an Analyzer based on a config, as well as
  a static function ProjectConfig to initialize an Analyzer from a project
  directory.
  Because polymer-analyzer 3.0.0 is in prerelease, project-config depends on it
  as a peer dependency, so that the dependant can choose the specific pre-release version of analyzer to use.


## [3.10.0] - 2018-03-12
* Added new option: `moduleResolution`. Determines algorithm used for resolving module specifiers. Can be 'none' or 'node'.

## [3.9.0] - 2018-02-23
* Added new options: `npm` and `componentDir`. Setting `npm` automatically sets `componentDir` to "node_modules/", unless explicitly set otherwise.

## [3.8.1] - 2018-01-29
* Changed name of `excludes` property to more natural `exclude` in `js-minify`, `js-compile`, `css-minify` and `html-minify` options prior to releasing the feature as `exclude` is more natural.  The `bundle` options will remain `excludes` for now, as that is already implemented in `polymer-bundler`.

## [3.8.0] - 2018-01-23
* Updated the `build` options `js-minify`, `js-compile`, `css-minify` and `html-minify` to support an object with `excludes` property to list specific files to exclude from minification or compilation.

## [3.7.0] - 2018-01-01
* Added new option: `autoBasePath`. This new flag sets `basePath: true` on all builds. See that option for more details.
* Removed `addPushManifest` from bundled build presets.

## [3.6.0] - 2017-11-28
* Added a new lint option: `filesToIgnore`. We'll never report warnings for any
  file that matches any of these globs.
  * `ignoreWarnings` is a confusing phrasing. Added the `warningsToIgnore`
    spelling which will be used preferentially. This was better than making the
    options inconsistent, or spelling the new option `ignoreFiles`.
* Improved error messages when validation of a polymer.json object fails.

## [3.5.0] - 2017-11-21
* Added static methods for constructing a ProjectConfig directly from an unvalidated JSON object, in addition to the methods for reading it from disk.

## [3.4.0] - 2017-06-21
* Modified the `bundle` property in project build options to support the subset of `polymer-bundler` options which can be serialized in a JSON file.

## [3.3.0] - 2017-06-09
* Removed `insertPrefetchLinks` from build presets. See https://github.com/Polymer/polymer-build/issues/239 for details.

## [3.2.1] - 2017-05-19
* Turned on insertPrefetchLinks for the bundled presets after prefetch link bugs were fixed.

## [3.2.0] - 2017-05-15
* Add ProjectConfig toJSON method.

## [3.1.0] - 2017-05-15
* Add browserCapabilities and basePath options.

## [3.0.0] - 2017-05-09
* Add build preset support.
* Remove Node v4 support: Node v4 is no longer in Active LTS, so as per the [Polymer Tools Node.js Support Policy](https://www.polymer-project.org/2.0/docs/tools/node-support) we will not support Node v4 going forward. Please update to Node v6 or later to continue using the latest verisons of Polymer tooling.


## [2.1.1] - 2017-04-14
* Add more documentation, and expose it through the JSON Schema.

## [2.1.0] - 2017-04-11
* `builds`: Add support for `addPushManifest` build option. See [polymer-build README](https://github.com/Polymer/polymer-build#projectaddpushmanifest) for more information.


## [2.0.1] - 2017-02-28
* Update version of plylog.

## [2.0.0] - 2017-02-10

* [BREAKING] polymer.json is validated upon being read and an error will be thrown if any fields are of the wrong type. All toplevel fields are optional, and no error is thrown on encountering extra fields.
* Added "lint" config option for configuring polymer-lint.

## [1.2.0] - 2017-01-27

* Added `isSources()` method to ProjectConfig: validates that a given file path matches the project's "sources" globs. Useful for determining if a file should be treated as a source file or as a dependency.
* New Config Option: `builds` defines project build configurations.
* `validate()` now checks the `builds` property for missing and duplicate names.

## [1.1.0] - 2016-12-09

* Added `validate()` method to ProjectConfig: validates that polymer.json contains valid paths.

## [1.0.2] - 2016-09-23

* Add `package.json` metadata

## [1.0.1] - 2016-09-23

* Add `.npmignore`

## [1.0.0] - 2016-09-23

* Initial release!
