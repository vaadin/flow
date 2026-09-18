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
import * as babel from '@babel/types';
import { Node } from '@babel/types';
import { SourceRange } from '../model/model';
import { Options as ParsedDocumentOptions, ParsedDocument, StringifyOptions } from '../parser/document';
import { Visitor } from './estree-visitor';
export { Visitor } from './estree-visitor';
export interface Options extends ParsedDocumentOptions<babel.File> {
    parsedAsSourceType: 'script' | 'module';
}
export declare class JavaScriptDocument extends ParsedDocument<Node, Visitor> {
    readonly type = "js";
    private visitorSkips;
    ast: babel.File;
    /**
     * How the js document was parsed. If 'module' then the source code is
     * definitely an ES6 module, as it has imports or exports. If 'script' then
     * it may be an ES6 module with no imports or exports, or it may be a
     * script.
     */
    parsedAsSourceType: 'script' | 'module';
    constructor(from: Options);
    visit(visitors: Visitor[]): void;
    protected _sourceRangeForNode(node: Node): SourceRange | undefined;
    stringify(options?: StringifyOptions): string;
}
