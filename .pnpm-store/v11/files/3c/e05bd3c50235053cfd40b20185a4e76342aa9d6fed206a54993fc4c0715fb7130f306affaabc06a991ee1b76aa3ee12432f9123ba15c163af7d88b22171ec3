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
Object.defineProperty(exports, "__esModule", { value: true });
// TODO Migrate to async tests.
const chai_1 = require("chai");
const fs = require("mz/fs");
const path = require("path");
const vfs = require("vinyl-fs");
const polymer_project_1 = require("../polymer-project");
const serviceWorker = require("../service-worker");
const temp = require('temp').track();
const mergeStream = require('merge-stream');
suite('service-worker', () => {
    let testBuildRoot;
    let defaultProject;
    setup((done) => {
        defaultProject = new polymer_project_1.PolymerProject({
            root: path.resolve('test-fixtures/test-project'),
            entrypoint: 'index.html',
            shell: 'shell.html',
            sources: [
                'source-dir/**',
            ],
        });
        temp.mkdir('polymer-build-test', (err, dir) => {
            if (err || dir === undefined) {
                return done(err || 'no dir given');
            }
            testBuildRoot = dir;
            vfs.src(path.join('test-fixtures/test-project/**'))
                .pipe(vfs.dest(dir))
                .on('end', () => {
                mergeStream(defaultProject.sources(), defaultProject.dependencies())
                    .pipe(vfs.dest(testBuildRoot))
                    .on('end', () => done())
                    .on('error', done);
            });
        });
    });
    teardown((done) => {
        temp.cleanup(done);
    });
    suite('hasNoFileExtension regexp', () => {
        test('matches URL paths correctly', () => {
            const test = (s) => serviceWorker.hasNoFileExtension.test(s);
            chai_1.assert.isTrue(test('/'));
            chai_1.assert.isTrue(test('/foo'));
            chai_1.assert.isTrue(test('/foo/'));
            chai_1.assert.isTrue(test('/foo.png/bar/'));
            chai_1.assert.isTrue(test('/foo?baz.png'));
            chai_1.assert.isFalse(test('/foo.png'));
            chai_1.assert.isFalse(test('/foo/bar.png'));
        });
    });
    suite('generateServiceWorkerConfig()', () => {
        test('should set entrypoint related options', () => __awaiter(this, void 0, void 0, function* () {
            const config = yield serviceWorker.generateServiceWorkerConfig({
                project: defaultProject,
                buildRoot: testBuildRoot,
            });
            chai_1.assert.equal(config.navigateFallback, 'index.html');
            chai_1.assert.deepEqual(config.navigateFallbackWhitelist, [serviceWorker.hasNoFileExtension]);
            chai_1.assert.equal(config.directoryIndex, '');
        }));
    });
    suite('generateServiceWorker()', () => {
        test('should throw when options are not provided', () => {
            // tslint:disable-next-line: no-any testing type unsafe code
            return serviceWorker.generateServiceWorker().then(() => {
                chai_1.assert.fail('generateServiceWorker() resolved, expected rejection!');
            }, (error) => {
                chai_1.assert.include(error.name, 'AssertionError');
                chai_1.assert.equal(error.message, '`project` & `buildRoot` options are required');
            });
        });
        test('should throw when options.project is not provided', () => {
            // tslint:disable-next-line: no-any testing type unsafe code
            const unsafeForm = serviceWorker.generateServiceWorker;
            return unsafeForm({ buildRoot: testBuildRoot })
                .then(() => {
                chai_1.assert.fail('generateServiceWorker() resolved, expected rejection!');
            }, (error) => {
                chai_1.assert.include(error.name, 'AssertionError');
                chai_1.assert.equal(error.message, '`project` option is required');
            });
        });
        test('should throw when options.buildRoot is not provided', () => {
            // tslint:disable-next-line: no-any testing type unsafe code
            const unsafeForm = serviceWorker.generateServiceWorker;
            return unsafeForm({ project: defaultProject })
                .then(() => {
                chai_1.assert.fail('generateServiceWorker() resolved, expected rejection!');
            }, (error) => {
                chai_1.assert.include(error.name, 'AssertionError');
                chai_1.assert.equal(error.message, '`buildRoot` option is required');
            });
        });
        test('should not modify the options object provided when called', () => {
            const swPrecacheConfig = { staticFileGlobs: [] };
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
                swPrecacheConfig: swPrecacheConfig,
            })
                .then(() => {
                chai_1.assert.equal(swPrecacheConfig.staticFileGlobs.length, 0);
            });
        });
        test('should resolve with a Buffer representing the generated service worker code', () => {
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
            })
                .then((swCode) => {
                chai_1.assert.ok(swCode instanceof Buffer);
            });
        });
        test('should add unbundled precached assets when options.unbundled is not provided', () => {
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
            })
                .then((swFile) => {
                const fileContents = swFile.toString();
                chai_1.assert.include(fileContents, '"index.html"');
                chai_1.assert.include(fileContents, '"shell.html"');
                chai_1.assert.include(fileContents, '"bower_components/dep.html"');
                chai_1.assert.notInclude(fileContents, '"source-dir/my-app.html"');
            });
        });
        test('should add bundled precached assets when options.bundled is provided', () => {
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
                bundled: true,
            })
                .then((swFile) => {
                const fileContents = swFile.toString();
                chai_1.assert.include(fileContents, '"index.html"');
                chai_1.assert.include(fileContents, '"shell.html"');
                chai_1.assert.notInclude(fileContents, '"bower_components/dep.html"');
                chai_1.assert.notInclude(fileContents, '"source-dir/my-app.html"');
            });
        });
        test('should add provided staticFileGlobs paths to the final list', () => {
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
                bundled: true,
                swPrecacheConfig: {
                    staticFileGlobs: ['/bower_components/dep.html'],
                },
            })
                .then((swFile) => {
                const fileContents = swFile.toString();
                chai_1.assert.include(fileContents, '"index.html"');
                chai_1.assert.include(fileContents, '"shell.html"');
                chai_1.assert.include(fileContents, '"bower_components/dep.html"');
                chai_1.assert.notInclude(fileContents, '"source-dir/my-app.html"');
            });
        });
        test('basePath should prefix resources', () => {
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
                basePath: '/my/base/path',
            })
                .then((swFile) => {
                const fileContents = swFile.toString();
                chai_1.assert.include(fileContents, '"/my/base/path/index.html"');
            });
        });
        test('basePath prefixes should not have double delimiters', () => {
            return serviceWorker
                .generateServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
                basePath: '/my/base/path/',
            })
                .then((swFile) => {
                const fileContents = swFile.toString();
                chai_1.assert.include(fileContents, '"/my/base/path/index.html"');
                chai_1.assert.notInclude(fileContents, '"/my/base/path//index.html"');
            });
        });
    });
    suite('addServiceWorker()', () => {
        test('should write generated service worker to file system', () => {
            return serviceWorker
                .addServiceWorker({
                project: defaultProject,
                buildRoot: testBuildRoot,
            })
                .then(() => {
                const content = fs.readFileSync(path.join(testBuildRoot, 'service-worker.js'), 'utf-8');
                chai_1.assert.include(content, '// This generated service worker JavaScript will precache your site\'s resources.');
            });
        });
    });
});
//# sourceMappingURL=service-worker_test.js.map