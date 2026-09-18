"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
const url = require("url");
const opn = require("opn");
/**
 * Open the given web page URL. If no browser keyword is provided, `opn` will
 * use the user's default browser.
 */
function openWebPage(url, withBrowser) {
    const openOptions = { app: withBrowser };
    opn(url, openOptions, (err) => {
        if (err) {
            // log error and continue
            console.error(`ERROR: Problem launching ` +
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
function openBrowser(options, serverUrl, componentUrl) {
    if (options.open) {
        let openUrl;
        if (options.openPath) {
            openUrl = Object.assign({}, serverUrl);
            openUrl.pathname = options.openPath;
        }
        else {
            openUrl = Object.assign({}, componentUrl);
        }
        if (!Array.isArray(options.browser)) {
            openWebPage(url.format(openUrl));
        }
        else {
            options.browser.forEach((browser) => {
                openWebPage(url.format(openUrl), browser);
            });
        }
    }
}
exports.openBrowser = openBrowser;
//# sourceMappingURL=open_browser.js.map