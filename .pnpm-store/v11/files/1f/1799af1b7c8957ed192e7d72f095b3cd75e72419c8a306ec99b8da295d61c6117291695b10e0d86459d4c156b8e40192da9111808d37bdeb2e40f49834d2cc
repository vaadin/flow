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
/// <reference types="vinyl-fs" />
import { Transform, TransformCallback } from 'stream';
import File = require('vinyl');
/**
 * Waits for the given ReadableStream
 */
export declare function waitFor(stream: NodeJS.ReadableStream): Promise<NodeJS.ReadableStream>;
/**
 * Waits for all the given ReadableStreams
 */
export declare function waitForAll(streams: NodeJS.ReadableStream[]): Promise<NodeJS.ReadableStream[]>;
/**
 * Returns the string contents of a Vinyl File object, waiting for
 * all chunks if the File is a stream.
 */
export declare function getFileContents(file: File): Promise<string>;
/**
 * Composes multiple streams (or Transforms) into one.
 */
export declare function compose(streams: NodeJS.ReadWriteStream[]): any;
/**
 * Implements `stream.Transform` via standard async iteration.
 *
 * The main advantage over implementing stream.Transform itself is that correct
 * error handling is built in and easy to get right, simply by using
 * async/await.
 *
 * `In` and `Out` extend `{}` because they may not be `null`.
 */
export declare abstract class AsyncTransformStream<In extends {}, Out extends {}> extends Transform {
    private readonly _inputs;
    /**
     * Implement this method!
     *
     * Read from the given iterator to consume input, yield values to write
     * chunks of your own. You may yield any number of values for each input.
     *
     * Note: currently you *must* completely consume `inputs` and return for this
     *   stream to close.
     */
    protected abstract _transformIter(inputs: AsyncIterable<In>): AsyncIterable<Out>;
    private _initialized;
    private _writingFinished;
    private _initializeOnce;
    /**
     * Don't override.
     *
     * Passes input into this._inputs.
     */
    _transform(input: In, _encoding: string, callback: (error?: Error, value?: Out) => void): void;
    /**
     * Don't override.
     *
     * Finish writing out the outputs.
     */
    _flush(callback: TransformCallback): Promise<void>;
}
/**
 * A stream that takes file path strings, and outputs full Vinyl file objects
 * for the file at each location.
 */
export declare class VinylReaderTransform extends AsyncTransformStream<string, File> {
    constructor();
    protected _transformIter(paths: AsyncIterable<string>): AsyncIterable<File>;
}
export declare type PipeStream = (NodeJS.ReadableStream | NodeJS.WritableStream | NodeJS.ReadableStream[] | NodeJS.WritableStream[]);
/**
 * pipeStreams() takes in a collection streams and pipes them together,
 * returning the last stream in the pipeline. Each element in the `streams`
 * array must be either a stream, or an array of streams (see PipeStream).
 * pipeStreams() will then flatten this array before piping them all together.
 */
export declare function pipeStreams(streams: PipeStream[]): NodeJS.ReadableStream;
