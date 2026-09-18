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

/**
 * CODE ADAPTED FROM THE "SLASH" LIBRARY BY SINDRE SORHUS
 * https://github.com/sindresorhus/slash
 *
 * ORIGINAL LICENSE:
 * The MIT License (MIT)
 *
 * Copyright (c) Sindre Sorhus <sindresorhus@gmail.com> (sindresorhus.com)*
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy*
 * of this software and associated documentation files (the "Software"), to
 * deal*
 * in the Software without restriction, including without limitation the rights*
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell*
 * copies of the Software, and to permit persons to whom the Software is*
 * furnished to do so, subject to the following conditions:*
 *
 * The above copyright notice and this permission notice shall be included in*
 * all copies or substantial portions of the Software.*
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,*
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE*
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN*
 * THE SOFTWARE.
 */

/**
 * This module consists of functions for transformations to filesystem and url
 * paths.
 * TODO(usergenic): We should consider migrating the responsibility of
 * path-related string transformation to a package like `upath`.
 * Please see: https://www.npmjs.com/package/upath
 */

import * as path from 'path';
import {PackageRelativeUrl} from 'polymer-analyzer';

export declare class LocalFsPathBrand {
  private LocalFsPathBrand: never;
}
export type LocalFsPath = string&LocalFsPathBrand;
export declare class PosixPathBrand {
  private PosixPathBrand: never;
}
export type PosixPath = string&PosixPathBrand;

/**
 * Returns a properly encoded URL representing the relative URL from the root
 * to the target.  This function will throw an error if the target is outside
 * the root.  We use this to map a file from the filesystem to the relative
 * URL that represents it in the build.
 */
export function urlFromPath(
    root: LocalFsPath, target: LocalFsPath): PackageRelativeUrl {
  const targetPosix = posixifyPath(target);
  const rootPosix = posixifyPath(root);

  const relativePath = path.posix.relative(rootPosix, targetPosix);

  // The startsWith(root) check is important on Windows because of the case
  // where paths have different drive letters.  The startsWith('../') will
  // catch the general not-in-root case.
  if (!targetPosix.startsWith(posixifyPath(root)) ||
      relativePath.startsWith('../')) {
    throw new Error(`target path is not in root: ${target} (${root})`);
  }

  return encodeURI(relativePath) as PackageRelativeUrl;
}

/**
 * Returns a filesystem path for the url, relative to the root.
 */
export function pathFromUrl(
    root: LocalFsPath,
    // TODO(usergenic): PackageRelativeUrl are not *necessarily* always just a
    // relative path from root.  Maybe subclass as PackageRelativeUrlPath or
    // something if this function doesn't disappear after
    // https://github.com/Polymer/polymer-build/issues/324 is addressed.
    url: PackageRelativeUrl): LocalFsPath {
  return path.normalize(decodeURIComponent(path.posix.join(
             posixifyPath(root), path.posix.join('/', url)))) as LocalFsPath;
}

/**
 * Returns a string where all Windows path separators are converted to forward
 * slashes.
 * NOTE(usergenic): We will generate only canonical Windows paths, but this
 * function is exported so that we can create a forward-slashed Windows root
 * path when dealing with the `sw-precache` library, which uses `glob` npm
 * module generates only forward-slash paths in building its `precacheConfig`
 * map.
 */
export function posixifyPath(filepath: LocalFsPath): PosixPath {
  // We don't want to change backslashes to forward-slashes in the case where
  // we're already on posix environment, because they would be intentional in
  // that case (albeit weird.)
  if (path.sep === '\\') {
    filepath = filepath.replace(/\\/g, '/') as string as LocalFsPath;
  }
  return filepath as string as PosixPath;
}
