/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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

import * as url from 'url';

import {ServerOptions} from '../start_server';

import opn = require('opn');

/**
 * Open the given web page URL. If no browser keyword is provided, `opn` will
 * use the user's default browser.
 */
function openWebPage(url: string, withBrowser?: string) {
  const openOptions = {app: withBrowser};
  opn(url, openOptions, (err) => {
    if (err) {
      // log error and continue
      console.error(
          `ERROR: Problem launching ` +
          `"${openOptions.app || 'default web browser'}".`);
    }
  });
}

/**
 * Opens one or more browsers based on the given options and url params.
 *
 * @param options
 * @param serverUrl
 * @param componentUrl
 */
export function openBrowser(
    options: ServerOptions, serverUrl: Object, componentUrl: url.Url) {
  if (options.open) {
    let openUrl: url.Url;
    if (options.openPath) {
      openUrl = Object.assign({}, serverUrl);
      openUrl.pathname = options.openPath;
    } else {
      openUrl = Object.assign({}, componentUrl);
    }
    if (!Array.isArray(options.browser)) {
      openWebPage(url.format(openUrl));
    } else {
      options.browser.forEach((browser) => {
        openWebPage(url.format(openUrl), browser);
      });
    }
  }
}
