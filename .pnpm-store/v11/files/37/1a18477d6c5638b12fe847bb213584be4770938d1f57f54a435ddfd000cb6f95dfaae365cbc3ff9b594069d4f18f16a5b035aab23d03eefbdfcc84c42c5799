"use strict";
/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
const whatwg_url_1 = require("whatwg-url");
const model_1 = require("../model/model");
const esutil = require("./esutil");
const resolve_specifier_node_1 = require("./resolve-specifier-node");
const isWindows = require("is-windows");
const isPathSpecifier = (s) => /^\.{0,2}\//.test(s);
class ScannedJavascriptImport extends model_1.ScannedImport {
    constructor(url, sourceRange, urlSourceRange, ast, astNodePath, lazy, originalSpecifier, statementAst) {
        super('js-import', url, sourceRange, urlSourceRange, ast, lazy);
        this.specifier = originalSpecifier;
        this.statementAst = statementAst;
        this.astNode = ast;
        this.astNodePath = astNodePath;
    }
    constructImport(resolvedUrl, relativeUrl, importedDocument, _containingDocument) {
        return new JavascriptImport(resolvedUrl, relativeUrl, this.type, importedDocument, this.sourceRange, this.urlSourceRange, this.astNode, this.astNodePath, this.warnings, this.lazy, this.specifier, this.statementAst);
    }
}
exports.ScannedJavascriptImport = ScannedJavascriptImport;
class JavascriptImport extends model_1.Import {
    constructor(url, originalUrl, type, document, sourceRange, urlSourceRange, ast, astNodePath, warnings, lazy, specifier, statementAst) {
        super(url, originalUrl, type, document, sourceRange, urlSourceRange, ast, warnings, lazy);
        this.specifier = specifier;
        this.statementAst = statementAst;
        this.astNode = ast;
        this.astNodePath = astNodePath;
    }
}
exports.JavascriptImport = JavascriptImport;
class JavaScriptImportScanner {
    constructor(options) {
        this.moduleResolution = options && options.moduleResolution;
    }
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const imports = [];
            const warnings = [];
            const scanner = this;
            yield visit({
                enterCallExpression(node, _, path) {
                    // TODO(usergenic): There's no babel.Import type or babel.isImport()
                    // function right now, we have to just check the type property
                    // here until there is; please change to use babel.isImport(node.callee)
                    // once it is a thing.
                    if (node.callee.type !== 'Import') {
                        return;
                    }
                    if (node.arguments.length !== 1) {
                        warnings.push(new model_1.Warning({
                            message: 'Malformed import',
                            sourceRange: document.sourceRangeForNode(node),
                            severity: model_1.Severity.WARNING,
                            code: 'malformed-import',
                            parsedDocument: document,
                        }));
                        return;
                    }
                    const arg = node.arguments[0];
                    if (arg.type !== 'StringLiteral') {
                        warnings.push(new model_1.Warning({
                            message: 'Cannot analyze dynamic imports with non-literal arguments',
                            sourceRange: document.sourceRangeForNode(node),
                            severity: model_1.Severity.WARNING,
                            code: 'non-literal-import',
                            parsedDocument: document,
                        }));
                        return;
                    }
                    const specifier = arg.value;
                    imports.push(new ScannedJavascriptImport(scanner._resolveSpecifier(specifier, document, node, warnings), document.sourceRangeForNode(node), document.sourceRangeForNode(node.callee), { language: 'js', node, containingDocument: document }, path, true, specifier, esutil.getCanonicalStatement(path)));
                },
                enterImportDeclaration(node, _, path) {
                    const specifier = node.source.value;
                    imports.push(new ScannedJavascriptImport(scanner._resolveSpecifier(specifier, document, node, warnings), document.sourceRangeForNode(node), document.sourceRangeForNode(node.source), { language: 'js', node, containingDocument: document }, path, false, specifier, esutil.getCanonicalStatement(path)));
                },
                enterExportAllDeclaration(node, _parent, path) {
                    const specifier = node.source.value;
                    imports.push(new ScannedJavascriptImport(scanner._resolveSpecifier(specifier, document, node, warnings), document.sourceRangeForNode(node), document.sourceRangeForNode(node.source), { language: 'js', node, containingDocument: document }, path, false, specifier, esutil.getCanonicalStatement(path)));
                },
                enterExportNamedDeclaration(node, _parent, path) {
                    if (node.source == null) {
                        return;
                    }
                    const specifier = node.source.value;
                    imports.push(new ScannedJavascriptImport(scanner._resolveSpecifier(specifier, document, node, warnings), document.sourceRangeForNode(node), document.sourceRangeForNode(node.source), { language: 'js', node, containingDocument: document }, path, false, specifier, esutil.getCanonicalStatement(path)));
                }
            });
            return { features: imports, warnings };
        });
    }
    _resolveSpecifier(specifier, document, node, warnings) {
        if (whatwg_url_1.parseURL(specifier) !== null) {
            return specifier;
        }
        const documentURL = new whatwg_url_1.URL(document.baseUrl);
        if (documentURL.protocol !== 'file:') {
            warnings.push(new model_1.Warning({
                message: 'Cannot resolve module specifier in non-local document',
                sourceRange: document.sourceRangeForNode(node),
                severity: model_1.Severity.WARNING,
                code: 'cant-resolve-module-specifier',
                parsedDocument: document,
            }));
            return undefined;
        }
        let documentPath = decodeURIComponent(documentURL.pathname);
        if (isWindows() && documentPath.startsWith('/')) {
            documentPath = documentPath.substring(1);
        }
        if (this.moduleResolution === 'node') {
            try {
                // TODO (justinfagnani): we need to pass packageName and componentDir,
                // or maybe the UrlResolver, to get correct relative URLs for
                // named imports from the root package to its dependencies
                return resolve_specifier_node_1.resolve(specifier, documentPath);
            }
            catch (e) {
                // If the specifier was a name, we'll emit a warning below.
            }
        }
        if (isPathSpecifier(specifier)) {
            return specifier;
        }
        warnings.push(new model_1.Warning({
            message: 'Cannot resolve module specifier with resolution algorithm: ' +
                (this.moduleResolution || 'none'),
            sourceRange: document.sourceRangeForNode(node),
            severity: model_1.Severity.WARNING,
            code: 'cant-resolve-module-specifier',
            parsedDocument: document,
        }));
        return undefined;
    }
}
exports.JavaScriptImportScanner = JavaScriptImportScanner;
//# sourceMappingURL=javascript-import-scanner.js.map