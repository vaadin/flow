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

import {browserCapabilities} from 'browser-capabilities';
import {parse as parseContentType} from 'content-type';
import {Request, RequestHandler, Response} from 'express';
import * as fs from 'fs';
import * as LRU from 'lru-cache';
import * as path from 'path';
import {htmlTransform, jsTransform} from 'polymer-build';

import {getCompileTarget} from './get-compile-target';
import {transformResponse} from './transform-middleware';

const javaScriptMimeTypes = [
  'application/javascript',
  'application/ecmascript',
  'text/javascript',
  'text/ecmascript',
];

const htmlMimeType = 'text/html';

const compileMimeTypes = [htmlMimeType, ...javaScriptMimeTypes];

// Match the polyfills from https://github.com/webcomponents/webcomponentsjs,
// but not their tests.
export const isPolyfill = /(^|\/)webcomponentsjs\/[^\/]+$/;

function getContentType(response: Response) {
  const contentTypeHeader = response.get('Content-Type');
  return contentTypeHeader && parseContentType(contentTypeHeader).type;
}

// NOTE: To change the max length of the cache at runtime, just use bracket
// notation, i.e. `babelCompileCache['max'] = 64 * 1024` for 64KB limit.
export const babelCompileCache = LRU<string>(<LRU.Options<string>>{
  length: (n: string, key: string) => n.length + key.length
});

// TODO(justinfagnani): see if we can just use the request path as the key
// See https://github.com/Polymer/polyserve/issues/248
export const getCompileCacheKey =
    (requestPath: string,
     body: string,
     options: {compileTarget: string, transformModules: boolean}): string =>
        JSON.stringify(options) + requestPath + body;

export function babelCompile(
    compile: 'always'|'never'|'auto',
    moduleResolution: 'none'|'node',
    rootDir: string,
    packageName: string,
    componentUrl: string,
    componentDir: string): RequestHandler {
  return transformResponse({

    shouldTransform(request: Request, response: Response) {
      // We must never compile the Custom Elements ES5 Adapter or other
      // polyfills/shims.
      if (isPolyfill.test(request.url)) {
        return false;
      }
      if ('nocompile' in request.query) {
        return false;
      }
      if (!compileMimeTypes.includes(getContentType(response))) {
        return false;
      }
      if (compile === 'always' || moduleResolution === 'node') {
        return true;
      }
      if (compile === 'never') {
        return false;
      }
      const capabilities = browserCapabilities(request.get('user-agent'));
      return !capabilities.has('es2015') || !capabilities.has('modules');
    },

    transform(request: Request, response: Response, body: string): string {
      const capabilities = browserCapabilities(request.get('user-agent'));
      const compileTarget = getCompileTarget(capabilities, compile);
      const options = {
        compileTarget,
        transformModules: compile === 'always' || !capabilities.has('modules'),
      };

      const cacheKey =
          getCompileCacheKey(request.baseUrl + request.path, body, options);
      const cached = babelCompileCache.get(cacheKey);
      if (cached !== undefined) {
        return cached;
      }

      // Make sure that componentDir is absolute, like jsTransform expects
      componentDir = path.resolve(rootDir, componentDir);

      let transformed;
      const contentType = getContentType(response);
      let requestPath = request.path;
      // TODO(justinfagnani): Use path-is-inside, but test on Windows
      const isRootPathRequest = request.path === `/${packageName}` ||
          request.path.startsWith(`/${packageName}/`);
      const isComponentRequest = request.baseUrl === `/${componentUrl}`;

      let filePath: string;

      if (isRootPathRequest) {
        requestPath = requestPath.substring(`/${packageName}`.length);
      }

      if (isComponentRequest && !isRootPathRequest) {
        filePath = path.join(componentDir, requestPath);
      } else {
        filePath = path.join(rootDir, requestPath);
      }

      // The file path needs to include the filename for correct relative
      // path calculation
      try {
        const stat = fs.statSync(filePath);
        if (stat.isDirectory()) {
          filePath = path.join(filePath, 'index.html');
        }
      } catch (e) {
        // file not found, will 404 later
      }

      if (contentType === htmlMimeType) {
        transformed = htmlTransform(body, {
          js: {
            compile: options.compileTarget,
            moduleResolution,
            filePath,
            packageName,
            isComponentRequest,
            componentDir,
            rootDir,
            transformModulesToAmd: options.transformModules,
            softSyntaxError: true,
          },
          injectAmdLoader: options.transformModules,
        });

      } else if (javaScriptMimeTypes.includes(contentType)) {
        transformed = jsTransform(body, {
          compile: options.compileTarget,
          transformModulesToAmd: options.transformModules ? 'auto' : false,
          moduleResolution,
          filePath: filePath,
          isComponentRequest,
          packageName,
          componentDir,
          rootDir,
        });

      } else {
        transformed = body;
      }
      babelCompileCache.set(cacheKey, transformed);
      return transformed;
    },
  });
}
