[![Build Status](https://travis-ci.org/Polymer/polymer-build.svg?branch=master)](https://travis-ci.org/Polymer/polymer-build)
[![NPM version](http://img.shields.io/npm/v/polymer-build.svg)](https://www.npmjs.com/package/polymer-build)

# Polymer Build

polymer-build is a library for building Polymer projects.


## Installation

```
npm install --save-dev polymer-build
```


## Relationship to Polymer CLI

The [Polymer CLI](https://github.com/Polymer/polymer-cli) uses polymer-build under the hood, so you can think of the CLI's `build` command like running a pre-configured polymer-build pipeline. Setting this up for you makes the CLI easy to use, but as a command-line wrapper its customization options are more limited. polymer-build allows you to completely customize your build and combine additional streams and build tasks in any order.

Consider using polymer-build instead of the CLI if you:

- Want to customize your build(s) without using the Polymer CLI
- Need to run your source code through custom optimizers/processors before, after, or during your build
- Need to hook additional work into any part of the build process


## Usage

While polymer-build was built to work easily with Gulp, it can be used in any Node.js environment. polymer-build is built on Node.js streams, and the build pipeline that you create with it is not much more than a series of connected streams. Files are represented as [Vinyl](https://github.com/gulpjs/vinyl) file objects, which means that polymer-build can interop with any existing Gulp/Vinyl streams.

Check out the [custom-build generator](https://github.com/PolymerElements/generator-polymer-init-custom-build) for an example of how polymer-build can be used to build a project.


### PolymerProject

`PolymerProject` represents your project in the build pipeline. Once configured, it will give you access to a collection of streams and helpers for building your project.

To create a new instance of `PolymerProject`, you'll need to give it some information about your application. If you already have a [`polymer.json`](https://www.polymer-project.org/2.0/docs/tools/polymer-json) configuration file in your project, you can create a new `PolymerProject` instance by loading it directly:

```js
const PolymerProject = require('polymer-build').PolymerProject;

const project = new PolymerProject(require('./polymer.json'));
```

Or, you can pass in configuration options directly to the `PolymerProject` constructor:

```js
const PolymerProject = require('polymer-build').PolymerProject;

const project = new PolymerProject({
  entrypoint: 'index.html',
  shell: 'src/my-app.js',
  fragments: [
    'src/my-view1.js',
    'src/my-view2.js',
    'src/my-view3.js'
  ]
});
```

#### project.sources()

Returns a readable stream of your project's source files. By default, these are the files in your project's `src/` directory, but if you have additional source files this can be configured via the `sources` property in [`ProjectOptions`](src/polymer-project.ts).

#### project.dependencies()

Returns a readable stream of your project's dependencies. This stream is automatically populated based on the files loaded inside of your project. You can include additional dependencies via the `extraDependencies` property in [`ProjectOptions`](src/polymer-project.ts) (this can be useful when the analyzer fails to detect a necessary dependency.)

```js
const gulp = require('gulp');
const mergeStream = require('merge-stream');

// Create a build pipeline to pipe both streams together to the 'build/' dir
mergeStream(project.sources(), project.dependencies())
  .pipe(gulp.dest('build/'));
```


### Handling Inlined CSS/JS

#### HtmlSplitter

Web components will sometimes include inlined CSS & JavaScript. This can pose a problem for tools that weren't built to read those languages from inside HTML. To solve this, you can create an `HtmlSplitter` instance to extract inlined CSS & JavaScript into individual files in your build stream for processing (and then rejoin them later).

```js
const gulpif = require('gulp-if');
const uglify = require('gulp-uglify');
const cssSlam = require('css-slam').gulp;
const htmlMinifier = require('gulp-html-minifier');
const HtmlSplitter = require('polymer-build').HtmlSplitter;

const sourcesHtmlSplitter = new HtmlSplitter();
const sourcesStream = project.sources()
  .pipe(sourcesHtmlSplitter.split()) // split inline JS & CSS out into individual .js & .css files
  .pipe(gulpif(/\.js$/, uglify()))
  .pipe(gulpif(/\.css$/, cssSlam()))
  .pipe(gulpif(/\.html$/, htmlMinifier()))
  .pipe(sourcesHtmlSplitter.rejoin()); // rejoins those files back into their original location

// NOTE: If you want to split/rejoin your dependencies stream as well, you'll need to create a new HtmlSplitter for that stream.
```

You can add splitters to any part of your build stream. We recommend using them to optimize your `sources()` and `dependencies()` streams individually as in the example above, but you can also optimize after merging the two together or even after bundling.


### Bundling Files

#### project.bundler()

A stream that combines seperate files into code bundles based on your application's dependency graph. This can be a great way to [improve performance](https://developer.yahoo.com/performance/rules.html#num_http) by reducing the number of frontend requests needed.

By default, the bundler will create one "shared-bundle.html" containing all shared dependencies. You can optimize even further by defining "fragments" in your project options. Fragments are lazy loaded parts of the application, typically views and other elements loaded on-demand. When fragments are defined, the bundler is able to create smaller bundles containing code that is only required for specific fragments.

```js
const gulp = require('gulp');
const mergeStream = require('merge-stream');

// Create a build pipeline to bundle our application before writing to the 'build/' dir
mergeStream(project.sources(), project.dependencies())
  .pipe(project.bundler())
  .pipe(gulp.dest('build/'));
```

The bundler() method accepts an options object to configure bundling.  See [Using polymer-bundler programmatically](https://github.com/Polymer/tools/tree/master/packages/bundler#using-polymer-bundler-programmatically) for a detailed list of accepted options.

```js
const {generateCountingSharedBundleUrlMapper,
       generateSharedDepsMergeStrategy} = require('polymer-bundler');

mergeStream(project.sources(), project.dependencies())
  .pipe(project.bundler({
    excludes: ['bower_components/polymer-code-mirror'],
    sourcemaps: true,
    stripComments: true,
    strategy: generateSharedDepsMergeStrategy(3),
    urlMapper: generateCountingSharedBundleUrlMapper('shared/bundle_')
  }))
  .pipe(gulp.dest('build/'));
```

NOTE: When working programmatically with Bundler, **Polymer 1.x** projects should include `rewriteUrlsInTemplates: true` when their projects rely on custom element definitions which use relative paths inside style tags and element attributes.  **Polymer 2.x** uses the `assetpath` property added to dom-modules during bundling to resolve relative urls in style tags and provides the [importPath](https://www.polymer-project.org/2.0/docs/devguide/dom-template#urls-in-templates) binding to prefix relative paths in templates.

### Generating a Service Worker

#### generateServiceWorker()

`generateServiceWorker()` will generate the service worker code based on your build. Unlike other parts of polymer-build, `generateServiceWorker()` returns a promise and not a stream. It can only be run **after** your build has finished writing to disk, so that it is able to analyze the entire build as it exists.

For bundled builds, be sure to set the bundled option to `true`. See [AddServiceWorkerOptions](src/service-worker.ts) for a list of all supported options.

```js
const generateServiceWorker = require('polymer-build').generateServiceWorker;

generateServiceWorker({
  buildRoot: 'build/',
  project: project,
  bundled: true, // set if `project.bundler()` was used
  swPrecacheConfig: {
    // See https://github.com/GoogleChrome/sw-precache#options-parameter for all supported options
    navigateFallback: '/index.html',
  }
}).then(() => { // ...
```

`generateServiceWorker()` is built on top of the [sw-precache](https://github.com/GoogleChrome/sw-precache) library. Any options it supports can be passed directly to that library via the `swPrecacheConfig` option. See [sw-preache](https://github.com/GoogleChrome/sw-precache#options-parameter) for a list of all supported options

In some cases you may need to whitelist 3rd party services with sw-precache, so the Service Worker doesn't intercept them. For instance, if you're hosting your app on Firebase, you'll want to add the `navigateFallbackWhitelist: [/^(?!\/__)/]` option to your `swPrecacheConfig` as Firebase owns the `__` namespace, and intercepting it will cause things like OAuth to fail.

#### addServiceWorker()

Like `generateServiceWorker()`, but writes the generated service worker to the file path you specify in the `path` option ("service-worker.js" by default).

```js
const addServiceWorker = require('polymer-build').addServiceWorker;

addServiceWorker({
  buildRoot: 'build/',
  project: project,
}).then(() => { // ...
```


### Generating an HTTP/2 Push Manifest

`polymer-build` can automatically generate a [push manifest](https://github.com/GoogleChrome/http2-push-manifest) for your application. This JSON file can be read by any HTTP/2 push-enabled web server to more easily construct the appropriate `Link: <URL>; rel=preload; as=<TYPE>` headers(s) for HTTP/2 push/preload. Check out [http2push-gae](https://github.com/GoogleChrome/http2push-gae) for an example Google Apps Engine server that supports this.

The generated push manifest describes the following behavior: Requesting the shell should push any shell dependencies as well. Requesting a fragment should push any dependencies of that fragment *that were not already pushed by the shell.* If no shell was defined for your build, `polymer-build` will use the application entrypoint URL instead (default: `index.html`).


#### project.addPushManifest()

This method will return a transform stream that injects a new push manifest into your build (default: `push-manifest.json`). The push manifest is based off the application import graph, so make sure that this stream is added after all changes to the application source code.

Use the `filePath` argument to configure the name of the generated file (relative to your application root).

Use the `prefix` argument to prepend a string to all resource paths in the generated manifest. This can
be useful when a build is going to be served from a sub-directory on the server.

```js
const gulp = require('gulp');
const mergeStream = require('merge-stream');

mergeStream(project.sources(), project.dependencies())
  .pipe(project.addPushManifest())
  .pipe(gulp.dest('build/'));
```

### Custom Elements ES5 Adapter

If your build pipeline outputs ES5 custom elements (either from source or by compilation with a tool like Babel), it is critical to include the [Custom Elements ES5 Adapter](https://github.com/webcomponents/custom-elements/blob/master/src/native-shim.js). This adapter provides compatibility between custom elements defined as ES5-style classes and browsers with native implementations of the Custom Elements API, such as Chrome. See the [adapter documentation](https://github.com/webcomponents/custom-elements/blob/master/src/native-shim.js) for details of why this is necessary.

#### project.addCustomElementsEs5Adapter()

This method will return a transform stream that identifies your entrypoint HTML file (by looking for a webcomponents polyfill import) and injects a block of HTML that loads the Custom Elements ES5 Adapter. You can use this in a build pipeline to conditionally inject the adapter only when you output ES5 (as it is not needed when you output ES6).

```js
const gulp = require('gulp');
const mergeStream = require('merge-stream');

mergeStream(project.sources(), project.dependencies())
  .pipe(project.addCustomElementsEs5Adapter())
  .pipe(gulp.dest('build/'));
```


### Multiple Builds

#### forkStream(stream)

Sometimes you'll want to pipe a build to multiple destinations. `forkStream()` creates a new stream that copies the original stream, cloning all files that pass through it.

```js
const gulp = require('gulp');
const mergeStream = require('merge-stream');
const forkStream = require('polymer-build').forkStream;

// Create a combined build stream of your application files
const buildStream = mergeStream(project.sources(), project.dependencies());

// Fork your build stream to write directly to the 'build/unbundled' dir
const unbundledBuildStream = forkStream(buildStream)
  .pipe(gulp.dest('build/unbundled'));

// Fork your build stream to bundle your application and write to the 'build/bundled' dir
const bundledBuildStream = forkStream(buildStream)
  .pipe(project.bundler())
  .pipe(gulp.dest('build/bundled'));
```

#### project.updateBaseTag()

This method will return a transform stream that finds a `<base>` tag in your configured entrypoint HTML file, and updates it with the specified value. This can be useful when multiple builds are served each from their own sub-directory on the same host, in conjunction with a convention of using relative URLs for static resources. Your entrypoint must already contain a `<base href="/">` or similar tag in its `<head>`, before any imports.

Note that *only the entrypoint will be updated*. Fragments with `<base>` tags will not be modified. Fragments should typically use relative URLs to refer to other artifacts in the build, so that they are agnostic to their serving path. The entrypoint gets special treatment here because it is typically served from paths that do not correspond to its location relative to other build artifacts.

```js
const gulp = require('gulp');
const mergeStream = require('merge-stream');
const forkStream = require('polymer-build').forkStream;

const buildStream = mergeStream(project.sources(), project.dependencies());

const unbundledBuildStream = forkStream(buildStream)
  // This build will be served from http://example.com/unbundled/
  .pipe(project.updateBaseTag('/unbundled/'))
  .pipe(gulp.dest('build/unbundled'));

const bundledBuildStream = forkStream(buildStream)
  .pipe(project.bundler())
  // While this build will be served from http://example.com/bundled/
  .pipe(project.updateBaseTag('/bundled/'))
  .pipe(gulp.dest('build/bundled'));
```


## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

You can compile polymer-build from source by cloning the repo and then running `npm run build`. Make sure you have already run `npm install` before compiling.


## Supported Node.js Versions

polymer-build officially supports the latest current & active [LTS](https://github.com/nodejs/LTS) versions of Node.js. See our [.travis.yml](/.travis.yml) for the current versions of Node.js under test and visit the [Polymer Tools Node.js Support Policy](https://www.polymer-project.org/2.0/docs/tools/node-support) for more information.
