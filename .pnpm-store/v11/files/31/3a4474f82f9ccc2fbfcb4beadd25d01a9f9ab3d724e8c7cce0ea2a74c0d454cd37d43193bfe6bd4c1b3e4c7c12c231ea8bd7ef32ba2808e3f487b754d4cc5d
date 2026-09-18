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
const babel = require("@babel/types");
const assert = require("assert");
const ast_value_1 = require("../javascript/ast-value");
const esutil = require("../javascript/esutil");
const jsdoc = require("../javascript/jsdoc");
const model_1 = require("../model/model");
const behavior_1 = require("./behavior");
const declaration_property_handlers_1 = require("./declaration-property-handlers");
const docs = require("./docs");
const templatizer = 'Polymer.Templatizer';
class BehaviorScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const visitor = new BehaviorVisitor(document);
            yield visit(visitor);
            return {
                features: Array.from(visitor.behaviors),
                warnings: visitor.warnings
            };
        });
    }
}
exports.BehaviorScanner = BehaviorScanner;
class BehaviorVisitor {
    constructor(document) {
        /** The behaviors we've found. */
        this.behaviors = new Set();
        this.warnings = [];
        this.currentBehavior = null;
        this.propertyHandlers = null;
        this.document = document;
    }
    /**
     * Look for object declarations with @polymerBehavior in the docs.
     */
    enterVariableDeclarator(node, _parent, path) {
        this._initBehavior(node, ast_value_1.getIdentifierName(node.id), path);
    }
    enterExportDefaultDeclaration(node, _parent, path) {
        this._initBehavior(node, 'default', path);
    }
    /**
     * Look for object assignments with @polymerBehavior in the docs.
     */
    enterAssignmentExpression(node, parent, path) {
        this._initBehavior(parent, ast_value_1.getIdentifierName(node.left), path);
    }
    /**
     * We assume that the object expression after such an assignment is the
     * behavior's declaration. Seems to be a decent assumption for now.
     */
    enterObjectExpression(node, _parent) {
        if (!this.currentBehavior || !this.propertyHandlers) {
            return;
        }
        for (const prop of esutil.getSimpleObjectProperties(node)) {
            const name = esutil.getPropertyName(prop);
            if (!name) {
                this.currentBehavior.warnings.push(new model_1.Warning({
                    code: 'cant-determine-name',
                    message: `Unable to determine property name from expression of type ` +
                        `${node.type}`,
                    severity: model_1.Severity.WARNING,
                    sourceRange: this.document.sourceRangeForNode(node),
                    parsedDocument: this.document
                }));
                continue;
            }
            if (name in this.propertyHandlers) {
                this.propertyHandlers[name](prop.value);
            }
            else if ((babel.isMethod(prop) && prop.kind === 'method') ||
                babel.isFunction(prop.value)) {
                const method = esutil.toScannedMethod(prop, this.document.sourceRangeForNode(prop), this.document);
                this.currentBehavior.addMethod(method);
            }
        }
        for (const prop of esutil
            .extractPropertiesFromClassOrObjectBody(node, this.document)
            .values()) {
            if (prop.name in this.propertyHandlers) {
                continue;
            }
            this.currentBehavior.addProperty(Object.assign({}, prop, { isConfiguration: esutil.configurationProperties.has(prop.name) }));
        }
        this._finishBehavior();
    }
    _startBehavior(behavior) {
        assert(this.currentBehavior == null);
        this.currentBehavior = behavior;
    }
    _finishBehavior() {
        assert(this.currentBehavior != null);
        this.behaviors.add(this.currentBehavior);
        this.currentBehavior = null;
    }
    _initBehavior(node, name, path) {
        if (name === undefined) {
            return;
        }
        const comment = esutil.getBestComment(path);
        // Quickly filter down to potential candidates.
        if (!comment || comment.indexOf('@polymerBehavior') === -1) {
            if (name !== templatizer) {
                return;
            }
        }
        const parsedJsdocs = jsdoc.parseJsdoc(comment || '');
        if (!jsdoc.hasTag(parsedJsdocs, 'polymerBehavior')) {
            if (name !== templatizer) {
                return;
            }
        }
        this._startBehavior(new behavior_1.ScannedBehavior({
            astNode: { language: 'js', node, containingDocument: this.document },
            statementAst: esutil.getCanonicalStatement(path),
            description: parsedJsdocs.description,
            events: esutil.getEventComments(node),
            sourceRange: this.document.sourceRangeForNode(node),
            privacy: esutil.getOrInferPrivacy(name, parsedJsdocs),
            abstract: jsdoc.hasTag(parsedJsdocs, 'abstract'),
            attributes: new Map(),
            properties: [],
            behaviors: [],
            className: undefined,
            extends: undefined,
            jsdoc: parsedJsdocs,
            listeners: [],
            methods: new Map(),
            staticMethods: new Map(),
            mixins: [],
            observers: [],
            superClass: undefined,
            tagName: undefined,
            isLegacyFactoryCall: false,
        }));
        const behavior = this.currentBehavior;
        this.propertyHandlers =
            declaration_property_handlers_1.declarationPropertyHandlers(behavior, this.document, path);
        docs.annotateElementHeader(behavior);
        const behaviorTag = jsdoc.getTag(behavior.jsdoc, 'polymerBehavior');
        behavior.className = behaviorTag && behaviorTag.name ||
            ast_value_1.getNamespacedIdentifier(name, behavior.jsdoc);
        if (!behavior.className) {
            throw new Error(`Unable to determine name for @polymerBehavior: ${comment}`);
        }
        behavior.privacy =
            esutil.getOrInferPrivacy(behavior.className, behavior.jsdoc);
        this._parseChainedBehaviors(node, path);
        this.currentBehavior = this.mergeBehavior(behavior);
        this.propertyHandlers =
            declaration_property_handlers_1.declarationPropertyHandlers(this.currentBehavior, this.document, path);
        // Some behaviors are just lists of other behaviors. If this is one then
        // add it to behaviors right away.
        if (isSimpleBehaviorArray(behaviorExpression(node))) {
            this._finishBehavior();
        }
    }
    /**
     * merges behavior with preexisting behavior with the same name.
     * here to support multiple @polymerBehavior tags referring
     * to same behavior. See iron-multi-selectable for example.
     */
    mergeBehavior(newBehavior) {
        const isBehaviorImpl = (b) => {
            // filter out BehaviorImpl
            return newBehavior.className === undefined ||
                b.identifier.indexOf(newBehavior.className) === -1;
        };
        for (const behavior of this.behaviors) {
            if (newBehavior.className !== behavior.className) {
                continue;
            }
            // TODO(justinfagnani): what?
            // merge desc, longest desc wins
            if (newBehavior.description) {
                if (behavior.description) {
                    if (newBehavior.description.length > behavior.description.length) {
                        behavior.description = newBehavior.description;
                    }
                }
                else {
                    behavior.description = newBehavior.description;
                }
            }
            // TODO(justinfagnani): move into ScannedBehavior
            behavior.demos = behavior.demos.concat(newBehavior.demos);
            for (const [key, val] of newBehavior.events) {
                behavior.events.set(key, val);
            }
            for (const property of newBehavior.properties.values()) {
                behavior.addProperty(property);
            }
            behavior.observers = behavior.observers.concat(newBehavior.observers);
            behavior.behaviorAssignments =
                (behavior.behaviorAssignments)
                    .concat(newBehavior.behaviorAssignments)
                    .filter(isBehaviorImpl);
            return behavior;
        }
        return newBehavior;
    }
    _parseChainedBehaviors(node, path) {
        if (this.currentBehavior == null) {
            throw new Error(`_parsedChainedBehaviors was called without a current behavior.`);
        }
        // If current behavior is part of an array, it gets extended by other
        // behaviors inside the array. Ex:
        // Polymer.IronMultiSelectableBehavior = [
        //     {....},
        //     Polymer.IronSelectableBehavior];
        // We add these to the behaviors array.
        const expression = behaviorExpression(node);
        const chained = [];
        if (expression && babel.isArrayExpression(expression)) {
            for (const arrElement of expression.elements) {
                const behaviorName = ast_value_1.getIdentifierName(arrElement);
                if (behaviorName) {
                    chained.push(new model_1.ScannedReference('behavior', behaviorName, this.document.sourceRangeForNode(arrElement), {
                        language: 'js',
                        node: arrElement,
                        containingDocument: this.document
                    }, path));
                }
            }
            if (chained.length > 0) {
                this.currentBehavior.behaviorAssignments = chained;
            }
        }
    }
}
/**
 * gets the expression representing a behavior from a node.
 */
function behaviorExpression(node) {
    if (babel.isVariableDeclarator(node)) {
        return node.init;
    }
    if (babel.isAssignmentExpression(node)) {
        return node.right;
    }
    if (babel.isExportDefaultDeclaration(node) ||
        babel.isExportNamedDeclaration(node)) {
        return behaviorExpression(node.declaration);
    }
    if (babel.isExpressionStatement(node)) {
        return behaviorExpression(node.expression);
    }
    if (babel.isVariableDeclaration(node)) {
        return behaviorExpression(node.declarations[0]);
    }
    if (babel.isObjectExpression(node) || babel.isArrayExpression(node)) {
        return node;
    }
}
/**
 * checks whether an expression is a simple array containing only member
 * expressions or identifiers.
 */
function isSimpleBehaviorArray(expression) {
    if (!expression || !babel.isArrayExpression(expression)) {
        return false;
    }
    for (const element of expression.elements) {
        if (!babel.isMemberExpression(element) && !babel.isIdentifier(element)) {
            return false;
        }
    }
    return true;
}
//# sourceMappingURL=behavior-scanner.js.map