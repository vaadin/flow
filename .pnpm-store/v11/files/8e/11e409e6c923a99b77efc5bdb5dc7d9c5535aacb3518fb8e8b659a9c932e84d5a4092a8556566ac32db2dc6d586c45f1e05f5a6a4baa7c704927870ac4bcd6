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
const File = require("vinyl");
const path = require("path");
const stream = require("stream");
const polymer_project_1 = require("../polymer-project");
const html_splitter_1 = require("../html-splitter");
const testProjectRoot = path.resolve('test-fixtures/splitter-project');
suite('HtmlSplitter', () => {
    let defaultProject;
    const unroot = ((p) => p.substring(testProjectRoot.length + 1));
    setup(() => {
        defaultProject = new polymer_project_1.PolymerProject({
            root: 'test-fixtures/splitter-project/',
            entrypoint: 'index.html',
            shell: 'shell.html',
            sources: [
                'source-dir/**',
            ],
        });
    });
    test('splits scripts', (done) => {
        const htmlSplitter = new html_splitter_1.HtmlSplitter();
        const splitFiles = new Map();
        defaultProject.sources()
            .pipe(htmlSplitter.split())
            .on('data', (f) => splitFiles.set(unroot(f.path), f))
            .pipe(htmlSplitter.rejoin())
            .on('data', () => { })
            .on('end', () => {
            const expectedSplitFiles = [
                'index.html',
                'shell.html',
                'shell.html_script_0.js',
                'shell.html_script_1.js',
                path.join('source-dir', 'my-app.html'),
            ];
            chai_1.assert.deepEqual(Array.from(splitFiles.keys()).sort(), expectedSplitFiles);
            chai_1.assert.include(splitFiles.get('shell.html_script_0.js').contents.toString(), `console.log('shell');`);
            chai_1.assert.include(splitFiles.get('shell.html_script_1.js').contents.toString(), `console.log('shell 2');`);
            chai_1.assert.notInclude(splitFiles.get('shell.html').contents.toString(), `console.log`);
            chai_1.assert.include(splitFiles.get('shell.html').contents.toString(), `# I am markdown`);
            done();
        });
    });
    test('rejoins scripts', (done) => {
        const htmlSplitter = new html_splitter_1.HtmlSplitter();
        const joinedFiles = new Map();
        defaultProject.sources()
            .pipe(htmlSplitter.split())
            .pipe(htmlSplitter.rejoin())
            .on('data', (f) => joinedFiles.set(unroot(f.path), f))
            .on('end', () => {
            const expectedJoinedFiles = [
                'index.html',
                'shell.html',
                path.join('source-dir', 'my-app.html'),
            ];
            chai_1.assert.deepEqual(Array.from(joinedFiles.keys()).sort(), expectedJoinedFiles);
            chai_1.assert.include(joinedFiles.get('shell.html').contents.toString(), `console.log`);
            done();
        });
    });
    test('handles bad paths', (done) => {
        const htmlSplitter = new html_splitter_1.HtmlSplitter();
        const sourceStream = new stream.Readable({
            objectMode: true,
        });
        const root = path.normalize('/foo');
        const filepath = path.join(root, '/bar/baz.html');
        const source = '<html><head><script>fooify();</script></head><body></body></html>';
        const file = new File({
            cwd: root,
            base: root,
            path: filepath,
            contents: Buffer.from(source),
        });
        sourceStream.pipe(htmlSplitter.split())
            .on('data', (file) => {
            // this is what gulp-html-minifier does...
            if (path.sep === '\\' && file.path.endsWith('.html')) {
                file.path = file.path.replace('\\', '/');
            }
        })
            .pipe(htmlSplitter.rejoin())
            .on('data', (file) => {
            const contents = file.contents.toString();
            chai_1.assert.equal(contents, source);
        })
            .on('end', done)
            .on('error', done);
        sourceStream.push(file);
        sourceStream.push(null);
    });
    test('does not add root elements to documents', (done) => {
        const htmlSplitter = new html_splitter_1.HtmlSplitter();
        const joinedFiles = new Map();
        defaultProject.sources()
            .pipe(htmlSplitter.split())
            .pipe(htmlSplitter.rejoin())
            .on('data', (f) => joinedFiles.set(unroot(f.path), f))
            .on('end', () => {
            const expectedJoinedFiles = [
                'index.html',
                'shell.html',
                path.join('source-dir', 'my-app.html'),
            ];
            chai_1.assert.deepEqual(Array.from(joinedFiles.keys()).sort(), expectedJoinedFiles);
            const shell = joinedFiles.get('shell.html').contents.toString();
            chai_1.assert.notInclude(shell, '<html', 'html element was added');
            chai_1.assert.notInclude(shell, '<head', 'head element was added');
            chai_1.assert.notInclude(shell, '<body', 'body element was added');
            done();
        });
    });
});
//# sourceMappingURL=html-splitter_test.js.map