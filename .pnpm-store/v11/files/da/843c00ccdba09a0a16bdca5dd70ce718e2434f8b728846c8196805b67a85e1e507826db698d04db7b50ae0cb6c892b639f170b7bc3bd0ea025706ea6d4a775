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

import {htmlTransform} from './html-transform';
import {AsyncTransformStream, getFileContents} from './streams';

import File = require('vinyl');

/**
 * When compiling to ES5 we need to inject Babel's helpers into a global so
 * that they don't need to be included with each compiled file.
 */
export class BabelHelpersInjector extends AsyncTransformStream<File, File> {
  constructor(private entrypoint: string) {
    super({objectMode: true});
  }

  protected async * _transformIter(files: AsyncIterable<File>) {
    for await (const file of files) {
      yield await this.processFile(file);
    }
  }

  private async processFile(file: File): Promise<File> {
    if (file.path !== this.entrypoint) {
      return file;
    }
    const contents = await getFileContents(file);
    const transformed = htmlTransform(contents, {injectBabelHelpers: 'full'});
    const newFile = file.clone();
    newFile.contents = Buffer.from(transformed, 'utf-8');
    return newFile;
  }
}
