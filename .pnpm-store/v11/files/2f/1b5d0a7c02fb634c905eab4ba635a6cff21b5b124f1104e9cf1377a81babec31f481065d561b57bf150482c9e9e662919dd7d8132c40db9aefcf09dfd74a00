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
import { SourceRange } from '../model/model';
export { Visitor } from '../javascript/estree-visitor';
export interface ScannedAttribute {
    readonly name: string;
    readonly sourceRange: SourceRange | undefined;
    readonly description?: string;
    readonly type?: string;
    readonly changeEvent?: string;
}
export interface Attribute extends ScannedAttribute {
    readonly inheritedFrom?: string;
}
