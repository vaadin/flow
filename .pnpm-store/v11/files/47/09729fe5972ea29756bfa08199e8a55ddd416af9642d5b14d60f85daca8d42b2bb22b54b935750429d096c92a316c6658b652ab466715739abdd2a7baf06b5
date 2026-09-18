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
const stripIndent = require("strip-indent");
const chai_1 = require("chai");
const babel_plugin_import_meta_1 = require("../babel-plugin-import-meta");
suite('babel-plugin-import-meta', () => {
    test('transforms import.meta', () => {
        const input = stripIndent(`
      console.log(import.meta);
      console.log(import.meta.url);
    `);
        const expected = stripIndent(`
      import * as meta from 'meta';
      console.log(meta);
      console.log(meta.url);
    `);
        const result = babelCore.transform(input, { plugins: [babel_plugin_import_meta_1.rewriteImportMeta] }).code;
        chai_1.assert.equal(result.trim(), expected.trim());
    });
    test('does not transform non-meta property', () => {
        const input = stripIndent(`
      console.log(foo.import.meta);
    `);
        const expected = stripIndent(`
      console.log(foo.import.meta);
    `);
        const result = babelCore.transform(input, { plugins: [babel_plugin_import_meta_1.rewriteImportMeta] }).code;
        chai_1.assert.equal(result.trim(), expected.trim());
    });
    test('picks a unique import name', () => {
        const input = stripIndent(`
      const meta = 'foo';
      console.log(import.meta);
    `);
        const expected = stripIndent(`
      import * as _meta from 'meta';
      const meta = 'foo';
      console.log(_meta);
    `);
        const result = babelCore.transform(input, { plugins: [babel_plugin_import_meta_1.rewriteImportMeta] }).code;
        chai_1.assert.equal(result.trim(), expected.trim());
    });
    test('picks a unique import name x2', () => {
        const input = stripIndent(`
      const meta = 'foo';
      const _meta = 'bar';
      console.log(import.meta);
    `);
        const expected = stripIndent(`
      import * as _meta2 from 'meta';
      const meta = 'foo';
      const _meta = 'bar';
      console.log(_meta2);
    `);
        const result = babelCore.transform(input, { plugins: [babel_plugin_import_meta_1.rewriteImportMeta] }).code;
        chai_1.assert.equal(result.trim(), expected.trim());
    });
});
//# sourceMappingURL=babel-plugin-import-meta_test.js.map