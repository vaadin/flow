[![NPM version](http://img.shields.io/npm/v/@polymer/esm-amd-loader.svg)](https://www.npmjs.com/package/@polymer/esm-amd-loader)

# @polymer/esm-amd-loader

A JavaScript library which loads AMD-style modules in the browser in 1.4 KB.

## Contents

- [Installation](#installation)
- [Example usage](#example-usage)
- [window.define](#windowdefine)
- [Special dependencies](#special-dependencies)
- [Differences from AMD/RequireJS](#differences-from-amdrequirejs)

## Installation

If you are using [Polymer CLI][1] 1.7.0 or above, then no separate installation
is needed. Polymer CLI will automatically transform your project to AMD modules
using the [Babel AMD transform plugin][2], and inject this loader into your HTML
document.

For other use cases, this loader can be installed directly from NPM:

```bash
$ npm install --save @polymer/esm-amd-loader
```

## Example usage

This loader is primarily intended to be used as the browser runtime component of
an automatic transformation of a project from ES modules to AMD modules, as
performed by tools like the [Babel AMD transform plugin][2] or [TypeScript AMD
generation][3]. It is not expected that users would typically author code
directly for this API.

#### index.html
```html
<script src="./node_modules/@polymer/esm-amd-loader/lib/esm-amd-loader.min.js"></script>

<script>
  define(['./foo.js'], function(foo) {
    console.log('imported', foo.stuff, 'from foo.js');
  });
</script>
```

#### foo.js
```js
define(['exports', 'require', 'meta'], function(exports, require, meta) {
  exports.stuff = 'neat stuff';

  require(['../bar.js'],
    function(bar) {
      console.log(meta.url, 'dynamically loaded bar.js:', bar);
    },
    function(error) {
      console.log(meta.url, 'failed to dynamically load bar.js:', error);
    });
});
```

## window.define

```ts
window.define = function(
    dependencies: string[],
    moduleBody?: (...args: Array<{}>) => void
```

### `dependencies`
An array of module paths, relative or absolute, which are dependencies of this
module. Relative paths are resolved relative to the location of this module.
Can also be one of the special dependencies listed below.

Dependencies are run in the same deterministic order as they would if they
were ES modules.

### `moduleBody`
A function which is invoked when all dependencies have resolved. The `exports`
of each dependency is passed as an argument to this function, in the same order
that they were specified in the `dependencies` array.

If any dependencies do not load (e.g. `404`), or if their module bodies throw an error nothing later in the dependency graph will execute, and an `Error` will be thrown up to the `window` error event.

## Special dependencies

### `"exports"`

The exports object for this module. If another module depends on this module,
this is the object that will be received.

### `"require"`

```ts
function require(
    dependencies: string[],
    onResolve?: (...args: Array<{}>),
    onError?: (error: Error)) => void
```

A function which will load the given dependencies, with relative paths resolved
relative to the current module. If successful, `onResolve` is called with the
resolved dependencies. If a dependency fails to load, `onError` is called with
the error from the first dependency which failed.

### `"meta"`

A `{url: string}` object, where `url` is the fully qualified URL of this module.
Corresponds to an ES module's [`import.meta`][5].

## Differences from AMD/RequireJS

- Minified and compressed size is 1.4 KB, vs 6.6 KB for RequireJS.

- Only supports specifying dependencies as paths, and does not support
  explicitly naming modules.

- Does not include a global `require` function. Instead,  modules created with
  `define` always execute immediately. RequireJS executes `require` calls
  immediately, but only executes `define` modules if they are a transitive
  dependency of a `require` call, or if they are named by the `data-name`
  bootstrap attribute.

- Modules always resolve to an `exports` object, even if the module did not
  request it or assign any properties to it. RequireJS modules will resolve to
  `undefined` if the module did not request its `exports` object.

- AMD does not specify the `meta` object. It does specify a similar object
  called `module`, which can contain `id` and `uri`. RequireJS provides `module`
  and sets `uri` to a path relative to the HTML document's base URL.

- RequireJS contains a [bug][4] whereby relative path resolution for modules
  above the HTML document base URL can result in duplicate requests for the same
  module.

- Module execution order happens according to the ES spec, including support for
  cyclical dependencies.

- Top level define calls are also ordered, similar to the way that multiple
  `<script type="module">` tags in an HTML document are.

[1]: https://github.com/Polymer/tools/tree/master/packages/cli
[2]: https://babeljs.io/docs/plugins/transform-es2015-modules-amd/
[3]: https://www.typescriptlang.org/docs/handbook/modules.html#code-generation-for-modules
[4]: https://github.com/requirejs/requirejs/issues/1732
[5]: https://github.com/tc39/proposal-import-meta/blob/master/HTML%20Integration.md
