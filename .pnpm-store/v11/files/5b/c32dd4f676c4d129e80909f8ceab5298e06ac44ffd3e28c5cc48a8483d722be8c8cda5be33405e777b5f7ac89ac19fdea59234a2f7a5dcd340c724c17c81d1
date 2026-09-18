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
import { Node } from 'typescript';
import { SourceRange } from '../model/model';
import { ParsedDocument, StringifyOptions } from '../parser/document';
import { Visitor } from './typescript-visitor';
export { Node, Program } from 'typescript';
export { Options } from '../parser/document';
export { Visitor } from './typescript-visitor';
export declare class ParsedTypeScriptDocument extends ParsedDocument<Node, Visitor> {
    readonly type = "typescript";
    visit(visitors: Visitor[]): void;
    protected _sourceRangeForNode(_node: Node): SourceRange | undefined;
    stringify(_options: StringifyOptions): string;
}
