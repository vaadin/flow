'use strict';

var parse = require('../');
var test = require('tape');

/* eslint no-proto: 0 */

// Not pollution as such, but verify protections working as intended.
test('trailing __proto__ key in dotted option ignored', function (t) {
	var argv = parse(['--a.__proto__', 'IGNORED']);
	t.deepEqual(argv.a, {});
	t.end();
});

// Not pollution as such, but verify protections working as intended.
test('trailing constructor key in dotted option ignored', function (t) {
	var argv = parse(['--a.constructor', 'IGNORED']);
	t.deepEqual(argv.a, {});
	t.end();
});

test('proto pollution', function (t) {
	var argv = parse(['--__proto__.x', '123']);
	t.equal({}.x, undefined);
	t.equal(argv.__proto__.x, undefined);
	t.equal(argv.x, undefined);
	t.end();
});

test('proto pollution (array)', function (t) {
	var argv = parse(['--x', '4', '--x', '5', '--x.__proto__.z', '789']);
	t.equal({}.z, undefined);
	t.deepEqual(argv.x, [4, 5]);
	t.equal(argv.x.z, undefined);
	t.equal(argv.x.__proto__.z, undefined);
	t.end();
});

test('proto pollution (number)', function (t) {
	var argv = parse(['--x', '5', '--x.__proto__.z', '100']);
	t.equal({}.z, undefined);
	t.equal((4).z, undefined);
	t.equal(argv.x, 5);
	t.equal(argv.x.z, undefined);
	t.end();
});

test('proto pollution (string)', function (t) {
	var argv = parse(['--x', 'abc', '--x.__proto__.z', 'def']);
	t.equal({}.z, undefined);
	t.equal('...'.z, undefined);
	t.equal(argv.x, 'abc');
	t.equal(argv.x.z, undefined);
	t.end();
});

test('proto pollution (constructor)', function (t) {
	var argv = parse(['--constructor.prototype.y', '123']);
	t.equal({}.y, undefined);
	t.equal(argv.y, undefined);
	t.end();
});
