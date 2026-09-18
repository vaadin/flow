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
const jsdocLib = require("../javascript/jsdoc");
const model_1 = require("../model/model");
const immutable_1 = require("./immutable");
/**
 * Represents a JS class as encountered in source code.
 *
 * We only emit a ScannedClass when there's not a more specific kind of feature.
 * Like, we don't emit a ScannedClass when we encounter an element or a mixin
 * (though in the future those features will likely extend from
 * ScannedClass/Class).
 *
 * TODO(rictic): currently there's a ton of duplicated code across the Class,
 *     Element, Mixin, PolymerElement, and PolymerMixin classes. We should
 *     really unify this stuff to a single representation and set of algorithms.
 */
class ScannedClass {
    constructor(className, localClassName, astNode, statementAst, jsdoc, description, sourceRange, properties, methods, constructorMethod, staticMethods, superClass, mixins, privacy, warnings, abstract, demos) {
        this.name = className;
        this.localName = localClassName;
        this.astNode = astNode;
        this.statementAst = statementAst;
        this.jsdoc = jsdoc;
        this.description = description;
        this.sourceRange = sourceRange;
        this.properties = properties;
        this.methods = methods;
        this.constructorMethod = constructorMethod;
        this.staticMethods = staticMethods;
        this.superClass = superClass;
        this.mixins = mixins;
        this.privacy = privacy;
        this.warnings = warnings;
        this.abstract = abstract;
        const summaryTag = jsdocLib.getTag(jsdoc, 'summary');
        this.summary = (summaryTag && summaryTag.description) || '';
        this.demos = demos;
    }
    resolve(document) {
        return new Class(this, document);
    }
    /**
     * Allows additional properties and methods
     * to be added to the class after initialization.
     * For example, members found attached to the
     * prototype at a later place in the document
     */
    finishInitialization(methods, properties) {
        const mutableMethods = immutable_1.unsafeAsMutable(this.methods);
        for (const [name, method] of methods) {
            mutableMethods.set(name, method);
        }
        for (const [name, prop] of properties) {
            this.properties.set(name, prop);
        }
    }
}
exports.ScannedClass = ScannedClass;
class Class {
    constructor(init, document) {
        this.kinds = new Set(['class']);
        this.identifiers = new Set();
        this.properties = new Map();
        this.methods = new Map();
        this.staticMethods = new Map();
        /**
         * Mixins that this class declares with `@mixes`.
         *
         * Mixins are applied linearly after the superclass, in order from first
         * to last. Mixins that compose other mixins will be flattened into a
         * single list. A mixin can be applied more than once, each time its
         * members override those before it in the prototype chain.
         */
        this.mixins = [];
        ({
            jsdoc: this.jsdoc,
            description: this.description,
            summary: this.summary,
            abstract: this.abstract,
            privacy: this.privacy,
            astNode: this.astNode,
            statementAst: this.statementAst,
            sourceRange: this.sourceRange
        } = init);
        this._parsedDocument = document.parsedDocument;
        this.warnings =
            init.warnings === undefined ? [] : Array.from(init.warnings);
        this.demos = [...init.demos || [], ...jsdocLib.extractDemos(init.jsdoc)];
        this.name = init.name || init.className;
        if (this.name) {
            this.identifiers.add(this.name);
        }
        if (init.superClass) {
            this.superClass = init.superClass;
        }
        this.mixins = (init.mixins || []);
        this.constructorMethod = init.constructorMethod;
        const superClassLikes = this._getSuperclassAndMixins(document, init);
        for (const superClassLike of superClassLikes) {
            this.inheritFrom(superClassLike);
        }
        if (init.properties !== undefined) {
            this._overwriteInherited(this.properties, init.properties, undefined, true);
        }
        if (init.methods !== undefined) {
            this._overwriteInherited(this.methods, init.methods, undefined, true);
        }
        if (init.constructorMethod !== undefined) {
            this.constructorMethod = this._overwriteSingleInherited(this.constructorMethod, init.constructorMethod, undefined);
        }
        if (init.staticMethods !== undefined) {
            this._overwriteInherited(this.staticMethods, init.staticMethods, undefined, true);
        }
    }
    /**
     * @deprecated use the `name` field instead.
     */
    get className() {
        return this.name;
    }
    inheritFrom(superClass) {
        this._overwriteInherited(this.staticMethods, superClass.staticMethods, superClass.name);
        this._overwriteInherited(this.properties, superClass.properties, superClass.name);
        this._overwriteInherited(this.methods, superClass.methods, superClass.name);
        this.constructorMethod = this._overwriteSingleInherited(this.constructorMethod, superClass.constructorMethod, superClass.name);
    }
    /**
     * This method is applied to an array of members to overwrite members lower in
     * the prototype graph (closer to Object) with members higher up (closer to
     * the final class we're constructing).
     *
     * @param . existing The array of members so far. N.B. *This param is
     *   mutated.*
     * @param . overriding The array of members from this new, higher prototype in
     *   the graph
     * @param . overridingClassName The name of the prototype whose members are
     *   being applied over the existing ones. Should be `undefined` when
     *   applyingSelf is true
     * @param . applyingSelf True on the last call to this method, when we're
     *   applying the class's own local members.
     */
    _overwriteInherited(existing, overriding, overridingClassName, applyingSelf = false) {
        for (const [key, overridingVal] of overriding) {
            const newVal = Object.assign({}, overridingVal, {
                inheritedFrom: overridingVal['inheritedFrom'] || overridingClassName
            });
            if (existing.has(key)) {
                /**
                 * TODO(rictic): if existingVal.privacy is protected, newVal should be
                 *    protected unless an explicit privacy was specified.
                 *    https://github.com/Polymer/polymer-analyzer/issues/631
                 */
                const existingValue = existing.get(key);
                if (existingValue.privacy === 'private') {
                    let warningSourceRange = this.sourceRange;
                    if (applyingSelf) {
                        warningSourceRange = newVal.sourceRange || this.sourceRange;
                    }
                    this.warnings.push(new model_1.Warning({
                        code: 'overriding-private',
                        message: `Overriding private member '${overridingVal.name}' ` +
                            `inherited from ${existingValue.inheritedFrom || 'parent'}`,
                        sourceRange: warningSourceRange,
                        severity: model_1.Severity.WARNING,
                        parsedDocument: this._parsedDocument,
                    }));
                }
            }
            existing.set(key, newVal);
        }
    }
    /**
     * This method is applied to a single member to overwrite members lower in
     * the prototype graph (closer to Object) with members higher up (closer to
     * the final class we're constructing).
     *
     * @param . existing The existin property on the class
     * @param . overriding The array of members from this new, higher prototype in
     *   the graph
     * @param . overridingClassName The name of the prototype whose members are
     *   being applied over the existing ones. Should be `undefined` when
     *   applyingSelf is true
     * @param . applyingSelf True on the last call to this method, when we're
     *   applying the class's own local members.
     */
    _overwriteSingleInherited(existing, overridingVal, overridingClassName) {
        if (!overridingVal) {
            return existing;
        }
        return Object.assign({}, overridingVal, {
            inheritedFrom: overridingVal['inheritedFrom'] || overridingClassName
        });
    }
    /**
     * Returns the elementLikes that make up this class's prototype chain.
     *
     * Should return them in the order that they're constructed in JS
     * engine (i.e. closest to HTMLElement first, closest to `this` last).
     */
    _getSuperclassAndMixins(document, _init) {
        const mixins = this.mixins.map((m) => this._resolveReferenceToSuperClass(m, document));
        const superClass = this._resolveReferenceToSuperClass(this.superClass, document);
        const prototypeChain = [];
        if (superClass) {
            prototypeChain.push(superClass);
        }
        for (const mixin of mixins) {
            if (mixin) {
                prototypeChain.push(mixin);
            }
        }
        return prototypeChain;
    }
    _resolveReferenceToSuperClass(scannedReference, document) {
        if (!scannedReference || scannedReference.identifier === 'HTMLElement') {
            return undefined;
        }
        const reference = scannedReference.resolve(document);
        if (reference.warnings.length > 0) {
            this.warnings.push(...reference.warnings);
        }
        return reference.feature;
    }
    emitMetadata() {
        return {};
    }
    emitPropertyMetadata(_property) {
        return {};
    }
    emitMethodMetadata(_method) {
        return {};
    }
}
exports.Class = Class;
//# sourceMappingURL=class.js.map