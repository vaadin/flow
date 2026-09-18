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
/// <reference types="node" />
import { Analyzer, PackageRelativeUrl, ResolvedUrl, UrlLoader, Warning } from 'polymer-analyzer';
import { ProjectConfig } from 'polymer-project-config';
import { LocalFsPath } from './path-transformers';
import File = require('vinyl');
export interface DocumentDeps {
    imports: PackageRelativeUrl[];
    scripts: PackageRelativeUrl[];
    styles: PackageRelativeUrl[];
}
export interface DepsIndex {
    depsToFragments: Map<PackageRelativeUrl, PackageRelativeUrl[]>;
    fragmentToDeps: Map<PackageRelativeUrl, PackageRelativeUrl[]>;
    fragmentToFullDeps: Map<PackageRelativeUrl, DocumentDeps>;
}
export declare class BuildAnalyzer {
    /** If null is given, we do not log warnings. */
    private readonly streamToWarnTo;
    config: ProjectConfig;
    loader: StreamLoader;
    analyzer: Analyzer;
    started: boolean;
    sourceFilesLoaded: boolean;
    private _sourcesStream;
    private _sourcesProcessingStream;
    private _dependenciesStream;
    private _dependenciesProcessingStream;
    private _warningsFilter;
    files: Map<PackageRelativeUrl, File>;
    warnings: Set<Warning>;
    allFragmentsToAnalyze: Set<LocalFsPath>;
    analyzeDependencies: Promise<DepsIndex>;
    _dependencyAnalysis: DepsIndex;
    _resolveDependencyAnalysis: (index: DepsIndex) => void;
    constructor(config: ProjectConfig, 
    /** If null is given, we do not log warnings. */
    streamToWarnTo?: (NodeJS.WriteStream | null));
    /**
     * Start analysis by setting up the sources and dependencies analysis
     * pipelines and starting the source stream. Files will not be loaded from
     * disk until this is called. Can be called multiple times but will only run
     * set up once.
     */
    startAnalysis(): void;
    /**
     * Return _dependenciesOutputStream, which will contain fully loaded file
     * objects for each dependency after analysis.
     */
    dependencies(): NodeJS.ReadableStream;
    /**
     * Return _sourcesOutputStream, which will contain fully loaded file
     * objects for each source after analysis.
     */
    sources(): NodeJS.ReadableStream;
    /**
     * Resolve a file in our loader so that the analyzer can read it.
     */
    resolveFile(file: File): void;
    /**
     * Analyze a file to find additional dependencies to load. Currently we only
     * get dependencies for application fragments. When all fragments are
     * analyzed, we call _done() to signal that analysis is complete.
     */
    analyzeFile(file: File): Promise<void>;
    /**
     * Perform some checks once we know that `_sourcesStream` is done loading.
     */
    private onSourcesStreamComplete;
    /**
     * Helper function for emitting a general analysis error onto both file
     * streams.
     */
    private emitAnalysisError;
    /**
     * Called when analysis is complete and there are no more files to analyze.
     * Checks for serious errors before resolving its dependency analysis and
     * ending the dependency stream (which it controls).
     */
    private _done;
    getFile(filepath: LocalFsPath): File | undefined;
    getFileByUrl(url: PackageRelativeUrl): File | undefined;
    /**
     * A side-channel to add files to the loader that did not come through the
     * stream transformation. This is for generated files, like
     * shared-bundle.html. This should probably be refactored so that the files
     * can be injected into the stream.
     */
    addFile(file: File): void;
    printWarnings(): void;
    private countWarningsByType;
    /**
     * Attempts to retreive document-order transitive dependencies for `url`.
     */
    _getDependencies(url: ResolvedUrl): Promise<DocumentDeps>;
    _addDependencies(filePath: LocalFsPath, deps: DocumentDeps): void;
    /**
     * Check that the source stream has not already completed loading by the
     * time
     * this file was analyzed.
     */
    sourcePathAnalyzed(filePath: LocalFsPath): void;
    /**
     * Push the given filepath into the dependencies stream for loading.
     * Each dependency is only pushed through once to avoid duplicates.
     */
    dependencyPathAnalyzed(filePath: LocalFsPath): void;
}
export declare type ResolveFileCallback = (a: string) => void;
export declare type RejectFileCallback = (err: Error) => void;
export declare type DeferredFileCallbacks = {
    resolve: ResolveFileCallback;
    reject: RejectFileCallback;
};
export declare class StreamLoader implements UrlLoader {
    config: ProjectConfig;
    private _buildAnalyzer;
    deferredFiles: Map<LocalFsPath, DeferredFileCallbacks>;
    constructor(buildAnalyzer: BuildAnalyzer);
    hasDeferredFile(filePath: LocalFsPath): boolean;
    hasDeferredFiles(): boolean;
    resolveDeferredFile(filePath: LocalFsPath, file: File): void;
    rejectDeferredFile(filePath: LocalFsPath, err: Error): void;
    canLoad(url: ResolvedUrl): boolean;
    load(url: ResolvedUrl): Promise<string>;
}
