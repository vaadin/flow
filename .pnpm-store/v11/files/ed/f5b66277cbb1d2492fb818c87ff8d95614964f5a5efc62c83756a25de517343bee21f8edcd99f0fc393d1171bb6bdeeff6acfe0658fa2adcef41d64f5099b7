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
import { ResolvedUrl } from '../model/url';
import { UrlLoader } from './url-loader';
/**
 * Resolves requests via a given delegate loader for URLs matching a given
 * prefix. URLs are provided to their delegate without the prefix.
 */
export declare class PrefixedUrlLoader implements UrlLoader {
    prefix: string;
    delegate: UrlLoader;
    constructor(prefix: string, delegate: UrlLoader);
    canLoad(url: ResolvedUrl): boolean;
    load(url: ResolvedUrl): Promise<string>;
    private _unprefix;
}
