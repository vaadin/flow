"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
const dom5 = require("dom5/lib/index-next");
const parse5 = require("parse5");
const shadyCSS = require("shady-css-parser");
const stream = require("stream");
class NoCommentStringifier extends shadyCSS.Stringifier {
    comment(node) {
        const value = node.value;
        if (value.indexOf('@license') >= 0) {
            return value;
        }
        return '';
    }
}
const parser = new shadyCSS.Parser();
const stringifier = new NoCommentStringifier();
const pred = dom5.predicates;
const isInlineStyle = pred.AND(pred.hasTagName('style'), pred.OR(pred.NOT(pred.hasAttr('type')), pred.hasAttrValue('type', 'text/css')));
/**
 * Transforms all inline styles in `html` with `filter`
 */
function html(text) {
    const ast = parse5.parse(text);
    const styleNodes = dom5.queryAll(ast, isInlineStyle, dom5.childNodesIncludeTemplate);
    for (const styleNode of styleNodes) {
        const text = dom5.getTextContent(styleNode);
        dom5.setTextContent(styleNode, css(text));
    }
    return parse5.serialize(ast);
}
exports.html = html;
const APPLY_NO_SEMI = /@apply\s*\(?--[\w-]+\)?[^;]/g;
function addSemi(match) {
    return match + ';';
}
function css(text) {
    text = text.replace(APPLY_NO_SEMI, addSemi);
    return stringifier.stringify(parser.parse(text));
}
exports.css = css;
class GulpTransform extends stream.Transform {
    constructor() {
        super({ objectMode: true });
    }
    _transform(file, _encoding, callback) {
        if (file.isStream()) {
            return callback(new Error('css-slam does not support streams'));
        }
        if (file.contents) {
            let contents;
            if (file.path.slice(-5) === '.html') {
                contents = file.contents.toString();
                file.contents = new Buffer(html(contents));
            }
            else if (file.path.slice(-4) === '.css') {
                contents = file.contents.toString();
                file.contents = new Buffer(css(contents));
            }
        }
        callback(undefined, file);
    }
}
exports.GulpTransform = GulpTransform;
function gulp() {
    return new GulpTransform();
}
exports.gulp = gulp;
//# sourceMappingURL=index.js.map