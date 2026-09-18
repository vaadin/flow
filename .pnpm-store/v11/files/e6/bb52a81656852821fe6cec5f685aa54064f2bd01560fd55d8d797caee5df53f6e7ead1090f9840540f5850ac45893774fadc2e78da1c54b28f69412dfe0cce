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
import { Annotation as JsDocAnnotation } from '../javascript/jsdoc';
import { AstNodeWithLanguage, Class, Document, ElementMixin, Privacy, ScannedElementMixin, ScannedMethod, ScannedReference, SourceRange } from '../model/model';
import { LocalId, Observer, PolymerExtension, PolymerProperty, ScannedPolymerExtension, ScannedPolymerProperty } from './polymer-element';
export interface Options {
    name: string;
    jsdoc: JsDocAnnotation;
    description: string;
    summary: string;
    privacy: Privacy;
    sourceRange: SourceRange;
    mixins: ScannedReference<'element-mixin'>[];
    astNode: AstNodeWithLanguage;
    statementAst: babel.Statement | undefined;
    classAstNode?: babel.Node;
}
export declare class ScannedPolymerElementMixin extends ScannedElementMixin implements ScannedPolymerExtension {
    readonly properties: Map<string, ScannedPolymerProperty>;
    readonly methods: Map<string, ScannedMethod>;
    readonly staticMethods: Map<string, ScannedMethod>;
    readonly observers: Observer[];
    readonly listeners: {
        event: string;
        handler: string;
    }[];
    readonly behaviorAssignments: ScannedReference<'behavior'>[];
    pseudo: boolean;
    readonly abstract: boolean;
    readonly sourceRange: SourceRange;
    classAstNode?: babel.Node;
    constructor({ name, jsdoc, description, summary, privacy, sourceRange, mixins, astNode, statementAst, classAstNode }: Options);
    addProperty(prop: ScannedPolymerProperty): void;
    addMethod(method: ScannedMethod): void;
    resolve(document: Document): PolymerElementMixin;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'polymer-element-mixin': PolymerElementMixin;
    }
}
export declare class PolymerElementMixin extends ElementMixin implements PolymerExtension {
    readonly properties: Map<string, PolymerProperty>;
    readonly observers: Observer[];
    readonly listeners: {
        event: string;
        handler: string;
    }[];
    readonly behaviorAssignments: ScannedReference<'behavior'>[];
    readonly localIds: LocalId[];
    readonly pseudo: boolean;
    constructor(scannedMixin: ScannedPolymerElementMixin, document: Document);
    emitPropertyMetadata(property: PolymerProperty): {
        polymer: {
            notify: boolean | undefined;
            observer: string | undefined;
            readOnly: boolean | undefined;
            attributeType: string | undefined;
        };
    };
    protected _getSuperclassAndMixins(document: Document, init: ScannedPolymerElementMixin): Class[];
}
