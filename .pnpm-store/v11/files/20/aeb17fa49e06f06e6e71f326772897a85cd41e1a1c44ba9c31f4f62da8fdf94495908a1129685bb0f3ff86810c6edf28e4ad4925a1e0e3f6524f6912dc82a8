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
const esutil_1 = require("./esutil");
const jsdoc = require("./jsdoc");
/**
 * Converts an ast literal to its underlying value.
 */
function literalToValue(literal) {
    if (babel.isBooleanLiteral(literal) || babel.isNumericLiteral(literal) ||
        babel.isStringLiteral(literal)) {
        return literal.value;
    }
    if (babel.isNullLiteral(literal)) {
        return null;
    }
    if (babel.isTemplateLiteral(literal)) {
        return templateLiteralToValue(literal);
    }
    // Any other literal value is treated as undefined.
    return undefined;
}
function templateLiteralToValue(literal) {
    const len = literal.quasis.length - 1;
    let value = '';
    for (let i = 0; i < len; i++) {
        value += literal.quasis[i].value.raw;
        const v = expressionToValue(literal.expressions[i]);
        if (v === undefined) {
            return;
        }
        value += `${v}`;
    }
    value += literal.quasis[len].value.raw;
    return value;
}
/**
 * Early evaluates a unary expression.
 */
function unaryToValue(unary) {
    const operand = expressionToValue(unary.argument);
    switch (unary.operator) {
        case '!':
            // tslint:disable-next-line: no-any Aping JS semantics, this is ok.
            return !operand;
        case '-':
            // tslint:disable-next-line: no-any Aping JS semantics, this is ok.
            return -operand;
        case '+':
            // tslint:disable-next-line: no-any Aping JS semantics, this is ok.
            return +operand;
        case '~':
            // tslint:disable-next-line: no-any Aping JS semantics, this is ok.
            return ~operand;
        case 'typeof':
            return typeof operand;
        case 'void':
            return void operand;
        case 'delete':
            return undefined;
        default:
            const never = unary.operator;
            throw new Error(`Unknown unary operator found: ${never}`);
    }
}
/**
 * Try to evaluate function bodies.
 */
function functionDeclarationToValue(fn) {
    if (babel.isBlockStatement(fn.body)) {
        return blockStatementToValue(fn.body);
    }
}
function functionExpressionToValue(fn) {
    if (babel.isBlockStatement(fn.body)) {
        return blockStatementToValue(fn.body);
    }
}
function arrowFunctionExpressionToValue(fn) {
    if (babel.isBlockStatement(fn.body)) {
        return blockStatementToValue(fn.body);
    }
    else {
        return expressionToValue(fn.body);
    }
}
/**
 * Block statement: find last return statement, and return its value
 */
function blockStatementToValue(block) {
    for (let i = block.body.length - 1; i >= 0; i--) {
        const body = block.body[i];
        if (babel.isReturnStatement(body)) {
            return returnStatementToValue(body);
        }
    }
}
/**
 * Evaluates return's argument
 */
function returnStatementToValue(ret) {
    return expressionToValue(ret.argument || { type: 'Literal', value: null, raw: 'null' });
}
/**
 * Evaluate array expression
 */
function arrayExpressionToValue(arry) {
    const value = [];
    for (let i = 0; i < arry.elements.length; i++) {
        const v = expressionToValue(arry.elements[i]);
        if (v === undefined) {
            continue;
        }
        value.push(v);
    }
    return value;
}
/**
 * Evaluate object expression
 */
function objectExpressionToValue(obj) {
    const evaluatedObjectExpression = {};
    for (const prop of esutil_1.getSimpleObjectProperties(obj)) {
        const evaluatedKey = esutil_1.getPropertyName(prop);
        if (evaluatedKey === undefined) {
            return;
        }
        const evaluatedValue = expressionToValue(prop.value);
        if (evaluatedValue === undefined) {
            return;
        }
        evaluatedObjectExpression[evaluatedKey] = evaluatedValue;
    }
    return evaluatedObjectExpression;
}
/**
 * Binary expressions, like 5 + 5
 */
function binaryExpressionToValue(member) {
    const left = expressionToValue(member.left);
    const right = expressionToValue(member.right);
    if (left == null || right == null) {
        return;
    }
    if (member.operator === '+') {
        // We need to cast to `any` here because, while it's usually not the right
        // thing to do to use '+' on two values of a mix of types because it's
        // unpredictable, that is what the original code we're evaluating does.
        // tslint:disable-next-line: no-any Aping JS semantics, this is ok.
        return left + right;
    }
    return;
}
/**
 * Tries to get the value of an expression. Returns undefined on failure.
 */
function expressionToValue(valueExpression) {
    if (babel.isLiteral(valueExpression)) {
        return literalToValue(valueExpression);
    }
    if (babel.isUnaryExpression(valueExpression)) {
        return unaryToValue(valueExpression);
    }
    if (babel.isFunctionDeclaration(valueExpression)) {
        return functionDeclarationToValue(valueExpression);
    }
    if (babel.isFunctionExpression(valueExpression)) {
        return functionExpressionToValue(valueExpression);
    }
    if (babel.isArrowFunctionExpression(valueExpression)) {
        return arrowFunctionExpressionToValue(valueExpression);
    }
    if (babel.isArrayExpression(valueExpression)) {
        return arrayExpressionToValue(valueExpression);
    }
    if (babel.isObjectExpression(valueExpression)) {
        return objectExpressionToValue(valueExpression);
    }
    if (babel.isBinaryExpression(valueExpression)) {
        return binaryExpressionToValue(valueExpression);
    }
}
exports.expressionToValue = expressionToValue;
/**
 * Extracts the name of the identifier or `.` separated chain of identifiers.
 *
 * Returns undefined if the given node isn't a simple identifier or chain of
 * simple identifiers.
 */
function getIdentifierName(node) {
    if (babel.isIdentifier(node)) {
        return node.name;
    }
    if (babel.isMemberExpression(node)) {
        const object = getIdentifierName(node.object);
        let property;
        if (node.computed) {
            property = expressionToValue(node.property);
        }
        else {
            property = getIdentifierName(node.property);
        }
        if (object != null && property != null) {
            return `${object}.${property}`;
        }
    }
}
exports.getIdentifierName = getIdentifierName;
/**
 * Formats the given identifier name under a namespace, if one is mentioned in
 * the commentedNode's comment. Otherwise, name is returned.
 */
function getNamespacedIdentifier(name, docs) {
    if (!docs) {
        return name;
    }
    const memberofTag = jsdoc.getTag(docs, 'memberof');
    const namespace = memberofTag && memberofTag.description;
    if (namespace) {
        const rightMostIdentifierName = name.substring(name.lastIndexOf('.') + 1);
        return namespace + '.' + rightMostIdentifierName;
    }
    else {
        return name;
    }
}
exports.getNamespacedIdentifier = getNamespacedIdentifier;
exports.CANT_CONVERT = 'UNKNOWN';
//# sourceMappingURL=ast-value.js.map