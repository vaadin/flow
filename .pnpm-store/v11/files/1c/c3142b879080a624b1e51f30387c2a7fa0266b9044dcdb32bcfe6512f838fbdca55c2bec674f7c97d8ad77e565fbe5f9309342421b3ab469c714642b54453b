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
import { Analyzer, ResolvedUrl } from 'polymer-analyzer';
export declare type DepsIndex = Map<ResolvedUrl, Set<ResolvedUrl>>;
/**
 * Analyzes all entrypoints and determines each of their transitive
 * dependencies.
 * @param entrypoints Urls of entrypoints to analyze.
 * @param analyzer
 * @return a dependency index of every entrypoint, including entrypoints that
 *     were discovered as lazy entrypoints in the graph.
 */
export declare function buildDepsIndex(entrypoints: ResolvedUrl[], analyzer: Analyzer): Promise<DepsIndex>;
/**
 * Constructs a ResolvedUrl to identify a sub bundle, which is a concatenation
 * of the super bundle or containing file's URL and an id for the sub-bundle.
 */
export declare function getSubBundleUrl(superBundleUrl: ResolvedUrl, id: string): ResolvedUrl;
/**
 * Strips the sub-bundle id off the end of a URL to return the super bundle or
 * containing file's URL.  If there is no sub-bundle id on the provided URL, the
 * result is essentially a NOOP, since nothing will have been stripped.
 *
 * Sub-Bundle URL for an inline ES6 module document:
 *     file:///my-project/src/page.html>inline#1>es6-module
 *
 * Super-bundle URL for the containing HTML document:
 *     file:///my-project/src/page.html
 */
export declare function getSuperBundleUrl(subBundleUrl: string): ResolvedUrl;
