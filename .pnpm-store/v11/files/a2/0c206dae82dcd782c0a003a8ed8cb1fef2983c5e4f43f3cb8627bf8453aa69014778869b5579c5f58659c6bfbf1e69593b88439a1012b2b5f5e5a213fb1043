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
Object.defineProperty(exports, "__esModule", { value: true });
const bowerConfig = require("bower-config");
const fs = require("fs");
const path = require("path");
function readConfigSync(filename, root) {
    const configPath = path.resolve(root || '', filename);
    let config;
    try {
        config = fs.readFileSync(configPath, 'utf-8');
    }
    catch (e) {
        return {};
    }
    try {
        return JSON.parse(config);
    }
    catch (e) {
        console.error(`Could not parse ${configPath}`);
        console.error(e);
    }
    return {};
}
/**
 * Determines the package name by reading from the following sources:
 *
 * 1. `options.packageName`
 * 2. bower.json, if options.npm is not true
 * 3. package.json
 * 4. The name of the root directory
 */
function getPackageName(options) {
    return options.packageName ||
        !options.npm && readConfigSync('bower.json', options.root).name ||
        readConfigSync('package.json', options.root).name ||
        path.basename(options.root);
}
exports.getPackageName = getPackageName;
function getComponentDir(options) {
    const root = options.root || process.cwd();
    const bowerDir = bowerConfig.read(root).directory;
    return options.componentDir || (options.npm ? 'node_modules' : bowerDir);
}
exports.getComponentDir = getComponentDir;
//# sourceMappingURL=config.js.map