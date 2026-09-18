"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Like NodeFactory, only doesn't care about ranges, or types.
 */
const shady_css_1 = require("../shady-css");
class TestNodeFactory {
    stylesheet(rules) {
        return { type: shady_css_1.nodeType.stylesheet, rules };
    }
    atRule(name, parameters, rulelist = undefined) {
        return { type: shady_css_1.nodeType.atRule, name, parameters, rulelist };
    }
    comment(value) {
        return { type: shady_css_1.nodeType.comment, value };
    }
    rulelist(rules) {
        return { type: shady_css_1.nodeType.rulelist, rules };
    }
    ruleset(selector, rulelist) {
        return { type: shady_css_1.nodeType.ruleset, selector, rulelist };
    }
    declaration(name, value) {
        return { type: shady_css_1.nodeType.declaration, name, value };
    }
    expression(text) {
        return { type: shady_css_1.nodeType.expression, text };
    }
    discarded(text) {
        return { type: shady_css_1.nodeType.discarded, text };
    }
}
exports.TestNodeFactory = TestNodeFactory;
//# sourceMappingURL=test-node-factory.js.map