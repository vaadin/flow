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
/// <reference types="node" />
import { Url } from 'url';
import { FileRelativeUrl, PackageRelativeUrl, ScannedImport } from '../index';
import { ResolvedUrl } from '../model/url';
import { UrlResolver } from './url-resolver';
/**
 * Resolves package-relative urls to a configured root directory.
 *
 * For file-relative URLs it does the normal URL resolution algorithm relative
 * to the base url.
 *
 * It does no remapping of urls in source to urls on the filesystem, but a
 * subclass can override modifyFsPath for this purpose.
 */
export declare class FsUrlResolver extends UrlResolver {
    private readonly host?;
    protected readonly protocol: string;
    protected readonly packageDir: string;
    protected readonly packageUrl: ResolvedUrl;
    constructor(packageDir: string | undefined, host?: string | undefined, protocol?: string);
    resolve(firstHref: ResolvedUrl | PackageRelativeUrl, secondHref?: FileRelativeUrl, _import?: ScannedImport): ResolvedUrl | undefined;
    protected shouldHandleAsFileUrl(url: Url): boolean;
    /**
     * Take the given URL which is either a file:// url or a url with the
     * configured hostname, and treat its pathname as though it points to a file
     * on the local filesystem, producing a file:/// url.
     *
     * Also corrects sibling URLs like `../foo` to point to
     * `./${component_dir}/foo`
     */
    private handleFileUrl;
    /**
     * Overridable method, for subclasses that want to redirect some filesystem
     * paths.
     *
     * @param fsPath An absolute path on the file system. Note that it will be
     *     OS-specific.
     * @return An absolute path on the file system that we should resolve to.
     */
    protected modifyFsPath(fsPath: string): string;
    relative(to: ResolvedUrl): PackageRelativeUrl;
    relative(from: ResolvedUrl, to: ResolvedUrl, _kind?: string): FileRelativeUrl;
    protected filesystemPathForPathname(decodedPathname: string): string;
}
