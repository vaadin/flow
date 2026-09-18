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
/// <reference path="../../custom_typings/main.d.ts" />
import { ParsedDocument } from '..';
import { Analysis } from '../model/model';
import { PackageRelativeUrl, ResolvedUrl } from '../model/url';
import { Parser } from '../parser/parser';
import { Scanner } from '../scanning/scanner';
import { UrlLoader } from '../url-loader/url-loader';
import { UrlResolver } from '../url-loader/url-resolver';
import { AnalysisContext } from './analysis-context';
import { MinimalCancelToken } from './cancel-token';
export interface Options {
    urlLoader: UrlLoader;
    urlResolver?: UrlResolver;
    parsers?: Map<string, Parser<ParsedDocument>>;
    scanners?: ScannerTable;
    /**
     * Map from url of an HTML Document to another HTML document it lazily depends
     * on.
     */
    lazyEdges?: LazyEdgeMap;
    /**
     * Algorithm to use for resolving module specifiers in import
     * and export statements when rewriting them to be web-compatible.
     * A value of 'node' uses Node.js resolution to find modules.
     */
    moduleResolution?: 'node';
    __contextPromise?: Promise<AnalysisContext>;
}
export interface AnalyzeOptions {
    /**
     * Used to indicate that the caller no longer cares about the result
     * of the analysis, to save on effort.
     */
    readonly cancelToken?: MinimalCancelToken;
}
/**
 * These are the options available to the `_fork` method.  Currently, only the
 * `urlLoader` override is implemented.
 */
export interface ForkOptions {
    urlLoader?: UrlLoader;
}
export declare class NoKnownParserError extends Error {
}
export declare type ScannerTable = Map<string, Scanner<ParsedDocument, {} | null | undefined, {}>[]>;
export declare type LazyEdgeMap = Map<ResolvedUrl, PackageRelativeUrl[]>;
/**
 * A static analyzer for web projects.
 *
 * An Analyzer can load and parse documents of various types, and extract
 * arbitrary information from the documents, and transitively load
 * dependencies. An Analyzer instance is configured with parsers, and scanners
 * which do the actual work of understanding different file types.
 */
export declare class Analyzer {
    private _analysisComplete;
    readonly urlResolver: UrlResolver;
    private readonly _urlLoader;
    constructor(options: Options);
    /**
     * Loads, parses and analyzes the root document of a dependency graph and its
     * transitive dependencies.
     */
    analyze(urls: string[], options?: AnalyzeOptions): Promise<Analysis>;
    analyzePackage(options?: AnalyzeOptions): Promise<Analysis>;
    private _filterFilesByParsableExtension;
    private _constructAnalysis;
    /**
     * Clears all information about the given files from our caches, such that
     * future calls to analyze() will reload these files if they're needed.
     *
     * The analyzer assumes that if this method isn't called with a file's url,
     * then that file has not changed and does not need to be reloaded.
     *
     * @param urls The urls of files which may have changed.
     */
    filesChanged(urls: string[]): Promise<void>;
    /**
     * Clear all cached information from this analyzer instance.
     *
     * Note: if at all possible, instead tell the analyzer about the specific
     * files that changed rather than clearing caches like this. Caching provides
     * large performance gains.
     */
    clearCaches(): Promise<void>;
    /**
     * Returns a copy of the analyzer.  If options are given, the AnalysisContext
     * is also forked and individual properties are overridden by the options.
     * is forked with the given options.
     *
     * When the analysis context is forked, its cache is preserved, so you will
     * see a mixture of pre-fork and post-fork contents when you analyze with a
     * forked analyzer.
     *
     * Note: this feature is experimental. It may be removed without being
     *     considered a breaking change, so check for its existence before calling
     *     it.
     */
    _fork(options?: ForkOptions): Analyzer;
    /**
     * Returns `true` if the provided resolved URL can be loaded.  Obeys the
     * semantics defined by `UrlLoader` and should only be used to check
     * resolved URLs.
     */
    canLoad(resolvedUrl: ResolvedUrl): boolean;
    /**
     * Loads the content at the provided resolved URL.  Obeys the semantics
     * defined by `UrlLoader` and should only be used to attempt to load resolved
     * URLs.
     */
    load(resolvedUrl: ResolvedUrl): Promise<string>;
    /**
     * Resoves `url` to a new location.
     */
    resolveUrl(url: string): ResolvedUrl | undefined;
    private brandUserInputUrls;
}
