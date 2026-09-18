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
/**
 * This directory exposes our underlying models. The naming scheme is based on
 * the stage of the processing pipeline that an object is produced by.
 *
 * The pipeline looks like:
 *   load: reads in bytes from filesystem/network
 *   parse: convert bytes to abstract syntax trees
 *   scan: extract entirely local features from a single ast
 *   resolve: integrate cross-file information to produce the final result
 *
 * Importantly, we can cache everything before `resolve` at the file level,
 * making incremental analysis efficient.
 *
 * Because the output of the resolve phase is the API that will get the most
 * use, its results have clear, unprefixed names. So a resolved document is just
 * a Document, a resolved element is an Element.
 *
 * Earlier stages have the longer names, like ParsedDocument and ScannedElement.
 */
export { Analysis } from './analysis';
export * from './attribute';
export * from './class';
export { Document, ScannedDocument } from './document';
export { DocumentBackreference } from './document-backreference';
export * from './element';
export * from './element-base';
export * from './element-mixin';
export { ElementReference, ScannedElementReference } from './element-reference';
export * from './event';
export * from './feature';
export * from './import';
export * from './inline-document';
export * from './literal';
export * from './property';
export * from './map';
export * from './method';
export { Queryable, FeatureKindMap, DocumentQuery, AnalysisQuery } from './queryable';
export * from './reference';
export * from './resolvable';
export * from './source-range';
export * from './url';
export * from './warning';
