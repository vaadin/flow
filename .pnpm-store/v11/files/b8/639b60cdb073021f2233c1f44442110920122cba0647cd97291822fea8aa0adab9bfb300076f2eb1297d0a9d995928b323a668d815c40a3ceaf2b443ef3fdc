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
import { PackageRelativeUrl, ResolvedUrl } from 'polymer-analyzer';
import { ProjectConfig } from 'polymer-project-config';
import File = require('vinyl');
import { LocalFsPath } from './path-transformers';
import { AsyncTransformStream } from './streams';
/**
 * Push Manifest Types Definitions
 * A push manifest is a JSON object representing relative application URL and
 * the resources that should be pushed when those URLs are requested by the
 * server. Below is a example of this data format:
 *
 *       {
 *         "index.html": {       // PushManifestEntryCollection
 *           "/css/app.css": {   // PushManifestEntry
 *             "type": "style",  // ResourceType
 *             "weight": 1
 *           },
 *           ...
 *         },
 *         "page.html": {
 *           "/css/page.css": {
 *             "type": "style",
 *             "weight": 1
 *           },
 *           ...
 *         }
 *       }
 *
 * NOTE(fks) 04-05-2017: Only weight=1 is supported by browsers at the moment.
 * When support is added, we can add automatic weighting and support multiple
 * numbers.
 */
export declare type ResourceType = 'document' | 'script' | 'style' | 'image' | 'font';
export interface PushManifestEntry {
    type?: ResourceType;
    weight?: 1;
}
export interface PushManifestEntryCollection {
    [dependencyAbsoluteUrl: string]: PushManifestEntry;
}
export interface PushManifest {
    [requestAbsoluteUrl: string]: PushManifestEntryCollection;
}
/**
 * A stream that reads in files from an application to generate an HTTP2/Push
 * manifest that gets injected into the stream.
 */
export declare class AddPushManifest extends AsyncTransformStream<File, File> {
    files: Map<ResolvedUrl, File>;
    outPath: LocalFsPath;
    private config;
    private analyzer;
    private basePath;
    constructor(config: ProjectConfig, outPath?: LocalFsPath, basePath?: PackageRelativeUrl);
    protected _transformIter(files: AsyncIterable<File>): AsyncIterable<File>;
    generatePushManifest(): Promise<PushManifest>;
}
