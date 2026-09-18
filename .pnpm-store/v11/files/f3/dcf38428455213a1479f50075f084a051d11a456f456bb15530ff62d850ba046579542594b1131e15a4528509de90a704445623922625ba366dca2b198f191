minimatch-all
====

[![Build Status](https://secure.travis-ci.org/joshwnj/minimatch-all.png)](http://travis-ci.org/joshwnj/minimatch-all)

`minimatch` a path against multiple patterns.

Install
----

`npm install minimatch-all`

Usage
----

```js
var minimatchAll = require('minimatch-all');

// minimatch options (see <https://www.npmjs.org/package/minimatch> for details)
var opts = {};

var patterns = [
  // match all js files
  '**/*.js',

  // except for js files in the foo/ directory
  '!foo/*.js',

  // unless it's foo/bar.js
  'foo/bar.js',
];

minimatchAll('foo/foo.js', patterns, opts);
// false

minimatchAll('foo/bar.js', patterns, opts);
// true
```

License
----

MIT
