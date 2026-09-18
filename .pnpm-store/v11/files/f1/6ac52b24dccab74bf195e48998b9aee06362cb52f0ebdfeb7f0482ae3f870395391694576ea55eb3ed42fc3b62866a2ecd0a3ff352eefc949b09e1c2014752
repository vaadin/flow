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

import * as dom5 from 'dom5/lib/index-next';
import * as parse5 from 'parse5';
import File = require('vinyl');
import {AsyncTransformStream, getFileContents} from './streams';
import {LocalFsPath} from './path-transformers';

const baseMatcher = dom5.predicates.hasTagName('base');

/**
 * Find a `<base>` tag in the specified file and if found, update its `href`
 * with the given new value.
 */
export class BaseTagUpdater extends AsyncTransformStream<File, File> {
  constructor(private filePath: LocalFsPath, private newHref: string) {
    super({objectMode: true});
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      if (file.path !== this.filePath) {
        yield file;
        continue;
      }

      const contents = await getFileContents(file);
      const parsed = parse5.parse(contents, {locationInfo: true});
      const base = dom5.query(parsed, baseMatcher);
      if (!base || dom5.getAttribute(base, 'href') === this.newHref) {
        yield file;
        continue;
      }

      dom5.setAttribute(base, 'href', this.newHref);
      dom5.removeFakeRootElements(parsed);
      const updatedFile = file.clone();
      updatedFile.contents = Buffer.from(parse5.serialize(parsed), 'utf-8');
      yield updatedFile;
    }
  }
}
