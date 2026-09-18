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
import { LiteralValue } from '../model/model';
import * as jsdoc from './jsdoc';
/**
 * Tries to get the value of an expression. Returns undefined on failure.
 */
export declare function expressionToValue(valueExpression: babel.Node): LiteralValue;
/**
 * Extracts the name of the identifier or `.` separated chain of identifiers.
 *
 * Returns undefined if the given node isn't a simple identifier or chain of
 * simple identifiers.
 */
export declare function getIdentifierName(node: babel.Node): string | undefined;
/**
 * Formats the given identifier name under a namespace, if one is mentioned in
 * the commentedNode's comment. Otherwise, name is returned.
 */
export declare function getNamespacedIdentifier(name: string, docs?: jsdoc.Annotation): string;
export declare const CANT_CONVERT = "UNKNOWN";
