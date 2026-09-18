'use strict';

var forEach = require('for-each');
var has = Object.prototype.hasOwnProperty;
var assign = require('object.assign');
var define = require('define-properties');
var entries = require('object.entries');
var inspect = require('object-inspect');
var hasSymbols = require('has-symbols')();
var mockProperty = require('mock-property');

var hasSticky = typeof (/a/).sticky === 'boolean';
var hasGroups = 'groups' in (/a/).exec('a');
/*
 * deliberately load-time: these must match what the modules under test saw when *they* were required, before
 * `es6-shim` runs - `supportsFlags` and es-abstract's `%Symbol.match%` are both captured once, at require time.
 */
var hasFlags = 'flags' in RegExp.prototype;
var hasSymbolMatch = hasSymbols && typeof Symbol.match === 'symbol';

// bounded, so a non-global regression reports a diff instead of hanging or throwing past the assertion
function matchIndexes(iterator, max) {
	var indexes = [];
	var result;
	var i = 0;
	do {
		result = iterator.next();
		if (!result.done) { indexes.push(result.value.index); }
		i += 1;
	} while (!result.done && i < (max || 6));
	return indexes;
}

function groups(matchObject) {
	return hasGroups ? assign(matchObject, { groups: matchObject.groups }, matchObject) : matchObject;
}

function arraySpread(iterator) {
	if (Array.isArray(iterator)) { return iterator; }
	var result;
	var values = [];
	do {
		result = iterator.next();
		values.push(result);
	} while (!result.done);
	return values;
}

function testResults(t, iterator, expectedResults, item) {
	var prefix = arguments.length > 3 ? inspect(item) + ': ' : '';
	var results = arraySpread(iterator);
	var expecteds = arraySpread(expectedResults);
	t.test(prefix + 'actual vs expected result lengths', function (st) {
		st.equal(results.length, expecteds.length, 'actual and expected result counts are the same');
		st.end();
	});
	t.test(prefix + 'actual vs expected results', { skip: results.length !== expecteds.length }, function (st) {
		forEach(expecteds, function (expected, index) {
			var result = results.shift();
			st.equal(result.done, expected.done, 'result ' + (index + 1) + ' is ' + (expected.done ? '' : 'not ') + 'done');
			st.test('result ' + (index + 1), { skip: result.done !== expected.done }, function (s2t) {
				if (expected.done) {
					s2t.equal(result.value, undefined, 'result ' + (index + 1) + ' value is undefined');
				} else {
					s2t.equal(Array.isArray(result.value), true, 'result ' + (index + 1) + ' value is an array');
					s2t.deepEqual(entries(result.value || {}), entries(expected.value || {}), 'result ' + (index + 1) + ' has the same entries');
					s2t.deepEqual(result.value, expected.value, 'result ' + (index + 1) + ' value is expected value');
				}
				s2t.end();
			});
		});
	});
}

module.exports = function (matchAll, regexMatchAll, t) {
	// computed here, not at module load: `getMatcher` re-reads `Symbol.matchAll` per call, and the shim installs it late
	var hasSymbolMatchAll = hasSymbols && typeof Symbol.matchAll === 'symbol';
	var hasUnicodeSets = 'unicodeSets' in RegExp.prototype;

	t.test('non-regexes', function (st) {
		var notRegexes = [
			[null, [{ value: undefined, done: true }]],
			[undefined, [
				{ value: assign([''], groups({ index: 0, input: 'abc' })), done: false },
				{ value: assign([''], groups({ index: 1, input: 'abc' })), done: false },
				{ value: assign([''], groups({ index: 2, input: 'abc' })), done: false },
				{ value: assign([''], groups({ index: 3, input: 'abc' })), done: false },
				{ value: undefined, done: true }
			]],
			[NaN, [{ value: undefined, done: true }]],
			[42, [{ value: undefined, done: true }]],
			[new Date(), [{ value: undefined, done: true }]],
			[{}, [
				{ value: assign(['b'], groups({ index: 1, input: 'abc' })), done: false },
				{ value: assign(['c'], groups({ index: 2, input: 'abc' })), done: false },
				{ value: undefined, done: true }
			]],
			[[], [
				{ value: assign([''], groups({ index: 0, input: 'abc' })), done: false },
				{ value: assign([''], groups({ index: 1, input: 'abc' })), done: false },
				{ value: assign([''], groups({ index: 2, input: 'abc' })), done: false },
				{ value: assign([''], groups({ index: 3, input: 'abc' })), done: false },
				{ value: undefined, done: true }
			]]
		];
		var str = 'abc';
		forEach(notRegexes, function (notRegex) {
			testResults(st, matchAll(str, notRegex[0]), notRegex[1], notRegex[0]);
		});
		st.end();
	});

	t.test('passing a string instead of a regex', function (st) {
		var str = 'aabcaba';
		testResults(st, matchAll(str, 'a'), matchAll(str, /a/g));
		st.end();
	});

	t.test('without `RegExp.prototype[Symbol.matchAll]`', { skip: !hasSymbols || typeof Symbol.matchAll !== 'symbol' }, function (st) {
		var descriptor = Object.getOwnPropertyDescriptor(RegExp.prototype, Symbol.matchAll);
		delete RegExp.prototype[Symbol.matchAll];
		st.teardown(function () {
			// eslint-disable-next-line no-extend-native
			if (descriptor) { Object.defineProperty(RegExp.prototype, Symbol.matchAll, descriptor); }
		});

		st['throws'](
			function () { matchAll('abc', /b/g); },
			TypeError,
			'a regex pattern throws, rather than substituting the polyfill matcher'
		);
		st['throws'](
			function () { matchAll('abc', 'b'); },
			TypeError,
			'a string pattern throws, rather than substituting the polyfill matcher'
		);

		st.end();
	});

	t.test('non-objects never have `Symbol.matchAll` looked up', { skip: !hasSymbolMatchAll }, function (st) {
		function poison() { throw new EvalError('`Symbol.matchAll` was looked up on a non-object'); }
		st.teardown(mockProperty(String.prototype, Symbol.matchAll, { nonEnumerable: true, value: poison }));
		st.teardown(mockProperty(Number.prototype, Symbol.matchAll, { nonEnumerable: true, value: poison }));
		st.teardown(mockProperty(Boolean.prototype, Symbol.matchAll, { nonEnumerable: true, value: poison }));

		var str = 'aabcaba';
		st.doesNotThrow(function () { matchAll(str, 'a'); }, 'a string pattern does not consult `String.prototype[Symbol.matchAll]`');
		st.doesNotThrow(function () { matchAll(str, 2); }, 'a number pattern does not consult `Number.prototype[Symbol.matchAll]`');
		st.doesNotThrow(function () { matchAll(str, true); }, 'a boolean pattern does not consult `Boolean.prototype[Symbol.matchAll]`');

		st.end();
	});

	t.test('objects with no matcher are stringified into a new regex', { skip: !hasSymbolMatch || !hasSymbolMatchAll }, function (st) {
		st.test('a regex whose `Symbol.match` is falsy', function (s2t) {
			var str = 'a/b/gc';
			var regex = /b/g;
			regex[Symbol.match] = false;
			regex[Symbol.matchAll] = undefined;

			var expectedResults = [
				{ value: assign(['/b/g'], groups({ index: 1, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, matchAll(str, regex), expectedResults);
			s2t.end();
		});

		st.test('a `Symbol.match`-y object is stringified, not read for "source"', function (s2t) {
			var str = 'abc';
			var fakeRegex = {
				flags: 'g',
				source: 'b',
				toString: function () { return 'c'; }
			};
			fakeRegex[Symbol.match] = true;

			var expectedResults = [
				{ value: assign(['c'], groups({ index: 2, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, matchAll(str, fakeRegex), expectedResults);
			s2t.end();
		});

		st.end();
	});

	t.test('ToString-able objects', function (st) {
		var str = 'aabc';
		var strObj = { toString: function () { return str; } };
		var regex = /[ac]/g;
		var expectedResults = [
			{ value: assign(['a'], groups({ index: 0, input: str })), done: false },
			{ value: assign(['a'], groups({ index: 1, input: str })), done: false },
			{ value: assign(['c'], groups({ index: 3, input: str })), done: false },
			{ value: undefined, done: true }
		];
		testResults(st, matchAll(strObj, regex), expectedResults);
		st.end();
	});

	t.test('#flags', function (st) {
		st.test('without a flags property', function (s2t) {
			var str = 'aabc';
			var regex = /[ac]/g;
			if (define.supportsDescriptors) {
				Object.defineProperty(regex, 'flags', { value: undefined });
			}
			s2t.equal(regex.flags, undefined, 'regex has an undefined "flags" property');
			s2t['throws'](
				function () { matchAll(str, regex); },
				'undefined flags throws'
			);
			s2t.end();
		});

		st.test('with no flags property at all', { skip: !hasSymbolMatch || !hasFlags }, function (s2t) {
			var fakeRegex = {};
			fakeRegex[Symbol.match] = true;
			fakeRegex.global = true;
			fakeRegex.source = 'b';

			s2t['throws'](
				function () { matchAll('abc', fakeRegex); },
				TypeError,
				'an absent "flags" property throws, rather than being derived from `global`'
			);
			s2t.end();
		});

		st.test('with a throwing flags getter', { skip: !define.supportsDescriptors }, function (s2t) {
			var str = 'aabc';

			var regex = /[ac]/g;
			Object.defineProperty(regex, 'flags', {
				configurable: true,
				get: function () { throw new EvalError('flags getter'); }
			});
			s2t['throws'](
				function () { matchAll(str, regex); },
				EvalError,
				'a throwing "flags" getter on a global regex throws'
			);

			var nonGlobalRegex = /[ac]/;
			Object.defineProperty(nonGlobalRegex, 'flags', {
				configurable: true,
				get: function () { throw new EvalError('flags getter'); }
			});
			s2t['throws'](
				function () { matchAll(str, nonGlobalRegex); },
				EvalError,
				'a throwing "flags" getter on a non-global regex throws before the global check'
			);

			s2t.test('on a non-regex with `Symbol.match`', { skip: !hasSymbolMatch }, function (s3t) {
				var fakeRegex = {};
				fakeRegex[Symbol.match] = true;
				Object.defineProperty(fakeRegex, 'flags', {
					configurable: true,
					get: function () { throw new EvalError('flags getter'); }
				});
				s3t['throws'](
					function () { matchAll(str, fakeRegex); },
					EvalError,
					'a throwing "flags" getter on a `Symbol.match`-y object throws'
				);
				s3t.end();
			});

			s2t.end();
		});

		st.test('with a static flags property', function (s2t) {
			var str = 'AaBC';
			var regex = /[ac]/;
			define(regex, { flags: 'ig' }, { flags: function () { return true; } });
			try {
				define(regex, { global: true }, { global: function () { return true; } });
				s2t.equal(regex.global, true);
			} catch (e) {
				s2t.comment('# SKIP in node < 6, `global` is not configurable on regexes');
				return s2t.end();
			}
			s2t.equal(regex.flags, 'ig');
			var expectedResults = [
				{ value: assign(['A'], groups({ index: 0, input: str })), done: false },
				{ value: assign(['a'], groups({ index: 1, input: str })), done: false },
				{ value: assign(['C'], groups({ index: 3, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, matchAll(str, regex), expectedResults);
			return s2t.end();
		});

		st.test('respects flags', function (s2t) {
			var str = 'A\na\nb\nC';
			var regex = /^[ac]/img;
			var expectedResults = [
				{ value: assign(['A'], groups({ index: 0, input: str })), done: false },
				{ value: assign(['a'], groups({ index: 2, input: str })), done: false },
				{ value: assign(['C'], groups({ index: 6, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, matchAll(str, regex), expectedResults);
			s2t.end();
		});

		st.test('throws with a non-global regex', function (s2t) {
			var str = 'AaBbCc';
			var regex = /[bc]/i;
			s2t['throws'](
				function () { matchAll(str, regex); },
				TypeError,
				'a non-global regex throws'
			);
			s2t.end();
		});

		st.test('works with a global non-sticky regex', function (s2t) {
			var str = 'AaBbCc';
			var regex = /[bc]/gi;
			var expectedResults = [
				{ value: assign(['B'], groups({ index: 2, input: str })), done: false },
				{ value: assign(['b'], groups({ index: 3, input: str })), done: false },
				{ value: assign(['C'], groups({ index: 4, input: str })), done: false },
				{ value: assign(['c'], groups({ index: 5, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, matchAll(str, regex), expectedResults);
			s2t.end();
		});
	});

	t.test('#[Symbol.matchAll]', function (st) {
		st.test('stringifies "flags" exactly once', { skip: !define.supportsDescriptors || !hasFlags }, function (s2t) {
			var str = 'abc';
			var count = 0;
			var regex = /b/;
			Object.defineProperty(regex, 'flags', {
				configurable: true,
				value: { toString: function () { count += 1; return 'g'; } }
			});

			var iterator = regexMatchAll(regex, str);
			s2t.equal(count, 1, '"flags" is stringified exactly once');

			var expectedResults = [
				{ value: assign(['b'], groups({ index: 1, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, iterator, expectedResults);
			s2t.end();
		});

		st.test('treats the `v` flag as full unicode', { skip: !hasUnicodeSets }, function (s2t) {
			var str = 'a😀b';
			forEach(['gu', 'gv'], function (flags) {
				s2t.deepEqual(
					matchIndexes(regexMatchAll(new RegExp('(?:)', flags), str)),
					[0, 1, 3, 4],
					inspect(flags) + ': zero-width matches advance by a whole code point'
				);
			});
			s2t.end();
		});

		st.test('with an inherited "flags" property, lacking `RegExp.prototype.flags`', { skip: hasFlags }, function (s2t) {
			function Fake() {}
			Fake.prototype.flags = 'g';
			Fake.prototype.source = 'b';
			Fake.prototype.lastIndex = 0;

			s2t.deepEqual(
				matchIndexes(regexMatchAll(new Fake(), 'abcb')),
				[1, 3],
				'an inherited "flags" of "g" produces a global matcher'
			);
			s2t.end();
		});

		st.test('without a "flags" property', { skip: !hasFlags }, function (s2t) {
			s2t['throws'](
				function () { regexMatchAll({ source: 'b', lastIndex: 0 }, 'abc'); },
				SyntaxError,
				'an absent "flags" property stringifies to "undefined", which is not a valid flags string'
			);
			s2t.end();
		});

		st.end();
	});

	t.test('returns an iterator', function (st) {
		var str = 'aabc';
		var iterator = matchAll(str, /[ac]/g);
		st.ok(iterator, 'iterator is truthy');
		st.equal(has.call(iterator, 'next'), false, 'iterator does not have own property "next"');
		for (var key in iterator) {
			st.fail('iterator has enumerable properties: ' + key);
		}
		var expectedResults = [
			{ value: assign(['a'], groups({ index: 0, input: str })), done: false },
			{ value: assign(['a'], groups({ index: 1, input: str })), done: false },
			{ value: assign(['c'], groups({ index: 3, input: str })), done: false },
			{ value: undefined, done: true }
		];
		testResults(st, iterator, expectedResults);
		st.end();
	});

	t.test('zero-width matches', function (st) {
		var str = 'abcde';

		st.test('global', function (s2t) {
			var expectedResults = [
				{ value: assign([''], groups({ index: 1, input: str })), done: false },
				{ value: assign([''], groups({ index: 2, input: str })), done: false },
				{ value: assign([''], groups({ index: 3, input: str })), done: false },
				{ value: assign([''], groups({ index: 4, input: str })), done: false },
				{ value: undefined, done: true }
			];
			testResults(s2t, matchAll(str, /\B/g), expectedResults);
			s2t.end();
		});

		st.test('sticky', { skip: !hasSticky }, function (s2t) {
			var expectedResults = [
				{ value: undefined, done: true }
			];

			/* eslint no-invalid-regexp: [2, { "allowConstructorFlags": ["y"] }] */
			var regex = new RegExp('\\B', 'y');
			s2t['throws'](
				function () { matchAll(str, regex); },
				TypeError,
				'non-global sticky regex throws'
			);

			/* eslint no-invalid-regexp: [2, { "allowConstructorFlags": ["y"] }] */
			testResults(s2t, matchAll(str, new RegExp('\\B', 'gy')), expectedResults);

			s2t.end();
		});

		st.test('unflagged', function (s2t) {
			s2t['throws'](
				function () { matchAll(str, /\B/); },
				TypeError,
				'unflagged regex throws'
			);
			s2t.end();
		});

		st.end();
	});
};
