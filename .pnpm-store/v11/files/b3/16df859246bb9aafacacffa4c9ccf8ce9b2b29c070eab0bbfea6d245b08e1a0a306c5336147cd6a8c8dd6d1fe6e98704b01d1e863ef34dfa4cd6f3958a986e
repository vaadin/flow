"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const dom5_1 = require("dom5");
const dom5 = require("dom5/lib/index-next");
const parse5_1 = require("parse5");
const html_document_1 = require("./html-document");
class HtmlParser {
    /**
     * Parse html into ASTs.
     *
     * @param {string} htmlString an HTML document.
     * @param {string} href is the path of the document.
     */
    parse(contents, url, urlResolver, inlineInfo) {
        const ast = parse5_1.parse(contents, { locationInfo: true });
        // There should be at most one <base> tag and it must be inside <head> tag.
        const baseTag = dom5.query(ast, dom5_1.predicates.AND(dom5_1.predicates.parentMatches(dom5_1.predicates.hasTagName('head')), dom5_1.predicates.hasTagName('base'), dom5_1.predicates.hasAttr('href')));
        const isInline = !!inlineInfo;
        inlineInfo = inlineInfo || {};
        let baseUrl = inlineInfo.baseUrl !== undefined ? inlineInfo.baseUrl : url;
        if (baseTag) {
            const baseTagHref = dom5_1.getAttribute(baseTag, 'href');
            const resolvedBaseTagHref = urlResolver.resolve(url, baseTagHref, undefined);
            if (resolvedBaseTagHref !== undefined) {
                baseUrl = resolvedBaseTagHref;
            }
        }
        return new html_document_1.ParsedHtmlDocument({
            url,
            baseUrl,
            contents,
            ast,
            locationOffset: inlineInfo.locationOffset,
            astNode: inlineInfo.astNode,
            isInline,
        });
    }
}
exports.HtmlParser = HtmlParser;
//# sourceMappingURL=html-parser.js.map