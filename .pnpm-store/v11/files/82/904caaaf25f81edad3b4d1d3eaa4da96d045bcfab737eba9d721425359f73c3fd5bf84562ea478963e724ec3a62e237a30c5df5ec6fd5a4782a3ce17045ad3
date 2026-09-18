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
const babel = require("@babel/types");
const ast_value_1 = require("./ast-value");
const esutil = require("./esutil");
const jsdoc = require("./jsdoc");
const namespace_1 = require("./namespace");
/**
 * Find namespaces from source code.
 */
class NamespaceScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const visitor = new NamespaceVisitor(document);
            yield visit(visitor);
            return {
                features: [...visitor.namespaces.values()],
                warnings: visitor.warnings
            };
        });
    }
}
exports.NamespaceScanner = NamespaceScanner;
class NamespaceVisitor {
    constructor(document) {
        this.namespaces = new Map();
        this.warnings = [];
        this.document = document;
    }
    /**
     * Look for object declarations with @namespace in the docs.
     */
    enterVariableDeclaration(node, _parent) {
        if (node.declarations.length !== 1) {
            return; // Ambiguous.
        }
        this._initNamespace(node, node.declarations[0].id);
    }
    /**
     * Look for object assignments with @namespace in the docs.
     */
    enterAssignmentExpression(node, parent) {
        this._initNamespace(parent, node.left);
    }
    enterObjectProperty(node, _parent) {
        this._initNamespace(node, node.key);
    }
    enterExpressionStatement(node) {
        if (!babel.isAssignmentExpression(node.expression) &&
            !babel.isMemberExpression(node.expression)) {
            return;
        }
        const jsdocAnn = jsdoc.parseJsdoc(esutil.getAttachedComment(node) || '');
        if (!jsdoc.hasTag(jsdocAnn, 'memberof')) {
            return;
        }
        const memberofTag = jsdoc.getTag(jsdocAnn, 'memberof');
        const namespaceName = memberofTag && memberofTag.description;
        let prop = undefined;
        let namespacedIdentifier;
        if (!namespaceName || !this.namespaces.has(namespaceName)) {
            return;
        }
        if (babel.isAssignmentExpression(node.expression)) {
            if (babel.isFunctionExpression(node.expression.right)) {
                return;
            }
            namespacedIdentifier = ast_value_1.getIdentifierName(node.expression.left);
        }
        else if (babel.isMemberExpression(node.expression)) {
            namespacedIdentifier = ast_value_1.getIdentifierName(node.expression);
        }
        if (!namespacedIdentifier ||
            namespacedIdentifier.indexOf('.prototype.') !== -1) {
            return;
        }
        const namespace = this.namespaces.get(namespaceName);
        const name = namespacedIdentifier.substring(namespacedIdentifier.lastIndexOf('.') + 1);
        prop = this._createPropertyFromExpression(name, node.expression, jsdocAnn);
        if (prop) {
            namespace.properties.set(name, prop);
        }
    }
    _createPropertyFromExpression(name, node, jsdocAnn) {
        let description;
        let type;
        let readOnly = false;
        const privacy = esutil.getOrInferPrivacy(name, jsdocAnn);
        const sourceRange = this.document.sourceRangeForNode(node);
        const warnings = [];
        if (jsdocAnn) {
            description = jsdoc.getDescription(jsdocAnn);
            readOnly = jsdoc.hasTag(jsdocAnn, 'readonly');
        }
        let detectedType;
        if (babel.isAssignmentExpression(node)) {
            detectedType = esutil.getClosureType(node.right, jsdocAnn, sourceRange, this.document);
        }
        else {
            detectedType =
                esutil.getClosureType(node, jsdocAnn, sourceRange, this.document);
        }
        if (detectedType.successful) {
            type = detectedType.value;
        }
        else {
            warnings.push(detectedType.error);
            type = '?';
        }
        return {
            name,
            astNode: { language: 'js', node, containingDocument: this.document },
            type,
            jsdoc: jsdocAnn,
            sourceRange,
            description,
            privacy,
            warnings,
            readOnly,
        };
    }
    _initNamespace(node, nameNode) {
        const comment = esutil.getAttachedComment(node);
        // Quickly filter down to potential candidates.
        if (!comment || comment.indexOf('@namespace') === -1) {
            return;
        }
        const analyzedName = ast_value_1.getIdentifierName(nameNode);
        const docs = jsdoc.parseJsdoc(comment);
        const namespaceTag = jsdoc.getTag(docs, 'namespace');
        const explicitName = namespaceTag && namespaceTag.name;
        let namespaceName;
        if (explicitName) {
            namespaceName = explicitName;
        }
        else if (analyzedName) {
            namespaceName = ast_value_1.getNamespacedIdentifier(analyzedName, docs);
        }
        else {
            // TODO(fks): Propagate a warning if name could not be determined
            return;
        }
        const sourceRange = this.document.sourceRangeForNode(node);
        if (!sourceRange) {
            throw new Error(`Unable to determine sourceRange for @namespace: ${comment}`);
        }
        const summaryTag = jsdoc.getTag(docs, 'summary');
        const summary = (summaryTag && summaryTag.description) || '';
        const description = docs.description;
        const properties = getNamespaceProperties(node, this.document);
        this.namespaces.set(namespaceName, new namespace_1.ScannedNamespace(namespaceName, description, summary, { language: 'js', node, containingDocument: this.document }, properties, docs, sourceRange));
    }
}
/**
 * Extracts properties from a given namespace node.
 */
function getNamespaceProperties(node, document) {
    const properties = new Map();
    let child;
    if (babel.isVariableDeclaration(node)) {
        if (node.declarations.length !== 1) {
            return properties;
        }
        const declaration = node.declarations[0].init;
        if (!babel.isObjectExpression(declaration)) {
            return properties;
        }
        child = declaration;
    }
    else if (babel.isExpressionStatement(node) &&
        babel.isAssignmentExpression(node.expression) &&
        babel.isObjectExpression(node.expression.right)) {
        child = node.expression.right;
    }
    else {
        return properties;
    }
    return esutil.extractPropertiesFromClassOrObjectBody(child, document);
}
//# sourceMappingURL=namespace-scanner.js.map