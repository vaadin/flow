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
const util = require("util");
const common_1 = require("./common");
function* iterateOverAst(node) {
    yield node;
    switch (node.type) {
        case common_1.nodeType.stylesheet:
            for (const rule of node.rules) {
                yield* iterateOverAst(rule);
            }
            return;
        case common_1.nodeType.ruleset:
            return yield* iterateOverAst(node.rulelist);
        case common_1.nodeType.rulelist:
            for (const rule of node.rules) {
                yield* iterateOverAst(rule);
            }
            return;
        case common_1.nodeType.declaration:
            if (node.value !== undefined) {
                yield* iterateOverAst(node.value);
            }
            return;
        case common_1.nodeType.atRule:
            if (node.rulelist) {
                yield* iterateOverAst(node.rulelist);
            }
            return;
        case common_1.nodeType.expression:
        case common_1.nodeType.comment:
        case common_1.nodeType.discarded:
            return; // no child nodes
        default:
            const never = node;
            console.error(`Got a node of unknown type: ${util.inspect(never)}`);
    }
}
exports.iterateOverAst = iterateOverAst;
//# sourceMappingURL=ast-iterator.js.map