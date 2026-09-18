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
Object.defineProperty(exports, "__esModule", { value: true });
const model_1 = require("../model/model");
const minimatch = require("minimatch");
class WarningFilter {
    constructor(options) {
        this.warningCodesToIgnore = new Set();
        this.minimumSeverity = model_1.Severity.INFO;
        this.fileGlobsToFilterOut = [];
        if (options.warningCodesToIgnore) {
            this.warningCodesToIgnore = options.warningCodesToIgnore;
        }
        if (options.minimumSeverity != null) {
            this.minimumSeverity = options.minimumSeverity;
        }
        if (options.filesToIgnore) {
            this.fileGlobsToFilterOut =
                (options.filesToIgnore ||
                    []).map((glob) => new minimatch.Minimatch(glob, {}));
        }
    }
    shouldIgnore(warning) {
        if (this.warningCodesToIgnore.has(warning.code)) {
            return true;
        }
        if (warning.severity > this.minimumSeverity) {
            return true;
        }
        for (const glob of this.fileGlobsToFilterOut) {
            if (glob.match(warning.sourceRange.file)) {
                return true;
            }
        }
        return false;
    }
}
exports.WarningFilter = WarningFilter;
//# sourceMappingURL=warning-filter.js.map