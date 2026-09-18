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
import { Node } from './common';
import { NodeVisitor } from './node-visitor';
/**
 * Class that implements basic stringification of an AST produced by the Parser.
 */
declare class Stringifier extends NodeVisitor<Node, string> {
    /**
     * Stringify an AST such as one produced by a Parser.
     * @param ast A node object representing the root of an AST.
     * @return The stringified CSS corresponding to the AST.
     */
    stringify(ast: Node): string;
}
export { Stringifier };
