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
const url_resolver_1 = require("./url-resolver");
/**
 * Resolves a URL using multiple resolvers.
 */
class MultiUrlResolver extends url_resolver_1.UrlResolver {
    constructor(_resolvers) {
        super();
        this._resolvers = _resolvers;
    }
    /**
     * Returns the first resolved URL (which is not undefined.)
     */
    resolve(firstUrl, secondUrl, import_) {
        for (const resolver of this._resolvers) {
            const resolved = secondUrl === undefined ?
                resolver.resolve(firstUrl) :
                resolver.resolve(firstUrl, secondUrl, import_);
            if (resolved !== undefined) {
                return resolved;
            }
        }
        return undefined;
    }
    relative(fromOrTo, maybeTo, kind) {
        const [from, to] = (maybeTo === undefined) ? [undefined, fromOrTo] : [fromOrTo, maybeTo];
        for (const resolver of this._resolvers) {
            if (resolver.resolve(this.brandAsPackageRelative(to)) === undefined) {
                continue;
            }
            if (from === undefined) {
                return resolver.relative(to);
            }
            else {
                return resolver.relative(from, to, kind);
            }
        }
        throw new Error(`Could not get relative url, with no configured url resolvers`);
    }
}
exports.MultiUrlResolver = MultiUrlResolver;
//# sourceMappingURL=multi-url-resolver.js.map