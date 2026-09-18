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
import * as dom5 from 'dom5/lib/index-next';
import { Annotation as JsDocAnnotation } from '../javascript/jsdoc';
import { ImmutableArray } from '../model/immutable';
import { AstNodeWithLanguage, Class, Document, Element, ElementBase, LiteralValue, Privacy, Property, ScannedAttribute, ScannedElement, ScannedElementBase, ScannedEvent, ScannedMethod, ScannedProperty, SourceRange, Warning } from '../model/model';
import { ScannedReference } from '../model/reference';
import { Behavior } from './behavior';
import { JavascriptDatabindingExpression } from './expression-scanner';
export interface BasePolymerProperty {
    published?: boolean;
    notify?: boolean;
    observer?: string;
    observerNode?: babel.Expression | babel.Pattern;
    observerExpression?: JavascriptDatabindingExpression;
    reflectToAttribute?: boolean;
    computedExpression?: JavascriptDatabindingExpression;
    /**
     * True if the property is part of Polymer's element configuration syntax.
     *
     * e.g. 'properties', 'is', 'extends', etc
     */
    isConfiguration?: boolean;
    /**
     * Constructor used when deserializing this property from an attribute.
     */
    attributeType?: string;
}
export interface ScannedPolymerProperty extends ScannedProperty, BasePolymerProperty {
}
export interface PolymerProperty extends Property, BasePolymerProperty {
}
export declare function mergePropertyDeclarations(propA: Readonly<ScannedPolymerProperty>, propB: Readonly<ScannedPolymerProperty>): ScannedPolymerProperty;
export declare class LocalId {
    name: string;
    range: SourceRange;
    nodeName: string;
    constructor(name: string, range: SourceRange, nodeName: string);
}
export interface Observer {
    javascriptNode: babel.Expression | babel.SpreadElement;
    expression: LiteralValue;
    parsedExpression: JavascriptDatabindingExpression | undefined;
}
export interface Options {
    tagName: string | undefined;
    className: string | undefined;
    superClass: ScannedReference<'class'> | undefined;
    mixins: ScannedReference<'element-mixin'>[];
    extends: string | undefined;
    jsdoc: JsDocAnnotation;
    description: string | undefined;
    properties: ScannedProperty[];
    methods: Map<string, ScannedMethod>;
    staticMethods: Map<string, ScannedMethod>;
    attributes: Map<string, ScannedAttribute>;
    observers: Observer[];
    listeners: {
        event: string;
        handler: string;
    }[];
    behaviors: ScannedReference<'behavior'>[];
    events: Map<string, ScannedEvent>;
    abstract: boolean;
    privacy: Privacy;
    astNode: AstNodeWithLanguage;
    statementAst: babel.Statement | undefined;
    sourceRange: SourceRange | undefined;
    /** true iff element was defined using the legacy Polymer() function. */
    isLegacyFactoryCall: boolean | undefined;
}
export interface ScannedPolymerExtension extends ScannedElementBase {
    properties: Map<string, ScannedPolymerProperty>;
    methods: Map<string, ScannedMethod>;
    observers: Observer[];
    listeners: {
        event: string;
        handler: string;
    }[];
    behaviorAssignments: ScannedReference<'behavior'>[];
    pseudo: boolean;
    addProperty(prop: ScannedPolymerProperty): void;
}
export declare function addProperty(target: ScannedPolymerExtension, prop: ScannedPolymerProperty): void;
export declare function addMethod(target: ScannedPolymerExtension, method: ScannedMethod): void;
export declare function addStaticMethod(target: ScannedPolymerExtension, method: ScannedMethod): void;
/**
 * The metadata for a single polymer element
 */
export declare class ScannedPolymerElement extends ScannedElement implements ScannedPolymerExtension {
    properties: Map<string, ScannedPolymerProperty>;
    methods: Map<string, ScannedMethod>;
    staticMethods: Map<string, ScannedMethod>;
    observers: Observer[];
    listeners: {
        event: string;
        handler: string;
    }[];
    behaviorAssignments: ScannedReference<'behavior'>[];
    pseudo: boolean;
    abstract: boolean;
    isLegacyFactoryCall: boolean;
    constructor(options: Options);
    addProperty(prop: ScannedPolymerProperty): void;
    addMethod(method: ScannedMethod): void;
    addStaticMethod(method: ScannedMethod): void;
    resolve(document: Document): PolymerElement;
}
export interface PolymerExtension extends ElementBase {
    properties: Map<string, PolymerProperty>;
    observers: ImmutableArray<{
        javascriptNode: babel.Expression | babel.SpreadElement;
        expression: LiteralValue;
        parsedExpression: JavascriptDatabindingExpression | undefined;
    }>;
    listeners: ImmutableArray<{
        event: string;
        handler: string;
    }>;
    behaviorAssignments: ImmutableArray<ScannedReference<'behavior'>>;
    localIds: ImmutableArray<LocalId>;
    emitPropertyMetadata(property: PolymerProperty): any;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'polymer-element': PolymerElement;
        'pseudo-element': Element;
    }
}
export declare class PolymerElement extends Element implements PolymerExtension {
    readonly properties: Map<string, PolymerProperty>;
    readonly observers: ImmutableArray<Observer>;
    readonly listeners: ImmutableArray<{
        event: string;
        handler: string;
    }>;
    readonly behaviorAssignments: ImmutableArray<ScannedReference<'behavior'>>;
    readonly domModule?: dom5.Node;
    readonly localIds: ImmutableArray<LocalId>;
    /** true iff element was defined using the legacy Polymer() function. */
    readonly isLegacyFactoryCall: boolean;
    constructor(scannedElement: ScannedPolymerElement, document: Document);
    emitPropertyMetadata(property: PolymerProperty): {
        polymer: {
            notify: boolean | undefined;
            observer: string | undefined;
            readOnly: boolean | undefined;
            attributeType: string | undefined;
        };
    };
    protected _getSuperclassAndMixins(document: Document, init: ScannedPolymerElement): Class[];
}
export declare function getBehaviors(behaviorReferences: Iterable<ScannedReference<'behavior'>>, document: Document): {
    warnings: Warning[];
    behaviors: Behavior[];
};
