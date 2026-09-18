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
const shady = require("shady-css-parser");
const document_1 = require("../parser/document");
const cssbeautify = require("cssbeautify");
class ParsedCssDocument extends document_1.ParsedDocument {
    constructor() {
        super(...arguments);
        this.type = 'css';
    }
    visit(visitors) {
        for (const node of this) {
            for (const visitor of visitors) {
                visitor.visit(node);
            }
        }
    }
    sourceRangeForNode(node) {
        return this.sourceRangeForShadyRange(node.range);
    }
    /**
     * Takes a range from a shadycss node directly, rather than a shadycss node.
     * Useful when there are multiple ranges for a given node.
     */
    sourceRangeForShadyRange(range) {
        return this.offsetsToSourceRange(range.start, range.end);
    }
    _sourceRangeForNode(node) {
        return this.sourceRangeForShadyRange(node.range);
    }
    stringify(options = {}) {
        const { prettyPrint = true } = options;
        const beautifulResults = cssbeautify(shadyStringifier.stringify(this.ast), {
            indent: prettyPrint ? '  ' : '',
            autosemicolon: true,
            openbrace: 'end-of-line'
        });
        const indent = '  '.repeat(options.indent || 0);
        return beautifulResults.split('\n')
            .map((line) => line === '' ? '' : indent + line)
            .join('\n') +
            '\n';
    }
    *[Symbol.iterator]() {
        yield* shady.iterateOverAst(this.ast);
    }
}
exports.ParsedCssDocument = ParsedCssDocument;
const shadyStringifier = new shady.Stringifier();
//# sourceMappingURL=css-document.js.map