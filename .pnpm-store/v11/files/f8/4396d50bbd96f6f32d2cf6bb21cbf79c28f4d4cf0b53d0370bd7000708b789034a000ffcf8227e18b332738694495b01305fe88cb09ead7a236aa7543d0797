"use strict";
/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
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
const path = require("path");
const url_1 = require("url");
const utils_1 = require("../core/utils");
/**
 * Resolves the given URL to the concrete URL that a resource can
 * be loaded from.
 *
 * This can be useful to resolve name to paths, such as resolving 'polymer' to
 * '../polymer/polymer.html', or component paths, like '../polymer/polymer.html'
 * to '/bower_components/polymer/polymer.html'.
 */
class UrlResolver {
    getBaseAndUnresolved(url1, url2) {
        return url2 === undefined ?
            [undefined, this.brandAsPackageRelative(url1)] :
            [this.brandAsResolved(url1), this.brandAsFileRelative(url2)];
    }
    simpleUrlResolve(baseUrl, url, defaultProtocol) {
        return this.brandAsResolved(utils_1.resolveUrl(baseUrl, url, defaultProtocol));
    }
    simpleUrlRelative(from, to) {
        const fromUrl = utils_1.parseUrl(from);
        const toUrl = utils_1.parseUrl(to);
        // Return the `to` as-is if there are conflicting components which
        // prohibit calculating a relative form.
        if (typeof toUrl.protocol === 'string' &&
            fromUrl.protocol !== toUrl.protocol ||
            typeof toUrl.slashes === 'boolean' &&
                fromUrl.slashes !== toUrl.slashes ||
            typeof toUrl.host === 'string' && fromUrl.host !== toUrl.host ||
            typeof toUrl.auth === 'string' && fromUrl.auth !== toUrl.auth) {
            return this.brandAsFileRelative(to);
        }
        let pathname;
        const { search, hash } = toUrl;
        if (fromUrl.pathname === toUrl.pathname) {
            pathname = '';
        }
        else {
            const fromDir = typeof fromUrl.pathname === 'string' ?
                fromUrl.pathname.replace(/[^/]+$/, '') :
                '';
            const toDir = typeof toUrl.pathname === 'string' &&
                typeof toUrl.pathname === 'string' ?
                toUrl.pathname :
                '';
            // In a browserify environment, there isn't path.posix.
            const pathlib = path.posix || path;
            // Note, below, the _ character is appended to the `toDir` so that paths
            // with trailing slash will retain the trailing slash in the result.
            pathname = pathlib.relative(fromDir, toDir + '_').replace(/_$/, '');
        }
        return this.brandAsFileRelative(url_1.format({ pathname, search, hash }));
    }
    brandAsFileRelative(url) {
        return url;
    }
    brandAsPackageRelative(url) {
        return url;
    }
    brandAsResolved(url) {
        return url;
    }
}
exports.UrlResolver = UrlResolver;
//# sourceMappingURL=url-resolver.js.map