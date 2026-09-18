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
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildPresets = new Map([
    [
        'es5-bundled',
        {
            name: 'es5-bundled',
            js: {
                minify: true,
                compile: 'es5',
                transformModulesToAmd: true,
            },
            css: { minify: true },
            html: { minify: true },
            bundle: true,
            addServiceWorker: true,
            addPushManifest: false,
        }
    ],
    [
        'es6-bundled',
        {
            name: 'es6-bundled',
            browserCapabilities: ['es2015'],
            js: {
                minify: true,
                compile: 'es2015',
                transformModulesToAmd: true,
            },
            css: { minify: true },
            html: { minify: true },
            bundle: true,
            addServiceWorker: true,
            addPushManifest: false,
        }
    ],
    [
        'es6-unbundled',
        {
            name: 'es6-unbundled',
            browserCapabilities: ['es2015', 'push'],
            js: {
                minify: true,
                compile: 'es2015',
                transformModulesToAmd: true,
            },
            css: { minify: true },
            html: { minify: true },
            bundle: false,
            addServiceWorker: true,
            addPushManifest: true,
        }
    ],
    [
        'uncompiled-bundled',
        {
            name: 'uncompiled-bundled',
            browserCapabilities: ['es2018', 'modules'],
            js: { minify: true },
            css: { minify: true },
            html: { minify: true },
            bundle: true,
            addServiceWorker: true,
            addPushManifest: false,
        }
    ],
    [
        'uncompiled-unbundled',
        {
            name: 'uncompiled-unbundled',
            browserCapabilities: ['es2018', 'modules', 'push'],
            js: { minify: true },
            css: { minify: true },
            html: { minify: true },
            bundle: false,
            addServiceWorker: true,
            addPushManifest: true,
        }
    ],
]);
function isValidPreset(presetName) {
    return exports.buildPresets.has(presetName);
}
exports.isValidPreset = isValidPreset;
/**
 * Apply a build preset (if a valid one exists on the config object) by
 * deep merging the given config with the preset values.
 */
function applyBuildPreset(config) {
    const presetName = config.preset;
    if (!presetName || !isValidPreset(presetName)) {
        return config;
    }
    const presetConfig = exports.buildPresets.get(presetName) || {};
    const mergedConfig = Object.assign({}, presetConfig, config);
    // Object.assign is shallow, so we need to make sure we properly merge these
    // deep options as well.
    // NOTE(fks) 05-05-2017: While a little annoying, we use multiple
    // Object.assign() calls here so that we do not filter-out additional
    // user-defined build options at the config level.
    mergedConfig.js = Object.assign({}, presetConfig.js, config.js);
    mergedConfig.css = Object.assign({}, presetConfig.css, config.css);
    mergedConfig.html = Object.assign({}, presetConfig.html, config.html);
    return mergedConfig;
}
exports.applyBuildPreset = applyBuildPreset;
//# sourceMappingURL=builds.js.map