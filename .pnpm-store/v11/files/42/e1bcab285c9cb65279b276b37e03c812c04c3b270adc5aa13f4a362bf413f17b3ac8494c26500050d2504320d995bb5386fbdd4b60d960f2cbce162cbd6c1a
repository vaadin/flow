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
import { InlineDocInfo } from '../model/model';
import { ResolvedUrl } from '../model/url';
import { Parser } from '../parser/parser';
import { UrlResolver } from '../url-loader/url-resolver';
import { ParsedTypeScriptDocument } from './typescript-document';
/**
 * A TypeScript parser that only parses a single file, not imported files.
 * This parser is suitable for parsing ES6 as well.
 *
 * This parser uses a TypeScript CompilerHost that resolves all imported
 * modules to null, and resolve the standard library to an empty file.
 * Type checking against the result will be riddled with errors, but the
 * parsed AST can be used to find imports.
 *
 * This parser may eventually be replaced with a lightweight parser that
 * can find import statements, but due to the addition of the import()
 * function, it could be that a full parse is needed anyway.
 */
export declare class TypeScriptPreparser implements Parser<ParsedTypeScriptDocument> {
    parse(contents: string, url: ResolvedUrl, _urlResolver: UrlResolver, inlineInfo?: InlineDocInfo): ParsedTypeScriptDocument;
}
