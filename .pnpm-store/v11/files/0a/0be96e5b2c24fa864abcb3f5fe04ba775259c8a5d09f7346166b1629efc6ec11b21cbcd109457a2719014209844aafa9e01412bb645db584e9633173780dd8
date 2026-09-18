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
import { UAParser } from 'ua-parser-js';
/**
 * A feature supported by a web browser.
 */
export declare type BrowserCapability = 'es2015' | 'es2016' | 'es2017' | 'es2018' | 'push' | 'serviceworker' | 'modules';
export declare type UserAgentPredicate = (ua: InstanceType<typeof UAParser>) => boolean;
/**
 * Return the set of capabilities for a user agent string.
 */
export declare function browserCapabilities(userAgent: string): Set<BrowserCapability>;
/**
 * Parse a "x.y.z" version string of any length into integer parts. Returns -1
 * for a part that doesn't parse.
 */
export declare function parseVersion(version: string | undefined): number[];
/**
 * Return whether `version` is at least as high as `atLeast`.
 */
export declare function versionAtLeast(atLeast: number[], version: number[]): boolean;
