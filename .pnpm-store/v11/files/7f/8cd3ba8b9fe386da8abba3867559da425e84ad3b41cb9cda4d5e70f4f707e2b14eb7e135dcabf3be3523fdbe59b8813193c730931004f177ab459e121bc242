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
/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
const babel_generator_1 = require("babel-generator");
const babel = require("babel-types");
const analyzer_utils_1 = require("./analyzer-utils");
const es6_module_utils_1 = require("./es6-module-utils");
const es6_rewriter_1 = require("./es6-rewriter");
/**
 * Produces an ES6 Module BundledDocument.
 */
function bundle(bundler, manifest, url) {
    return __awaiter(this, void 0, void 0, function* () {
        const bundle = manifest.bundles.get(url);
        if (!bundle) {
            throw new Error(`No bundle found in manifest for url ${url}.`);
        }
        const assignedBundle = { url, bundle };
        const generatedCode = yield prepareBundleModule(bundler, manifest, assignedBundle);
        const es6Rewriter = new es6_rewriter_1.Es6Rewriter(bundler, manifest, assignedBundle);
        const { code: rolledUpCode } = yield es6Rewriter.rollup(url, generatedCode);
        const document = analyzer_utils_1.assertIsJsDocument(yield bundler.analyzeContents(assignedBundle.url, rolledUpCode));
        return {
            language: 'js',
            ast: document.parsedDocument.ast,
            content: document.parsedDocument.contents,
            files: [...assignedBundle.bundle.files]
        };
    });
}
exports.bundle = bundle;
/**
 * Generate code containing import statements to all bundled modules and
 * export statements to re-export their namespaces and exports.
 *
 * Example: a bundle containing files `module-a.js` and `module-b.js` would
 * result in a prepareBundleModule result like:
 *
 *     import * as $moduleA from './module-a.js';
 *     import * as $moduleB from './module-b.js';
 *     import $moduleBDefault from './module-b.js';
 *     export {thing1, thing2} from './module-a.js';
 *     export {thing3} from './module-b.js';
 *     export {$moduleA, $moduleB, $moduleBDefault};
 */
function prepareBundleModule(bundler, manifest, assignedBundle) {
    return __awaiter(this, void 0, void 0, function* () {
        const bundleSource = babel.program([]);
        const sourceAnalysis = yield bundler.analyzer.analyze([...assignedBundle.bundle.files]);
        for (const resolvedSourceUrl of [...assignedBundle.bundle.files].sort()) {
            const moduleDocument = analyzer_utils_1.getAnalysisDocument(sourceAnalysis, resolvedSourceUrl);
            const moduleExports = es6_module_utils_1.getModuleExportNames(moduleDocument);
            const starExportName = es6_module_utils_1.getOrSetBundleModuleExportName(assignedBundle, resolvedSourceUrl, '*');
            bundleSource.body.push(babel.importDeclaration([babel.importNamespaceSpecifier(babel.identifier(starExportName))], babel.stringLiteral(resolvedSourceUrl)));
            if (moduleExports.size > 0) {
                bundleSource.body.push(babel.exportNamedDeclaration(undefined, [babel.exportSpecifier(babel.identifier(starExportName), babel.identifier(starExportName))]));
                bundleSource.body.push(babel.exportNamedDeclaration(undefined, [...moduleExports].map((e) => babel.exportSpecifier(babel.identifier(e), babel.identifier(es6_module_utils_1.getOrSetBundleModuleExportName(assignedBundle, resolvedSourceUrl, e)))), babel.stringLiteral(resolvedSourceUrl)));
            }
        }
        const { code } = babel_generator_1.default(bundleSource);
        return code;
    });
}
//# sourceMappingURL=es6-module-bundler.js.map