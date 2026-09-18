# gen-typescript-declarations

[![NPM version](https://img.shields.io/npm/v/@polymer/gen-typescript-declarations.svg)](https://www.npmjs.com/package/@polymer/gen-typescript-declarations)

A library which generates TypeScript declarations for Polymer and custom
elements.

## How do I use the typings?

### Polymer 3

Typings for Polymer 3 are included starting from version 3.0.5. To use them,
install `@polymer/polymer` from npm, and use standard ES module import
specifiers:

```ts
import {PolymerElement} from '@polymer/polymer';

class MyElement extends PolymerElement {
   ...
}
```

### Polymer 2

Typings for Polymer 2 are included starting from version 2.4.0. To use them,
install Polymer from Bower and add a [triple-slash
directive](https://www.typescriptlang.org/docs/handbook/triple-slash-directives.html)
anywhere in your TypeScript project for the typings you require. Each HTML
import from Polymer has a corresponding typings file. For example, if you depend
on `polymer-element.html`:

```ts
/// <reference path="./bower_components/polymer/types/polymer-element.d.ts" />`

class MyElement extends Polymer.Element {
  ...
}
```

Alternatively, you can add the dependency to `tsconfig.json` in the root of your
project:

```javascript
{
...
	"include": [
		"src/**/*.ts",
		"src/bower_components/polymer/**/*.d.ts",
	]
}
```

You may also be interested in the [Polymer
decorators](https://github.com/Polymer/polymer-decorators).

## How do I generate new typings?

You can run this package from the command line with
`gen-typescript-declarations`, or as a library with the `generateDeclarations`
function.

It is recommended to integrate typings generation as part of your build/release
process:

```sh
$ npm install --save-dev @polymer/gen-typescript-declarations
```

Add a `generate-typings` script to your `package.json`:

```js
{
  ...
  "scripts": {
    "generate-typings": "gen-typescript-declarations"
  }
}
```

If you're using NPM, you can add this script to the NPM `prepack` script to
generate and include typings in your NPM package every time you publish. Most
users will want to configure their `.gitignore` so that the generated typings
are not committed to their Git repository. In this case, take care to configure
your `.npmignore` and/or `package.json`  to ensure that they are included when
you publish to NPM (run `npm pack` to check before publishing).

If you are still using Bower, ensure you run `npm run generate-typings` to
generate the latest typings and commit them to your repository before tagging
each release.

## Config options

By default the `gen-typescript-declarations` command will read a file called
`gen-tsd.json` in your root directory. It has the following options:

* **`excludeFiles`**`: string[]`

  Skip source files whose paths match any of these glob patterns. If
  `undefined`, defaults to excluding directories ending in "test" or "demo".

* **`excludeIdentifiers`**`: string[]`

  Do not emit any declarations for features that have any of these identifiers.

* **`removeReferences`**`: string[]`

  Remove any triple-slash references to these files, specified as paths
  relative to the analysis root directory.

* **`addReferences`**`: {[filepath: string]: string[]}`

  Additional files to insert as triple-slash reference statements. Given the
  map `a: b[]`, a will get an additional reference statement for each file
  path in b. All paths are relative to the analysis root directory.

* **`renameTypes`**`: {[name: string]: string}`

  Whenever a type with a name in this map is encountered, replace it with
  the given name. Note this only applies to named types found in places like
  function/method parameters and return types. It does not currently rename
  e.g. entire generated classes.

* **`autoImport`**`: {[modulePath: string]: string[]}`

  A map from an ES module path (relative to the analysis root directory) to
  an array of identifiers exported by that module. If any of those
  identifiers are encountered in a generated typings file, an import for that
  identifier from the specified module will be inserted into the typings
  file.

## Using as a module

You can also use this package as a module:

```js
import {generateDeclarations} from 'gen-typescript-declarations';

const config = {
  "exclude": [
    "test/**",
  ],
  "removeReferences": [
    "../shadycss/apply-shim.d.ts",
  ],
  "addReferences": {
    "lib/utils/boot.d.ts": [
      "extra-types.d.ts"
    ]
  },
  "renameTypes": {
    "Polymer_PropertyEffects": "Polymer.PropertyEffects"
  }
}

// A map of d.ts file paths to file contents.
const declarations = await generateDeclarations('/my/root/dir', config);
```

## FAQ

### Why are some typings missing?
This library is based on [Polymer
Analyzer](https://github.com/Polymer/polymer-analyzer) which has limitations in
its static analysis. For certain patterns, Analyzer relies on additional JSDoc
annotations.

### Can I augment the generated typings with hand-written ones?
Yes, see the `addReferences` config option above.
