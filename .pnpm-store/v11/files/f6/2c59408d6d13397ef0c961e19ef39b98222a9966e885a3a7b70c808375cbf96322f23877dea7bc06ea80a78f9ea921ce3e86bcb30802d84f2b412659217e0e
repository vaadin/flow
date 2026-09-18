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
import { NodePath } from '@babel/traverse';
import { AstNodeWithLanguage } from '..';
import { Annotation } from '../javascript/jsdoc';
import { Document } from './document';
import { Feature, ScannedFeature } from './feature';
import { FeatureKindMap } from './queryable';
import { Resolvable } from './resolvable';
import { SourceRange } from './source-range';
import { Warning } from './warning';
/**
 * A reference to another feature by identifier.
 */
export declare class ScannedReference<K extends keyof FeatureKindMap> extends ScannedFeature implements Resolvable {
    readonly identifier: string;
    readonly kind: K;
    readonly sourceRange: SourceRange | undefined;
    readonly astPath: NodePath;
    readonly astNode: AstNodeWithLanguage | undefined;
    constructor(kind: K, identifier: string, sourceRange: SourceRange | undefined, astNode: AstNodeWithLanguage | undefined, astPath: NodePath, description?: string, jsdoc?: Annotation, warnings?: Warning[]);
    resolve(document: Document): Reference<FeatureKindMap[K]>;
    resolveWithKind<DK extends keyof FeatureKindMap>(document: Document, kind: DK): Reference<FeatureKindMap[DK]>;
}
declare module './queryable' {
    interface FeatureKindMap {
        'reference': Reference<Feature>;
    }
}
/**
 * A reference to another feature by identifier.
 */
export declare class Reference<F extends Feature> implements Feature {
    readonly kinds: ReadonlySet<"reference">;
    readonly identifiers: ReadonlySet<string>;
    readonly identifier: string;
    readonly sourceRange: SourceRange | undefined;
    readonly astNode: AstNodeWithLanguage | undefined;
    readonly feature: F | undefined;
    readonly warnings: ReadonlyArray<Warning>;
    constructor(identifier: string, sourceRange: SourceRange | undefined, astNode: AstNodeWithLanguage | undefined, feature: F | undefined, warnings: ReadonlyArray<Warning>);
}
