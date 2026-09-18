"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const model_1 = require("../model/model");
const ast_value_1 = require("./ast-value");
/**
 * Finds inline HTML documents in Javascript source.
 *
 * e.g.
 *     html`<div></div>`;
 */
class InlineHtmlDocumentScanner {
    scan(document, visit) {
        return __awaiter(this, void 0, void 0, function* () {
            const features = [];
            const myVisitor = {
                enterTaggedTemplateExpression(node) {
                    const tagName = ast_value_1.getIdentifierName(node.tag);
                    if (tagName === undefined || !/(^|\.)html$/.test(tagName)) {
                        return;
                    }
                    const inlineDocument = getInlineDocument(node, document);
                    if (inlineDocument !== undefined) {
                        features.push(inlineDocument);
                    }
                }
            };
            yield visit(myVisitor);
            return { features };
        });
    }
}
exports.InlineHtmlDocumentScanner = InlineHtmlDocumentScanner;
/**
 * Parses the given string as an inline HTML document.
 */
function getInlineDocument(node, parsedDocument, options = {}) {
    const sourceRangeForLiteral = parsedDocument.sourceRangeForNode(node.quasi);
    if (sourceRangeForLiteral === undefined) {
        return;
    }
    const sourceRangeForContents = {
        file: sourceRangeForLiteral.file,
        start: {
            line: sourceRangeForLiteral.start.line,
            column: sourceRangeForLiteral.start.column + 1
        },
        end: {
            line: sourceRangeForLiteral.end.line,
            column: sourceRangeForLiteral.end.column - 1
        }
    };
    let contents = '';
    let previousEnd;
    for (const quasi of node.quasi.quasis) {
        if (previousEnd !== undefined) {
            const fullExpressionTextWithDelimitors = parsedDocument.contents.slice(previousEnd, quasi.start);
            /**
             * Replace everything but whitespace in ${expressions} (including the
             * ${} delimitor part) with whitespace.
             * This skips over the problem of handling expressions, and there's lots
             * of cases it doesn't handle correctly, but it's a start.
             * Consider the js file:
             * ```js
             *   html`<div>${
             *     'Hello world'
             *   }</div>
             * ```
             *
             * If we remove the expression entirely, the html parser receives
             * `<div></div>` and when we ask for the source range of the closing tag
             * it'll give one on the first line, and starting just after the `<div>`.
             * By preserving whitespace and replacing every other character with a
             * space, the HTML parser will receive
             *
             * ```html
             *   <div>
             *     (a bunch of spaces on this line)
             *    </div>
             * ```
             *
             * and so the html parser's source locations will map cleanly onto offsets
             * in the original template literal (excluding characters like `\n`). We
             * could do something more sophisticated later, but this works for most
             * cases and is quick and easy to implement.
             */
            contents += fullExpressionTextWithDelimitors.replace(/\S/g, ' ');
        }
        if (options.useRawContents) {
            contents += quasi.value.raw;
        }
        else {
            contents += quasi.value.cooked;
        }
        previousEnd = quasi.end;
    }
    let commentText;
    if (node.leadingComments != null) {
        commentText = node.leadingComments.map((c) => c.value).join('\n');
    }
    else {
        commentText = '';
    }
    return new model_1.ScannedInlineDocument('html', contents, {
        filename: sourceRangeForContents.file,
        col: sourceRangeForContents.start.column,
        line: sourceRangeForContents.start.line
    }, commentText, sourceRangeForContents, { language: 'js', node, containingDocument: parsedDocument });
}
exports.getInlineDocument = getInlineDocument;
//# sourceMappingURL=html-template-literal-scanner.js.map