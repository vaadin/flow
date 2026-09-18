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
Object.defineProperty(exports, "__esModule", { value: true });
const babel = require("@babel/types");
const util = require("util");
const esutil = require("../javascript/esutil");
const feature_1 = require("./feature");
const warning_1 = require("./warning");
/**
 * A reference to another feature by identifier.
 */
class ScannedReference extends feature_1.ScannedFeature {
    constructor(kind, identifier, sourceRange, astNode, astPath, description, jsdoc, warnings) {
        super(sourceRange, astNode, description, jsdoc, warnings);
        this.kind = kind;
        this.astNode = astNode;
        this.astPath = astPath;
        this.sourceRange = sourceRange;
        this.identifier = identifier;
    }
    resolve(document) {
        return this.resolveWithKind(document, this.kind);
    }
    // Leaving this as a public method, in case we want to use a more
    // specific kind (e.g. resolve a PolymerElement rather than just a Class).
    resolveWithKind(document, kind) {
        let feature;
        const warnings = [...this.warnings];
        const scopedResult = resolveScopedAt(this.astPath, this.identifier, document, kind, this.sourceRange);
        if (scopedResult.successful) {
            feature = scopedResult.value;
        }
        else {
            if (scopedResult.error !== undefined) {
                warnings.push(scopedResult.error);
            }
        }
        // TODO(https://github.com/Polymer/polymer-analyzer/issues/917):
        //     Don't look things up in the global map if the scoped binding
        //     resolves.
        if (feature === undefined) {
            // We didn't find it by doing principled scope-based analysis. Let's try
            // looking it up in our big global map!
            const features = document.getFeatures({ imported: true, externalPackages: true, kind, id: this.identifier });
            if (this.sourceRange) {
                if (features.size === 0) {
                    let message = `Could not resolve reference to ${this.kind}`;
                    if (kind === 'behavior') {
                        message += `. Is it annotated with @polymerBehavior?`;
                    }
                    warnings.push(new warning_1.Warning({
                        code: 'could-not-resolve-reference',
                        sourceRange: this.sourceRange,
                        message,
                        parsedDocument: document.parsedDocument,
                        severity: warning_1.Severity.WARNING
                    }));
                }
                else if (features.size > 1) {
                    warnings.push(new warning_1.Warning({
                        code: 'multiple-global-declarations',
                        sourceRange: this.sourceRange,
                        message: `Multiple global declarations of ${this.kind} with identifier ${this.identifier}`,
                        parsedDocument: document.parsedDocument,
                        severity: warning_1.Severity.WARNING
                    }));
                }
            }
            [feature] = features;
        }
        return new Reference(this.identifier, this.sourceRange, this.astNode, feature, warnings);
    }
}
exports.ScannedReference = ScannedReference;
function resolveScopedAt(path, identifier, document, kind, sourceRange) {
    // Handle all kinds of imports except namespace imports (see below for them).
    if (isSomeKindOfImport(path)) {
        const exportedIdentifier = getExportedIdentifier(path.node, identifier);
        if (exportedIdentifier === undefined) {
            if (sourceRange === undefined) {
                return { successful: false, error: undefined };
            }
            else {
                return {
                    successful: false,
                    error: new warning_1.Warning({
                        code: 'could-not-resolve-reference',
                        message: `Could not resolve reference to '${identifier}' ` +
                            `with kind ${kind}`,
                        severity: warning_1.Severity.WARNING,
                        sourceRange,
                        parsedDocument: document.parsedDocument,
                    }),
                };
            }
        }
        return resolveThroughImport(path, exportedIdentifier, document, kind, sourceRange);
    }
    if (babel.isExportNamedDeclaration(path.node) && !path.node.source) {
        for (const specifier of path.node.specifiers) {
            if (specifier.exported.name !== specifier.local.name &&
                specifier.exported.name === identifier) {
                // In cases like `export {foo as bar}`, we need to look for a feature
                // called `foo` instead of `bar`.
                return resolveScopedAt(path, specifier.local.name, document, kind, sourceRange);
            }
        }
    }
    const statement = esutil.getCanonicalStatement(path);
    if (statement === undefined) {
        return { successful: false, error: undefined };
    }
    const features = document.getFeatures({ kind, id: identifier, statement });
    if (features.size > 1) {
        // TODO(rictic): narrow down by identifier? warn?
        return { successful: false, error: undefined };
    }
    const [feature] = features;
    if (feature !== undefined) {
        return { successful: true, value: feature };
    }
    // Handle namespace imports. e.g.
    //     import * as foo from 'foo-library'; class X extends foo.Bar {}
    const hasASingleDotInName = /^[^\.]+\.[^\.]+$/;
    if (hasASingleDotInName.test(identifier)) {
        const [namespace, name] = identifier.split('.');
        const namespaceBinding = path.scope.getBinding(namespace);
        if (namespaceBinding !== undefined) {
            const node = namespaceBinding.path.node;
            if (babel.isImportNamespaceSpecifier(node)) {
                return resolveThroughImport(namespaceBinding.path, name, document, kind, sourceRange);
            }
        }
    }
    const binding = path.scope.getBinding(identifier);
    if (binding === undefined || binding.path.node === path.node) {
        return { successful: false, error: undefined };
    }
    return resolveScopedAt(binding.path, identifier, document, kind, sourceRange);
}
function resolveThroughImport(path, exportedAs, document, kind, sourceRange) {
    const statement = esutil.getCanonicalStatement(path);
    if (statement === undefined) {
        throw new Error(`Internal error, could not get statement for node of type ${path.node.type}`);
    }
    const [import_] = document.getFeatures({ kind: 'import', statement });
    if (import_ === undefined || import_.document === undefined) {
        // Import failed, maybe it could not be loaded.
        return { successful: false, error: undefined };
    }
    // If it was renamed like `import {foo as bar} from 'baz';` then
    // node.imported.name will be `foo`
    const [export_] = import_.document.getFeatures({ kind: 'export', id: exportedAs });
    if (export_ === undefined) {
        // That symbol was not exported from the other file.
        return { successful: false, error: undefined };
    }
    return resolveScopedAt(export_.astNodePath, exportedAs, import_.document, kind, sourceRange);
}
function isSomeKindOfImport(path) {
    const node = path.node;
    return babel.isImportSpecifier(node) ||
        babel.isImportDefaultSpecifier(node) ||
        (babel.isExportNamedDeclaration(node) && node.source != null) ||
        (babel.isExportAllDeclaration(node));
}
function getExportedIdentifier(node, localIdentifier) {
    switch (node.type) {
        case 'ImportDefaultSpecifier':
            return 'default';
        case 'ExportNamedDeclaration':
            for (const specifier of node.specifiers) {
                if (specifier.exported.name === localIdentifier) {
                    return specifier.local.name;
                }
            }
            return undefined;
        case 'ExportAllDeclaration':
            // Can't rename through an export all, the name we're looking for in
            // this file is the same name in the next file.
            return localIdentifier;
        case 'ImportSpecifier':
            return node.imported.name;
    }
    return assertNever(node);
}
function assertNever(never) {
    throw new Error(`Unexpected ast node: ${util.inspect(never)}`);
}
const referenceSet = new Set(['reference']);
const emptySet = new Set();
/**
 * A reference to another feature by identifier.
 */
class Reference {
    constructor(identifier, sourceRange, astNode, feature, warnings) {
        this.kinds = referenceSet;
        this.identifiers = emptySet;
        this.identifier = identifier;
        this.sourceRange = sourceRange;
        this.astNode = astNode;
        this.warnings = warnings;
        this.feature = feature;
    }
}
exports.Reference = Reference;
//# sourceMappingURL=reference.js.map