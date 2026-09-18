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
/// <reference path="../custom_typings/sw-precache.d.ts" />
const assert = require("assert");
const fs_1 = require("fs");
const path = require("path");
const logging = require("plylog");
const sw_precache_1 = require("sw-precache");
const path_transformers_1 = require("./path-transformers");
const logger = logging.getLogger('polymer-build.service-worker');
/**
 * Given a user-provided AddServiceWorkerOptions object, check for deprecated
 * options. When one is found, warn the user and fix if possible.
 */
// tslint:disable-next-line: no-any Turned off for user input.
function fixDeprecatedOptions(options) {
    if (typeof options.serviceWorkerPath !== 'undefined') {
        logger.warn('"serviceWorkerPath" config option has been renamed to "path" and will no longer be supported in future versions');
        options.path = options.path || options.serviceWorkerPath;
    }
    if (typeof options.swConfig !== 'undefined') {
        logger.warn('"swConfig" config option has been renamed to "swPrecacheConfig" and will no longer be supported in future versions');
        options.swPrecacheConfig = options.swPrecacheConfig || options.swConfig;
    }
    return options;
}
/**
 * Returns an array of file paths for the service worker to precache, based on
 * the information provided in the DepsIndex object.
 */
function getPrecachedAssets(depsIndex, project) {
    const precachedAssets = new Set(project.config.allFragments);
    precachedAssets.add(project.config.entrypoint);
    for (const depImports of depsIndex.fragmentToFullDeps.values()) {
        depImports.imports.forEach((s) => precachedAssets.add(s));
        depImports.scripts.forEach((s) => precachedAssets.add(s));
        depImports.styles.forEach((s) => precachedAssets.add(s));
    }
    return Array.from(precachedAssets);
}
/**
 * Returns an array of file paths for the service worker to precache for a
 * BUNDLED build, based on the information provided in the DepsIndex object.
 */
function getBundledPrecachedAssets(project) {
    const precachedAssets = new Set(project.config.allFragments);
    precachedAssets.add(project.config.entrypoint);
    return Array.from(precachedAssets);
}
// Matches URLs like "/foo.png/bar" but not "/foo/bar.png".
exports.hasNoFileExtension = /\/[^\/\.]*(\?|$)/;
/**
 * Returns a promise that resolves with a generated service worker
 * configuration.
 */
function generateServiceWorkerConfig(options) {
    return __awaiter(this, void 0, void 0, function* () {
        assert(!!options, '`project` & `buildRoot` options are required');
        assert(!!options.project, '`project` option is required');
        assert(!!options.buildRoot, '`buildRoot` option is required');
        options = fixDeprecatedOptions(options);
        options = Object.assign({}, options);
        const project = options.project;
        const buildRoot = options.buildRoot;
        const swPrecacheConfig = Object.assign({}, options.swPrecacheConfig);
        const depsIndex = yield project.analyzer.analyzeDependencies;
        let staticFileGlobs = Array.from(swPrecacheConfig.staticFileGlobs || []);
        const precachedAssets = (options.bundled) ?
            getBundledPrecachedAssets(project) :
            getPrecachedAssets(depsIndex, project);
        staticFileGlobs = staticFileGlobs.concat(precachedAssets);
        staticFileGlobs = staticFileGlobs.map((filePath) => {
            if (filePath.startsWith(project.config.root)) {
                filePath = filePath.substring(project.config.root.length);
            }
            return path.join(buildRoot, filePath);
        });
        if (swPrecacheConfig.navigateFallback === undefined) {
            // Map all application routes to the entrypoint.
            swPrecacheConfig.navigateFallback =
                path.relative(project.config.root, project.config.entrypoint);
        }
        if (swPrecacheConfig.navigateFallbackWhitelist === undefined) {
            // Don't fall back to the entrypoint if the URL looks like a static file.
            // We want those to 404 instead, since they are probably missing assets,
            // not application routes. Note it's important that this matches the
            // behavior of prpl-server.
            swPrecacheConfig.navigateFallbackWhitelist = [exports.hasNoFileExtension];
        }
        if (swPrecacheConfig.directoryIndex === undefined) {
            // By default, sw-precache maps any path ending with "/" to "index.html".
            // This is a reasonable default for matching application routes, but 1) our
            // entrypoint might not be called "index.html", and 2) this case is already
            // handled by the navigateFallback configuration above. Simplest to just
            // disable this feature.
            swPrecacheConfig.directoryIndex = '';
        }
        // swPrecache will determine the right urls by stripping buildRoot.
        // NOTE:(usergenic) sw-precache generate() apparently replaces the
        // prefix on an already posixified version of the path on win32.
        //
        // We include a trailing slash in `stripPrefix` so that we remove leading
        // slashes on the pre-cache asset URLs, hence producing relative URLs
        // instead of absolute. We want relative URLs for builds mounted at non-root
        // paths. Note that service worker fetches are relative to its own URL.
        swPrecacheConfig.stripPrefix = addTrailingSlash(path_transformers_1.posixifyPath(buildRoot));
        if (options.basePath) {
            // TODO Drop this feature once CLI doesn't depend on it.
            let replacePrefix = path_transformers_1.posixifyPath(options.basePath);
            if (!replacePrefix.endsWith('/')) {
                replacePrefix = replacePrefix + '/';
            }
            if (swPrecacheConfig.replacePrefix) {
                console.info(`Replacing service worker configuration's ` +
                    `replacePrefix option (${swPrecacheConfig.replacePrefix}) ` +
                    `with the build configuration's basePath (${replacePrefix}).`);
            }
            swPrecacheConfig.replacePrefix = replacePrefix;
        }
        // static files will be pre-cached
        swPrecacheConfig.staticFileGlobs = staticFileGlobs;
        // Log service-worker helpful output at the debug log level
        swPrecacheConfig.logger = swPrecacheConfig.logger || logger.debug;
        return swPrecacheConfig;
    });
}
exports.generateServiceWorkerConfig = generateServiceWorkerConfig;
/**
 * Returns a promise that resolves with a generated service worker (the file
 * contents), based off of the options provided.
 */
function generateServiceWorker(options) {
    return __awaiter(this, void 0, void 0, function* () {
        const swPrecacheConfig = yield generateServiceWorkerConfig(options);
        return yield (new Promise((resolve, reject) => {
            logger.debug(`writing service worker...`, swPrecacheConfig);
            sw_precache_1.generate(swPrecacheConfig, (err, fileContents) => {
                if (err || fileContents == null) {
                    reject(err || 'No file contents provided.');
                }
                else {
                    // Note: Node 10 Function.prototype.toString() produces output
                    // like `function() { }` where earlier versions produce
                    // `function () { }` (note the space between function keyword)
                    // and parentheses.  To ensure the output is consistent across
                    // versions, we will correctively insert missing space here.
                    fileContents = fileContents.replace(/\bfunction\(/g, 'function (');
                    resolve(Buffer.from(fileContents));
                }
            });
        }));
    });
}
exports.generateServiceWorker = generateServiceWorker;
/**
 * Returns a promise that resolves when a service worker has been generated
 * and written to the build directory. This uses generateServiceWorker() to
 * generate a service worker, which it then writes to the file system based on
 * the buildRoot & path (if provided) options.
 */
function addServiceWorker(options) {
    return generateServiceWorker(options).then((fileContents) => {
        return new Promise((resolve, reject) => {
            const serviceWorkerPath = path.join(options.buildRoot, options.path || 'service-worker.js');
            fs_1.writeFile(serviceWorkerPath, fileContents, (err) => {
                if (err) {
                    reject(err);
                }
                else {
                    resolve();
                }
            });
        });
    });
}
exports.addServiceWorker = addServiceWorker;
function addTrailingSlash(s) {
    return s.endsWith('/') ? s : s + '/';
}
//# sourceMappingURL=service-worker.js.map