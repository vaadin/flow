# es-abstract-get <sup>[![Version Badge][npm-version-svg]][package-url]</sup>

[![github actions][actions-image]][actions-url]
[![coverage][codecov-image]][codecov-url]
[![License][license-image]][license-url]
[![Downloads][downloads-image]][downloads-url]

[![npm badge][npm-badge-png]][package-url]

The ECMAScript “getting properties” abstract operations — [`Get`][Get], [`GetV`][GetV], and [`GetMethod`][GetMethod] — plus the [`isPropertyKey`][isPropertyKey] helper they share, with TypeScript types.

## Motivation

These operations are also available in [`es-abstract`][es-abstract],
but `es-abstract` ships every abstract operation for every spec edition,
so depending on it to use one or two of them pulls in a very large dependency graph.
These three operations (and `isPropertyKey`) historically never differ between spec editions,
so they’re split out here:
a package that only needs `Get`, `GetV`, `GetMethod`, or `isPropertyKey` can depend on this instead and get a far smaller install.

Each operation is its own entry point - there is intentionally no main (`.`) export - so you only pay for what you import.

## Example

```js
var Get = require('es-abstract-get/Get');
var GetV = require('es-abstract-get/GetV');
var GetMethod = require('es-abstract-get/GetMethod');
var isPropertyKey = require('es-abstract-get/isPropertyKey');
var assert = require('assert');

// Get(O, P): O must be an Object
assert.equal(Get({ a: 1 }, 'a'), 1);

// GetV(V, P): V may be a primitive (it is coerced to an object for the lookup,
// but the original value is the receiver)
assert.equal(GetV('abc', 'length'), 3);

// GetMethod(O, P): returns undefined for null/undefined, throws if not callable
assert.equal(GetMethod({}, 'toString'), Object.prototype.toString);
assert.equal(GetMethod({ a: null }, 'a'), undefined);

// isPropertyKey(argument): true for strings and symbols
assert.equal(isPropertyKey('a'), true);
assert.equal(isPropertyKey(Symbol.iterator), true);
assert.equal(isPropertyKey(1), false);
```

## Tests
Simply clone the repo, `npm install`, and run `npm test`

[package-url]: https://npmjs.org/package/es-abstract-get
[npm-version-svg]: https://versionbadg.es/ljharb/es-abstract-get.svg
[npm-badge-png]: https://nodei.co/npm/es-abstract-get.png?downloads=true&stars=true
[license-image]: https://img.shields.io/npm/l/es-abstract-get.svg
[license-url]: LICENSE
[downloads-image]: https://img.shields.io/npm/dm/es-abstract-get.svg
[downloads-url]: https://npm-stat.com/charts.html?package=es-abstract-get
[codecov-image]: https://codecov.io/gh/ljharb/es-abstract-get/branch/main/graphs/badge.svg
[codecov-url]: https://app.codecov.io/gh/ljharb/es-abstract-get/
[actions-image]: https://img.shields.io/github/check-runs/ljharb/es-abstract-get/main
[actions-url]: https://github.com/ljharb/es-abstract-get/actions
[es-abstract]: https://npmjs.org/package/es-abstract
[Get]: https://tc39.es/ecma262/#sec-get-o-p
[GetV]: https://tc39.es/ecma262/#sec-getv
[GetMethod]: https://tc39.es/ecma262/#sec-getmethod
[isPropertyKey]: https://tc39.es/ecma262/#sec-ispropertykey
