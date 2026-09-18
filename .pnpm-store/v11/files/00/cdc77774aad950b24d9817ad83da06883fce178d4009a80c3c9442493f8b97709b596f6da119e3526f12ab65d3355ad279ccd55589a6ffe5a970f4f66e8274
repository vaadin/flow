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

import {BrowserCapability} from 'browser-capabilities';
import {JsCompileTarget} from 'polymer-project-config';

export function getCompileTarget(
    capabilities: Set<BrowserCapability>, compile?: 'always'|'never'|'auto') {
  let compileTarget: JsCompileTarget = undefined;
  if (compile === 'always') {
    compileTarget = 'es5';
  } else if (compile === 'auto') {
    const jsLevels: Array<BrowserCapability&JsCompileTarget> =
        ['es2018', 'es2017', 'es2016', 'es2015'];
    compileTarget = jsLevels.find((c) => capabilities.has(c));
    if (compileTarget === undefined) {
      compileTarget = 'es5';
    }
  }
  return compileTarget;
}
