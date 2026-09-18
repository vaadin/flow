import { ResolvedUrl } from 'polymer-analyzer';
/**
 * Produce a version of the URL provided with the given extension concatenated
 * to the path. Example:
 *     appendUrlPath('file:///something/something.html?ponies', '_omg.js')
 * Produces:
 *     'file:///something/something.html_omg.js?ponies'
 */
export declare function appendUrlPath(url: string, extension: string): string;
/**
 * Given a string representing a relative path of some form, ensure a `./`
 * leader if it doesn't already start with dot-based path leader or a scheme
 * (like, you wouldn't want to change `file:///example.js` into
 * `./file:///example.js`)
 */
export declare function ensureLeadingDot<T extends string>(href: T): T;
/**
 * Given a string representing a URL or path of some form, append a `/`
 * character if it doesn't already end with one.
 */
export declare function ensureTrailingSlash<T extends string>(href: T): T;
/**
 * Parses the URL and returns the extname of the path.
 */
export declare function getFileExtension(url_: string): string;
/**
 * Parses the URL and returns only the filename part of the path.
 */
export declare function getFileName(url_: string): string;
/**
 * Returns a WHATWG ResolvedURL for a filename on local filesystem.
 */
export declare function getFileUrl(filename: string): ResolvedUrl;
/**
 * Returns a URL with the basename removed from the pathname.  Strips the
 * search off of the URL as well, since it will not apply.
 */
export declare function stripUrlFileSearchAndHash<T extends string>(href: T): T;
/**
 * Returns true if the href is an absolute path.
 */
export declare function isAbsolutePath(href: string): boolean;
/**
 * Returns true if the href is a templated value, i.e. `{{...}}` or `[[...]]`
 */
export declare function isTemplatedUrl(href: string): boolean;
/**
 * The path library's resolve function drops the trailing slash from the input
 * when returning the result.  This is bad because clients of the function then
 * have to ensure it is reapplied conditionally.  This function resolves the
 * input path while preserving the trailing slash, when present.
 */
export declare function resolvePath(...segments: string[]): string;
