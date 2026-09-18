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
const pathlib = require("path");
const path_1 = require("path");
const pathIsInside = require("path-is-inside");
const url_1 = require("url");
const utils_1 = require("../core/utils");
const fs_url_resolver_1 = require("./fs-url-resolver");
/**
 * Resolves a URL to a canonical URL within a package.
 */
class PackageUrlResolver extends fs_url_resolver_1.FsUrlResolver {
    constructor(options = {}) {
        super(options.packageDir, options.host, options.protocol);
        this.componentDir = options.componentDir || 'bower_components/';
        this.resolvedComponentDir =
            pathlib.join(this.packageDir, this.componentDir);
    }
    modifyFsPath(path) {
        // If the path points to a sibling directory, resolve it to the
        // component directory
        const parentOfPackageDir = pathlib.dirname(this.packageDir);
        if (pathIsInside(path, parentOfPackageDir) &&
            !pathIsInside(path, this.packageDir)) {
            path = pathlib.join(this.packageDir, this.componentDir, path.substring(parentOfPackageDir.length));
        }
        return path;
    }
    relative(fromOrTo, maybeTo, _kind) {
        const [from, to] = (maybeTo !== undefined) ? [fromOrTo, maybeTo] :
            [this.packageUrl, fromOrTo];
        const result = this.relativeImpl(from, to);
        if (maybeTo === undefined) {
            return this.brandAsPackageRelative(result);
        }
        else {
            return result;
        }
    }
    relativeImpl(from, to) {
        const pathnameInComponentDir = this.pathnameForComponentDirUrl(to);
        // If the URL we're going to is in the component directory, we need
        // to correct for that when generating a relative URL...
        if (pathnameInComponentDir !== undefined) {
            // ... unless the URL we're coming from is *also* in the component
            // directory
            if (this.pathnameForComponentDirUrl(from) === undefined) {
                // Ok, transform the path from looking like `/path/to/node_modules/foo`
                // to look instead like `/path/foo` so we get a `../` relative url.
                const componentDirPath = pathnameInComponentDir.slice(this.resolvedComponentDir.length);
                const reresolved = this.simpleUrlResolve(this.packageUrl, ('../' + componentDirPath), this.protocol);
                if (reresolved !== undefined) {
                    const reresolvedUrl = utils_1.parseUrl(reresolved);
                    const toUrl = utils_1.parseUrl(to);
                    reresolvedUrl.search = toUrl.search;
                    reresolvedUrl.hash = toUrl.hash;
                    to = this.brandAsResolved(url_1.format(reresolvedUrl));
                }
            }
        }
        return super.relative(from, to);
    }
    /**
     * If the given URL is a file url inside our dependencies (e.g.
     * bower_components) then return a resolved posix path to its file.
     * Otherwise return undefined.
     */
    pathnameForComponentDirUrl(resolvedUrl) {
        const url = utils_1.parseUrl(resolvedUrl);
        if (this.shouldHandleAsFileUrl(url) && url.pathname) {
            let pathname;
            try {
                pathname = path_1.posix.normalize(decodeURIComponent(url.pathname));
            }
            catch (_a) {
                return undefined;
            }
            const path = this.filesystemPathForPathname(pathname);
            if (path && pathIsInside(path, this.resolvedComponentDir)) {
                return path;
            }
        }
        return undefined;
    }
}
exports.PackageUrlResolver = PackageUrlResolver;
//# sourceMappingURL=package-url-resolver.js.map