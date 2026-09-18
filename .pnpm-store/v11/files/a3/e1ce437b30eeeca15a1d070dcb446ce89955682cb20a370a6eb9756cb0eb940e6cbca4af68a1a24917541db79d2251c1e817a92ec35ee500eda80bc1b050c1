"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
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
const cleankill = require("cleankill");
const clireporter_1 = require("./clireporter");
const context_1 = require("./context");
const steps = require("./steps");
/**
 * Runs a suite of web component tests.
 *
 * The returned Context (a kind of EventEmitter) fires various events to allow
 * you to track the progress of the tests:
 *
 * Lifecycle Events:
 *
 * `run-start`
 *   WCT is ready to begin spinning up browsers.
 *
 * `browser-init` {browser} {stats}
 *   WCT is ready to begin spinning up browsers.
 *
 * `browser-start` {browser} {metadata} {stats}
 *   The browser has begun running tests. May fire multiple times (i.e. when
 *   manually refreshing the tests).
 *
 * `sub-suite-start` {browser} {sharedState} {stats}
 *   A suite file has begun running.
 *
 * `test-start` {browser} {test} {stats}
 *   A test has begun.
 *
 * `test-end` {browser} {test} {stats}
 *  A test has ended.
 *
 * `sub-suite-end` {browser} {sharedState} {stats}
 *   A suite file has finished running all of its tests.
 *
 * `browser-end` {browser} {error} {stats}
 *   The browser has completed, and it shutting down.
 *
 * `run-end` {error}
 *   WCT has run all browsers, and is shutting down.
 *
 * Generic Events:
 *
 *  * log:debug
 *  * log:info
 *  * log:warn
 *  * log:error
 *
 * @param {!Config|!Context} options The configuration or an already formed
 *     `Context` object.
 */
function test(options) {
    return __awaiter(this, void 0, void 0, function* () {
        const context = (options instanceof context_1.Context) ? options : new context_1.Context(options);
        // We assume that any options related to logging are passed in via the initial
        // `options`.
        if (context.options.output) {
            new clireporter_1.CliReporter(context, context.options.output, context.options);
        }
        try {
            yield steps.setupOverrides(context);
            yield steps.loadPlugins(context);
            yield steps.configure(context);
            yield steps.prepare(context);
            yield steps.runTests(context);
        }
        finally {
            if (!context.options.skipCleanup) {
                yield cleankill.close();
            }
        }
    });
}
exports.test = test;
// HACK
test['test'] = test;
module.exports = test;
