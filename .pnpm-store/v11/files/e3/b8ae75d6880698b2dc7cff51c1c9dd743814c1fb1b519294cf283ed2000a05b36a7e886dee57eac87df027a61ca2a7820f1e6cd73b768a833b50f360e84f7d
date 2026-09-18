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
const document_1 = require("./document");
const warning_1 = require("./warning");
/**
 * Represents an import, such as an HTML import, an external script or style
 * tag, or an JavaScript import.
 *
 * @template N The AST node type
 */
class ScannedImport {
    constructor(type, url, sourceRange, urlSourceRange, ast, lazy) {
        this.warnings = [];
        this.type = type;
        this.url = url;
        this.sourceRange = sourceRange;
        this.urlSourceRange = urlSourceRange;
        this.astNode = ast;
        this.lazy = lazy;
    }
    resolve(document) {
        const resolvedUrl = this.getLoadableUrlOrWarn(document);
        if (this.url === undefined || resolvedUrl === undefined) {
            // Warning will already have been added to the document if necessary, so
            // we can just return here.
            return undefined;
        }
        const importedDocumentOrWarning = document._analysisContext.getDocument(resolvedUrl);
        let importedDocument;
        if (!(importedDocumentOrWarning instanceof document_1.Document)) {
            this.addCouldNotLoadWarning(document, importedDocumentOrWarning);
            importedDocument = undefined;
        }
        else {
            importedDocument = importedDocumentOrWarning;
        }
        return this.constructImport(resolvedUrl, this.url, importedDocument, document);
    }
    constructImport(resolvedUrl, relativeUrl, importedDocument, _containingDocument) {
        return new Import(resolvedUrl, relativeUrl, this.type, importedDocument, this.sourceRange, this.urlSourceRange, this.astNode, this.warnings, this.lazy);
    }
    addCouldNotLoadWarning(document, warning) {
        const error = this.error && this.error.message || this.error ||
            warning && warning.message || '';
        if (error) {
            document.warnings.push(new warning_1.Warning({
                code: 'could-not-load',
                message: `Unable to load import: ${error}`,
                sourceRange: (this.urlSourceRange || this.sourceRange),
                severity: warning_1.Severity.ERROR,
                parsedDocument: document.parsedDocument,
            }));
        }
    }
    /**
     * Resolve the URL for this import and return it if the analyzer
     */
    getLoadableUrlOrWarn(document) {
        if (this.url === undefined) {
            return;
        }
        const resolvedUrl = document._analysisContext.resolver.resolve(document.parsedDocument.baseUrl, this.url, this);
        if (!resolvedUrl) {
            return;
        }
        if (!document._analysisContext.loader.canLoad(resolvedUrl)) {
            // We have no way to load this resolved URL, so we will warn.
            document.warnings.push(new warning_1.Warning({
                code: 'not-loadable',
                message: `URL loader not configured to load '${resolvedUrl}'`,
                sourceRange: (this.urlSourceRange || this.sourceRange),
                severity: warning_1.Severity.WARNING,
                parsedDocument: document.parsedDocument,
            }));
            return undefined;
        }
        return resolvedUrl;
    }
}
exports.ScannedImport = ScannedImport;
class Import {
    constructor(url, originalUrl, type, document, sourceRange, urlSourceRange, ast, warnings, lazy) {
        this.identifiers = new Set();
        this.kinds = new Set(['import']);
        this.url = url;
        this.originalUrl = originalUrl;
        this.type = type;
        this.document = document;
        this.kinds.add(this.type);
        this.sourceRange = sourceRange;
        this.urlSourceRange = urlSourceRange;
        this.astNode = ast;
        this.warnings = warnings;
        this.lazy = lazy;
        if (lazy) {
            this.kinds.add('lazy-import');
        }
    }
    toString() {
        return `<Import type="${this.type}" url="${this.url}">`;
    }
}
exports.Import = Import;
//# sourceMappingURL=import.js.map