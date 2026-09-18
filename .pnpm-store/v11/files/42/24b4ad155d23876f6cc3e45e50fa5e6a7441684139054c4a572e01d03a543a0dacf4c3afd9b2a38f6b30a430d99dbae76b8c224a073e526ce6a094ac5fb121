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
import { AstNodeWithLanguage, Document, FileRelativeUrl, Import, ResolvedUrl, ScannedImport, SourceRange, Warning } from '../model/model';
/**
 * <script> tags are represented in two different ways: as inline documents,
 * or as imports, depending on whether the tag has a `src` attribute. This class
 * represents a script tag with a `src` attribute as an import, so that the
 * analyzer loads and parses the referenced document.
 */
export declare class ScriptTagImport extends Import {
    readonly type = "html-script";
    readonly isModule: boolean;
    constructor(url: ResolvedUrl, originalUrl: FileRelativeUrl, type: string, document: Document | undefined, sourceRange: SourceRange | undefined, urlSourceRange: SourceRange | undefined, ast: AstNodeWithLanguage | undefined, warnings: Warning[], lazy: boolean, isModule: boolean);
}
export declare class ScannedScriptTagImport extends ScannedImport {
    readonly isModule: boolean;
    constructor(url: FileRelativeUrl, sourceRange: SourceRange, urlSourceRange: SourceRange, ast: AstNodeWithLanguage, isModule: boolean);
    resolve(document: Document): Import | undefined;
    protected constructImport(resolvedUrl: ResolvedUrl, relativeUrl: FileRelativeUrl, importedDocument: Document | undefined, _containingDocument: Document): ScriptTagImport;
}
