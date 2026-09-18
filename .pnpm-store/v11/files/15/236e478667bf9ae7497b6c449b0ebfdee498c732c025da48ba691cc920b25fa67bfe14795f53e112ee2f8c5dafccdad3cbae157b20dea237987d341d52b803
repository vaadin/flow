import * as babel from 'babel-types';
import { Document, ResolvedUrl } from 'polymer-analyzer';
import { Analysis } from 'polymer-analyzer';
import { AssignedBundle, BundleManifest } from './bundle-manifest';
import { Bundler } from './bundler';
/**
 * Utility class to rollup/merge ES6 modules code using rollup and rewrite
 * import statements to point to appropriate bundles.
 */
export declare class Es6Rewriter {
    bundler: Bundler;
    manifest: BundleManifest;
    bundle: AssignedBundle;
    constructor(bundler: Bundler, manifest: BundleManifest, bundle: AssignedBundle);
    /**
     * Produces a module bundle from given source code string and from analysis of
     * imported ES6 modules.
     * @param url The base URL of the module to roll-up.
     * @param code The source code to roll-up.
     * @param document The optional Document which contains the source code to
     * rollup; this is used to get access to the Analyzer's resolutions of module
     * specifiers encountered in the source code if available.
     * TODO(usergenic): Return a valid source-map along with the code.
     */
    rollup(url: ResolvedUrl, code: string, document?: Document): Promise<{
        code: string;
        map: undefined;
    }>;
    getEs6ImportResolutions(document: Document): Map<string, ResolvedUrl>;
    rewriteEs6SourceUrlsToResolved(node: babel.Node, jsImportResolvedUrls: Map<string, ResolvedUrl>): void;
    rewriteExportAllToNamedExports(node: babel.Node, analysis: Analysis): void;
    /**
     * Attempts to reduce the number of distinct import declarations by
     * combining those referencing the same source into the same declaration.
     * Results in deduplication of imports of the same item as well.  It
     * should NOT touch dynamic imports at all.
     *
     * Before:
     *     import {a} from './module-1.js';
     *     import {b} from './module-1.js';
     *     import {c} from './module-2.js';
     *     import('./module-3.js');
     *     import('./module-3.js');
     * After:
     *     import {a,b} from './module-1.js';
     *     import {c} from './module-2.js';
     *     import('./module-3.js');
     *     import('./module-3.js');
     */
    private _deduplicateImportStatements;
    /**
     * Rewrite export declarations source URLs to reference the bundle URL for
     * bundled files.
     */
    private _rewriteExportStatements;
    /**
     * Rewrite import declarations source URLs to reference the bundle URL for
     * bundled files and import names to correspond to names as exported by
     * bundles.
     */
    private _rewriteImportStatements;
    /**
     * Extends dynamic import statements to extract the explicitly namespace
     * export for the imported module.
     *
     * Before:
     *     import('./module-a.js')
     *         .then((moduleA) => moduleA.doSomething());
     *
     * After:
     *     import('./bundle_1.js')
     *         .then(bundle => bundle && bundle.$moduleA || {})
     *         .then((moduleA) => moduleA.doSomething());
     */
    private _rewriteDynamicImport;
    /**
     * Changes an import specifier to use the exported name defined in the bundle.
     *
     * Before:
     *     import {something} from './module-a.js';
     *
     * After:
     *     import {something_1} from './bundle_1.js';
     */
    private _rewriteImportSpecifierName;
    /**
     * Changes an import specifier to use the exported name for original module's
     * default as defined in the bundle.
     *
     * Before:
     *     import moduleA from './module-a.js';
     *
     * After:
     *     import {$moduleADefault} from './bundle_1.js';
     */
    private _rewriteImportDefaultSpecifier;
    /**
     * Changes an import specifier to use the exported name for original module's
     * namespace as defined in the bundle.
     *
     * Before:
     *     import * as moduleA from './module-a.js';
     *
     * After:
     *     import {$moduleA} from './bundle_1.js';
     */
    private _rewriteImportNamespaceSpecifier;
    private _rewriteImportMetaToBundleMeta;
}
