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
/// <reference path="../custom_typings/sw-precache.d.ts" />
/// <reference types="node" />
import { SWConfig } from 'sw-precache';
import { LocalFsPath } from './path-transformers';
import { PolymerProject } from './polymer-project';
export interface AddServiceWorkerOptions {
    project: PolymerProject;
    buildRoot: LocalFsPath;
    bundled?: boolean;
    path?: LocalFsPath;
    swPrecacheConfig?: SWConfig | null;
    basePath?: LocalFsPath;
}
export declare const hasNoFileExtension: RegExp;
/**
 * Returns a promise that resolves with a generated service worker
 * configuration.
 */
export declare function generateServiceWorkerConfig(options: AddServiceWorkerOptions): Promise<SWConfig>;
/**
 * Returns a promise that resolves with a generated service worker (the file
 * contents), based off of the options provided.
 */
export declare function generateServiceWorker(options: AddServiceWorkerOptions): Promise<Buffer>;
/**
 * Returns a promise that resolves when a service worker has been generated
 * and written to the build directory. This uses generateServiceWorker() to
 * generate a service worker, which it then writes to the file system based on
 * the buildRoot & path (if provided) options.
 */
export declare function addServiceWorker(options: AddServiceWorkerOptions): Promise<void>;
