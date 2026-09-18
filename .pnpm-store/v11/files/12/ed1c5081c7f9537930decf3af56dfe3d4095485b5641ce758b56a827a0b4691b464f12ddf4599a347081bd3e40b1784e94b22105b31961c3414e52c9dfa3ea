/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
import * as analyzer from 'polymer-analyzer';
/**
 * Return whether an Analyzer document is a JavaScript document which was parsed
 * as a module.
 */
export declare function isEsModuleDocument(doc: analyzer.Document): doc is analyzer.Document<analyzer.ParsedJavaScriptDocument>;
/**
 * Resolve an identifier being imported or exported to the feature it refers to.
 */
export declare function resolveImportExportFeature(feature: analyzer.JavascriptImport | analyzer.Export, identifier: string, doc: analyzer.Document): analyzer.Reference<ResolvedTypes> | undefined;
export declare type ResolvedTypes = analyzer.Element | analyzer.PolymerBehavior | analyzer.ElementMixin | analyzer.Class | analyzer.Function | analyzer.Namespace;
