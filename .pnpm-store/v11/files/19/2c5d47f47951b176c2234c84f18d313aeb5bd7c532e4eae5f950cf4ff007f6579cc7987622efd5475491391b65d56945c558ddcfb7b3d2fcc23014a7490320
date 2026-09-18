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
import { ScannedImport } from '../model/model';
import { PackageRelativeUrl, ResolvedUrl } from '../model/url';
import { HtmlVisitor, ParsedHtmlDocument } from './html-document';
import { HtmlScanner } from './html-scanner';
/**
 * Scans for <link rel="import"> and <link rel="lazy-import">
 */
export declare class HtmlImportScanner implements HtmlScanner {
    private _lazyEdges?;
    constructor(_lazyEdges?: Map<ResolvedUrl, PackageRelativeUrl[]> | undefined);
    scan(document: ParsedHtmlDocument, visit: (visitor: HtmlVisitor) => Promise<void>): Promise<{
        features: ScannedImport[];
    }>;
}
