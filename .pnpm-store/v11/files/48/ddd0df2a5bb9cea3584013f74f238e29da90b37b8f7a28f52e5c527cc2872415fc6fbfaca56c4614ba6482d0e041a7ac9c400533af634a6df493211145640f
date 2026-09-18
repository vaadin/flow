"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const dom5 = require("dom5/lib/index-next");
const parse5_1 = require("parse5");
const model_1 = require("../model/model");
const p = dom5.predicates;
const isStyleElement = p.AND(p.hasTagName('style'), p.OR(p.NOT(p.hasAttr('type')), p.hasAttrValue('type', 'text/css')));
const isStyleLink = p.AND(p.hasTagName('link'), p.hasSpaceSeparatedAttrValue('rel', 'stylesheet'), p.hasAttr('href'));
const isStyleNode = p.OR(isStyleElement, isStyleLink);
class HtmlStyleScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const features = [];
            const visitor = (node) => __awaiter(this, void 0, void 0, function* () {
                if (isStyleNode(node)) {
                    const tagName = node.nodeName;
                    if (tagName === 'link') {
                        const href = dom5.getAttribute(node, 'href');
                        features.push(new model_1.ScannedImport('html-style', href, document.sourceRangeForNode(node), document.sourceRangeForAttributeValue(node, 'href'), { language: 'html', node, containingDocument: document }, true));
                    }
                    else {
                        const contents = dom5.getTextContent(node);
                        const locationOffset = model_1.getLocationOffsetOfStartOfTextContent(node, document);
                        const commentText = model_1.getAttachedCommentText(node) || '';
                        features.push(new model_1.ScannedInlineDocument('css', contents, locationOffset, commentText, document.sourceRangeForNode(node), { language: 'html', node, containingDocument: document }));
                    }
                }
                // Descend into templates.
                if (node.tagName === 'template') {
                    const content = parse5_1.treeAdapters.default.getTemplateContent(node);
                    if (content) {
                        for (const n of dom5.depthFirst(content)) {
                            visitor(n);
                        }
                    }
                }
            });
            yield visit(visitor);
            return { features };
        });
    }
}
exports.HtmlStyleScanner = HtmlStyleScanner;
//# sourceMappingURL=html-style-scanner.js.map