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
import { PackageRelativeUrl, ResolvedUrl, UrlResolver } from 'polymer-analyzer';
/**
 * A bundle strategy function is used to transform an array of bundles.
 */
export declare type BundleStrategy = (bundles: Bundle[]) => Bundle[];
/**
 * A bundle URL mapper function produces a map of URLs to bundles.
 */
export declare type BundleUrlMapper = (bundles: Bundle[]) => Map<ResolvedUrl, Bundle>;
/**
 * A mapping of entrypoints to their full set of transitive dependencies,
 * such that a dependency graph `a->c, c->d, d->e, b->d, b->f` would be
 * represented `{a:[a,c,d,e], b:[b,d,e,f]}`.  Please note that there is an
 * explicit identity dependency (`a` depends on `a`, `b` depends on `b`).
 */
export declare type TransitiveDependenciesMap = Map<ResolvedUrl, Set<ResolvedUrl>>;
/**
 * The output format of the bundle.
 */
export declare type BundleType = 'html-fragment' | 'es6-module';
export declare const bundleTypeExtnames: Map<BundleType, string>;
/**
 * A bundle is a grouping of files which serve the need of one or more
 * entrypoint files.
 */
export declare class Bundle {
    type: BundleType;
    entrypoints: Set<ResolvedUrl>;
    files: Set<ResolvedUrl>;
    stripImports: Set<ResolvedUrl>;
    missingImports: Set<ResolvedUrl>;
    inlinedHtmlImports: Set<ResolvedUrl>;
    inlinedScripts: Set<ResolvedUrl>;
    inlinedStyles: Set<ResolvedUrl>;
    bundledExports: Map<ResolvedUrl, Map<string, string>>;
    constructor(type: BundleType, entrypoints?: Set<ResolvedUrl>, files?: Set<ResolvedUrl>);
    readonly extname: string | undefined;
}
/**
 * Represents a bundle assigned to an output URL.
 */
export declare class AssignedBundle {
    bundle: Bundle;
    url: ResolvedUrl;
}
export interface BundleManifestJson {
    [entrypoint: string]: PackageRelativeUrl[];
}
/**
 * A bundle manifest is a mapping of URLs to bundles.
 */
export declare class BundleManifest {
    bundles: Map<ResolvedUrl, Bundle>;
    private _bundleUrlForFile;
    /**
     * Given a collection of bundles and a BundleUrlMapper to generate URLs for
     * them, the constructor populates the `bundles` and `files` index properties.
     */
    constructor(bundles: Bundle[], urlMapper: BundleUrlMapper);
    fork(): BundleManifest;
    getBundleForFile(url: ResolvedUrl): AssignedBundle | undefined;
    toJson(urlResolver: UrlResolver): BundleManifestJson;
}
/**
 * Chains multiple bundle strategy functions together so the output of one
 * becomes the input of the next and so-on.
 */
export declare function composeStrategies(strategies: BundleStrategy[]): BundleStrategy;
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
export declare function generateBundles(depsIndex: TransitiveDependenciesMap): Bundle[];
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
export declare function mergeSingleEntrypointSubBundles(bundles: Bundle[]): void;
/**
 * Creates a bundle URL mapper function which takes a prefix and appends an
 * incrementing value, starting with `1` to the filename.
 */
export declare function generateCountingSharedBundleUrlMapper(urlPrefix: ResolvedUrl): BundleUrlMapper;
/**
 * Generates a strategy function which finds all non-entrypoint bundles which
 * are dependencies of the given entrypoint and merges them into that
 * entrypoint's bundle.
 */
export declare function generateEagerMergeStrategy(entrypoint: ResolvedUrl): BundleStrategy;
/**
 * Generates a strategy function which finds all bundles matching the predicate
 * function and merges them into the bundle containing the target file.
 */
export declare function generateMatchMergeStrategy(predicate: (b: Bundle) => boolean): BundleStrategy;
/**
 * Creates a bundle URL mapper function which maps non-shared bundles to the
 * URLs of their single entrypoint and yields responsibility for naming
 * remaining shared bundle URLs to the `mapper` function argument.  The
 * mapper function takes a collection of shared bundles and a URL map, calling
 * `.set(url, bundle)` for each.
 */
export declare function generateSharedBundleUrlMapper(mapper: (sharedBundles: Bundle[]) => ResolvedUrl[]): BundleUrlMapper;
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
export declare function generateSharedDepsMergeStrategy(maybeMinEntrypoints?: number): BundleStrategy;
/**
 * A bundle strategy function which merges all shared dependencies into a
 * bundle for an application shell.
 */
export declare function generateShellMergeStrategy(shell: ResolvedUrl, maybeMinEntrypoints?: number): BundleStrategy;
/**
 * Generates a strategy function that ensures bundles do not link to given URLs.
 * Bundles which contain matching files will still have them inlined.
 */
export declare function generateNoBackLinkStrategy(urls: ResolvedUrl[]): BundleStrategy;
/**
 * Given an Array of bundles, produce a single bundle with the entrypoints and
 * files of all bundles represented.  By default, bundles of different types
 * can not be merged, but this constraint can be skipped by providing
 * `ignoreTypeCheck` argument with value `true`, which is necessary to merge a
 * bundle containining an inline document's unique transitive dependencies, as
 * inline documents typically are of different type (`<script type="module">`
 * within HTML document contains JavaScript document).
 */
export declare function mergeBundles(bundles: Bundle[], ignoreTypeCheck?: boolean): Bundle;
/**
 * Return a new bundle array where bundles within it matching the predicate
 * are merged together.  Note that merge operations are segregated by type so
 * that no attempt to merge bundles of different types will occur.
 */
export declare function mergeMatchingBundles(bundles: Bundle[], predicate: (bundle: Bundle) => boolean): Bundle[];
