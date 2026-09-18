# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

<!-- ## [Unreleased] -->
<!-- Add new, unreleased changes here. -->

## [1.6.2] - 2019-06-19
* Names in `excludeIdentifiers` will now also be removed from `import`
  statements.

## [1.6.1] - 2019-01-18
* Fix missing `source-map-support` dependency.

## [1.6.0] - 2018-12-05
* Added `--googModules` flag, for generating declarations compatible
  with `goog.module`-style modules.
* Added `--hideWarnings` flag, for hiding warning-level severity diagnostics.
* Generate declarations with tslint disables for relevant rules, like variable
  names, and no-any.

## [1.5.1] - 2018-08-25
* Legacy Polymer function element interfaces are now exported from ES modules.

## [1.5.0] - 2018-08-15
* Legacy Polymer function components will no longer have a `_template`
  property.
* Functions defined within exported Polymer function calls or exported objects
  are no longer themselves emitted as top-level functions.
* `FooBehaviorImpl` style behavior objects (that have been merged into a main
  `FooBehavior` array) will no longer have a broken `import` or `export`
  statements emitted for them.
* Update the URL for this repo put in generated file headers, since it has moved
  into the Polymer tools monorepo.
* Added `--verify` command-line flag which will run the generated typings
  through the TypeScript compiler (3.0) and fail with the log if it fails.
* The `--deleteExisting` flag will no longer delete `.d.ts` files that are
  referenced as keys in the `autoImport` config option.
* Do not `import` or `export` features with `private` visibility.
* Display warnings generated during the analysis phase.
* `autoImport` now supports bare-module specifiers. Local files must now
  begin with `.`.
* `excludeIdentifiers` option now applies to properties and methods.
* The pattern `import * as foo from 'foo'; export {foo as bar};` is now
  supported.
* Exit with a non-zero code when an analysis error is encountered.

## [1.4.0] - 2018-07-25
- Support for ES module imports and exports.
- Warnings are now printed with file names, line numbers, and code snippets.
- Add `autoImport` config option to automatically add ES module imports when
  particular identifiers are referenced.
- Automatically detect if a project uses NPM or Bower and configure module
  resolution settings accordingly.
- Automatically import/export synthetic mixin constructor interfaces.
- Superclasses and mixins are now emitted for classes.
- Element super classes are now emitted.
- Legacy Polymer elements now extend `LegacyElementMixin` and `HTMLElement`
  instead of `PolymerElement`.
- Mixin instance interfaces now extend the instance interfaces for the mixins
  that they automatically apply.

## [1.3.0] - 2018-06-29
- Generate typings for class constructors.
- Add `@ts-ignore` comment if method contains `@suppress {checkTypes}` in jsdoc.

## [1.2.2] - 2018-03-19
- Fix bad file path handling which broke Windows support.

## [1.2.1] - 2018-03-09
- Fix bug where if a package name was a prefix of one of its dependencies (e.g.
  `iron-icons` depends on `iron-iconset-svg`), then `<reference>` statements to
  include that dependency's typings would not be emitted (via
  https://github.com/Polymer/polymer-analyzer/pull/902).

## [1.2.0] - 2018-02-15
- Functions which definitely never return a value will now be automatically
  inferred as returning `void`.
- Getters on Polymer elements defined with the legacy Polymer function are now
  scanned correctly (previously type annotations were missing, and the readonly
  bit was inverted.

## [1.1.3] - 2018-02-12
- Mixin functions now include all of the additional mixins they automatically
  apply. Previously, only the immediately applied mixins were accounted for, but
  not ones that were applied transitively.

## [1.1.2] - 2018-02-08
- Elements that are constructable (usually a call to the Polymer function whose
  result is assigned to some variable) can now have behaviors.
- The `--deleteExisting` command line flag will no longer delete `.d.ts` files
  that are referenced as values in the `addReferences` config option, since such
  files are typically hand-written.

## [1.1.1] - 2018-02-05
- Fix missing `glob` dependency.

## [1.1.0] - 2018-02-05
- Added `excludeIdentifiers` config option. Use this to skip emitting any
  declarations for some feature by its class name, etc.
- Renamed `exclude` config option to `excludeFiles` to disambiguate it from
  `excludeIdentifiers`. `exclude` still works as before for backwards
  compatibility, but will be removed in the next major version.
- Polymer behavior interfaces now extend any additional behaviors that they
  apply. This is done with an array of behavior objects as documented at
  https://www.polymer-project.org/1.0/docs/devguide/behaviors#extending.
- Added `--deleteExisting` command line flag (default false) which recursively
  deletes all `.d.ts` files in the output directory before writing new typings,
  excluding `node_modules/` and `bower_components/`.

## [1.0.1] - 2018-02-01
- Always parameterize `Promise`. In Closure `Promise` is valid, but in
  TypeScript this is invalid and must be `Promise<any>` instead.
- Escape `*\` comment end sequences when formatting comments. These turn up in
  practice when an HTML comment embeds a JavaScript style block comment, like
  here:
  https://github.com/PolymerElements/paper-icon-button/blob/master/paper-icon-button.html#L51
- Hybrid Polymer elements without a LHS assignment now have `Element` appended
  to their generated interface name, to match the behavior of the Closure
  Polymer Pass
  (https://github.com/google/closure-compiler/wiki/Polymer-Pass#element-type-names-for-1xhybrid-call-syntax).
  For example, `interface IronRequest` is now `interface IronRequestElement`.
- Typings are now emitted for all HTML files, even if they contain no script
  tags. Added `index.html` to the default `exclude` set (alongside the existing
  `test/**` and `demo/**` globs).

## [1.0.0] - 2018-01-25
- [BREAKING] The `--outDir` flag is now required when using the command line
  tool. Previously it would print all concatenated typings to `stdout`, which
  doesn't make much sense given that we emit multiple files.
- Rewrite triple-slash references to Polymer into the `types/` directory so that
  they resolve correctly. Polymer is a special case where we put the typings in
  a `types/` subdirectory in order not to clutter the repo.
- Emit a `const FooBehavior: object` for behaviors. This lets TypeScript know
  that e.g. `Polymer.AppLocalizeBehavior` is a valid symbol that could be
  passed, for example, to the `Polymer.mixinBehaviors` function.

## [0.3.6] - 2018-01-09
- Support parameterized types other than `Array` and `Object`, such as `Foo<T>`.

## [0.3.5] - 2018-01-02
- Properties are now emitted as `readonly` when applicable.
- Bump Analyzer for latest scanning features (getters/setters, static methods,
  methods/properties on class prototypes).

## [0.3.4] - 2017-12-20
- Handle optional and rest parameters in function type expressions.

## [0.3.3] - 2017-12-18
- Pin Analyzer version for upcoming major refactor.

## [0.3.2] - 2017-12-18
- Static methods are now supported on classes, elements, and mixins.
- Add `renameTypes` config option, a map of renames to apply to named types that
  can be configured per-project.
- Convert Closure `ITemplateArray` type to TypeScript `TemplateStringsArray`.
- Support object index signatures (e.g. `Object<foo, bar>` maps to `{[key: foo]:
  bar}`).

## [0.3.1] - 2017-12-15
- Convert Closure `Object` to TypeScript `object`.
- Use glob patterns instead of RegExps to exclude files.
- Bump Analyzer version to include
  https://github.com/Polymer/polymer-analyzer/pull/791 which makes Polymer
  properties possibly `null|undefined`.

## [0.3.0] - 2017-12-12
- `void` is not nullable.
- Support constructor functions (e.g. `function(new:HTMLElement, string)`).
- Support record types (e.g. `@param {{foo: bar}}`).
- Include method `@return` descriptions.

## [0.2.0] - 2017-12-08
- Many fixes. See
  https://github.com/Polymer/gen-typescript-declarations/issues/23.

## [0.1.0] - 2017-11-09
- Initial release on NPM.
