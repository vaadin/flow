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
import File = require('vinyl');
import { AsyncTransformStream } from './streams';
import { LocalFsPath } from './path-transformers';
/**
 * Find a `<base>` tag in the specified file and if found, update its `href`
 * with the given new value.
 */
export declare class BaseTagUpdater extends AsyncTransformStream<File, File> {
    private filePath;
    private newHref;
    constructor(filePath: LocalFsPath, newHref: string);
    protected _transformIter(files: AsyncIterable<File>): AsyncIterable<File>;
}
