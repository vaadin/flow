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

import * as escapeHtml from 'escape-html';
import * as express from 'express';
import * as path from 'path';
import {parse as parseUrl} from 'url';

import send = require('send');

export interface AppOptions {
  componentDir: string;
  packageName: string;
  headers?: {[name: string]: string};
  root?: string;
  compile?: 'always'|'never'|'auto';
}

export interface PolyserveApplication extends express.Express {
  packageName: string;
}

/**
 * Make a polyserve express app.
 * @param  {Object} options
 * @param  {string} options.componentDir The directory to serve components from.
 * @param  {string} options.packageName A name for this polyserve package.
 * @param  {Object} options.headers An object keyed by header name containing
 *         header values.
 * @param  {string} options.root The root directory to serve a package from
 * @return {Object} An express app which can be served with `app.get`
 */
export function makeApp(options: AppOptions): PolyserveApplication {
  const root = path.resolve(options.root);
  const baseComponentDir = options.componentDir;
  const componentDir = path.resolve(root, baseComponentDir);
  const packageName = options.packageName;
  const headers = options.headers || {};

  if (packageName == null) {
    throw new Error('packageName not provided');
  }

  const app: PolyserveApplication = <PolyserveApplication>express();

  app.get('*', (req, res) => {
    // Serve local files from . and other components from bower_components
    const url = parseUrl(req.url, true);
    let splitPath = url.pathname.split('/').slice(1);
    const splitPackagePath = packageName.split('/');

    if (arrayStartsWith(splitPath, splitPackagePath)) {
      if (root) {
        splitPath = [root].concat(splitPath.slice(splitPackagePath.length));
      } else {
        splitPath = splitPath.slice(splitPackagePath.length);
      }
    } else {
      splitPath = [componentDir].concat(splitPath);
    }
    const filePath = splitPath.join('/');

    if (headers) {
      for (const header in headers) {
        res.setHeader(header, headers[header]);
      }
    }
    const _send = send(req, filePath, {etag: false, lastModified: false});
    // Uncomment this to disable 304s from send(). This will make the
    // compileMiddleware used in startServer always compile. Useful for testing
    // and working on the compilation middleware.
    // _send.isFresh = () => false;

    // The custom redirect is needed becuase send() redirects to the
    // _file_ path plus a leading slash, not the URL. :(
    // https://github.com/pillarjs/send/issues/132
    _send
        .on('directory',
            () => {
              res.statusCode = 301;
              res.setHeader('Location', req.originalUrl + '/');
              res.end('Redirecting to ' + req.originalUrl + '/');
            })
        .on('error',
            (err: {statusCode?: number, path: string, Error: string}) => {
              res.status(err.statusCode);
              res.type('html');
              res.end(escapeHtml(err.Error));
            })
        .pipe(res);
  });
  app.packageName = packageName;
  return app;
}

function arrayStartsWith<V>(array: V[], prefix: V[]) {
  for (let i = 0; i < prefix.length; i++) {
    if (i >= array.length || array[i] !== prefix[i]) {
      return false;
    }
  }
  return true;
}
