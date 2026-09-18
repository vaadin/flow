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

import importMetaSyntax from '@babel/plugin-syntax-import-meta';
import template from '@babel/template';
import {NodePath} from '@babel/traverse';
import {Program, MetaProperty} from '@babel/types';

const ast = template.ast;

/**
 * Rewrites `import.meta`[1] into an import for a module named "meta". It is
 * expected this plugin runs alongside @babel/plugin-transform-modules-amd which
 * will transform this import into an AMD dependency, and is loaded using
 * @polymer/esm-amd-loader which will provide an object with a `url`[2] property
 * for the "meta" dependency.
 *
 * [1]: https://github.com/tc39/proposal-import-meta
 * [2]: https://html.spec.whatwg.org/#hostgetimportmetaproperties
 */
export const rewriteImportMeta = {
  inherits: importMetaSyntax,

  visitor: {
    Program(path: NodePath<Program>) {
      const metas: NodePath<MetaProperty>[] = [];
      const identifiers = new Set<string>();

      path.traverse({
        MetaProperty(path: NodePath<MetaProperty>) {
          const node = path.node;
          if (node.meta && node.meta.name === 'import' &&
              node.property.name === 'meta') {
            metas.push(path);
            for (const name of Object.keys(path.scope.getAllBindings())) {
              identifiers.add(name);
            }
          }
        }
      });

      if (metas.length === 0) {
        return;
      }

      let metaId = 'meta';
      while (identifiers.has(metaId)) {
        metaId = path.scope.generateUidIdentifier('meta').name;
      }

      path.node.body.unshift(ast`import * as ${metaId} from 'meta';`);
      for (const meta of metas) {
        meta.replaceWith(ast`${metaId}`);
      }
    },
  }
};
