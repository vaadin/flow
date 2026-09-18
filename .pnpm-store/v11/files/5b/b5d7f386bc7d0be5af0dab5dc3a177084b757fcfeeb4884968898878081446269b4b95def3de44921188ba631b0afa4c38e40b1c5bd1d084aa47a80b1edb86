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
import { Analyzer } from '../core/analyzer';
import { ImmutableArray } from '../model/immutable';
import { ScannedFeature, Warning } from '../model/model';
import { ParsedDocument } from '../parser/document';
/**
 * An object that can scan and find "features" in a particular
 * document type.
 *
 * @template D The document type
 * @template A the AST type
 * @template V the visitor type
 */
export interface Scanner<D extends ParsedDocument<A, V>, A, V> {
    scan(document: D, visit: (visitor: V) => Promise<void>): Promise<{
        features: ImmutableArray<ScannedFeature>;
        warnings?: ImmutableArray<Warning>;
    }>;
}
export interface ScannerConstructor {
    new (analyzer: Analyzer): Scanner<ParsedDocument, {} | null | undefined, {}>;
}
