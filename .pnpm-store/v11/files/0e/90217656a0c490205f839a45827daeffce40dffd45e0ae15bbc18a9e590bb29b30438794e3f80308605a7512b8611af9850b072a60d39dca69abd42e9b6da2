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
import * as babel from '@babel/types';
import { Visitor } from './estree-visitor';
/**
 * These enum options mimic the estraverse enums that are returned by their
 * `enterX`/`leaveX` visitor methods to advise flow of the visitor.
 */
export declare enum VisitorOption {
    Skip = "skip",
    Break = "break",
    Remove = "remove"
}
/**
 * This method mirrors the API of `estraverse`'s `traverse` function.  It uses
 * `babel-traverse` to perform a traversal of an AST, but does so with `noScope:
 * true` which turns off the scoping logic and enables it to traverse from any
 * node; whereasc `babel-traverse`'s scopes require traversal from the root
 * node, the `File` type, which we don't yet even store on our JavaScript
 * documents.
 */
export declare function traverse(ast: babel.Node, visitor: Visitor): void;
