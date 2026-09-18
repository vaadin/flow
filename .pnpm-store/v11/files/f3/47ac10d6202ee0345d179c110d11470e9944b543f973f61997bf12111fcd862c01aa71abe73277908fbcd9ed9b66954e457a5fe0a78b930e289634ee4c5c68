'use strict';

var callBind = require('call-bind');
var define = require('define-properties');
var mockProperty = require('mock-property');
var test = require('tape');

var implementation = require('../implementation');
var regexMatchAll = require('../regexp-matchall');

var runTests = require('./tests');

var hasFlags = 'flags' in RegExp.prototype;

function requireFresh(id) {
	var resolved = require.resolve(id);
	delete require.cache[resolved];
	var fresh = require(id); // eslint-disable-line global-require
	delete require.cache[resolved];
	return fresh;
}

test('as a function', function (t) {
	runTests(callBind(implementation), callBind(regexMatchAll), t);

	t.end();
});

/*
 * mimics a broken native getter, the reason the `regexp.prototype.flags` shim exists: here it drops "g" from "gim",
 * so the module-level probe fails *and* a `gim` regex is observably wrong if the broken getter is consulted.
 * only "gim" is mangled: anything broader breaks `get-intrinsic`, whose path parser relies on
 * `Symbol.replace` reading the `flags` of its own `g` regex.
 */
var nativeFlags = hasFlags && define.supportsDescriptors && Object.getOwnPropertyDescriptor(RegExp.prototype, 'flags').get;

test('with a broken native `RegExp.prototype.flags`', { skip: !hasFlags || !define.supportsDescriptors }, function (t) {
	t.teardown(mockProperty(RegExp.prototype, 'flags', {
		get: function () {
			var flags = nativeFlags.call(this);
			return flags === 'gim' ? 'im' : flags;
		}
	}));

	var freshMatchAll = requireFresh('../implementation');
	var freshRegexMatchAll = requireFresh('../regexp-matchall');

	t.test('String.prototype.matchAll', function (st) {
		st.doesNotThrow(
			function () { freshMatchAll.call('aabc', /[ac]/gim); },
			'flags come from the `flags` shim, not the broken getter that drops "g"'
		);

		var nonGlobal = /[ac]/;
		Object.defineProperty(nonGlobal, 'flags', { configurable: true, value: 'g' });
		st.doesNotThrow(
			function () { freshMatchAll.call('aabc', nonGlobal); },
			'an own "flags" of "g" is honored on a non-global regex'
		);

		var global = /[ac]/g;
		Object.defineProperty(global, 'flags', { configurable: true, value: '' });
		st['throws'](
			function () { freshMatchAll.call('aabc', global); },
			TypeError,
			'an own "flags" without "g" throws, even on a global regex'
		);

		st.end();
	});

	t.test('RegExp.prototype[Symbol.matchAll]', function (st) {
		var regex = /b/;
		Object.defineProperty(regex, 'flags', { configurable: true, value: 'g' });

		var iterator = freshRegexMatchAll.call(regex, 'abcb');
		var indexes = [];
		var result;
		var i = 0;
		do {
			result = iterator.next();
			if (!result.done) { indexes.push(result.value.index); }
			i += 1;
		} while (!result.done && i < 4);
		st.deepEqual(indexes, [1, 3], 'an own "flags" of "g" produces a global matcher');

		st.end();
	});

	t.end();
});
