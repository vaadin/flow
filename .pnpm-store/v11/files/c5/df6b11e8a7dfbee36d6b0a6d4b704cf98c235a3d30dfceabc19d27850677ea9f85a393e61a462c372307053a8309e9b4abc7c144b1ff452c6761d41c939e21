"use strict";
/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
const index_1 = require("../index");
const fs_url_resolver_1 = require("./fs-url-resolver");
/**
 * A URL resolver for very large codebases where source files map in an
 * arbitrary but predetermined fashion onto URL space.
 *
 * It also separates the root directory – the root of all source code that's
 * legal to load – from the package directory, which is how the user refers to
 * files on the CLI or the IDE.
 */
class IndirectUrlResolver extends fs_url_resolver_1.FsUrlResolver {
    /**
     * @param rootPath All loadable source code must be a descendent of this
     *     directory. Should be the same as FsUrlLoader's rootPath.
     * @param packagePath The base directory for package-relative paths. Usually
     *     the current working directory.
     * @param indirectionMap Maps the runtime URL space to the paths for those
     *     files on the filesystem.
     *
     *     The keys must be relative paths, like `paper-button/paper-button.html`.
     *     The filesystem paths must be be relative Fs paths from `rootPath` to
     *     the file on disk that corresponds to the runtime URL.
     */
    constructor(rootPath, packagePath, indirectionMap, protocol = 'https') {
        super(packagePath);
        this.protocol = protocol;
        const rootResolver = new fs_url_resolver_1.FsUrlResolver(rootPath);
        const urlspaceToFilesystem = new Map();
        const filesystemToUrlspace = new Map();
        for (const [u, fsPath] of indirectionMap) {
            const url = u;
            const fsUrl = rootResolver.resolve(this.brandAsPackageRelative(fsPath));
            if (fsUrl === undefined) {
                throw new Error(`Invalid fs path in indirection map: ${fsPath}`);
            }
            urlspaceToFilesystem.set(url, fsUrl);
            filesystemToUrlspace.set(fsUrl, url);
        }
        this.runtimeUrlToResolvedUrl = urlspaceToFilesystem;
        this.resolvedUrlToRuntimeUrl = filesystemToUrlspace;
    }
    resolve(firstHref, secondHref) {
        const [baseUrl, unresolvedHref] = this.getBaseAndUnresolved(firstHref, secondHref);
        // If we're just resolving a package-relative url, do the basic thing.
        if (baseUrl === undefined) {
            return super.resolve(this.brandAsPackageRelative(unresolvedHref));
        }
        const url = this.brandAsFileRelative(unresolvedHref);
        const runtimeBaseUrl = this.resolvedUrlToRuntimeUrl.get(baseUrl);
        if (runtimeBaseUrl === undefined) {
            return super.resolve(baseUrl, url);
        }
        const runtimeUrl = this.runtimeResolve(url, runtimeBaseUrl);
        const resolvedUrl = this.runtimeUrlToResolvedUrl.get(runtimeUrl);
        if (resolvedUrl === undefined) {
            return super.resolve(baseUrl, url);
        }
        return resolvedUrl;
    }
    runtimeResolve(url, runtimeBaseUrl) {
        const resolved = this.simpleUrlResolve(this.brandAsResolved(runtimeBaseUrl), url, this.protocol);
        return brandAsRuntimeUrl(resolved);
    }
    relative(fromOrTo, maybeTo, _kind) {
        const [from, to] = (maybeTo !== undefined) ? [fromOrTo, maybeTo] :
            [this.packageUrl, fromOrTo];
        return this.relativeImpl(from, to);
    }
    relativeImpl(from, to) {
        const fromWeb = this.resolvedUrlToRuntimeUrl.get(from);
        const toWeb = this.resolvedUrlToRuntimeUrl.get(to);
        if (fromWeb === undefined || toWeb === undefined) {
            return this.simpleUrlRelative(from, to);
        }
        return this.simpleUrlRelative(this.brandAsResolved(fromWeb), this.brandAsResolved(toWeb));
    }
}
exports.IndirectUrlResolver = IndirectUrlResolver;
function brandAsRuntimeUrl(resolved) {
    const str = resolved;
    return str;
}
//# sourceMappingURL=indirect-url-resolver.js.map