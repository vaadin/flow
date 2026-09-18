/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
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
import { Feature, HtmlAstNode, Resolvable, SourceRange, Warning } from '../model/model';
import { Document } from './document';
import { ImmutableArray, ImmutableMap, ImmutableSet } from './immutable';
export interface Attribute {
    readonly name: string;
    readonly sourceRange: SourceRange;
    readonly nameSourceRange: SourceRange;
    readonly valueSourceRange: SourceRange | undefined;
    readonly value?: string;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'element-reference': ElementReference;
    }
}
export declare class ElementReference implements Feature {
    readonly tagName: string;
    readonly attributes: ImmutableMap<string, Attribute>;
    readonly sourceRange: SourceRange;
    readonly astNode: HtmlAstNode;
    readonly warnings: ImmutableArray<Warning>;
    readonly kinds: ImmutableSet<string>;
    constructor(scannedRef: ScannedElementReference, _document: Document);
    readonly identifiers: ImmutableSet<string>;
}
export declare class ScannedElementReference implements Resolvable {
    readonly tagName: string;
    readonly attributes: Map<string, Attribute>;
    readonly sourceRange: SourceRange;
    readonly astNode: HtmlAstNode;
    readonly warnings: ImmutableArray<Warning>;
    constructor(tagName: string, sourceRange: SourceRange, ast: HtmlAstNode);
    resolve(document: Document): ElementReference;
}
