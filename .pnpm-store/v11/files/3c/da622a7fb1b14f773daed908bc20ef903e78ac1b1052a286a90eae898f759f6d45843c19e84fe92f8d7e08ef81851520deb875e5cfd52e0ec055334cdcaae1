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
const chai_1 = require("chai");
const path = require("path");
const vfs = require("vinyl-fs");
const polymer_project_1 = require("../polymer-project");
const streams_1 = require("../streams");
/**
 * A utility stream to check all files that pass through it for a file that
 * matches the given push manifest file path. For that file, the stream asserts
 * that it matches the expected push manifest contents. It will emit
 * "match-success" & "match-failure" events for each test to listen to.
 */
class CheckPushManifest extends streams_1.AsyncTransformStream {
    constructor(filePath, expectedManifest) {
        super({ objectMode: true });
        this.filePath = filePath;
        this.expectedManifest = expectedManifest;
    }
    _transformIter(files) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_1, _a;
            let didAssert = false;
            try {
                for (var files_1 = __asyncValues(files), files_1_1; files_1_1 = yield __await(files_1.next()), !files_1_1.done;) {
                    const file = files_1_1.value;
                    if (this.filePath !== file.path) {
                        yield yield __await(file);
                        continue;
                    }
                    try {
                        const pushManifestContents = file.contents.toString();
                        const pushManifestJson = JSON.parse(pushManifestContents);
                        chai_1.assert.deepEqual(pushManifestJson, this.expectedManifest);
                        this.emit('match-success');
                    }
                    catch (err) {
                        this.emit('match-failure', err);
                    }
                    didAssert = true;
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
            if (!didAssert) {
                throw new Error(`never saw file ${this.filePath}`);
            }
        });
    }
}
/**
 * Utility function to set up the boilerplate for testing the
 * project.addPushManifest() transform stream.
 */
function testPushManifest(project, manifestRelativePath, prefix, expectedManifest, done) {
    const expectedManifestAbsolutePath = path.join(project.config.root, manifestRelativePath || 'push-manifest.json');
    const pushManifestChecker = new CheckPushManifest(expectedManifestAbsolutePath, expectedManifest);
    vfs.src(path.join(project.config.root, '**'))
        .on('error', done)
        .pipe(project.addPushManifest(manifestRelativePath, prefix))
        .on('error', done)
        .pipe(pushManifestChecker)
        .on('data', () => { })
        .on('match-success', done)
        .on('match-failure', done)
        .on('error', done);
}
suite('AddPushManifest', () => {
    const testProjectRoot = path.resolve('test-fixtures/push-manifest-data');
    test('with entrypoint-only config options', (done) => {
        const project = new polymer_project_1.PolymerProject({
            root: testProjectRoot,
            entrypoint: 'entrypoint-only.html',
        });
        const expectedPushManifest = {
            'entrypoint-only.html': {
                'framework.html': {
                    type: 'document',
                    weight: 1,
                },
            },
        };
        testPushManifest(project, undefined, undefined, expectedPushManifest, done);
    });
    test('with entrypoint and fragments config options', (done) => {
        const project = new polymer_project_1.PolymerProject({
            root: testProjectRoot,
            entrypoint: 'entrypoint-only.html',
            fragments: ['entrypoint-b.html', 'entrypoint-c.html'],
            sources: [
                'framework.html',
                'shell.html',
                'entrypoint-a.html',
                'entrypoint-b.html',
                'entrypoint-c.html',
                'common-dependency.html',
            ],
        });
        const expectedPushManifest = {
            'entrypoint-only.html': {
                'framework.html': {
                    type: 'document',
                    weight: 1,
                },
            },
            'common-dependency.html': {
                'example-script.js': {
                    'type': 'script',
                    'weight': 1,
                },
                'example-style.css': {
                    'type': 'style',
                    'weight': 1,
                }
            },
            'entrypoint-b.html': {
                'common-dependency.html': {
                    type: 'document',
                    weight: 1,
                },
                'example-script.js': {
                    type: 'script',
                    weight: 1,
                },
                'example-style.css': {
                    type: 'style',
                    weight: 1,
                },
            },
            'entrypoint-c.html': {},
        };
        testPushManifest(project, undefined, undefined, expectedPushManifest, done);
    });
    test('with full app-shell config options', (done) => {
        const project = new polymer_project_1.PolymerProject({
            root: testProjectRoot,
            entrypoint: 'entrypoint-a.html',
            shell: 'shell.html',
            fragments: ['entrypoint-b.html', 'entrypoint-c.html'],
            sources: [
                'framework.html',
                'shell.html',
                'entrypoint-a.html',
                'entrypoint-b.html',
                'entrypoint-c.html',
                'common-dependency.html',
            ],
        });
        const expectedPushManifest = {
            'shell.html': {
                'framework.html': {
                    type: 'document',
                    weight: 1,
                }
            },
            'common-dependency.html': {
                'example-script.js': {
                    'type': 'script',
                    'weight': 1,
                },
                'example-style.css': {
                    'type': 'style',
                    'weight': 1,
                }
            },
            'entrypoint-b.html': {
                'common-dependency.html': {
                    type: 'document',
                    weight: 1,
                },
                'example-script.js': {
                    type: 'script',
                    weight: 1,
                },
                'example-style.css': {
                    type: 'style',
                    weight: 1,
                },
            },
            'entrypoint-c.html': {}
        };
        testPushManifest(project, undefined, undefined, expectedPushManifest, done);
    });
    test('with custom file path', (done) => {
        const project = new polymer_project_1.PolymerProject({
            root: testProjectRoot,
            entrypoint: 'entrypoint-a.html',
            shell: 'shell.html',
            fragments: ['entrypoint-b.html', 'entrypoint-c.html'],
            sources: [
                'framework.html',
                'shell.html',
                'entrypoint-a.html',
                'entrypoint-b.html',
                'entrypoint-c.html',
                'common-dependency.html',
            ],
        });
        const pushManifestRelativePath = 'custom/push-manifest/path.json';
        const expectedPushManifest = {
            'shell.html': {
                'framework.html': {
                    type: 'document',
                    weight: 1,
                }
            },
            'common-dependency.html': {
                'example-script.js': {
                    'type': 'script',
                    'weight': 1,
                },
                'example-style.css': {
                    'type': 'style',
                    'weight': 1,
                }
            },
            'entrypoint-b.html': {
                'common-dependency.html': {
                    type: 'document',
                    weight: 1,
                },
                'example-script.js': {
                    type: 'script',
                    weight: 1,
                },
                'example-style.css': {
                    type: 'style',
                    weight: 1,
                },
            },
            'entrypoint-c.html': {}
        };
        testPushManifest(project, pushManifestRelativePath, undefined, expectedPushManifest, done);
    });
    test('with prefix', (done) => {
        const project = new polymer_project_1.PolymerProject({
            root: testProjectRoot,
            entrypoint: 'entrypoint-a.html',
            shell: 'shell.html',
            fragments: ['entrypoint-b.html'],
            sources: [
                'framework.html',
                'common-dependency.html',
            ],
        });
        const expectedPushManifest = {
            'foo/shell.html': {
                'foo/framework.html': {
                    type: 'document',
                    weight: 1,
                }
            },
            'foo/entrypoint-b.html': {
                'foo/common-dependency.html': {
                    type: 'document',
                    weight: 1,
                },
                'foo/example-script.js': {
                    type: 'script',
                    weight: 1,
                },
                'foo/example-style.css': {
                    type: 'style',
                    weight: 1,
                },
            },
        };
        testPushManifest(project, undefined, '/foo/', expectedPushManifest, done);
    });
});
//# sourceMappingURL=push-manifest_test.js.map