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
import { FileRelativeUrl, PackageRelativeUrl } from '../index';
import { ResolvedUrl } from '../model/url';
import { FsUrlResolver } from './fs-url-resolver';
export interface PackageUrlResolverOptions {
    packageDir?: string;
    componentDir?: string;
    host?: string;
    protocol?: string;
}
/**
 * Resolves a URL to a canonical URL within a package.
 */
export declare class PackageUrlResolver extends FsUrlResolver {
    readonly componentDir: string;
    private readonly resolvedComponentDir;
    constructor(options?: PackageUrlResolverOptions);
    protected modifyFsPath(path: string): string;
    relative(to: ResolvedUrl): PackageRelativeUrl;
    relative(from: ResolvedUrl, to: ResolvedUrl, _kind?: string): FileRelativeUrl;
    private relativeImpl;
    /**
     * If the given URL is a file url inside our dependencies (e.g.
     * bower_components) then return a resolved posix path to its file.
     * Otherwise return undefined.
     */
    private pathnameForComponentDirUrl;
}
