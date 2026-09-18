'use strict';

var callBind = require('call-bind');
var define = require('define-properties');
var hasSymbols = require('has-symbols')();
var mockProperty = require('mock-property');
var test = require('tape');

var matchAllShim = require('../');
var getPolyfill = require('../polyfill');
var implementation = require('../implementation');
var regexMatchAll = require('../regexp-matchall');

var runTests = require('./tests');

function canDetect() {
	return hasSymbols && typeof Symbol.matchAll === 'symbol' && define.supportsDescriptors;
}

function throwsOnRegExpPrototype(pattern) {
	if (pattern === RegExp.prototype) {
		throw new TypeError('matchAll requires a global regular expression');
	}
}

var preES2026Calls = 0;
// eslint-disable-next-line func-style
var preES2026 = function matchAll(pattern) {
	preES2026Calls += 1;
	throwsOnRegExpPrototype(pattern);
	if (pattern !== null && typeof pattern !== 'undefined') {
		var matcher = pattern[Symbol.matchAll];
		if (typeof matcher === 'function') { return matcher.call(pattern, this); }
	}
	return [][Symbol.iterator]();
};

// eslint-disable-next-line func-style
var compliant = function matchAll(pattern) {
	throwsOnRegExpPrototype(pattern);
	if (pattern !== null && (typeof pattern === 'object' || typeof pattern === 'function')) {
		var matcher = pattern[Symbol.matchAll];
		if (typeof matcher === 'function') { return matcher.call(pattern, this); }
	}
	return [][Symbol.iterator]();
};

test('as a function', function (t) {
	runTests(matchAllShim, callBind(regexMatchAll), t);

	t.end();
});

test('getPolyfill', function (t) {
	if (!canDetect()) {
		t.comment('# SKIP `Symbol.matchAll` is not available');
		return t.end();
	}

	t.test('wraps a native that looks up `Symbol.matchAll` on primitives', function (st) {
		st.teardown(mockProperty(String.prototype, 'matchAll', { nonEnumerable: true, value: preES2026 }));

		var polyfill = getPolyfill();
		st.notEqual(polyfill, preES2026, 'a pre-ES2026 native is not returned as-is');
		st.notEqual(polyfill, implementation, 'nor is it discarded in favor of the implementation');
		st.equal(polyfill, getPolyfill(), 'the wrapper is memoized, so the identity is stable');
		st.equal(polyfill.length, 1, 'the wrapper has a length of 1');

		var before = preES2026Calls;
		polyfill.call('abc', /b/g);
		st.equal(preES2026Calls, before + 1, 'an Object pattern is delegated to the native');

		st.teardown(mockProperty(String.prototype, Symbol.matchAll, {
			nonEnumerable: true,
			value: function () { throw new EvalError('`Symbol.matchAll` was looked up on a non-object'); }
		}));
		before = preES2026Calls;
		st.doesNotThrow(
			function () { polyfill.call('abc', 'b'); },
			'a primitive pattern does not reach the native, so `Symbol.matchAll` is never looked up on it'
		);
		st.equal(preES2026Calls, before, 'a primitive pattern is not delegated to the native');

		st.end();
	});

	t.test('keeps a native that only inspects Objects', function (st) {
		st.teardown(mockProperty(String.prototype, 'matchAll', { nonEnumerable: true, value: compliant }));

		st.equal(getPolyfill(), compliant, 'a compliant native is kept');
		st.end();
	});

	t.test('restores `Boolean.prototype` after probing it', function (st) {
		function sentinel() { return [][Symbol.iterator](); }
		st.teardown(mockProperty(Boolean.prototype, Symbol.matchAll, { nonEnumerable: true, value: sentinel }));
		st.teardown(mockProperty(String.prototype, 'matchAll', { nonEnumerable: true, value: preES2026 }));

		st.notEqual(getPolyfill(), preES2026, 'the probe ran');
		st.equal(
			Boolean.prototype[Symbol.matchAll],
			sentinel,
			'a pre-existing `Boolean.prototype[Symbol.matchAll]` survives the probe'
		);
		st.end();
	});

	return t.end();
});
