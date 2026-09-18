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
import { PackageRelativeUrl, UrlResolver } from '../index';
import { FileRelativeUrl, ResolvedUrl } from '../model/url';
import { FsUrlResolver } from './fs-url-resolver';
/**
 * A URL resolver for very large codebases where source files map in an
 * arbitrary but predetermined fashion onto URL space.
 *
 * It also separates the root directory – the root of all source code that's
 * legal to load – from the package directory, which is how the user refers to
 * files on the CLI or the IDE.
 */
export declare class IndirectUrlResolver extends FsUrlResolver implements UrlResolver {
    protected readonly protocol: string;
    private readonly runtimeUrlToResolvedUrl;
    private readonly resolvedUrlToRuntimeUrl;
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
    constructor(rootPath: string, packagePath: string, indirectionMap: Map<string, string>, protocol?: string);
    resolve(firstHref: ResolvedUrl | PackageRelativeUrl, secondHref?: FileRelativeUrl): ResolvedUrl | undefined;
    private runtimeResolve;
    relative(to: ResolvedUrl): PackageRelativeUrl;
    relative(from: ResolvedUrl, to: ResolvedUrl, _kind?: string): FileRelativeUrl;
    private relativeImpl;
}
