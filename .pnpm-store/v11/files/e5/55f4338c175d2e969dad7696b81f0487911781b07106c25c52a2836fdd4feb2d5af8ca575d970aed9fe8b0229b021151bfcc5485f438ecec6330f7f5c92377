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

import * as assert from 'assert';
import * as dom5 from 'dom5/lib/index-next';
import * as parse5 from 'parse5';
import * as osPath from 'path';
import {Transform} from 'stream';
import File = require('vinyl');
import {AsyncTransformStream, getFileContents} from './streams';

const pred = dom5.predicates;

const extensionsForType: {[mimetype: string]: string} = {
  'text/ecmascript-6': 'js',
  'application/javascript': 'js',
  'text/javascript': 'js',
  'application/x-typescript': 'ts',
  'text/x-typescript': 'ts',
  'module': 'js',
};

/**
 * HTMLSplitter represents the shared state of files as they are passed through
 * a splitting stream and then a rejoining stream. Creating a new instance of
 * HTMLSplitter and adding its streams to the build pipeline is the
 * supported user interface for splitting out and rejoining inlined CSS & JS in
 * the build process.
 */
export class HtmlSplitter {
  private _splitFiles: Map<string, SplitFile> = new Map();
  private _parts: Map<string, SplitFile> = new Map();

  /**
   * Returns a new `Transform` stream that splits inline script and styles into
   * new, separate files that are passed out of the stream.
   */
  split(): Transform {
    return new HtmlSplitTransform(this);
  }

  /**
   * Returns a new `Transform` stream that rejoins inline scripts and styles
   * that were originally split from this `HTMLSplitter`'s `split()` back into
   * their parent HTML files.
   */
  rejoin(): Transform {
    return new HtmlRejoinTransform(this);
  }

  isSplitFile(parentPath: string): boolean {
    return this._splitFiles.has(parentPath);
  }

  getSplitFile(parentPath: string): SplitFile {
    // TODO(justinfagnani): rewrite so that processing a parent file twice
    // throws to protect against bad configurations of multiple streams that
    // contain the same file multiple times.
    let splitFile = this._splitFiles.get(parentPath);
    if (!splitFile) {
      splitFile = new SplitFile(parentPath);
      this._splitFiles.set(parentPath, splitFile);
    }
    return splitFile;
  }

  addSplitPath(parentPath: string, childPath: string): void {
    const splitFile = this.getSplitFile(parentPath);
    splitFile.addPartPath(childPath);
    this._parts.set(childPath, splitFile);
  }

  getParentFile(childPath: string): SplitFile|undefined {
    return this._parts.get(childPath);
  }
}

const htmlSplitterAttribute = 'html-splitter';

/**
 * Returns whether the given script tag was an inline script that was split out
 * into a fake file by HtmlSplitter.
 */
export function scriptWasSplitByHtmlSplitter(script: dom5.Node): boolean {
  return dom5.hasAttribute(script, htmlSplitterAttribute);
}

export type HtmlSplitterFile = File&{
  fromHtmlSplitter?: true;
  isModule?: boolean
};

/**
 * Return whether the given Vinyl file was created by the HtmlSplitter from an
 * HTML document script tag.
 */
export function isHtmlSplitterFile(file: File): file is HtmlSplitterFile {
  return file.fromHtmlSplitter === true;
}

/**
 * Represents a file that is split into multiple files.
 */
export class SplitFile {
  path: string;
  parts: Map<string, string|null> = new Map();
  outstandingPartCount = 0;
  vinylFile: File|null = null;

  constructor(path: string) {
    this.path = path;
  }

  addPartPath(path: string): void {
    this.parts.set(path, null);
    this.outstandingPartCount++;
  }

  setPartContent(path: string, content: string): void {
    assert(
        this.parts.get(path) !== undefined,
        `Trying to save unexpected file part "${path}".`);
    assert(
        this.parts.get(path) === null,
        `Trying to save already-saved file part "${path}".`);
    assert(
        this.outstandingPartCount > 0,
        `Trying to save valid file part "${path}", ` +
            `but somehow no file parts are outstanding.`);
    this.parts.set(path, content);
    this.outstandingPartCount--;
  }

  get isComplete(): boolean {
    return this.outstandingPartCount === 0 && this.vinylFile != null;
  }
}

/**
 * Splits HTML files, extracting scripts and styles into separate File objects.
 */
class HtmlSplitTransform extends AsyncTransformStream<File, File> {
  _state: HtmlSplitter;

  constructor(splitter: HtmlSplitter) {
    super({objectMode: true});
    this._state = splitter;
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      const filePath = osPath.normalize(file.path);
      if (!(file.contents && filePath.endsWith('.html'))) {
        yield file;
        continue;
      }
      const contents = await getFileContents(file);
      const doc = parse5.parse(contents, {locationInfo: true});
      dom5.removeFakeRootElements(doc);
      const scriptTags = [...dom5.queryAll(doc, pred.hasTagName('script'))];
      for (let i = 0; i < scriptTags.length; i++) {
        const scriptTag = scriptTags[i];
        const source = dom5.getTextContent(scriptTag);
        const typeAttribute =
            dom5.getAttribute(scriptTag, 'type') || 'application/javascript';
        const extension = extensionsForType[typeAttribute];
        // If we don't recognize the script type attribute, don't split
        // out.
        if (!extension) {
          continue;
        }

        const isInline = !dom5.hasAttribute(scriptTag, 'src');

        if (isInline) {
          const childFilename =
              `${osPath.basename(filePath)}_script_${i}.${extension}`;
          const childPath =
              osPath.join(osPath.dirname(filePath), childFilename);
          scriptTag.childNodes = [];
          dom5.setAttribute(scriptTag, 'src', childFilename);
          dom5.setAttribute(scriptTag, htmlSplitterAttribute, '');
          const scriptFile: HtmlSplitterFile = new File({
            cwd: file.cwd,
            base: file.base,
            path: childPath,
            contents: Buffer.from(source),
          });
          scriptFile.fromHtmlSplitter = true;
          scriptFile.isModule = typeAttribute === 'module';
          this._state.addSplitPath(filePath, childPath);
          this.push(scriptFile);
        }
      }

      const splitContents = parse5.serialize(doc);
      const newFile = new File({
        cwd: file.cwd,
        base: file.base,
        path: filePath,
        contents: Buffer.from(splitContents),
      });
      yield newFile;
    }
  }
}


/**
 * Joins HTML files originally split by `Splitter`, based on the relationships
 * stored in its HTMLSplitter state.
 */
class HtmlRejoinTransform extends AsyncTransformStream<File, File> {
  static isExternalScript =
      pred.AND(pred.hasTagName('script'), pred.hasAttr('src'));

  _state: HtmlSplitter;

  constructor(splitter: HtmlSplitter) {
    super({objectMode: true});
    this._state = splitter;
  }

  protected async *
      _transformIter(files: AsyncIterable<File>): AsyncIterable<File> {
    for await (const file of files) {
      const filePath = osPath.normalize(file.path);
      if (this._state.isSplitFile(filePath)) {
        // this is a parent file
        const splitFile = this._state.getSplitFile(filePath);
        splitFile.vinylFile = file;
        if (splitFile.isComplete) {
          yield await this._rejoin(splitFile);
        } else {
          splitFile.vinylFile = file;
        }
      } else {
        const parentFile = this._state.getParentFile(filePath);
        if (parentFile) {
          // this is a child file
          parentFile.setPartContent(filePath, file.contents!.toString());
          if (parentFile.isComplete) {
            yield await this._rejoin(parentFile);
          }
        } else {
          yield file;
        }
      }
    }
  }

  async _rejoin(splitFile: SplitFile) {
    const file = splitFile.vinylFile;
    if (file == null) {
      throw new Error(`Internal error: no vinylFile found for splitfile: ${
          splitFile.path}`);
    }
    const filePath = osPath.normalize(file.path);
    const contents = await getFileContents(file);
    const doc = parse5.parse(contents, {locationInfo: true});
    dom5.removeFakeRootElements(doc);
    const scriptTags = dom5.queryAll(doc, HtmlRejoinTransform.isExternalScript);

    for (const scriptTag of scriptTags) {
      const srcAttribute = dom5.getAttribute(scriptTag, 'src')!;
      const scriptPath =
          osPath.join(osPath.dirname(splitFile.path), srcAttribute);
      const scriptSource = splitFile.parts.get(scriptPath);
      if (scriptSource != null) {
        dom5.setTextContent(scriptTag, scriptSource);
        dom5.removeAttribute(scriptTag, 'src');
        dom5.removeAttribute(scriptTag, htmlSplitterAttribute);
      }
    }

    const joinedContents = parse5.serialize(doc);

    return new File({
      cwd: file.cwd,
      base: file.base,
      path: filePath,
      contents: Buffer.from(joinedContents),
    });
  }
}
