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
import { PackageRelativeUrl } from 'polymer-analyzer';
export declare class LocalFsPathBrand {
    private LocalFsPathBrand;
}
export declare type LocalFsPath = string & LocalFsPathBrand;
export declare class PosixPathBrand {
    private PosixPathBrand;
}
export declare type PosixPath = string & PosixPathBrand;
/**
 * Returns a properly encoded URL representing the relative URL from the root
 * to the target.  This function will throw an error if the target is outside
 * the root.  We use this to map a file from the filesystem to the relative
 * URL that represents it in the build.
 */
export declare function urlFromPath(root: LocalFsPath, target: LocalFsPath): PackageRelativeUrl;
/**
 * Returns a filesystem path for the url, relative to the root.
 */
export declare function pathFromUrl(root: LocalFsPath, url: PackageRelativeUrl): LocalFsPath;
/**
 * Returns a string where all Windows path separators are converted to forward
 * slashes.
 * NOTE(usergenic): We will generate only canonical Windows paths, but this
 * function is exported so that we can create a forward-slashed Windows root
 * path when dealing with the `sw-precache` library, which uses `glob` npm
 * module generates only forward-slash paths in building its `precacheConfig`
 * map.
 */
export declare function posixifyPath(filepath: LocalFsPath): PosixPath;
