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
import { NodePath } from '@babel/traverse';
import * as babel from '@babel/types';
import * as doctrine from 'doctrine';
import { Annotation, Tag } from 'doctrine';
import { Demo } from '../index';
import { Privacy } from '../model/model';
import { ScannedReference, Warning } from '../model/model';
import { JavaScriptDocument } from './javascript-document';
export { Annotation, Tag } from 'doctrine';
/**
 * Given a JSDoc string (minus opening/closing comment delimiters), extract its
 * description and tags.
 */
export declare function parseJsdoc(docs: string): doctrine.Annotation;
/**
 * removes leading *, and any space before it
 */
export declare function removeLeadingAsterisks(description: string): string;
export declare function hasTag(jsdoc: Annotation | undefined, title: string): boolean;
/**
 * Finds the first JSDoc tag matching `title`.
 */
export declare function getTag(jsdoc: Annotation | undefined, title: string): Tag | undefined;
export declare function unindent(text: string): string;
export declare function isAnnotationEmpty(docs: Annotation | undefined): boolean;
export declare function getPrivacy(jsdoc: Annotation | undefined): Privacy | undefined;
/**
 * Returns the mixin applications, in the form of ScannedReferences, for the
 * jsdocs of class.
 *
 * The references are returned in presumed order of application - from furthest
 * up the prototype chain to closest to the subclass.
 */
export declare function getMixinApplications(document: JavaScriptDocument, node: babel.Node, docs: Annotation, warnings: Warning[], path: NodePath): Array<ScannedReference<'element-mixin'>>;
export declare function extractDemos(jsdoc: Annotation | undefined): Demo[];
export declare function join(...jsdocs: Array<Annotation | undefined>): Annotation;
/**
 * Assume that if the same symbol is documented in multiple places, the longer
 * description is probably the intended one.
 *
 * TODO(rictic): unify logic with join(...)'s above.
 */
export declare function pickBestDescription(...descriptions: Array<string | undefined>): string;
/**
 * Extracts the description from a jsdoc annotation and uses
 * known descriptive tags if no explicit description is set.
 */
export declare function getDescription(jsdocAnn: Annotation): string | undefined;
