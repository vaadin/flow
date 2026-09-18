'use strict';

var defineProperty = require('es-define-property');

var ES5 = require('./es5');
var ES6 = require('./es6');
var ES2015 = require('./es2015');

module.exports = ES2015;

// `module.exports.x =` form lets `cjs-module-lexer` statically detect these as named exports (ESM interop / attw)
module.exports.ES5 = ES5;
module.exports.ES6 = ES6;
module.exports.ES2015 = ES2015;

if (defineProperty) {
	// `ES2015` is `module.exports`; defining here rather than on `module.exports` keeps the lexer from de-listing the above
	defineProperty(ES2015, 'ES5', { enumerable: false });
	defineProperty(ES2015, 'ES6', { enumerable: false });
	defineProperty(ES2015, 'ES2015', { enumerable: false });
}
