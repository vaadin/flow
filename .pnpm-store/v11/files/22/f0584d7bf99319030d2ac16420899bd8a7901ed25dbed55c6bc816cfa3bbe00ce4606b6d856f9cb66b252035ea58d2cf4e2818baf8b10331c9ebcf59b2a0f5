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
const template_1 = require("@babel/template");
const ast = template_1.default.ast;
/**
 * Rewrites dynamic import() calls to AMD async require() calls.
 */
exports.dynamicImportAmd = {
    inherits: plugin_syntax_dynamic_import_1.default,
    visitor: {
        Program(path) {
            // We transform dynamic import() into a Promise whose initializer calls
            // require(). We must use the "local" require - the one provided by the
            // AMD loaded when a module explicitly depends on the "require" module ID.
            // To get the emitted define() call to depend on "require", we inject an
            // import of a module called "require".
            // Collect all the NodePaths to dynamic import() expressions
            // and all the identifiers in scope for them.
            const identifiers = new Set();
            const dynamicImports = [];
            path.traverse({
                CallExpression(path) {
                    if (path.node.callee.type === 'Import') {
                        dynamicImports.push(path);
                        const bindings = path.scope.getAllBindings();
                        for (const name of Object.keys(bindings)) {
                            identifiers.add(name);
                        }
                    }
                }
            });
            if (dynamicImports.length === 0) {
                return;
            }
            // Choose a unique name to import "require" as.
            let requireId = undefined;
            do {
                requireId = path.scope.generateUidIdentifier('require');
            } while (identifiers.has(requireId.name));
            // Inject the import of "require"
            const statements = path.node.body;
            statements.unshift(ast `import * as ${requireId} from 'require';`);
            // Transform the dynamic import callsites
            for (const importPath of dynamicImports) {
                const specifier = importPath.node.arguments[0];
                // Call as `require.default` because the AMD transformer that we assume
                // is running next will rewrite `require` from a function to a module
                // object with the function at `default`.
                importPath.replaceWith(ast `(
          new Promise((res, rej) => ${requireId}.default([${specifier}], res, rej))
        )`);
            }
        },
    },
};
//# sourceMappingURL=babel-plugin-dynamic-import-amd.js.map