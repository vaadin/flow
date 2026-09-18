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
/// <reference types="vinyl-fs" />
import * as express from 'express';
import { ServerOptions } from '../start_server';
/**
 * Reads a push-manifest from the specified path, or a cached version
 * of the file
 * @param root path to root directory
 * @param manifestPath path to manifest file
 * @returns the manifest
 */
export declare function getPushManifest(root: string, manifestPath: string): {
    [path: string]: {};
};
/**
 * Pushes any resources for the requested file
 * @param options server options
 * @param req HTTP request
 * @param res HTTP response
 */
export declare function pushResources(options: ServerOptions, req: express.Request, res: Response): void;
export interface Response extends express.Response {
    push?(filname: string, pushedThing: {
        request: {
            [key: string]: string;
        };
        response: {
            [key: string]: string;
        };
    }): NodeJS.WritableStream;
}
