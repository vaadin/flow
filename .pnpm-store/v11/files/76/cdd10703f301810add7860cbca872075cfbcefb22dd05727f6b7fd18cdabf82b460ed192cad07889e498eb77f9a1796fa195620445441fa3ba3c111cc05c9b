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
const dom5 = require("dom5/lib/index-next");
const parse5 = require("parse5");
const path = require("path");
const polymer_analyzer_1 = require("polymer-analyzer");
const File = require("vinyl");
const path_transformers_1 = require("./path-transformers");
const file_map_url_loader_1 = require("./file-map-url-loader");
const streams_1 = require("./streams");
/**
 * A stream that modifies HTML files to include prefetch links for all of the
 * file's transitive dependencies.
 */
class AddPrefetchLinks extends streams_1.AsyncTransformStream {
    constructor(config) {
        super({ objectMode: true });
        this.files = new Map();
        this._config = config;
        this._analyzer =
            new polymer_analyzer_1.Analyzer({ urlLoader: new file_map_url_loader_1.FileMapUrlLoader(this.files) });
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            const htmlFileUrls = [];
            try {
                // Map all files; pass-through all non-HTML files.
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    const fileUrl = this._analyzer.resolveUrl(path_transformers_1.urlFromPath(this._config.root, file.path));
                    this.files.set(fileUrl, file);
                    if (path.extname(file.path) !== '.html') {
                        yield yield __await(file);
                    }
                    else {
                        htmlFileUrls.push(fileUrl);
                    }
                }
            }
            catch (e_1_1) { e_1 = { error: e_1_1 }; }
            finally {
                try {
                    if (files_1_1 && !files_1_1.done && (_a = files_1.return)) yield __await(_a.call(files_1));
                }
                finally { if (e_1) throw e_1.error; }
            }
            // Analyze each HTML file and add prefetch links.
            const analysis = yield __await(this._analyzer.analyze(htmlFileUrls));
            for (const documentUrl of htmlFileUrls) {
                const result = analysis.getDocument(documentUrl);
                if (result.successful === false) {
                    const message = result.error && result.error.message;
                    console.warn(`Unable to get document ${documentUrl}: ${message}`);
                    continue;
                }
                const document = result.value;
                const allDependencyUrls = [...document.getFeatures({
                        kind: 'import',
                        externalPackages: true,
                        imported: true,
                        noLazyImports: true
                    })].filter((d) => d.document !== undefined && !d.lazy)
                    .map((d) => d.document.url);
                const directDependencyUrls = [...document.getFeatures({
                        kind: 'import',
                        externalPackages: true,
                        imported: false,
                        noLazyImports: true
                    })].filter((d) => d.document !== undefined && !d.lazy)
                    .map((d) => d.document.url);
                const onlyTransitiveDependencyUrls = allDependencyUrls.filter((d) => directDependencyUrls.indexOf(d) === -1);
                // No need to transform a file if it has no dependencies to prefetch.
                if (onlyTransitiveDependencyUrls.length === 0) {
                    yield yield __await(this.files.get(documentUrl));
                    continue;
                }
                const prefetchUrls = new Set(onlyTransitiveDependencyUrls);
                const html = createLinks(this._analyzer.urlResolver, document.parsedDocument.contents, document.parsedDocument.baseUrl, prefetchUrls, document.url ===
                    this._analyzer.resolveUrl(path_transformers_1.urlFromPath(this._config.root, this._config.entrypoint)));
                const filePath = path_transformers_1.pathFromUrl(this._config.root, this._analyzer.urlResolver.relative(documentUrl));
                yield yield __await(new File({ contents: Buffer.from(html, 'utf-8'), path: filePath }));
            }
        });
    }
}
exports.AddPrefetchLinks = AddPrefetchLinks;
/**
 * Returns the given HTML updated with import or prefetch links for the given
 * dependencies. The given url and deps are expected to be project-relative
 * URLs (e.g. "index.html" or "src/view.html") unless absolute parameter is
 * `true` and there is no base tag in the document.
 */
function createLinks(urlResolver, html, baseUrl, deps, absolute = false) {
    const ast = parse5.parse(html, { locationInfo: true });
    const baseTag = dom5.query(ast, dom5.predicates.hasTagName('base'));
    const baseTagHref = baseTag ? dom5.getAttribute(baseTag, 'href') : '';
    // parse5 always produces a <head> element.
    const head = dom5.query(ast, dom5.predicates.hasTagName('head'));
    for (const dep of deps) {
        let href;
        if (absolute && !baseTagHref) {
            href = absUrl(urlResolver.relative(dep));
        }
        else {
            href = urlResolver.relative(baseUrl, dep);
        }
        const link = dom5.constructors.element('link');
        dom5.setAttribute(link, 'rel', 'prefetch');
        dom5.setAttribute(link, 'href', href);
        dom5.append(head, link);
    }
    dom5.removeFakeRootElements(ast);
    return parse5.serialize(ast);
}
exports.createLinks = createLinks;
function absUrl(url) {
    return (url.startsWith('/') ? url : '/' + url);
}
//# sourceMappingURL=prefetch-links.js.map