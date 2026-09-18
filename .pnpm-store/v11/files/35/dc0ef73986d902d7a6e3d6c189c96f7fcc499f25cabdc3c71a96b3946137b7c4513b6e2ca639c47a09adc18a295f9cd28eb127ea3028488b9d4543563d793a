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
const generator_1 = require("@babel/generator");
const babel = require("@babel/types");
const doctrine = require("doctrine");
const model_1 = require("../model/model");
const declaration_property_handlers_1 = require("../polymer/declaration-property-handlers");
const polymer_element_1 = require("../polymer/polymer-element");
const polymer2_config_1 = require("../polymer/polymer2-config");
const polymer2_mixin_scanner_1 = require("../polymer/polymer2-mixin-scanner");
const astValue = require("./ast-value");
const ast_value_1 = require("./ast-value");
const esutil = require("./esutil");
const esutil_1 = require("./esutil");
const jsdoc = require("./jsdoc");
/**
 * Find and classify classes from source code.
 *
 * Currently this has a bunch of Polymer stuff baked in that shouldn't be here
 * in order to support generating only one feature for stuff that's essentially
 * more specific kinds of classes, like Elements, PolymerElements, Mixins, etc.
 *
 * In a future change we'll add a mechanism whereby plugins can claim and
 * specialize classes.
 */
class ClassScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const classFinder = new ClassFinder(document);
            const elementDefinitionFinder = new CustomElementsDefineCallFinder(document);
            const prototypeMemberFinder = new PrototypeMemberFinder(document);
            yield visit(prototypeMemberFinder);
            const mixinFinder = new polymer2_mixin_scanner_1.MixinVisitor(document, prototypeMemberFinder);
            // Find all classes and all calls to customElements.define()
            yield Promise.all([
                visit(classFinder),
                visit(elementDefinitionFinder),
                visit(mixinFinder),
            ]);
            const mixins = mixinFinder.mixins;
            const elementDefinitionsByClassName = new Map();
            // For classes that show up as expressions in the second argument position
            // of a customElements.define call.
            const elementDefinitionsByClassExpression = new Map();
            for (const defineCall of elementDefinitionFinder.calls) {
                // MaybeChainedIdentifier is invented below. It's like Identifier, but it
                // includes 'Polymer.Element' as a name.
                if (defineCall.class_.type === 'MaybeChainedIdentifier') {
                    elementDefinitionsByClassName.set(defineCall.class_.name, defineCall);
                }
                else {
                    elementDefinitionsByClassExpression.set(defineCall.class_, defineCall);
                }
            }
            // TODO(rictic): emit ElementDefineCallFeatures for define calls that don't
            //     map to any local classes?
            const mixinClassExpressions = new Set();
            for (const mixin of mixins) {
                if (mixin.classAstNode) {
                    mixinClassExpressions.add(mixin.classAstNode);
                }
            }
            // Next we want to distinguish custom elements from other classes.
            const customElements = [];
            const normalClasses = [];
            const classMap = new Map();
            for (const class_ of classFinder.classes) {
                if (class_.astNode.language === 'js' &&
                    mixinClassExpressions.has(class_.astNode.node)) {
                    // This class is a mixin and has already been handled as such.
                    continue;
                }
                if (class_.name) {
                    classMap.set(class_.name, class_);
                }
                // Class expressions inside the customElements.define call
                if (babel.isClassExpression(class_.astNode.node)) {
                    const definition = elementDefinitionsByClassExpression.get(class_.astNode.node);
                    if (definition) {
                        customElements.push({ class_, definition });
                        continue;
                    }
                }
                // Classes whose names are referenced in a same-file customElements.define
                const definition = elementDefinitionsByClassName.get(class_.name) ||
                    elementDefinitionsByClassName.get(class_.localName);
                if (definition) {
                    customElements.push({ class_, definition });
                    continue;
                }
                // Classes explicitly defined as elements in their jsdoc tags.
                // TODO(justinfagnani): remove @polymerElement support
                if (jsdoc.hasTag(class_.jsdoc, 'customElement') ||
                    jsdoc.hasTag(class_.jsdoc, 'polymerElement')) {
                    customElements.push({ class_ });
                    continue;
                }
                // Classes that aren't custom elements, or at least, aren't obviously.
                normalClasses.push(class_);
            }
            for (const [name, members] of prototypeMemberFinder.members) {
                if (classMap.has(name)) {
                    const cls = classMap.get(name);
                    cls.finishInitialization(members.methods, members.properties);
                }
            }
            const scannedFeatures = [];
            for (const element of customElements) {
                scannedFeatures.push(this._makeElementFeature(element, document));
            }
            for (const scannedClass of normalClasses) {
                scannedFeatures.push(scannedClass);
            }
            for (const mixin of mixins) {
                scannedFeatures.push(mixin);
            }
            const collapsedClasses = this.collapseEphemeralSuperclasses(scannedFeatures, classFinder);
            return {
                features: collapsedClasses,
                warnings: [
                    ...elementDefinitionFinder.warnings,
                    ...classFinder.warnings,
                    ...mixinFinder.warnings,
                ]
            };
        });
    }
    /**
     * Handle the pattern where a class's superclass is declared as a separate
     * variable, usually so that mixins can be applied in a way that is compatible
     * with the Closure compiler. We consider a class ephemeral if:
     *
     * 1) It is the superclass of one or more classes.
     * 2) It is declared using a const, let, or var.
     * 3) It is annotated as @private.
     */
    collapseEphemeralSuperclasses(allClasses, classFinder) {
        const possibleEphemeralsById = new Map();
        const classesBySuperClassId = new Map();
        for (const cls of allClasses) {
            if (cls.name === undefined) {
                continue;
            }
            if (classFinder.fromVariableDeclarators.has(cls) &&
                cls.privacy === 'private') {
                possibleEphemeralsById.set(cls.name, cls);
            }
            if (cls.superClass !== undefined) {
                const superClassId = cls.superClass.identifier;
                const childClasses = classesBySuperClassId.get(superClassId);
                if (childClasses === undefined) {
                    classesBySuperClassId.set(superClassId, [cls]);
                }
                else {
                    childClasses.push(cls);
                }
            }
        }
        const ephemerals = new Set();
        for (const [superClassId, childClasses] of classesBySuperClassId) {
            const superClass = possibleEphemeralsById.get(superClassId);
            if (superClass === undefined) {
                continue;
            }
            let isEphemeral = false;
            for (const childClass of childClasses) {
                // Feature properties are readonly, hence this hacky cast. We could also
                // make a new feature, but then we'd need a good way to clone a feature.
                // It's pretty safe because we're still in the construction phase for
                // scanned classes, so we know nothing else could be relying on the
                // previous value yet.
                childClass.superClass = superClass.superClass;
                childClass.mixins.push(...superClass.mixins);
                isEphemeral = true;
            }
            if (isEphemeral) {
                ephemerals.add(superClass);
            }
        }
        return allClasses.filter((cls) => !ephemerals.has(cls));
    }
    _makeElementFeature(element, document) {
        const class_ = element.class_;
        const astNode = element.class_.astNode;
        const docs = element.class_.jsdoc;
        const customElementTag = jsdoc.getTag(class_.jsdoc, 'customElement');
        let tagName = undefined;
        if (element.definition &&
            element.definition.tagName.type === 'string-literal') {
            tagName = element.definition.tagName.value;
        }
        else if (customElementTag && customElementTag.description) {
            tagName = customElementTag.description;
        }
        else if (babel.isClassExpression(astNode.node) ||
            babel.isClassDeclaration(astNode.node)) {
            tagName = polymer2_config_1.getIsValue(astNode.node);
        }
        let warnings = [];
        let scannedElement;
        let methods = new Map();
        let staticMethods = new Map();
        let observers = [];
        // This will cover almost all classes, except those defined only by
        // applying a mixin. e.g.   const MyElem = Mixin(HTMLElement)
        if (babel.isClassExpression(astNode.node) ||
            babel.isClassDeclaration(astNode.node)) {
            const observersResult = this._getObservers(astNode.node, document);
            observers = [];
            if (observersResult) {
                observers = observersResult.observers;
                warnings = warnings.concat(observersResult.warnings);
            }
            const polymerProps = polymer2_config_1.getPolymerProperties(astNode.node, document);
            for (const prop of polymerProps) {
                const constructorProp = class_.properties.get(prop.name);
                let finalProp;
                if (constructorProp) {
                    finalProp = polymer_element_1.mergePropertyDeclarations(constructorProp, prop);
                }
                else {
                    finalProp = prop;
                }
                class_.properties.set(prop.name, finalProp);
            }
            methods = esutil_1.getMethods(astNode.node, document);
            staticMethods = esutil_1.getStaticMethods(astNode.node, document);
        }
        const extends_ = getExtendsTypeName(docs);
        // TODO(justinfagnani): Infer mixin applications and superclass from AST.
        scannedElement = new polymer_element_1.ScannedPolymerElement({
            className: class_.name,
            tagName,
            astNode,
            statementAst: class_.statementAst,
            properties: [...class_.properties.values()],
            methods,
            staticMethods,
            observers,
            events: astNode.language === 'js' ?
                esutil.getEventComments(astNode.node) :
                new Map(),
            attributes: new Map(),
            behaviors: [],
            extends: extends_,
            listeners: [],
            description: class_.description,
            sourceRange: class_.sourceRange,
            superClass: class_.superClass,
            jsdoc: class_.jsdoc,
            abstract: class_.abstract,
            mixins: class_.mixins,
            privacy: class_.privacy,
            isLegacyFactoryCall: false,
        });
        if (babel.isClassExpression(astNode.node) ||
            babel.isClassDeclaration(astNode.node)) {
            const observedAttributes = this._getObservedAttributes(astNode.node, document);
            if (observedAttributes != null) {
                // If a class defines observedAttributes, it overrides what the base
                // classes defined.
                // TODO(justinfagnani): define and handle composition patterns.
                scannedElement.attributes.clear();
                for (const attr of observedAttributes) {
                    scannedElement.attributes.set(attr.name, attr);
                }
            }
        }
        warnings.forEach((w) => scannedElement.warnings.push(w));
        return scannedElement;
    }
    _getObservers(node, document) {
        const returnedValue = polymer2_config_1.getStaticGetterValue(node, 'observers');
        if (returnedValue) {
            return declaration_property_handlers_1.extractObservers(returnedValue, document);
        }
    }
    _getObservedAttributes(node, document) {
        const returnedValue = polymer2_config_1.getStaticGetterValue(node, 'observedAttributes');
        if (returnedValue && babel.isArrayExpression(returnedValue)) {
            return this._extractAttributesFromObservedAttributes(returnedValue, document);
        }
    }
    /**
     * Extract attributes from the array expression inside a static
     * observedAttributes method.
     *
     * e.g.
     *     static get observedAttributes() {
     *       return [
     *         /** @type {boolean} When given the element is totally inactive *\/
     *         'disabled',
     *         /** @type {boolean} When given the element is expanded *\/
     *         'open'
     *       ];
     *     }
     */
    _extractAttributesFromObservedAttributes(arry, document) {
        const results = [];
        for (const expr of arry.elements) {
            const value = astValue.expressionToValue(expr);
            if (value && typeof value === 'string') {
                let description = '';
                let type = null;
                const comment = esutil.getAttachedComment(expr);
                if (comment) {
                    const annotation = jsdoc.parseJsdoc(comment);
                    description = annotation.description || description;
                    const tags = annotation.tags || [];
                    for (const tag of tags) {
                        if (tag.title === 'type') {
                            type = type || doctrine.type.stringify(tag.type);
                        }
                        // TODO(justinfagnani): this appears wrong, any tag could have a
                        // description do we really let any tag's description override
                        // the previous?
                        description = description || tag.description || '';
                    }
                }
                const attribute = {
                    name: value,
                    description: description,
                    sourceRange: document.sourceRangeForNode(expr),
                    astNode: { language: 'js', containingDocument: document, node: expr },
                    warnings: [],
                };
                if (type) {
                    attribute.type = type;
                }
                results.push(attribute);
            }
        }
        return results;
    }
}
exports.ClassScanner = ClassScanner;
class PrototypeMemberFinder {
    constructor(document) {
        this.members = new model_1.MapWithDefault(() => ({
            methods: new Map(),
            properties: new Map()
        }));
        this._document = document;
    }
    enterExpressionStatement(node) {
        if (babel.isAssignmentExpression(node.expression)) {
            this._createMemberFromAssignment(node.expression, getJSDocAnnotationForNode(node));
        }
        else if (babel.isMemberExpression(node.expression)) {
            this._createMemberFromMemberExpression(node.expression, getJSDocAnnotationForNode(node));
        }
    }
    _createMemberFromAssignment(node, jsdocAnn) {
        if (!babel.isMemberExpression(node.left) ||
            !babel.isMemberExpression(node.left.object) ||
            !babel.isIdentifier(node.left.property)) {
            return;
        }
        const leftExpr = node.left.object;
        const leftProperty = node.left.property;
        const cls = ast_value_1.getIdentifierName(leftExpr.object);
        if (!cls || ast_value_1.getIdentifierName(leftExpr.property) !== 'prototype') {
            return;
        }
        if (babel.isFunctionExpression(node.right)) {
            const prop = this._createMethodFromExpression(leftProperty.name, node.right, jsdocAnn);
            if (prop) {
                this._addMethodToClass(cls, prop);
            }
        }
        else {
            const method = this._createPropertyFromExpression(leftProperty.name, node, jsdocAnn);
            if (method) {
                this._addPropertyToClass(cls, method);
            }
        }
    }
    _addMethodToClass(cls, member) {
        const classMembers = this.members.get(cls);
        classMembers.methods.set(member.name, member);
    }
    _addPropertyToClass(cls, member) {
        const classMembers = this.members.get(cls);
        classMembers.properties.set(member.name, member);
    }
    _createMemberFromMemberExpression(node, jsdocAnn) {
        const left = node.object;
        // we only want `something.prototype.member`
        if (!babel.isIdentifier(node.property) || !babel.isMemberExpression(left) ||
            ast_value_1.getIdentifierName(left.property) !== 'prototype') {
            return;
        }
        const cls = ast_value_1.getIdentifierName(left.object);
        if (!cls) {
            return;
        }
        if (jsdoc.hasTag(jsdocAnn, 'function')) {
            const method = this._createMethodFromExpression(node.property.name, node, jsdocAnn);
            if (method) {
                this._addMethodToClass(cls, method);
            }
        }
        else {
            const prop = this._createPropertyFromExpression(node.property.name, node, jsdocAnn);
            if (prop) {
                this._addPropertyToClass(cls, prop);
            }
        }
    }
    _createPropertyFromExpression(name, node, jsdocAnn) {
        let description;
        let type;
        let readOnly = false;
        const privacy = esutil_1.getOrInferPrivacy(name, jsdocAnn);
        const sourceRange = this._document.sourceRangeForNode(node);
        const warnings = [];
        if (jsdocAnn) {
            description = jsdoc.getDescription(jsdocAnn);
            readOnly = jsdoc.hasTag(jsdocAnn, 'readonly');
        }
        let detectedType;
        if (babel.isAssignmentExpression(node)) {
            detectedType =
                esutil_1.getClosureType(node.right, jsdocAnn, sourceRange, this._document);
        }
        else {
            detectedType =
                esutil_1.getClosureType(node, jsdocAnn, sourceRange, this._document);
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
            astNode: { language: 'js', containingDocument: this._document, node },
            type,
            jsdoc: jsdocAnn,
            sourceRange,
            description,
            privacy,
            warnings,
            readOnly,
        };
    }
    _createMethodFromExpression(name, node, jsdocAnn) {
        let description;
        let ret;
        const privacy = esutil_1.getOrInferPrivacy(name, jsdocAnn);
        const params = new Map();
        if (jsdocAnn) {
            description = jsdoc.getDescription(jsdocAnn);
            ret = esutil_1.getReturnFromAnnotation(jsdocAnn);
            if (babel.isFunctionExpression(node)) {
                (node.params || []).forEach((nodeParam) => {
                    const param = esutil_1.toMethodParam(nodeParam, jsdocAnn);
                    params.set(param.name, param);
                });
            }
            else {
                for (const tag of (jsdocAnn.tags || [])) {
                    if (tag.title !== 'param' || !tag.name) {
                        continue;
                    }
                    let tagType;
                    let tagDescription;
                    if (tag.type) {
                        tagType = doctrine.type.stringify(tag.type);
                    }
                    if (tag.description) {
                        tagDescription = tag.description;
                    }
                    params.set(tag.name, { name: tag.name, type: tagType, description: tagDescription });
                }
            }
        }
        if (ret === undefined && babel.isFunctionExpression(node)) {
            ret = esutil_1.inferReturnFromBody(node);
        }
        return {
            name,
            type: ret !== undefined ? ret.type : undefined,
            description,
            sourceRange: this._document.sourceRangeForNode(node),
            warnings: [],
            astNode: { language: 'js', containingDocument: this._document, node },
            jsdoc: jsdocAnn,
            params: Array.from(params.values()),
            return: ret,
            privacy
        };
    }
}
exports.PrototypeMemberFinder = PrototypeMemberFinder;
/**
 * Finds all classes and matches them up with their best jsdoc comment.
 */
class ClassFinder {
    constructor(document) {
        this.classes = [];
        this.warnings = [];
        this.fromVariableDeclarators = new Set();
        this.alreadyMatched = new Set();
        this._document = document;
    }
    enterAssignmentExpression(node, _parent, path) {
        this.handleGeneralAssignment(astValue.getIdentifierName(node.left), node.right, path);
    }
    enterVariableDeclarator(node, _parent, path) {
        if (node.init) {
            this.handleGeneralAssignment(astValue.getIdentifierName(node.id), node.init, path);
        }
    }
    enterFunctionDeclaration(node, _parent, path) {
        this.handleGeneralAssignment(astValue.getIdentifierName(node.id), node.body, path);
    }
    /** Generalizes over variable declarators and assignment expressions. */
    handleGeneralAssignment(assignedName, value, path) {
        const doc = jsdoc.parseJsdoc(esutil.getBestComment(path) || '');
        if (babel.isClassExpression(value)) {
            const name = assignedName ||
                value.id && astValue.getIdentifierName(value.id) || undefined;
            this._classFound(name, doc, value, path);
        }
        else if (jsdoc.hasTag(doc, 'constructor') ||
            // TODO(justinfagnani): remove @polymerElement support
            jsdoc.hasTag(doc, 'customElement') ||
            jsdoc.hasTag(doc, 'polymerElement')) {
            this._classFound(assignedName, doc, value, path);
        }
    }
    enterClassExpression(node, parent, path) {
        // Class expressions may be on the right hand side of assignments, so
        // we may have already handled this expression from the parent or
        // grandparent node. Class declarations can't be on the right hand side of
        // assignments, so they'll definitely only be handled once.
        if (this.alreadyMatched.has(node)) {
            return;
        }
        const name = node.id ? astValue.getIdentifierName(node.id) : undefined;
        const comment = esutil.getAttachedComment(node) ||
            esutil.getAttachedComment(parent) || '';
        this._classFound(name, jsdoc.parseJsdoc(comment), node, path);
    }
    enterClassDeclaration(node, parent, path) {
        const name = astValue.getIdentifierName(node.id);
        const comment = esutil.getAttachedComment(node) ||
            esutil.getAttachedComment(parent) || '';
        this._classFound(name, jsdoc.parseJsdoc(comment), node, path);
    }
    _classFound(name, doc, astNode, path) {
        const namespacedName = name && ast_value_1.getNamespacedIdentifier(name, doc);
        const warnings = [];
        const properties = extractPropertiesFromClass(astNode, this._document);
        const methods = esutil_1.getMethods(astNode, this._document);
        const constructorMethod = esutil_1.getConstructorMethod(astNode, this._document);
        const scannedClass = new model_1.ScannedClass(namespacedName, name, { language: 'js', containingDocument: this._document, node: astNode }, esutil.getCanonicalStatement(path), doc, (doc.description || '').trim(), this._document.sourceRangeForNode(astNode), properties, methods, constructorMethod, esutil_1.getStaticMethods(astNode, this._document), this._getExtends(astNode, doc, warnings, this._document, path), jsdoc.getMixinApplications(this._document, astNode, doc, warnings, path), esutil_1.getOrInferPrivacy(namespacedName || '', doc), warnings, jsdoc.hasTag(doc, 'abstract'), jsdoc.extractDemos(doc));
        this.classes.push(scannedClass);
        if (babel.isVariableDeclarator(path.node)) {
            this.fromVariableDeclarators.add(scannedClass);
        }
        if (babel.isClassExpression(astNode)) {
            this.alreadyMatched.add(astNode);
        }
    }
    /**
     * Returns the name of the superclass, if any.
     */
    _getExtends(node, docs, warnings, document, path) {
        const extendsId = getExtendsTypeName(docs);
        // prefer @extends annotations over extends clauses
        if (extendsId !== undefined) {
            // TODO(justinfagnani): we need source ranges for jsdoc annotations
            const sourceRange = document.sourceRangeForNode(node);
            if (extendsId == null) {
                warnings.push(new model_1.Warning({
                    code: 'class-extends-annotation-no-id',
                    message: '@extends annotation with no identifier',
                    severity: model_1.Severity.WARNING,
                    sourceRange,
                    parsedDocument: this._document
                }));
            }
            else {
                return new model_1.ScannedReference('class', extendsId, sourceRange, undefined, path);
            }
        }
        else if (babel.isClassDeclaration(node) || babel.isClassExpression(node)) {
            // If no @extends tag, look for a superclass.
            const superClass = node.superClass;
            if (superClass != null) {
                let extendsId = ast_value_1.getIdentifierName(superClass);
                if (extendsId != null) {
                    if (extendsId.startsWith('window.')) {
                        extendsId = extendsId.substring('window.'.length);
                    }
                    const sourceRange = document.sourceRangeForNode(superClass);
                    return new model_1.ScannedReference('class', extendsId, sourceRange, {
                        language: 'js',
                        node: node.superClass,
                        containingDocument: document
                    }, path);
                }
            }
        }
    }
}
/** Finds calls to customElements.define() */
class CustomElementsDefineCallFinder {
    constructor(document) {
        this.warnings = [];
        this.calls = [];
        this._document = document;
    }
    enterCallExpression(node) {
        const callee = astValue.getIdentifierName(node.callee);
        if (!(callee === 'window.customElements.define' ||
            callee === 'customElements.define')) {
            return;
        }
        const tagNameExpression = this._getTagNameExpression(node.arguments[0]);
        if (tagNameExpression == null) {
            return;
        }
        const elementClassExpression = this._getElementClassExpression(node.arguments[1]);
        if (elementClassExpression == null) {
            return;
        }
        this.calls.push({ tagName: tagNameExpression, class_: elementClassExpression });
    }
    _getTagNameExpression(expression) {
        if (expression == null) {
            return;
        }
        const tryForLiteralString = astValue.expressionToValue(expression);
        if (tryForLiteralString != null &&
            typeof tryForLiteralString === 'string') {
            return {
                type: 'string-literal',
                value: tryForLiteralString,
                sourceRange: this._document.sourceRangeForNode(expression)
            };
        }
        if (babel.isMemberExpression(expression)) {
            // Might be something like MyElement.is
            const isPropertyNameIs = (babel.isIdentifier(expression.property) &&
                expression.property.name === 'is') ||
                (astValue.expressionToValue(expression.property) === 'is');
            const className = astValue.getIdentifierName(expression.object);
            if (isPropertyNameIs && className) {
                return {
                    type: 'is',
                    className,
                    classNameSourceRange: this._document.sourceRangeForNode(expression.object)
                };
            }
        }
        this.warnings.push(new model_1.Warning({
            code: 'cant-determine-element-tagname',
            message: `Unable to evaluate this expression down to a definitive string ` +
                `tagname.`,
            severity: model_1.Severity.WARNING,
            sourceRange: this._document.sourceRangeForNode(expression),
            parsedDocument: this._document
        }));
        return undefined;
    }
    _getElementClassExpression(elementDefn) {
        if (elementDefn == null) {
            return null;
        }
        const className = astValue.getIdentifierName(elementDefn);
        if (className) {
            return {
                type: 'MaybeChainedIdentifier',
                name: className,
                sourceRange: this._document.sourceRangeForNode(elementDefn)
            };
        }
        if (babel.isClassExpression(elementDefn)) {
            return elementDefn;
        }
        this.warnings.push(new model_1.Warning({
            code: 'cant-determine-element-class',
            message: `Unable to evaluate this expression down to a class reference.`,
            severity: model_1.Severity.WARNING,
            sourceRange: this._document.sourceRangeForNode(elementDefn),
            parsedDocument: this._document,
        }));
        return null;
    }
}
function extractPropertiesFromClass(astNode, document) {
    const properties = new Map();
    if (!babel.isClass(astNode)) {
        return properties;
    }
    const construct = esutil_1.getConstructorClassMethod(astNode);
    if (construct) {
        const props = extractPropertiesFromConstructor(construct, document);
        for (const prop of props.values()) {
            properties.set(prop.name, prop);
        }
    }
    for (const prop of esutil
        .extractPropertiesFromClassOrObjectBody(astNode, document)
        .values()) {
        const existing = properties.get(prop.name);
        if (!existing) {
            properties.set(prop.name, prop);
        }
        else {
            properties.set(prop.name, {
                name: prop.name,
                astNode: prop.astNode,
                type: prop.type || existing.type,
                jsdoc: prop.jsdoc,
                sourceRange: prop.sourceRange,
                description: prop.description || existing.description,
                privacy: prop.privacy || existing.privacy,
                warnings: prop.warnings,
                readOnly: prop.readOnly === undefined ?
                    existing.readOnly : prop.readOnly
            });
        }
    }
    return properties;
}
exports.extractPropertiesFromClass = extractPropertiesFromClass;
function extractPropertyFromExpressionStatement(statement, document) {
    let name;
    let astNode;
    let defaultValue;
    if (babel.isAssignmentExpression(statement.expression)) {
        // statements like:
        // /** @public The foo. */
        // this.foo = baz;
        name = getPropertyNameOnThisExpression(statement.expression.left);
        astNode = statement.expression.left;
        defaultValue = generator_1.default(statement.expression.right).code;
    }
    else if (babel.isMemberExpression(statement.expression)) {
        // statements like:
        // /** @public The foo. */
        // this.foo;
        name = getPropertyNameOnThisExpression(statement.expression);
        astNode = statement;
    }
    else {
        return null;
    }
    if (name === undefined) {
        return null;
    }
    const annotation = getJSDocAnnotationForNode(statement);
    if (!annotation) {
        return null;
    }
    return {
        name,
        astNode: { language: 'js', containingDocument: document, node: astNode },
        type: getTypeFromAnnotation(annotation),
        default: defaultValue,
        jsdoc: annotation,
        sourceRange: document.sourceRangeForNode(astNode),
        description: jsdoc.getDescription(annotation),
        privacy: esutil_1.getOrInferPrivacy(name, annotation),
        warnings: [],
        readOnly: jsdoc.hasTag(annotation, 'const'),
    };
}
function extractPropertiesFromConstructor(method, document) {
    const properties = new Map();
    for (const statement of method.body.body) {
        if (!babel.isExpressionStatement(statement)) {
            continue;
        }
        const prop = extractPropertyFromExpressionStatement(statement, document);
        if (!prop) {
            continue;
        }
        properties.set(prop.name, prop);
    }
    return properties;
}
function getJSDocAnnotationForNode(node) {
    const comment = esutil.getAttachedComment(node);
    const jsdocAnn = comment === undefined ? undefined : jsdoc.parseJsdoc(comment);
    if (!jsdocAnn || jsdocAnn.tags.length === 0) {
        // The comment only counts if there's a jsdoc annotation in there
        // somewhere.
        // Otherwise it's just an assignment, maybe to a property in a
        // super class or something.
        return undefined;
    }
    return jsdocAnn;
}
function getTypeFromAnnotation(jsdocAnn) {
    const typeTag = jsdoc.getTag(jsdocAnn, 'type');
    let type = undefined;
    if (typeTag && typeTag.type) {
        type = doctrine.type.stringify(typeTag.type);
    }
    return type;
}
function getPropertyNameOnThisExpression(node) {
    if (!babel.isMemberExpression(node) || node.computed ||
        !babel.isThisExpression(node.object) ||
        !babel.isIdentifier(node.property)) {
        return;
    }
    return node.property.name;
}
/**
 * Return the type name from the first @extends annotation. Supports either
 * `@extends {SuperClass}` or `@extends SuperClass` forms.
 */
function getExtendsTypeName(docs) {
    const tag = jsdoc.getTag(docs, 'extends');
    if (!tag) {
        return undefined;
    }
    return tag.type ? doctrine.type.stringify(tag.type) : tag.name;
}
//# sourceMappingURL=class-scanner.js.map