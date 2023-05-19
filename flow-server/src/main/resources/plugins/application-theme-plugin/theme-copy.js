/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * This contains functions and features used to copy theme files.
 */

import { readdirSync, statSync, mkdirSync, existsSync, copyFileSync } from 'fs';
import { resolve, basename, relative, extname } from 'path';
import glob from 'glob';
import mkdirp from 'mkdirp';

const { sync } = glob;
const { sync: mkdirpSync } = mkdirp;

const ignoredFileExtensions = ['.css', '.js', '.json'];

/**
 * Copy theme static resources to static assets folder. All files in the theme
 * folder will be copied excluding css, js and json files that will be
 * handled by webpack and not be shared as static files.
 *
 * @param {string} themeFolder Folder with theme file
 * @param {string} projectStaticAssetsOutputFolder resources output folder
 * @param {object} logger plugin logger
 */
function copyThemeResources(themeFolder, projectStaticAssetsOutputFolder, logger) {
  const staticAssetsThemeFolder = resolve(projectStaticAssetsOutputFolder, 'themes', basename(themeFolder));
  const collection = collectFolders(themeFolder, logger);

  // Only create assets folder if there are files to copy.
  if (collection.files.length > 0) {
    mkdirpSync(staticAssetsThemeFolder);
    // create folders with
    collection.directories.forEach((directory) => {
      const relativeDirectory = relative(themeFolder, directory);
      const targetDirectory = resolve(staticAssetsThemeFolder, relativeDirectory);

      mkdirpSync(targetDirectory);
    });

    collection.files.forEach((file) => {
      const relativeFile = relative(themeFolder, file);
      const targetFile = resolve(staticAssetsThemeFolder, relativeFile);
      copyFileIfAbsentOrNewer(file, targetFile, logger);
    });
  }
}

/**
 * Collect all folders with copyable files and all files to be copied.
 * Foled will not be added if no files in folder or subfolders.
 *
 * Files will not contain files with ignored extensions and folders only containing ignored files will not be added.
 *
 * @param folderToCopy folder we will copy files from
 * @param logger plugin logger
 * @return {{directories: [], files: []}} object containing directories to create and files to copy
 */
function collectFolders(folderToCopy, logger) {
  const collection = { directories: [], files: [] };
  logger.trace('files in directory', readdirSync(folderToCopy));
  readdirSync(folderToCopy).forEach((file) => {
    const fileToCopy = resolve(folderToCopy, file);
    try {
      if (statSync(fileToCopy).isDirectory()) {
        logger.debug('Going through directory', fileToCopy);
        const result = collectFolders(fileToCopy, logger);
        if (result.files.length > 0) {
          collection.directories.push(fileToCopy);
          logger.debug('Adding directory', fileToCopy);
          collection.directories.push.apply(collection.directories, result.directories);
          collection.files.push.apply(collection.files, result.files);
        }
      } else if (!ignoredFileExtensions.includes(extname(fileToCopy))) {
        logger.debug('Adding file', fileToCopy);
        collection.files.push(fileToCopy);
      }
    } catch (error) {
      handleNoSuchFileError(fileToCopy, error, logger);
    }
  });
  return collection;
}

/**
 * Copy any static node_modules assets marked in theme.json to
 * project static assets folder.
 *
 * The theme.json content for assets is set up as:
 * {
 *   assets: {
 *     "node_module identifier": {
 *       "copy-rule": "target/folder",
 *     }
 *   }
 * }
 *
 * This would mean that an asset would be built as:
 * "@fortawesome/fontawesome-free": {
 *   "svgs/regular/**": "fortawesome/icons"
 * }
 * Where '@fortawesome/fontawesome-free' is the npm package, 'svgs/regular/**' is what should be copied
 * and 'fortawesome/icons' is the target directory under projectStaticAssetsOutputFolder where things
 * will get copied to.
 *
 * Note! there can be multiple copy-rules with target folders for one npm package asset.
 *
 * @param {string} themeName name of the theme we are copying assets for
 * @param {json} themeProperties theme properties json with data on assets
 * @param {string} projectStaticAssetsOutputFolder project output folder where we copy assets to under theme/[themeName]
 * @param {object} logger plugin logger
 */
function copyStaticAssets(themeName, themeProperties, projectStaticAssetsOutputFolder, logger) {
  const assets = themeProperties['assets'];
  if (!assets) {
    logger.debug('no assets to handle no static assets were copied');
    return;
  }

  mkdirSync(projectStaticAssetsOutputFolder, {
    recursive: true
  });
  const missingModules = checkModules(Object.keys(assets));
  if (missingModules.length > 0) {
    throw Error(
      "Missing npm modules '" +
        missingModules.join("', '") +
        "' for assets marked in 'theme.json'.\n" +
        "Install package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'"
    );
  }
  Object.keys(assets).forEach((module) => {
    const copyRules = assets[module];
    Object.keys(copyRules).forEach((copyRule) => {
      const nodeSources = resolve('node_modules/', module, copyRule);
      const files = sync(nodeSources, { nodir: true });
      const targetFolder = resolve(projectStaticAssetsOutputFolder, 'themes', themeName, copyRules[copyRule]);

      mkdirSync(targetFolder, {
        recursive: true
      });
      files.forEach((file) => {
        const copyTarget = resolve(targetFolder, basename(file));
        copyFileIfAbsentOrNewer(file, copyTarget, logger);
      });
    });
  });
}

function checkModules(modules) {
  const missing = [];

  modules.forEach((module) => {
    if (!existsSync(resolve('node_modules/', module))) {
      missing.push(module);
    }
  });

  return missing;
}

/**
 * Copies given file to a given target path, if target file doesn't exist or if
 * file to copy is newer.
 * @param {string} fileToCopy path of the file to copy
 * @param {string} copyTarget path of the target file
 * @param {object} logger plugin logger
 */
function copyFileIfAbsentOrNewer(fileToCopy, copyTarget, logger) {
  try {
    if (!existsSync(copyTarget) || statSync(copyTarget).mtime < statSync(fileToCopy).mtime) {
      logger.trace('Copying: ', fileToCopy, '=>', copyTarget);
      copyFileSync(fileToCopy, copyTarget);
    }
  } catch (error) {
    handleNoSuchFileError(fileToCopy, error, logger);
  }
}

// Ignores errors due to file missing during theme processing
// This may happen for example when an IDE creates a temporary file
// and then immediately deletes it
function handleNoSuchFileError(file, error, logger) {
    if (error.code === 'ENOENT') {
        logger.warn('Ignoring not existing file ' + file +
            '. File may have been deleted during theme processing.');
    } else {
        throw error;
    }
}

export {checkModules, copyStaticAssets, copyThemeResources};
