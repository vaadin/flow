"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const test_1 = require("./test");
function init(gulp, dependencies) {
    if (!dependencies) {
        dependencies = [];
    }
    gulp.task('wct:local', gulp.series([
        ...dependencies,
        () => test_1.test({ plugins: { local: {}, sauce: false } }).catch(cleanError)
    ]));
    gulp.task('wct:sauce', gulp.series([
        ...dependencies,
        () => test_1.test({ plugins: { local: false, sauce: {} } }).catch(cleanError)
    ]));
    // TODO(nevir): Migrate fully to wct:local/etc.
    gulp.task('test', gulp.series(['wct:local']));
    gulp.task('test:local', gulp.series(['wct:local']));
    gulp.task('test:remote', gulp.series(['wct:sauce']));
    gulp.task('wct', gulp.series(['wct:local']));
}
exports.init = init;
// Utility
function cleanError(error) {
    // Pretty error for gulp.
    error = new Error(chalk.red(error.message || error));
    error.showStack = false;
    throw error;
}
