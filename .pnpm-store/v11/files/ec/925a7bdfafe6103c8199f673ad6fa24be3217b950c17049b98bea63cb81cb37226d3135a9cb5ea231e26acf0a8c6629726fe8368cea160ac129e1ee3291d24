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
import * as babel from '@babel/types';
import { MapWithDefault, ScannedClass, ScannedFeature, ScannedMethod, ScannedProperty, Warning } from '../model/model';
import { ScannedPolymerElement } from '../polymer/polymer-element';
import { ScannedPolymerElementMixin } from '../polymer/polymer-element-mixin';
import { Visitor } from './estree-visitor';
import { JavaScriptDocument } from './javascript-document';
import { JavaScriptScanner } from './javascript-scanner';
export interface ScannedAttribute extends ScannedFeature {
    name: string;
    type?: string;
}
declare type ClassType = ScannedPolymerElement | ScannedClass | ScannedPolymerElementMixin;
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
export declare class ClassScanner implements JavaScriptScanner {
    scan(document: JavaScriptDocument, visit: (visitor: Visitor) => Promise<void>): Promise<{
        features: ClassType[];
        warnings: Warning[];
    }>;
    /**
     * Handle the pattern where a class's superclass is declared as a separate
     * variable, usually so that mixins can be applied in a way that is compatible
     * with the Closure compiler. We consider a class ephemeral if:
     *
     * 1) It is the superclass of one or more classes.
     * 2) It is declared using a const, let, or var.
     * 3) It is annotated as @private.
     */
    private collapseEphemeralSuperclasses;
    private _makeElementFeature;
    private _getObservers;
    private _getObservedAttributes;
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
    private _extractAttributesFromObservedAttributes;
}
export declare class PrototypeMemberFinder implements Visitor {
    readonly members: MapWithDefault<string, {
        methods: Map<string, ScannedMethod>;
        properties: Map<string, ScannedProperty>;
    }>;
    private readonly _document;
    constructor(document: JavaScriptDocument);
    enterExpressionStatement(node: babel.ExpressionStatement): void;
    private _createMemberFromAssignment;
    private _addMethodToClass;
    private _addPropertyToClass;
    private _createMemberFromMemberExpression;
    private _createPropertyFromExpression;
    private _createMethodFromExpression;
}
export declare function extractPropertiesFromClass(astNode: babel.Node, document: JavaScriptDocument): Map<string, ScannedProperty>;
export {};
