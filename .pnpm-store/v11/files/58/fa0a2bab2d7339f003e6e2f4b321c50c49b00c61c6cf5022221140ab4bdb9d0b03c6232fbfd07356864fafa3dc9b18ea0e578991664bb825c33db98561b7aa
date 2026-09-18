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
const pathlib = require("path");
const path_1 = require("path");
const url_1 = require("url");
const vscode_uri_1 = require("vscode-uri");
const utils_1 = require("../core/utils");
const url_resolver_1 = require("./url-resolver");
const isWindows = process.platform === 'win32';
/**
 * Resolves package-relative urls to a configured root directory.
 *
 * For file-relative URLs it does the normal URL resolution algorithm relative
 * to the base url.
 *
 * It does no remapping of urls in source to urls on the filesystem, but a
 * subclass can override modifyFsPath for this purpose.
 */
class FsUrlResolver extends url_resolver_1.UrlResolver {
    constructor(packageDir, 
    // If provided, any URL which matches `host` will attempt to resolve
    // to a `file` protocol URL regardless of the protocol represented in the
    // URL to-be-resolved.
    host, 
    // When attempting to resolve a protocol-relative URL (that is a URL which
    // begins `//`), the default protocol to resolve to if the resolver can
    // not produce a `file` URL.
    protocol = 'https') {
        super();
        this.host = host;
        this.protocol = protocol;
        this.packageDir =
            normalizeFsPath(pathlib.resolve(packageDir || process.cwd()));
        this.packageUrl =
            this.brandAsResolved(vscode_uri_1.default.file(this.packageDir).toString());
        if (!this.packageUrl.endsWith('/')) {
            this.packageUrl = this.brandAsResolved(this.packageUrl + '/');
        }
    }
    resolve(firstHref, secondHref, _import) {
        const [baseUrl = this.packageUrl, unresolvedHref] = this.getBaseAndUnresolved(firstHref, secondHref);
        const resolvedHref = this.simpleUrlResolve(baseUrl, unresolvedHref, this.protocol);
        if (resolvedHref === undefined) {
            return undefined;
        }
        const url = utils_1.parseUrl(resolvedHref);
        if (this.shouldHandleAsFileUrl(url)) {
            return this.handleFileUrl(url, unresolvedHref);
        }
        return this.brandAsResolved(resolvedHref);
    }
    shouldHandleAsFileUrl(url) {
        const isLocalFileUrl = url.protocol === 'file:' && (!url.host || url.host === 'localhost');
        const isOurHost = url.host === this.host;
        return isLocalFileUrl || isOurHost;
    }
    /**
     * Take the given URL which is either a file:// url or a url with the
     * configured hostname, and treat its pathname as though it points to a file
     * on the local filesystem, producing a file:/// url.
     *
     * Also corrects sibling URLs like `../foo` to point to
     * `./${component_dir}/foo`
     */
    handleFileUrl(url, unresolvedHref) {
        let pathname;
        const unresolvedUrl = utils_1.parseUrl(unresolvedHref);
        if (unresolvedUrl.pathname && unresolvedUrl.pathname.startsWith('/') &&
            unresolvedUrl.protocol !== 'file:') {
            // Absolute urls point to the package root.
            let unresolvedPathname;
            try {
                unresolvedPathname =
                    path_1.posix.normalize(decodeURIComponent(unresolvedUrl.pathname));
            }
            catch (e) {
                return undefined; // undecodable url
            }
            pathname = pathlib.join(this.packageDir, unresolvedPathname);
        }
        else {
            // Otherwise, consider the url that has already been resolved
            // against the baseUrl
            try {
                pathname = path_1.posix.normalize(decodeURIComponent(url.pathname || ''));
            }
            catch (e) {
                return undefined; // undecodable url
            }
        }
        const path = this.modifyFsPath(this.filesystemPathForPathname(pathname));
        // TODO(rictic): investigate moving to whatwg URLs internally:
        //     https://github.com/Polymer/polymer-analyzer/issues/804
        // Re-encode URI, since it is expected we are emitting a relative URL.
        const resolvedUrl = utils_1.parseUrl(vscode_uri_1.default.file(path).toString());
        resolvedUrl.search = url.search;
        resolvedUrl.hash = url.hash;
        return this.brandAsResolved(url_1.format(resolvedUrl));
    }
    /**
     * Overridable method, for subclasses that want to redirect some filesystem
     * paths.
     *
     * @param fsPath An absolute path on the file system. Note that it will be
     *     OS-specific.
     * @return An absolute path on the file system that we should resolve to.
     */
    modifyFsPath(fsPath) {
        return fsPath;
    }
    relative(fromOrTo, maybeTo, _kind) {
        const [from, to] = (maybeTo !== undefined) ? [fromOrTo, maybeTo] :
            [this.packageUrl, fromOrTo];
        return this.simpleUrlRelative(from, to);
    }
    filesystemPathForPathname(decodedPathname) {
        return normalizeFsPath(vscode_uri_1.default.file(decodedPathname).fsPath);
    }
}
exports.FsUrlResolver = FsUrlResolver;
/**
 * Normalizes slashes, `..`, `.`, and on Windows the capitalization of the
 * drive letter.
 */
function normalizeFsPath(fsPath) {
    fsPath = pathlib.normalize(fsPath);
    if (isWindows && /^[a-z]:/.test(fsPath)) {
        // Upper case the drive letter
        fsPath = fsPath[0].toUpperCase() + fsPath.slice(1);
    }
    return fsPath;
}
//# sourceMappingURL=fs-url-resolver.js.map