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
import * as shady from 'shady-css-parser';
import { SourceRange } from '../model/model';
import { ParsedDocument, StringifyOptions } from '../parser/document';
export interface Visitor {
    visit(node: shady.Node): void;
}
export declare class ParsedCssDocument extends ParsedDocument<shady.Node, Visitor> {
    readonly type = "css";
    visit(visitors: Visitor[]): void;
    sourceRangeForNode(node: shady.Node): SourceRange;
    /**
     * Takes a range from a shadycss node directly, rather than a shadycss node.
     * Useful when there are multiple ranges for a given node.
     */
    sourceRangeForShadyRange(range: shady.Range): SourceRange;
    protected _sourceRangeForNode(node: shady.Node): SourceRange;
    stringify(options?: StringifyOptions): string;
    [Symbol.iterator](): Iterator<shady.Node>;
}
