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
import { ForkOptions, Options } from '../core/analyzer';
import { Result } from '../model/analysis';
import { Document, ScannedDocument, Warning } from '../model/model';
import { PackageRelativeUrl, ResolvedUrl } from '../model/url';
import { ParsedDocument } from '../parser/document';
import { Parser } from '../parser/parser';
import { Scanner } from '../scanning/scanner';
import { UrlLoader } from '../url-loader/url-loader';
import { UrlResolver } from '../url-loader/url-resolver';
import { AnalysisCache } from './analysis-cache';
import { MinimalCancelToken } from './cancel-token';
export declare const analyzerVersion: string;
/**
 * An analysis of a set of files at a specific point-in-time with respect to
 * updates to those files. New files can be added to an existing context, but
 * updates to files will cause a fork of the context with new analysis results.
 *
 * All file contents and analysis results are consistent within a single
 * anaysis context. A context is forked via either the fileChanged or
 * clearCaches methods.
 *
 * For almost all purposes this is an entirely internal implementation detail.
 * An Analyzer instance has a reference to its current context, so it will
 * appear to be statefull with respect to file updates.
 */
export declare class AnalysisContext {
    readonly parsers: Map<string, Parser<ParsedDocument<{} | null | undefined, {}>>>;
    private readonly _languageAnalyzers;
    /** A map from import url to urls that document lazily depends on. */
    private readonly _lazyEdges;
    private readonly _scanners;
    readonly loader: UrlLoader;
    readonly resolver: UrlResolver;
    private readonly _cache;
    /** Incremented each time we fork. Useful for debugging. */
    private readonly _generation;
    /**
     * Resolves when the previous analysis has completed.
     *
     * Used to serialize analysis requests, not for correctness surprisingly
     * enough, but for performance, so that we can reuse AnalysisResults.
     */
    private _analysisComplete;
    static getDefaultScanners(options: Options): Map<string, Scanner<ParsedDocument<{} | null | undefined, {}>, {} | null | undefined, {}>[]>;
    constructor(options: Options, cache?: AnalysisCache, generation?: number);
    /**
     * Returns a copy of this cache context with proper cache invalidation.
     */
    filesChanged(urls: PackageRelativeUrl[]): AnalysisContext;
    /**
     * Implements Analyzer#analyze, see its docs.
     */
    analyze(urls: PackageRelativeUrl[], cancelToken: MinimalCancelToken): Promise<AnalysisContext>;
    /**
     * Internal analysis method called when we know we need to fork.
     */
    private _analyze;
    /**
     * Gets an analyzed Document from the document cache. This is only useful for
     * Analyzer plugins. You almost certainly want to use `analyze()` instead.
     *
     * If a document has been analyzed, it returns the analyzed Document. If not
     * the scanned document cache is used and a new analyzed Document is returned.
     * If a file is in neither cache, it returns `undefined`.
     */
    getDocument(resolvedUrl: ResolvedUrl): Document | Warning;
    /**
     * This is only useful for Analyzer plugins.
     *
     * If a url has been scanned, returns the ScannedDocument.
     */
    _getScannedDocument(resolvedUrl: ResolvedUrl): ScannedDocument | undefined;
    /**
     * Clear all cached information from this analyzer instance.
     *
     * Note: if at all possible, instead tell the analyzer about the specific
     * files that changed rather than clearing caches like this. Caching provides
     * large performance gains.
     */
    clearCaches(): AnalysisContext;
    /**
     * Returns a copy of the context but with optional replacements of cache or
     * constructor options.
     *
     * Note: this feature is experimental.
     */
    _fork(cache?: AnalysisCache, options?: ForkOptions): AnalysisContext;
    /**
     * Scans a file locally, that is for features that do not depend
     * on this files imports. Local features can be cached even when
     * imports are invalidated. This method does not trigger transitive
     * scanning, _scan() does that.
     *
     * TODO(justinfagnani): consider renaming this to something like
     * _preScan, since about the only useful things it can find are
     * imports, exports and other syntactic structures.
     */
    private _scanLocal;
    /**
     * Scan a toplevel document and all of its transitive dependencies.
     */
    scan(resolvedUrl: ResolvedUrl, cancelToken: MinimalCancelToken): Promise<Result<ScannedDocument, Warning>>;
    /**
     * Scans a ParsedDocument.
     */
    private _scanDocument;
    private _getScannedFeatures;
    private _scanInlineDocuments;
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
     *
     * Currently does no caching. If the provided contents are given then they
     * are used instead of hitting the UrlLoader (e.g. when you have in-memory
     * contents that should override disk).
     */
    load(resolvedUrl: ResolvedUrl): Promise<Result<string, string>>;
    /**
     * Caching + loading wrapper around _parseContents.
     */
    private _parse;
    /**
     * Parse the given string into the Abstract Syntax Tree (AST) corresponding
     * to its type.
     */
    private _parseContents;
    /**
     * Resolves all resolvable URLs in the list, removes unresolvable ones.
     */
    resolveUserInputUrls(urls: PackageRelativeUrl[]): ResolvedUrl[];
}
