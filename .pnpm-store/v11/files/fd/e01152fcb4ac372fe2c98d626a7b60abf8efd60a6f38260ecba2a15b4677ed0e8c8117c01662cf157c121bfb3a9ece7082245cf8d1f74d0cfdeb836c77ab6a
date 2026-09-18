"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const html_script_tag_1 = require("polymer-analyzer/lib/html/html-script-tag");
const analyzer_utils_1 = require("./analyzer-utils");
/**
 * Analyzes all entrypoints and determines each of their transitive
 * dependencies.
 * @param entrypoints Urls of entrypoints to analyze.
 * @param analyzer
 * @return a dependency index of every entrypoint, including entrypoints that
 *     were discovered as lazy entrypoints in the graph.
 */
function buildDepsIndex(entrypoints, analyzer) {
    return __awaiter(this, void 0, void 0, function* () {
        const depsIndex = new Map();
        const analysis = yield analyzer.analyze(entrypoints);
        const allEntrypoints = new Set(entrypoints);
        const inlineDocuments = new Map();
        // Note: the following iteration takes place over a Set which may be added
        // to from within the loop.
        for (const entrypoint of allEntrypoints) {
            let document;
            try {
                document = inlineDocuments.has(entrypoint) ?
                    inlineDocuments.get(entrypoint) :
                    analyzer_utils_1.getAnalysisDocument(analysis, entrypoint);
            }
            catch (e) {
                console.warn(e.message);
            }
            if (document) {
                const deps = getDependencies(analyzer, document);
                depsIndex.set(entrypoint, new Set([
                    ...(document.isInline ? [] : [document.url]),
                    ...deps.eagerDeps
                ]));
                // Add lazy imports to the set of all entrypoints, which supports
                // recursive
                for (const dep of deps.lazyImports) {
                    allEntrypoints.add(dep);
                }
                // Treat top-level module imports as entrypoints by creating "sub-bundle"
                // URLs to enable the inline document to be processed as an entrypoint
                // document on a subsequent iteration of the outer loop over all
                // entrypoints.
                for (const [id, imported] of deps.moduleScriptImports) {
                    const subBundleUrl = getSubBundleUrl(document.url, id);
                    allEntrypoints.add(subBundleUrl);
                    inlineDocuments.set(subBundleUrl, imported);
                }
            }
        }
        return depsIndex;
    });
}
exports.buildDepsIndex = buildDepsIndex;
/**
 * Constructs a ResolvedUrl to identify a sub bundle, which is a concatenation
 * of the super bundle or containing file's URL and an id for the sub-bundle.
 */
function getSubBundleUrl(superBundleUrl, id) {
    return `${superBundleUrl}>${id}`;
}
exports.getSubBundleUrl = getSubBundleUrl;
/**
 * Strips the sub-bundle id off the end of a URL to return the super bundle or
 * containing file's URL.  If there is no sub-bundle id on the provided URL, the
 * result is essentially a NOOP, since nothing will have been stripped.
 *
 * Sub-Bundle URL for an inline ES6 module document:
 *     file:///my-project/src/page.html>inline#1>es6-module
 *
 * Super-bundle URL for the containing HTML document:
 *     file:///my-project/src/page.html
 */
function getSuperBundleUrl(subBundleUrl) {
    return subBundleUrl.split('>').shift();
}
exports.getSuperBundleUrl = getSuperBundleUrl;
/**
 * These are the options included in every `Document#getFeatures` call, DRY'd up
 * here for brevity and consistency.
 */
const getFeaturesOptions = {
    imported: false,
    externalPackages: true,
    excludeBackreferences: true,
};
/**
 * For a given document, return a set of transitive dependencies, including
 * all eagerly-loaded dependencies and lazy html imports encountered.
 */
function getDependencies(analyzer, document) {
    const deps = new Set();
    const eagerDeps = new Set();
    const lazyImports = new Set();
    const moduleScriptImports = new Map();
    _getDependencies(document, true);
    return { deps, eagerDeps, lazyImports, moduleScriptImports };
    function _getDependencies(document, viaEager) {
        // HTML document dependencies include external modules referenced by script
        // src attribute, external modules imported by inline module import
        // statements, and HTML imports (recursively).
        if (document.kinds.has('html-document')) {
            _getHtmlExternalModuleDependencies(document);
            _getHtmlInlineModuleDependencies(document);
            _getImportDependencies(document.getFeatures(Object.assign({ kind: 'html-import' }, getFeaturesOptions)), viaEager);
        }
        // JavaScript documents, when parsed as modules, have dependencies defined
        // by their import statements.
        if (document.kinds.has('js-document')) {
            _getImportDependencies(
            // TODO(usergenic): We should be able to filter here on:
            // `.filter((d) => d.parsedAsSourceType === 'module')`
            // here, but Analyzer wont report that if there are no
            // import/export statements in the imported file.
            document.getFeatures(Object.assign({ kind: 'js-import' }, getFeaturesOptions)), viaEager);
        }
    }
    function _getHtmlExternalModuleDependencies(document) {
        let externalModuleCount = 0;
        const htmlScripts = [...document.getFeatures(Object.assign({ kind: 'html-script' }, getFeaturesOptions))]
            .filter((i) => i.document !== undefined &&
            i instanceof html_script_tag_1.ScriptTagImport && i.isModule);
        for (const htmlScript of htmlScripts) {
            const relativeUrl = analyzer.urlResolver.relative(document.url, htmlScript.document.url);
            moduleScriptImports.set(`external#${++externalModuleCount}>${relativeUrl}>es6-module`, htmlScript.document);
        }
    }
    function _getHtmlInlineModuleDependencies(document) {
        let jsDocumentCount = 0;
        const jsDocuments = [...document.getFeatures(Object.assign({ kind: 'js-document' }, getFeaturesOptions))]
            .filter((d) => d.kinds.has('inline-document') &&
            d.parsedDocument.parsedAsSourceType === 'module');
        for (const jsDocument of jsDocuments) {
            moduleScriptImports.set(`inline#${++jsDocumentCount}>es6-module`, jsDocument);
        }
    }
    function _getImportDependencies(imports, viaEager) {
        for (const imprt of imports) {
            if (imprt.document === undefined) {
                continue;
            }
            const importUrl = imprt.document.url;
            if (imprt.lazy) {
                lazyImports.add(importUrl);
            }
            if (eagerDeps.has(importUrl)) {
                continue;
            }
            const isEager = viaEager && !lazyImports.has(importUrl);
            if (isEager) {
                // In this case we've visited a node eagerly for the first time,
                // so recurse.
                eagerDeps.add(importUrl);
            }
            else if (deps.has(importUrl)) {
                // In this case we're seeing a node lazily again, so don't recurse
                continue;
            }
            deps.add(importUrl);
            _getDependencies(imprt.document, isEager);
        }
    }
}
//# sourceMappingURL=deps-index.js.map