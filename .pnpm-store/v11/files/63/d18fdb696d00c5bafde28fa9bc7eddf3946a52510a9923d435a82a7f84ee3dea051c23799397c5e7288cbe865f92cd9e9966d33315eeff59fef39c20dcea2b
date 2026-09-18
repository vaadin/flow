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
import { HtmlVisitor, ParsedHtmlDocument } from '../html/html-document';
import { HtmlScanner } from '../html/html-scanner';
import { ScannedPolymerElement } from './polymer-element';
/**
 * A Polymer pseudo-element is an element that is declared in an unusual way,
 * such
 * that the analyzer couldn't normally analyze it, so instead it is declared in
 * comments.
 */
export declare class PseudoElementScanner implements HtmlScanner {
    scan(document: ParsedHtmlDocument, visit: (visitor: HtmlVisitor) => Promise<void>): Promise<{
        features: ScannedPolymerElement[];
    }>;
}
