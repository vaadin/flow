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
const chai_1 = require("chai");
const path = require("path");
const polymer_project_config_1 = require("polymer-project-config");
const sinon = require("sinon");
const stream_1 = require("stream");
const util_1 = require("./util");
const analyzer_1 = require("../analyzer");
const streams_1 = require("../streams");
/**
 * Streams will remain paused unless something is listening for it's data.
 * NoopStream is useful for piping to if you just want the stream to run and end
 * successfully without checking the data passed through it.
 */
class NoopStream extends stream_1.Writable {
    constructor() {
        super({ objectMode: true });
    }
    _write(_chunk, _encoding, callback) {
        callback();
    }
}
suite('Analyzer', () => {
    suite('DepsIndex', () => {
        test('fragment to deps list has only uniques', () => {
            const config = new polymer_project_config_1.ProjectConfig({
                root: `test-fixtures/analyzer-data`,
                entrypoint: 'entrypoint.html',
                fragments: [
                    'a.html',
                    'b.html',
                ],
                sources: ['a.html', 'b.html', 'entrypoint.html'],
            });
            const analyzer = new analyzer_1.BuildAnalyzer(config, null);
            analyzer.sources().pipe(new NoopStream());
            analyzer.dependencies().pipe(new NoopStream());
            return streams_1.waitForAll([analyzer.sources(), analyzer.dependencies()])
                .then(() => {
                return analyzer.analyzeDependencies;
            })
                .then((depsIndex) => {
                const ftd = depsIndex.fragmentToDeps;
                for (const frag of ftd.keys()) {
                    chai_1.assert.deepEqual(ftd.get(frag), ['shared-1.html', 'shared-2.html'].map((u) => u));
                }
            });
        });
        test('analyzing shell and entrypoint doesn\'t double load files', () => {
            const root = `test-fixtures/analyzer-data`;
            const sourceFiles = ['shell.html', 'entrypoint.html'].map((p) => path.resolve(root, p));
            const config = new polymer_project_config_1.ProjectConfig({
                root: root,
                entrypoint: 'entrypoint.html',
                shell: 'shell.html',
                sources: sourceFiles,
            });
            const analyzer = new analyzer_1.BuildAnalyzer(config, null);
            analyzer.sources().pipe(new NoopStream());
            analyzer.dependencies().pipe(new NoopStream());
            return streams_1.waitForAll([analyzer.sources(), analyzer.dependencies()])
                .then(() => {
                return analyzer.analyzeDependencies;
            })
                .then((depsIndex) => {
                chai_1.assert.isTrue(depsIndex.depsToFragments.has('shared-2.html'));
                chai_1.assert.isFalse(depsIndex.depsToFragments.has('shell.html'));
                chai_1.assert.isFalse(depsIndex.depsToFragments.has('shared-banana.html'));
            });
        });
    });
    suite('.dependencies', () => {
        test('outputs all dependencies needed by source', () => {
            const foundDependencies = new Set();
            const root = `test-fixtures/analyzer-data`;
            const sourceFiles = ['shell.html', 'entrypoint.html'].map((p) => path.resolve(root, p));
            const config = new polymer_project_config_1.ProjectConfig({
                root: root,
                entrypoint: 'entrypoint.html',
                shell: 'shell.html',
                sources: sourceFiles,
            });
            const analyzer = new analyzer_1.BuildAnalyzer(config, null);
            analyzer.sources().pipe(new NoopStream());
            analyzer.dependencies().on('data', (file) => {
                foundDependencies.add(file.path);
            });
            return streams_1.waitForAll([analyzer.sources(), analyzer.dependencies()])
                .then(() => {
                // shared-1 is never imported by shell/entrypoint, so it is not
                // included as a dep.
                chai_1.assert.isFalse(foundDependencies.has(path.resolve(root, 'shared-1.html')));
                // shared-2 is imported by shell, so it is included as a dep.
                chai_1.assert.isTrue(foundDependencies.has(path.resolve(root, 'shared-2.html')));
            });
        });
        test('outputs all dependencies needed by source and given fragments', () => {
            const foundDependencies = new Set();
            const root = `test-fixtures/analyzer-data`;
            const sourceFiles = ['a.html', 'b.html', 'shell.html', 'entrypoint.html'].map((p) => path.resolve(root, p));
            const config = new polymer_project_config_1.ProjectConfig({
                root: root,
                entrypoint: 'entrypoint.html',
                shell: 'shell.html',
                fragments: [
                    'a.html',
                    'b.html',
                ],
                sources: sourceFiles,
            });
            const analyzer = new analyzer_1.BuildAnalyzer(config, null);
            analyzer.sources().pipe(new NoopStream());
            analyzer.dependencies().on('data', (file) => {
                foundDependencies.add(file.path);
            });
            return streams_1.waitForAll([analyzer.sources(), analyzer.dependencies()])
                .then(() => {
                // shared-1 is imported by 'a' & 'b', so it is included as a
                // dep.
                chai_1.assert.isTrue(foundDependencies.has(path.resolve(root, 'shared-1.html')));
                // shared-1 is imported by 'a' & 'b', so it is included as a
                // dep.
                chai_1.assert.isTrue(foundDependencies.has(path.resolve(root, 'shared-2.html')));
            });
        });
    });
    test('propagates an error when a dependency filepath is analyzed but cannot be found', (done) => {
        const root = `test-fixtures/bad-src-import`;
        const config = new polymer_project_config_1.ProjectConfig({
            root: root,
            entrypoint: 'index.html',
            sources: ['src/**/*'],
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        let errorCounter = 0;
        const errorListener = (err) => {
            chai_1.assert.equal(err.message, '3 error(s) occurred during build.');
            errorCounter++;
            if (errorCounter >= 2) {
                done();
            }
        };
        analyzer.sources().pipe(new NoopStream());
        analyzer.sources().on('error', errorListener);
        analyzer.dependencies().pipe(new NoopStream());
        analyzer.dependencies().on('error', errorListener);
    });
    test('propagates an error when a source filepath is analyzed but cannot be found', (done) => {
        const root = `test-fixtures/bad-dependency-import`;
        const config = new polymer_project_config_1.ProjectConfig({
            root: root,
            entrypoint: 'index.html',
            sources: ['src/**/*'],
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        analyzer.dependencies().pipe(new NoopStream());
        analyzer.dependencies().on('error', (err) => {
            chai_1.assert.match(err.message, /ENOENT\: no such file or directory.*does\-not\-exist\-in\-dependencies\.html/);
            done();
        });
    });
    test('both file streams will emit a analysis warning of type "error"', (done) => {
        const root = path.resolve('test-fixtures/project-analysis-error');
        const sourceFiles = path.join(root, '**');
        const config = new polymer_project_config_1.ProjectConfig({
            root: root,
            sources: [sourceFiles],
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        let errorCounter = 0;
        const errorListener = (err) => {
            chai_1.assert.equal(err.message, '1 error(s) occurred during build.');
            errorCounter++;
            if (errorCounter >= 2) {
                done();
            }
        };
        analyzer.sources().pipe(new NoopStream());
        analyzer.sources().on('error', errorListener);
        analyzer.dependencies().pipe(new NoopStream());
        analyzer.dependencies().on('error', errorListener);
    });
    test('the analyzer stream will log all analysis warnings at the end of the stream', () => {
        const root = path.resolve('test-fixtures/project-analysis-error');
        const sourceFiles = path.join(root, '**');
        const config = new polymer_project_config_1.ProjectConfig({
            root: root,
            sources: [sourceFiles],
        });
        let prematurePrintWarnings = false;
        const prematurePrintWarningsCheck = () => prematurePrintWarnings =
            prematurePrintWarnings ||
                analyzer.allFragmentsToAnalyze.size > 0 && printWarningsSpy.called;
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        const printWarningsSpy = sinon.spy(analyzer, 'printWarnings');
        analyzer.sources().on('data', prematurePrintWarningsCheck);
        analyzer.dependencies().on('data', prematurePrintWarningsCheck);
        return streams_1.waitForAll([analyzer.sources(), analyzer.dependencies()])
            .then(() => {
            throw new Error('Parse error expected!');
        }, (_err) => {
            chai_1.assert.isFalse(prematurePrintWarnings);
            chai_1.assert.isTrue(printWarningsSpy.calledOnce);
        });
    });
    test('analyzer filters warnings', () => {
        const root = path.resolve('test-fixtures/project-analysis-warning');
        const sources = [path.join(root, '**')];
        const config = new polymer_project_config_1.ProjectConfig({
            root,
            sources,
            lint: { rules: ['polymer-2'], ignoreWarnings: ['invalid-polymer-expression'] }
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        analyzer.sources().pipe(new NoopStream());
        return streams_1.waitFor(analyzer.sources()).then(() => {
            chai_1.assert.isTrue(analyzer.warnings.size === 0);
        });
    });
    test('calling sources() starts analysis', () => __awaiter(this, void 0, void 0, function* () {
        const config = new polymer_project_config_1.ProjectConfig({
            root: `test-fixtures/analyzer-data`,
            entrypoint: 'entrypoint.html',
            fragments: [
                'a.html',
                'b.html',
            ],
            sources: ['a.html', 'b.html', 'entrypoint.html'],
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        chai_1.assert.isFalse(analyzer.started);
        analyzer.sources().pipe(new NoopStream());
        chai_1.assert.isTrue(analyzer.started);
    }));
    test('calling dependencies() starts analysis', () => {
        const config = new polymer_project_config_1.ProjectConfig({
            root: `test-fixtures/analyzer-data`,
            entrypoint: 'entrypoint.html',
            fragments: [
                'a.html',
                'b.html',
            ],
            sources: ['a.html', 'b.html', 'entrypoint.html'],
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        chai_1.assert.isFalse(analyzer.started);
        analyzer.dependencies().pipe(new NoopStream());
        chai_1.assert.isTrue(analyzer.started);
    });
    test('the source/dependency streams remain paused until use', () => {
        const config = new polymer_project_config_1.ProjectConfig({
            root: `test-fixtures/analyzer-data`,
            entrypoint: 'entrypoint.html',
            fragments: [
                'a.html',
                'b.html',
            ],
            sources: ['a.html', 'b.html', 'entrypoint.html'],
        });
        const analyzer = new analyzer_1.BuildAnalyzer(config, null);
        // Cast analyzer to <any> so that we can check private properties of it.
        // We need to access these private streams directly because the public
        // `sources()` and `dependencies()` functions have intentional side effects
        // related to these streams that we are trying to test here.
        // tslint:disable-next-line: no-any
        const analyzerWithPrivates = analyzer;
        chai_1.assert.isUndefined(analyzerWithPrivates._sourcesStream);
        chai_1.assert.isUndefined(analyzerWithPrivates._dependenciesStream);
        analyzerWithPrivates.sources();
        chai_1.assert.isDefined(analyzerWithPrivates._sourcesStream);
        chai_1.assert.isDefined(analyzerWithPrivates._dependenciesStream);
        chai_1.assert.isTrue(util_1.getFlowingState(analyzerWithPrivates._sourcesStream));
        chai_1.assert.isTrue(util_1.getFlowingState(analyzerWithPrivates._dependenciesStream));
        // Check that even though `sources()` has been called, the public file
        // streams aren't flowing until data listeners are attached (directly or via
        // piping) so that files are never lost).
        chai_1.assert.isNull(util_1.getFlowingState(analyzer.sources()));
        chai_1.assert.isNull(util_1.getFlowingState(analyzer.dependencies()));
        analyzer.sources().on('data', () => { });
        chai_1.assert.isTrue(util_1.getFlowingState(analyzer.sources()));
        chai_1.assert.isNull(util_1.getFlowingState(analyzer.dependencies()));
        analyzer.dependencies().pipe(new NoopStream());
        chai_1.assert.isTrue(util_1.getFlowingState(analyzer.sources()));
        chai_1.assert.isTrue(util_1.getFlowingState(analyzer.dependencies()));
    });
    // TODO(fks) 10-26-2016: Refactor logging to be testable, and configurable by
    // the consumer.
    suite.skip('.printWarnings()', () => { });
});
//# sourceMappingURL=analyzer_test.js.map