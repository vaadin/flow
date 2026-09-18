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
import { ScannedFeature, Warning } from '../model/model';
import { ParsedDocument } from '../parser/document';
import { Scanner } from './scanner';
export declare function scan<AstNode, Visitor, PDoc extends ParsedDocument<AstNode, Visitor>>(document: PDoc, scanners: Scanner<PDoc, AstNode, Visitor>[]): Promise<{
    features: ScannedFeature[];
    warnings: Warning[];
}>;
