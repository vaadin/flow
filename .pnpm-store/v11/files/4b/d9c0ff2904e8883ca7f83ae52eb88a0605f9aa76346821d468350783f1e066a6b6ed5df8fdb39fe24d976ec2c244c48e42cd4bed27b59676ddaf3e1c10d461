[![Build Status](https://travis-ci.org/Polymer/polymer-analyzer.svg?branch=master)](https://travis-ci.org/Polymer/polymer-analyzer)
[![NPM version](http://img.shields.io/npm/v/polymer-analyzer.svg)](https://www.npmjs.com/package/polymer-analyzer)

# Polymer Analyzer

A static analysis framework for Web Components.

## Install

```
npm install polymer-analyzer
```

## Usage
```js
const {Analyzer, FsUrlLoader, PackageUrlResolver} = require('polymer-analyzer');

const rootDir = process.cwd();
const analyzer = new Analyzer({
  urlLoader: new FsUrlLoader(rootDir),
  urlResolver: new PackageUrlResolver({ packageDir: rootDir }),
});

// This path is relative to the root dir
analyzer.analyze(['my-element.html']).then((analysis) => {
  // Print the name of every property on paper-button, and where it was
  // inherited from.
  const [paperButton] = analysis.getFeatures(
      {kind: 'element', id: 'paper-button', externalPackages: true});
  if (paperButton) {
    for (const [name, property] of paperButton.properties) {
      let message = `${name}`;
      if (property.inheritedFrom) {
        message += ` inherited from ${property.inheritedFrom}`;
      } else {
        message += ` was defined directly on paper-button`;
      }
      console.log(message);
    }
  } else {
    console.log(`my-element.html didn't define or import paper-button.`);
  }
});
```

## What's it used for?

* [webcomponents.org](https://webcomponents.org) - discovery, demos, and docs for web components
* [polymer-linter](https://github.com/Polymer/polymer-linter) - lints the web
* [polymer-build](https://github.com/Polymer/polymer-build) - performs HTML-aware buildtime optimization
* [polymer-editor-service](https://github.com/Polymer/polymer-editor-service) - IDE plugin, provides live as-you-type help

## Developing

Polymer Analyzer is supported on Node LTS and stable. It is written
in TypeScript. All development dependencies are installed via npm.

```sh
npm install
npm test
```

Or watch the source for changes, and run tests each time a file is modified:

```sh
npm run test:watch
```

## Looking for Hydrolysis?

Hydrolysis has been renamed to Polymer Analyzer for version 2. You can find the
hydrolysis source on the
[`hydrolysis-1.x`](https://github.com/Polymer/polymer-analyzer/tree/hydrolysis-1.x)
branch.
