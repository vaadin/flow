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

import * as fs from 'fs';
import * as pathlib from 'path';

let babelHelpersFull: string;
export function getBabelHelpersFull() {
  if (babelHelpersFull === undefined) {
    babelHelpersFull = fs.readFileSync(
        pathlib.join(__dirname, 'babel-helpers-full.min.js'), 'utf-8');
  }
  return babelHelpersFull;
}

let babelHelpersAmd: string;
export function getBabelHelpersAmd() {
  if (babelHelpersAmd === undefined) {
    babelHelpersAmd = fs.readFileSync(
        pathlib.join(__dirname, 'babel-helpers-amd.min.js'), 'utf-8');
  }
  return babelHelpersAmd;
}

let regeneratorRuntime: string;
export function getRegeneratorRuntime() {
  if (regeneratorRuntime === undefined) {
    regeneratorRuntime = fs.readFileSync(
        pathlib.join(__dirname, 'regenerator-runtime.min.js'), 'utf-8');
  }
  return regeneratorRuntime;
}

let amdLoader: string;
export function getAmdLoader() {
  if (amdLoader === undefined) {
    amdLoader =
        fs.readFileSync(require.resolve('@polymer/esm-amd-loader'), 'utf-8');
  }
  return amdLoader;
}
