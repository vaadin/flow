"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __await = (this && this.__await) || function (v) { return this instanceof __await ? (this.v = v, this) : new __await(v); }
var __asyncGenerator = (this && this.__asyncGenerator) || function (thisArg, _arguments, generator) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var g = generator.apply(thisArg, _arguments || []), i, q = [];
    return i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i;
    function verb(n) { if (g[n]) i[n] = function (v) { return new Promise(function (a, b) { q.push([n, v, a, b]) > 1 || resume(n, v); }); }; }
    function resume(n, v) { try { step(g[n](v)); } catch (e) { settle(q[0][3], e); } }
    function step(r) { r.value instanceof __await ? Promise.resolve(r.value.v).then(fulfill, reject) : settle(q[0][2], r); }
    function fulfill(value) { resume("next", value); }
    function reject(value) { resume("throw", value); }
    function settle(f, v) { if (f(v), q.shift(), q.length) resume(q[0][0], q[0][1]); }
};
var __asyncValues = (this && this.__asyncValues) || function (o) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var m = o[Symbol.asyncIterator], i;
    return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i);
    function verb(n) { i[n] = o[n] && function (v) { return new Promise(function (resolve, reject) { v = o[n](v), settle(resolve, reject, v.done, v.value); }); }; }
    function settle(resolve, reject, d, v) { Promise.resolve(v).then(function(v) { resolve({ value: v, done: d }); }, reject); }
};
Object.defineProperty(exports, "__esModule", { value: true });
const mz_1 = require("mz");
const utils_1 = require("polymer-analyzer/lib/core/utils");
const stream_1 = require("stream");
const File = require("vinyl");
const multipipe = require('multipipe');
if (Symbol.asyncIterator === undefined) {
    // tslint:disable-next-line: no-any polyfilling.
    Symbol.asyncIterator = Symbol('asyncIterator');
}
/**
 * Waits for the given ReadableStream
 */
function waitFor(stream) {
    return new Promise((resolve, reject) => {
        stream.on('end', resolve);
        stream.on('error', reject);
    });
}
exports.waitFor = waitFor;
/**
 * Waits for all the given ReadableStreams
 */
function waitForAll(streams) {
    return Promise.all(streams.map((s) => waitFor(s)));
}
exports.waitForAll = waitForAll;
/**
 * Returns the string contents of a Vinyl File object, waiting for
 * all chunks if the File is a stream.
 */
function getFileContents(file) {
    return __awaiter(this, void 0, void 0, function* () {
        if (file.isBuffer()) {
            return file.contents.toString('utf-8');
        }
        else if (file.isStream()) {
            const stream = file.contents;
            stream.setEncoding('utf-8');
            const contents = [];
            stream.on('data', (chunk) => contents.push(chunk));
            return new Promise((resolve, reject) => {
                stream.on('end', () => resolve(contents.join('')));
                stream.on('error', reject);
            });
        }
        throw new Error(`Unable to get contents of file ${file.path}. ` +
            `It has neither a buffer nor a stream.`);
    });
}
exports.getFileContents = getFileContents;
/**
 * Composes multiple streams (or Transforms) into one.
 */
function compose(streams) {
    if (streams && streams.length > 0) {
        return multipipe(streams);
    }
    else {
        return new stream_1.PassThrough({ objectMode: true });
    }
}
exports.compose = compose;
/**
 * An asynchronous queue that is read as an async iterable.
 */
class AsyncQueue {
    constructor() {
        this.blockedOn = undefined;
        this.backlog = [];
        this._closed = false;
        this._finished = false;
    }
    /**
     * Add the given value onto the queue.
     *
     * The return value of this method resolves once the value has been removed
     * from the queue. Useful for flow control.
     *
     * Must not be called after the queue has been closed.
     */
    write(value) {
        return __awaiter(this, void 0, void 0, function* () {
            if (this._closed) {
                throw new Error('Wrote to closed writable iterable');
            }
            return this._write({ value, done: false });
        });
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
    close() {
        return __awaiter(this, void 0, void 0, function* () {
            this._closed = true;
            return this._write({ done: true });
        });
    }
    _write(value) {
        return __awaiter(this, void 0, void 0, function* () {
            if (this.blockedOn) {
                this.blockedOn.resolve(value);
                this.blockedOn = undefined;
            }
            else {
                const deferred = new utils_1.Deferred();
                this.backlog.push({ value, deferred });
                yield deferred.promise;
            }
        });
    }
    /**
     * Iterate over values in the queue. Not intended for multiple readers.
     * In the case where there are multiple readers, some values may be received
     * by multiple readers, but all values will be seen by at least one reader.
     */
    [Symbol.asyncIterator]() {
        return __asyncGenerator(this, arguments, function* _a() {
            while (true) {
                let value;
                const maybeValue = this.backlog.shift();
                if (maybeValue) {
                    maybeValue.deferred.resolve(undefined);
                    value = maybeValue.value;
                }
                else {
                    this.blockedOn = new utils_1.Deferred();
                    value = yield __await(this.blockedOn.promise);
                }
                if (value.done) {
                    this._finished = true;
                    this._write(value);
                    return yield __await(void 0);
                }
                else {
                    yield yield __await(value.value);
                }
            }
        });
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
class AsyncTransformStream extends stream_1.Transform {
    constructor() {
        super(...arguments);
        this._inputs = new AsyncQueue();
        this._initialized = false;
        this._writingFinished = new utils_1.Deferred();
    }
    _initializeOnce() {
        if (this._initialized === false) {
            this._initialized = true;
            const transformDonePromise = (() => __awaiter(this, void 0, void 0, function* () {
                var e_1, _a;
                try {
                    for (var _b = __asyncValues(this._transformIter(this._inputs)), _c; _c = yield _b.next(), !_c.done;) {
                        const value = _c.value;
                        // TODO(rictic): if `this.push` returns false, should we wait until
                        //     we get a drain event to keep iterating?
                        this.push(value);
                    }
                }
                catch (e_1_1) { e_1 = { error: e_1_1 }; }
                finally {
                    try {
                        if (_c && !_c.done && (_a = _b.return)) yield _a.call(_b);
                    }
                    finally { if (e_1) throw e_1.error; }
                }
            }))();
            transformDonePromise.then(() => {
                if (this._inputs.finished) {
                    this._writingFinished.resolve(undefined);
                }
                else {
                    this.emit('error', new Error(`${this.constructor.name}` +
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
    _transform(input, _encoding, callback) {
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
    _flush(callback) {
        return __awaiter(this, void 0, void 0, function* () {
            try {
                // We won't get any more inputs. Wait for them all to be processed.
                yield this._inputs.close();
                // Wait for all of our output to be written.
                yield this._writingFinished.promise;
                callback();
            }
            catch (e) {
                callback(e);
            }
        });
    }
}
exports.AsyncTransformStream = AsyncTransformStream;
/**
 * A stream that takes file path strings, and outputs full Vinyl file objects
 * for the file at each location.
 */
class VinylReaderTransform extends AsyncTransformStream {
    constructor() {
        super({ objectMode: true });
    }
    _transformIter(paths) {
        return __asyncGenerator(this, arguments, function* _transformIter_1() {
            var e_2, _a;
            try {
                for (var paths_1 = __asyncValues(paths), paths_1_1; paths_1_1 = yield __await(paths_1.next()), !paths_1_1.done;) {
                    const filePath = paths_1_1.value;
                    yield yield __await(new File({ path: filePath, contents: yield __await(mz_1.fs.readFile(filePath)) }));
                }
            }
            catch (e_2_1) { e_2 = { error: e_2_1 }; }
            finally {
                try {
                    if (paths_1_1 && !paths_1_1.done && (_a = paths_1.return)) yield __await(_a.call(paths_1));
                }
                finally { if (e_2) throw e_2.error; }
            }
        });
    }
}
exports.VinylReaderTransform = VinylReaderTransform;
/**
 * pipeStreams() takes in a collection streams and pipes them together,
 * returning the last stream in the pipeline. Each element in the `streams`
 * array must be either a stream, or an array of streams (see PipeStream).
 * pipeStreams() will then flatten this array before piping them all together.
 */
function pipeStreams(streams) {
    return Array.prototype.concat.apply([], streams)
        .reduce((a, b) => {
        return a.pipe(b);
    });
}
exports.pipeStreams = pipeStreams;
//# sourceMappingURL=streams.js.map