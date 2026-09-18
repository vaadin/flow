'use strict';

var GetIntrinsic = require('get-intrinsic');

var callBound = require('call-bound');
var safePushApply = require('safe-push-apply');

var $ownKeys = GetIntrinsic('%Reflect.ownKeys%', true);
var $gOPN = GetIntrinsic('%Object.getOwnPropertyNames%', true);
var $gOPS = GetIntrinsic('%Object.getOwnPropertySymbols%', true);

var $isEnumerable = callBound('Object.prototype.propertyIsEnumerable');

var keys = require('object-keys');

// eslint-disable-next-line array-bracket-newline
var order = ['b', 'd', 'a', 'c', 'g', 'e', 'h', 'f', 'j', 'i', 'l', 'k'];
/*
 * Some pre-ES2015 engines (notably V8 in node 0.8) enumerate own property names
 * in a per-realm randomized order rather than property-creation order, and a
 * `Reflect.ownKeys`/`getOwnPropertyNames` shim layered on top (e.g. es6-shim)
 * inherits that. `Object.keys` is ordered even there, so probe whichever key
 * source we're about to use and reconstruct from `object-keys` when it can't be
 * trusted to preserve insertion order.
 */
/** @param {(o: object) => (string | symbol)[]} getNames */
function preservesInsertionOrder(getNames) {
	/** @type {Record<string, boolean>} */
	var probe = {};
	var got;
	var i;
	for (i = 0; i < order.length; i += 1) {
		probe[order[i]] = true;
	}
	got = getNames(probe);
	if (got.length !== order.length) {
		return false;
	}
	for (i = 0; i < order.length; i += 1) {
		if (got[i] !== order[i]) {
			return false;
		}
	}
	return true;
}

var nativeOwnKeys = $ownKeys && preservesInsertionOrder($ownKeys) ? $ownKeys : null;
var trustGOPNOrder = !nativeOwnKeys && $gOPN && preservesInsertionOrder($gOPN);

/** @type {typeof import('.')} */
module.exports = nativeOwnKeys || function ownKeys(source) {
	/** @type {(keyof typeof source)[]} */
	var sourceKeys;
	var allNames;
	var name;
	var i;

	if ($gOPN && trustGOPNOrder) {
		sourceKeys = $gOPN(source);
	} else {
		/*
		 * The available key source can't be trusted to preserve order, so take the
		 * enumerable keys from `object-keys` (ordered) and borrow only the
		 * non-enumerable names from `getOwnPropertyNames` (their relative order is
		 * unrecoverable on such engines).
		 */
		sourceKeys = keys(source);
		if ($gOPN) {
			allNames = $gOPN(source);
			for (i = 0; i < allNames.length; i += 1) {
				name = allNames[i];
				if (!$isEnumerable(source, name)) {
					sourceKeys[sourceKeys.length] = name;
				}
			}
		}
	}
	if ($gOPS) {
		safePushApply(sourceKeys, $gOPS(source));
	}
	return sourceKeys;
};
