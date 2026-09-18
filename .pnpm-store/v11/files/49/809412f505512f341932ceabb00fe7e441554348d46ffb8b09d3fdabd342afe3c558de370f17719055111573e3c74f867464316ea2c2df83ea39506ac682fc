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
const url_1 = require("url");
const url_resolver_1 = require("./url-resolver");
/**
 * Resolves a URL having one prefix to another URL with a different prefix.
 */
class RedirectResolver extends url_resolver_1.UrlResolver {
    constructor(packageUrl, _redirectFrom, _redirectTo) {
        super();
        this.packageUrl = packageUrl;
        this._redirectFrom = _redirectFrom;
        this._redirectTo = _redirectTo;
    }
    resolve(firstUrl, secondUrl, _import) {
        const [baseUrl = this.packageUrl, unresolvedUrl] = this.getBaseAndUnresolved(firstUrl, secondUrl);
        const packageRelativeUrl = this.brandAsResolved(url_1.resolve(baseUrl, unresolvedUrl));
        if (packageRelativeUrl.startsWith(this._redirectFrom)) {
            return this.brandAsResolved(this._redirectTo +
                packageRelativeUrl.slice(this._redirectFrom.length));
        }
        if (packageRelativeUrl.startsWith(this._redirectTo)) {
            return packageRelativeUrl;
        }
        return undefined;
    }
    relative(fromOrTo, maybeTo, _kind) {
        let from, to;
        if (maybeTo === undefined) {
            from = this.packageUrl;
            to = fromOrTo;
        }
        else {
            from = fromOrTo;
            to = maybeTo;
        }
        if (!from.startsWith(this._redirectTo) && to.startsWith(this._redirectTo)) {
            to = this.brandAsResolved(this._redirectFrom + to.slice(this._redirectTo.length));
        }
        const result = this.simpleUrlRelative(from, to);
        if (maybeTo === undefined) {
            return this.brandAsPackageRelative(result);
        }
        else {
            return result;
        }
    }
}
exports.RedirectResolver = RedirectResolver;
//# sourceMappingURL=redirect-resolver.js.map