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
import * as babel from 'babel-types';
import { ASTNode } from 'parse5';
import { ResolvedUrl } from 'polymer-analyzer';
export interface BundledBaseDocument<Node> {
    ast: Node;
    content: string;
    files: ResolvedUrl[];
}
export declare type BundledHtmlDocument = {
    language: 'html';
} & BundledBaseDocument<ASTNode>;
export declare type BundledJsDocument = {
    language: 'js';
} & BundledBaseDocument<babel.Node>;
export declare type BundledDocument = BundledHtmlDocument | BundledJsDocument;
export declare class DocumentCollection extends Map<ResolvedUrl, BundledDocument> {
    getHtmlDoc(url: ResolvedUrl): BundledHtmlDocument | undefined;
    getJsDoc(url: ResolvedUrl): BundledJsDocument | undefined;
}
