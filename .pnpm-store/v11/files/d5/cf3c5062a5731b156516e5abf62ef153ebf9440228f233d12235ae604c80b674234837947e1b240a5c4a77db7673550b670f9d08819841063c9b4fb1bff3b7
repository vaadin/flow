"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * A set of common RegExp matchers for tokenizing CSS.
 */
const matcher = {
    whitespace: /\s/,
    whitespaceGreedy: /(\s+)/g,
    commentGreedy: /(\*\/)/g,
    boundary: /[\(\)\{\}'"@;:\s]/,
    stringBoundary: /['"]/
};
exports.matcher = matcher;
/**
 * An enumeration of Node types.
 */
var nodeType;
(function (nodeType) {
    nodeType["stylesheet"] = "stylesheet";
    nodeType["comment"] = "comment";
    nodeType["atRule"] = "atRule";
    nodeType["ruleset"] = "ruleset";
    nodeType["expression"] = "expression";
    nodeType["declaration"] = "declaration";
    nodeType["rulelist"] = "rulelist";
    nodeType["discarded"] = "discarded";
})(nodeType = exports.nodeType || (exports.nodeType = {}));
//# sourceMappingURL=common.js.map