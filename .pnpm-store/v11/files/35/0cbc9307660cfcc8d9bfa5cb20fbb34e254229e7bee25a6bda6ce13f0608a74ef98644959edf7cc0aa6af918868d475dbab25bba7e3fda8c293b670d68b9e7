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

import * as assert from 'assert';
import {writeFile} from 'fs';
import * as path from 'path';
import * as logging from 'plylog';
import {generate as swPrecacheGenerate, SWConfig} from 'sw-precache';

import {DepsIndex} from './analyzer';
import {LocalFsPath, posixifyPath, PosixPath} from './path-transformers';
import {PolymerProject} from './polymer-project';

const logger = logging.getLogger('polymer-build.service-worker');

export interface AddServiceWorkerOptions {
  project: PolymerProject;
  buildRoot: LocalFsPath;
  bundled?: boolean;
  path?: LocalFsPath;
  swPrecacheConfig?: SWConfig|null;
  basePath?: LocalFsPath;
}

/**
 * Given a user-provided AddServiceWorkerOptions object, check for deprecated
 * options. When one is found, warn the user and fix if possible.
 */
// tslint:disable-next-line: no-any Turned off for user input.
function fixDeprecatedOptions(options: any): AddServiceWorkerOptions {
  if (typeof options.serviceWorkerPath !== 'undefined') {
    logger.warn(
        '"serviceWorkerPath" config option has been renamed to "path" and will no longer be supported in future versions');
    options.path = options.path || options.serviceWorkerPath;
  }
  if (typeof options.swConfig !== 'undefined') {
    logger.warn(
        '"swConfig" config option has been renamed to "swPrecacheConfig" and will no longer be supported in future versions');
    options.swPrecacheConfig = options.swPrecacheConfig || options.swConfig;
  }
  return options;
}

/**
 * Returns an array of file paths for the service worker to precache, based on
 * the information provided in the DepsIndex object.
 */
function getPrecachedAssets(
    depsIndex: DepsIndex, project: PolymerProject): string[] {
  const precachedAssets = new Set<string>(project.config.allFragments);
  precachedAssets.add(project.config.entrypoint);

  for (const depImports of depsIndex.fragmentToFullDeps.values()) {
    depImports.imports.forEach((s) => precachedAssets.add(s));
    depImports.scripts.forEach((s) => precachedAssets.add(s));
    depImports.styles.forEach((s) => precachedAssets.add(s));
  }

  return Array.from(precachedAssets);
}

/**
 * Returns an array of file paths for the service worker to precache for a
 * BUNDLED build, based on the information provided in the DepsIndex object.
 */
function getBundledPrecachedAssets(project: PolymerProject) {
  const precachedAssets = new Set<string>(project.config.allFragments);
  precachedAssets.add(project.config.entrypoint);

  return Array.from(precachedAssets);
}

// Matches URLs like "/foo.png/bar" but not "/foo/bar.png".
export const hasNoFileExtension = /\/[^\/\.]*(\?|$)/;

/**
 * Returns a promise that resolves with a generated service worker
 * configuration.
 */
export async function generateServiceWorkerConfig(
    options: AddServiceWorkerOptions): Promise<SWConfig> {
  assert(!!options, '`project` & `buildRoot` options are required');
  assert(!!options.project, '`project` option is required');
  assert(!!options.buildRoot, '`buildRoot` option is required');
  options = fixDeprecatedOptions(options);

  options = Object.assign({}, options);
  const project = options.project;
  const buildRoot = options.buildRoot;
  const swPrecacheConfig: SWConfig =
      Object.assign({}, options.swPrecacheConfig);

  const depsIndex = await project.analyzer.analyzeDependencies;
  let staticFileGlobs = Array.from(swPrecacheConfig.staticFileGlobs || []);
  const precachedAssets = (options.bundled) ?
      getBundledPrecachedAssets(project) :
      getPrecachedAssets(depsIndex, project);

  staticFileGlobs = staticFileGlobs.concat(precachedAssets);
  staticFileGlobs = staticFileGlobs.map((filePath: string) => {
    if (filePath.startsWith(project.config.root)) {
      filePath = filePath.substring(project.config.root.length);
    }
    return path.join(buildRoot, filePath);
  });

  if (swPrecacheConfig.navigateFallback === undefined) {
    // Map all application routes to the entrypoint.
    swPrecacheConfig.navigateFallback =
        path.relative(project.config.root, project.config.entrypoint);
  }

  if (swPrecacheConfig.navigateFallbackWhitelist === undefined) {
    // Don't fall back to the entrypoint if the URL looks like a static file.
    // We want those to 404 instead, since they are probably missing assets,
    // not application routes. Note it's important that this matches the
    // behavior of prpl-server.
    swPrecacheConfig.navigateFallbackWhitelist = [hasNoFileExtension];
  }

  if (swPrecacheConfig.directoryIndex === undefined) {
    // By default, sw-precache maps any path ending with "/" to "index.html".
    // This is a reasonable default for matching application routes, but 1) our
    // entrypoint might not be called "index.html", and 2) this case is already
    // handled by the navigateFallback configuration above. Simplest to just
    // disable this feature.
    swPrecacheConfig.directoryIndex = '';
  }

  // swPrecache will determine the right urls by stripping buildRoot.
  // NOTE:(usergenic) sw-precache generate() apparently replaces the
  // prefix on an already posixified version of the path on win32.
  //
  // We include a trailing slash in `stripPrefix` so that we remove leading
  // slashes on the pre-cache asset URLs, hence producing relative URLs
  // instead of absolute. We want relative URLs for builds mounted at non-root
  // paths. Note that service worker fetches are relative to its own URL.
  swPrecacheConfig.stripPrefix = addTrailingSlash(posixifyPath(buildRoot));

  if (options.basePath) {
    // TODO Drop this feature once CLI doesn't depend on it.
    let replacePrefix = posixifyPath(options.basePath);
    if (!replacePrefix.endsWith('/')) {
      replacePrefix = replacePrefix + '/' as PosixPath;
    }
    if (swPrecacheConfig.replacePrefix) {
      console.info(
          `Replacing service worker configuration's ` +
          `replacePrefix option (${swPrecacheConfig.replacePrefix}) ` +
          `with the build configuration's basePath (${replacePrefix}).`);
    }
    swPrecacheConfig.replacePrefix = replacePrefix;
  }

  // static files will be pre-cached
  swPrecacheConfig.staticFileGlobs = staticFileGlobs;

  // Log service-worker helpful output at the debug log level
  swPrecacheConfig.logger = swPrecacheConfig.logger || logger.debug;

  return swPrecacheConfig;
}

/**
 * Returns a promise that resolves with a generated service worker (the file
 * contents), based off of the options provided.
 */
export async function generateServiceWorker(options: AddServiceWorkerOptions):
    Promise<Buffer> {
  const swPrecacheConfig = await generateServiceWorkerConfig(options);
  return await<Promise<Buffer>>(new Promise((resolve, reject) => {
    logger.debug(`writing service worker...`, swPrecacheConfig);
    swPrecacheGenerate(
        swPrecacheConfig, (err?: Error, fileContents?: string) => {
          if (err || fileContents == null) {
            reject(err || 'No file contents provided.');
          } else {
            // Note: Node 10 Function.prototype.toString() produces output
            // like `function() { }` where earlier versions produce
            // `function () { }` (note the space between function keyword)
            // and parentheses.  To ensure the output is consistent across
            // versions, we will correctively insert missing space here.
            fileContents = fileContents.replace(/\bfunction\(/g, 'function (');
            resolve(Buffer.from(fileContents));
          }
        });
  }));
}

/**
 * Returns a promise that resolves when a service worker has been generated
 * and written to the build directory. This uses generateServiceWorker() to
 * generate a service worker, which it then writes to the file system based on
 * the buildRoot & path (if provided) options.
 */
export function addServiceWorker(options: AddServiceWorkerOptions):
    Promise<void> {
  return generateServiceWorker(options).then((fileContents: Buffer) => {
    return new Promise<void>((resolve, reject) => {
      const serviceWorkerPath =
          path.join(options.buildRoot, options.path || 'service-worker.js');
      writeFile(serviceWorkerPath, fileContents, (err) => {
        if (err) {
          reject(err);
        } else {
          resolve();
        }
      });
    });
  });
}

function addTrailingSlash(s: string): string {
  return s.endsWith('/') ? s : s + '/';
}
