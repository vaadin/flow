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
const doctrine = require("doctrine");
const source_range_1 = require("../model/source-range");
const ast_value_1 = require("./ast-value");
const esutil_1 = require("./esutil");
const function_1 = require("./function");
const jsdoc = require("./jsdoc");
class FunctionScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const visitor = new FunctionVisitor(document);
            yield visit(visitor);
            return {
                features: [...visitor.functions].sort((a, b) => source_range_1.comparePosition(a.sourceRange.start, b.sourceRange.start)),
            };
        });
    }
}
exports.FunctionScanner = FunctionScanner;
class FunctionVisitor {
    constructor(document) {
        this.functions = new Set();
        this.warnings = [];
        this.document = document;
    }
    /**
     * Scan standalone function declarations.
     */
    enterFunctionDeclaration(node, _parent, path) {
        this.initFunction(node, path, ast_value_1.getIdentifierName(node.id));
    }
    /**
     * Scan object method declarations.
     */
    enterObjectMethod(node, _parent, path) {
        this.initFunction(node, path, ast_value_1.getIdentifierName(node.key));
    }
    /**
     * Scan functions assigned to newly declared variables.
     */
    enterVariableDeclaration(node, _parent, path) {
        if (node.declarations.length !== 1) {
            return; // Ambiguous.
        }
        const declaration = node.declarations[0];
        const declarationValue = declaration.init;
        if (declarationValue && babel.isFunction(declarationValue)) {
            this.initFunction(declarationValue, path, ast_value_1.getIdentifierName(declaration.id));
        }
    }
    /**
     * Scan functions assigned to variables and object properties.
     */
    enterAssignmentExpression(node, _parent, path) {
        if (babel.isFunction(node.right)) {
            this.initFunction(node.right, path, ast_value_1.getIdentifierName(node.left));
        }
    }
    /**
     * Scan functions defined inside of object literals.
     */
    enterObjectExpression(_node, _parent, path) {
        for (const propPath of esutil_1.getSimpleObjectPropPaths(path)) {
            const prop = propPath.node;
            const propValue = prop.value;
            const name = esutil_1.getPropertyName(prop);
            if (babel.isFunction(propValue)) {
                this.initFunction(propValue, propPath, name);
                continue;
            }
            const comment = esutil_1.getBestComment(propPath) || '';
            const docs = jsdoc.parseJsdoc(comment);
            if (jsdoc.getTag(docs, 'function')) {
                this.initFunction(prop, propPath, name);
                continue;
            }
        }
    }
    initFunction(node, path, analyzedName) {
        const docs = jsdoc.parseJsdoc(esutil_1.getBestComment(path) || '');
        // The @function annotation can override the name.
        const functionTag = jsdoc.getTag(docs, 'function');
        if (functionTag && functionTag.name) {
            analyzedName = functionTag.name;
        }
        if (!analyzedName) {
            // TODO(fks): Propagate a warning if name could not be determined
            return;
        }
        if (!jsdoc.hasTag(docs, 'global') && !jsdoc.hasTag(docs, 'memberof') &&
            !this.isExported(path)) {
            // Without this check we would emit a lot of functions not worthy of
            // inclusion. Since we don't do scope analysis, we can't tell when a
            // function is actually part of an exposed API. Only include functions
            // that are explicitly @global, or declared as part of some namespace
            // with @memberof.
            return;
        }
        // TODO(justinfagnani): remove polymerMixin support
        if (jsdoc.hasTag(docs, 'mixinFunction') ||
            jsdoc.hasTag(docs, 'polymerMixin')) {
            // This is a mixin, not a normal function.
            return;
        }
        const functionName = ast_value_1.getNamespacedIdentifier(analyzedName, docs);
        const sourceRange = this.document.sourceRangeForNode(node);
        const summaryTag = jsdoc.getTag(docs, 'summary');
        const summary = (summaryTag && summaryTag.description) || '';
        const description = docs.description;
        let functionReturn = esutil_1.getReturnFromAnnotation(docs);
        if (functionReturn === undefined && babel.isFunction(node)) {
            functionReturn = esutil_1.inferReturnFromBody(node);
        }
        // TODO(justinfagnani): consolidate with similar param processing code in
        // docs.ts
        const functionParams = [];
        const templateTypes = [];
        for (const tag of docs.tags) {
            if (tag.title === 'param') {
                functionParams.push({
                    type: tag.type ? doctrine.type.stringify(tag.type) : 'N/A',
                    desc: tag.description || '',
                    name: tag.name || 'N/A'
                });
            }
            else if (tag.title === 'template') {
                for (let t of (tag.description || '').split(',')) {
                    t = t.trim();
                    if (t.length > 0) {
                        templateTypes.push(t);
                    }
                }
            }
        }
        // TODO(fks): parse params directly from `fn`, merge with docs.tags data
        const specificName = functionName.slice(functionName.lastIndexOf('.') + 1);
        this.functions.add(new function_1.ScannedFunction(functionName, description, summary, esutil_1.getOrInferPrivacy(specificName, docs), { language: 'js', node, containingDocument: this.document }, docs, sourceRange, functionParams, functionReturn, templateTypes));
    }
    isExported(path) {
        const node = path.node;
        if (babel.isObjectExpression(node)) {
            // This function recurses up the AST until it finds an exported statement.
            // That's a little crude, since being within an exported statement doesn't
            // necessarily mean the function itself is exported. There are lots of
            // cases where this fails, but a method defined on an object is a common
            // one.
            return false;
        }
        if (babel.isStatement(node)) {
            const parent = path.parent;
            if (parent && babel.isExportDefaultDeclaration(parent) ||
                babel.isExportNamedDeclaration(parent)) {
                return true;
            }
            return false;
        }
        const parentPath = path.parentPath;
        if (parentPath == null) {
            return false;
        }
        return this.isExported(parentPath);
    }
}
//# sourceMappingURL=function-scanner.js.map