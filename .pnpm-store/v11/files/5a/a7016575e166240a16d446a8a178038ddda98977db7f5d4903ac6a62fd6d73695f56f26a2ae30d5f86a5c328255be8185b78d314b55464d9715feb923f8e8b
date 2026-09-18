'use strict';

var hasSymbols = require('has-symbols')();
var gOPD = require('gopd');
var isObject = require('es-object-atoms/isObject');
var setFunctionName = require('set-function-name');
var callBind = require('call-bind');

var implementation = require('./implementation');

var callBoundImplementation = callBind(implementation);

var $defineProperty = Object.defineProperty;

function looksUpMatcherOnPrimitives(matchAll) {
	if (!hasSymbols || typeof Symbol.matchAll !== 'symbol' || !gOPD || !$defineProperty) {
		return false;
	}

	var looked = false;
	var original = gOPD(Boolean.prototype, Symbol.matchAll);
	try {
		$defineProperty(Boolean.prototype, Symbol.matchAll, {
			configurable: true,
			enumerable: false,
			value: function () {
				looked = true;
				return [];
			},
			writable: true
		});
	} catch (e) {
		return false;
	}

	try {
		callBind(matchAll)('', true);
	} catch (e) { /**/ }

	try {
		if (original) {
			$defineProperty(Boolean.prototype, Symbol.matchAll, original);
		} else {
			delete Boolean.prototype[Symbol.matchAll];
		}
	} catch (e) { /**/ }

	return looked;
}

var wrapped;
var wrappedNative;

/*
 * a native that predates the "is an Object" change is correct for every Object pattern, so keep using it there;
 * only a primitive pattern, which it inspects and the spec does not, is routed through the implementation.
 * memoized so repeated `getPolyfill()` calls keep returning the same function.
 */
function wrapNative(nativeMatchAll) {
	if (wrappedNative !== nativeMatchAll) {
		wrappedNative = nativeMatchAll;
		var callBoundNative = callBind(nativeMatchAll);
		wrapped = setFunctionName(function matchAll(regexpOrPattern) {
			return isObject(regexpOrPattern)
				? callBoundNative(this, regexpOrPattern)
				: callBoundImplementation(this, regexpOrPattern);
		}, 'matchAll', true);
	}
	return wrapped;
}

module.exports = function getPolyfill() {
	if (String.prototype.matchAll) {
		try {
			''.matchAll(RegExp.prototype);
		} catch (e) {
			return looksUpMatcherOnPrimitives(String.prototype.matchAll)
				? wrapNative(String.prototype.matchAll)
				: String.prototype.matchAll;
		}
	}
	return implementation;
};
