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
import { Document } from './document';
import { Feature } from './feature';
import { AstNodeWithLanguage, SourceRange } from './model';
import { Resolvable } from './resolvable';
import { FileRelativeUrl, ResolvedUrl } from './url';
import { Warning } from './warning';
/**
 * Represents an import, such as an HTML import, an external script or style
 * tag, or an JavaScript import.
 *
 * @template N The AST node type
 */
export declare class ScannedImport implements Resolvable {
    readonly type: 'html-import' | 'html-script' | 'html-style' | 'js-import' | string;
    /**
     * URL of the import, relative to the base directory.
     */
    url: FileRelativeUrl | undefined;
    sourceRange: SourceRange | undefined;
    error: {
        message?: string;
    } | undefined;
    /**
     * The source range specifically for the URL or reference to the imported
     * resource.
     */
    urlSourceRange: SourceRange | undefined;
    astNode: AstNodeWithLanguage | undefined;
    warnings: Warning[];
    /**
     * If true, the imported document may not be loaded until well after the
     * containing document has been evaluated, and indeed may never load.
     */
    lazy: boolean;
    constructor(type: string, url: FileRelativeUrl | undefined, sourceRange: SourceRange | undefined, urlSourceRange: SourceRange | undefined, ast: AstNodeWithLanguage | undefined, lazy: boolean);
    resolve(document: Document): Import | undefined;
    protected constructImport(resolvedUrl: ResolvedUrl, relativeUrl: FileRelativeUrl, importedDocument: Document | undefined, _containingDocument: Document): Import;
    protected addCouldNotLoadWarning(document: Document, warning?: Warning): void;
    /**
     * Resolve the URL for this import and return it if the analyzer
     */
    protected getLoadableUrlOrWarn(document: Document): ResolvedUrl | undefined;
}
declare module './queryable' {
    interface FeatureKindMap {
        'import': Import;
        'lazy-import': Import;
        'html-import': Import;
        'html-script': Import;
        'html-style': Import;
        'css-import': Import;
    }
}
export declare class Import implements Feature {
    readonly type: 'html-import' | 'html-script' | 'html-style' | string;
    readonly url: ResolvedUrl;
    readonly originalUrl: FileRelativeUrl;
    readonly document: Document | undefined;
    readonly identifiers: Set<any>;
    readonly kinds: Set<string>;
    readonly sourceRange: SourceRange | undefined;
    readonly urlSourceRange: SourceRange | undefined;
    readonly astNode: AstNodeWithLanguage | undefined;
    readonly warnings: Warning[];
    readonly lazy: boolean;
    constructor(url: ResolvedUrl, originalUrl: FileRelativeUrl, type: string, document: Document | undefined, sourceRange: SourceRange | undefined, urlSourceRange: SourceRange | undefined, ast: AstNodeWithLanguage | undefined, warnings: Warning[], lazy: boolean);
    toString(): string;
}
