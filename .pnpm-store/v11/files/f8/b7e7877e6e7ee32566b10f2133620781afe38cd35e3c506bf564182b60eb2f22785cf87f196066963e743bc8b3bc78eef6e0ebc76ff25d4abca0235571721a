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
import { PackageRelativeUrl, ResolvedUrl } from '../model/url';
import { UrlLoader } from './url-loader';
/**
 * Resolves requests first from an in-memory map of file contents, and if a
 * file isn't found there, defers to another url loader.
 *
 * Useful for the editor use case. An editor will have a number of files in open
 * buffers at any time. For these files, the editor's in-memory buffer is
 * canonical, so that their contents are read even when they have unsaved
 * changes. For all other files, we can load the files using another loader,
 * e.g. from disk.
 *
 * TODO(rictic): make this a mixin that mixes another loader.
 */
export declare class InMemoryOverlayUrlLoader implements UrlLoader {
    private readonly _fallbackLoader;
    urlContentsMap: Map<ResolvedUrl, string>;
    constructor(fallbackLoader?: UrlLoader);
    canLoad(url: ResolvedUrl): boolean;
    load(url: ResolvedUrl): Promise<string>;
    readDirectory?: (pathFromRoot: string, deep?: boolean) => Promise<PackageRelativeUrl[]>;
}
