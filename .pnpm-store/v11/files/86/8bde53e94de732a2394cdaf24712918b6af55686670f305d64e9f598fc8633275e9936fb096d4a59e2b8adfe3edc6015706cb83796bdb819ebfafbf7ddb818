"use strict";
/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
const pathlib = require("path");
const pathIsInside = require("path-is-inside");
const vscode_uri_1 = require("vscode-uri");
/**
 * Resolves requests via the file system.
 */
class FsUrlLoader {
    constructor(root = '') {
        this.root = pathlib.resolve(root);
    }
    canLoad(url) {
        const parsed = vscode_uri_1.default.parse(url);
        return parsed.scheme === 'file' && !parsed.authority &&
            pathIsInside(parsed.fsPath, this.root);
    }
    load(url) {
        return new Promise((resolve, reject) => {
            const result = this.getFilePath(url);
            if (!result.successful) {
                throw new Error(`FsUrlLoader can not load url ${JSON.stringify(url)} - ${result.error}`);
            }
            fs.readFile(result.value, 'utf8', (error, contents) => {
                if (error) {
                    // Improve the error message of the most common error.
                    if (error.code === 'ENOENT') {
                        reject(new Error(`No such file: ${error.path}`));
                    }
                    reject(error);
                }
                else {
                    resolve(contents);
                }
            });
        });
    }
    /**
     * If successful, result.value will be the filesystem path that we would load
     * the given url from.
     *
     * If unsuccessful, result.value will be an error message as a string.
     */
    getFilePath(url) {
        if (!url.startsWith('file://')) {
            return { successful: false, error: 'Not a local file:// url.' };
        }
        if (!this.canLoad(url)) {
            return {
                successful: false,
                error: `Path is not inside root directory: ${JSON.stringify(this.root)}`
            };
        }
        const path = vscode_uri_1.default.parse(url).fsPath;
        return { successful: true, value: path };
    }
    readDirectory(pathFromRoot, deep) {
        return __awaiter(this, void 0, void 0, function* () {
            const files = yield new Promise((resolve, reject) => {
                fs.readdir(pathlib.join(this.root, pathFromRoot), (err, files) => err ? reject(err) : resolve(files));
            });
            const results = [];
            const subDirResultPromises = [];
            for (const basename of files) {
                const file = pathlib.join(pathFromRoot, basename);
                const stat = yield new Promise((resolve, reject) => fs.stat(pathlib.join(this.root, file), (err, stat) => err ? reject(err) : resolve(stat)));
                if (stat.isDirectory()) {
                    if (deep) {
                        subDirResultPromises.push(this.readDirectory(file, deep));
                    }
                }
                else {
                    results.push(file);
                }
            }
            const arraysOfFiles = yield Promise.all(subDirResultPromises);
            for (const dirResults of arraysOfFiles) {
                for (const file of dirResults) {
                    results.push(file);
                }
            }
            return results;
        });
    }
}
exports.FsUrlLoader = FsUrlLoader;
//# sourceMappingURL=fs-url-loader.js.map