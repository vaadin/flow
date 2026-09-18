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

import * as assert from 'assert';
import * as express from 'express';
import * as fs from 'fs';
import * as mime from 'mime';
import * as path from 'path';

import {ServerOptions} from '../start_server';


/** h2 push manifest cache */
const _pushManifest = {};

/**
 * Asserts file existence for all specified files in a push-manifest
 * @param root path to root directory
 * @param manifest manifest object
 */
function assertValidManifest(root: string, manifest: {[path: string]: string}) {
  function assertExists(filename: string) {
    const fname = path.join(root, filename);
    try {
      // Ignore root path, since that always exists for the router
      if (filename !== '/') {
        assert(fs.existsSync(fname), `not found: ${fname}`);
      }
    } catch (err) {
      throw new Error(`invalid h2-push manifest: ${err}`);
    }
  }

  for (const refFile of Object.keys(manifest)) {
    assertExists(refFile);
    for (const pushFile of Object.keys(manifest[refFile])) {
      assertExists(pushFile);
    }
  }
}

/**
 * Reads a push-manifest from the specified path, or a cached version
 * of the file
 * @param root path to root directory
 * @param manifestPath path to manifest file
 * @returns the manifest
 */
export function getPushManifest(
    root: string, manifestPath: string): {[path: string]: {}} {
  if (!_pushManifest[manifestPath]) {
    const data = fs.readFileSync(manifestPath);
    const manifest = JSON.parse(data.toString());
    assertValidManifest(root, manifest);
    _pushManifest[manifestPath] = manifest;
  }
  return _pushManifest[manifestPath];
}


/**
 * Pushes any resources for the requested file
 * @param options server options
 * @param req HTTP request
 * @param res HTTP response
 */
export function pushResources(
    options: ServerOptions, req: express.Request, res: Response) {
  if (res.push && options.protocol === 'h2' && options.pushManifestPath &&
      !req.get('x-is-push')) {
    // TODO: Handle preload link headers
    const pushManifest =
        getPushManifest(options.root, options.pushManifestPath);
    const resources = pushManifest[req.path];
    if (resources) {
      const root = options.root;
      for (const filename of Object.keys(resources)) {
        const stream = res.push(filename, {
                            request: {accept: '*/*'},
                            response: {
                              'content-type': mime.getType(filename),

                              // Add an X-header to the pushed request so we
                              // don't trigger pushes for pushes
                              'x-is-push': 'true'
                            }
                          })
                           .on('error',
                               (err: {}) => console.error(
                                   'failed to push', filename, err));
        fs.createReadStream(path.join(root, filename)).pipe(stream);
      }
    }
  }
}

export interface Response extends express.Response {
  push?(filname: string, pushedThing: {
    request: {[key: string]: string},
    response: {[key: string]: string}
  }): NodeJS.WritableStream;
}
