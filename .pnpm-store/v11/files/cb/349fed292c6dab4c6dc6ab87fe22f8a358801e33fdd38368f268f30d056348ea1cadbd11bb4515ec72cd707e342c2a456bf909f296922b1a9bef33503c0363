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
Object.defineProperty(exports, "__esModule", { value: true });
const babel = require("@babel/types");
const ast_value_1 = require("../javascript/ast-value");
const class_scanner_1 = require("../javascript/class-scanner");
const esutil = require("../javascript/esutil");
const esutil_1 = require("../javascript/esutil");
const jsdoc = require("../javascript/jsdoc");
const polymer_element_mixin_1 = require("./polymer-element-mixin");
const polymer2_config_1 = require("./polymer2-config");
class MixinVisitor {
    constructor(document, prototypeMemberFinder) {
        this.mixins = [];
        this._currentMixin = null;
        this._currentMixinNode = null;
        this._currentMixinFunction = null;
        this.warnings = [];
        this._document = document;
        this._prototypeMemberFinder = prototypeMemberFinder;
    }
    enterAssignmentExpression(node, _parent, path) {
        this.tryInitializeMixin(path, node.left);
    }
    enterFunctionDeclaration(node, _parent, path) {
        this.tryInitializeMixin(path, node.id);
    }
    leaveFunctionDeclaration(node, _parent) {
        this.clearOnLeave(node);
    }
    enterVariableDeclaration(node, _parent, path) {
        this.tryInitializeMixin(path, node.declarations[0].id);
    }
    leaveVariableDeclaration(node, _parent) {
        this.clearOnLeave(node);
    }
    tryInitializeMixin(nodePath, nameNode) {
        const comment = esutil.getBestComment(nodePath);
        if (comment === undefined) {
            return;
        }
        const docs = jsdoc.parseJsdoc(comment);
        if (!hasMixinFunctionDocTag(docs)) {
            return;
        }
        const node = nodePath.node;
        const name = ast_value_1.getIdentifierName(nameNode);
        const namespacedName = name ? ast_value_1.getNamespacedIdentifier(name, docs) : undefined;
        if (namespacedName === undefined) {
            // TODO(#903): Warn about a mixin whose name we can't infer?
            return;
        }
        const sourceRange = this._document.sourceRangeForNode(node);
        const summaryTag = jsdoc.getTag(docs, 'summary');
        const mixin = new polymer_element_mixin_1.ScannedPolymerElementMixin({
            name: namespacedName,
            sourceRange,
            astNode: { language: 'js', node, containingDocument: this._document },
            statementAst: esutil.getCanonicalStatement(nodePath),
            description: docs.description,
            summary: (summaryTag && summaryTag.description) || '',
            privacy: esutil_1.getOrInferPrivacy(namespacedName, docs),
            jsdoc: docs,
            mixins: jsdoc.getMixinApplications(this._document, node, docs, this.warnings, nodePath)
        });
        this._currentMixin = mixin;
        this._currentMixinNode = node;
        this.mixins.push(this._currentMixin);
    }
    clearOnLeave(node) {
        if (this._currentMixinNode === node) {
            this._currentMixin = null;
            this._currentMixinNode = null;
            this._currentMixinFunction = null;
        }
    }
    enterFunctionExpression(node, _parent) {
        if (this._currentMixin != null && this._currentMixinFunction == null) {
            this._currentMixinFunction = node;
        }
    }
    enterArrowFunctionExpression(node, _parent) {
        if (this._currentMixin != null && this._currentMixinFunction == null) {
            this._currentMixinFunction = node;
        }
    }
    enterClassExpression(node, parent) {
        if (!babel.isReturnStatement(parent) &&
            !babel.isArrowFunctionExpression(parent)) {
            return;
        }
        this._handleClass(node);
    }
    enterClassDeclaration(node, _parent) {
        const comment = esutil.getAttachedComment(node) || '';
        const docs = jsdoc.parseJsdoc(comment);
        const isMixinClass = hasMixinClassDocTag(docs);
        if (isMixinClass) {
            this._handleClass(node);
        }
    }
    _handleClass(node) {
        const mixin = this._currentMixin;
        if (mixin == null) {
            return;
        }
        mixin.classAstNode = node;
        const classProperties = class_scanner_1.extractPropertiesFromClass(node, this._document);
        for (const prop of classProperties.values()) {
            mixin.addProperty(prop);
        }
        polymer2_config_1.getPolymerProperties(node, this._document)
            .forEach((p) => mixin.addProperty(p));
        esutil_1.getMethods(node, this._document).forEach((m) => mixin.addMethod(m));
        esutil_1.getStaticMethods(node, this._document)
            .forEach((m) => mixin.staticMethods.set(m.name, m));
        mixin.events = esutil.getEventComments(node);
        // mixin.sourceRange = this._document.sourceRangeForNode(node);
        // Also add members that were described like:
        //   /** @type {string} */
        //   MixinClass.prototype.foo;
        const name = ast_value_1.getIdentifierName(node.id);
        if (name !== undefined) {
            const prototypeMembers = this._prototypeMemberFinder.members.get(name);
            if (prototypeMembers !== undefined) {
                for (const [, property] of prototypeMembers.properties) {
                    mixin.addProperty(property);
                }
                for (const [, method] of prototypeMembers.methods) {
                    mixin.addMethod(method);
                }
            }
        }
    }
}
exports.MixinVisitor = MixinVisitor;
function hasMixinFunctionDocTag(docs) {
    // TODO(justinfagnani): remove polymerMixin support
    return (jsdoc.hasTag(docs, 'polymer') &&
        jsdoc.hasTag(docs, 'mixinFunction')) ||
        jsdoc.hasTag(docs, 'polymerMixin');
}
exports.hasMixinFunctionDocTag = hasMixinFunctionDocTag;
function hasMixinClassDocTag(docs) {
    // TODO(justinfagnani): remove polymerMixinClass support
    return (jsdoc.hasTag(docs, 'polymer') && jsdoc.hasTag(docs, 'mixinClass')) ||
        jsdoc.hasTag(docs, 'polymerMixinClass');
}
exports.hasMixinClassDocTag = hasMixinClassDocTag;
//# sourceMappingURL=polymer2-mixin-scanner.js.map