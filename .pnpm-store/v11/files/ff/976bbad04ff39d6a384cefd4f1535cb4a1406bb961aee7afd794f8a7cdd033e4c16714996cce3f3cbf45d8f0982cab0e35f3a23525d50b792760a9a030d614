"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const cancel_token_1 = require("cancel-token");
const path = require("path");
const analyzer_1 = require("../core/analyzer");
const css_custom_property_scanner_1 = require("../css/css-custom-property-scanner");
const css_parser_1 = require("../css/css-parser");
const html_element_reference_scanner_1 = require("../html/html-element-reference-scanner");
const html_import_scanner_1 = require("../html/html-import-scanner");
const html_parser_1 = require("../html/html-parser");
const html_script_scanner_1 = require("../html/html-script-scanner");
const html_style_scanner_1 = require("../html/html-style-scanner");
const class_scanner_1 = require("../javascript/class-scanner");
const function_scanner_1 = require("../javascript/function-scanner");
const html_template_literal_scanner_1 = require("../javascript/html-template-literal-scanner");
const javascript_export_scanner_1 = require("../javascript/javascript-export-scanner");
const javascript_import_scanner_1 = require("../javascript/javascript-import-scanner");
const javascript_parser_1 = require("../javascript/javascript-parser");
const namespace_scanner_1 = require("../javascript/namespace-scanner");
const json_parser_1 = require("../json/json-parser");
const model_1 = require("../model/model");
const document_1 = require("../parser/document");
const behavior_scanner_1 = require("../polymer/behavior-scanner");
const css_import_scanner_1 = require("../polymer/css-import-scanner");
const dom_module_scanner_1 = require("../polymer/dom-module-scanner");
const polymer_core_feature_scanner_1 = require("../polymer/polymer-core-feature-scanner");
const polymer_element_scanner_1 = require("../polymer/polymer-element-scanner");
const pseudo_element_scanner_1 = require("../polymer/pseudo-element-scanner");
const scan_1 = require("../scanning/scan");
const package_url_resolver_1 = require("../url-loader/package-url-resolver");
const analysis_cache_1 = require("./analysis-cache");
exports.analyzerVersion = require('../../package.json').version;
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
class AnalysisContext {
    constructor(options, cache, generation) {
        this.parsers = new Map([
            ['html', new html_parser_1.HtmlParser()],
            ['js', new javascript_parser_1.JavaScriptParser()],
            ['mjs', new javascript_parser_1.JavaScriptParser()],
            ['css', new css_parser_1.CssParser()],
            ['json', new json_parser_1.JsonParser()],
        ]);
        this._languageAnalyzers = new Map([
        // TODO(rictic): add typescript language analyzer back after investigating
        //     https://github.com/Polymer/polymer-analyzer/issues/623
        ]);
        this.loader = options.urlLoader;
        this.resolver = options.urlResolver || new package_url_resolver_1.PackageUrlResolver();
        this.parsers = options.parsers || this.parsers;
        this._lazyEdges = options.lazyEdges;
        this._scanners =
            options.scanners || AnalysisContext.getDefaultScanners(options);
        this._cache = cache || new analysis_cache_1.AnalysisCache();
        this._generation = generation || 0;
        this._analysisComplete = Promise.resolve();
    }
    static getDefaultScanners(options) {
        return new Map([
            [
                'html',
                [
                    new html_import_scanner_1.HtmlImportScanner(options.lazyEdges),
                    new html_script_scanner_1.HtmlScriptScanner(),
                    new html_style_scanner_1.HtmlStyleScanner(),
                    new dom_module_scanner_1.DomModuleScanner(),
                    new css_import_scanner_1.CssImportScanner(),
                    new html_element_reference_scanner_1.HtmlCustomElementReferenceScanner(),
                    new pseudo_element_scanner_1.PseudoElementScanner(),
                ]
            ],
            [
                'js',
                [
                    new polymer_element_scanner_1.PolymerElementScanner(),
                    new polymer_core_feature_scanner_1.PolymerCoreFeatureScanner(),
                    new behavior_scanner_1.BehaviorScanner(),
                    new namespace_scanner_1.NamespaceScanner(),
                    new function_scanner_1.FunctionScanner(),
                    new class_scanner_1.ClassScanner(),
                    new javascript_import_scanner_1.JavaScriptImportScanner({ moduleResolution: options.moduleResolution }),
                    new javascript_export_scanner_1.JavaScriptExportScanner(),
                    new html_template_literal_scanner_1.InlineHtmlDocumentScanner(),
                ]
            ],
            ['css', [new css_custom_property_scanner_1.CssCustomPropertyScanner()]]
        ]);
    }
    /**
     * Returns a copy of this cache context with proper cache invalidation.
     */
    filesChanged(urls) {
        const newCache = this._cache.invalidate(this.resolveUserInputUrls(urls));
        return this._fork(newCache);
    }
    /**
     * Implements Analyzer#analyze, see its docs.
     */
    analyze(urls, cancelToken) {
        return __awaiter(this, void 0, void 0, function* () {
            const resolvedUrls = this.resolveUserInputUrls(urls);
            // 1. Await current analysis if there is one, so we can check to see if it
            // has all of the requested URLs.
            yield this._analysisComplete;
            // 2. Check to see if we have all of the requested documents
            const hasAllDocuments = resolvedUrls.every((url) => this._cache.analyzedDocuments.get(url) != null);
            if (hasAllDocuments) {
                // all requested URLs are present, return the existing context
                return this;
            }
            // 3. Some URLs are new, so fork, but don't invalidate anything
            const newCache = this._cache.invalidate([]);
            const newContext = this._fork(newCache);
            return newContext._analyze(resolvedUrls, cancelToken);
        });
    }
    /**
     * Internal analysis method called when we know we need to fork.
     */
    _analyze(resolvedUrls, cancelToken) {
        return __awaiter(this, void 0, void 0, function* () {
            const analysisComplete = (() => __awaiter(this, void 0, void 0, function* () {
                // 1. Load and scan all root documents
                const maybeScannedDocuments = yield Promise.all(resolvedUrls.map((url) => __awaiter(this, void 0, void 0, function* () {
                    try {
                        const scannedResult = yield this.scan(url, cancelToken);
                        if (scannedResult.successful === true) {
                            this._cache.failedDocuments.delete(url);
                            return scannedResult.value;
                        }
                        else {
                            this._cache.failedDocuments.set(url, scannedResult.error);
                            return undefined;
                        }
                    }
                    catch (e) {
                        if (cancel_token_1.isCancel(e)) {
                            return;
                        }
                        // This is a truly unexpected error. We should fail.
                        throw e;
                    }
                })));
                const scannedDocuments = maybeScannedDocuments.filter((d) => d !== undefined);
                // 2. Run per-document resolution
                const documents = scannedDocuments.map((d) => this.getDocument(d.url));
                // TODO(justinfagnani): instead of the above steps, do:
                // 1. Load and run prescanners
                // 2. Run global analyzers (_languageAnalyzers now, but it doesn't need to
                //    be separated by file type)
                // 3. Run per-document scanners and resolvers
                return documents;
            }))();
            this._analysisComplete = analysisComplete.then((_) => { });
            yield this._analysisComplete;
            return this;
        });
    }
    /**
     * Gets an analyzed Document from the document cache. This is only useful for
     * Analyzer plugins. You almost certainly want to use `analyze()` instead.
     *
     * If a document has been analyzed, it returns the analyzed Document. If not
     * the scanned document cache is used and a new analyzed Document is returned.
     * If a file is in neither cache, it returns `undefined`.
     */
    getDocument(resolvedUrl) {
        const cachedWarning = this._cache.failedDocuments.get(resolvedUrl);
        if (cachedWarning) {
            return cachedWarning;
        }
        const cachedResult = this._cache.analyzedDocuments.get(resolvedUrl);
        if (cachedResult) {
            return cachedResult;
        }
        const scannedDocument = this._cache.scannedDocuments.get(resolvedUrl);
        if (!scannedDocument) {
            return makeRequestedWithoutLoadingWarning(resolvedUrl);
        }
        const extension = path.extname(resolvedUrl).substring(1);
        const languageAnalyzer = this._languageAnalyzers.get(extension);
        let analysisResult;
        if (languageAnalyzer) {
            analysisResult = languageAnalyzer.analyze(scannedDocument.url);
        }
        const document = new model_1.Document(scannedDocument, this, analysisResult);
        this._cache.analyzedDocuments.set(resolvedUrl, document);
        this._cache.analyzedDocumentPromises.getOrCompute(resolvedUrl, () => __awaiter(this, void 0, void 0, function* () { return document; }));
        document.resolve();
        return document;
    }
    /**
     * This is only useful for Analyzer plugins.
     *
     * If a url has been scanned, returns the ScannedDocument.
     */
    _getScannedDocument(resolvedUrl) {
        return this._cache.scannedDocuments.get(resolvedUrl);
    }
    /**
     * Clear all cached information from this analyzer instance.
     *
     * Note: if at all possible, instead tell the analyzer about the specific
     * files that changed rather than clearing caches like this. Caching provides
     * large performance gains.
     */
    clearCaches() {
        return this._fork(new analysis_cache_1.AnalysisCache());
    }
    /**
     * Returns a copy of the context but with optional replacements of cache or
     * constructor options.
     *
     * Note: this feature is experimental.
     */
    _fork(cache, options) {
        const contextOptions = {
            lazyEdges: this._lazyEdges,
            parsers: this.parsers,
            scanners: this._scanners,
            urlLoader: this.loader,
            urlResolver: this.resolver,
        };
        if (options && options.urlLoader) {
            contextOptions.urlLoader = options.urlLoader;
        }
        if (!cache) {
            cache = this._cache.invalidate([]);
        }
        const copy = new AnalysisContext(contextOptions, cache, this._generation + 1);
        return copy;
    }
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
    _scanLocal(resolvedUrl, cancelToken) {
        return __awaiter(this, void 0, void 0, function* () {
            return this._cache.scannedDocumentPromises.getOrCompute(resolvedUrl, () => __awaiter(this, void 0, void 0, function* () {
                const parsedDocResult = yield this._parse(resolvedUrl, cancelToken);
                if (parsedDocResult.successful === false) {
                    this._cache.dependencyGraph.rejectDocument(resolvedUrl, new model_1.WarningCarryingException(parsedDocResult.error));
                    return parsedDocResult;
                }
                const parsedDoc = parsedDocResult.value;
                try {
                    const scannedDocument = yield this._scanDocument(parsedDoc);
                    const imports = scannedDocument.getNestedFeatures().filter((e) => e instanceof model_1.ScannedImport);
                    // Update dependency graph
                    const importUrls = filterOutUndefineds(imports.map((i) => i.url === undefined ?
                        undefined :
                        this.resolver.resolve(parsedDoc.baseUrl, i.url, i)));
                    this._cache.dependencyGraph.addDocument(resolvedUrl, importUrls);
                    return { successful: true, value: scannedDocument };
                }
                catch (e) {
                    const message = (e && e.message) || `Unknown error during scan.`;
                    const warning = new model_1.Warning({
                        code: 'could-not-scan',
                        message,
                        parsedDocument: parsedDoc,
                        severity: model_1.Severity.ERROR,
                        sourceRange: {
                            file: resolvedUrl,
                            start: { column: 0, line: 0 },
                            end: { column: 0, line: 0 }
                        }
                    });
                    this._cache.dependencyGraph.rejectDocument(resolvedUrl, new model_1.WarningCarryingException(warning));
                    return { successful: false, error: warning };
                }
            }), cancelToken);
        });
    }
    /**
     * Scan a toplevel document and all of its transitive dependencies.
     */
    scan(resolvedUrl, cancelToken) {
        return __awaiter(this, void 0, void 0, function* () {
            return this._cache.dependenciesScannedPromises.getOrCompute(resolvedUrl, () => __awaiter(this, void 0, void 0, function* () {
                const scannedDocumentResult = yield this._scanLocal(resolvedUrl, cancelToken);
                if (scannedDocumentResult.successful === false) {
                    return scannedDocumentResult;
                }
                const scannedDocument = scannedDocumentResult.value;
                const imports = scannedDocument.getNestedFeatures().filter((e) => e instanceof model_1.ScannedImport);
                // Scan imports
                for (const scannedImport of imports) {
                    if (scannedImport.url === undefined) {
                        continue;
                    }
                    const importUrl = this.resolver.resolve(scannedDocument.document.baseUrl, scannedImport.url, scannedImport);
                    if (importUrl === undefined) {
                        continue;
                    }
                    // Request a scan of `importUrl` but do not wait for the results to
                    // avoid deadlock in the case of cycles. Later we use the
                    // DependencyGraph to wait for all transitive dependencies to load.
                    this.scan(importUrl, cancelToken)
                        .then((result) => {
                        if (result.successful === true) {
                            return;
                        }
                        scannedImport.error = result.error;
                    })
                        .catch((e) => {
                        if (cancel_token_1.isCancel(e)) {
                            return;
                        }
                        throw e;
                    });
                }
                yield this._cache.dependencyGraph.whenReady(resolvedUrl);
                return scannedDocumentResult;
            }), cancelToken);
        });
    }
    /**
     * Scans a ParsedDocument.
     */
    _scanDocument(document, maybeAttachedComment, maybeContainingDocument) {
        return __awaiter(this, void 0, void 0, function* () {
            const { features: scannedFeatures, warnings } = yield this._getScannedFeatures(document);
            // If there's an HTML comment that applies to this document then we assume
            // that it applies to the first feature.
            const firstScannedFeature = scannedFeatures[0];
            if (firstScannedFeature && firstScannedFeature instanceof model_1.ScannedElement) {
                firstScannedFeature.applyHtmlComment(maybeAttachedComment, maybeContainingDocument);
            }
            const scannedDocument = new model_1.ScannedDocument(document, scannedFeatures, warnings);
            if (!scannedDocument.isInline) {
                if (this._cache.scannedDocuments.has(scannedDocument.url)) {
                    throw new Error('Scanned document already in cache. This should never happen.');
                }
                this._cache.scannedDocuments.set(scannedDocument.url, scannedDocument);
            }
            yield this._scanInlineDocuments(scannedDocument);
            return scannedDocument;
        });
    }
    _getScannedFeatures(document) {
        return __awaiter(this, void 0, void 0, function* () {
            const scanners = this._scanners.get(document.type);
            if (scanners) {
                try {
                    return yield scan_1.scan(document, scanners);
                }
                catch (e) {
                    if (e instanceof model_1.WarningCarryingException) {
                        throw e;
                    }
                    const message = e == null ? `Unknown error while scanning.` :
                        `Error while scanning: ${String(e)}`;
                    throw new model_1.WarningCarryingException(new model_1.Warning({
                        code: 'internal-scanning-error',
                        message,
                        parsedDocument: document,
                        severity: model_1.Severity.ERROR,
                        sourceRange: {
                            file: document.url,
                            start: { column: 0, line: 0 },
                            end: { column: 0, line: 0 },
                        }
                    }));
                }
            }
            return { features: [], warnings: [] };
        });
    }
    _scanInlineDocuments(containingDocument) {
        return __awaiter(this, void 0, void 0, function* () {
            for (const feature of containingDocument.features) {
                if (!(feature instanceof model_1.ScannedInlineDocument)) {
                    continue;
                }
                const locationOffset = {
                    line: feature.locationOffset.line,
                    col: feature.locationOffset.col,
                    filename: containingDocument.url
                };
                try {
                    const parsedDoc = this._parseContents(feature.type, feature.contents, containingDocument.url, {
                        locationOffset,
                        astNode: feature.astNode,
                        baseUrl: containingDocument.document.baseUrl
                    });
                    const scannedDoc = yield this._scanDocument(parsedDoc, feature.attachedComment, containingDocument.document);
                    feature.scannedDocument = scannedDoc;
                }
                catch (err) {
                    if (err instanceof model_1.WarningCarryingException) {
                        containingDocument.warnings.push(err.warning);
                        continue;
                    }
                    throw err;
                }
            }
        });
    }
    /**
     * Returns `true` if the provided resolved URL can be loaded.  Obeys the
     * semantics defined by `UrlLoader` and should only be used to check
     * resolved URLs.
     */
    canLoad(resolvedUrl) {
        return this.loader.canLoad(resolvedUrl);
    }
    /**
     * Loads the content at the provided resolved URL.  Obeys the semantics
     * defined by `UrlLoader` and should only be used to attempt to load resolved
     * URLs.
     *
     * Currently does no caching. If the provided contents are given then they
     * are used instead of hitting the UrlLoader (e.g. when you have in-memory
     * contents that should override disk).
     */
    load(resolvedUrl) {
        return __awaiter(this, void 0, void 0, function* () {
            if (!this.canLoad(resolvedUrl)) {
                return {
                    successful: false,
                    error: `Configured URL Loader can not load URL ${resolvedUrl}`
                };
            }
            try {
                const value = yield this.loader.load(resolvedUrl);
                return { successful: true, value };
            }
            catch (e) {
                const message = (e && e.message) || `Unknown failure while loading.`;
                return { successful: false, error: message };
            }
        });
    }
    /**
     * Caching + loading wrapper around _parseContents.
     */
    _parse(resolvedUrl, cancelToken) {
        return __awaiter(this, void 0, void 0, function* () {
            return this._cache.parsedDocumentPromises.getOrCompute(resolvedUrl, () => __awaiter(this, void 0, void 0, function* () {
                const result = yield this.load(resolvedUrl);
                if (!result.successful) {
                    return {
                        successful: false,
                        error: new model_1.Warning({
                            code: 'could-not-load',
                            parsedDocument: new document_1.UnparsableParsedDocument(resolvedUrl, ''),
                            severity: model_1.Severity.ERROR,
                            sourceRange: {
                                file: resolvedUrl,
                                start: { column: 0, line: 0 },
                                end: { column: 0, line: 0 }
                            },
                            message: result.error
                        })
                    };
                }
                const extension = path.extname(resolvedUrl).substring(1);
                try {
                    const parsedDoc = this._parseContents(extension, result.value, resolvedUrl);
                    return { successful: true, value: parsedDoc };
                }
                catch (e) {
                    if (e instanceof model_1.WarningCarryingException) {
                        return { successful: false, error: e.warning };
                    }
                    const message = (e && e.message) || `Unknown error while parsing.`;
                    return {
                        successful: false,
                        error: new model_1.Warning({
                            code: 'could-not-parse',
                            parsedDocument: new document_1.UnparsableParsedDocument(resolvedUrl, result.value),
                            severity: model_1.Severity.ERROR,
                            sourceRange: {
                                file: resolvedUrl,
                                start: { column: 0, line: 0 },
                                end: { column: 0, line: 0 }
                            },
                            message
                        })
                    };
                }
            }), cancelToken);
        });
    }
    /**
     * Parse the given string into the Abstract Syntax Tree (AST) corresponding
     * to its type.
     */
    _parseContents(type, contents, url, inlineInfo) {
        const parser = this.parsers.get(type);
        if (parser == null) {
            throw new analyzer_1.NoKnownParserError(`No parser for for file type ${type}`);
        }
        try {
            return parser.parse(contents, url, this.resolver, inlineInfo);
        }
        catch (error) {
            if (error instanceof model_1.WarningCarryingException) {
                throw error;
            }
            const parsedDocument = new document_1.UnparsableParsedDocument(url, contents);
            const message = error == null ? `Unable to parse as ${type}` :
                `Unable to parse as ${type}: ${error}`;
            throw new model_1.WarningCarryingException(new model_1.Warning({
                parsedDocument,
                code: 'parse-error',
                message,
                severity: model_1.Severity.ERROR,
                sourceRange: { file: url, start: { line: 0, column: 0 }, end: { line: 0, column: 0 } }
            }));
        }
    }
    /**
     * Resolves all resolvable URLs in the list, removes unresolvable ones.
     */
    resolveUserInputUrls(urls) {
        return filterOutUndefineds(urls.map((u) => this.resolver.resolve(u)));
    }
}
exports.AnalysisContext = AnalysisContext;
function filterOutUndefineds(arr) {
    return arr.filter((t) => t !== undefined);
}
/**
 * A warning for a weird situation that should never happen.
 *
 * Before calling getDocument(), which is synchronous, a caller must first
 * have finished loading and scanning, as those phases are asynchronous.
 *
 * So we need to construct a warning, but we don't have a parsed document,
 * so we construct this weird fake one. This is such a rare case that it's
 * worth going out of our way here so that warnings can uniformly expect to
 * have documents.
 */
function makeRequestedWithoutLoadingWarning(resolvedUrl) {
    const parsedDocument = new document_1.UnparsableParsedDocument(resolvedUrl, '');
    return new model_1.Warning({
        sourceRange: {
            file: resolvedUrl,
            start: { line: 0, column: 0 },
            end: { line: 0, column: 0 }
        },
        code: 'unable-to-analyze',
        message: `[Internal Error] Document was requested ` +
            `before loading and scanning finished. This usually indicates an ` +
            `anomalous error during loading or analysis. Please file a bug at ` +
            `https://github.com/Polymer/polymer-analyzer/issues/new with info ` +
            `on the source code that caused this. ` +
            `Polymer Analyzer version: ${exports.analyzerVersion}`,
        severity: model_1.Severity.ERROR,
        parsedDocument
    });
}
//# sourceMappingURL=analysis-context.js.map