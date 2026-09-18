/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
import { ResolvedUrl } from '../index';
import { Result } from '../model/analysis';
import { PackageRelativeUrl } from '../model/url';
import { UrlLoader } from './url-loader';
/**
 * Resolves requests via the file system.
 */
export declare class FsUrlLoader implements UrlLoader {
    root: string;
    constructor(root?: string);
    canLoad(url: ResolvedUrl): boolean;
    load(url: ResolvedUrl): Promise<string>;
    /**
     * If successful, result.value will be the filesystem path that we would load
     * the given url from.
     *
     * If unsuccessful, result.value will be an error message as a string.
     */
    getFilePath(url: ResolvedUrl): Result<string, string>;
    readDirectory(pathFromRoot: string, deep?: boolean): Promise<PackageRelativeUrl[]>;
}
