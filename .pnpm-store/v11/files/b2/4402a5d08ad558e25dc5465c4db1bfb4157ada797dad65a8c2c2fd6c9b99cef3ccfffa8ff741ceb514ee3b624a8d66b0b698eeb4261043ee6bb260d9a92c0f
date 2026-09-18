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
import * as dom5 from 'dom5/lib/index-next';
import { ASTNode } from 'parse5';
import * as shady from 'shady-css-parser';
import { ParsedCssDocument } from '..';
import { ParsedHtmlDocument } from '../html/html-document';
import { JavaScriptDocument } from '../javascript/javascript-document';
import { Document, ScannedDocument } from './document';
import { ScannedFeature } from './feature';
import { Resolvable } from './resolvable';
import { LocationOffset, SourceRange } from './source-range';
import { ResolvedUrl } from './url';
import { Warning } from './warning';
export interface InlineDocInfo {
    astNode?: AstNodeWithLanguage;
    locationOffset?: LocationOffset;
    baseUrl?: ResolvedUrl;
}
export interface HtmlAstNode {
    language: 'html';
    node: dom5.Node;
    containingDocument: ParsedHtmlDocument;
}
export interface JsAstNode<N extends babel.Node = babel.Node> {
    language: 'js';
    node: N;
    containingDocument: JavaScriptDocument;
}
export interface CssAstNode {
    language: 'css';
    node: shady.Node;
    containingDocument: ParsedCssDocument;
}
export declare type AstNodeWithLanguage = HtmlAstNode | JsAstNode | CssAstNode;
/**
 * Represents an inline document, usually a <script> or <style> tag in an HTML
 * document.
 *
 * @template N The AST node type
 */
export declare class ScannedInlineDocument implements ScannedFeature, Resolvable {
    readonly type: 'html' | 'js' | 'css' | /* etc */ string;
    readonly contents: string;
    /** The location offset of this document within the containing document. */
    readonly locationOffset: LocationOffset;
    readonly attachedComment?: string;
    scannedDocument?: ScannedDocument;
    readonly sourceRange: SourceRange;
    readonly warnings: Warning[];
    readonly astNode: AstNodeWithLanguage;
    constructor(type: string, contents: string, locationOffset: LocationOffset, attachedComment: string, sourceRange: SourceRange, astNode: AstNodeWithLanguage);
    resolve(document: Document): Document | undefined;
}
declare module './queryable' {
    interface FeatureKindMap {
        'inline-document': InlineDocument;
    }
}
export declare class InlineDocument extends Document {
    constructor(base: ScannedDocument, containerDocument: Document);
}
export declare function getAttachedCommentText(node: ASTNode): string | undefined;
export declare function getLocationOffsetOfStartOfTextContent(node: ASTNode, parsedDocument: ParsedHtmlDocument): LocationOffset;
