import { createFilter } from '@rollup/pluginutils';
import transformAst from 'transform-ast';
import MagicString from 'magic-string';

import process from 'node:process';

import * as path from 'node:path';
import * as fsPromises from 'node:fs/promises';

const chunkNameMarker = '__VAADIN_I18n_chunkName__';
const registerChunkImport = `import { i18n } from '@vaadin/hilla-react-i18n';\n`;
const registerChunkCall = `await i18n.registerChunk('${chunkNameMarker}');\n`;


/**
 * Vaadin Rollup/Vite plugin for automatic splitting of i18n bundles for Hilla
 * apps based on the chunks in the JS bundle output.
 *
 * @param {Object} options - the plugin options
 * @param {string=} options.include - the input modules include pattern
 * @param {string=} options.exclude - the input modules exclude pattern
 * @param {string=} options.cwd - the base directory for resolving directory options
 * @param {Object=} options.meta - bundle JSON metadata options
 * @param {Object=} options.meta.output - bundle JSON metadata output options
 * @param {Object=} options.meta.output.dir - bundle JSON metadata output directory
 * @param {Object=} options.meta.output.filename - bundle JSON metadata file name, defaults to 'i18n.json'
 *
 * @returns the Rollup/Vite plugin instance
 */
export default function vaadinI18n(options = {}) {
  const defaultOptions = {
    include: '**/*.{js,jsx,ts,tsx}',
    exclude: null,
    cwd: process.cwd(),
    meta: {
      output: {
        dir: '',
        filename: 'i18n.json'
      },
    },
  };

  const filter = createFilter(options?.include ?? defaultOptions.include, options?.exclude ?? defaultOptions.exclude);

  const cwd = options?.cwd ?? defaultOptions.cwd;
  const metaOutputDir = path.resolve(cwd, options?.meta?.output?.dir ?? defaultOptions.meta.output.dir);
  const metaOutputFilename = path.resolve(metaOutputDir, options?.meta?.output?.filename ?? defaultOptions.meta.output.filename);

  const chunkKeySets = new Map();
  const mainChunkKeySet = new Set();

  return {
    name: 'vaadin-i18n',
    async transform(code, id) {
      if (!filter(id)) {
        return;
      }
      const keyTagBindings = new Set();
      const ast = this.parse(code, {});
      const moduleKeySet = new Set();
      let hasI18nImport = false;
      const magicString = transformAst(code, { ast }, (node) => {
        if (node.type === 'ImportDeclaration' && node.source?.value === '@vaadin/hilla-react-i18n') {
          for (const spec of node?.specifiers) {
            if (spec.imported?.name === 'key') {
              keyTagBindings.add(spec.local?.name);
            }
            if (spec.type === 'ImportSpecifier' && spec.imported?.name === 'i18n') {
              hasI18nImport = true;
            }
          }
        }
        if (
          node.type === 'TaggedTemplateExpression' &&
          node.tag.type === 'Identifier' &&
          keyTagBindings.has(node.tag.name) &&
          node.quasi &&
          node.quasi.type === 'TemplateLiteral' &&
          node.quasi.quasis.length === 1 &&
          typeof node.quasi.quasis[0].value.raw === 'string'
        ) {
          if (
            node.parent &&
            node.parent.type === 'CallExpression' &&
            node.parent.callee.type === 'Identifier' &&
            node.parent.callee.name === 'translate'
          ) {
            // translate(key`something`) goes to the module chunk
            moduleKeySet.add(node.quasi.quasis[0].value.raw);
          } else {
            // key`something` goes to the main chunk (assume ViewConfig)
            mainChunkKeySet.add(node.quasi.quasis[0].value.raw);
          }
        }
      });

      if (moduleKeySet.size === 0) {
        // No Hilla i18n keys detected: return the original module
        // return { code, ast, map: null };
        return {code, map: null};
      }

      // Prepend the automatic registerChunk call
      if (!hasI18nImport) {
        magicString.prepend(registerChunkImport);
      }
      magicString.prepend(registerChunkCall);

      return {
        code: magicString.toString(),
        map: magicString.generateMap({hires: true}),
        meta: {
          vaadinI18n: {
            keys: Array.from(moduleKeySet),
          },
        },
      };
    },
    renderStart() {
      chunkKeySets.clear();
    },
    async generateBundle(_options, bundle) {
      for (const [fileName, chunk] of Object.entries(bundle)) {
        if (chunk.type === 'chunk') {
          const magicString = new MagicString(chunk.code);
          // Extra imports are removed automatically from the final chunk, but
          // there might be still multiple registerChunk calls originating from
          // the modules using Hilla i18n. One such call per chunk is enough
          // to load all the translations for the code below it, so let us
          // remove the duplicate calls.
          let idx = 0;
          let firstIdx = -1;
          let searchIdx = 0;
          while ((idx = magicString.toString().indexOf(registerChunkCall, searchIdx)) !== -1) {
            if (firstIdx === -1) {
              firstIdx = idx;
              searchIdx = idx + registerChunkCall.length;
            } else {
              // Remove this occurrence
              magicString.remove(idx, idx + registerChunkCall.length);
              searchIdx = idx; // Don't advance, as string just got shorter
            }
          }

          // Replace the chunk name markers with the actual chunk name
          magicString.replace(chunkNameMarker, fileName);
          chunk.code = magicString.toString();
          chunk.map = magicString.generateMap({ hires: true });

          // Collect i18n translation keys from all modules of the chunk
          const chunkKeySet = new Set();
          for (const id of chunk.moduleIds) {
            if (!filter(id)) {
              continue;
            }
            const info = this.getModuleInfo(id);
            (info?.meta?.vaadinI18n?.keys ?? []).forEach((key) => chunkKeySet.add(key));
          }
          // Write a chunk metadata JSON file with the keys
          chunkKeySets.set(fileName, chunkKeySet);
        }
      }
      // Serialize chunk key sets
      const chunks = {};
      for (const [name, chunkKeySet] of chunkKeySets.entries()) {
        if (name.includes('indexhtml')) {
          for (const key of mainChunkKeySet) {
            chunkKeySet.add(key);
          }
          mainChunkKeySet.clear();
        }

        const keys = Array.from(chunkKeySet);

        if (keys.length > 0) {
          chunks[name] = {keys};
        }
      }
      // Write i18n metadata JSON file:
      // {
      //   "chunks": {
      //     [name]: {
      //       keys: [...]
      //     }
      //   }
      // }
      await fsPromises.mkdir(metaOutputDir, {recursive: true});
      await fsPromises.writeFile(path.resolve(metaOutputDir, metaOutputFilename), JSON.stringify({chunks}, undefined, 2), {encoding: 'utf-8'});
    },
  };
};
