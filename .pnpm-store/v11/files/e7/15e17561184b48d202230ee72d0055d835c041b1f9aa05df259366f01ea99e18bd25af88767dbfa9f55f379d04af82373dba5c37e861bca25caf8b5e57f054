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

import File = require('vinyl');

import {AsyncTransformStream} from './streams';

export function forkStream(stream: NodeJS.ReadableStream):
    NodeJS.ReadableStream {
  const fork = new ForkedVinylStream();
  stream.pipe(fork);
  return fork;
}

/**
 * Forks a stream of Vinyl files, cloning each file before emitting on the fork.
 */
export class ForkedVinylStream extends AsyncTransformStream<File, File> {
  constructor() {
    super({objectMode: true});
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      yield file.clone({deep: true, contents: true});
    }
  }
}
