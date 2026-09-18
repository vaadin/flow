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
import { NodePath } from '@babel/traverse';
import { Program } from '@babel/types';
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
export declare const rewriteImportMeta: {
    inherits: any;
    visitor: {
        Program(path: NodePath<Program>): void;
    };
};
