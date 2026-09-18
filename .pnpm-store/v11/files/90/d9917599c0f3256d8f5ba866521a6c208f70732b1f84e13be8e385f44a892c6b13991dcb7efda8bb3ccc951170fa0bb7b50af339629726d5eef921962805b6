"use strict";
/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
const babel = require("@babel/types");
const esutil = require("./esutil");
const exportKinds = new Set(['export']);
class Export {
    constructor(astNode, statementAst, sourceRange, nodePath, exportingAllFrom) {
        this.kinds = exportKinds;
        this.identifiers = new Set();
        this.warnings = [];
        this.astNode = astNode;
        this.statementAst = statementAst;
        let exportedIdentifiers;
        if (exportingAllFrom !== undefined) {
            exportedIdentifiers = flatMap(exportingAllFrom, (export_) => [...export_.identifiers].filter((i) => i !== 'default'));
        }
        else {
            exportedIdentifiers = esutil.getBindingNamesFromDeclaration(astNode.node);
        }
        for (const name of exportedIdentifiers) {
            this.identifiers.add(name);
        }
        this.astNodePath = nodePath;
        this.sourceRange = sourceRange;
    }
    // TODO: Could potentially get a speed boost by doing the Reference
    //   resolution algorithm here, rather than re-doing it every single place
    //   this export is referenced.
    resolve(document) {
        if (babel.isExportAllDeclaration(this.astNode.node)) {
            const [import_] = document.getFeatures({ kind: 'import', statement: this.statementAst });
            if (import_ === undefined || import_.document === undefined) {
                // Import did not resolve.
                return undefined;
            }
            return new Export(this.astNode, this.statementAst, this.sourceRange, this.astNodePath, import_.document.getFeatures({ kind: 'export' }));
        }
        // It's immutable, and it doesn't care about other documents, so it's
        // both a ScannedFeature and a Feature. This is just one step in an
        // arbitrarily long chain of references.
        return this;
    }
}
exports.Export = Export;
function* flatMap(inputs, map) {
    for (const input of inputs) {
        yield* map(input);
    }
}
class JavaScriptExportScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const exports = [];
            const warnings = [];
            yield visit({
                enterExportNamedDeclaration(node, _parent, path) {
                    exports.push(new Export({ language: 'js', containingDocument: document, node }, esutil.getCanonicalStatement(path), document.sourceRangeForNode(node), path));
                },
                enterExportAllDeclaration(node, _parent, path) {
                    exports.push(new Export({ language: 'js', containingDocument: document, node }, esutil.getCanonicalStatement(path), document.sourceRangeForNode(node), path));
                },
                enterExportDefaultDeclaration(node, _parent, path) {
                    exports.push(new Export({ language: 'js', containingDocument: document, node }, esutil.getCanonicalStatement(path), document.sourceRangeForNode(node), path));
                }
            });
            return { features: exports, warnings };
        });
    }
}
exports.JavaScriptExportScanner = JavaScriptExportScanner;
//# sourceMappingURL=javascript-export-scanner.js.map