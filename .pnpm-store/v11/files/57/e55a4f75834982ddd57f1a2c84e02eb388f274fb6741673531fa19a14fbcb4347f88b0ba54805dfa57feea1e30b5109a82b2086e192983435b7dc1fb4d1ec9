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
import { PackageRelativeUrl, ResolvedUrl } from '../model/url';
/**
 * An object that reads files.
 */
export interface UrlLoader {
    /**
     * Returns `true` if this loader can load the given `url`.
     */
    canLoad(url: ResolvedUrl): boolean;
    /**
     * Reads a file from `url`.
     *
     * This should only be called if `canLoad` returns `true` for `url`.
     */
    load(url: ResolvedUrl): Promise<string>;
    /**
     * Lists files in a directory in the current project.
     *
     * @param path A relative path to a directory to read.
     * @param deep If true, lists files recursively. Returned paths are
     *     relative to `url`.
     */
    readDirectory?(path: ResolvedUrl, deep?: boolean): Promise<PackageRelativeUrl[]>;
}
