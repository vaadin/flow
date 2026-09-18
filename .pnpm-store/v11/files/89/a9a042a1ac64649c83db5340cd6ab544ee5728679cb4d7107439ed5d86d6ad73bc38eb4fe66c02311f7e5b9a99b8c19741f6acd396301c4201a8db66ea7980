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
/**
 * These are a set of entirely-compile time types, used to help us manage the
 * lifecycle of a URL in the system, to ensure that we primarily deal with
 * canonical urls internally in the project.
 *
 * These types should be project-level concerns and should not impact users.
 */
/**
 * A URL that must be resolved relative to a specific base url.
 *
 * Note that it might still be an absolute URL, or even a url with a full
 * protocol and host.
 *
 * This is mostly used to type urls extracted from import statements taken
 * directly out of documents. For example, in `import * as foo from "./foo.js"`
 * `./foo.js` is relative to the containing document.
 *
 * Use UrlResolver#resolve to transform it to a ResolvedUrl.
 * Use UrlResolver#relative to transform ResolvedUrls to FileRelativeUrls.
 */
export declare type FileRelativeUrl = string & FileRelativeUrlBrand;
/**
 * A URL that must be resolved relative to the package itself.
 *
 * Note it might still be an absolute URL, or even a url with a full protocol
 * and host.
 *
 * This is the assumed format of user input to Analyzer methods.
 *
 * Use UrlResolver#resolve to transform it to a ResolvedUrl.
 */
export declare type PackageRelativeUrl = string & PackageRelativeUrlBrand;
/**
 * A URL that has been resolved to its canonical and loadable form, by passing
 * through the project's URL Resolver.
 *
 * Use AnalysisContext#resolve to get a ResolvedUrl.
 */
export declare type ResolvedUrl = string & ResolvedUrlBrand;
export declare class ResolvedUrlBrand {
    private ResolvedUrlBrand;
}
export declare class PackageRelativeUrlBrand {
    private PackageRelativeUrlBrand;
}
export declare class FileRelativeUrlBrand {
    private FileRelativeUrlBrand;
}
