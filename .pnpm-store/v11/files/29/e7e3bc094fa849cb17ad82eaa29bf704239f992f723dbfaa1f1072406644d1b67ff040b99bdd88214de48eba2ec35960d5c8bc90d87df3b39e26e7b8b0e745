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
import { ASTNode } from 'parse5';
import { HtmlVisitor, ParsedHtmlDocument } from '../html/html-document';
import { HtmlScanner } from '../html/html-scanner';
import { Feature, HtmlAstNode, Resolvable, Slot, SourceRange, Warning } from '../model/model';
import { HtmlDatabindingExpression, Template } from './expression-scanner';
import { LocalId } from './polymer-element';
export declare class ScannedDomModule implements Resolvable {
    id: string | null;
    node: ASTNode;
    comment?: string;
    sourceRange: SourceRange;
    astNode: HtmlAstNode;
    warnings: Warning[];
    'slots': Slot[];
    localIds: LocalId[];
    template: Template | undefined;
    databindings: HtmlDatabindingExpression[];
    constructor(id: string | null, node: ASTNode, sourceRange: SourceRange, ast: HtmlAstNode, warnings: Warning[], template: Template | undefined, slots: Slot[], localIds: LocalId[], databindings: HtmlDatabindingExpression[]);
    resolve(): DomModule;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'dom-module': DomModule;
    }
}
export declare class DomModule implements Feature {
    kinds: Set<string>;
    identifiers: Set<string>;
    node: ASTNode;
    id: string | null;
    comment?: string;
    sourceRange: SourceRange;
    astNode: HtmlAstNode;
    warnings: Warning[];
    'slots': Slot[];
    localIds: LocalId[];
    template: Template | undefined;
    databindings: HtmlDatabindingExpression[];
    constructor(node: ASTNode, id: string | null, comment: string | undefined, sourceRange: SourceRange, ast: HtmlAstNode, warnings: Warning[], slots: Slot[], localIds: LocalId[], template: Template | undefined, databindings: HtmlDatabindingExpression[]);
}
export declare class DomModuleScanner implements HtmlScanner {
    scan(document: ParsedHtmlDocument, visit: (visitor: HtmlVisitor) => Promise<void>): Promise<{
        features: ScannedDomModule[];
    }>;
}
