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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const fs = require("fs");
const glob = require("glob");
const _ = require("lodash");
const path = require("path");
const util_1 = require("util");
/**
 * Expands a series of path patterns (globs, files, directories) into a set of
 * files that represent those patterns.
 *
 * @param baseDir The directory that patterns are relative to.
 * @param patterns The patterns to expand.
 * @returns The expanded paths.
 */
function expand(baseDir, patterns) {
    return __awaiter(this, void 0, void 0, function* () {
        return expandDirectories(baseDir, yield unglob(baseDir, patterns));
    });
}
exports.expand = expand;
/**
 * Expands any glob expressions in `patterns`.
 */
function unglob(baseDir, patterns) {
    return __awaiter(this, void 0, void 0, function* () {
        const strs = [];
        const pGlob = util_1.promisify(glob);
        for (const pattern of patterns) {
            strs.push(yield pGlob(String(pattern), { cwd: baseDir, root: baseDir }));
        }
        // for non-POSIX support, replacing path separators
        return _.union(_.flatten(strs)).map((str) => str.replace(/\//g, path.sep));
    });
}
/**
 * Expands any directories in `patterns`, following logic similar to a web
 * server.
 *
 * If a pattern resolves to a directory, that directory is expanded. If the
 * directory contains an `index.html`, it is expanded to that. Otheriwse, the
 * it expands into its children (recursively).
 */
function expandDirectories(baseDir, paths) {
    return __awaiter(this, void 0, void 0, function* () {
        const listsOfPaths = [];
        for (const aPath of paths) {
            listsOfPaths.push(yield expandDirectory(baseDir, aPath));
        }
        const files = _.union(_.flatten(listsOfPaths));
        return files.filter((file) => /\.(js|html)$/.test(file));
    });
}
function expandDirectory(baseDir, aPath) {
    return __awaiter(this, void 0, void 0, function* () {
        const stat = yield util_1.promisify(fs.stat)(path.resolve(baseDir, aPath));
        if (!stat.isDirectory()) {
            return [aPath];
        }
        const files = yield util_1.promisify(fs.readdir)(path.resolve(baseDir, aPath));
        // We have an index; defer to that.
        if (_.includes(files, 'index.html')) {
            return [path.join(aPath, 'index.html')];
        }
        const children = yield expandDirectories(path.join(baseDir, aPath), files);
        return children.map((child) => path.join(aPath, child));
    });
}
