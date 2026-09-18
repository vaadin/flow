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
import { ResolvedUrl, UrlResolver } from 'polymer-analyzer';
import { ProjectConfig } from 'polymer-project-config';
import File = require('vinyl');
import { AsyncTransformStream } from './streams';
/**
 * A stream that modifies HTML files to include prefetch links for all of the
 * file's transitive dependencies.
 */
export declare class AddPrefetchLinks extends AsyncTransformStream<File, File> {
    files: Map<ResolvedUrl, File>;
    private _analyzer;
    private _config;
    constructor(config: ProjectConfig);
    protected _transformIter(files: AsyncIterable<File>): AsyncIterable<File>;
}
/**
 * Returns the given HTML updated with import or prefetch links for the given
 * dependencies. The given url and deps are expected to be project-relative
 * URLs (e.g. "index.html" or "src/view.html") unless absolute parameter is
 * `true` and there is no base tag in the document.
 */
export declare function createLinks(urlResolver: UrlResolver, html: string, baseUrl: ResolvedUrl, deps: Set<ResolvedUrl>, absolute?: boolean): string;
