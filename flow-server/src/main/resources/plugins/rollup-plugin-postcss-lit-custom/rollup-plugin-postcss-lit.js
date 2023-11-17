/**
 * MIT License

Copyright (c) 2019 Umberto Pepato

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
// This is https://github.com/umbopepato/rollup-plugin-postcss-lit 2.0.0 + https://github.com/umbopepato/rollup-plugin-postcss-lit/pull/54
// to make it work with Vite 3
// Once / if https://github.com/umbopepato/rollup-plugin-postcss-lit/pull/54 is merged this should be removed and rollup-plugin-postcss-lit should be used instead

import { createFilter } from '@rollup/pluginutils';
import transformAst from 'transform-ast';

const assetUrlRE = /__VITE_ASSET__([\w$]+)__(?:\$_(.*?)__)?/g

const escape = (str) =>
  str
    .replace(assetUrlRE, '${unsafeCSSTag("__VITE_ASSET__$1__$2")}')
    .replace(/`/g, '\\`')
    .replace(/\\(?!`)/g, '\\\\');

export default function postcssLit(options = {}) {
  const defaultOptions = {
    include: '**/*.{css,sss,pcss,styl,stylus,sass,scss,less}',
    exclude: null,
    importPackage: 'lit'
  };

  const opts = { ...defaultOptions, ...options };
  const filter = createFilter(opts.include, opts.exclude);

  return {
    name: 'postcss-lit',
    enforce: 'post',
    transform(code, id) {
      if (!filter(id)) return;
      const ast = this.parse(code, {});
      // export default const css;
      let defaultExportName;

      // export default '...';
      let isDeclarationLiteral = false;
      const magicString = transformAst(code, { ast: ast }, (node) => {
        if (node.type === 'ExportDefaultDeclaration') {
          defaultExportName = node.declaration.name;

          isDeclarationLiteral = node.declaration.type === 'Literal';
        }
      });

      if (!defaultExportName && !isDeclarationLiteral) {
        return;
      }
      magicString.walk((node) => {
        if (defaultExportName && node.type === 'VariableDeclaration') {
          const exportedVar = node.declarations.find((d) => d.id.name === defaultExportName);
          if (exportedVar) {
            exportedVar.init.edit.update(`cssTag\`${escape(exportedVar.init.value)}\``);
          }
        }

        if (isDeclarationLiteral && node.type === 'ExportDefaultDeclaration') {
          node.declaration.edit.update(`cssTag\`${escape(node.declaration.value)}\``);
        }
      });
      magicString.prepend(`import {css as cssTag, unsafeCSS as unsafeCSSTag} from '${opts.importPackage}';\n`);
      return {
        code: magicString.toString(),
        map: magicString.generateMap({
          hires: true
        })
      };
    }
  };
};
