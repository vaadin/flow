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
const chai_1 = require("chai");
const polymer_analyzer_1 = require("polymer-analyzer");
const polymer_bundler_1 = require("polymer-bundler");
const polymer_project_config_1 = require("polymer-project-config");
const dom5 = require("dom5/lib/index-next");
const parse5_1 = require("parse5");
const path = require("path");
const mergeStream = require('merge-stream');
const analyzer_1 = require("../analyzer");
const bundle_1 = require("../bundle");
const streams_1 = require("../streams");
const defaultRoot = path.resolve('test-fixtures/bundler-data');
class FileTransform extends streams_1.AsyncTransformStream {
    constructor(transform) {
        super({ objectMode: true });
        this.transform = transform;
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    this.transform(this, file.clone());
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
function resolveUrls(root, urls) {
    const resolver = new polymer_analyzer_1.FsUrlResolver(root);
    return urls.map((u) => resolver.resolve(u));
}
suite('BuildBundler', () => {
    let root;
    let bundler;
    let bundledStream;
    let files;
    const setupTest = (projectOptions, bundlerOptions, transform) => __awaiter(this, void 0, void 0, function* () {
        return new Promise((resolve, reject) => {
            if (projectOptions.root === undefined) {
                throw new Error('projectOptions.root is undefined');
            }
            root = projectOptions.root;
            const config = new polymer_project_config_1.ProjectConfig(projectOptions);
            const analyzer = new analyzer_1.BuildAnalyzer(config);
            bundler = new bundle_1.BuildBundler(config, analyzer, bundlerOptions);
            bundledStream = mergeStream(analyzer.sources(), analyzer.dependencies());
            if (transform) {
                bundledStream = bundledStream.pipe(transform);
            }
            bundledStream = bundledStream.pipe(bundler);
            bundler = new bundle_1.BuildBundler(config, analyzer);
            files = new Map();
            bundledStream.on('data', (file) => {
                files.set(file.path, file);
            });
            bundledStream.on('end', () => {
                resolve(files);
            });
            bundledStream.on('error', (err) => {
                reject(err);
            });
        });
    });
    const getFile = (filename) => {
        // we're getting FS paths, so add root
        const file = files.get(path.resolve(root, filename));
        return file && file.contents && file.contents.toString();
    };
    const getFileOrDie = (filename) => {
        const file = getFile(filename);
        if (file == null) {
            throw new Error(`Unable to get file with filename ${filename}`);
        }
        return file;
    };
    const hasMarker = (doc, id) => {
        const marker = dom5.query(doc, dom5.predicates.AND(dom5.predicates.hasTagName('div'), dom5.predicates.hasAttrValue('id', id)));
        return marker != null;
    };
    const hasImport = (doc, url) => {
        const link = dom5.query(doc, dom5.predicates.AND(dom5.predicates.hasTagName('link'), dom5.predicates.hasAttrValue('rel', 'import'), dom5.predicates.hasAttrValue('href', url)));
        return link != null;
    };
    const addHeaders = new FileTransform((stream, file) => {
        if (path.extname(file.path) === '.html') {
            file.contents =
                Buffer.from(`<!-- ${path.basename(file.path)} -->${file.contents}`);
        }
        else if (path.extname(file.path).match(/^\.(js|css)$/)) {
            file.contents =
                Buffer.from(`/* ${path.basename(file.path)} */${file.contents}`);
        }
        stream.push(file);
    });
    test('entrypoint only', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            root: defaultRoot,
            entrypoint: 'entrypoint-only.html',
        });
        const doc = parse5_1.parse(getFileOrDie('entrypoint-only.html'));
        chai_1.assert.isTrue(hasMarker(doc, 'framework'), 'has framework');
        chai_1.assert.isFalse(hasImport(doc, 'framework.html'));
        chai_1.assert.isNotOk(getFile('shared_bundle_1.html'));
    }));
    test('two fragments', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            root: defaultRoot,
            entrypoint: 'entrypoint-a.html',
            fragments: ['shell.html', 'entrypoint-a.html'],
        });
        // shell doesn't import framework
        const shellDoc = parse5_1.parse(getFileOrDie('shell.html'));
        chai_1.assert.isFalse(hasMarker(shellDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(shellDoc, 'framework.html'));
        // entrypoint doesn't import framework
        const entrypointDoc = parse5_1.parse(getFileOrDie('entrypoint-a.html'));
        chai_1.assert.isFalse(hasMarker(entrypointDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(entrypointDoc, 'framework.html'));
        // No shared-bundle bundles framework
        const sharedDoc = parse5_1.parse(getFileOrDie('shared_bundle_1.html'));
        chai_1.assert.isTrue(hasMarker(sharedDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(sharedDoc, 'framework.html'));
        // fragments import shared-bundle
        chai_1.assert.isTrue(hasImport(entrypointDoc, 'shared_bundle_1.html'));
        chai_1.assert.isTrue(hasImport(shellDoc, 'shared_bundle_1.html'));
    }));
    // TODO(usergenic): It appears that this test is aspirational.  It wants
    // build to manipulate the entrypoint to remove things that have been bundled
    // into the shell, in this case, but we don't yet support manipulating the
    // entrypoint properly.  In part, this is because entrypoints can not have
    // relative paths, since they can be served from any url.   Note that the
    // test 'entrypoint and fragments' below is skipped for the same reason.
    test.skip('shell and entrypoint', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            entrypoint: 'entrypoint-a.html',
            shell: 'shell.html',
        });
        // shell bundles framework
        const shellDoc = parse5_1.parse(getFileOrDie('shell.html'));
        chai_1.assert.isTrue(hasMarker(shellDoc, 'framework'));
        // entrypoint doesn't import framework
        const entrypointDoc = parse5_1.parse(getFileOrDie('entrypoint-a.html'));
        chai_1.assert.isFalse(hasMarker(entrypointDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(entrypointDoc, 'framework.html'));
        // entrypoint imports shell
        chai_1.assert.isTrue(hasImport(entrypointDoc, 'shell.html'));
        // No shared-bundle with a shell
        chai_1.assert.isNotOk(getFile('shared_bundle_1.html'));
    }));
    test('shell and fragments with shared dependency', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            root: defaultRoot,
            entrypoint: 'entrypoint-a.html',
            shell: 'shell.html',
            fragments: ['entrypoint-b.html', 'entrypoint-c.html'],
        });
        // shell bundles framework
        const shellDoc = parse5_1.parse(getFileOrDie('shell.html'));
        chai_1.assert.isTrue(hasMarker(shellDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(shellDoc, 'framework.html'));
        // shell bundles commonDep
        chai_1.assert.isTrue(hasMarker(shellDoc, 'commonDep'));
        chai_1.assert.isFalse(hasImport(shellDoc, 'common-dependency.html'));
        // entrypoint B doesn't import commonDep
        const entrypointBDoc = parse5_1.parse(getFileOrDie('entrypoint-b.html'));
        chai_1.assert.isFalse(hasMarker(entrypointBDoc, 'commonDep'));
        chai_1.assert.isFalse(hasImport(entrypointBDoc, 'common-dependency.html'));
        // entrypoint C doesn't import commonDep
        const entrypointCDoc = parse5_1.parse(getFileOrDie('entrypoint-c.html'));
        chai_1.assert.isFalse(hasMarker(entrypointCDoc, 'commonDep'));
        chai_1.assert.isFalse(hasImport(entrypointCDoc, 'common-dependency.html'));
        // entrypoints don't import shell
        chai_1.assert.isFalse(hasImport(entrypointBDoc, 'shell.html'));
        chai_1.assert.isFalse(hasImport(entrypointCDoc, 'shell.html'));
        // No shared-bundle with a shell
        chai_1.assert.isNotOk(getFile('shared_bundle_1.html'));
    }));
    // TODO(usergenic): This test is skipped for the same reason as the test
    // above called 'shell and entrypoint'.
    test.skip('entrypoint and fragments', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            entrypoint: 'entrypoint-a.html',
            fragments: [
                'shell.html',
                'entrypoint-b.html',
                'entrypoint-c.html',
            ],
        });
        // shared bundle was emitted
        const bundle = getFileOrDie('shared_bundle_1.html');
        chai_1.assert.ok(bundle);
        const bundleDoc = parse5_1.parse(bundle);
        // shared-bundle bundles framework
        chai_1.assert.isTrue(hasMarker(bundleDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(bundleDoc, 'framework.html'));
        // shared-bundle bundles commonDep
        chai_1.assert.isTrue(hasMarker(bundleDoc, 'commonDep'));
        chai_1.assert.isFalse(hasImport(bundleDoc, 'common-dependency.html'));
        // entrypoint doesn't import framework
        const entrypointDoc = parse5_1.parse(getFileOrDie('entrypoint-a.html'));
        chai_1.assert.isFalse(hasMarker(entrypointDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(entrypointDoc, 'framework.html'));
        // shell doesn't import framework
        const shellDoc = parse5_1.parse(getFileOrDie('entrypoint-a.html'));
        chai_1.assert.isFalse(hasMarker(shellDoc, 'framework'));
        chai_1.assert.isFalse(hasImport(shellDoc, 'framework.html'));
        // entrypoint B doesn't import commonDep
        const entrypointBDoc = parse5_1.parse(getFileOrDie('entrypoint-b.html'));
        chai_1.assert.isFalse(hasMarker(entrypointBDoc, 'commonDep'));
        chai_1.assert.isFalse(hasImport(entrypointBDoc, 'common-dependency.html'));
        // entrypoint C doesn't import commonDep
        const entrypointCDoc = parse5_1.parse(getFileOrDie('entrypoint-c.html'));
        chai_1.assert.isFalse(hasMarker(entrypointCDoc, 'commonDep'));
        chai_1.assert.isFalse(hasImport(entrypointCDoc, 'common-dependency.html'));
        // entrypoint and fragments import shared-bundle
        chai_1.assert.isTrue(hasImport(entrypointDoc, 'shared_bundle_1.html'));
        chai_1.assert.isTrue(hasImport(entrypointBDoc, 'shared_bundle_1.html'));
        chai_1.assert.isTrue(hasImport(entrypointCDoc, 'shared_bundle_1.html'));
        chai_1.assert.isTrue(hasImport(shellDoc, 'shared_bundle_1.html'));
    }));
    test('bundler loads changed files from stream', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            root: path.resolve('test-fixtures/bundle-project'),
            entrypoint: 'index.html',
        }, {}, addHeaders);
        const bundledHtml = getFileOrDie('index.html');
        // In setupTest, we use a transform stream that to prepends
        // each file with a comment including its basename before it makes it
        // into the bundler.  This verifies that bundler is processing files from
        // the stream instead of from the filesystem.
        chai_1.assert.include(bundledHtml, '<!-- index.html -->');
        chai_1.assert.include(bundledHtml, '<!-- simple-import.html -->');
        chai_1.assert.include(bundledHtml, '<!-- simple-import-2.html -->');
        chai_1.assert.include(bundledHtml, '/* simple-style.css */');
    }));
    test('bundler deals with win32 platform separators on win32', () => __awaiter(this, void 0, void 0, function* () {
        const platformSepPaths = new FileTransform((stream, file) => {
            if (path.sep === '\\') {
                file.path = file.path.replace(/\//g, path.sep);
            }
            stream.push(file);
        });
        yield setupTest({
            root: path.resolve('test-fixtures/bundle-project'),
            entrypoint: 'index.html',
        }, {}, platformSepPaths);
        const bundledHtml = getFileOrDie('index.html');
        // In setupTest, we use a transform stream that forces the file paths to
        // be in the original platform form (this only changes/matters for win32)
        // and it verifies that bundler can process files that may be merged in
        // or have otherwise reverted form paths in win32 separator form.
        chai_1.assert.include(bundledHtml, '<title>Sample Build</title>', 'index.html');
        chai_1.assert.include(bundledHtml, '<dom-module id="my-element">', 'simple-import.html');
        chai_1.assert.include(bundledHtml, '<dom-module id="my-element-2">', 'simple-import-2.html');
        chai_1.assert.include(bundledHtml, '.simply-red', 'simple-style.css');
    }));
    test('bundler deals with posix platform separators on win32', () => __awaiter(this, void 0, void 0, function* () {
        const posixSepPaths = new FileTransform((stream, file) => {
            if (path.sep === '\\') {
                file.path = file.path.replace(/\\/g, '/');
            }
            stream.push(file);
        });
        yield setupTest({
            root: path.resolve('test-fixtures/bundle-project'),
            entrypoint: 'index.html'
        }, {}, posixSepPaths);
        const bundledHtml = getFileOrDie('index.html');
        // In setupTest, we use a transform stream that forces the file paths to
        // be in the posix form (this only changes/matters for win32)
        // and it verifies that bundler can process files that may be merged in
        // or have otherwise have paths in posix separator form.
        chai_1.assert.include(bundledHtml, '<title>Sample Build</title>', 'index.html');
        chai_1.assert.include(bundledHtml, '<dom-module id="my-element">', 'simple-import.html');
        chai_1.assert.include(bundledHtml, '<dom-module id="my-element-2">', 'simple-import-2.html');
        chai_1.assert.include(bundledHtml, '.simply-red', 'simple-style.css');
    }));
    test('bundler does not output inlined html imports', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({ root: defaultRoot, entrypoint: 'entrypoint-only.html' });
        // We should have an entrypoint-only.html file (bundled).
        chai_1.assert.isOk(getFile('entrypoint-only.html'));
        // We should not have the inlined file in the output.
        chai_1.assert.isNotOk(getFile('framework.html'));
    }));
    test('bundler outputs html imports that are not inlined', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({ root: defaultRoot, entrypoint: 'entrypoint-only.html' }, { excludes: resolveUrls(defaultRoot, ['framework.html']) });
        // We should have an entrypoint-only.html file (bundled).
        chai_1.assert.isOk(getFile('entrypoint-only.html'));
        // We should have the html import that was excluded from inlining.
        chai_1.assert.isOk(getFile('framework.html'));
    }));
    test('bundler does not output inlined scripts or styles', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            root: path.resolve('test-fixtures/bundle-project'),
            entrypoint: 'index.html',
        });
        chai_1.assert.deepEqual([...files.keys()].sort(), [path.resolve('test-fixtures/bundle-project/index.html')]);
    }));
    test('bundler does output scripts and styles not inlined', () => __awaiter(this, void 0, void 0, function* () {
        yield setupTest({
            root: path.resolve('test-fixtures/bundle-project'),
            entrypoint: 'index.html',
        }, {
            inlineCss: false,
            inlineScripts: false,
        });
        chai_1.assert.deepEqual([...files.keys()].sort(), [
            'test-fixtures/bundle-project/index.html',
            'test-fixtures/bundle-project/simple-script.js',
            'test-fixtures/bundle-project/simple-style.css'
        ].map((p) => path.resolve(p)));
    }));
    suite('options', () => {
        const projectOptions = {
            root: 'test-fixtures/test-project',
            entrypoint: 'index.html',
            fragments: ['shell.html'],
            componentDir: 'bower_components',
        };
        test('excludes: html file urls listed are not inlined', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, {
                excludes: resolveUrls(projectOptions.root, ['bower_components/loads-external-dependencies.html']),
            });
            chai_1.assert.isOk(getFile('bower_components/loads-external-dependencies.html'), 'Excluded import is passed through the bundler');
            chai_1.assert.include(getFileOrDie('shell.html'), '<link rel="import" href="bower_components/loads-external-dependencies.html">');
        }));
        test('excludes: html files in folders listed are not inlined', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { excludes: resolveUrls(projectOptions.root, ['bower_components/']) });
            chai_1.assert.include(getFileOrDie('shell.html'), '<link rel="import" href="bower_components/dep.html">');
        }));
        test('excludes: nothing is excluded when no excludes are given', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { excludes: [] });
            chai_1.assert.isNotOk(getFile('bower_components/loads-external-dependencies.html'), 'Inlined imports are not passed through the bundler');
            chai_1.assert.notInclude(getFileOrDie('shell.html'), '<link rel="import" href="bower_components/loads-external-dependencies.html">');
            chai_1.assert.include(getFileOrDie('shell.html'), '<script src="https://www.example.com/script.js">', 'Inlined import content');
        }));
        test('inlineCss: false, does not inline external stylesheets', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { inlineCss: false });
            chai_1.assert.notInclude(getFileOrDie('shell.html'), '.test-project-style');
        }));
        test('inlineCss: true, inlines external stylesheets', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { inlineCss: true });
            chai_1.assert.include(getFileOrDie('shell.html'), '.test-project-style');
        }));
        test('inlineScripts: false, does not inline external scripts', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { inlineScripts: false });
            chai_1.assert.notInclude(getFileOrDie('shell.html'), 'console.log(\'shell\')');
        }));
        test('inlineScripts: true, inlines external scripts', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { inlineScripts: true });
            chai_1.assert.include(getFileOrDie('shell.html'), 'console.log(\'shell\')');
        }));
        test('rewriteUrlsInTemplates: false, does not rewrite urls', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { rewriteUrlsInTemplates: false });
            chai_1.assert.include(getFileOrDie('shell.html'), 'url(\'dep-bg.png\')');
        }));
        test('rewriteUrlsInTemplates: true, rewrites relative urls', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { rewriteUrlsInTemplates: true });
            chai_1.assert.include(getFileOrDie('shell.html'), 'url("bower_components/dep-bg.png")');
        }));
        test('stripComments: false, does not strip html comments', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { stripComments: false });
            chai_1.assert.include(getFileOrDie('shell.html'), '<!-- remote dependencies should be ignored during build -->');
        }));
        test('stripComments: true, strips html comments', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, { stripComments: true });
            chai_1.assert.notInclude(getFileOrDie('shell.html'), '<!-- remote dependencies should be ignored during build -->');
        }));
        test('strategy: fn(), applies bundle strategy function', () => __awaiter(this, void 0, void 0, function* () {
            yield setupTest(projectOptions, {
                // Custom strategy creates a separate bundle for everything in the
                // `bower_components` folder.
                strategy: (bundles) => {
                    const bowerBundle = new polymer_bundler_1.Bundle('html-fragment');
                    bundles.forEach((bundle) => {
                        bundle.files.forEach((file) => {
                            if (file.includes('bower_components')) {
                                bowerBundle.files.add(file);
                                bundle.files.delete(file);
                            }
                        });
                    });
                    return bundles.concat(bowerBundle);
                }
            });
            chai_1.assert.isOk(getFile('shared_bundle_1.html'));
            chai_1.assert.include(getFileOrDie('shared_bundle_1.html'), '<dom-module id="dep" assetpath="bower_components/"');
        }));
        test('urlMapper: fn(), applies bundle url mapper function', () => __awaiter(this, void 0, void 0, function* () {
            const urlResolver = new polymer_analyzer_1.FsUrlResolver(projectOptions.root);
            yield setupTest(projectOptions, {
                urlMapper: (bundles) => {
                    const map = new Map();
                    for (const bundle of bundles) {
                        map.set(urlResolver.resolve(`bundled/${[...bundle.entrypoints]
                            .map((u) => urlResolver.relative(u))
                            .join()}`), bundle);
                    }
                    return map;
                }
            });
            chai_1.assert.isOk(getFile('bundled/shell.html'));
        }));
    });
});
//# sourceMappingURL=bundle_test.js.map