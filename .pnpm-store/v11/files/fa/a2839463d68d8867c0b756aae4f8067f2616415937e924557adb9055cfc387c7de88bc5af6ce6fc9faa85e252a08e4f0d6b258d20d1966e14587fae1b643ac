"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const babelCore = require("@babel/core");
const babylon = require("babylon");
const uuid = require("uuid/v1");
const babel_plugin_bare_specifiers_1 = require("./babel-plugin-bare-specifiers");
const babel_plugin_dynamic_import_amd_1 = require("./babel-plugin-dynamic-import-amd");
const babel_plugin_import_meta_1 = require("./babel-plugin-import-meta");
const externalJs = require("./external-js");
// TODO(aomarks) Switch to babel-preset-env. But how do we get just syntax
// plugins without turning on transformation, for the case where we are
// minifying but not compiling?
// Syntax and transform plugins for ES2015. This is roughly equivalent to
// @babel/preset-es2015, with modules removed and
// @babel/plugin-transform-classes pinned to v7.0.0-beta.35 to avoid
// https://github.com/babel/babel/issues/7506 . As mentioned in the bug, native
// constructors are wrapped with a ES5 'class' which has a constructor that does
// nothing; however, the custom elements polyfill needs the polyfilled
// constructor to be called so that it can supply the element being upgraded as
// the object to use for `this`.
const babelTransformEs2015 = [
    require('@babel/plugin-transform-template-literals'),
    require('@babel/plugin-transform-literals'),
    require('@babel/plugin-transform-function-name'),
    require('@babel/plugin-transform-arrow-functions'),
    require('@babel/plugin-transform-block-scoped-functions'),
    require('@babel/plugin-transform-classes'),
    require('@babel/plugin-transform-object-super'),
    require('@babel/plugin-transform-shorthand-properties'),
    require('@babel/plugin-transform-duplicate-keys'),
    require('@babel/plugin-transform-computed-properties'),
    require('@babel/plugin-transform-for-of'),
    require('@babel/plugin-transform-sticky-regex'),
    require('@babel/plugin-transform-unicode-regex'),
    require('@babel/plugin-transform-spread'),
    require('@babel/plugin-transform-parameters'),
    require('@babel/plugin-transform-destructuring'),
    require('@babel/plugin-transform-block-scoping'),
    require('@babel/plugin-transform-typeof-symbol'),
    require('@babel/plugin-transform-instanceof'),
    [
        require('@babel/plugin-transform-regenerator'),
        { async: false, asyncGenerators: false }
    ],
];
const babelTransformEs2016 = [
    require('@babel/plugin-transform-exponentiation-operator'),
];
const babelTransformEs2017 = [
    require('@babel/plugin-transform-async-to-generator'),
];
const babelTransformEs2018 = [
    require('@babel/plugin-proposal-object-rest-spread'),
    require('@babel/plugin-proposal-async-generator-functions'),
];
// Loading this plugin removes inlined Babel helpers.
const babelExternalHelpersPlugin = require('@babel/plugin-external-helpers');
const babelTransformModulesAmd = [
    babel_plugin_dynamic_import_amd_1.dynamicImportAmd,
    babel_plugin_import_meta_1.rewriteImportMeta,
    require('@babel/plugin-transform-modules-amd'),
];
// We enumerate syntax plugins that would automatically be loaded by our
// transform plugins because we need to support the configuration where we
// minify but don't compile, and don't want Babel to error when it encounters
// syntax that we support when compiling.
const babelSyntaxPlugins = [
    // ES2017 and below syntax plugins are included by default.
    // ES2018 (partial)
    require('@babel/plugin-syntax-object-rest-spread'),
    require('@babel/plugin-syntax-async-generators'),
    // Future
    // require('@babel/plugin-syntax-export-extensions'),
    require('@babel/plugin-syntax-dynamic-import'),
    require('@babel/plugin-syntax-import-meta'),
];
const babelPresetMinify = require('babel-preset-minify')({}, {
    // Disable this or you get `{ err: 'Couldn\'t find intersection' }` now.
    // https://github.com/babel/minify/issues/904
    builtIns: false,
    // Disable the minify-constant-folding plugin because it has a bug relating
    // to invalid substitution of constant values into export specifiers:
    // https://github.com/babel/minify/issues/820
    evaluate: false,
    // TODO(aomarks) Find out why we disabled this plugin.
    simplifyComparisons: false,
    // Prevent removal of things that babel thinks are unreachable, but sometimes
    // gets wrong: https://github.com/Polymer/tools/issues/724
    deadcode: false,
    // Prevents this `isPure` on null problem from blowing up minification.
    // https://github.com/babel/minify/issues/790
    removeUndefined: false,
    // Disable the simplify plugin because it can eat some statements preceeding
    // loops. https://github.com/babel/minify/issues/824
    simplify: false,
    // This is breaking ES6 output. https://github.com/Polymer/tools/issues/261
    mangle: false,
});
/**
 * Transform some JavaScript according to the given options.
 */
function jsTransform(js, options) {
    // Even with no transform plugins, parsing and serializing with Babel will
    // make some minor formatting changes to the code. Skip Babel altogether
    // if we have no meaningful changes to make.
    let doBabelTransform = false;
    // Note that Babel plugins run in this order:
    // 1) plugins, first to last
    // 2) presets, last to first
    const plugins = [...babelSyntaxPlugins];
    const presets = [];
    if (options.externalHelpers) {
        plugins.push(babelExternalHelpersPlugin);
    }
    if (options.minify) {
        doBabelTransform = true;
        // Minify last, so push first.
        presets.push(babelPresetMinify);
    }
    if (options.compile === true || options.compile === 'es5') {
        doBabelTransform = true;
        plugins.push(...babelTransformEs2015);
        plugins.push(...babelTransformEs2016);
        plugins.push(...babelTransformEs2017);
        plugins.push(...babelTransformEs2018);
    }
    else if (options.compile === 'es2015') {
        doBabelTransform = true;
        plugins.push(...babelTransformEs2016);
        plugins.push(...babelTransformEs2017);
        plugins.push(...babelTransformEs2018);
    }
    else if (options.compile === 'es2016') {
        doBabelTransform = true;
        plugins.push(...babelTransformEs2017);
        plugins.push(...babelTransformEs2018);
    }
    else if (options.compile === 'es2017') {
        doBabelTransform = true;
        plugins.push(...babelTransformEs2018);
    }
    if (options.moduleResolution === 'node') {
        if (!options.filePath) {
            throw new Error('Cannot perform node module resolution without filePath.');
        }
        doBabelTransform = true;
        plugins.push(babel_plugin_bare_specifiers_1.resolveBareSpecifiers(options.filePath, !!options.isComponentRequest, options.packageName, options.componentDir, options.rootDir));
    }
    // When the AMD option is "auto", these options will change based on whether
    // we have a module or not (unless they are already definitely true).
    let transformModulesToAmd = options.transformModulesToAmd;
    if (transformModulesToAmd === true) {
        doBabelTransform = true;
    }
    const maybeDoBabelTransform = doBabelTransform || transformModulesToAmd === 'auto';
    if (maybeDoBabelTransform) {
        let ast;
        try {
            ast = babylon.parse(js, {
                // TODO(aomarks) Remove any when typings are updated for babylon 7.
                // tslint:disable-next-line: no-any
                sourceType: transformModulesToAmd === 'auto' ? 'unambiguous' :
                    'module',
                plugins: [
                    'asyncGenerators',
                    'dynamicImport',
                    // tslint:disable-next-line: no-any
                    'importMeta',
                    'objectRestSpread',
                ],
            });
        }
        catch (e) {
            if (options.softSyntaxError && e.constructor.name === 'SyntaxError') {
                console.error('ERROR [polymer-build]: failed to parse JavaScript' +
                    (options.filePath ? ` (${options.filePath}):` : ':'), e);
                return js;
            }
            else {
                throw e;
            }
        }
        if (transformModulesToAmd === 'auto' &&
            ast.program.sourceType === 'module') {
            transformModulesToAmd = true;
        }
        if (transformModulesToAmd) {
            doBabelTransform = true;
            plugins.push(...babelTransformModulesAmd);
        }
        if (doBabelTransform) {
            const result = babelCore.transformFromAst(ast, js, { presets, plugins });
            if (result.code === undefined) {
                throw new Error('Babel transform failed: resulting code was undefined.');
            }
            js = result.code;
            if (!options.externalHelpers && options.compile === 'es5' &&
                js.includes('regeneratorRuntime')) {
                js = externalJs.getRegeneratorRuntime() + js;
            }
        }
    }
    js = replaceTemplateObjectNames(js);
    return js;
}
exports.jsTransform = jsTransform;
/**
 * Modifies variables names of tagged template literals (`"_templateObject"`)
 * from a given string so that they're all unique.
 *
 * This is needed to workaround a potential naming collision when individually
 * transpiled scripts are bundled. See #950.
 */
function replaceTemplateObjectNames(js) {
    // Breakdown of regular expression to match "_templateObject" variables
    //
    // Pattern                | Meaning
    // -------------------------------------------------------------------
    // (                      | Group1
    // _templateObject        | Match "_templateObject"
    // \d*                    | Match 0 or more digits
    // \b                     | Match word boundary
    // )                      | End Group1
    const searchValueRegex = /(_templateObject\d*\b)/g;
    // The replacement pattern appends an underscore and UUID to the matches:
    //
    // Pattern                | Meaning
    // -------------------------------------------------------------------
    // $1                     | Insert matching Group1 (from above)
    // _                      | Insert "_"
    // ${uniqueId}            | Insert previously generated UUID
    const uniqueId = uuid().replace(/-/g, '');
    const replaceValue = `$1_${uniqueId}`;
    // Example output:
    // _templateObject  -> _templateObject_200817b1154811e887be8b38cea68555
    // _templateObject2 -> _templateObject2_5e44de8015d111e89b203116b5c54903
    return js.replace(searchValueRegex, replaceValue);
}
//# sourceMappingURL=js-transform.js.map