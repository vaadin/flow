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
import File = require('vinyl');
import { ResolvedUrl } from 'polymer-analyzer';
import { Options } from 'polymer-bundler';
import { ProjectConfig } from 'polymer-project-config';
import { BuildAnalyzer } from './analyzer';
import { AsyncTransformStream } from './streams';
export { Options } from 'polymer-bundler';
export declare class BuildBundler extends AsyncTransformStream<File, File> {
    config: ProjectConfig;
    private _buildAnalyzer;
    private _bundler;
    files: Map<ResolvedUrl, File>;
    constructor(config: ProjectConfig, buildAnalyzer: BuildAnalyzer, options?: Options);
    protected _transformIter(files: AsyncIterable<File>): AsyncIterable<File>;
    private _buildBundles;
    private _generateBundleManifest;
    private _getFilesChangedSinceInitialAnalysis;
    private _mapFile;
    /**
     * Removes all of the inlined files in a bundle manifest from the filemap.
     */
    private _unmapBundledFiles;
}
