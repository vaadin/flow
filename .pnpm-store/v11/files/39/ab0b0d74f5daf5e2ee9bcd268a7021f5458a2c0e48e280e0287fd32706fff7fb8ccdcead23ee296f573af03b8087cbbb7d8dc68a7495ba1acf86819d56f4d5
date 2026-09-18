/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
import { ScannedInlineDocument } from '../model/model';
import { Visitor } from './estree-visitor';
import { JavaScriptDocument } from './javascript-document';
import { JavaScriptScanner } from './javascript-scanner';
/**
 * Finds inline HTML documents in Javascript source.
 *
 * e.g.
 *     html`<div></div>`;
 */
export declare class InlineHtmlDocumentScanner implements JavaScriptScanner {
    scan(document: JavaScriptDocument, visit: (visitor: Visitor) => Promise<void>): Promise<{
        features: ScannedInlineDocument[];
    }>;
}
export interface Options {
    /**
     * If true, uses the "raw" template string contents rather than the "cooked"
     * contents. For example: raw contents yields `\n` as two characters, cooked
     * yields it as a newline.
     */
    useRawContents?: boolean;
}
/**
 * Parses the given string as an inline HTML document.
 */
export declare function getInlineDocument(node: babel.TaggedTemplateExpression, parsedDocument: JavaScriptDocument, options?: Options): ScannedInlineDocument | undefined;
