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
const doctrine = require("doctrine");
const astValue = require("../javascript/ast-value");
const esutil = require("../javascript/esutil");
const jsdoc = require("../javascript/jsdoc");
const model_1 = require("../model/model");
const expression_scanner_1 = require("./expression-scanner");
const js_utils_1 = require("./js-utils");
/**
 * Given a properties block (i.e. object literal), parses and extracts the
 * properties declared therein.
 *
 * @param node The value of the `properties` key in a Polymer 1 declaration, or
 *     the return value of the `properties` static getter in a Polymer 2 class.
 * @param document The containing JS document.
 */
function analyzeProperties(node, document) {
    const analyzedProps = [];
    if (!babel.isObjectExpression(node)) {
        return analyzedProps;
    }
    for (const property of esutil.getSimpleObjectProperties(node)) {
        const prop = js_utils_1.toScannedPolymerProperty(property, document.sourceRangeForNode(property), document);
        if (prop === undefined) {
            continue;
        }
        // toScannedPolymerProperty does the wrong thing for us with type. We want
        // type to be undefined unless there's a positive signal for the type.
        // toScannedPolymerProperty will give Object because it infers based on the
        // property declaration.
        prop.type = undefined;
        const typeTag = jsdoc.getTag(prop.jsdoc, 'type');
        if (typeTag) {
            prop.type =
                typeTag.type ? doctrine.type.stringify(typeTag.type) : undefined;
        }
        prop.published = true;
        let isComputed = false;
        const value = property.value;
        if (babel.isIdentifier(value)) {
            // Polymer supports this simple syntax, where only the attribute
            // deserializer is specified.
            prop.attributeType = value.name;
        }
        else if (!babel.isObjectExpression(value)) {
            continue;
        }
        else {
            /**
             * Parse the expression inside a property object block. e.g.
             * property: {
             *   key: {
             *     type: String,
             *     notify: true,
             *     value: -1,
             *     readOnly: true,
             *     reflectToAttribute: true
             *   }
             * }
             */
            for (const propertyArg of esutil.getSimpleObjectProperties(value)) {
                const propertyKey = esutil.getPropertyName(propertyArg);
                switch (propertyKey) {
                    case 'type':
                        prop.attributeType = astValue.getIdentifierName(propertyArg.value);
                        if (prop.attributeType === undefined && prop.type === undefined) {
                            prop.warnings.push(new model_1.Warning({
                                code: 'invalid-property-type',
                                message: 'Invalid type in property object.',
                                severity: model_1.Severity.WARNING,
                                sourceRange: document.sourceRangeForNode(propertyArg),
                                parsedDocument: document
                            }));
                        }
                        break;
                    case 'notify':
                        prop.notify = !!astValue.expressionToValue(propertyArg.value);
                        break;
                    case 'observer':
                        const val = astValue.expressionToValue(propertyArg.value);
                        prop.observerNode = propertyArg.value;
                        const parseResult = expression_scanner_1.parseExpressionInJsStringLiteral(document, propertyArg.value, 'identifierOnly');
                        prop.warnings.push(...parseResult.warnings);
                        prop.observerExpression = parseResult.databinding;
                        if (val === undefined) {
                            prop.observer = astValue.CANT_CONVERT;
                        }
                        else {
                            prop.observer = JSON.stringify(val);
                        }
                        break;
                    case 'readOnly':
                        prop.readOnly = !!astValue.expressionToValue(propertyArg.value);
                        break;
                    case 'reflectToAttribute':
                        prop.reflectToAttribute =
                            !!astValue.expressionToValue(propertyArg.value);
                        break;
                    case 'computed':
                        isComputed = true;
                        const computedParseResult = expression_scanner_1.parseExpressionInJsStringLiteral(document, propertyArg.value, 'callExpression');
                        prop.warnings.push(...computedParseResult.warnings);
                        prop.computedExpression = computedParseResult.databinding;
                        break;
                    case 'value':
                        prop.default =
                            JSON.stringify(astValue.expressionToValue(propertyArg.value));
                        break;
                    default:
                        break;
                }
            }
        }
        if (isComputed) {
            prop.readOnly = true;
        }
        // If we've already got a type, then it was found in the jsdocs, and is
        // canonical. Otherwise we can infer it now from the attribute
        // deserializer.
        if (prop.type === undefined && prop.attributeType !== undefined) {
            prop.type = prop.attributeType;
            // Map primitive constructors to their primitive value types (e.g. String
            // => string).
            prop.type = esutil.CLOSURE_CONSTRUCTOR_MAP.get(prop.type) || prop.type;
            // Assume this type is possibly null or undefined. Users should add a
            // @type annotation if they want to tighten this type.
            prop.type += ' | null | undefined';
        }
        if (prop.type === undefined) {
            prop.warnings.push(new model_1.Warning({
                code: 'no-type-for-property',
                message: 'Unable to determine type for property.',
                severity: model_1.Severity.WARNING,
                sourceRange: document.sourceRangeForNode(property),
                parsedDocument: document
            }));
        }
        analyzedProps.push(prop);
    }
    return analyzedProps;
}
exports.analyzeProperties = analyzeProperties;
//# sourceMappingURL=analyze-properties.js.map