/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
import { AsyncTransformStream } from './streams';
import File = require('vinyl');
/**
 * When compiling to ES5 we need to inject Babel's helpers into a global so
 * that they don't need to be included with each compiled file.
 */
export declare class BabelHelpersInjector extends AsyncTransformStream<File, File> {
    private entrypoint;
    constructor(entrypoint: string);
    protected _transformIter(files: AsyncIterable<File>): AsyncIterableIterator<File>;
    private processFile;
}
