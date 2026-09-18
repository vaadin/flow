/**
 * @license
 * Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
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
import { Analyzer, Document, ResolvedUrl } from 'polymer-analyzer';
import { BundleManifest, BundleStrategy, BundleUrlMapper } from './bundle-manifest';
import { DocumentCollection } from './document-collection';
export * from './bundle-manifest';
export interface Options {
    analyzer?: Analyzer;
    excludes?: ResolvedUrl[];
    inlineCss?: boolean;
    inlineScripts?: boolean;
    rewriteUrlsInTemplates?: boolean;
    sourcemaps?: boolean;
    stripComments?: boolean;
    strategy?: BundleStrategy;
    treeshake?: boolean;
    urlMapper?: BundleUrlMapper;
}
export interface BundleResult {
    documents: DocumentCollection;
    manifest: BundleManifest;
}
export declare class Bundler {
    analyzer: Analyzer;
    enableCssInlining: boolean;
    enableScriptInlining: boolean;
    excludes: ResolvedUrl[];
    rewriteUrlsInTemplates: boolean;
    sourcemaps: boolean;
    stripComments: boolean;
    strategy: BundleStrategy;
    treeshake: boolean;
    urlMapper: BundleUrlMapper;
    private _overlayUrlLoader;
    constructor(options?: Options);
    /**
     * Analyze an HTML URL using the given contents in place of what would
     * otherwise have been loaded.
     */
    analyzeContents(url: ResolvedUrl, contents: string, permanent?: boolean): Promise<Document>;
    /**
     * Given a manifest describing the bundles, produce a collection of bundled
     * documents with HTML imports, external stylesheets and external scripts
     * inlined according to the options for this Bundler.
     *
     * @param manifest - The manifest that describes the bundles to be produced.
     */
    bundle(manifest: BundleManifest): Promise<BundleResult>;
    /**
     * Generates a BundleManifest with all bundles defined, using entrypoints,
     * strategy and mapper.
     *
     * @param entrypoints - The list of entrypoints that will be analyzed for
     *     dependencies. The results of the analysis will be passed to the
     *     `strategy`.
     * @param strategy - The strategy used to construct the output bundles.
     *     See 'polymer-analyzer/src/bundle-manifest'.
     * @param mapper - A function that produces URLs for the generated bundles.
     *     See 'polymer-analyzer/src/bundle-manifest'.
     */
    generateManifest(entrypoints: ResolvedUrl[]): Promise<BundleManifest>;
    /**
     * Given an array of Bundles, remove all files from bundles which are in the
     * "excludes" set.  Remove any bundles which are left empty after excluded
     * files are removed.
     */
    private _filterExcludesFromBundles;
}
