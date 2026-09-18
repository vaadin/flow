"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
const document_1 = require("../parser/document");
class ParsedJsonDocument extends document_1.ParsedDocument {
    constructor() {
        super(...arguments);
        this.type = 'json';
    }
    visit(visitors) {
        this._visit(this.ast, visitors);
    }
    _visit(node, visitors) {
        for (const visitor of visitors) {
            visitor.visit(node);
        }
        if (Array.isArray(node)) {
            for (const value of node) {
                this._visit(value, visitors);
            }
        }
        else if (typeof node === 'object' && node !== null) {
            for (const value of Object.values(node)) {
                this._visit(value, visitors);
            }
        }
    }
    _sourceRangeForNode(_node) {
        throw new Error('Not Implemented.');
    }
    stringify(options = {}) {
        const { prettyPrint = true, indent = 2 } = options;
        return JSON.stringify(this.ast, null, prettyPrint ? indent : 0);
    }
}
exports.ParsedJsonDocument = ParsedJsonDocument;
//# sourceMappingURL=json-document.js.map