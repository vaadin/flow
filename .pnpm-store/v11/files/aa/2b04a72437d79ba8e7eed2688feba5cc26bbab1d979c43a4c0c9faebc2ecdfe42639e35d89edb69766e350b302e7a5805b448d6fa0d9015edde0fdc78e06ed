/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
import { NodePath } from '@babel/traverse';
import * as babel from '@babel/types';
import { Document, Feature, JsAstNode, SourceRange, Warning } from '../model/model';
import { Resolvable } from '../model/resolvable';
import { Visitor } from './estree-visitor';
import { JavaScriptDocument } from './javascript-document';
import { JavaScriptScanner } from './javascript-scanner';
export declare type ExportNode = babel.ExportNamedDeclaration | babel.ExportAllDeclaration | babel.ExportDefaultDeclaration;
declare module '../model/queryable' {
    interface FeatureKindMap {
        'export': Export;
    }
}
export declare class Export implements Resolvable, Feature {
    readonly kinds: ReadonlySet<string>;
    readonly identifiers: Set<string>;
    readonly description: undefined;
    readonly jsdoc: undefined;
    readonly sourceRange: SourceRange | undefined;
    readonly astNodePath: NodePath<babel.Node>;
    readonly astNode: JsAstNode<ExportNode>;
    readonly statementAst: babel.Statement;
    readonly warnings: ReadonlyArray<Warning>;
    constructor(astNode: JsAstNode<ExportNode>, statementAst: babel.Statement, sourceRange: SourceRange | undefined, nodePath: NodePath<babel.Node>, exportingAllFrom?: Iterable<Export>);
    resolve(document: Document): Feature | undefined;
}
export declare class JavaScriptExportScanner implements JavaScriptScanner {
    scan(document: JavaScriptDocument, visit: (visitor: Visitor) => Promise<void>): Promise<{
        features: Export[];
        warnings: Warning[];
    }>;
}
