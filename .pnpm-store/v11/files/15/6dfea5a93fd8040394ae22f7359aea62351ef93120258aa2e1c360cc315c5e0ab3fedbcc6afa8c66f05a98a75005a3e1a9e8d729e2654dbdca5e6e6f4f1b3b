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
var __asyncValues = (this && this.__asyncValues) || function (o) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var m = o[Symbol.asyncIterator], i;
    return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i);
    function verb(n) { i[n] = o[n] && function (v) { return new Promise(function (resolve, reject) { v = o[n](v), settle(resolve, reject, v.done, v.value); }); }; }
    function settle(resolve, reject, d, v) { Promise.resolve(v).then(function(v) { resolve({ value: v, done: d }); }, reject); }
};
var __await = (this && this.__await) || function (v) { return this instanceof __await ? (this.v = v, this) : new __await(v); }
var __asyncGenerator = (this && this.__asyncGenerator) || function (thisArg, _arguments, generator) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var g = generator.apply(thisArg, _arguments || []), i, q = [];
    return i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i;
    function verb(n) { if (g[n]) i[n] = function (v) { return new Promise(function (a, b) { q.push([n, v, a, b]) > 1 || resume(n, v); }); }; }
    function resume(n, v) { try { step(g[n](v)); } catch (e) { settle(q[0][3], e); } }
    function step(r) { r.value instanceof __await ? Promise.resolve(r.value.v).then(fulfill, reject) : settle(q[0][2], r); }
    function fulfill(value) { resume("next", value); }
    function reject(value) { resume("throw", value); }
    function settle(f, v) { if (f(v), q.shift(), q.length) resume(q[0][0], q[0][1]); }
};
Object.defineProperty(exports, "__esModule", { value: true });
const path = require("path");
const logging = require("plylog");
const polymer_analyzer_1 = require("polymer-analyzer");
const stream_1 = require("stream");
const vinyl_fs_1 = require("vinyl-fs");
const path_transformers_1 = require("./path-transformers");
const streams_1 = require("./streams");
const logger = logging.getLogger('cli.build.analyzer');
/**
 * A stream that tells the BuildAnalyzer to resolve each file it sees. It's
 * important that files are resolved here in a seperate stream, so that analysis
 * and file loading/resolution can't block each other while waiting.
 */
class ResolveTransform extends streams_1.AsyncTransformStream {
    constructor(buildAnalyzer) {
        super({ objectMode: true });
        this._buildAnalyzer = buildAnalyzer;
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    this._buildAnalyzer.resolveFile(file);
                    yield yield __await(file);
                }
            }
            catch (e_1_1) { e_1 = { error: e_1_1 }; }
            finally {
                try {
                    if (files_1_1 && !files_1_1.done && (_a = files_1.return)) yield __await(_a.call(files_1));
                }
                finally { if (e_1) throw e_1.error; }
            }
        });
    }
}
/**
 * A stream to analyze every file that passes through it. This is used to
 * analyze important application fragments as they pass through the source
 * stream.
 *
 * We create a new stream to handle this because the alternative (attaching
 * event listeners directly to the existing sources stream) would
 * start the flow of data before the user was ready to consume it. By
 * analyzing inside of the stream instead of via "data" event listeners, the
 * source stream will remain paused until the user is ready to start the stream
 * themselves.
 */
class AnalyzeTransform extends streams_1.AsyncTransformStream {
    constructor(buildAnalyzer) {
        // A high `highWaterMark` value is needed to keep this from pausing the
        // entire source stream.
        // TODO(fks) 02-02-2017: Move analysis out of the source stream itself so
        // that it no longer blocks during analysis.
        super({ objectMode: true, highWaterMark: 10000 });
        this._buildAnalyzer = buildAnalyzer;
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_2() {
            var e_2, _a;
            try {
                for (var files_2 = __asyncValues(files), files_2_1; files_2_1 = yield __await(files_2.next()), !files_2_1.done;) {
                    const file = files_2_1.value;
                    yield __await(this._buildAnalyzer.analyzeFile(file));
                    yield yield __await(file);
                }
            }
            catch (e_2_1) { e_2 = { error: e_2_1 }; }
            finally {
                try {
                    if (files_2_1 && !files_2_1.done && (_a = files_2.return)) yield __await(_a.call(files_2));
                }
                finally { if (e_2) throw e_2.error; }
            }
        });
    }
}
class BuildAnalyzer {
    constructor(config, 
    /** If null is given, we do not log warnings. */
    streamToWarnTo = process.stdout) {
        this.streamToWarnTo = streamToWarnTo;
        this.started = false;
        this.sourceFilesLoaded = false;
        this.files = new Map();
        this.warnings = new Set();
        this._dependencyAnalysis = {
            depsToFragments: new Map(),
            fragmentToDeps: new Map(),
            fragmentToFullDeps: new Map()
        };
        this.config = config;
        this.loader = new StreamLoader(this);
        this.analyzer = new polymer_analyzer_1.Analyzer({
            urlLoader: this.loader,
            // TODO(usergenic): Add option to polymer-build to propagate a protocol
            // and host option to the FsUrlResolver.
            urlResolver: new polymer_analyzer_1.FsUrlResolver(config.root),
            moduleResolution: config.moduleResolution === 'none' ?
                undefined :
                config.moduleResolution,
        });
        this.allFragmentsToAnalyze =
            new Set(this.config.allFragments.map((f) => f));
        this.analyzeDependencies = new Promise((resolve, _reject) => {
            this._resolveDependencyAnalysis = resolve;
        });
        const lintOptions = (this.config.lint || {});
        const warningCodesToIgnore = new Set(lintOptions.ignoreWarnings || []);
        // These are expected, as we never want to load remote URLs like
        // `https://example.com/` when we're building
        warningCodesToIgnore.add('not-loadable');
        this._warningsFilter = new polymer_analyzer_1.WarningFilter({ warningCodesToIgnore, minimumSeverity: polymer_analyzer_1.Severity.WARNING });
    }
    /**
     * Start analysis by setting up the sources and dependencies analysis
     * pipelines and starting the source stream. Files will not be loaded from
     * disk until this is called. Can be called multiple times but will only run
     * set up once.
     */
    startAnalysis() {
        if (this.started) {
            return;
        }
        this.started = true;
        // Create the base streams for sources & dependencies to be read from.
        this._dependenciesStream = new stream_1.PassThrough({ objectMode: true });
        this._sourcesStream = vinyl_fs_1.src(this.config.sources, {
            cwdbase: true,
            nodir: true,
        });
        // _sourcesProcessingStream: Pipe the sources stream through...
        //   1. The resolver stream, to resolve each file loaded via the analyzer
        //   2. The analyzer stream, to analyze app fragments for dependencies
        this._sourcesProcessingStream =
            this._sourcesStream
                .on('error', (err) => this._sourcesProcessingStream.emit('error', err))
                .pipe(new ResolveTransform(this))
                .on('error', (err) => this._sourcesProcessingStream.emit('error', err))
                .on('end', this.onSourcesStreamComplete.bind(this))
                .pipe(new AnalyzeTransform(this));
        // _dependenciesProcessingStream: Pipe the dependencies stream through...
        //   1. The vinyl loading stream, to load file objects from file paths
        //   2. The resolver stream, to resolve each loaded file for the analyzer
        this._dependenciesProcessingStream =
            this._dependenciesStream
                .on('error', (err) => this._dependenciesProcessingStream.emit('error', err))
                .pipe(new streams_1.VinylReaderTransform())
                .on('error', (err) => this._dependenciesProcessingStream.emit('error', err))
                .pipe(new ResolveTransform(this));
    }
    /**
     * Return _dependenciesOutputStream, which will contain fully loaded file
     * objects for each dependency after analysis.
     */
    dependencies() {
        this.startAnalysis();
        return this._dependenciesProcessingStream;
    }
    /**
     * Return _sourcesOutputStream, which will contain fully loaded file
     * objects for each source after analysis.
     */
    sources() {
        this.startAnalysis();
        return this._sourcesProcessingStream;
    }
    /**
     * Resolve a file in our loader so that the analyzer can read it.
     */
    resolveFile(file) {
        const filePath = file.path;
        this.addFile(file);
        // If our resolver is waiting for this file, resolve its deferred loader
        if (this.loader.hasDeferredFile(filePath)) {
            this.loader.resolveDeferredFile(filePath, file);
        }
    }
    /**
     * Analyze a file to find additional dependencies to load. Currently we only
     * get dependencies for application fragments. When all fragments are
     * analyzed, we call _done() to signal that analysis is complete.
     */
    analyzeFile(file) {
        return __awaiter(this, void 0, void 0, function* () {
            const filePath = file.path;
            // If the file is a fragment, begin analysis on its dependencies
            if (this.config.isFragment(filePath)) {
                const deps = yield this._getDependencies(this.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, filePath)));
                this._addDependencies(filePath, deps);
                this.allFragmentsToAnalyze.delete(filePath);
                // If there are no more fragments to analyze, we are done
                if (this.allFragmentsToAnalyze.size === 0) {
                    this._done();
                }
            }
        });
    }
    /**
     * Perform some checks once we know that `_sourcesStream` is done loading.
     */
    onSourcesStreamComplete() {
        // Emit an error if there are missing source files still deferred. Otherwise
        // this would cause the analyzer to hang.
        for (const filePath of this.loader.deferredFiles.keys()) {
            if (this.config.isSource(filePath)) {
                const err = new Error(`Not found: ${filePath}`);
                this.loader.rejectDeferredFile(filePath, err);
            }
        }
        // Set sourceFilesLoaded so that future files aren't accidentally deferred
        this.sourceFilesLoaded = true;
    }
    /**
     * Helper function for emitting a general analysis error onto both file
     * streams.
     */
    emitAnalysisError(err) {
        this._sourcesProcessingStream.emit('error', err);
        this._dependenciesProcessingStream.emit('error', err);
    }
    /**
     * Called when analysis is complete and there are no more files to analyze.
     * Checks for serious errors before resolving its dependency analysis and
     * ending the dependency stream (which it controls).
     */
    _done() {
        this.printWarnings();
        const allWarningCount = this.countWarningsByType();
        const errorWarningCount = allWarningCount.get(polymer_analyzer_1.Severity.ERROR);
        // If any ERROR warnings occurred, propagate an error in each build stream.
        if (errorWarningCount > 0) {
            this.emitAnalysisError(new Error(`${errorWarningCount} error(s) occurred during build.`));
            return;
        }
        // If analysis somehow finished with files that still needed to be loaded,
        // propagate an error in each build stream.
        for (const filePath of this.loader.deferredFiles.keys()) {
            const err = new Error(`Not found: ${filePath}`);
            this.loader.rejectDeferredFile(filePath, err);
            return;
        }
        // Resolve our dependency analysis promise now that we have seen all files
        this._dependenciesStream.end();
        this._resolveDependencyAnalysis(this._dependencyAnalysis);
    }
    getFile(filepath) {
        const url = path_transformers_1.urlFromPath(this.config.root, filepath);
        return this.getFileByUrl(url);
    }
    getFileByUrl(url) {
        // TODO(usergenic): url carefulness bug, take an extra careful look at this.
        if (url.startsWith('/')) {
            url = url.substring(1);
        }
        return this.files.get(url);
    }
    /**
     * A side-channel to add files to the loader that did not come through the
     * stream transformation. This is for generated files, like
     * shared-bundle.html. This should probably be refactored so that the files
     * can be injected into the stream.
     */
    addFile(file) {
        logger.debug(`addFile: ${file.path}`);
        // Badly-behaved upstream transformers (looking at you gulp-html-minifier)
        // may use posix path separators on Windows.
        const filepath = path.normalize(file.path);
        // Store only root-relative paths, in URL/posix format
        this.files.set(path_transformers_1.urlFromPath(this.config.root, filepath), file);
    }
    printWarnings() {
        if (this.streamToWarnTo === null) {
            return;
        }
        const warningPrinter = new polymer_analyzer_1.WarningPrinter(this.streamToWarnTo);
        warningPrinter.printWarnings(this.warnings);
    }
    countWarningsByType() {
        const errorCountMap = new Map();
        errorCountMap.set(polymer_analyzer_1.Severity.INFO, 0);
        errorCountMap.set(polymer_analyzer_1.Severity.WARNING, 0);
        errorCountMap.set(polymer_analyzer_1.Severity.ERROR, 0);
        for (const warning of this.warnings) {
            errorCountMap.set(warning.severity, errorCountMap.get(warning.severity) + 1);
        }
        return errorCountMap;
    }
    /**
     * Attempts to retreive document-order transitive dependencies for `url`.
     */
    _getDependencies(url) {
        return __awaiter(this, void 0, void 0, function* () {
            const analysis = yield this.analyzer.analyze([url]);
            const result = analysis.getDocument(url);
            if (result.successful === false) {
                const message = result.error && result.error.message || 'unknown';
                throw new Error(`Unable to get document ${url}: ${message}`);
            }
            const doc = result.value;
            doc.getWarnings({ imported: true })
                .filter((w) => !this._warningsFilter.shouldIgnore(w))
                .forEach((w) => this.warnings.add(w));
            const scripts = new Set();
            const styles = new Set();
            const imports = new Set();
            const importFeatures = doc.getFeatures({ kind: 'import', externalPackages: true, imported: true });
            for (const importFeature of importFeatures) {
                const importUrl = importFeature.url;
                if (!this.analyzer.canLoad(importUrl)) {
                    logger.debug(`ignoring external dependency: ${importUrl}`);
                }
                else if (importFeature.type === 'html-script') {
                    scripts.add(this.analyzer.urlResolver.relative(importUrl));
                }
                else if (importFeature.type === 'html-style') {
                    styles.add(this.analyzer.urlResolver.relative(importUrl));
                }
                else if (importFeature.type === 'html-import') {
                    imports.add(this.analyzer.urlResolver.relative(importUrl));
                }
                else {
                    logger.debug(`unexpected import type encountered: ${importFeature.type}`);
                }
            }
            const deps = {
                scripts: [...scripts],
                styles: [...styles],
                imports: [...imports],
            };
            logger.debug(`dependencies analyzed for: ${url}`, deps);
            return deps;
        });
    }
    _addDependencies(filePath, deps) {
        // Make sure function is being called properly
        if (!this.allFragmentsToAnalyze.has(filePath)) {
            throw new Error(`Dependency analysis incorrectly called for ${filePath}`);
        }
        const relativeUrl = path_transformers_1.urlFromPath(this.config.root, filePath);
        // Add dependencies to _dependencyAnalysis object, and push them through
        // the dependency stream.
        this._dependencyAnalysis.fragmentToFullDeps.set(relativeUrl, deps);
        this._dependencyAnalysis.fragmentToDeps.set(relativeUrl, deps.imports);
        deps.imports.forEach((url) => {
            const entrypointList = this._dependencyAnalysis.depsToFragments.get(url);
            if (entrypointList) {
                entrypointList.push(relativeUrl);
            }
            else {
                this._dependencyAnalysis.depsToFragments.set(url, [relativeUrl]);
            }
        });
    }
    /**
     * Check that the source stream has not already completed loading by the
     * time
     * this file was analyzed.
     */
    sourcePathAnalyzed(filePath) {
        // If we've analyzed a new path to a source file after the sources
        // stream has completed, we can assume that that file does not
        // exist. Reject with a "Not Found" error.
        if (this.sourceFilesLoaded) {
            throw new Error(`Not found: "${filePath}"`);
        }
        // Source files are loaded automatically through the vinyl source
        // stream. If it hasn't been seen yet, defer resolving until it has
        // been loaded by vinyl.
        logger.debug('dependency is a source file, ignoring...', { dep: filePath });
    }
    /**
     * Push the given filepath into the dependencies stream for loading.
     * Each dependency is only pushed through once to avoid duplicates.
     */
    dependencyPathAnalyzed(filePath) {
        if (this.getFile(filePath)) {
            logger.debug('dependency has already been pushed, ignoring...', { dep: filePath });
            return;
        }
        logger.debug('new dependency analyzed, pushing into dependency stream...', filePath);
        this._dependenciesStream.push(filePath);
    }
}
exports.BuildAnalyzer = BuildAnalyzer;
class StreamLoader {
    constructor(buildAnalyzer) {
        // Store files that have not yet entered the Analyzer stream here.
        // Later, when the file is seen, the DeferredFileCallback can be
        // called with the file contents to resolve its loading.
        this.deferredFiles = new Map();
        this._buildAnalyzer = buildAnalyzer;
        this.config = this._buildAnalyzer.config;
    }
    hasDeferredFile(filePath) {
        return this.deferredFiles.has(filePath);
    }
    hasDeferredFiles() {
        return this.deferredFiles.size > 0;
    }
    resolveDeferredFile(filePath, file) {
        const deferredCallbacks = this.deferredFiles.get(filePath);
        if (deferredCallbacks == null) {
            throw new Error(`Internal error: could not get deferredCallbacks for ${filePath}`);
        }
        deferredCallbacks.resolve(file.contents.toString());
        this.deferredFiles.delete(filePath);
    }
    rejectDeferredFile(filePath, err) {
        const deferredCallbacks = this.deferredFiles.get(filePath);
        if (deferredCallbacks == null) {
            throw new Error(`Internal error: could not get deferredCallbacks for ${filePath}`);
        }
        deferredCallbacks.reject(err);
        this.deferredFiles.delete(filePath);
    }
    // We can't load external dependencies.
    canLoad(url) {
        return url.startsWith('file:///');
    }
    load(url) {
        return __awaiter(this, void 0, void 0, function* () {
            logger.debug(`loading: ${url}`);
            if (!this.canLoad(url)) {
                throw new Error('Unable to load ${url}.');
            }
            const urlPath = this._buildAnalyzer.analyzer.urlResolver.relative(url);
            const filePath = path_transformers_1.pathFromUrl(this.config.root, urlPath);
            const file = this._buildAnalyzer.getFile(filePath);
            if (file) {
                return file.contents.toString();
            }
            return new Promise((resolve, reject) => {
                this.deferredFiles.set(filePath, { resolve, reject });
                try {
                    if (this.config.isSource(filePath)) {
                        this._buildAnalyzer.sourcePathAnalyzed(filePath);
                    }
                    else {
                        this._buildAnalyzer.dependencyPathAnalyzed(filePath);
                    }
                }
                catch (err) {
                    this.rejectDeferredFile(filePath, err);
                }
            });
        });
    }
}
exports.StreamLoader = StreamLoader;
//# sourceMappingURL=analyzer.js.map