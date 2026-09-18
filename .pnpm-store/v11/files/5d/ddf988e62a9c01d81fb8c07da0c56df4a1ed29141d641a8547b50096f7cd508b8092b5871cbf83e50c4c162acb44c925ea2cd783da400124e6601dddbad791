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

import {fs} from 'mz';
import {Deferred} from 'polymer-analyzer/lib/core/utils';
import {PassThrough, Transform, TransformCallback} from 'stream';

import File = require('vinyl');

const multipipe = require('multipipe');

if (Symbol.asyncIterator === undefined) {
  // tslint:disable-next-line: no-any polyfilling.
  (Symbol as any).asyncIterator = Symbol('asyncIterator');
}

/**
 * Waits for the given ReadableStream
 */
export function waitFor(stream: NodeJS.ReadableStream):
    Promise<NodeJS.ReadableStream> {
  return new Promise<NodeJS.ReadableStream>((resolve, reject) => {
    stream.on('end', resolve);
    stream.on('error', reject);
  });
}

/**
 * Waits for all the given ReadableStreams
 */
export function waitForAll(streams: NodeJS.ReadableStream[]):
    Promise<NodeJS.ReadableStream[]> {
  return Promise.all<NodeJS.ReadableStream>(streams.map((s) => waitFor(s)));
}

/**
 * Returns the string contents of a Vinyl File object, waiting for
 * all chunks if the File is a stream.
 */
export async function getFileContents(file: File): Promise<string> {
  if (file.isBuffer()) {
    return file.contents.toString('utf-8');
  } else if (file.isStream()) {
    const stream = file.contents;
    stream.setEncoding('utf-8');
    const contents: string[] = [];
    stream.on('data', (chunk: string) => contents.push(chunk));

    return new Promise<string>((resolve, reject) => {
      stream.on('end', () => resolve(contents.join('')));
      stream.on('error', reject);
    });
  }
  throw new Error(
      `Unable to get contents of file ${file.path}. ` +
      `It has neither a buffer nor a stream.`);
}

/**
 * Composes multiple streams (or Transforms) into one.
 */
export function compose(streams: NodeJS.ReadWriteStream[]) {
  if (streams && streams.length > 0) {
    return multipipe(streams);
  } else {
    return new PassThrough({objectMode: true});
  }
}


/**
 * An asynchronous queue that is read as an async iterable.
 */
class AsyncQueue<V> implements AsyncIterable<V> {
  private blockedOn: Deferred<IteratorResult<V>>|undefined = undefined;
  backlog: Array<{value: IteratorResult<V>, deferred: Deferred<void>}> = [];
  private _closed = false;
  private _finished = false;

  /**
   * Add the given value onto the queue.
   *
   * The return value of this method resolves once the value has been removed
   * from the queue. Useful for flow control.
   *
   * Must not be called after the queue has been closed.
   */
  async write(value: V) {
    if (this._closed) {
      throw new Error('Wrote to closed writable iterable');
    }
    return this._write({value, done: false});
  }

  /**
   * True once the queue has been closed and all input has been read from it.
   */
  get finished() {
    return this._finished;
  }

  /**
   * Close the queue, indicating that no more values will be written.
   *
   * If this method is not called, a consumer iterating over the values will
   * wait forever.
   *
   * The returned promise resolves once the consumer has been notified of the
   * end of the queue.
   */
  async close() {
    this._closed = true;
    return this._write({done: true} as IteratorResult<V>);
  }

  private async _write(value: IteratorResult<V>) {
    if (this.blockedOn) {
      this.blockedOn.resolve(value);
      this.blockedOn = undefined;
    } else {
      const deferred = new Deferred<void>();
      this.backlog.push({value, deferred});
      await deferred.promise;
    }
  }

  /**
   * Iterate over values in the queue. Not intended for multiple readers.
   * In the case where there are multiple readers, some values may be received
   * by multiple readers, but all values will be seen by at least one reader.
   */
  async * [Symbol.asyncIterator](): AsyncIterator<V> {
    while (true) {
      let value;
      const maybeValue = this.backlog.shift();
      if (maybeValue) {
        maybeValue.deferred.resolve(undefined);
        value = maybeValue.value;
      } else {
        this.blockedOn = new Deferred();
        value = await this.blockedOn.promise;
      }
      if (value.done) {
        this._finished = true;
        this._write(value);
        return;
      } else {
        yield value.value;
      }
    }
  }
}

/**
 * Implements `stream.Transform` via standard async iteration.
 *
 * The main advantage over implementing stream.Transform itself is that correct
 * error handling is built in and easy to get right, simply by using
 * async/await.
 *
 * `In` and `Out` extend `{}` because they may not be `null`.
 */
export abstract class AsyncTransformStream<In extends {}, Out extends {}>
    extends Transform {
  private readonly _inputs = new AsyncQueue<In>();

  /**
   * Implement this method!
   *
   * Read from the given iterator to consume input, yield values to write
   * chunks of your own. You may yield any number of values for each input.
   *
   * Note: currently you *must* completely consume `inputs` and return for this
   *   stream to close.
   */
  protected abstract _transformIter(inputs: AsyncIterable<In>):
      AsyncIterable<Out>;

  private _initialized = false;
  private _writingFinished = new Deferred<void>();
  private _initializeOnce() {
    if (this._initialized === false) {
      this._initialized = true;
      const transformDonePromise = (async () => {
        for await (const value of this._transformIter(this._inputs)) {
          // TODO(rictic): if `this.push` returns false, should we wait until
          //     we get a drain event to keep iterating?
          this.push(value);
        }
      })();
      transformDonePromise.then(() => {
        if (this._inputs.finished) {
          this._writingFinished.resolve(undefined);
        } else {
          this.emit(
              'error',
              new Error(
                  `${this.constructor.name}` +
                  ` did not consume all input while transforming.`));
          // Since _transformIter has exited, but not all input was consumed,
          // this._flush won't be called. We need to signal manually that
          // no more output will be written by this stream.
          this.push(null);
        }
      }, (err) => this.emit('error', err));
    }
  }

  /**
   * Don't override.
   *
   * Passes input into this._inputs.
   */
  _transform(
      input: In, _encoding: string,
      callback: (error?: Error, value?: Out) => void) {
    this._initializeOnce();
    this._inputs.write(input).then(() => {
      callback();
    }, (err) => callback(err));
  }


  /**
   * Don't override.
   *
   * Finish writing out the outputs.
   */
  async _flush(callback: TransformCallback) {
    try {
      // We won't get any more inputs. Wait for them all to be processed.
      await this._inputs.close();
      // Wait for all of our output to be written.
      await this._writingFinished.promise;
      callback();
    } catch (e) {
      callback(e);
    }
  }
}

/**
 * A stream that takes file path strings, and outputs full Vinyl file objects
 * for the file at each location.
 */
export class VinylReaderTransform extends AsyncTransformStream<string, File> {
  constructor() {
    super({objectMode: true});
  }

  protected async *
      _transformIter(paths: AsyncIterable<string>): AsyncIterable<File> {
    for await (const filePath of paths) {
      yield new File({path: filePath, contents: await fs.readFile(filePath)});
    }
  }
}

export type PipeStream = (NodeJS.ReadableStream|NodeJS.WritableStream|
                          NodeJS.ReadableStream[]|NodeJS.WritableStream[]);

/**
 * pipeStreams() takes in a collection streams and pipes them together,
 * returning the last stream in the pipeline. Each element in the `streams`
 * array must be either a stream, or an array of streams (see PipeStream).
 * pipeStreams() will then flatten this array before piping them all together.
 */
export function pipeStreams(streams: PipeStream[]): NodeJS.ReadableStream {
  return Array.prototype.concat.apply([], streams)
      .reduce((a: NodeJS.ReadableStream, b: NodeJS.ReadWriteStream) => {
        return a.pipe(b);
      });
}
