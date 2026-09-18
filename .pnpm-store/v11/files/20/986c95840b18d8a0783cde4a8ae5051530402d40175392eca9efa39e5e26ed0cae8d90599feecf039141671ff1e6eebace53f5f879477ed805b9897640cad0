"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
const polymer_analyzer_1 = require("polymer-analyzer");
const deps_index_1 = require("polymer-bundler/lib/deps-index");
const File = require("vinyl");
const path_transformers_1 = require("./path-transformers");
const file_map_url_loader_1 = require("./file-map-url-loader");
const streams_1 = require("./streams");
/**
 * A mapping of file extensions and their default resource type.
 */
const extensionToTypeMapping = new Map([
    ['.css', 'style'],
    ['.gif', 'image'],
    ['.html', 'document'],
    ['.png', 'image'],
    ['.jpg', 'image'],
    ['.js', 'script'],
    ['.json', 'script'],
    ['.svg', 'image'],
    ['.webp', 'image'],
    ['.woff', 'font'],
    ['.woff2', 'font'],
]);
/**
 * Get the default resource type for a file based on its extension.
 */
function getResourceTypeFromUrl(url) {
    return extensionToTypeMapping.get(path.extname(url));
}
/**
 * Get the resource type for an import, handling special import types and
 * falling back to getResourceTypeFromUrl() if the resource type can't be
 * detected directly from importFeature.
 */
function getResourceTypeFromImport(importFeature) {
    const importKinds = importFeature.kinds;
    if (importKinds.has('css-import') || importKinds.has('html-style')) {
        return 'style';
    }
    if (importKinds.has('html-import')) {
        return 'document';
    }
    if (importKinds.has('html-script')) {
        return 'script';
    }
    // @NOTE(fks) 04-07-2017: A js-import can actually import multiple types of
    // resources, so we can't guarentee that it's a script and should instead rely
    // on the default file-extension mapping.
    return getResourceTypeFromUrl(importFeature.url);
}
/**
 * Create a PushManifestEntry from an analyzer Import.
 */
function createPushEntryFromImport(importFeature) {
    return {
        type: getResourceTypeFromImport(importFeature),
        weight: 1,
    };
}
/**
 * Analyze the given URL and resolve with a collection of push manifest entries
 * to be added to the overall push manifest.
 */
function generatePushManifestEntryForUrl(analyzer, url) {
    return __awaiter(this, void 0, void 0, function* () {
        const analysis = yield analyzer.analyze([url]);
        const result = analysis.getDocument(url);
        if (result.successful === false) {
            const message = result.error && result.error.message || 'unknown';
            throw new Error(`Unable to get document ${url}: ${message}`);
        }
        const analyzedDocument = result.value;
        const rawImports = [...analyzedDocument.getFeatures({
                kind: 'import',
                externalPackages: true,
                imported: true,
                noLazyImports: true,
            })];
        const importsToPush = rawImports.filter((i) => !(i.type === 'html-import' && i.lazy) &&
            !(i.kinds.has('html-script-back-reference')));
        const pushManifestEntries = {};
        for (const analyzedImport of importsToPush) {
            // TODO This import URL does not respect the document's base tag.
            // Probably an issue more generally with all URLs analyzed out of
            // documents, but base tags are somewhat rare.
            const analyzedImportUrl = analyzedImport.url;
            const relativeImportUrl = analyzer.urlResolver.relative(analyzedImportUrl);
            const analyzedImportEntry = pushManifestEntries[relativeImportUrl];
            if (!analyzedImportEntry) {
                pushManifestEntries[relativeImportUrl] =
                    createPushEntryFromImport(analyzedImport);
            }
        }
        return pushManifestEntries;
    });
}
/**
 * A stream that reads in files from an application to generate an HTTP2/Push
 * manifest that gets injected into the stream.
 */
class AddPushManifest extends streams_1.AsyncTransformStream {
    constructor(config, outPath, basePath) {
        super({ objectMode: true });
        this.files = new Map();
        this.config = config;
        this.analyzer = new polymer_analyzer_1.Analyzer({
            urlLoader: new file_map_url_loader_1.FileMapUrlLoader(this.files),
            urlResolver: new polymer_analyzer_1.FsUrlResolver(config.root),
        });
        this.outPath =
            path.join(this.config.root, outPath || 'push-manifest.json');
        this.basePath = (basePath || '');
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    this.files.set(this.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, file.path)), file);
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
            // Generate a push manifest, and propagate any errors up.
            const pushManifest = yield __await(this.generatePushManifest());
            const pushManifestContents = JSON.stringify(pushManifest, undefined, '  ');
            // Push the new push manifest into the stream.
            yield yield __await(new File({
                path: this.outPath,
                contents: Buffer.from(pushManifestContents),
            }));
        });
    }
    generatePushManifest() {
        return __awaiter(this, void 0, void 0, function* () {
            // Bundler's buildDepsIndex code generates an index with all fragments and
            // all lazy-imports encountered are the keys, so we'll use that function to
            // produce the set of all fragments to generate push-manifest entries for.
            const depsIndex = yield deps_index_1.buildDepsIndex(this.config.allFragments.map((path) => this.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, path))), this.analyzer);
            // Don't include bundler's fake "sub-bundle" URLs (e.g.
            // "foo.html>external#1>bar.js").
            const allFragments = new Set([...depsIndex.keys()].filter((url) => !url.includes('>')));
            // If an app-shell exists, use that as our main push URL because it has a
            // reliable URL. Otherwise, support the single entrypoint URL.
            const mainPushEntrypointUrl = this.analyzer.resolveUrl(path_transformers_1.urlFromPath(this.config.root, this.config.shell ||
                this.config.entrypoint));
            allFragments.add(mainPushEntrypointUrl);
            // Generate the dependencies to push for each fragment.
            const pushManifest = {};
            for (const fragment of allFragments) {
                const absoluteFragmentUrl = '/' + this.analyzer.urlResolver.relative(fragment);
                pushManifest[absoluteFragmentUrl] =
                    yield generatePushManifestEntryForUrl(this.analyzer, fragment);
            }
            // The URLs we got may be absolute or relative depending on how they were
            // declared in the source. This will normalize them to relative by stripping
            // any leading slash.
            //
            // TODO Decide whether they should really be relative or absolute. Relative
            // was chosen here only because most links were already relative so it was
            // a smaller change, but
            // https://github.com/GoogleChrome/http2-push-manifest actually shows
            // relative for the keys and absolute for the values.
            const normalize = (p) => path.posix.join(this.basePath, p).replace(/^\/+/, '');
            const normalized = {};
            for (const source of Object.keys(pushManifest)) {
                const targets = {};
                for (const target of Object.keys(pushManifest[source])) {
                    targets[normalize(target)] = pushManifest[source][target];
                }
                normalized[normalize(source)] = targets;
            }
            return normalized;
        });
    }
}
exports.AddPushManifest = AddPushManifest;
//# sourceMappingURL=push-manifest.js.map