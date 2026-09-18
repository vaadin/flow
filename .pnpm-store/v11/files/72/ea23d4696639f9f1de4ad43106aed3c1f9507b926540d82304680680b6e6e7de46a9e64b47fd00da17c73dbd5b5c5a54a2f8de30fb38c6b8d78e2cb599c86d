'use strict';

// var Construct = require('es-abstract/2025/Construct');
var CreateRegExpStringIterator = require('es-abstract/2025/CreateRegExpStringIterator');
var Get = require('es-abstract/2025/Get');
var Set = require('es-abstract/2025/Set');
var SpeciesConstructor = require('es-abstract/2025/SpeciesConstructor');
var ToLength = require('es-abstract/2025/ToLength');
var ToString = require('es-abstract/2025/ToString');
var flagsGetter = require('regexp.prototype.flags');
var setFunctionName = require('set-function-name');
var callBound = require('call-bound');
var GetIntrinsic = require('get-intrinsic');
var $TypeError = require('es-errors/type');
var isObject = require('es-object-atoms/isObject');

var $indexOf = callBound('String.prototype.indexOf');
var $hasOwn = callBound('Object.prototype.hasOwnProperty');

var OrigRegExp = GetIntrinsic('%RegExp%');

var hasFlagsGetter = 'flags' in OrigRegExp.prototype;
var supportsFlags = hasFlagsGetter && (/a/mig).flags === 'gim';

function constructRegexWithFlags(C, R) {
	var matcher;
	/*
	 * workaround for older engines that lack RegExp.prototype.flags, or that have a buggy one:
	 * with no getter to be fooled by, trust the whole chain; with a broken one, trust only own properties
	 */
	var readFlags = supportsFlags || (hasFlagsGetter ? $hasOwn(R, 'flags') : 'flags' in R);
	var flags = ToString(readFlags ? Get(R, 'flags') : flagsGetter(R));
	if (hasFlagsGetter) {
		matcher = new C(R, flags);
	} else if (C === OrigRegExp) {
		// workaround for older engines that can not construct a RegExp with flags
		matcher = new C(R.source, flags);
	} else {
		matcher = new C(R, flags);
	}
	return { flags: flags, matcher: matcher };
}

var regexMatchAll = setFunctionName(function SymbolMatchAll(string) {
	var R = this;
	if (!isObject(R)) {
		throw new $TypeError('"this" value must be an Object');
	}
	var S = ToString(string);
	var C = SpeciesConstructor(R, OrigRegExp);

	var tmp = constructRegexWithFlags(C, R);
	// var flags = ToString(Get(R, 'flags'));
	var flags = tmp.flags;
	// var matcher = Construct(C, [R, flags]);
	var matcher = tmp.matcher;

	var lastIndex = ToLength(Get(R, 'lastIndex'));
	Set(matcher, 'lastIndex', lastIndex, true);
	var global = $indexOf(flags, 'g') > -1;
	var fullUnicode = $indexOf(flags, 'u') > -1 || $indexOf(flags, 'v') > -1;
	return CreateRegExpStringIterator(matcher, S, global, fullUnicode);
}, '[Symbol.matchAll]', true);

module.exports = regexMatchAll;
