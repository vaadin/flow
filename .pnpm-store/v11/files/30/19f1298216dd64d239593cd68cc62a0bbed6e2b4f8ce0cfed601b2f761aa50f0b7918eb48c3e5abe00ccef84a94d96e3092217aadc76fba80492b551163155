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
const File = require("vinyl");
const polymer_bundler_1 = require("polymer-bundler");
const file_map_url_loader_1 = require("./file-map-url-loader");
const path_transformers_1 = require("./path-transformers");
const streams_1 = require("./streams");
class BuildBundler extends streams_1.AsyncTransformStream {
    constructor(config, buildAnalyzer, options = {}) {
        super({ objectMode: true });
        // A map of urls to file objects.  As the transform stream handleds files
        // coming into the stream, it collects all files here.  After bundlling,
        // we remove files from this set that have been inlined and replace
        // entrypoint/fragment files with bundled versions.
        this.files = new Map();
        this.config = config;
        this._buildAnalyzer = buildAnalyzer;
        const bundlerOptions = Object.assign({}, options);
        const urlLoader = new file_map_url_loader_1.FileMapUrlLoader(this.files, bundlerOptions.analyzer || buildAnalyzer.analyzer);
        bundlerOptions.analyzer =
            (bundlerOptions.analyzer || buildAnalyzer.analyzer)._fork({ urlLoader });
        if (bundlerOptions.strategy === undefined &&
            this.config.shell !== undefined) {
            bundlerOptions.strategy = polymer_bundler_1.generateShellMergeStrategy(bundlerOptions.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, this.config.shell)));
        }
        this._bundler = new polymer_bundler_1.Bundler(bundlerOptions);
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    this._mapFile(file);
                }
            }
            catch (e_1_1) { e_1 = { error: e_1_1 }; }
            finally {
                try {
                    if (files_1_1 && !files_1_1.done && (_a = files_1.return)) yield __await(_a.call(files_1));
                }
                finally { if (e_1) throw e_1.error; }
            }
            yield __await(this._buildBundles());
            for (const file of this.files.values()) {
                yield yield __await(file);
            }
        });
    }
    _buildBundles() {
        return __awaiter(this, void 0, void 0, function* () {
            // Tell the analyzer about changed files so it can purge them from its cache
            // before using the analyzer for bundling.
            yield this._bundler.analyzer.filesChanged(this._getFilesChangedSinceInitialAnalysis());
            const { documents, manifest } = yield this._bundler.bundle(yield this._generateBundleManifest());
            // Remove the bundled files from the file map so they are not emitted later.
            this._unmapBundledFiles(manifest);
            // Map the bundles into the file map.
            for (const [url, document] of documents) {
                this._mapFile(new File({
                    path: path_transformers_1.pathFromUrl(this.config.root, this._bundler.analyzer.urlResolver.relative(url)),
                    contents: Buffer.from(document.content),
                }));
            }
        });
    }
    _generateBundleManifest() {
        return __awaiter(this, void 0, void 0, function* () {
            const entrypoints = this.config.allFragments.map((e) => this._bundler.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, e)));
            return this._bundler.generateManifest(entrypoints);
        });
    }
    _getFilesChangedSinceInitialAnalysis() {
        const filesChanged = [];
        for (const [url, originalFile] of this._buildAnalyzer.files) {
            const downstreamFile = this.files.get(this._buildAnalyzer.analyzer.resolveUrl(url));
            if (downstreamFile == null) {
                throw new Error(`Internal error: could not find downstreamFile at ${url}`);
            }
            if (downstreamFile.contents.toString() !==
                originalFile.contents.toString()) {
                filesChanged.push(url);
            }
        }
        return filesChanged;
    }
    _mapFile(file) {
        this.files.set(this._buildAnalyzer.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, file.path)), file);
    }
    /**
     * Removes all of the inlined files in a bundle manifest from the filemap.
     */
    _unmapBundledFiles(manifest) {
        for (const { files, inlinedHtmlImports, inlinedScripts, inlinedStyles, } of manifest.bundles.values()) {
            for (const url of [...files,
                ...inlinedHtmlImports,
                ...inlinedScripts,
                ...inlinedStyles]) {
                // Don't unmap the bundle file url itself.
                if (!manifest.bundles.has(url)) {
                    this.files.delete(url);
                }
            }
        }
    }
}
exports.BuildBundler = BuildBundler;
//# sourceMappingURL=bundle.js.map