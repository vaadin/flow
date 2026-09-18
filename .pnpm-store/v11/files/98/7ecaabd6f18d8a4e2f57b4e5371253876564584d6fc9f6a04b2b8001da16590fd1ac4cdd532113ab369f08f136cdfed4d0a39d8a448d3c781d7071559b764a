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
import * as dom5 from 'dom5/lib/index-next';
import { ASTNode } from 'parse5';
import { SourceRange } from '../model/model';
import { ParsedDocument, StringifyOptions } from '../parser/document';
/**
 * The ASTs of the HTML elements needed to represent Polymer elements.
 */
export interface HtmlVisitor {
    (node: ASTNode): void;
}
export declare class ParsedHtmlDocument extends ParsedDocument<ASTNode, HtmlVisitor> {
    type: string;
    visit(visitors: HtmlVisitor[]): void;
    private _sourceRangeForElementWithEndTag;
    protected _sourceRangeForNode(node: ASTNode): SourceRange | undefined;
    sourceRangeForAttribute(node: ASTNode, attrName: string): SourceRange | undefined;
    sourceRangeForAttributeName(node: ASTNode, attrName: string): SourceRange | undefined;
    sourceRangeForAttributeValue(node: ASTNode, attrName: string, excludeQuotes?: boolean): SourceRange | undefined;
    sourceRangeForStartTag(node: ASTNode): SourceRange | undefined;
    sourceRangeForEndTag(node: ASTNode): SourceRange | undefined;
    private _getSourceRangeForLocation;
    private _findClonedContainingNode;
    stringify(options?: StringifyOptions): string;
}
export declare function isFakeNode(ast: dom5.Node): boolean;
