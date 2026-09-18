/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */

import * as util from 'util';

import {Node, nodeType} from './common';

export function* iterateOverAst(node: Node): Iterable<Node> {
  yield node;
  switch (node.type) {
    case nodeType.stylesheet:
      for (const rule of node.rules) {
        yield* iterateOverAst(rule);
      }
      return;
    case nodeType.ruleset:
      return yield* iterateOverAst(node.rulelist);
    case nodeType.rulelist:
      for (const rule of node.rules) {
        yield* iterateOverAst(rule);
      }
      return;
    case nodeType.declaration:
      if (node.value !== undefined) {
        yield* iterateOverAst(node.value);
      }
      return;
    case nodeType.atRule:
      if (node.rulelist) {
        yield* iterateOverAst(node.rulelist);
      }
      return;
    case nodeType.expression:
    case nodeType.comment:
    case nodeType.discarded:
      return;  // no child nodes
    default:
      const never: never = node;
      console.error(`Got a node of unknown type: ${util.inspect(never)}`);
  }
}
