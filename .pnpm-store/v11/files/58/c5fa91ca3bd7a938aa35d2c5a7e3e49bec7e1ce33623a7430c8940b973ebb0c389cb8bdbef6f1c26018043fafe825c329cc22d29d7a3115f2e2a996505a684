[![NPM version](http://img.shields.io/npm/v/web-component-tester.svg?style=flat-square)](https://npmjs.org/package/web-component-tester)
[![Build Status](http://img.shields.io/travis/Polymer/web-component-tester.svg?style=flat-square)](https://travis-ci.org/Polymer/web-component-tester)
[![Gitter](http://img.shields.io/badge/slack-join%20chat%20%E2%86%92-brightgreen.svg?style=flat-square)](https://polymer-slack.herokuapp.com/)

`web-component-tester` makes testing your web components a breeze!

You get a browser-based testing environment, configured out of the box with:

* [Mocha][mocha] as a test framework.
* [Chai][chai] assertions.
* [Async][async] to keep your sanity.
* [Lodash][lodash] (3.0) to repeat fewer things.
* [Sinon][sinon] and [sinon-chai][sinon-chai] to test just your things.
* [test-fixture][test-fixture] for easy fixture testing with `<template>` tags.
* [accessibility-developer-tools][a11ydevtools] through `a11ySuite` for simple, automated Accessibility testing.

WCT will [run your tests](#running-your-tests) against whatever browsers you have locally installed, or remotely via Sauce Labs.


# Getting Started

## `.html` Suites

Your test suites can be `.html` documents. For example,
`test/awesomest-tests.html`:

```html
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <script src="../../webcomponentsjs/webcomponents-lite.js"></script>
  <script src="../../web-component-tester/browser.js"></script>
  <link rel="import" href="../awesome-element.html">
</head>
<body>
  <awesome-element id="fixture"></awesome-element>
  <script>
    suite('<awesome-element>', function() {
      test('is awesomest', function() {
        assert.isTrue(document.getElementById('fixture').awesomest);
      });
    });
  </script>
</body>
</html>
```

Note that it is _critical_ that you include `../web-component-tester/browser.js`
in your test suites. `browser.js` contains all of WCT's client logic (and loads
bundled libraries like mocha and chai). You can also load it from the absolute URL `/components/web-component-tester/browser.js`.

## `.js` Suites

Alternatively, you can write tests in separate `.js` sources. For example,
`test/awesome-tests.js`:

```js
suite('AwesomeLib', function() {
  test('is awesome', function() {
    assert.isTrue(AwesomeLib.awesome);
  });
});
```

## Special Features

### test-fixture
`test-fixture` can be used to reset DOM state between test runs.
```html
<test-fixture id="simple">
  <template>
    <div></div>
  </template>
</test-fixture>
<script>
 suite('classList', () => {
   let div;
   setup(() => {
     div = fixture('simple');
   });
   test('foo', () => {
     div.classList.add('foo');
     assertSomethingOrOther(div);
   });
   test('bar', () => {
     div.classList.add('bar');
     assertNoFooClass(div);
   });
 });
 ```

### a11ySuite(fixureId, ignoredTests, beforeEach)
|Parameter|Type|Descrption|
|---|---|---|
|fixtureId|String|ID of the fixture to instantiate and test|
|ignoredTests|Array<string> (optional)|Tests to ignore. <br />Test names are found [here](https://github.com/GoogleChrome/accessibility-developer-tools/blob/master/dist/js/axs_testing.js) as calls to axs.AuditRules.addRule()|
|beforeEach|Function (optional)|Called before each test is run|

`a11ySuite` provides an simple way to run accessibility-developer-tools' high quality accessibility
audits when given a `test-fixture`.
The `a11ySuite` will show all the audit results via the standard Mocha test output.
```html
<test-fixture id="NoLabel">
  <template>
    <paper-radio-button id="radio-1"></paper-radio-button>
  </template>
</test-fixture>

<script>
  a11ySuite('NoLabel');
</script>
```
![Accessibility Suite Test Run](a11ySuiteExample.png)

## Running Your Tests

### `wct`

The easiest way to run your tests is via the `wct` command line tool. Install
it globally via:

```sh
npm install -g web-component-tester
```

Make sure that you also have [Java][java] installed and available on your
`PATH`.

The `wct` tool will run your tests in all the browsers you have installed. Just
run it:

```sh
wct
```

By default, any tests under `test/` will be run. You can override this by
specifying particular files (or globs of files) via `wct path/to/files`.


### Web Server

If you prefer not to use WCT's command line tool, you can also run WCT tests
directly in a browser via a web server of your choosing.

Make sure that WCT's `browser.js` is accessible by your web server, and have
your tests load `browser.js`.

The recommended way to fetch these is via Bower:

    bower install Polymer/web-component-tester --save


#### Nested Suites

To help support this case, you can also directly define an index that will load
any desired tests:

```html
<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <script src="../../webcomponentsjs/webcomponents-lite.js"></script>
    <script src="../../web-component-tester/browser.js"></script>
    <script src="../awesome.js"></script>
  </head>
  <body>
    <script>
      WCT.loadSuites([
        'awesome-tests.js',
        'awesomest-tests.html',
      ]);
    </script>
  </body>
</html>
```

_When you use `wct` on the command line, it is generating an index like this for
you based on the suites you ask it to load._


# Configuration

The `wct` command line tool will pick up custom configuration from a
`wct.conf.json` file located in the root of your project.
Or, you can specify your own file via the `--config-file` command line option.
Example: `--config-file my.wct.conf.js`
If you define your own configuration file, make sure you also provide the correct root if needed.
By default it will use the directory in which the configuration file is found as rootpath, which can result in errors if the file is in a sub directory.

 It should export the custom configuration:

```js
{
  "verbose": true,
  "plugins": {
    "local": {
      "browsers": ["chrome", "firefox"]
    }
  }
}
```

See [`runner/config.ts`](runner/config.ts) for the canonical reference of
configuration properties.

You can also specify global defaults (such as `sauce.username`, etc) via a
config file located at `~/wct.conf.json`.

## Plugins

Note that by specifying a plugin's configuration, you are letting WCT know that
it should load that plugin. If you wish to provide default configuration for a
plugin, but not enable it, you can have it default to disabled:

Requesting that plugin via `--plugin` on the command line (or overriding the
plugin's configuration to `disabled: false`) will cause the plugin to kick in.

### Sauce

The **sauce** plugin runs your tests on [Sauce](https://saucelabs.com/).

To use the sauce plugin you need to include credentials. To do this, either
set the `SAUCE_USERNAME` and `SAUCE_ACCESS_KEY` environment variables, or
set the `username` and `accessKey` fields in the `plugins.sauce` section of
your `wct.conf.json`.

You can configure the browsers that Sauce will run by setting the `browsers`
array in the plugin configuration. The options are the same as the ones documented
on the Sauce Wiki [here](https://wiki.saucelabs.com/display/DOCS/Test+Configuration+Options#TestConfigurationOptions-RequiredSeleniumTestConfigurationSettings).
For example:

```js
{
  "plugins": {
    "sauce": {
      "browsers": [{
          "browserName": "microsoftedge",
          "platform": "Windows 10",
          "version": ""
        }, {
          "browserName": "internet explorer",
          "platform": "Windows 8.1",
          "version": "11"
        },
        {
          "browserName": "safari",
          "platform": "OS X 10.11",
          "version": "9"
        }
      ]
    }
  }
}
```

## Variant dependencies

Sometimes you want to run your project's tests against different versions of
your dependencies. For example, suppose there was a significant change in
paper-button version `v1.5` and you want to confirm that your code works with
`v1.4` and `v1.5`.

WCT will serve from the `bower_components` directory in your project's root
directory as siblings of your project's root directory. So if you depend on
paper-button, you can import it with the url
`../paper-button/paper-button.html`.

For each directory that WCT detects with a name like
`bower_components-${variantName}`, it will also run your tests separately
against that variant of your dependencies. So you could use the [`directory`
environment variable](https://bower.io/docs/config/) option with bower to set
up a `bower_components-button-v1.4` directory while developing. WCT would
notice that directory and run your tests in two different variations, one where
`../paper-button/paper-button.html` gets `v1.4`, the other where it gets
`v1.5`.

This is implemented by starting one test server per variant, and one copy of each launched browser per test server.

# Nitty Gritty

## Polymer

By default, WCT will defer tests until `WebComponentsReady` has fired. This
saves you from having to wait for elements to upgrade and all that yourself.

If you need to test something that occurs before that event, the [`testImmediate` helper](https://github.com/Polymer/web-component-tester/blob/master/browser/environment/helpers.js#L29-41) can be used. Or, if you just want tests to run as soon as possible, you can disable the delay by setting `WCT.waitForFrameworks = false` (though, they are still async due to Mocha).


## Mocha

WCT supports Mocha's [TDD][mocha-tdd] (`suite`/`test`/etc) and [BDD][mocha-bdd]
(`describe`/`it`/etc) interfaces, and will call `mocha.setup`/`mocha.run` for
you. Just write your tests, and you're set.


## Chai

Similarly, Chai's [`expect`][chai-bdd] and [`assert`][chai-tdd] interfaces are
exposed for your convenience.


## Custom Environments

If you would rather not load WCT's shared environment, or want to have WCT
load additional libraries, you can override the list of scripts loaded. There
are two ways of doing this:

Inside your test code (before `browser.js` is loaded):
```html
<script>
  WCT = {
    environmentScripts: [
      // Mocha and Stacky are required dependencies
      'stacky/lib/parsing.js',
      'stacky/lib/formatting.js',
      'stacky/lib/normalization.js',
      'mocha/mocha.js',
      // Include anything else that you like!
    ],
  };
```

Alternatively, you can specify these options via the `clientOptions`
key in `wct.conf.json`.

A reference of the default configuration can be found at
[browser/config.ts](browser/config.ts).


## Gulp

We also provide Gulp tasks for your use. `gulpfile.js`:

```js
var gulp = require('gulp');
require('web-component-tester').gulp.init(gulp, [dependencies]);
```

Exposes `gulp test:local` and `gulp test:remote`, which depend on the optional
`dependencies`.


## Grunt

Or, Grunt tasks, if you prefer. `gruntfile.js`:

```js
grunt.initConfig({
  'wct-test': {
    local: {
      options: {remote: false},
    },
    remote: {
      options: {remote: true},
    },
    chrome: {
      options: {browsers: ['chrome']},
    },
  },
});

grunt.loadNpmTasks('web-component-tester');
```

Gives you two grunt tasks: `wct-test:local` and `wct-test:remote`. The
`options` you can use are specified in [`runner/config.ts`](runner/config.ts).


# Plugin Authoring

A plugin is a node module that can hook into various steps of WCT's flow. It
looks like this:

`package.json`:
```js
{
  // ...
  "wct-plugin": {
    "cli-options": {
      // ... option configuration (nomnom)
    }
  }
}
```

`plugin.js` (the plugin's main module)
```js
module.exports = function(context, pluginOptions, plugin) {
  // ...
};
```

The plugin can subscribe to hooks via the [`Context`](runner/context.ts)
object. Any options (via wct.conf.json or command line) are merged into
`pluginOptions`. And, `plugin` is the instance of [`Plugin`](runner/plugin.ts)
for the plugin.

[wct-local](https://github.com/Polymer/wct-local) and
[wct-sauce](https://github.com/Polymer/wct-sauce) are example plugins you can
follow.


### Node support

WCT supports node versions 8 and up.

WCT supports running tests for web-components which are installed via npm to `node_modules` (instead of via bower to `bower_components`) by use of an `--npm` flag. Components which wish to support npm-based installation should include `wct-browser-legacy` in their `devDependencies`, which is a package that contains only the client-side javascript necessary for executing WCT tests in an npm-based environment.  WCT will attempt to identify which package provides the client-side code by checking for compatible packages in the following order of preference: `wct-mocha`, `wct-browser-legacy` and `web-component-tester`.  If you want to specify which package provides WCT client-side code, use the `--wct-package-name` flag or `wctPackageName` option in wct.conf.json with the npm package name. 


<!-- References -->
[async]:      https://github.com/caolan/async       "Async.js"
[chai-bdd]:   http://chaijs.com/api/bdd/            "Chai's BDD Interface"
[chai-tdd]:   http://chaijs.com/api/assert/         "Chai's TDD Interface"
[chai]:       http://chaijs.com/                    "Chai Assertion Library"
[java]:       https://java.com/download             "Java"
[mocha-bdd]:  http://mochajs.org/#bdd-interface     "Mocha's BDD Interface"
[mocha-tdd]:  http://mochajs.org/#tdd-interface     "Mocha's TDD Interface"
[mocha]:      http://mochajs.org/                   "Mocha Test Framework"
[sauce]:      http://saucelabs.com                  "Sauce Labs"
[opensauce]:  https://saucelabs.com/opensauce       "Open Sauce Testing"
[lodash]:     https://lodash.com/                   "Lo-Dash"
[sinon]:      http://sinonjs.org/                   "Sinon.JS"
[sinon-chai]: https://github.com/domenic/sinon-chai "Chai assertions for Sinon"
[test-fixture]: https://github.com/PolymerElements/test-fixture "Easy DOM fixture testing"
[a11ydevtools]: https://github.com/GoogleChrome/accessibility-developer-tools "A collection of audit rules checking for common accessibility problems, and an API for running these rules in an HTML page."
