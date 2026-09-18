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
import { FileRelativeUrl, PackageRelativeUrl, ResolvedUrl, ScannedImport } from '../model/model';
import { UrlResolver } from './url-resolver';
/**
 * Resolves a URL having one prefix to another URL with a different prefix.
 */
export declare class RedirectResolver extends UrlResolver {
    private readonly packageUrl;
    private readonly _redirectFrom;
    private readonly _redirectTo;
    constructor(packageUrl: ResolvedUrl, _redirectFrom: string, _redirectTo: string);
    resolve(firstUrl: ResolvedUrl | PackageRelativeUrl, secondUrl?: FileRelativeUrl, _import?: ScannedImport): ResolvedUrl | undefined;
    relative(to: ResolvedUrl): PackageRelativeUrl;
    relative(from: ResolvedUrl, to: ResolvedUrl, kind?: string): FileRelativeUrl;
}
