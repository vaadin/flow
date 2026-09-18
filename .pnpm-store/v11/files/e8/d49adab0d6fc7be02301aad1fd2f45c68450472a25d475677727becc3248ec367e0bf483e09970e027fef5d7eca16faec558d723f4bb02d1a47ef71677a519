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

import * as path from 'path';
import * as logging from 'plylog';
import {Analyzer, FsUrlResolver, PackageRelativeUrl, ResolvedUrl, Severity, UrlLoader, Warning, WarningFilter, WarningPrinter} from 'polymer-analyzer';
import {ProjectConfig} from 'polymer-project-config';
import {PassThrough, Transform} from 'stream';
import {src as vinylSrc} from 'vinyl-fs';

import {LocalFsPath, pathFromUrl, urlFromPath} from './path-transformers';
import {AsyncTransformStream, VinylReaderTransform} from './streams';

import File = require('vinyl');

const logger = logging.getLogger('cli.build.analyzer');

export interface DocumentDeps {
  imports: PackageRelativeUrl[];
  scripts: PackageRelativeUrl[];
  styles: PackageRelativeUrl[];
}

export interface DepsIndex {
  // An index of dependency -> fragments that depend on it
  depsToFragments: Map<PackageRelativeUrl, PackageRelativeUrl[]>;
  // TODO(garlicnation): Remove this map.
  // An index of fragments -> html dependencies
  fragmentToDeps: Map<PackageRelativeUrl, PackageRelativeUrl[]>;
  // A map from frament urls to html, js, and css dependencies.
  fragmentToFullDeps: Map<PackageRelativeUrl, DocumentDeps>;
}

/**
 * A stream that tells the BuildAnalyzer to resolve each file it sees. It's
 * important that files are resolved here in a seperate stream, so that analysis
 * and file loading/resolution can't block each other while waiting.
 */
class ResolveTransform extends AsyncTransformStream<File, File> {
  private _buildAnalyzer: BuildAnalyzer;

  constructor(buildAnalyzer: BuildAnalyzer) {
    super({objectMode: true});
    this._buildAnalyzer = buildAnalyzer;
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      this._buildAnalyzer.resolveFile(file);
      yield file;
    }
  }
}

/**
 * A stream to analyze every file that passes through it. This is used to
 * analyze important application fragments as they pass through the source
 * stream.
 *
 * We create a new stream to handle this because the alternative (attaching
 * event listeners directly to the existing sources stream) would
 * start the flow of data before the user was ready to consume it. By
 * analyzing inside of the stream instead of via "data" event listeners, the
 * source stream will remain paused until the user is ready to start the stream
 * themselves.
 */
class AnalyzeTransform extends AsyncTransformStream<File, File> {
  private _buildAnalyzer: BuildAnalyzer;

  constructor(buildAnalyzer: BuildAnalyzer) {
    // A high `highWaterMark` value is needed to keep this from pausing the
    // entire source stream.
    // TODO(fks) 02-02-2017: Move analysis out of the source stream itself so
    // that it no longer blocks during analysis.
    super({objectMode: true, highWaterMark: 10000});
    this._buildAnalyzer = buildAnalyzer;
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      await this._buildAnalyzer.analyzeFile(file);
      yield file;
    }
  }
}


export class BuildAnalyzer {
  config: ProjectConfig;
  loader: StreamLoader;
  analyzer: Analyzer;
  started: boolean = false;
  sourceFilesLoaded: boolean = false;

  private _sourcesStream!: NodeJS.ReadableStream;
  private _sourcesProcessingStream!: NodeJS.ReadWriteStream;
  private _dependenciesStream!: Transform;
  private _dependenciesProcessingStream!: NodeJS.ReadWriteStream;
  private _warningsFilter: WarningFilter;

  files = new Map<PackageRelativeUrl, File>();
  warnings = new Set<Warning>();
  allFragmentsToAnalyze: Set<LocalFsPath>;

  analyzeDependencies: Promise<DepsIndex>;
  _dependencyAnalysis: DepsIndex = {
    depsToFragments: new Map(),
    fragmentToDeps: new Map(),
    fragmentToFullDeps: new Map()
  };
  _resolveDependencyAnalysis!: (index: DepsIndex) => void;

  constructor(
      config: ProjectConfig,
      /** If null is given, we do not log warnings. */
      private readonly streamToWarnTo: (NodeJS.WriteStream|
                                        null) = process.stdout) {
    this.config = config;

    this.loader = new StreamLoader(this);
    this.analyzer = new Analyzer({
      urlLoader: this.loader,
      // TODO(usergenic): Add option to polymer-build to propagate a protocol
      // and host option to the FsUrlResolver.
      urlResolver: new FsUrlResolver(config.root),

      moduleResolution: config.moduleResolution === 'none' ?
          undefined :
          config.moduleResolution,
    });

    this.allFragmentsToAnalyze =
        new Set(this.config.allFragments.map((f) => f as LocalFsPath));
    this.analyzeDependencies = new Promise((resolve, _reject) => {
      this._resolveDependencyAnalysis = resolve;
    });

    const lintOptions: Partial<typeof config.lint> = (this.config.lint || {});

    const warningCodesToIgnore = new Set(lintOptions.ignoreWarnings || []);
    // These are expected, as we never want to load remote URLs like
    // `https://example.com/` when we're building
    warningCodesToIgnore.add('not-loadable');
    this._warningsFilter = new WarningFilter(
        {warningCodesToIgnore, minimumSeverity: Severity.WARNING});
  }

  /**
   * Start analysis by setting up the sources and dependencies analysis
   * pipelines and starting the source stream. Files will not be loaded from
   * disk until this is called. Can be called multiple times but will only run
   * set up once.
   */
  startAnalysis(): void {
    if (this.started) {
      return;
    }
    this.started = true;

    // Create the base streams for sources & dependencies to be read from.
    this._dependenciesStream = new PassThrough({objectMode: true});
    this._sourcesStream = vinylSrc(this.config.sources, {
      cwdbase: true,
      nodir: true,
    });

    // _sourcesProcessingStream: Pipe the sources stream through...
    //   1. The resolver stream, to resolve each file loaded via the analyzer
    //   2. The analyzer stream, to analyze app fragments for dependencies
    this._sourcesProcessingStream =
        this._sourcesStream
            .on('error',
                (err: Error) =>
                    this._sourcesProcessingStream.emit('error', err))
            .pipe(new ResolveTransform(this))
            .on('error',
                (err: Error) =>
                    this._sourcesProcessingStream.emit('error', err))
            .on('end', this.onSourcesStreamComplete.bind(this))
            .pipe(new AnalyzeTransform(this));

    // _dependenciesProcessingStream: Pipe the dependencies stream through...
    //   1. The vinyl loading stream, to load file objects from file paths
    //   2. The resolver stream, to resolve each loaded file for the analyzer
    this._dependenciesProcessingStream =
        this._dependenciesStream
            .on('error',
                (err: Error) =>
                    this._dependenciesProcessingStream.emit('error', err))
            .pipe(new VinylReaderTransform())
            .on('error',
                (err: Error) =>
                    this._dependenciesProcessingStream.emit('error', err))
            .pipe(new ResolveTransform(this));
  }

  /**
   * Return _dependenciesOutputStream, which will contain fully loaded file
   * objects for each dependency after analysis.
   */
  dependencies(): NodeJS.ReadableStream {
    this.startAnalysis();
    return this._dependenciesProcessingStream;
  }

  /**
   * Return _sourcesOutputStream, which will contain fully loaded file
   * objects for each source after analysis.
   */
  sources(): NodeJS.ReadableStream {
    this.startAnalysis();
    return this._sourcesProcessingStream;
  }

  /**
   * Resolve a file in our loader so that the analyzer can read it.
   */
  resolveFile(file: File) {
    const filePath = file.path as LocalFsPath;
    this.addFile(file);
    // If our resolver is waiting for this file, resolve its deferred loader
    if (this.loader.hasDeferredFile(filePath)) {
      this.loader.resolveDeferredFile(filePath, file);
    }
  }

  /**
   * Analyze a file to find additional dependencies to load. Currently we only
   * get dependencies for application fragments. When all fragments are
   * analyzed, we call _done() to signal that analysis is complete.
   */
  async analyzeFile(file: File): Promise<void> {
    const filePath = file.path as LocalFsPath;

    // If the file is a fragment, begin analysis on its dependencies
    if (this.config.isFragment(filePath)) {
      const deps = await this._getDependencies(this.analyzer.resolveUrl(
          urlFromPath(this.config.root as LocalFsPath, filePath))!);
      this._addDependencies(filePath, deps);
      this.allFragmentsToAnalyze.delete(filePath);
      // If there are no more fragments to analyze, we are done
      if (this.allFragmentsToAnalyze.size === 0) {
        this._done();
      }
    }
  }

  /**
   * Perform some checks once we know that `_sourcesStream` is done loading.
   */
  private onSourcesStreamComplete() {
    // Emit an error if there are missing source files still deferred. Otherwise
    // this would cause the analyzer to hang.
    for (const filePath of this.loader.deferredFiles.keys()) {
      if (this.config.isSource(filePath)) {
        const err = new Error(`Not found: ${filePath}`);
        this.loader.rejectDeferredFile(filePath, err);
      }
    }

    // Set sourceFilesLoaded so that future files aren't accidentally deferred
    this.sourceFilesLoaded = true;
  }

  /**
   * Helper function for emitting a general analysis error onto both file
   * streams.
   */
  private emitAnalysisError(err: Error) {
    this._sourcesProcessingStream.emit('error', err);
    this._dependenciesProcessingStream.emit('error', err);
  }

  /**
   * Called when analysis is complete and there are no more files to analyze.
   * Checks for serious errors before resolving its dependency analysis and
   * ending the dependency stream (which it controls).
   */
  private _done() {
    this.printWarnings();
    const allWarningCount = this.countWarningsByType();
    const errorWarningCount = allWarningCount.get(Severity.ERROR)!;

    // If any ERROR warnings occurred, propagate an error in each build stream.
    if (errorWarningCount > 0) {
      this.emitAnalysisError(
          new Error(`${errorWarningCount} error(s) occurred during build.`));
      return;
    }

    // If analysis somehow finished with files that still needed to be loaded,
    // propagate an error in each build stream.
    for (const filePath of this.loader.deferredFiles.keys()) {
      const err = new Error(`Not found: ${filePath}`);
      this.loader.rejectDeferredFile(filePath, err);
      return;
    }

    // Resolve our dependency analysis promise now that we have seen all files
    this._dependenciesStream.end();
    this._resolveDependencyAnalysis(this._dependencyAnalysis);
  }

  getFile(filepath: LocalFsPath): File|undefined {
    const url = urlFromPath(this.config.root as LocalFsPath, filepath);
    return this.getFileByUrl(url as PackageRelativeUrl);
  }

  getFileByUrl(url: PackageRelativeUrl): File|undefined {
    // TODO(usergenic): url carefulness bug, take an extra careful look at this.
    if (url.startsWith('/')) {
      url = url.substring(1) as PackageRelativeUrl;
    }
    return this.files.get(url);
  }

  /**
   * A side-channel to add files to the loader that did not come through the
   * stream transformation. This is for generated files, like
   * shared-bundle.html. This should probably be refactored so that the files
   * can be injected into the stream.
   */
  addFile(file: File): void {
    logger.debug(`addFile: ${file.path}`);
    // Badly-behaved upstream transformers (looking at you gulp-html-minifier)
    // may use posix path separators on Windows.
    const filepath = path.normalize(file.path) as LocalFsPath;
    // Store only root-relative paths, in URL/posix format
    this.files.set(
        urlFromPath(this.config.root as LocalFsPath, filepath), file);
  }

  printWarnings(): void {
    if (this.streamToWarnTo === null) {
      return;
    }
    const warningPrinter = new WarningPrinter(this.streamToWarnTo);
    warningPrinter.printWarnings(this.warnings);
  }

  private countWarningsByType(): Map<Severity, number> {
    const errorCountMap = new Map<Severity, number>();
    errorCountMap.set(Severity.INFO, 0);
    errorCountMap.set(Severity.WARNING, 0);
    errorCountMap.set(Severity.ERROR, 0);
    for (const warning of this.warnings) {
      errorCountMap.set(
          warning.severity, errorCountMap.get(warning.severity)! + 1);
    }
    return errorCountMap;
  }


  /**
   * Attempts to retreive document-order transitive dependencies for `url`.
   */
  async _getDependencies(url: ResolvedUrl): Promise<DocumentDeps> {
    const analysis = await this.analyzer.analyze([url]);
    const result = analysis.getDocument(url);

    if (result.successful === false) {
      const message = result.error && result.error.message || 'unknown';
      throw new Error(`Unable to get document ${url}: ${message}`);
    }

    const doc = result.value;
    doc.getWarnings({imported: true})
        .filter((w) => !this._warningsFilter.shouldIgnore(w))
        .forEach((w) => this.warnings.add(w));

    const scripts = new Set<PackageRelativeUrl>();
    const styles = new Set<PackageRelativeUrl>();
    const imports = new Set<PackageRelativeUrl>();

    const importFeatures = doc.getFeatures(
        {kind: 'import', externalPackages: true, imported: true});
    for (const importFeature of importFeatures) {
      const importUrl = importFeature.url;
      if (!this.analyzer.canLoad(importUrl)) {
        logger.debug(`ignoring external dependency: ${importUrl}`);
      } else if (importFeature.type === 'html-script') {
        scripts.add(this.analyzer.urlResolver.relative(importUrl));
      } else if (importFeature.type === 'html-style') {
        styles.add(this.analyzer.urlResolver.relative(importUrl));
      } else if (importFeature.type === 'html-import') {
        imports.add(this.analyzer.urlResolver.relative(importUrl));
      } else {
        logger.debug(
            `unexpected import type encountered: ${importFeature.type}`);
      }
    }

    const deps = {
      scripts: [...scripts],
      styles: [...styles],
      imports: [...imports],
    };
    logger.debug(`dependencies analyzed for: ${url}`, deps);
    return deps;
  }

  _addDependencies(filePath: LocalFsPath, deps: DocumentDeps) {
    // Make sure function is being called properly
    if (!this.allFragmentsToAnalyze.has(filePath)) {
      throw new Error(`Dependency analysis incorrectly called for ${filePath}`);
    }

    const relativeUrl = urlFromPath(this.config.root as LocalFsPath, filePath);
    // Add dependencies to _dependencyAnalysis object, and push them through
    // the dependency stream.
    this._dependencyAnalysis.fragmentToFullDeps.set(relativeUrl, deps);
    this._dependencyAnalysis.fragmentToDeps.set(relativeUrl, deps.imports);
    deps.imports.forEach((url) => {
      const entrypointList = this._dependencyAnalysis.depsToFragments.get(url);
      if (entrypointList) {
        entrypointList.push(relativeUrl);
      } else {
        this._dependencyAnalysis.depsToFragments.set(url, [relativeUrl]);
      }
    });
  }

  /**
   * Check that the source stream has not already completed loading by the
   * time
   * this file was analyzed.
   */
  sourcePathAnalyzed(filePath: LocalFsPath): void {
    // If we've analyzed a new path to a source file after the sources
    // stream has completed, we can assume that that file does not
    // exist. Reject with a "Not Found" error.
    if (this.sourceFilesLoaded) {
      throw new Error(`Not found: "${filePath}"`);
    }
    // Source files are loaded automatically through the vinyl source
    // stream. If it hasn't been seen yet, defer resolving until it has
    // been loaded by vinyl.
    logger.debug('dependency is a source file, ignoring...', {dep: filePath});
  }

  /**
   * Push the given filepath into the dependencies stream for loading.
   * Each dependency is only pushed through once to avoid duplicates.
   */
  dependencyPathAnalyzed(filePath: LocalFsPath): void {
    if (this.getFile(filePath)) {
      logger.debug(
          'dependency has already been pushed, ignoring...', {dep: filePath});
      return;
    }

    logger.debug(
        'new dependency analyzed, pushing into dependency stream...', filePath);
    this._dependenciesStream.push(filePath);
  }
}

export type ResolveFileCallback = (a: string) => void;
export type RejectFileCallback = (err: Error) => void;
export type DeferredFileCallbacks = {
  resolve: ResolveFileCallback; reject: RejectFileCallback;
};

export class StreamLoader implements UrlLoader {
  config: ProjectConfig;
  private _buildAnalyzer: BuildAnalyzer;

  // Store files that have not yet entered the Analyzer stream here.
  // Later, when the file is seen, the DeferredFileCallback can be
  // called with the file contents to resolve its loading.
  deferredFiles = new Map<LocalFsPath, DeferredFileCallbacks>();

  constructor(buildAnalyzer: BuildAnalyzer) {
    this._buildAnalyzer = buildAnalyzer;
    this.config = this._buildAnalyzer.config;
  }

  hasDeferredFile(filePath: LocalFsPath): boolean {
    return this.deferredFiles.has(filePath);
  }

  hasDeferredFiles(): boolean {
    return this.deferredFiles.size > 0;
  }

  resolveDeferredFile(filePath: LocalFsPath, file: File): void {
    const deferredCallbacks = this.deferredFiles.get(filePath);
    if (deferredCallbacks == null) {
      throw new Error(
          `Internal error: could not get deferredCallbacks for ${filePath}`);
    }
    deferredCallbacks.resolve(file.contents!.toString());
    this.deferredFiles.delete(filePath);
  }

  rejectDeferredFile(filePath: LocalFsPath, err: Error): void {
    const deferredCallbacks = this.deferredFiles.get(filePath);
    if (deferredCallbacks == null) {
      throw new Error(
          `Internal error: could not get deferredCallbacks for ${filePath}`);
    }
    deferredCallbacks.reject(err);
    this.deferredFiles.delete(filePath);
  }

  // We can't load external dependencies.
  canLoad(url: ResolvedUrl): boolean {
    return url.startsWith('file:///');
  }

  async load(url: ResolvedUrl): Promise<string> {
    logger.debug(`loading: ${url}`);
    if (!this.canLoad(url)) {
      throw new Error('Unable to load ${url}.');
    }

    const urlPath = this._buildAnalyzer.analyzer.urlResolver.relative(url);
    const filePath = pathFromUrl(this.config.root as LocalFsPath, urlPath);
    const file = this._buildAnalyzer.getFile(filePath);

    if (file) {
      return file.contents!.toString();
    }

    return new Promise(
        (resolve: ResolveFileCallback, reject: RejectFileCallback) => {
          this.deferredFiles.set(filePath, {resolve, reject});
          try {
            if (this.config.isSource(filePath)) {
              this._buildAnalyzer.sourcePathAnalyzed(filePath);
            } else {
              this._buildAnalyzer.dependencyPathAnalyzed(filePath);
            }
          } catch (err) {
            this.rejectDeferredFile(filePath, err);
          }
        });
  }
}
