"use strict";
/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved. This
 * code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be
 * found at http://polymer.github.io/AUTHORS.txt The complete set of
 * contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt Code
 * distributed by Google as part of the polymer project is also subject to an
 * additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
const ts = require("typescript");
const compilerOptions = {
    target: ts.ScriptTarget.ES2015,
    module: ts.ModuleKind.ES2015,
    moduleResolution: ts.ModuleResolutionKind.NodeJs,
    strict: true,
    noUnusedLocals: true,
    noUnusedParameters: true,
    declaration: true,
    lib: [
        'lib.dom.d.ts',
        'lib.esnext.d.ts',
    ],
};
const diagnosticsHost = {
    getNewLine: () => '\n',
    getCurrentDirectory: () => process.cwd(),
    getCanonicalFileName: (fileName) => fileName,
};
/**
 * Compile the given declaration file paths with TypeScript and return whether
 * compilation succeeded or failed, and a "pretty" formatted error log string.
 *
 * Uses a TypeScript compiler configuration suitable for web development, and
 * strict type checking.
 */
function verifyTypings(filePaths) {
    const program = ts.createProgram(filePaths, compilerOptions);
    const emitResult = program.emit();
    const diagnostics = ts.getPreEmitDiagnostics(program).concat(emitResult.diagnostics);
    if (diagnostics.length > 0) {
        return {
            success: false,
            errorLog: ts.formatDiagnosticsWithColorAndContext(diagnostics, diagnosticsHost)
        };
    }
    return { success: true, errorLog: '' };
}
exports.verifyTypings = verifyTypings;
//# sourceMappingURL=verify.js.map