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
Object.defineProperty(exports, "__esModule", { value: true });
const chai_1 = require("chai");
const path_1 = require("path");
const path_transformers_1 = require("../path-transformers");
const WindowsRootPath = 'C:\\Users\\TEST_USER\\TEST_ROOT';
const MacRootPath = '/Users/TEST_USER/TEST_ROOT';
const RootPath = path_1.sep === '\\' ? WindowsRootPath : MacRootPath;
suite('pathFromUrl()', () => {
    test('creates a filesystem path using the platform separators', () => {
        const otherSeparator = path_1.sep === '/' ? '\\' : '/';
        const path = path_transformers_1.pathFromUrl(RootPath, '/some/url/pathname');
        chai_1.assert.include(path, path_1.sep);
        chai_1.assert.notInclude(path, otherSeparator);
    });
    test('returns a path if url is absolute', () => {
        const path = path_transformers_1.pathFromUrl(RootPath, '/absolute/path');
        chai_1.assert.equal(path, path_1.join(RootPath, 'absolute', 'path'));
    });
    test('returns a path if url relative', () => {
        const path = path_transformers_1.pathFromUrl(RootPath, 'relative/path');
        chai_1.assert.equal(path, path_1.join(RootPath, 'relative', 'path'));
    });
    test('will not go outside the root path', () => {
        const path = path_transformers_1.pathFromUrl(RootPath, '../../../still/../root/path');
        chai_1.assert.equal(path, path_1.join(RootPath, 'root', 'path'));
    });
    test('will decode URI percent encoded characters', () => {
        const path = path_transformers_1.pathFromUrl(RootPath, '/%40foo/spaced%20out');
        chai_1.assert.equal(path, path_1.join(RootPath, '/@foo/spaced out'));
    });
});
suite('urlFromPath()', () => {
    test('throws error when path is not in root', () => {
        chai_1.assert.throws(() => {
            path_transformers_1.urlFromPath('/this/is/a/path', '/some/other/path/shop-app.html');
        });
        chai_1.assert.throws(() => {
            path_transformers_1.urlFromPath('/the/path', '/the/pathologist/index.html');
        });
    });
    test('creates a URL path relative to root', () => {
        const shortPath = path_transformers_1.urlFromPath(RootPath, path_1.join(RootPath, 'shop-app.html'));
        chai_1.assert.equal(shortPath, 'shop-app.html');
        const medPath = path_transformers_1.urlFromPath(RootPath, path_1.join(RootPath, 'src', 'shop-app.html'));
        chai_1.assert.equal(medPath, 'src/shop-app.html');
        const longPath = path_transformers_1.urlFromPath(RootPath, path_1.join(RootPath, 'bower_components', 'app-layout', 'docs.html'));
        chai_1.assert.equal(longPath, 'bower_components/app-layout/docs.html');
    });
    test('will properly encode URL-unfriendly characters like spaces', () => {
        const url = path_transformers_1.urlFromPath(RootPath, path_1.join(RootPath, 'spaced out'));
        chai_1.assert.equal(url, 'spaced%20out');
    });
});
//# sourceMappingURL=path-transformers_test.js.map