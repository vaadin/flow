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
const ast_value_1 = require("../javascript/ast-value");
const esutil = require("../javascript/esutil");
const jsdoc = require("../javascript/jsdoc");
const model_1 = require("../model/model");
const declaration_property_handlers_1 = require("./declaration-property-handlers");
const polymer_element_1 = require("./polymer-element");
class PolymerElementScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const visitor = new ElementVisitor(document);
            yield visit(visitor);
            return { features: visitor.features, warnings: visitor.warnings };
        });
    }
}
exports.PolymerElementScanner = PolymerElementScanner;
/**
 * Handles Polymer({}) calls.
 */
class ElementVisitor {
    constructor(document) {
        this.features = [];
        this.warnings = [];
        this.document = document;
    }
    enterCallExpression(node, parent, path) {
        const callee = node.callee;
        if (!babel.isIdentifier(callee) || callee.name !== 'Polymer') {
            return;
        }
        const rawDescription = esutil.getAttachedComment(parent);
        let className = undefined;
        if (babel.isAssignmentExpression(parent)) {
            className = ast_value_1.getIdentifierName(parent.left);
        }
        else if (babel.isVariableDeclarator(parent)) {
            className = ast_value_1.getIdentifierName(parent.id);
        }
        const jsDoc = jsdoc.parseJsdoc(rawDescription || '');
        const element = new polymer_element_1.ScannedPolymerElement({
            className,
            astNode: { node, language: 'js', containingDocument: this.document },
            statementAst: esutil.getCanonicalStatement(path),
            description: jsDoc.description,
            events: esutil.getEventComments(parent),
            sourceRange: this.document.sourceRangeForNode(node.arguments[0]),
            privacy: esutil.getOrInferPrivacy('', jsDoc),
            abstract: jsdoc.hasTag(jsDoc, 'abstract'),
            attributes: new Map(),
            properties: [],
            behaviors: [],
            extends: undefined,
            jsdoc: jsDoc,
            listeners: [],
            methods: new Map(),
            staticMethods: new Map(),
            mixins: [],
            observers: [],
            superClass: undefined,
            tagName: undefined,
            isLegacyFactoryCall: true,
        });
        element.description = (element.description || '').trim();
        const propertyHandlers = declaration_property_handlers_1.declarationPropertyHandlers(element, this.document, path);
        const argument = node.arguments[0];
        if (babel.isObjectExpression(argument)) {
            this.handleObjectExpression(argument, propertyHandlers, element);
        }
        this.features.push(element);
    }
    handleObjectExpression(node, propertyHandlers, element) {
        for (const prop of esutil.getSimpleObjectProperties(node)) {
            const name = esutil.getPropertyName(prop);
            if (!name) {
                element.warnings.push(new model_1.Warning({
                    message: `Can't determine name for property key from expression ` +
                        `with type ${prop.key.type}.`,
                    code: 'cant-determine-property-name',
                    severity: model_1.Severity.WARNING,
                    sourceRange: this.document.sourceRangeForNode(prop.key),
                    parsedDocument: this.document
                }));
                continue;
            }
            if (name in propertyHandlers) {
                propertyHandlers[name](prop.value);
            }
            else if ((babel.isMethod(prop) && prop.kind === 'method') ||
                babel.isFunction(prop.value)) {
                const method = esutil.toScannedMethod(prop, this.document.sourceRangeForNode(prop), this.document);
                element.addMethod(method);
            }
        }
        for (const prop of esutil
            .extractPropertiesFromClassOrObjectBody(node, this.document)
            .values()) {
            if (prop.name in propertyHandlers) {
                continue;
            }
            element.addProperty(Object.assign({}, prop, { isConfiguration: esutil.configurationProperties.has(prop.name) }));
        }
    }
}
//# sourceMappingURL=polymer-element-scanner.js.map