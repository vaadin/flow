/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
import * as dom5 from 'dom5/lib/index-next';
import * as parse5 from 'parse5';
import * as shadyCSS from 'shady-css-parser';
import * as stream from 'stream';
import * as vinyl from 'vinyl';

class NoCommentStringifier extends shadyCSS.Stringifier {
  comment(node: shadyCSS.Comment) {
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
const isInlineStyle = pred.AND(
  pred.hasTagName('style'),
  pred.OR(
    pred.NOT(
      pred.hasAttr('type')
    ),
    pred.hasAttrValue('type', 'text/css')
  )
);

/**
 * Transforms all inline styles in `html` with `filter`
 */
export function html(text: string) {
  const ast = parse5.parse(text);
  const styleNodes = dom5.queryAll(
      ast, isInlineStyle, dom5.childNodesIncludeTemplate);
  for (const styleNode of styleNodes) {
    const text = dom5.getTextContent(styleNode);
    dom5.setTextContent(styleNode, css(text));
  }
  return parse5.serialize(ast);
}

const APPLY_NO_SEMI = /@apply\s*\(?--[\w-]+\)?[^;]/g;

function addSemi(match: string) {
  return match + ';';
}

export function css(text: string) {
  text = text.replace(APPLY_NO_SEMI, addSemi);
  return stringifier.stringify(parser.parse(text));
}

export class GulpTransform extends stream.Transform {
  constructor() {
    super({objectMode: true});
  }
  _transform(file: vinyl, _encoding: string,
             callback: (error?: Error, file?: vinyl) => void) {
    if (file.isStream()) {
      return callback(new Error('css-slam does not support streams'));
    }
    if (file.contents) {
      let contents;
      if (file.path.slice(-5) === '.html') {
        contents = file.contents.toString();
        file.contents = new Buffer(html(contents));
      } else if (file.path.slice(-4) === '.css') {
        contents = file.contents.toString();
        file.contents = new Buffer(css(contents));
      }
    }
    callback(undefined, file);
  }
}

export function gulp() {
  return new GulpTransform();
}
