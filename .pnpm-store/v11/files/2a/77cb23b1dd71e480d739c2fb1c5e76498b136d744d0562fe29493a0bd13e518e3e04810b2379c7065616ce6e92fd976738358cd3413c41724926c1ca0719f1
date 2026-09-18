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
const analyzer_utils_1 = require("./analyzer-utils");
const url_utils_1 = require("./url-utils");
const utils_1 = require("./utils");
/**
 * Looks up and/or defines the unique name for an item exported with the given
 * name in a module within a bundle.
 */
function getOrSetBundleModuleExportName(bundle, moduleUrl, name) {
    let moduleExports = bundle.bundle.bundledExports.get(moduleUrl);
    const bundledExports = bundle.bundle.bundledExports;
    if (!moduleExports) {
        moduleExports = new Map();
        bundledExports.set(moduleUrl, moduleExports);
    }
    let exportName = moduleExports.get(name);
    if (!exportName) {
        let trialName = name;
        const moduleFileNameIdentifier = '$' + utils_1.camelCase(url_utils_1.getFileName(moduleUrl).replace(/\.[a-z0-9_]+$/, ''));
        trialName =
            trialName.replace(/^default$/, `${moduleFileNameIdentifier}Default`)
                .replace(/^\*$/, moduleFileNameIdentifier)
                .replace(/[^a-z0-9_]/gi, '$');
        while (!exportName) {
            if ([...bundledExports.values()].every((map) => [...map.values()].indexOf(trialName) === -1)) {
                exportName = trialName;
            }
            else {
                if (trialName.match(/\$[0-9]+$/)) {
                    trialName = trialName.replace(/[0-9]+$/, (v) => `${parseInt(v) + 1}`);
                }
                else {
                    trialName = `${trialName}$1`;
                }
            }
        }
        moduleExports.set(name, exportName);
    }
    return exportName;
}
exports.getOrSetBundleModuleExportName = getOrSetBundleModuleExportName;
/**
 * Returns a set of every name exported by a module.
 */
function getModuleExportNames(document) {
    const exports_ = document.getFeatures({ kind: 'export' });
    const identifiers = new Set();
    for (const export_ of exports_) {
        for (const identifier of export_.identifiers) {
            identifiers.add(identifier);
        }
    }
    return identifiers;
}
exports.getModuleExportNames = getModuleExportNames;
/**
 * Ensures that exported names from modules which have the same URL as their
 * bundle will have precedence over other module exports, which will be
 * counter-suffixed in the event of name collisions.  This has no technical
 * benefit, but it results in module export naming choices that are easier
 * to reason about for developers and may aid in debugging.
 */
function reserveBundleModuleExportNames(analyzer, manifest) {
    return __awaiter(this, void 0, void 0, function* () {
        const es6ModuleBundles = [...manifest.bundles]
            .map(([url, bundle]) => ({ url, bundle }))
            .filter(({ bundle }) => bundle.type === 'es6-module');
        const analysis = yield analyzer.analyze(es6ModuleBundles.map(({ url }) => url));
        for (const { url, bundle } of es6ModuleBundles) {
            if (bundle.files.has(url)) {
                const document = analyzer_utils_1.getAnalysisDocument(analysis, url);
                for (const exportName of getModuleExportNames(document)) {
                    getOrSetBundleModuleExportName({ url, bundle }, url, exportName);
                }
            }
        }
    });
}
exports.reserveBundleModuleExportNames = reserveBundleModuleExportNames;
//# sourceMappingURL=es6-module-utils.js.map