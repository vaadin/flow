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
Object.defineProperty(exports, "__esModule", { value: true });
const streams_1 = require("./streams");
/**
 * This is a `UrlLoader` for use with a `polymer-analyzer` that reads files
 * that have been gathered by a `BuildBundler` transform stream.
 */
class FileMapUrlLoader {
    constructor(files, fallbackLoader) {
        this.files = files;
        this.fallbackLoader = fallbackLoader;
    }
    // Return true if we can return load the given url.
    canLoad(url) {
        return !!(this.files.has(url) ||
            this.fallbackLoader && this.fallbackLoader.canLoad(url));
    }
    // Try to load the file from the map.  If not in the map, try to load
    // from the fallback loader.
    load(url) {
        return __awaiter(this, void 0, void 0, function* () {
            const file = this.files.get(url);
            if (file == null) {
                if (this.fallbackLoader) {
                    if (this.fallbackLoader.canLoad(url)) {
                        return this.fallbackLoader.load(url);
                    }
                    throw new Error(`${url} not present in file map and fallback loader can not load.`);
                }
                throw new Error(`${url} not present in file map and no fallback loader.`);
            }
            return streams_1.getFileContents(file);
        });
    }
}
exports.FileMapUrlLoader = FileMapUrlLoader;
//# sourceMappingURL=file-map-url-loader.js.map