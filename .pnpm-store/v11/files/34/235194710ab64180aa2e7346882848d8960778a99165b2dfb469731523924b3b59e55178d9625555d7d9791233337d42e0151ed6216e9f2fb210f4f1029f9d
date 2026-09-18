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
import { PackageRelativeUrl } from 'polymer-analyzer';
import { ProjectConfig, ProjectOptions } from 'polymer-project-config';
import { BuildAnalyzer } from './analyzer';
import { BuildBundler, Options as BuildBundlerOptions } from './bundle';
import { LocalFsPath } from './path-transformers';
export declare class PolymerProject {
    config: ProjectConfig;
    /**
     * A `Transform` stream that uses polymer-analyzer to analyze the files. It
     * can be used to get information on dependencies and fragments for the
     * project once the source & dependency streams have been piped into it.
     */
    analyzer: BuildAnalyzer;
    constructor(config: ProjectConfig | ProjectOptions | string);
    /**
     * Returns a `Transform` stream that modifies the files that pass through it
     * based on the dependency analysis done by the `analyzer` transform. It
     * "bundles" a project by injecting its dependencies into the application
     * fragments themselves, so that a minimum number of requests need to be made
     * to load.
     *
     * (NOTE: The analyzer stream must be in the pipeline somewhere before this.)
     */
    bundler(options?: BuildBundlerOptions): BuildBundler;
    /**
     * Returns the analyzer's stream of this project's source files - files
     * matched by the project's `config.sources` value.
     */
    sources(): NodeJS.ReadableStream;
    /**
     * Returns the analyzer's stream of this project's dependency files - files
     * loaded inside the analyzed project that are not considered source files.
     */
    dependencies(): NodeJS.ReadableStream;
    /**
     * Returns a stream transformer that injects 'prefetch' link tags into HTML
     * documents based on the transitive dependencies of the document.
     * For entrypoint documents without `<base>` tag, absolute urls are used in
     * prefetch link hrefs.  In all other cases, link hrefs will be relative urls.
     */
    addPrefetchLinks(): NodeJS.ReadWriteStream;
    /**
     * Returns a stream transformer that adds a push manifest file to the set
     * of all input files that pass through.
     */
    addPushManifest(outPath?: LocalFsPath, basePath?: PackageRelativeUrl): NodeJS.ReadWriteStream;
    /**
     * Returns a stream transformer that injects `custom-elements-es5-adapter.js`
     * into the entry point HTML file. This adapter is needed when serving ES5
     * to browsers that support the native Custom Elements API.
     */
    addCustomElementsEs5Adapter(): NodeJS.ReadWriteStream;
    addBabelHelpersInEntrypoint(entrypoint?: string): NodeJS.ReadWriteStream;
    /**
     * Return a stream transformer that updates the `<base>` tag of the project's
     * entrypoint HTML file with the given new value. No change is made if a
     * `<base>` tag does not already exist.
     */
    updateBaseTag(baseHref: string): NodeJS.ReadWriteStream;
}
