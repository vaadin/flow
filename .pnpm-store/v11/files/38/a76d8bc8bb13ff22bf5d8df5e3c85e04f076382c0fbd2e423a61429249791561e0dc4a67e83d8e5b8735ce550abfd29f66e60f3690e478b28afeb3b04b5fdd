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
import { JavaScriptDocument } from '../javascript/javascript-document';
import { ScannedPolymerProperty } from './polymer-element';
/**
 * Given a properties block (i.e. object literal), parses and extracts the
 * properties declared therein.
 *
 * @param node The value of the `properties` key in a Polymer 1 declaration, or
 *     the return value of the `properties` static getter in a Polymer 2 class.
 * @param document The containing JS document.
 */
export declare function analyzeProperties(node: babel.Node, document: JavaScriptDocument): ScannedPolymerProperty[];
