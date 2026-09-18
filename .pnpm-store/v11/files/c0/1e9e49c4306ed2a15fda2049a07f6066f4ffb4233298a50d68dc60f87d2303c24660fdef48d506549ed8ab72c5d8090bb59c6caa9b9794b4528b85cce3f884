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
import { ASTNode } from 'parse5';
import * as jsdoc from '../javascript/jsdoc';
import { ParsedDocument } from '../parser/document';
import { Class, ClassInit } from './class';
import { Privacy } from './feature';
import { ImmutableArray } from './immutable';
import { ScannedMethod } from './method';
import { AstNodeWithLanguage, Attribute, Document, Event, Feature, Resolvable, ScannedAttribute, ScannedEvent, ScannedProperty, ScannedReference, SourceRange, Warning } from './model';
import { FileRelativeUrl } from './url';
export { Visitor } from '../javascript/estree-visitor';
/**
 * Base class for ScannedElement and ScannedElementMixin.
 */
export declare abstract class ScannedElementBase implements Resolvable {
    properties: Map<string, ScannedProperty>;
    attributes: Map<string, ScannedAttribute>;
    description: string;
    summary: string;
    demos: Demo[];
    events: Map<string, ScannedEvent>;
    sourceRange: SourceRange | undefined;
    staticMethods: Map<string, ScannedMethod>;
    methods: Map<string, ScannedMethod>;
    astNode: AstNodeWithLanguage | undefined;
    statementAst: babel.Statement | undefined;
    warnings: Warning[];
    jsdoc?: jsdoc.Annotation;
    'slots': Slot[];
    mixins: Array<ScannedReference<'element-mixin'>>;
    privacy: Privacy;
    abstract: boolean;
    superClass?: ScannedReference<'class'>;
    applyHtmlComment(commentText: string | undefined, containingDocument: ParsedDocument | undefined): void;
    abstract resolve(_document: Document): Feature;
}
export declare class Slot {
    name: string;
    range: SourceRange;
    astNode?: AstNodeWithLanguage;
    constructor(name: string, range: SourceRange, astNode: AstNodeWithLanguage | undefined);
}
export interface Demo {
    desc?: string;
    path: FileRelativeUrl;
}
export interface ElementBaseInit extends ClassInit {
    readonly events?: Map<string, Event>;
    readonly attributes?: Map<string, Attribute>;
    readonly slots?: Slot[];
}
/**
 * The element's runtime contents.
 */
export declare type ElementTemplate = {
    /**
     * HTML that is stamped out without data binding or other
     * interpretation beyond normal HTML semantics.
     */
    kind: 'html';
    contents: ASTNode;
} | {
    /**
     * HTML that's interpreted with the polymer databinding
     * system.
     */
    kind: 'polymer-databinding';
    contents: ASTNode;
};
/**
 * Base class for Element and ElementMixin.
 */
export declare abstract class ElementBase extends Class implements Feature {
    attributes: Map<string, Attribute>;
    events: Map<string, Event>;
    'slots': ImmutableArray<Slot>;
    constructor(init: ElementBaseInit, document: Document);
    protected inheritFrom(superClass: Class): void;
    emitAttributeMetadata(_attribute: Attribute): Object;
    emitEventMetadata(_event: Event): Object;
    template: undefined | ElementTemplate;
}
