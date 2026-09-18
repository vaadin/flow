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
Object.defineProperty(exports, "__esModule", { value: true });
const chai_1 = require("chai");
const path = require("path");
const polymer_project_1 = require("../polymer-project");
const testProjectRoot = path.resolve('test-fixtures/custom-elements-es5-adapter');
suite('Babel Helpers Injecter', () => {
    let defaultProject;
    const unroot = ((p) => p.substring(testProjectRoot.length + 1));
    setup(() => {
        defaultProject = new polymer_project_1.PolymerProject({
            root: 'test-fixtures/custom-elements-es5-adapter/',
            entrypoint: 'index.html',
            shell: 'shell.html',
            sources: [
                'source-dir/**',
            ],
        });
    });
    test('injects babel helpers inline into the entrypoint', (done) => {
        const webcomponentsLoaderFilename = 'webcomponents-loader.js';
        const files = new Map();
        defaultProject.sources()
            .pipe(defaultProject.addBabelHelpersInEntrypoint())
            .on('data', (f) => files.set(unroot(f.path), f))
            .on('data', () => { })
            .on('end', () => {
            const expectedFiles = [
                'index.html',
                'shell.html',
            ];
            chai_1.assert.deepEqual(Array.from(files.keys()).sort(), expectedFiles);
            const index = files.get('index.html').contents.toString();
            const shell = files.get('shell.html').contents.toString();
            chai_1.assert.include(index, 'babelHelpers=');
            chai_1.assert(index.indexOf('babelHelpers=') <
                index.indexOf(webcomponentsLoaderFilename), 'babel helpers should come before all other executable code.');
            chai_1.assert.notInclude(shell, 'babelHelpers=');
            done();
        });
    });
});
//# sourceMappingURL=inject-babel-helpers_test.js.map