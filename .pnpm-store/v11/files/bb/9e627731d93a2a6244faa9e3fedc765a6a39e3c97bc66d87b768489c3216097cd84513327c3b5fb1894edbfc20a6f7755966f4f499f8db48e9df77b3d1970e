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
const dom5 = require("dom5/lib/index-next");
const parse5 = require("parse5");
const esutil_1 = require("../javascript/esutil");
const jsdoc = require("../javascript/jsdoc");
const model_1 = require("../model/model");
function mergePropertyDeclarations(propA, propB) {
    if (propA.name !== propB.name) {
        throw new Error(`Tried to merge properties with different names: ` +
            `'${propA.name}' and ' ${propB.name}'`);
    }
    const name = propA.name;
    const description = jsdoc.pickBestDescription(propA.description, propB.description);
    const jsdocAnn = { description: description || '', tags: [] };
    if (propA.jsdoc) {
        jsdocAnn.tags.push(...propA.jsdoc.tags);
    }
    if (propB.jsdoc) {
        jsdocAnn.tags.push(...propB.jsdoc.tags);
    }
    const privacy = esutil_1.getOrInferPrivacy(propA.name, jsdocAnn);
    const warnings = [...propA.warnings, ...propB.warnings];
    // If either are marked as readOnly, both are.
    const readOnly = propA.readOnly || propB.readOnly;
    // Handle all regular property metadata.
    const scannedRegularProperty = {
        // calculated above with care
        name,
        privacy,
        description,
        warnings,
        readOnly,
        jsdoc: jsdocAnn,
        // prefer A, but take B if there's no A.
        sourceRange: propA.sourceRange || propB.sourceRange,
        astNode: propA.astNode || propB.astNode,
        changeEvent: propA.changeEvent || propB.changeEvent,
        default: propA.default || propB.default,
        type: propA.type || propB.type,
    };
    const scannedPolymerProperty = scannedRegularProperty;
    // For the scannedPolymerProperty keys, set them if they're there
    const keys = [
        'published',
        'notify',
        'observer',
        'observerNode',
        'observerExpression',
        'reflectToAttribute',
        'computedExpression'
    ];
    for (const key of keys) {
        if (propA[key] || propB[key]) {
            scannedPolymerProperty[key] = propA[key] || propB[key];
        }
    }
    if (propA.published || propB.published) {
        scannedPolymerProperty.published = propA.published || propB.published;
    }
    return scannedPolymerProperty;
}
exports.mergePropertyDeclarations = mergePropertyDeclarations;
class LocalId {
    constructor(name, range, nodeName) {
        this.name = name;
        this.range = range;
        this.nodeName = nodeName;
    }
}
exports.LocalId = LocalId;
function addProperty(target, prop) {
    const existingProp = target.properties.get(prop.name);
    if (existingProp) {
        prop = mergePropertyDeclarations(existingProp, prop);
    }
    target.properties.set(prop.name, prop);
    const attributeName = propertyToAttributeName(prop.name);
    // Don't produce attributes or events for nonpublic properties, properties
    // that aren't in Polymer's `properties` block (i.e. not published),
    // or properties whose names can't be converted into attribute names.
    if ((prop.privacy && prop.privacy !== 'public') || !attributeName ||
        !prop.published) {
        return;
    }
    target.attributes.set(attributeName, {
        name: attributeName,
        sourceRange: prop.sourceRange,
        description: prop.description,
        type: prop.type,
        changeEvent: prop.notify ? `${attributeName}-changed` : undefined
    });
    if (prop.notify) {
        const name = `${attributeName}-changed`;
        target.events.set(name, {
            name,
            description: `Fired when the \`${prop.name}\` property changes.`,
            sourceRange: prop.sourceRange,
            astNode: prop.astNode,
            warnings: [],
            params: []
        });
    }
}
exports.addProperty = addProperty;
function addMethod(target, method) {
    target.methods.set(method.name, method);
}
exports.addMethod = addMethod;
function addStaticMethod(target, method) {
    target.staticMethods.set(method.name, method);
}
exports.addStaticMethod = addStaticMethod;
/**
 * The metadata for a single polymer element
 */
class ScannedPolymerElement extends model_1.ScannedElement {
    constructor(options) {
        super();
        this.properties = new Map();
        this.methods = new Map();
        this.staticMethods = new Map();
        this.observers = [];
        this.listeners = [];
        this.behaviorAssignments = [];
        // Indicates if an element is a pseudo element
        this.pseudo = false;
        this.abstract = false;
        this.tagName = options.tagName;
        this.className = options.className;
        this.superClass = options.superClass;
        this.mixins = options.mixins;
        this.extends = options.extends;
        this.jsdoc = options.jsdoc;
        this.description = options.description || '';
        this.attributes = options.attributes;
        this.observers = options.observers;
        this.listeners = options.listeners;
        this.behaviorAssignments = options.behaviors;
        this.events = options.events;
        this.abstract = options.abstract;
        this.privacy = options.privacy;
        this.astNode = options.astNode;
        this.statementAst = options.statementAst;
        this.sourceRange = options.sourceRange;
        this.isLegacyFactoryCall = options.isLegacyFactoryCall || false;
        if (options.properties) {
            options.properties.forEach((p) => this.addProperty(p));
        }
        if (options.methods) {
            options.methods.forEach((m) => this.addMethod(m));
        }
        if (options.staticMethods) {
            options.staticMethods.forEach((m) => this.addStaticMethod(m));
        }
        const summaryTag = jsdoc.getTag(this.jsdoc, 'summary');
        this.summary =
            (summaryTag !== undefined && summaryTag.description != null) ?
                summaryTag.description :
                '';
    }
    addProperty(prop) {
        addProperty(this, prop);
    }
    addMethod(method) {
        addMethod(this, method);
    }
    addStaticMethod(method) {
        addStaticMethod(this, method);
    }
    resolve(document) {
        return new PolymerElement(this, document);
    }
}
exports.ScannedPolymerElement = ScannedPolymerElement;
class PolymerElement extends model_1.Element {
    constructor(scannedElement, document) {
        super(scannedElement, document);
        this.observers = [];
        this.listeners = [];
        this.behaviorAssignments = [];
        this.localIds = [];
        this.kinds.add('polymer-element');
        this.observers = Array.from(scannedElement.observers);
        this.listeners = Array.from(scannedElement.listeners);
        this.behaviorAssignments = Array.from(scannedElement.behaviorAssignments);
        const domModules = scannedElement.tagName == null ?
            new Set() :
            document.getFeatures({
                kind: 'dom-module',
                id: scannedElement.tagName,
                imported: true,
                externalPackages: true
            });
        let domModule = undefined;
        if (domModules.size === 1) {
            // TODO(rictic): warn if this isn't true.
            domModule = domModules.values().next().value;
        }
        if (domModule) {
            this.domModule = domModule.node;
            this.slots = this.slots.concat(domModule.slots);
            this.localIds = domModule.localIds.slice();
            // If there's a domModule and it's got a comment, that comment documents
            // this element too. Extract its description and @demo annotations.
            if (domModule.comment) {
                const domModuleJsdoc = jsdoc.parseJsdoc(domModule.comment);
                this.demos = [...jsdoc.extractDemos(domModuleJsdoc), ...this.demos];
                if (domModuleJsdoc.description) {
                    this.description =
                        (domModuleJsdoc.description + '\n\n' + this.description).trim();
                }
            }
            const template = dom5.query(domModule.node, dom5.predicates.hasTagName('template'));
            if (template) {
                this.template = {
                    kind: 'polymer-databinding',
                    contents: parse5.treeAdapters.default.getTemplateContent(template)
                };
            }
        }
        if (scannedElement.pseudo) {
            this.kinds.add('pseudo-element');
        }
        this.isLegacyFactoryCall = scannedElement.isLegacyFactoryCall;
    }
    emitPropertyMetadata(property) {
        return {
            polymer: {
                notify: property.notify,
                observer: property.observer,
                readOnly: property.readOnly,
                attributeType: property.attributeType,
            }
        };
    }
    _getSuperclassAndMixins(document, init) {
        const prototypeChain = super._getSuperclassAndMixins(document, init);
        const { warnings, behaviors } = getBehaviors(init.behaviorAssignments, document);
        this.warnings.push(...warnings);
        prototypeChain.push(...behaviors);
        return prototypeChain;
    }
}
exports.PolymerElement = PolymerElement;
/**
 * Implements Polymer core's translation of property names to attribute names.
 *
 * Returns null if the property name cannot be so converted.
 */
function propertyToAttributeName(propertyName) {
    // Polymer core will not map a property name that starts with an uppercase
    // character onto an attribute.
    if (propertyName[0].toUpperCase() === propertyName[0]) {
        return null;
    }
    return propertyName.replace(/([A-Z])/g, (_, c1) => `-${c1.toLowerCase()}`);
}
function getBehaviors(behaviorReferences, document) {
    const warnings = [];
    const behaviors = [];
    for (const scannedReference of behaviorReferences) {
        const resolvedReference = scannedReference.resolve(document);
        if (resolvedReference.warnings.length > 0) {
            warnings.push(...resolvedReference.warnings);
        }
        if (resolvedReference.feature) {
            behaviors.push(resolvedReference.feature);
        }
    }
    return { warnings, behaviors };
}
exports.getBehaviors = getBehaviors;
//# sourceMappingURL=polymer-element.js.map