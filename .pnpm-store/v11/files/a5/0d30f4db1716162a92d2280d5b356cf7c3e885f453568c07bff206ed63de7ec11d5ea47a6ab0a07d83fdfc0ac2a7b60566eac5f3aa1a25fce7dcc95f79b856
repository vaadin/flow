"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
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
const path = require("path");
const url = require("url");
const vscode_uri_1 = require("vscode-uri");
const constants_1 = require("./constants");
/**
 * Produce a version of the URL provided with the given extension concatenated
 * to the path. Example:
 *     appendUrlPath('file:///something/something.html?ponies', '_omg.js')
 * Produces:
 *     'file:///something/something.html_omg.js?ponies'
 */
function appendUrlPath(url, extension) {
    const { scheme, authority, path, query, fragment } = vscode_uri_1.default.parse(url);
    return vscode_uri_1.default.from({ scheme, authority, path: path + extension, query, fragment })
        .toString();
}
exports.appendUrlPath = appendUrlPath;
/**
 * Given a string representing a relative path of some form, ensure a `./`
 * leader if it doesn't already start with dot-based path leader or a scheme
 * (like, you wouldn't want to change `file:///example.js` into
 * `./file:///example.js`)
 */
function ensureLeadingDot(href) {
    if (!vscode_uri_1.default.parse(href).scheme &&
        !(href.startsWith('./') || href.startsWith('../'))) {
        return './' + href;
    }
    return href;
}
exports.ensureLeadingDot = ensureLeadingDot;
/**
 * Given a string representing a URL or path of some form, append a `/`
 * character if it doesn't already end with one.
 */
function ensureTrailingSlash(href) {
    return href.endsWith('/') ? href : (href + '/');
}
exports.ensureTrailingSlash = ensureTrailingSlash;
/**
 * Parses the URL and returns the extname of the path.
 */
function getFileExtension(url_) {
    return path.extname(getFileName(url_));
}
exports.getFileExtension = getFileExtension;
/**
 * Parses the URL and returns only the filename part of the path.
 */
function getFileName(url_) {
    const uri = vscode_uri_1.default.parse(url_);
    return uri.path.split(/\//).pop() || '';
}
exports.getFileName = getFileName;
/**
 * Returns a WHATWG ResolvedURL for a filename on local filesystem.
 */
function getFileUrl(filename) {
    return vscode_uri_1.default.file(resolvePath(filename)).toString();
}
exports.getFileUrl = getFileUrl;
/**
 * Returns a URL with the basename removed from the pathname.  Strips the
 * search off of the URL as well, since it will not apply.
 */
function stripUrlFileSearchAndHash(href) {
    const u = url.parse(href);
    // Using != so tests for null AND undefined
    if (u.pathname != null) {
        // Suffix path with `_` so that `/a/b/` is treated as `/a/b/_` and that
        // `path.posix.dirname()` returns `/a/b` because it would otherwise
        // return `/a` incorrectly.
        u.pathname = ensureTrailingSlash(path.posix.dirname(u.pathname + '_'));
    }
    // Assigning to undefined because TSC says type of these is
    // `string | undefined` as opposed to `string | null`
    u.search = undefined;
    u.hash = undefined;
    return url.format(u);
}
exports.stripUrlFileSearchAndHash = stripUrlFileSearchAndHash;
/**
 * Returns true if the href is an absolute path.
 */
function isAbsolutePath(href) {
    return constants_1.default.ABS_URL.test(href);
}
exports.isAbsolutePath = isAbsolutePath;
/**
 * Returns true if the href is a templated value, i.e. `{{...}}` or `[[...]]`
 */
function isTemplatedUrl(href) {
    return href.search(constants_1.default.URL_TEMPLATE) >= 0;
}
exports.isTemplatedUrl = isTemplatedUrl;
/**
 * The path library's resolve function drops the trailing slash from the input
 * when returning the result.  This is bad because clients of the function then
 * have to ensure it is reapplied conditionally.  This function resolves the
 * input path while preserving the trailing slash, when present.
 */
function resolvePath(...segments) {
    if (segments.length === 0) {
        // Special cwd case
        return ensureTrailingSlash(path.resolve());
    }
    const lastSegment = segments[segments.length - 1];
    const resolved = path.resolve(...segments);
    return lastSegment.endsWith('/') ? ensureTrailingSlash(resolved) : resolved;
}
exports.resolvePath = resolvePath;
//# sourceMappingURL=url-utils.js.map