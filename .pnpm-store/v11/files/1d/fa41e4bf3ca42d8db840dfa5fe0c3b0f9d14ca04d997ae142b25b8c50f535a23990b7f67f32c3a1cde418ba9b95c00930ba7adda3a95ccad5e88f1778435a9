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
/// <reference types="node" />
import { Url } from 'url';
export declare function parseUrl(url: string, defaultProtocol?: string): Url;
export declare function ensureProtocol(url: string, defaultProtocol: string): string;
export declare function resolveUrl(baseUrl: string, targetUrl: string, defaultProtocol: string): string;
export declare function trimLeft(str: string, char: string): string;
export declare class Deferred<T> {
    promise: Promise<T>;
    resolve: (result: T) => void;
    reject: (error: {} | undefined | null) => void;
    resolved: boolean;
    rejected: boolean;
    error: {} | undefined | null;
    constructor();
    toNodeCallback(): (error: {} | null | undefined, value: T) => void;
}
export declare function addAll<T>(set1: Set<T>, set2: Set<T>): Set<T>;
