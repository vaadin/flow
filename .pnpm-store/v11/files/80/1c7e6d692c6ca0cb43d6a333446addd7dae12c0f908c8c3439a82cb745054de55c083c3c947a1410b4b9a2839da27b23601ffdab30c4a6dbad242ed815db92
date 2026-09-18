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
import { ScannedElementReference } from '../model/element-reference';
import { HtmlVisitor, ParsedHtmlDocument } from './html-document';
import { HtmlScanner } from './html-scanner';
/**
 * Scans for HTML element references/uses in a given document.
 * All elements will be detected, including anything in <head>.
 * This scanner will not be loaded by default, but the custom
 * element extension of it will be.
 */
export declare class HtmlElementReferenceScanner implements HtmlScanner {
    matches(node: ASTNode): boolean;
    scan(document: ParsedHtmlDocument, visit: (visitor: HtmlVisitor) => Promise<void>): Promise<{
        features: ScannedElementReference[];
    }>;
}
/**
 * Scans for custom element references/uses.
 * All custom elements will be detected except <dom-module>.
 * This is a default scanner.
 */
export declare class HtmlCustomElementReferenceScanner extends HtmlElementReferenceScanner {
    matches(node: ASTNode): boolean;
}
