/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
/**
 * Configuration for declaration generation.
 */
export interface Config {
    /**
     * Skip source files whose paths match any of these glob patterns. If
     * undefined, defaults to excluding "index.html" and directories ending in
     * "test" or "demo".
     */
    excludeFiles?: string[];
    /**
     * The same as `excludeFiles`, for backwards compatibility. Will be removed in
     * next major version.
     */
    exclude?: string[];
    /**
     * Do not emit any declarations for features that have any of these
     * identifiers.
     */
    excludeIdentifiers?: string[];
    /**
     * Remove any triple-slash references to these files, specified as paths
     * relative to the analysis root directory.
     */
    removeReferences?: string[];
    /**
     * Additional files to insert as triple-slash reference statements. Given the
     * map `a: b[]`, a will get an additional reference statement for each file
     * path in b. All paths are relative to the analysis root directory.
     */
    addReferences?: {
        [filepath: string]: string[];
    };
    /**
     * Whenever a type with a name in this map is encountered, replace it with
     * the given name. Note this only applies to named types found in places like
     * function/method parameters and return types. It does not currently rename
     * e.g. entire generated classes.
     */
    renameTypes?: {
        [name: string]: string;
    };
    /**
     * A map from an ES module path (relative to the analysis root directory) to
     * an array of identifiers exported by that module. If any of those
     * identifiers are encountered in a generated typings file, an import for that
     * identifier from the specified module will be inserted into the typings
     * file.
     */
    autoImport?: {
        [modulePath: string]: string[];
    };
    /**
     * If true, outputs declarations in 'goog:' modules instead of using
     * simple ES modules. This is a temporary hack to account for how modules are
     * resolved for TypeScript inside google3. This is probably not at all useful
     * for anyone but the Polymer team.
     */
    googModules?: boolean;
    /**
     * If true, does not log warnings detected when analyzing code,
     * only diagnostics of Error severity.
     */
    hideWarnings?: boolean;
}
/**
 * Analyze all files in the given directory using Polymer Analyzer, and return
 * TypeScript declaration document strings in a map keyed by relative path.
 */
export declare function generateDeclarations(rootDir: string, config: Config): Promise<Map<string, string>>;
