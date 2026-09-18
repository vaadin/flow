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
import { InlineDocInfo, LocationOffset, Severity, SourceRange } from '../model/model';
import { ResolvedUrl } from '../model/url';
import { Parser } from '../parser/parser';
import { UrlResolver } from '../url-loader/url-resolver';
import { JavaScriptDocument } from './javascript-document';
export declare type SourceType = 'script' | 'module';
export declare class JavaScriptParser implements Parser<JavaScriptDocument> {
    readonly sourceType?: SourceType;
    parse(contents: string, url: ResolvedUrl, _urlResolver: UrlResolver, inlineInfo?: InlineDocInfo): JavaScriptDocument;
}
export declare class JavaScriptModuleParser extends JavaScriptParser {
    readonly sourceType: SourceType;
}
export declare class JavaScriptScriptParser extends JavaScriptParser {
    readonly sourceType: SourceType;
}
export declare type ParseResult = {
    type: 'success';
    sourceType: SourceType;
    parsedFile: babel.File;
} | {
    type: 'failure';
    warningish: {
        sourceRange: SourceRange;
        severity: Severity;
        code: string;
        message: string;
    };
};
/**
 * Parse the given contents and return either an AST or a parse error as a
 * Warning. It needs the filename and the location offset to produce correct
 * warnings.
 */
export declare function parseJs(contents: string, file: ResolvedUrl, locationOffset?: LocationOffset, warningCode?: string, sourceType?: SourceType): ParseResult;
