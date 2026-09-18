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
Object.defineProperty(exports, "__esModule", { value: true });
const dom5 = require("dom5/lib/index-next");
const util = require("util");
const html_document_1 = require("../html/html-document");
const jsdoc = require("../javascript/jsdoc");
const document_1 = require("./document");
const document_backreference_1 = require("./document-backreference");
const immutable_1 = require("./immutable");
/**
 * Represents an inline document, usually a <script> or <style> tag in an HTML
 * document.
 *
 * @template N The AST node type
 */
class ScannedInlineDocument {
    constructor(type, contents, locationOffset, attachedComment, sourceRange, astNode) {
        this.warnings = [];
        this.type = type;
        this.contents = contents;
        this.locationOffset = locationOffset;
        this.attachedComment = attachedComment;
        this.sourceRange = sourceRange;
        this.astNode = astNode;
    }
    resolve(document) {
        if (!this.scannedDocument) {
            // Parse error on the inline document.
            return;
        }
        const inlineDocument = new InlineDocument(this.scannedDocument, document);
        inlineDocument.resolve();
        return inlineDocument;
    }
}
exports.ScannedInlineDocument = ScannedInlineDocument;
class InlineDocument extends document_1.Document {
    constructor(base, containerDocument) {
        super(base, containerDocument._analysisContext);
        immutable_1.unsafeAsMutable(this.kinds).add('inline-document');
        this._addFeature(new document_backreference_1.DocumentBackreference(containerDocument));
    }
}
exports.InlineDocument = InlineDocument;
function getAttachedCommentText(node) {
    const commentNode = getAttachedCommentNode(node);
    if (!commentNode) {
        return;
    }
    const comment = dom5.getTextContent(commentNode);
    if (!comment || /@license/.test(comment)) {
        return;
    }
    return jsdoc.unindent(comment).trim();
}
exports.getAttachedCommentText = getAttachedCommentText;
function getAttachedCommentNode(node) {
    const predecessors = getPreviousSiblings(node);
    const visiblePredecessors = filterOutInvisibleNodes(predecessors);
    const [closestVisiblePredecessor] = visiblePredecessors;
    if (!closestVisiblePredecessor) {
        return; // no predecessors at all
    }
    if (!dom5.isCommentNode(closestVisiblePredecessor)) {
        return; // attached node isn't a comment
    }
    return closestVisiblePredecessor;
}
/**
 * Filter out nodes that are just whitespace, or aren't present in the original
 * source text of the file.
 */
function* filterOutInvisibleNodes(nodeIter) {
    for (const node of nodeIter) {
        // Ignore nodes that aren't present in the original text of the file,
        // like parser-hallucinated <head> and <body> nodes.
        if (html_document_1.isFakeNode(node)) {
            continue;
        }
        // Ignore text nodes that are just whitespace
        if (dom5.isTextNode(node)) {
            const text = dom5.getTextContent(node).trim();
            if (text === '') {
                continue;
            }
        }
        yield node;
    }
}
/**
 * An iterable over siblings that come before the given node.
 *
 * Note that this method gives siblings from the author's point of view, not
 * the pedantic parser's point of view, so we need to traverse some fake
 * nodes. e.g. consider this document
 *
 *     <link rel="import" href="foo.html">
 *     <dom-module></dom-module>
 *
 * For this method's purposes, these nodes are siblings, but in the AST
 * they're actually cousins! The link is in a hallucinated <head> node, and
 * the dom-module is in a hallucinated <body> node, so to get to the link we
 * need to go up to the body, then back to the head, then back down, but
 * only if the head and body are hallucinated.
 */
function* getPreviousSiblings(node) {
    const parent = node.parentNode;
    if (parent) {
        const siblings = parent.childNodes;
        for (let i = siblings.indexOf(node) - 1; i >= 0; i--) {
            const predecessor = siblings[i];
            if (html_document_1.isFakeNode(predecessor)) {
                if (predecessor.childNodes) {
                    yield* iterReverse(predecessor.childNodes);
                }
            }
            else {
                yield predecessor;
            }
        }
        if (html_document_1.isFakeNode(parent)) {
            yield* getPreviousSiblings(parent);
        }
    }
}
function* iterReverse(arr) {
    for (let i = arr.length - 1; i >= 0; i--) {
        yield arr[i];
    }
}
function getLocationOffsetOfStartOfTextContent(node, parsedDocument) {
    const sourceRange = parsedDocument.sourceRangeForStartTag(node);
    if (!sourceRange) {
        throw new Error(`Couldn't extract a location offset from HTML node: ` +
            `${util.inspect(node)}`);
    }
    return { line: sourceRange.end.line, col: sourceRange.end.column };
}
exports.getLocationOffsetOfStartOfTextContent = getLocationOffsetOfStartOfTextContent;
//# sourceMappingURL=inline-document.js.map