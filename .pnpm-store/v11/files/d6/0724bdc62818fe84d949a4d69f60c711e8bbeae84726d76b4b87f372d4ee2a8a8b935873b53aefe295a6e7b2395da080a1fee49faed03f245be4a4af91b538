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
const polymer_analyzer_1 = require("polymer-analyzer");
const path = require("path");
const util_1 = require("./util");
const polymer_project_1 = require("../polymer-project");
const streams_1 = require("../streams");
const testProjectRoot = path.resolve('test-fixtures/test-project');
suite('PolymerProject', () => {
    let defaultProject;
    const unroot = ((p) => p.substring(testProjectRoot.length + 1));
    setup(() => {
        defaultProject = new polymer_project_1.PolymerProject({
            root: 'test-fixtures/test-project/',
            entrypoint: 'index.html',
            shell: 'shell.html',
            sources: [
                'source-dir/**',
            ],
        });
    });
    test('will not throw an exception when created with minimum options', () => {
        new polymer_project_1.PolymerProject({
            root: 'test-fixtures/test-project/',
        });
    });
    test('reads sources', (done) => {
        const files = [];
        defaultProject.sources()
            .on('data', (f) => files.push(f))
            .on('end', () => {
            const names = files.map((f) => unroot(f.path));
            const expected = [
                'index.html',
                'shell.html',
                path.join('source-dir', 'my-app.html'),
                path.join('source-dir', 'shell.js'),
                path.join('source-dir', 'style.css'),
            ];
            chai_1.assert.deepEqual(names.sort(), expected);
            done();
        });
    });
    test('the sources & dependencies streams remain paused until use', () => {
        // Check that data isn't flowing through sources until consumer usage
        const sourcesStream = defaultProject.sources();
        chai_1.assert.isNull(util_1.getFlowingState(sourcesStream));
        sourcesStream.on('data', () => { });
        chai_1.assert.isTrue(util_1.getFlowingState(sourcesStream));
        // Check that data isn't flowing through dependencies until consumer usage
        const dependencyStream = defaultProject.dependencies();
        chai_1.assert.isNull(util_1.getFlowingState(dependencyStream));
        dependencyStream.on('data', () => { });
        chai_1.assert.isTrue(util_1.getFlowingState(dependencyStream));
    });
    suite('.bundler()', () => {
        test('returns a different bundler each time', () => {
            const bundlerA = defaultProject.bundler();
            const bundlerB = defaultProject.bundler();
            chai_1.assert.notEqual(bundlerA, bundlerB);
        });
        test('takes options to configure bundler', () => {
            const urlResolver = new polymer_analyzer_1.FsUrlResolver('test-fixtures/test-project');
            const bundler = defaultProject.bundler({
                analyzer: new polymer_analyzer_1.Analyzer({
                    urlResolver,
                    urlLoader: new polymer_analyzer_1.FsUrlLoader('test-fixtures/test-project')
                }),
                excludes: ['bower_components/loads-external-dependencies.html'].map((p) => urlResolver.resolve(p)),
                inlineCss: true,
                inlineScripts: false,
                rewriteUrlsInTemplates: true,
                stripComments: true,
                strategy: (b) => b,
                // TODO(usergenic): Replace this with a BundleUrlMapper when
                // https://github.com/Polymer/polymer-bundler/pull/483 is released.
                urlMapper: (b) => new Map(b.map((b) => ['x', b])),
            });
            chai_1.assert.isOk(bundler);
        });
    });
    suite('.dependencies()', () => {
        test('reads dependencies', (done) => {
            const files = [];
            const dependencyStream = defaultProject.dependencies();
            dependencyStream.on('data', (f) => files.push(f));
            dependencyStream.on('end', () => {
                const names = files.map((f) => unroot(f.path));
                const expected = [
                    path.join('bower_components', 'dep.html'),
                    path.join('bower_components', 'loads-external-dependencies.html'),
                ];
                chai_1.assert.deepEqual(names.sort(), expected);
                done();
            });
        });
        const testName = 'reads dependencies in a monolithic (non-shell) application without timing out';
        test(testName, () => {
            const project = new polymer_project_1.PolymerProject({
                root: testProjectRoot,
                entrypoint: 'index.html',
                sources: [
                    'source-dir/**',
                    'index.html',
                    'shell.html',
                ],
            });
            const sourcesStream = project.sources();
            const dependencyStream = project.dependencies();
            sourcesStream.on('data', () => { });
            dependencyStream.on('data', () => { });
            return Promise.all([streams_1.waitFor(project.sources()), streams_1.waitFor(dependencyStream)]);
        });
        test('reads dependencies and includes additionally provided files', (done) => {
            const files = [];
            const projectWithIncludedDeps = new polymer_project_1.PolymerProject({
                root: testProjectRoot,
                entrypoint: 'index.html',
                shell: 'shell.html',
                sources: [
                    'source-dir/**',
                ],
                extraDependencies: [
                    'bower_components/unreachable*',
                ],
            });
            const dependencyStream = projectWithIncludedDeps.dependencies();
            dependencyStream.on('data', (f) => files.push(f));
            dependencyStream.on('error', done);
            dependencyStream.on('end', () => {
                const names = files.map((f) => unroot(f.path));
                const expected = [
                    path.join('bower_components', 'dep.html'),
                    path.join('bower_components', 'loads-external-dependencies.html'),
                    path.join('bower_components', 'unreachable-dep.html'),
                ];
                chai_1.assert.deepEqual(names.sort(), expected);
                done();
            });
        });
    });
});
//# sourceMappingURL=polymer-project_test.js.map