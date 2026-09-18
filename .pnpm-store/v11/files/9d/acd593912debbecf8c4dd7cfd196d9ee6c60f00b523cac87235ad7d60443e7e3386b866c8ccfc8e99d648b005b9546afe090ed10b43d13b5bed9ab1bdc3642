/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
import { ImmutableArray } from '../model/immutable';
import { CssAstNode, Feature, ScannedFeature, SourceRange, Warning } from '../model/model';
import { ParsedCssDocument, Visitor } from './css-document';
import { CssScanner } from './css-scanner';
export declare class CssCustomPropertyScanner implements CssScanner {
    scan(document: ParsedCssDocument, visit: (visitor: Visitor) => Promise<void>): Promise<{
        features: CssCustomPropertyAssignment[];
        warnings: Warning[];
    }>;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'css-custom-property-assignment': CssCustomPropertyAssignment;
    }
}
export declare class CssCustomPropertyAssignment implements ScannedFeature, Feature {
    readonly sourceRange: SourceRange;
    readonly warnings: ImmutableArray<Warning>;
    readonly kinds: Set<string>;
    readonly identifiers: Set<string>;
    readonly name: string;
    readonly astNode: CssAstNode;
    constructor(name: string, sourceRange: SourceRange, astNode: CssAstNode);
    resolve(): this;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'css-custom-property-use': CssCustomPropertyUse;
    }
}
export declare class CssCustomPropertyUse implements ScannedFeature, Feature {
    readonly sourceRange: SourceRange;
    readonly warnings: ImmutableArray<Warning>;
    readonly kinds: Set<string>;
    readonly identifiers: Set<string>;
    readonly name: string;
    readonly astNode: CssAstNode;
    constructor(name: string, sourceRange: SourceRange, astNode: CssAstNode);
    resolve(): this;
}
