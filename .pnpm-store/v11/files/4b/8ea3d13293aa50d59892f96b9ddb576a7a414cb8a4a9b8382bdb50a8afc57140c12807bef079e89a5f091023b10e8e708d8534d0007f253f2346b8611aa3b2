/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

export function quotePropertyName(name: string): string {
  // TODO We should escape reserved words, and there are many more safe
  // characters than are included in this RegExp.
  // See https://mathiasbynens.be/notes/javascript-identifiers-es6
  const safe = name.match(/^[_$a-zA-Z][_$a-zA-Z0-9]*$/);
  return safe ? name : JSON.stringify(name);
}

const indentSpaces = 2;

export function indent(depth: number): string {
  return ' '.repeat(depth * indentSpaces);
}

export function formatComment(comment: string, depth: number): string {
  // Make sure we don't end our comment early by printing out the `*/` end
  // comment sequence if it is contained in the comment. Escape it as `*\/`
  // instead. One way this sequence could get here is if an HTML comment
  // embedded a JavaScript style block comment.
  comment = comment.replace(/\*\//g, '*\\/');

  // Indent the comment one space so that it doesn't touch the `*` we add next,
  // but only if there is a character there. If we also indented blank lines by
  // one space, then they would have an unneccessary space after the `*`.
  comment = comment.replace(/^(.)/gm, ' $1');

  // Indent to the given level and add the `*` character.
  const i = indent(depth);
  comment = comment.replace(/^/gm, `${i} *`);

  return `${i}/**\n${comment}\n${i} */\n`;
}
