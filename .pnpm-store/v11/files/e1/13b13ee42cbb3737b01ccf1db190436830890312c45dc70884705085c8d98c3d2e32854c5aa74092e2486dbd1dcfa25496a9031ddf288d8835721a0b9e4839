'use strict';

var Call = require('es-abstract/2025/Call');
var Get = require('es-abstract/2025/Get');
var GetMethod = require('es-abstract/2025/GetMethod');
var isObject = require('es-object-atoms/isObject');
var IsRegExp = require('es-abstract/2025/IsRegExp');
var RegExpCreate = require('es-abstract/2025/RegExpCreate');
var ToString = require('es-abstract/2025/ToString');

var RequireObjectCoercible = require('es-object-atoms/RequireObjectCoercible');

var callBound = require('call-bound');
var hasSymbols = require('has-symbols')();
var flagsGetter = require('regexp.prototype.flags');
var GetIntrinsic = require('get-intrinsic');
var $TypeError = require('es-errors/type');

var $RegExp = GetIntrinsic('%RegExp%');
var $indexOf = callBound('String.prototype.indexOf');
var $hasOwn = callBound('Object.prototype.hasOwnProperty');

var hasFlagsGetter = 'flags' in $RegExp.prototype;
var supportsFlags = hasFlagsGetter && (/a/mig).flags === 'gim';

var regexpMatchAllPolyfill = require('./polyfill-regexp-matchall');

function getMatcher(regexp) { // eslint-disable-line consistent-return
	var matcherPolyfill = regexpMatchAllPolyfill();
	if (hasSymbols && typeof Symbol.matchAll === 'symbol') {
		var matcher = GetMethod(regexp, Symbol.matchAll);
		if (typeof matcher !== 'undefined' && matcher === $RegExp.prototype[Symbol.matchAll] && matcher !== matcherPolyfill) {
			return matcherPolyfill;
		}
		return matcher;
	}
	// fallback for pre-Symbol.matchAll environments
	if (IsRegExp(regexp)) {
		return matcherPolyfill;
	}
}

module.exports = function matchAll(regexp) {
	var O = RequireObjectCoercible(this);

	if (isObject(regexp)) {
		var isRegExp = IsRegExp(regexp);
		if (isRegExp) {
			/*
			 * workaround for older engines that lack RegExp.prototype.flags, or that have a buggy one:
			 * with no getter to be fooled by, trust the whole chain; with a broken one, trust only own properties
			 */
			var readFlags = supportsFlags || (hasFlagsGetter ? $hasOwn(regexp, 'flags') : 'flags' in regexp);
			var flags = readFlags ? Get(regexp, 'flags') : flagsGetter(regexp);
			RequireObjectCoercible(flags);
			if ($indexOf(ToString(flags), 'g') < 0) {
				throw new $TypeError('matchAll requires a global regular expression');
			}
		}

		var matcher = getMatcher(regexp);
		if (typeof matcher !== 'undefined') {
			return Call(matcher, regexp, [O]);
		}
	}

	var S = ToString(O);
	var rx = RegExpCreate(regexp, 'g');
	return Call(getMatcher(rx), rx, [S]);
};
