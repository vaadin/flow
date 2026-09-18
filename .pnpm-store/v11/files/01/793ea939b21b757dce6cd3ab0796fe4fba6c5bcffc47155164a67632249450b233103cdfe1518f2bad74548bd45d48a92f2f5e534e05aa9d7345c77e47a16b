"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const shady = require("shady-css-parser");
class CssCustomPropertyScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const warnings = [];
            const visitor = new CssCustomPropertyVisitor(document);
            yield visit(visitor);
            return { features: visitor.features, warnings };
        });
    }
}
exports.CssCustomPropertyScanner = CssCustomPropertyScanner;
class CssCustomPropertyAssignment {
    constructor(name, sourceRange, astNode) {
        this.warnings = [];
        this.kinds = new Set(['css-custom-property-assignment']);
        this.identifiers = new Set([name]);
        this.name = name;
        this.sourceRange = sourceRange;
        this.astNode = astNode;
    }
    resolve() {
        return this;
    }
}
exports.CssCustomPropertyAssignment = CssCustomPropertyAssignment;
class CssCustomPropertyUse {
    constructor(name, sourceRange, astNode) {
        this.warnings = [];
        this.kinds = new Set(['css-custom-property-use']);
        this.identifiers = new Set([name]);
        this.sourceRange = sourceRange;
        this.name = name;
        this.astNode = astNode;
    }
    resolve() {
        return this;
    }
}
exports.CssCustomPropertyUse = CssCustomPropertyUse;
class CssCustomPropertyVisitor {
    constructor(document) {
        this.document = document;
        this.features = [];
    }
    visit(node) {
        if (node.type === shady.nodeType.declaration &&
            node.name.startsWith('--')) {
            this.features.push(new CssCustomPropertyAssignment(node.name, this.document.sourceRangeForShadyRange(node.nameRange), { language: 'css', node, containingDocument: this.document }));
        }
        else if (node.type === shady.nodeType.expression) {
            this.getCustomPropertiesIn(node, node.text, node.range);
        }
        else if (node.type === shady.nodeType.atRule && node.parametersRange) {
            this.getCustomPropertiesIn(node, node.parameters, node.parametersRange);
        }
    }
    getCustomPropertiesIn(node, text, range) {
        const matches = findAllMatchesInString(CssCustomPropertyVisitor.customPropRegex, text);
        const baseOffset = range.start;
        for (const { offset, matched } of matches) {
            const range = this.document.sourceRangeForShadyRange({
                start: offset + baseOffset,
                end: offset + baseOffset + matched.length
            });
            this.features.push(new CssCustomPropertyUse(matched, range, { language: 'css', node, containingDocument: this.document }));
        }
    }
}
CssCustomPropertyVisitor.customPropRegex = /--[A-Za-z0-9_\-]+/;
function* findAllMatchesInString(regex, haystack) {
    regex = new RegExp(regex, 'g');
    let match;
    while (match = regex.exec(haystack)) {
        yield { offset: match.index, matched: match[0] };
    }
}
//# sourceMappingURL=css-custom-property-scanner.js.map