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
const plugin_syntax_dynamic_import_1 = require("@babel/plugin-syntax-dynamic-import");
const resolve_specifier_node_1 = require("polymer-analyzer/lib/javascript/resolve-specifier-node");
const isPathSpecifier = (s) => /^\.{0,2}\//.test(s);
/**
 * Rewrites so-called "bare module specifiers" to be web-compatible paths.
 */
exports.resolveBareSpecifiers = (filePath, isComponentRequest, packageName, componentDir, rootDir) => ({
    inherits: plugin_syntax_dynamic_import_1.default,
    visitor: {
        Program(path) {
            path.traverse({
                CallExpression(path) {
                    if (path.node.callee.type === 'Import') {
                        const specifierArg = path.node.arguments[0];
                        if (specifierArg.type !== 'StringLiteral') {
                            // Should never happen
                            return;
                        }
                        const specifier = specifierArg.value;
                        specifierArg.value = maybeResolve(specifier, filePath, isComponentRequest, packageName, componentDir, rootDir);
                    }
                }
            });
        },
        'ImportDeclaration|ExportNamedDeclaration|ExportAllDeclaration'(path) {
            const node = path.node;
            // An export without a 'from' clause
            if (node.source == null) {
                return;
            }
            const specifier = node.source.value;
            node.source.value = maybeResolve(specifier, filePath, isComponentRequest, packageName, componentDir, rootDir);
        }
    }
});
const maybeResolve = (specifier, filePath, isComponentRequest, packageName, componentDir, rootDir) => {
    try {
        let componentInfo;
        if (isComponentRequest) {
            componentInfo = {
                packageName: packageName,
                componentDir: componentDir,
                rootDir: rootDir,
            };
        }
        return resolve_specifier_node_1.resolve(specifier, filePath, componentInfo);
    }
    catch (e) {
        // `require` and `meta` are fake imports that our other build tooling
        // injects, so we should not warn for them.
        if (!isPathSpecifier(specifier) && specifier !== 'require' &&
            specifier !== 'meta') {
            // Don't warn if the specifier was already a path, even though we do
            // resolve paths, because maybe the user is serving it some other
            // way.
            console.warn(`Could not resolve module specifier "${specifier}" ` +
                `in file "${filePath}".`);
        }
        return specifier;
    }
};
//# sourceMappingURL=babel-plugin-bare-specifiers.js.map