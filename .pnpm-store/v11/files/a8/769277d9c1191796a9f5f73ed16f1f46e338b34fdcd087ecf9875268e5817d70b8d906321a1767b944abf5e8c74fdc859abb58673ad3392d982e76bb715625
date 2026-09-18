"use strict";
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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const chai_1 = require("chai");
function getFlowingState(stream) {
    // Cast our streams to <any> so that we can check the flowing state.
    // _readableState is undocumented in the Node.js TypeScript definition,
    // however it is the supported way to assert if a stream is flowing or not.
    // See: https://nodejs.org/api/stream.html#stream_three_states
    // tslint:disable-next-line: no-any
    const privateReadableState = stream._readableState;
    return privateReadableState.flowing;
}
exports.getFlowingState = getFlowingState;
/**
 * This method makes it possible to `await` a map of paths to `File` objects
 * emitted by a stream. It returns a Promise that resolves with the map
 * where the paths in the map exclude the optional `root` prefix.
 */
function emittedFiles(stream, root = '') {
    return __awaiter(this, void 0, void 0, function* () {
        const files = new Map();
        return new Promise((resolve, reject) => stream
            .on('data', (f) => files.set(f.path.substring(root.length + 1), f))
            .on('data', () => { })
            .on('end', () => resolve(files))
            .on('error', (e) => reject(e)));
    });
}
exports.emittedFiles = emittedFiles;
/**
 * Assert that two strings are equal after collapsing their whitespace.
 */
exports.assertEqualIgnoringWhitespace = (actual, expected) => chai_1.assert.equal(collapseWhitespace(actual), collapseWhitespace(expected));
/**
 * Assert that two string maps are equal, where their values have had their
 * whitespace collapsed.
 */
exports.assertMapEqualIgnoringWhitespace = (actual, expected) => assertMapEqual(transformMapValues(actual, collapseWhitespace), transformMapValues(expected, collapseWhitespace));
/**
 * Collapse all leading whitespace, trailing whitespace, and newlines. Very
 * lossy, but good for loose comparison of HTML, JS, etc.
 */
const collapseWhitespace = (s) => s.replace(/^\s+/gm, '').replace(/\s+$/gm, '').replace(/\n/gm, '');
/**
 * Assert that two maps are equal. Note that early versions of chai's deepEqual
 * will always return true, and while later ones will compare correctly, they do
 * not produce very readable output compared to this approach.
 */
const assertMapEqual = (actual, expected) => chai_1.assert.deepEqual([...actual.entries()], [...expected.entries()]);
/**
 * Return a new map where all values have been transformed with the given
 * function.
 */
const transformMapValues = (map, transform) => new Map([...map.entries()].map(([key, val]) => [key, transform(val)]));
/**
 * Calls the given async function and captures all console.log and friends
 * output while until the returned Promise settles.
 *
 * Does not capture plylog, which doesn't seem to be very easy to intercept.
 *
 * TODO(rictic): this function is shared across many of our packages,
 *   put it in a shared package instead.
 */
function interceptOutput(captured) {
    return __awaiter(this, void 0, void 0, function* () {
        const originalLog = console.log;
        const originalError = console.error;
        const originalWarn = console.warn;
        const buffer = [];
        // tslint:disable-next-line:no-any This is genuinely the API.
        const capture = (...args) => {
            buffer.push(args.join(' '));
        };
        console.log = capture;
        console.error = capture;
        console.warn = capture;
        const restoreAndGetOutput = () => {
            console.log = originalLog;
            console.error = originalError;
            console.warn = originalWarn;
            return buffer.join('\n');
        };
        try {
            yield captured();
        }
        catch (err) {
            const output = restoreAndGetOutput();
            console.error(output);
            throw err;
        }
        return restoreAndGetOutput();
    });
}
exports.interceptOutput = interceptOutput;
//# sourceMappingURL=util.js.map