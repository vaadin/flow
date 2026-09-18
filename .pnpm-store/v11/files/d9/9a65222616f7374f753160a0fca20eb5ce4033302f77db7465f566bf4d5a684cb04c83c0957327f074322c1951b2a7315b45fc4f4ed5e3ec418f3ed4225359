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
const assert = require("assert");
const clone = require("clone");
const deps_index_1 = require("./deps-index");
const url_utils_1 = require("./url-utils");
const utils_1 = require("./utils");
exports.bundleTypeExtnames = new Map([
    ['es6-module', '.js'],
    ['html-fragment', '.html'],
]);
/**
 * A bundle is a grouping of files which serve the need of one or more
 * entrypoint files.
 */
class Bundle {
    constructor(
    // Filetype discriminator for Bundles.
    type, 
    // Set of all dependant entrypoint URLs of this bundle.
    entrypoints = new Set(), 
    // Set of all files included in the bundle.
    files = new Set()) {
        this.type = type;
        this.entrypoints = entrypoints;
        this.files = files;
        // Set of imports which should be removed when encountered.
        this.stripImports = new Set();
        // Set of imports which could not be loaded.
        this.missingImports = new Set();
        // These sets are updated as bundling occurs.
        this.inlinedHtmlImports = new Set();
        this.inlinedScripts = new Set();
        this.inlinedStyles = new Set();
        // Maps the URLs of bundled ES6 modules to a map of their original exported
        // names to names which may have been rewritten to prevent conflicts.
        this.bundledExports = new Map();
    }
    get extname() {
        return exports.bundleTypeExtnames.get(this.type);
    }
}
exports.Bundle = Bundle;
/**
 * Represents a bundle assigned to an output URL.
 */
class AssignedBundle {
}
exports.AssignedBundle = AssignedBundle;
/**
 * A bundle manifest is a mapping of URLs to bundles.
 */
class BundleManifest {
    /**
     * Given a collection of bundles and a BundleUrlMapper to generate URLs for
     * them, the constructor populates the `bundles` and `files` index properties.
     */
    constructor(bundles, urlMapper) {
        this.bundles = urlMapper(Array.from(bundles));
        this._bundleUrlForFile = new Map();
        for (const bundleMapEntry of this.bundles) {
            const bundleUrl = bundleMapEntry[0];
            const bundle = bundleMapEntry[1];
            for (const fileUrl of bundle.files) {
                assert(!this._bundleUrlForFile.has(fileUrl));
                this._bundleUrlForFile.set(fileUrl, bundleUrl);
            }
        }
    }
    // Returns a clone of the manifest.
    fork() {
        return clone(this);
    }
    // Convenience method to return a bundle for a constituent file URL.
    getBundleForFile(url) {
        const bundleUrl = this._bundleUrlForFile.get(url);
        if (bundleUrl) {
            return { bundle: this.bundles.get(bundleUrl), url: bundleUrl };
        }
    }
    toJson(urlResolver) {
        const json = {};
        const missingImports = new Set();
        for (const [url, bundle] of this.bundles) {
            json[urlResolver.relative(url)] =
                [...new Set([
                        // `files` and `inlinedHtmlImports` will be partially
                        // duplicative, but use of both ensures the basis document
                        // for a file is included since there is no other specific
                        // property that currently expresses it.
                        ...bundle.files,
                        ...bundle.inlinedHtmlImports,
                        ...bundle.inlinedScripts,
                        ...bundle.inlinedStyles
                    ])].map((url) => urlResolver.relative(url));
            for (const missingImport of bundle.missingImports) {
                missingImports.add(missingImport);
            }
        }
        if (missingImports.size > 0) {
            json['_missing'] = [...missingImports].map((url) => urlResolver.relative(url));
        }
        return json;
    }
}
exports.BundleManifest = BundleManifest;
/**
 * Chains multiple bundle strategy functions together so the output of one
 * becomes the input of the next and so-on.
 */
function composeStrategies(strategies) {
    return strategies.reduce((s1, s2) => (b) => s2(s1(b)));
}
exports.composeStrategies = composeStrategies;
/**
 * Given an index of files and their dependencies, produce an array of bundles,
 * where a bundle is defined for each set of dependencies.
 *
 * For example, a dependency index representing the graph:
 *   `a->b, b->c, b->d, e->c, e->f`
 *
 * Would produce an array of three bundles:
 *   `[a]->[a,b,d], [e]->[e,f], [a,e]->[c]`
 */
function generateBundles(depsIndex) {
    const bundles = [];
    // TODO(usergenic): Assert a valid transitive dependencies map; i.e.
    // entrypoints should include themselves as dependencies and entrypoints
    // should *probably* not include other entrypoints as dependencies.
    const invertedIndex = invertMultimap(depsIndex);
    for (const entry of invertedIndex.entries()) {
        const dep = entry[0];
        const entrypoints = entry[1];
        // Find the bundle defined by the specific set of shared dependant
        // entrypoints.
        let bundle = bundles.find((bundle) => setEquals(entrypoints, bundle.entrypoints));
        if (!bundle) {
            const type = getBundleTypeForUrl([...entrypoints][0]);
            bundle = new Bundle(type, entrypoints);
            bundles.push(bundle);
        }
        bundle.files.add(dep);
    }
    return bundles;
}
exports.generateBundles = generateBundles;
/**
 * Instances of `<script type="module">` generate synthetic entrypoints in the
 * depsIndex and are treated as entrypoints during the initial phase of
 * `generateBundles`.  Any bundle which provides dependencies to a single
 * synthetic entrypoint of this type (aka a single entrypoint sub-bundle) are
 * merged back into the bundle for the HTML containing the script tag.
 *
 * For example, the following bundles:
 *   `[a]->[a], [a>1]->[x], [a>1,a>2]->[y], [a>2]->[z]`
 *
 * Would be merged into the following set of bundles:
 *   `[a]->[a,x,z], [a>1,a>2]->[y]`
 *
 * `a>1` and `a>2` represent script tag entrypoints. Only `x` and `z` are
 * bundled with `a` because they each serve only a single script tag entrypoint.
 * `y` has to be in a separate bundle so that it is not inlined into bundle `a`
 * in both script tags.
 */
function mergeSingleEntrypointSubBundles(bundles) {
    for (const subBundle of [...bundles]) {
        if (subBundle.entrypoints.size !== 1) {
            continue;
        }
        const entrypointUrl = [...subBundle.entrypoints][0];
        const superBundleUrl = deps_index_1.getSuperBundleUrl(entrypointUrl);
        // If the entrypoint URL is the same as the super bundle URL then the
        // entrypoint URL has not changed and did not represent a sub bundle, so
        // continue to next candidate sub bundle.
        if (entrypointUrl === superBundleUrl) {
            continue;
        }
        const superBundleIndex = bundles.findIndex((b) => b.files.has(superBundleUrl));
        if (superBundleIndex < 0) {
            continue;
        }
        const superBundle = bundles[superBundleIndex];
        // The synthetic entrypoint identifier does not need to be represented in
        // the super bundle's entrypoints list, so we'll clear the sub-bundle's
        // entrypoints in the bundle before merging.
        subBundle.entrypoints.clear();
        const mergedBundle = mergeBundles([superBundle, subBundle], true);
        bundles.splice(superBundleIndex, 1, mergedBundle);
        const subBundleIndex = bundles.findIndex((b) => b === subBundle);
        bundles.splice(subBundleIndex, 1);
    }
}
exports.mergeSingleEntrypointSubBundles = mergeSingleEntrypointSubBundles;
/**
 * Creates a bundle URL mapper function which takes a prefix and appends an
 * incrementing value, starting with `1` to the filename.
 */
function generateCountingSharedBundleUrlMapper(urlPrefix) {
    return generateSharedBundleUrlMapper((sharedBundles) => {
        let counter = 0;
        return sharedBundles.map((b) => `${urlPrefix}${++counter}${b.extname}`);
    });
}
exports.generateCountingSharedBundleUrlMapper = generateCountingSharedBundleUrlMapper;
/**
 * Generates a strategy function which finds all non-entrypoint bundles which
 * are dependencies of the given entrypoint and merges them into that
 * entrypoint's bundle.
 */
function generateEagerMergeStrategy(entrypoint) {
    return generateMatchMergeStrategy((b) => b.files.has(entrypoint) ||
        b.entrypoints.has(entrypoint) && !getBundleEntrypoint(b));
}
exports.generateEagerMergeStrategy = generateEagerMergeStrategy;
/**
 * Generates a strategy function which finds all bundles matching the predicate
 * function and merges them into the bundle containing the target file.
 */
function generateMatchMergeStrategy(predicate) {
    return (bundles) => mergeMatchingBundles(bundles, predicate);
}
exports.generateMatchMergeStrategy = generateMatchMergeStrategy;
/**
 * Creates a bundle URL mapper function which maps non-shared bundles to the
 * URLs of their single entrypoint and yields responsibility for naming
 * remaining shared bundle URLs to the `mapper` function argument.  The
 * mapper function takes a collection of shared bundles and a URL map, calling
 * `.set(url, bundle)` for each.
 */
function generateSharedBundleUrlMapper(mapper) {
    return (bundles) => {
        const urlMap = new Map();
        const sharedBundles = [];
        for (const bundle of bundles) {
            const bundleUrl = getBundleEntrypoint(bundle);
            if (bundleUrl) {
                urlMap.set(bundleUrl, bundle);
            }
            else {
                sharedBundles.push(bundle);
            }
        }
        mapper(sharedBundles)
            .forEach((url, i) => urlMap.set(url, sharedBundles[i]));
        return urlMap;
    };
}
exports.generateSharedBundleUrlMapper = generateSharedBundleUrlMapper;
/**
 * Generates a strategy function to merge all bundles where the dependencies
 * for a bundle are shared by at least 2 entrypoints (default; set
 * `minEntrypoints` to change threshold).
 *
 * This function will convert an array of 4 bundles:
 *   `[a]->[a,b], [a,c]->[d], [c]->[c,e], [f,g]->[f,g,h]`
 *
 * Into the following 3 bundles, including a single bundle for all of the
 * dependencies which are shared by at least 2 entrypoints:
 *   `[a]->[a,b], [c]->[c,e], [a,c,f,g]->[d,f,g,h]`
 */
function generateSharedDepsMergeStrategy(maybeMinEntrypoints) {
    const minEntrypoints = maybeMinEntrypoints === undefined ? 2 : maybeMinEntrypoints;
    if (minEntrypoints < 0) {
        throw new Error(`Minimum entrypoints argument must be non-negative`);
    }
    return generateMatchMergeStrategy((b) => b.entrypoints.size >= minEntrypoints && !getBundleEntrypoint(b));
}
exports.generateSharedDepsMergeStrategy = generateSharedDepsMergeStrategy;
/**
 * A bundle strategy function which merges all shared dependencies into a
 * bundle for an application shell.
 */
function generateShellMergeStrategy(shell, maybeMinEntrypoints) {
    const minEntrypoints = maybeMinEntrypoints === undefined ? 2 : maybeMinEntrypoints;
    if (minEntrypoints < 0) {
        throw new Error(`Minimum entrypoints argument must be non-negative`);
    }
    return composeStrategies([
        // Merge all bundles that are direct dependencies of the shell into the
        // shell.
        generateEagerMergeStrategy(shell),
        // Create a new bundle which contains the contents of all bundles which
        // either...
        generateMatchMergeStrategy((bundle) => {
            // ...contain the shell file
            return bundle.files.has(shell) ||
                // or are dependencies of at least the minimum number of
                // entrypoints and are not entrypoints themselves.
                bundle.entrypoints.size >= minEntrypoints &&
                    !getBundleEntrypoint(bundle);
        }),
        // Don't link to the shell from other bundles.
        generateNoBackLinkStrategy([shell]),
    ]);
}
exports.generateShellMergeStrategy = generateShellMergeStrategy;
/**
 * Generates a strategy function that ensures bundles do not link to given URLs.
 * Bundles which contain matching files will still have them inlined.
 */
function generateNoBackLinkStrategy(urls) {
    return (bundles) => {
        for (const bundle of bundles) {
            for (const url of urls) {
                if (!bundle.files.has(url)) {
                    bundle.stripImports.add(url);
                }
            }
        }
        return bundles;
    };
}
exports.generateNoBackLinkStrategy = generateNoBackLinkStrategy;
/**
 * Given an Array of bundles, produce a single bundle with the entrypoints and
 * files of all bundles represented.  By default, bundles of different types
 * can not be merged, but this constraint can be skipped by providing
 * `ignoreTypeCheck` argument with value `true`, which is necessary to merge a
 * bundle containining an inline document's unique transitive dependencies, as
 * inline documents typically are of different type (`<script type="module">`
 * within HTML document contains JavaScript document).
 */
function mergeBundles(bundles, ignoreTypeCheck = false) {
    if (bundles.length === 0) {
        throw new Error('Can not merge 0 bundles.');
    }
    const bundleTypes = utils_1.uniq(bundles, (b) => b.type);
    if (!ignoreTypeCheck && bundleTypes.size > 1) {
        throw new Error('Can not merge bundles of different types: ' +
            [...bundleTypes].join(' and '));
    }
    const bundleType = bundles[0].type;
    const newBundle = new Bundle(bundleType);
    for (const { entrypoints, files, inlinedHtmlImports, inlinedScripts, inlinedStyles, bundledExports, } of bundles) {
        newBundle.entrypoints =
            new Set([...newBundle.entrypoints, ...entrypoints]);
        newBundle.files = new Set([...newBundle.files, ...files]);
        newBundle.inlinedHtmlImports = new Set([...newBundle.inlinedHtmlImports, ...inlinedHtmlImports]);
        newBundle.inlinedScripts =
            new Set([...newBundle.inlinedScripts, ...inlinedScripts]);
        newBundle.inlinedStyles =
            new Set([...newBundle.inlinedStyles, ...inlinedStyles]);
        newBundle.bundledExports = new Map([...newBundle.bundledExports, ...bundledExports]);
    }
    return newBundle;
}
exports.mergeBundles = mergeBundles;
/**
 * Return a new bundle array where bundles within it matching the predicate
 * are merged together.  Note that merge operations are segregated by type so
 * that no attempt to merge bundles of different types will occur.
 */
function mergeMatchingBundles(bundles, predicate) {
    const newBundles = bundles.filter((b) => !predicate(b));
    const bundlesToMerge = utils_1.partitionMap(bundles.filter((b) => !newBundles.includes(b)), (b) => b.type);
    for (const bundlesOfType of bundlesToMerge.values()) {
        newBundles.push(mergeBundles(bundlesOfType));
    }
    return newBundles;
}
exports.mergeMatchingBundles = mergeMatchingBundles;
/**
 * Return the single entrypoint that represents the given bundle, or null
 * if bundle contains more or less than a single file URL matching URLs
 * in its entrypoints set. This makes it convenient to identify whether a
 * bundle is a named fragment or whether it is simply a shared bundle
 * of some kind.
 */
function getBundleEntrypoint(bundle) {
    let bundleEntrypoint = null;
    for (const entrypoint of bundle.entrypoints) {
        if (bundle.files.has(entrypoint)) {
            if (bundleEntrypoint) {
                return null;
            }
            bundleEntrypoint = entrypoint;
        }
    }
    return bundleEntrypoint;
}
/**
 * Generally bundle types are determined by the file extension of the URL,
 * though in the case of sub-bundles, the bundle type is the last segment of the
 * `>` delimited URL, (e.g. `page1.html>inline#1>es6-module`).
 */
function getBundleTypeForUrl(url) {
    const segments = url.split('>');
    if (segments.length === 1) {
        const extname = url_utils_1.getFileExtension(segments[0]);
        return extname === '.js' ? 'es6-module' : 'html-fragment';
    }
    if (segments.length === 0) {
        throw new Error(`ResolvedUrl "${url}" is empty/invalid.`);
    }
    return segments.pop();
}
/**
 * Inverts a map of collections such that  `{a:[c,d], b:[c,e]}` would become
 * `{c:[a,b], d:[a], e:[b]}`.
 */
function invertMultimap(multimap) {
    const inverted = new Map();
    for (const [value, keys] of multimap.entries()) {
        for (const key of keys) {
            const set = inverted.get(key) || new Set();
            set.add(value);
            inverted.set(key, set);
        }
    }
    return inverted;
}
/**
 * Returns true if both sets contain exactly the same items.  This check is
 * order-independent.
 */
function setEquals(set1, set2) {
    if (set1.size !== set2.size) {
        return false;
    }
    for (const item of set1) {
        if (!set2.has(item)) {
            return false;
        }
    }
    return true;
}
//# sourceMappingURL=bundle-manifest.js.map