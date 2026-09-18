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
var __asyncValues = (this && this.__asyncValues) || function (o) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var m = o[Symbol.asyncIterator], i;
    return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i);
    function verb(n) { i[n] = o[n] && function (v) { return new Promise(function (resolve, reject) { v = o[n](v), settle(resolve, reject, v.done, v.value); }); }; }
    function settle(resolve, reject, d, v) { Promise.resolve(v).then(function(v) { resolve({ value: v, done: d }); }, reject); }
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
Object.defineProperty(exports, "__esModule", { value: true });
const chai_1 = require("chai");
const streams_1 = require("../streams");
suite('AsyncTransformStream', () => {
    test('transforms input', () => __awaiter(this, void 0, void 0, function* () {
        class DoubleTransformer extends streams_1.AsyncTransformStream {
            _transformIter(inputs) {
                return __asyncGenerator(this, arguments, function* _transformIter_1() {
                    var e_1, _a;
                    try {
                        for (var inputs_1 = __asyncValues(inputs), inputs_1_1; inputs_1_1 = yield __await(inputs_1.next()), !inputs_1_1.done;) {
                            const input = inputs_1_1.value;
                            yield yield __await(input * 2);
                        }
                    }
                    catch (e_1_1) { e_1 = { error: e_1_1 }; }
                    finally {
                        try {
                            if (inputs_1_1 && !inputs_1_1.done && (_a = inputs_1.return)) yield __await(_a.call(inputs_1));
                        }
                        finally { if (e_1) throw e_1.error; }
                    }
                });
            }
        }
        const transformer = new DoubleTransformer({ objectMode: true });
        const results = [];
        transformer.on('data', (v) => results.push(v));
        const result = new Promise((resolve, reject) => {
            transformer.on('end', () => resolve(results));
            transformer.on('error', (err) => reject(err));
        });
        transformer.write(10);
        transformer.write(20);
        transformer.write(30);
        transformer.end();
        const final = yield result;
        chai_1.assert.deepEqual(final, [20, 40, 60]);
    }));
    test('fails if the stream does not consume all input', () => __awaiter(this, void 0, void 0, function* () {
        class GivesUpAfterTwo extends streams_1.AsyncTransformStream {
            _transformIter(inputs) {
                return __asyncGenerator(this, arguments, function* _transformIter_2() {
                    var e_2, _a;
                    let i = 0;
                    try {
                        for (var inputs_2 = __asyncValues(inputs), inputs_2_1; inputs_2_1 = yield __await(inputs_2.next()), !inputs_2_1.done;) {
                            const input = inputs_2_1.value;
                            i++;
                            if (i > 2) {
                                return yield __await(void 0);
                            }
                            yield yield __await(input * 3);
                        }
                    }
                    catch (e_2_1) { e_2 = { error: e_2_1 }; }
                    finally {
                        try {
                            if (inputs_2_1 && !inputs_2_1.done && (_a = inputs_2.return)) yield __await(_a.call(inputs_2));
                        }
                        finally { if (e_2) throw e_2.error; }
                    }
                });
            }
        }
        const transformer = new GivesUpAfterTwo({ objectMode: true });
        const results = [];
        transformer.on('data', (v) => results.push(v));
        const onEnd = new Promise((resolve) => transformer.once('end', resolve));
        const onError = new Promise((resolve) => transformer.once('error', resolve));
        transformer.write(10);
        transformer.write(20);
        transformer.write(30);
        transformer.end();
        chai_1.assert.deepEqual((yield onError).message, 'GivesUpAfterTwo did not consume all input while transforming.');
        yield onEnd;
        // We still do emit the two.
        chai_1.assert.deepEqual(results, [30, 60]);
    }));
});
//# sourceMappingURL=streams_test.js.map