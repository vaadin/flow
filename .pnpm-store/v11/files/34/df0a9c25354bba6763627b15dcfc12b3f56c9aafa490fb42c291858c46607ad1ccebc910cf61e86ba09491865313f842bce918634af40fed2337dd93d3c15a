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
const _ = require("lodash");
const path = require("path");
// Plugin module names can be prefixed by the following:
const PREFIXES = [
    'web-component-tester-',
    'wct-',
];
/**
 * A WCT plugin. This constructor is private. Plugins can be retrieved via
 * `Plugin.get`.
 */
class Plugin {
    constructor(packageName, metadata) {
        this.packageName = packageName;
        this.metadata = metadata;
        this.name = Plugin.shortName(packageName);
        this.cliConfig = this.metadata['cli-options'] || {};
    }
    /**
     * @param {!Context} context The context that this plugin should be evaluated
     *     within.
     */
    execute(context) {
        return __awaiter(this, void 0, void 0, function* () {
            try {
                const plugin = require(this.packageName);
                plugin(context, context.pluginOptions(this.name), this);
            }
            catch (error) {
                throw `Failed to load plugin "${this.name}": ${error}`;
            }
        });
    }
    /**
     * Retrieves a plugin by shorthand or module name (loading it as necessary).
     *
     * @param {string} name
     */
    static get(name) {
        return __awaiter(this, void 0, void 0, function* () {
            const shortName = Plugin.shortName(name);
            if (_loadedPlugins[shortName]) {
                return _loadedPlugins[shortName];
            }
            const names = [shortName].concat(PREFIXES.map((p) => p + shortName));
            const loaded = _.compact(names.map(_tryLoadPluginPackage));
            if (loaded.length > 1) {
                const prettyNames = loaded.map((p) => p.packageName).join(' ');
                throw `Loaded conflicting WCT plugin packages: ${prettyNames}`;
            }
            if (loaded.length < 1) {
                throw `Could not find WCT plugin named "${name}"`;
            }
            return loaded[0];
        });
    }
    /**
     * @param {string} name
     * @return {string} The short form of `name`.
     */
    static shortName(name) {
        for (const prefix of PREFIXES) {
            if (name.indexOf(prefix) === 0) {
                return name.substr(prefix.length);
            }
        }
        return name;
    }
}
// HACK(rictic): Makes es6 style imports happy, so that we can do, e.g.
//     import {Plugin} from './plugin';
Plugin.Plugin = Plugin;
exports.Plugin = Plugin;
// Plugin Loading
// We maintain an identity map of plugins, keyed by short name.
const _loadedPlugins = {};
/**
 * @param {string} packageName Attempts to load a package as a WCT plugin.
 * @return {Plugin}
 */
function _tryLoadPluginPackage(packageName) {
    let packageInfo;
    try {
        packageInfo = require(path.join(packageName, 'package.json'));
    }
    catch (error) {
        if (error.code !== 'MODULE_NOT_FOUND') {
            console.log(error);
        }
        return null;
    }
    // Plugins must have a (truthy) wct-plugin field.
    if (!packageInfo['wct-plugin']) {
        return null;
    }
    // Allow {"wct-plugin": true} as a shorthand.
    const metadata = _.isObject(packageInfo['wct-plugin']) ? packageInfo['wct-plugin'] : {};
    return new Plugin(packageName, metadata);
}
module.exports = Plugin;
