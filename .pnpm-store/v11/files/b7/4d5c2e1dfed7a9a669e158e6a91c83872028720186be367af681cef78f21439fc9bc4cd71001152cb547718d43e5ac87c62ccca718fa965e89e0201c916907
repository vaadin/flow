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
/// <reference types="node" />
import File = require('vinyl');
import { AsyncTransformStream } from './streams';
export declare function forkStream(stream: NodeJS.ReadableStream): NodeJS.ReadableStream;
/**
 * Forks a stream of Vinyl files, cloning each file before emitting on the fork.
 */
export declare class ForkedVinylStream extends AsyncTransformStream<File, File> {
    constructor();
    protected _transformIter(files: AsyncIterable<File>): AsyncIterable<File>;
}
