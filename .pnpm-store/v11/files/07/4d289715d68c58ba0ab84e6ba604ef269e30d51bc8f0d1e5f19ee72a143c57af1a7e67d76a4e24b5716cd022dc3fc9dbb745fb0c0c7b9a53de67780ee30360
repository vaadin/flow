# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add new, unreleased changes here. -->

## [3.2.4] - 2019-07-09
* Merge property information from constructors into associated scanned
  properties when scanning classes

## [3.2.3] - 2019-06-04
* Fix "Scheme is missing" errors from `vscode-uri` dependency which was broken
  from a bad patch release in
  https://github.com/microsoft/vscode-uri/commit/7f15d244457fd4e09c88264c676dc915beffb634

## [3.2.2] - 2019-01-10
* Removed non-essential files from published package, such as tests.

## [3.2.1] - 2018-12-19
* Recognize .mjs as JavaScript files.

## [3.2.0] - 2018-12-03
* Support annotating mixin class properties and methods using
  `MixinClass.prototype.foo` syntax.

## [3.1.3] - 2018-10-15
* Make `HtmlDocument#stringify()` faster by only cloning the ast and stringifing
  inline documents into the cloned ast.
* Add `prettyPrint` option to `StringifyOptions`
* Implement behavior changes based on `prettyPrint` flag for
  `HtmlDocument`, `JavascriptDocument`, `CssDocument`, and `JsonDocument`

## [3.1.2] - 2018-08-23
* Failing to resolve an exported identifier will now be a warning instead
  of an uncaught exception.

## [3.1.1] - 2018-08-15
* Legacy Polymer function component features will no longer have a `_template`
  property.
* Functions defined within exported objects are no longer themselves emitted as
  top-level functions.

## [3.1.0] - 2018-07-25
* The `Function` type is now exported from `index.js`.
* `Import` features now have an `astNodePath`.
* `WarningStringifyOptions` now takes an optional `maxCodeLines` to print.
* Elements registered with the legacy `Polymer()` function now have
  `isLegacyPolymerFactoryCall: true`.
* `@extends {SuperClass}` annotations are now parsed. Previously the type was
  not extracted if it had curly braces.
* Expressions annotated with `@constructor` will now be scanned as classes.
* Ephemeral super classes will now be collapsed into their child classes.

## [3.0.2] - 2018-06-28
* Better support for resolving `export {foo as bar}` statements.

## [3.0.1] - 2018-05-11
* Better support for handling errors early on in the analysis process (load,
  parse, scan). This should solve the vast majority of Internal Error warnings.

## [3.0.0] - 2018-05-08
* `Analyzer#analyze` when called with an explicit list of files, will ignore
  any files that do not have a known file extension, as it will not know
  how to parse them.

## [3.0.0-pre.25] - 2018-05-03
* Dropped support for node v6. This is a soft break, as we aren't
  making any changes that are known to break node v6, but we're no longer testing against it. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support)
  for details.
* ScriptTagImport#isModule is true for `<script type="module" src="...">`
* Module `<script>` imports don't inherit imports from the HTML file that
  imports them, because unlike normal scripts, modules can be imported from
  many HTML files, and they have their own way to import their dependencies,
  rather than depending on HTML container files.

## [3.0.0-pre.24] - 2018-04-18
* Typings change: `ParsedDocument#astNode` had the type `any`. It now has a
  more strict type of `{} | null | undefined`. If this breaks downstream code,
  that code should probably use a more specific type of `ParsedDocument`, or
  `Document<MoreSpecificParsedDocType>`.
* Fix node module resolution for the case where components/ directory URL
  rewriting is happening (e.g. polyserve), and a root package is importing
  something from its own package using a path.

## [3.0.0-pre.23] - 2018-04-17
* [BREAKING] `Feature.astNode` is now an `AstNodeWithLanguage` rather than
  being an unwrapped ast node (usually typed as `any`). This makes it easier
  and safer to handle features which could be declared in HTML or JS. Most
  tools will not care about these ast nodes, this mostly comes up in tools
  like the linter, which does additional analysis of the AST.

## [3.0.0-pre.22] - 2018-04-09
* Fix module resolution in the case of root packages served from a component
  directory.

## [3.0.0-pre.21] - 2018-04-05
* Clarify module resolution error message.

## [3.0.0-pre.20] - 2018-04-03
* Added support for parsing `import.meta` in JavaScript modules.

## [3.0.0-pre.19] - 2018-03-30
* [breaking] Removed WarningFilter.fromConfig. ProjectConfig#initializeAnalyzer
  now does this from the other side (and cyclic deps are not a great idea).
* Add the ability to cancel an analysis by passing in a CancelToken, which can
  signal that the request has been cancelled. This can be useful in saving work
  if you know that the result of the analysis won't be used.

## [3.0.0-pre.18] - 2018-03-27
* [BREAKING] Removed Analyzer.createFromDirectory, the replacement will go into
  polymer-project-config.
* [BREAKING] Import features will now be produced when a document has a broken
  import (e.g. because the imported file does not exist, or it does not parse,
  etc). We still emit a warning in such cases.
* Recognize behaviors declared in export statements.
* Apply node module resolution to path imports as well as "named" imports.

## [3.0.0-pre.17] - 2018-03-20
* [BREAKING] `FSUrlLoader` is now `FsUrlLoader`.
* Reference resolution now supports javascript scoping rules, and will follow
  javascript module imports, including aliased imports, namespace imports, and
  re-exports. References to super classes, mixins, and behaviors use this
  resolution system.
* Add `nodeName` to `LocalId` references
* Added an `Export` feature, for JS module exports.


## [3.0.0-pre.16] - 2018-03-15
* Import URLs which are resolved, but for which the URL loader returns
  `canLoad() === false` result in a `not-loadable` Warning instead of
  `could-not-load`, which is reserved for other failures, such as parser
  errors or load errors.

## [3.0.0-pre.15] - 2018-03-15
* Now recognizes classes, behaviors, elements, and mixins that are
  declared in an ES6 `export` declaration.
* Document#getFeatures now supports querying by `statement`. Given the
  canonical statement (see esutil.getCanonicalStatement), it will lookup
  the feature at that statement. This will be used by internal
  APIs to do scope-based dereferencing of super classes, behaviors, and mixins.
* Added JavascriptImport#specifier for getting the original import specifier,
  before it may have been resolved to a file-relative url by the node module
  resolution algorithm.
* Parse concatenated strings in expressions
* Fixed issue where FsUrlResolver and IndirectUrlResolver didn't correctly
  resolve protocol-relative URLs.  These classes now accept a protocol option
  which defaults to `https` that is prepended when resolving these URLs.

## [3.0.0-pre.14] - 2018-03-09
* [BREAKING] JavascriptDocument#ast is now a `babel.File` rather than a
  `babel.Program`. Use `jsDoc.ast.program` instead of `jsDoc.ast` in the
  unlikely case that the `Program` is required.
* Fix bug where if a package's name was a prefix of one of its dependencies,
  that dependency would not resolve to its components directory.

## [3.0.0-pre.13] - 2018-03-05
* Support specifying tag names in jsdoc, e.g. `@customElement fancy-button`
* Add "bare" module specifier support for JavaScript imports and exports. ie,
  `import * as jquery from 'jquery'`.

## [3.0.0-pre.12] - 2018-02-14
* Functions and methods will now be automatically inferred as returning `void`
  in certain cases. This occurs when all of the following are true: 1) it has
  no `@return` or `@returns` JSDoc annotation, 2) it is not async or a
  generator, 3) its body contains no return statements with arguments.

## [3.0.0-pre.11] - 2018-02-14
* Support Windows line endings in JSDoc annotations
* Support scanning of properties in namespaces
* Support scanning of accessors in behaviors
* `Document#getFeatures()` now supports an `excludeBackreferences`
  option for use with inline documents to exclude the container/importer
  document and its features from the results.
* Support @return annotations on getters/setters on legacy Polymer function
  calls, and fix bug where readonly was inverted.
* Add `IndirectUrlResolver`, a URL resolver for very large codebases where
  source files map in an arbitrary but predetermined fashion onto URL space.
  This resolver is still experimental.
  * It is generally true that if something has to be imported from
    `polymer-analyzer/lib/...` then it's not yet stable. caveat importer.
* [BREAKING] Removed the `ScriptTagBackReferenceImport` and replaced it
  with new general-purpose `DocumentBackreference` feature used to
  link inline/imported documents back to their containers/importers.

## [3.0.0-pre.10] - 2018-02-01
* The `UrlResolver#relative()` method now returns a `PackageRelativeUrl`
  when called with only one argument, since the only use of this call form
  is to reverse a resolved URL back into a simpler package relative form.

## [3.0.0-pre.9] - 2018-01-26
* [BREAKING] Document#astNode and ParsedDocument#astNode are now an
  `AstNodeWithLanguage`, because we support inline documents in more than just
  HTML, as we've added an HTML-in-JS scanner.
* `MultiUrlResolver` now delegates the `relative()` method to the first
  `UrlResolver` in its `_resolvers` array that can `resolve()` the
  destination URL.  Makes it possible now to rely on the Analyzer's
  resolver to return a valid `PackageRelativeUrl` from a resolved URL.

## [3.0.0-pre.8] - 2018-01-18
* `FSUrlLoader#canLoad` reports false for local urls outside the loader's
  own root; enables fall-thru support needed for use with `MultiUrlLoader`.
* Add `Element#template` for getting the template of an element.
* In MultiUrlLoader, proxy the first implementation of readDirectory, if any.
* Use event annotation descriptions over their tag description.
* `RedirectResolver` resolves URLs which start with its redirect-to.

## [3.0.0-pre.7] - 2018-01-01
* [BREAKING]: `UrlResolver#resolve()` argument order swapped so that the
  optional `baseUrl` argument comes first instead of second.  This makes
  resolve more similar to `url.resolve`.
* [BREAKING] Removed scriptElement and domModule from PolymerElementMixin.
  They were always undefined.
* `UrlResolver#resolve()` returns urls containing querystring and fragment
  components where they were previously stripped out.
* Add FSUrlLoader#getFilePath which will return the file path that would
  be loaded for a given ResolvedUrl, or an error message explaining why
  it can't be.
* Add a `resolver` option when converting warnings to string, or when
  using the WarningPrinter. This will resolve file urls in warnings using
  `resolver.relative`, to avoid super long urls in warning messages.
* Getters and setters are now detected.
* Static methods are now detected in Polymer elements.
* Methods and properties added to a class prototype are now detected.

## [3.0.0-pre.6] - 2017-12-18
* [BREAKING] `Analysis#getDocument` now returns a `Result` object. When
  `result.successful` is true, `result.value` is a Document. When
  `result.successful` is false, then `result.value` is
  either a Warning or undefined.
* [BREAKING] UrlResolvers must now return complete URLs, like
  file:///path/to/paper-button/paper-button.html or
  https://example.com/components/paper-button/paper-button.html
* Introduce getDocumentContaining to find containing inline document for a
  feature

## [3.0.0-pre.5] - 2017-12-15
* [BREAKING] Removed `Analyzer#resolveUrl` in favor of just exposing the
  `UrlResolver` at `Analyzer#urlResolver.resolve`
* [BREAKING] Polymer property types are now assumed to be possibly
  `null|undefined` unless an explicit `@type` annotation says otherwise.
* Add `Analyzer.createForDirectory()` for easily getting a well configured
  analyzer for a given directory.
* Add `Import#originalUrl` which has the original url of the import as it was
  encountered in the document, before it was resolved relative to the base url
  of its containing document.
* Added `attributeType` field to Polymer property, which contains the name
  of the Polymer property declaration `type` field Constructor.

## [3.0.0-pre.4] - 2017-12-14

* [BREAKING] Removed the `UrlResolver#canResolve` method. A UrlResolver should
  return `undefined` when `resolve` is called to indicate that it can't resolve
  a URL.
* Add Analyzer.forDirectory() for easily getting a well configured analyzer
  for a given directory.
* Removed the `UrlResolver#canResolve` method. A UrlResolver should return
  `undefined` when `resolve` is called to indicate that it can't resolve a URL.
* Analyzer#urlResolver is a property that exposes the analyzer's url resolver,
  for cases where more direct access to url resolution is desired.
* Fix a situation where a warning would be reported as `[Object object]`.
* Fix issue where inline JavaScript module import statements did not honor
  their containing document's baseUrl; inline documents now inherit baseUrl
  from their containing documents.

## [3.0.0-pre.3] - 2017-12-08

* Added missing model typings for branded `url` types to top-level package
  exports.
* Added `templateTypes` property to functions, extracted from `@template`
  annotations.
* Add `Analyzer.createForDirectory()` for easily getting a well configured
  analyzer for a given directory.

## [3.0.0-pre.2] - 2017-11-30

* Added `js-import` feature with `lazy: true` for dynamic imports call
  expressions of the form `import()`.
* Functions will now be scanned if they have a `@global` annotation. Previously
  they would only be scanned if they had a `@memberof` annotation. One of these
  annotations is required because otherwise a lot of functions that aren't
  really public are included in the analysis (e.g. because they are hidden due
  to their scoping).
* Function names can now be overridden with e.g. `@function MyNewName`.

## [3.0.0-pre.1] - 2017-11-29

* [BREAKING] Switched the underlying parser/AST for JavaScript from
  `espree/estree` to `babylon/babel-types`.  This was needed to support parsing
  of important platform features such as dynamic imports and moves us closer to
  supporting TypeScript.
* When printing Warnings, use one-based indexes for lines and columns, as most
  text editors and other tools do.

## [2.7.0] - 2017-11-16

* Emit more accurate super classes for Elements when generating analysis JSON.
* Added the concept of automatically safe fixes and less-safe edit actions for
  Warnings. This is an upstreaming of functionality originally defined in
  polymer-linter.

## [2.6.0] - 2017-11-06

* Add `defaultValue` and `rest` fields to method parameters.

## [2.5.0] - 2017-11-05

* Use branded subtypes of string to be more careful about how we canonicalize and resolve urls. They're totally normal strings at runtime. TypeScript users that wrote their own UrlLoaders or UrlResolvers may need to make some minor changes to compile green, but since runtime isn't broken this isn't a breaking change. See src/mode/url.ts for more info.
* Handle method rest parameters correctly.

## [2.4.1] - 2017-10-31

* Minor fixes for TypeScript 2.6 compatibility.

## [2.4.0] - 2017-10-26

* Scan for module imports in inline and external JavaScript, analyzing the entire import graph.
* Changed the way HTML script tag containing document features are made available to the JavaScript document, by creating a ScriptTagBackReferenceImport instead of appending the HTML document features directly to JavaScript document.
* [minor] Add an `astNode` property on `Slot`.
* Improve handling of types of properties defined in a behavior or legacy polymer element.

## [2.3.0] - 2017-09-25

* Scan for CSS custom variable uses and assignments.
* Fix value of reflectToAttribute for polymer properties.
* Remove scriptElement from PolymerElement.
* `html-style` features are now scanned for inside templates.

## [2.2.2] - 2017-07-20

* Fixed a bug where we were too aggressive in associating HTML comments with
  nodes, such that any comment that came before a `<script>` tag e.g. could
  become part of the description of the element defined therin.
* Added support for recognizing instance properties in constructors.
  * The properties must be annotated with a jsdoc tag to be recognized.
  * Specific handling of the following tags is supported:
    * `@public`, `@private`, `@protected`, `@type`, and `@const`
    * The description can be combined with a visibility or type annotation. e.g.
      * `/** @type {number} How many bacon wrapped waffles to eat. */`

## [2.2.1] - 2017-07-06

* Removed TypeScript from production dependencies until TypeScript analysis is fully supported.

## [2.2.0] - 2017-06-22

* Comments are now only associated with classes and other objects if they are touching (<2 newlines between them).

## [2.1.0] - 2017-06-02

* Minor bugfix: JSON.stringify(warning) had a bunch of extra unnecessary properties.
* Track static methods on classes, emit them in analysis.json.
* Emit `superclass` for classes in analysis.json.

## [2.0.2] - 2017-05-18

* Track methods on behaviors as such. They were being treated as properties.

## [2.0.1] - 2017-05-15

* Many errors were changed to warnings so they wouldn't stop builds from completing.

## [2.0.0] - 2017-05-15

* Stable release of the Polymer Analyzer.


#### Changes since the previous prerelease version:

* [BREAKING] `attributes`, `properties`, `methods`, and `events` are now Maps from the name to the value rather than arrays. This better models what's actually going on (you can't have two different properties with the same name in javascript) and it makes our inheritance modeling more efficient.
  * Note that the serialized output format `analysis.json` is not changed, it still uses arrays.
* Warning is now a class, not just an interface. It also now keeps track of the
  parsed document that it came from. This fixes a race condition and API wart
  where to print a warning you needed to load the source of the document
  that was seen when the warning was generated.
  * Also, thanks to using the ParsedDocument interface, printing a warning is
    now O(size of underlined text) rather than O(size of document).

## [2.0.0-alpha.42] - 2017-05-09

* [minor breaking change] The paths in `@demo` annotations are no longer resolved.
* Added `MultiUrlLoader` and `PrefixedUrlLoader` to support analysis of more complicated project layouts.

## [2.0.0-alpha.41] - 2017-05-08

* [BREAKING] The `ElementLike` interface in `AnalysisFormat` changed its `demos` property from `string[]` to `Demo[]`, to include more information from `@demo` annotations in Jsdocs.
* [minor breaking change] Simplify rules for infering privacy. Now all features: classes, elements, properties, methods, etc have one set of rules for inferring privacy. Explicit js doc annotations are respected, otherwise `__foo` and `foo_` are private, `_foo` is protected, and `foo` is public.
* Fixed issue where jsdocs syntax in HTML comments was not parsed for polymer elements.

## [2.0.0-alpha.40] - 2017-04-28

* Added support for new JSDoc tags: @customElement, @polymer, @mixinFunction, @appliesMixin

## [2.0.0-alpha.39] - 2017-04-26

* Add a Class feature kind for describing all kinds of classes. This is a superclass of the existing elements and mixins.
* Mix mixins into mixins. A PolymerElementMixin now has all of the members it inherits other mixins it mixes.
* Improved our modeling of inheritance:
  * overriding inherited members now works correctly
  * overriding a private member produces a Warning
* Documented many fields as being readonly/immutable. This isn't complete, in fact all features and scanned features should be treated as immutable once they're created and initialized.
* Treat behaviors more like mixins, and treat using behaviors more like inheriting from mixins. This means that a Behavior object knows about all of the properties, methods, etc that it has inherited from other Behaviors that it builds upon.
* Emit more warnings when recognizing mixins, elements, and classes.

## [2.0.0-alpha.38] - 2017-04-13

* [minor breaking change] Revert of a change from alpha.37. The experimental `_fork` method on Analyzer returns an Analyzer again, not a Promise of an Analyzer.

## [2.0.0-alpha.37] - 2017-04-12

* [BREAKING] Analyzer.analyze() now must be passed an array of urls instead of a single url and it returns an Analysis. Use Analysis.getDocument(url) to get a Document from an Analysis.
* [BREAKING] The `getByKind`, `getById`, `getOnlyAtId`, and `listFeatures` methods on Document and Package have been replaced with a single method: `getFeatures`. It has all of the functionality of the individual methods, just packaged as a single simple query interface.
* [BREAKING] Dropped support for node v4, added support for node v8. See our [node version support policy](https://www.polymer-project.org/2.0/docs/tools/node-support) for details.
* Added `canLoad` method to `Analyzer`.
* Handle undefined source ranges more gracefully in WarningPrinter.
* [polymer] Negative number literals are allowed in databindings.

## [2.0.0-alpha.36] - 2017-04-07

* [BREAKING] Analyzer.analyze no longer takes the file's current contents as a second argument. This functionality was broken into two pieces, `filesChanged`, and `InMemoryOverlayLoader`.
  * Added a `filesChanged` method to Analyzer letting it know when a file needs to be reloaded.
  * Added an `InMemoryOverlayLoader` UrlLoader, for cases like a text editor where you'd like to use an in memory source of truth for a subset of files.
* The warning printer displays the squiggle underline in the correct place on lines indented by tabs.
* Extract className from the form `var className = Polymer({...})`
* Do a better job of matching comments up with Polymer 2 style element declarations.

## [2.0.0-alpha.35] - 2017-04-05

* [minor breaking change] By default queries for features and warnings now traverse lazy imports. Added a query option to limit results only to those reachable by normal (eager) imports.
* `generateAnalysis()` now includes PolymerBehavior information in `metadata.polymer.behaviors` collection.
* Jsdoc `@demo` annotations are now added to `demos` collection for `Element`, `ElementMixin`, `PolymerElement` and `Behavior`.
* Types and descriptions are now extracted from method @param and @returns jsdoc annotations.
* Fixed caching issue such that Documents would not always have information from the latest versions of their dependencies.

## [2.0.0-alpha.34] - 2017-03-20

* All Documents, ParsedDocuments, and ScannedDocuments now have correct SourceRanges. This also fixes a bug whereby `getByKind('document')` would fail to filter out package-external Documents.
* Fix an issue where package-external features could be included in queries that dot not ask for them. Specifically we were filtering based on the text in files  like `../paper-button/paper-button.html` rather than the path to the file on disk like `bower_components/paper-button/paper-button.html` when determining externality.
* Treat a directory named `build` as external, same as we do ones named `bower_components.*` or `node_modules.*`

## [2.0.0-alpha.33] - 2017-03-14

* Populate `astNode` on `PolymerElementMixin`.

## [2.0.0-alpha.32] - 2017-03-13

* Added options argument for Analyzer's experimental `_fork` method, supporting individual property overrides.
* Parse observers and computed functions in Polymer declarations. Warn on parse errors, or using the wrong syntax (e.g. function calls in `observer`, not a function call in `computed`).
* Correctly scan the `customElements.define(MyClass.is, MyClass)` pattern.

## [2.0.0-alpha.31] - 2017-03-07

* Clean up method descriptions

## [2.0.0-alpha.30] - 2017-03-03

* Fix typo in package.json.

## [2.0.0-alpha.29] - 2017-03-03

* Added a `_fork` method to Analyzer.
* Support for method detection on polymer elements and mixins via the new `methods` property.
* Support for function analysis (namespaced functions only for now, via the `@memberof` jsdoc tag).
* Track privacy on elements, mixins, properties, methods, and functions on namespaces.
  * replaced `private: boolean` with `privacy: 'public' | 'private' | 'protected'`
  * respects `@public` `@private` and `@protected` in jsdoc
  * considers one leading underscore to be protected, and two to be private
  * one trailing underscore is private (closure style)
* Track inheritance source for mixin and element members.
* Exported all public api through the package's `main` file, so that all public api can be accessed directly off of the object returned by `require('polymer-analyzer')` without the need to add imports into `polymer-analyzer/lib/whatever`.
* Track mixins on mixins and emit them in the Analysis. We don't mix properties into mixins yet.

## [2.0.0-alpha.28] - 2017-02-24
* Support for `@memberof` jsdoc tag
* Fix bad mixin descriptions


### Fixed
* PackageUrlResolver encodes URIs returned from `resolve` method.
* Support new properties/observers getters in Polymer 2.0 elements

## [2.0.0-alpha.27] - 2017-02-24

### Added
* Support for `@extends` and `@namespace` jsdoc tags
* Analyze inherited members for class-based elements annotated with `@extends`
* New Namespace and Reference features
* Mixins and namespaces are included in metadata export
* Ability to generate metadata for a package and filter exported metadata
* Analyze Polymer 2.0 elements that override observedAttributes

### Fixed
* Analyzer will not attempt to load or add warnings for imports which can not be resolved by its urlResolver.
* Protocol-less URLs such as `//host/path` are properly handled instead of treated as absolute paths.
* Infer tagnames from the static `is` getter.
* Unified Polymer and Vanilla element scanners

## [2.0.0-alpha.26] - 2017-02-22
* Fix issue with file missing from package.json "files" array.

## [2.0.0-alpha.25] - 2017-02-22

### Added
* Polymer 2.0 mixin scanner
* [polymer] Parse polymer databinding expressions.
  Give accurate warnings on parse errors.

## [2.0.0-alpha.24] - 2017-02-14

### Added
* HTML Documents now include a `baseUrl` property which is properly resolved
  from a `<base href="...">` tag, when present.
* Add `localIds` to `PolymerElement`, tracking elements in its template by their id attributes.

### Fixed

* [Polymer] Better handle unexpected behavior reference syntax.

## [2.0.0-alpha.23] - 2017-02-10

### Added

* [BREAKING] All methods on Document no longer return results from dependencies, but do return results from inline documents. Added a queryOptions param to all query methods to specify getting results from dependencies.
* [BREAKING] All methods on both Document and Package no longer return results from external code. Must specify `externalPackages: true` to get features from code outside the current package.

### Fixed

* Properly cache warning for incorrect imports or when failing parsing an import.
* Notice references to elements within `<template>` tags.

## [2.0.0-alpha.22] - 2017-01-13

### Fixed

* Do not complain about ES6 module syntax, but store enough information that we can warn about referencing modules as scripts.


## [2.0.0-alpha.21] - 2016-12-22

### Added
* Add an analyzePackage() method, for getting a queryable representation of everything in a package.

### Fixed
* Fix a deadlock when there are concurrent analysis runs of cyclic graphs.

## [2.0.0-alpha.20] - 2016-12-19

### Added
* [Polymer] Extract 'listeners' from 1.0-style declarations.
* [Polymer] Extract pseudo elements from HTML comments

### Fixed
* Fix a class of race conditions and cache invalidation errors that can occur when there are concurrent analysis runs and edits to files.

## [2.0.0-alpha.19] - 2016-12-12

### Added
* Add `changeEvent` to `Attribute`.

### Fixed
* Bump JS parser up to parse ES2017 (ES8).
* [Polymer] Property descriptors are allowed to be just a type name, like `value: String`.

## [2.0.0-alpha.18] - 2016-11-21

### Fixed
* [Polymer] A number of fixes around warnings when resolving behaviors
  * Warn, don't throw when a behavior is declared twice.
  * Warn when there's a problem mixing behaviors into other behaviors, the same way that we warn when mixing behaviors into elements.

* Fix some bugs with recursive and mutually recursive imports.

### Added
* Add `slots` to `Element`.

## [2.0.0-alpha.17] - 2016-10-28 - [minor]

### Added
* Improve the way we expose the `ElementReference` type.

## [2.0.0-alpha.16] - 2016-10-20

### Added

* Add a new default scanner for custom element references in html.
  * Also adds a non-default scanner for all element references in html.
  * Known bug: does not find element references inside of `<template>` elements.

## [2.0.0-alpha.15] - 2016-10-15

### Changed

* (polymer) - computed properties are implicitly readOnly.

* (editor service) - Greatly improve `getLocationForPosition` for determining the meaning of text at an HTML source location. Presently used for doing autocompletions.
  * Handles the contents of `<template>` elements.
  * Distinguishes attribute names from values.
  * Understands when text is inside a comment, inline style, and inline script.
  * Better handle many, many edge cases.
