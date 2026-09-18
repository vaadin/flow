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
import { NodePath } from '@babel/traverse';
import * as babel from '@babel/types';
import { Document, FileRelativeUrl, Import, JsAstNode, ResolvedUrl, ScannedImport, Warning } from '../model/model';
import { Visitor } from './estree-visitor';
import { JavaScriptDocument } from './javascript-document';
import { JavaScriptScanner } from './javascript-scanner';
import { SourceRange } from '../model/model';
import { DeclaredWithStatement } from '../model/document';
export interface JavaScriptImportScannerOptions {
    /**
     * Algorithm to use for resolving module specifiers in import
     * and export statements when converting them to URLs.
     * A value of 'node' uses Node.js resolution to find modules.
     *
     * If this argument is not given, module specifiers must be web-compatible
     * urls.
     */
    moduleResolution?: 'node';
}
export declare type ImportNode = babel.ImportDeclaration | babel.CallExpression | babel.ExportAllDeclaration | babel.ExportNamedDeclaration;
export declare class ScannedJavascriptImport extends ScannedImport {
    readonly type: 'js-import';
    readonly specifier: string;
    readonly statementAst: babel.Statement | undefined;
    readonly astNode: JsAstNode<ImportNode>;
    readonly astNodePath: NodePath<babel.Node>;
    constructor(url: FileRelativeUrl | undefined, sourceRange: SourceRange | undefined, urlSourceRange: SourceRange | undefined, ast: JsAstNode<ImportNode>, astNodePath: NodePath<babel.Node>, lazy: boolean, originalSpecifier: string, statementAst: babel.Statement | undefined);
    protected constructImport(resolvedUrl: ResolvedUrl, relativeUrl: FileRelativeUrl, importedDocument: Document | undefined, _containingDocument: Document): JavascriptImport;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'js-import': JavascriptImport;
    }
}
export declare class JavascriptImport extends Import implements DeclaredWithStatement {
    /**
     * The original text of the specifier. Unlike `this.url`, this may not
     * be a URL, but may be a bare module specifier, like 'jquery'.
     */
    readonly specifier: string;
    readonly statementAst: babel.Statement | undefined;
    readonly astNode: JsAstNode<ImportNode>;
    readonly astNodePath: NodePath<babel.Node>;
    constructor(url: ResolvedUrl, originalUrl: FileRelativeUrl, type: string, document: Document | undefined, sourceRange: SourceRange | undefined, urlSourceRange: SourceRange | undefined, ast: JsAstNode<ImportNode>, astNodePath: NodePath<babel.Node>, warnings: Warning[], lazy: boolean, specifier: string, statementAst: babel.Statement | undefined);
}
export declare class JavaScriptImportScanner implements JavaScriptScanner {
    moduleResolution?: 'node';
    constructor(options?: JavaScriptImportScannerOptions);
    scan(document: JavaScriptDocument, visit: (visitor: Visitor) => Promise<void>): Promise<{
        features: ScannedJavascriptImport[];
        warnings: Warning[];
    }>;
    private _resolveSpecifier;
}
