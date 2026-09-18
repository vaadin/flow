/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
import { PackageRelativeUrl } from '../index';
import { ResolvedUrl } from '../model/url';
import { UrlLoader } from './url-loader';
/**
 * Resolves requests via one of a sequence of loaders.
 */
export declare class MultiUrlLoader implements UrlLoader {
    private _loaders;
    constructor(_loaders: UrlLoader[]);
    canLoad(url: ResolvedUrl): boolean;
    load(url: ResolvedUrl): Promise<string>;
    readDirectory?(path: ResolvedUrl, deep?: boolean): Promise<PackageRelativeUrl[]>;
}
