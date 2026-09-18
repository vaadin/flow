"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
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
const chai_1 = require("chai");
const polymer_analyzer_1 = require("polymer-analyzer");
const polymer_project_1 = require("../polymer-project");
const prefetch_links_1 = require("../prefetch-links");
const util_1 = require("./util");
const mergeStream = require('merge-stream');
suite('prefetch-links', () => {
    suite('AddPrefetchLinks', () => {
        test('adds prefetch links for transitive deps of unbundled', () => __awaiter(this, void 0, void 0, function* () {
            const project = new polymer_project_1.PolymerProject({
                root: 'test-fixtures/bundle-project/',
                entrypoint: 'index.html',
            });
            const files = yield util_1.emittedFiles(mergeStream(project.sources(), project.dependencies())
                .pipe(project.addPrefetchLinks()), project.config.root);
            const html = files.get('index.html').contents.toString();
            // No prefetch links needed for direct dependency.
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/simple-import.html">');
            // Prefetch added for the transitive dependencies of `index.html`,
            // which are all direct dependencies of `simple-import.html`.
            chai_1.assert.include(html, '<link rel="prefetch" href="/simple-script.js">');
            chai_1.assert.include(html, '<link rel="prefetch" href="/simple-style.css">');
            chai_1.assert.include(html, '<link rel="prefetch" href="/simple-import-2.html">');
        }));
        test('add prefetch links for transitive deps of bundled', () => __awaiter(this, void 0, void 0, function* () {
            const project = new polymer_project_1.PolymerProject({
                root: 'test-fixtures/bundle-project/',
                entrypoint: 'index.html',
                fragments: ['simple-import.html'],
            });
            const files = yield util_1.emittedFiles(mergeStream(project.sources(), project.dependencies())
                .pipe(project.bundler({ inlineScripts: false }))
                .pipe(project.addPrefetchLinks()), project.config.root);
            const expectedFiles = ['index.html', 'simple-import.html', 'simple-script.js'];
            chai_1.assert.deepEqual(expectedFiles, [...files.keys()].sort());
            const html = files.get('index.html').contents.toString();
            // `simple-import.html` is a direct dependency, so we should not add
            // prefetch link to it.
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/simple-import.html">');
            // `simple-import.html` has inlined `simple-import-2.html` which has an
            // external script import `simple-script.js`.  A prefetch link is added
            // for `simple-script.js` because it is a transitive dependency of the
            // `index.html`
            chai_1.assert.include(html, '<link rel="prefetch" href="/simple-script.js">');
        }));
        test('prefetch links do not include lazy dependencies', () => __awaiter(this, void 0, void 0, function* () {
            const project = new polymer_project_1.PolymerProject({
                root: 'test-fixtures/bundler-data/',
                entrypoint: 'index.html',
            });
            const files = yield util_1.emittedFiles(mergeStream(project.sources(), project.dependencies())
                .pipe(project.addPrefetchLinks()), project.config.root);
            const html = files.get('index.html').contents.toString();
            // Shell is a direct dependency, so should not have a prefetch link.
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/shell.html">');
            // Framework is in the shell, so is a transitive dependency of index, and
            // should be prefetched.
            chai_1.assert.include(html, '<link rel="prefetch" href="/framework.html">');
            // These are lazy imports and should not be prefetched.
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/entrypoint-a.html">');
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/entrypoint-b.html">');
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/entrypoint-c.html">');
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/common-dependency.html">');
            chai_1.assert.notInclude(html, '<link rel="prefetch" href="/lazy-dependency.html">');
        }));
        test('prefetch links are relative when base tag present', () => __awaiter(this, void 0, void 0, function* () {
            const project = new polymer_project_1.PolymerProject({
                root: 'test-fixtures/differential-serving/',
                entrypoint: 'index.html',
                fragments: ['shell.html'],
            });
            const files = yield util_1.emittedFiles(mergeStream(project.sources(), project.dependencies())
                .pipe(project.bundler({ inlineScripts: false }))
                .pipe(project.addPrefetchLinks()), project.config.root);
            const html = files.get('index.html').contents.toString();
            // The `external-script.js` file is imported by `shell.html` so is
            // transitive dependency of `index.html`.  Because `index.html` has a base
            // tag with an href, the prefetch is a relative URL.
            chai_1.assert.include(html, '<link rel="prefetch" href="shell-stuff/external-script.js">');
        }));
    });
    suite('createLinks', () => {
        const urlResolver = new polymer_analyzer_1.FsUrlResolver('');
        const html = '<html><body>foo</body></html>';
        const htmlWithBase = '<html><base href="/base/"><body>foo</body></html>';
        const deps = new Set([
            'bower_components/polymer/polymer.html',
            'src/my-icons.html',
        ].map((u) => urlResolver.resolve(u)));
        test('with no base tag and absolute true', () => {
            const url = 'index.html';
            const expected = ('<html>' +
                '<link rel="prefetch" href="/bower_components/polymer/polymer.html">' +
                '<link rel="prefetch" href="/src/my-icons.html">' +
                '<body>foo</body></html>');
            const actual = prefetch_links_1.createLinks(urlResolver, html, urlResolver.resolve(url), deps, true);
            chai_1.assert.equal(actual, expected);
        });
        test('with a base tag and absolute true', () => {
            const url = 'index.html';
            const expected = ('<html><base href="/base/">' +
                '<link rel="prefetch" href="bower_components/polymer/polymer.html">' +
                '<link rel="prefetch" href="src/my-icons.html">' +
                '<body>foo</body></html>');
            const actual = prefetch_links_1.createLinks(urlResolver, htmlWithBase, urlResolver.resolve(url), deps, true);
            chai_1.assert.equal(actual, expected);
        });
    });
});
//# sourceMappingURL=prefetch-links_test.js.map