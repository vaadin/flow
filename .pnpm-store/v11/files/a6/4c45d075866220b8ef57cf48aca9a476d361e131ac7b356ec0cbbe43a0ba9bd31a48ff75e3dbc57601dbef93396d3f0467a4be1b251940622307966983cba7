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
const generator_1 = require("@babel/generator");
const traverse_1 = require("@babel/traverse");
const babel = require("@babel/types");
const assert = require("assert");
const doctrine = require("doctrine");
const util = require("util");
const model_1 = require("../model/model");
const docs = require("../polymer/docs");
const docs_1 = require("../polymer/docs");
const astValue = require("./ast-value");
const jsdoc = require("./jsdoc");
/**
 * Returns whether a Babel node matches a particular object path.
 *
 * e.g. you have a MemberExpression node, and want to see whether it represents
 * `Foo.Bar.Baz`:
 *    matchesCallExpressio
    (node, ['Foo', 'Bar', 'Baz'])
 *
 * @param {babel.Node} expression The Babel node to match against.
 * @param {Array<string>} path The path to look for.
 */
function matchesCallExpression(expression, path) {
    if (!expression.property || !expression.object) {
        return false;
    }
    assert(path.length >= 2);
    if (!babel.isIdentifier(expression.property)) {
        return false;
    }
    // Unravel backwards, make sure properties match each step of the way.
    if (expression.property.name !== path[path.length - 1]) {
        return false;
    }
    // We've got ourselves a final member expression.
    if (path.length === 2 && babel.isIdentifier(expression.object)) {
        return expression.object.name === path[0];
    }
    // Nested expressions.
    if (path.length > 2 && babel.isMemberExpression(expression.object)) {
        return matchesCallExpression(expression.object, path.slice(0, path.length - 1));
    }
    return false;
}
exports.matchesCallExpression = matchesCallExpression;
/**
 * Given a property or method, return its name, or undefined if that name can't
 * be determined.
 */
function getPropertyName(prop) {
    const key = prop.key;
    // {foo: bar} // note that `foo` is not quoted, so it's an identifier
    if (!prop.computed && babel.isIdentifier(key)) {
        return key.name;
    }
    // Otherwise, try to statically evaluate the expression
    const keyValue = astValue.expressionToValue(key);
    if (keyValue !== undefined) {
        return '' + keyValue;
    }
    return undefined;
}
exports.getPropertyName = getPropertyName;
/**
 * Yields properties and methods, filters out spread expressions or anything
 * else.
 */
function* getSimpleObjectProperties(node) {
    for (const property of node.properties) {
        if (babel.isObjectProperty(property) || babel.isObjectMethod(property)) {
            yield property;
        }
    }
}
exports.getSimpleObjectProperties = getSimpleObjectProperties;
/** Like getSimpleObjectProperties but deals with paths. */
function* getSimpleObjectPropPaths(nodePath) {
    // tslint:disable-next-line: no-any typings are wrong here
    const props = nodePath.get('properties');
    for (const propPath of props) {
        if (propPath.isObjectProperty() || propPath.isObjectMethod()) {
            yield propPath;
        }
    }
}
exports.getSimpleObjectPropPaths = getSimpleObjectPropPaths;
exports.CLOSURE_CONSTRUCTOR_MAP = new Map([['Boolean', 'boolean'], ['Number', 'number'], ['String', 'string']]);
const VALID_EXPRESSION_TYPES = new Map([
    ['ArrayExpression', 'Array'],
    ['BlockStatement', 'Function'],
    ['BooleanLiteral', 'boolean'],
    ['FunctionExpression', 'Function'],
    ['NullLiteral', 'null'],
    ['NumericLiteral', 'number'],
    ['ObjectExpression', 'Object'],
    ['RegExpLiteral', 'RegExp'],
    ['StringLiteral', 'string'],
    ['TemplateLiteral', 'string'],
]);
/**
 * AST expression -> Closure type.
 *
 * Accepts literal values, and native constructors.
 *
 * @param {Node} node A Babel expression node.
 * @return {string} The type of that expression, in Closure terms.
 */
function getClosureType(node, parsedJsdoc, sourceRange, document) {
    if (parsedJsdoc) {
        const typeTag = jsdoc.getTag(parsedJsdoc, 'type');
        if (typeTag) {
            return { successful: true, value: doctrine.type.stringify(typeTag.type) };
        }
    }
    const type = VALID_EXPRESSION_TYPES.get(node.type);
    if (type) {
        return { successful: true, value: type };
    }
    if (babel.isIdentifier(node)) {
        return {
            successful: true,
            value: exports.CLOSURE_CONSTRUCTOR_MAP.get(node.name) || node.name
        };
    }
    const warning = new model_1.Warning({
        code: 'no-closure-type',
        message: `Unable to determine closure type for expression of type ` +
            `${node.type}`,
        severity: model_1.Severity.WARNING,
        sourceRange,
        parsedDocument: document,
    });
    return { successful: false, error: warning };
}
exports.getClosureType = getClosureType;
/**
 * Tries to find the comment for the given node.
 *
 * Will look up the tree at comments on parents as appropriate, but should
 * not look at unrelated nodes. Stops at the nearest statement boundary.
 */
function getBestComment(nodePath) {
    const maybeComment = getAttachedComment(nodePath.node);
    if (maybeComment !== undefined) {
        return maybeComment;
    }
    const parent = nodePath.parentPath;
    if (parent === undefined) {
        return undefined;
    }
    if (!isStatementWithUniqueStatementChild(parent.node) &&
        babel.isStatement(nodePath.node)) {
        // Don't walk up above the nearest statement.
        return undefined;
    }
    if (babel.isVariableDeclaration(parent.node) &&
        parent.node.declarations.length !== 1) {
        // The parent node is multiple declarations. We can't be sure its
        // comment applies to us.
        return undefined;
    }
    if (parent.isClassBody() || nodePath.isObjectMember()) {
        // don't go above an object or class member.
        return undefined;
    }
    return getBestComment(parent);
}
exports.getBestComment = getBestComment;
function getAttachedComment(node) {
    const comments = getLeadingComments(node) || [];
    return comments && comments[comments.length - 1];
}
exports.getAttachedComment = getAttachedComment;
/**
 * Returns all comments from a tree defined with @event.
 */
function getEventComments(node) {
    const eventComments = new Set();
    traverse_1.default(node, {
        enter(path) {
            const node = path.node;
            [...(node.leadingComments || []), ...(node.trailingComments || [])]
                .map((commentAST) => commentAST.value)
                .filter((comment) => comment.indexOf('@event') !== -1)
                .forEach((comment) => eventComments.add(comment));
        },
        noScope: true,
    });
    const events = [...eventComments]
        .map((comment) => docs_1.annotateEvent(jsdoc.parseJsdoc(jsdoc.removeLeadingAsterisks(comment).trim())))
        .filter((ev) => !!ev)
        .sort((ev1, ev2) => ev1.name.localeCompare(ev2.name));
    return new Map(events.map((e) => [e.name, e]));
}
exports.getEventComments = getEventComments;
function getLeadingComments(node) {
    if (!node) {
        return;
    }
    const comments = [];
    for (const comment of node.leadingComments || []) {
        // Espree says any comment that immediately precedes a node is
        // "leading", but we want to be stricter and require them to be
        // touching. If we don't have locations for some reason, err on the
        // side of including the comment.
        if (!node.loc || !comment.loc ||
            node.loc.start.line - comment.loc.end.line < 2) {
            comments.push(comment.value);
        }
    }
    return comments.length ? comments : undefined;
}
function getPropertyValue(node, name) {
    for (const property of getSimpleObjectProperties(node)) {
        if (getPropertyName(property) === name) {
            return property.value;
        }
    }
}
exports.getPropertyValue = getPropertyValue;
/**
 * Create a ScannedMethod object from an estree Property AST node.
 */
function toScannedMethod(node, sourceRange, document) {
    const parsedJsdoc = jsdoc.parseJsdoc(getAttachedComment(node) || '');
    const description = parsedJsdoc.description.trim();
    const maybeName = getPropertyName(node);
    const warnings = [];
    if (!maybeName) {
        warnings.push(new model_1.Warning({
            code: 'unknown-method-name',
            message: `Could not determine name of method from expression of type: ` +
                `${node.key.type}`,
            sourceRange: sourceRange,
            severity: model_1.Severity.INFO,
            parsedDocument: document
        }));
    }
    const value = babel.isObjectProperty(node) ? node.value : node;
    const result = getClosureType(value, parsedJsdoc, sourceRange, document);
    const type = result.successful === true ? result.value : 'Function';
    const name = maybeName || '';
    const scannedMethod = {
        name,
        type,
        description,
        sourceRange,
        warnings,
        astNode: { language: 'js', node, containingDocument: document },
        jsdoc: parsedJsdoc,
        privacy: getOrInferPrivacy(name, parsedJsdoc)
    };
    if (value && babel.isFunction(value)) {
        if (scannedMethod.jsdoc !== undefined) {
            scannedMethod.return = getReturnFromAnnotation(scannedMethod.jsdoc);
        }
        if (scannedMethod.return === undefined) {
            scannedMethod.return = inferReturnFromBody(value);
        }
        scannedMethod.params =
            (value.params ||
                []).map((nodeParam) => toMethodParam(nodeParam, scannedMethod.jsdoc));
    }
    return scannedMethod;
}
exports.toScannedMethod = toScannedMethod;
function getReturnFromAnnotation(jsdocAnn) {
    const tag = jsdoc.getTag(jsdocAnn, 'return') || jsdoc.getTag(jsdocAnn, 'returns');
    if (!tag || (!tag.type && !tag.description)) {
        return undefined;
    }
    const type = {};
    if (tag && (tag.type || tag.description)) {
        if (tag.type) {
            type.type = doctrine.type.stringify(tag.type);
        }
        if (tag.description) {
            type.desc = tag.description;
        }
    }
    return type;
}
exports.getReturnFromAnnotation = getReturnFromAnnotation;
/**
 * Examine the body of a function to see if we can infer something about its
 * return type. This currently only handles the case where a function definitely
 * returns void.
 */
function inferReturnFromBody(node) {
    if (node.async === true || node.generator === true) {
        // Async functions always return promises, and generators always return
        // iterators, so they are never void.
        return undefined;
    }
    if (babel.isArrowFunctionExpression(node) &&
        !babel.isBlockStatement(node.body)) {
        // An arrow function that immediately returns a value (e.g. () => 'foo').
        return undefined;
    }
    let returnsVoid = true;
    traverse_1.default(node, {
        ReturnStatement(path) {
            const statement = path.node;
            // The typings claim that statement.argument is always an Expression, but
            // actually when there is no argument it is null.
            if (statement.argument !== null) {
                returnsVoid = false;
                path.stop();
            }
        },
        // If this function contains another function, don't traverse into it. Only
        // return statements in the immediate function scope matter.
        FunctionDeclaration(path) {
            path.skip();
        },
        FunctionExpression(path) {
            path.skip();
        },
        ClassMethod(path) {
            path.skip();
        },
        ArrowFunctionExpression(path) {
            path.skip();
        },
        ObjectMethod(path) {
            path.skip();
        },
        noScope: true
    });
    if (returnsVoid) {
        return { type: 'void' };
    }
    return undefined;
}
exports.inferReturnFromBody = inferReturnFromBody;
function toMethodParam(nodeParam, jsdocAnn) {
    const paramTags = new Map();
    let name;
    let defaultValue;
    let rest;
    if (jsdocAnn) {
        for (const tag of (jsdocAnn.tags || [])) {
            if (tag.title === 'param' && tag.name) {
                paramTags.set(tag.name, tag);
            }
        }
    }
    if (babel.isIdentifier(nodeParam)) {
        // Basic parameter: method(param)
        name = nodeParam.name;
    }
    else if (babel.isRestElement(nodeParam) &&
        babel.isIdentifier(nodeParam.argument)) {
        // Rest parameter: method(...param)
        name = nodeParam.argument.name;
        rest = true;
    }
    else if (babel.isAssignmentPattern(nodeParam) &&
        babel.isIdentifier(nodeParam.left)) {
        // Parameter with a default: method(param = "default")
        name = nodeParam.left.name;
        defaultValue = generator_1.default(nodeParam.right).code;
    }
    else {
        // Some AST pattern we don't recognize. Hope the code generator does
        // something reasonable.
        name = generator_1.default(nodeParam).code;
    }
    let type;
    let description;
    const tag = paramTags.get(name);
    if (tag) {
        if (tag.type) {
            type = doctrine.type.stringify(tag.type);
        }
        if (tag.description) {
            description = tag.description;
        }
    }
    const param = { name, type, defaultValue, rest, description };
    return param;
}
exports.toMethodParam = toMethodParam;
function getOrInferPrivacy(name, annotation, defaultPrivacy = 'public') {
    const explicitPrivacy = jsdoc.getPrivacy(annotation);
    const specificName = name.slice(name.lastIndexOf('.') + 1);
    if (explicitPrivacy) {
        return explicitPrivacy;
    }
    if (specificName.startsWith('__')) {
        return 'private';
    }
    else if (specificName.startsWith('_')) {
        return 'protected';
    }
    else if (specificName.endsWith('_')) {
        return 'private';
    }
    else if (exports.configurationProperties.has(specificName)) {
        return 'protected';
    }
    return defaultPrivacy;
}
exports.getOrInferPrivacy = getOrInferPrivacy;
/**
 * Properties on element prototypes that are part of the custom elment
 * lifecycle or Polymer configuration syntax.
 *
 * TODO(rictic): only treat the Polymer ones as private when dealing with
 *   Polymer.
 */
exports.configurationProperties = new Set([
    'attached',
    'attributeChanged',
    'beforeRegister',
    'configure',
    'constructor',
    'created',
    'detached',
    'enableCustomStyleProperties',
    'extends',
    'hostAttributes',
    'is',
    'listeners',
    'mixins',
    'observers',
    'properties',
    'ready',
    'registered',
]);
/**
 * Scan any methods on the given node, if it's a class expression/declaration.
 */
function getMethods(node, document) {
    const methods = new Map();
    for (const statement of _getMethods(node)) {
        if (statement.static === false) {
            const method = toScannedMethod(statement, document.sourceRangeForNode(statement), document);
            docs.annotate(method);
            methods.set(method.name, method);
        }
    }
    return methods;
}
exports.getMethods = getMethods;
function getConstructorMethod(astNode, document) {
    if (!babel.isClass(astNode)) {
        return;
    }
    const statement = getConstructorClassMethod(astNode);
    if (statement) {
        const method = toScannedMethod(statement, document.sourceRangeForNode(statement), document);
        const typeTag = getReturnFromAnnotation(jsdoc.parseJsdoc(getAttachedComment(statement) || ''));
        if (typeTag) {
            method.return = Object.assign({}, method.return, typeTag);
        }
        else {
            method.return = undefined;
        }
        return method;
    }
}
exports.getConstructorMethod = getConstructorMethod;
function getConstructorClassMethod(astNode) {
    for (const member of astNode.body.body) {
        if (babel.isClassMethod(member) && member.kind === 'constructor') {
            return member;
        }
    }
}
exports.getConstructorClassMethod = getConstructorClassMethod;
/**
 * Scan any static methods on the given node, if it's a class
 * expression/declaration.
 */
function getStaticMethods(node, document) {
    const methods = new Map();
    for (const method of _getMethods(node)) {
        if (method.static === true) {
            const scannedMethod = toScannedMethod(method, document.sourceRangeForNode(method), document);
            docs.annotate(scannedMethod);
            methods.set(scannedMethod.name, scannedMethod);
        }
    }
    return methods;
}
exports.getStaticMethods = getStaticMethods;
function* _getMethods(node) {
    if (!babel.isClassDeclaration(node) && !babel.isClassExpression(node)) {
        return;
    }
    for (const statement of node.body.body) {
        if (babel.isClassMethod(statement) && statement.kind === 'method') {
            yield statement;
        }
    }
}
/*
 * Extracts a property from a given getter or setter method,
 * whether it be an object method or a class method.
 */
function extractPropertyFromGetterOrSetter(method, jsdocAnn, document) {
    // TODO(43081j): remove this when static properties are supported
    if (babel.isClassMethod(method) && method.static) {
        return null;
    }
    if (method.kind !== 'get' && method.kind !== 'set') {
        return null;
    }
    // TODO(43081j): use getPropertyName, see
    // https://github.com/Polymer/polymer-analyzer/pull/867
    const name = getPropertyName(method);
    if (name === undefined) {
        return null;
    }
    let type;
    let description;
    let privacy = 'public';
    let readOnly = false;
    if (jsdocAnn) {
        const ret = getReturnFromAnnotation(jsdocAnn);
        type = ret ? ret.type : undefined;
        description = jsdoc.getDescription(jsdocAnn);
        privacy = getOrInferPrivacy(name, jsdocAnn);
        readOnly = jsdoc.hasTag(jsdocAnn, 'readonly');
    }
    return {
        name,
        astNode: { language: 'js', node: method, containingDocument: document },
        type,
        jsdoc: jsdocAnn,
        sourceRange: document.sourceRangeForNode(method),
        description,
        privacy,
        warnings: [],
        readOnly,
    };
}
exports.extractPropertyFromGetterOrSetter = extractPropertyFromGetterOrSetter;
/**
 * Extracts properties (including accessors) from a given class
 * or object expression.
 */
function extractPropertiesFromClassOrObjectBody(node, document) {
    const properties = new Map();
    const accessors = new Map();
    let body;
    if (babel.isClass(node)) {
        body = node.body.body;
    }
    else {
        body = node.properties;
    }
    for (const member of body) {
        if (!babel.isMethod(member) && !babel.isObjectProperty(member)) {
            continue;
        }
        const name = getPropertyName(member);
        if (name === undefined) {
            continue;
        }
        if (babel.isMethod(member) || babel.isFunction(member.value)) {
            if (babel.isMethod(member) &&
                (member.kind === 'get' || member.kind === 'set')) {
                let accessor = accessors.get(name);
                if (!accessor) {
                    accessor = {};
                    accessors.set(name, accessor);
                }
                if (member.kind === 'get') {
                    accessor.getter = member;
                }
                else {
                    accessor.setter = member;
                }
            }
            continue;
        }
        const astNode = member.key;
        const sourceRange = document.sourceRangeForNode(member);
        const jsdocAnn = jsdoc.parseJsdoc(getAttachedComment(member) || '');
        const detectedType = getClosureType(member.value, jsdocAnn, sourceRange, document);
        let type = undefined;
        if (detectedType.successful) {
            type = detectedType.value;
        }
        properties.set(name, {
            name,
            astNode: { language: 'js', node: astNode, containingDocument: document },
            type,
            jsdoc: jsdocAnn,
            sourceRange,
            description: jsdocAnn ? jsdoc.getDescription(jsdocAnn) : undefined,
            privacy: getOrInferPrivacy(name, jsdocAnn),
            warnings: [],
            readOnly: jsdoc.hasTag(jsdocAnn, 'readonly'),
        });
    }
    for (const val of accessors.values()) {
        let getter = null;
        let setter = null;
        if (val.getter) {
            const parsedJsdoc = jsdoc.parseJsdoc(getAttachedComment(val.getter) || '');
            getter =
                extractPropertyFromGetterOrSetter(val.getter, parsedJsdoc, document);
        }
        if (val.setter) {
            const parsedJsdoc = jsdoc.parseJsdoc(getAttachedComment(val.setter) || '');
            setter =
                extractPropertyFromGetterOrSetter(val.setter, parsedJsdoc, document);
        }
        const prop = getter || setter;
        if (!prop) {
            continue;
        }
        if (!prop.readOnly) {
            prop.readOnly = (val.setter === undefined);
        }
        properties.set(prop.name, prop);
    }
    return properties;
}
exports.extractPropertiesFromClassOrObjectBody = extractPropertiesFromClassOrObjectBody;
/**
 * Get the canonical statement or declaration for the given node.
 *
 * It would otherwise be difficult, or require specialized code for each kind of
 * feature, to determine which node is the canonical node for a feature. This
 * function is simple, it only walks up, and it stops once it reaches a clear
 * feature boundary. And since we're calling this function both on the indexing
 * and the lookup sides, we can be confident that both will agree on the same
 * node.
 *
 * There may be more than one feature within a single statement (e.g. `export
 * class Foo {}` is both a Class and an Export, but between `kind` and `id` we
 * should still have enough info to narrow down to the intended feature.
 *
 * See `DeclaredWithStatement` and `BaseDocumentQuery` to see where this is
 * used.
 */
function getCanonicalStatement(nodePath) {
    const node = nodePath.node;
    const parent = nodePath.parentPath;
    if ((parent && !isStatementWithUniqueStatementChild(parent.node)) &&
        babel.isStatement(node)) {
        return node;
    }
    if (parent != null) {
        return getCanonicalStatement(parent);
    }
    return undefined;
}
exports.getCanonicalStatement = getCanonicalStatement;
/**
 * Some statements have many statments as children, like a BlockStatement.
 *
 * Some statements have a single unique statement child, like
 * ExportNamedDeclaration or ExportDefaultDeclaration. When we're talking up the
 * node tree but we want to stay within a single statement, we don't want to
 * walk up to a BlockStatement, as that's a group of many statements, but we do
 * want to walk up to ExportNamedDeclaration.
 */
function isStatementWithUniqueStatementChild(node) {
    return babel.isExportNamedDeclaration(node) ||
        babel.isExportDefaultDeclaration(node);
}
/** What names does a declaration assign to? */
function* getBindingNamesFromDeclaration(declaration) {
    if (declaration == null) {
        return;
    }
    switch (declaration.type) {
        case 'ClassDeclaration':
        case 'DeclareClass':
            yield declaration.id.name;
            break;
        case 'VariableDeclaration':
            for (const varDecl of declaration.declarations) {
                yield* getNamesFromLValue(varDecl.id);
            }
            break;
        case 'FunctionDeclaration':
        case 'DeclareFunction':
        case 'DeclareInterface':
        case 'DeclareTypeAlias':
        case 'InterfaceDeclaration':
        case 'DeclareVariable':
        case 'TypeAlias':
            yield declaration.id.name;
            break;
        case 'ExportAllDeclaration':
            // Can't do this syntactically. See Export#resolve.
            break;
        case 'ExportDefaultDeclaration':
            yield 'default';
            break;
        case 'ExportNamedDeclaration':
            for (const specifier of declaration.specifiers) {
                if (specifier.exported.type === 'Identifier') {
                    yield specifier.exported.name;
                }
            }
            yield* getBindingNamesFromDeclaration(declaration.declaration);
            break;
        case 'DeclareModule':
            if (declaration.id.type === 'StringLiteral') {
                yield declaration.id.value;
            }
            else {
                yield declaration.id.name;
            }
            break;
        case 'ImportDeclaration':
            for (const specifier of declaration.specifiers) {
                yield specifier.local.name;
            }
            break;
        default:
            assertNever(declaration);
    }
}
exports.getBindingNamesFromDeclaration = getBindingNamesFromDeclaration;
/**
 * Given an LValue, what are the names it assigns to?
 *
 * Internal utility function for getBindingNamesFromDeclaration.
 */
function* getNamesFromLValue(lhs) {
    switch (lhs.type) {
        case 'Identifier':
            // x = _;
            yield lhs.name;
            break;
        case 'ArrayPattern':
            // [a, b, c] = _;
            for (const element of lhs.elements) {
                if (babel.isLVal(element)) {
                    yield* getNamesFromLValue(element);
                }
            }
            break;
        case 'RestElement':
            // the `...more` part of either
            // [a, b, ...more] = _;
            // {a: b, ...more} = _;
            yield* getNamesFromLValue(lhs.argument);
            break;
        case 'MemberExpression':
            // foo.bar = _;
            const name = astValue.getIdentifierName(lhs);
            if (name !== undefined) {
                yield name;
            }
            break;
        case 'ObjectPattern':
            // {a: b, c} = _;
            for (const prop of lhs.properties) {
                switch (prop.type) {
                    case 'ObjectProperty':
                        // If the property has a 'value' (like)
                        yield* getNamesFromLValue(prop.value);
                        break;
                    case 'RestProperty':
                        yield* getNamesFromLValue(prop.argument);
                        break;
                    default:
                        assertNever(prop);
                }
            }
            break;
        case 'AssignmentPattern':
            // var [a = 'defaultVal'] = _;
            yield* getNamesFromLValue(lhs.left);
            break;
        default:
            assertNever(lhs);
    }
}
function assertNever(never) {
    throw new Error(`Unexpected ast node: ${util.inspect(never)}`);
}
//# sourceMappingURL=esutil.js.map