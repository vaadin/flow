/*
 * Copyright 2000-2020 Vaadin Ltd.
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

const fs = require('fs');
const path = require('path');
const glob = require('glob');
const mkdirp = require('mkdirp');

const ignoredFileExtensions = [".css", ".js", ".json"];

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
  const staticAssetsThemeFolder = path.resolve(projectStaticAssetsOutputFolder, "themes", path.basename(themeFolder));
  const collection = collectFolders(themeFolder, logger);

  // Only create assets folder if there are files to copy.
  if(collection.files.length > 0) {
    mkdirp.sync(staticAssetsThemeFolder);
    // create folders with
    collection.directories.forEach(directory => {
      const relativeDirectory = path.relative(themeFolder, directory);
      const targetDirectory = path.resolve(staticAssetsThemeFolder, relativeDirectory);

      mkdirp.sync(targetDirectory);
    });

    collection.files.forEach(file => {
      const relativeFile = path.relative(themeFolder, file);
      const targetFile = path.resolve(staticAssetsThemeFolder, relativeFile);
      copyFileIfAbsentOrNewer(file, targetFile, logger);
    });
  }
}

/**
 * Collect all folders with copyable files and all files to be copied.
 * Folder will not be added if no files in folder or subfolders.
 *
 * Files will not contain files with ignored extensions and folders only containing ignored files will not be added.
 *
 * @param folderToCopy folder we will copy files from
 * @param logger plugin logger
 * @return {{directories: [], files: []}} object containing directories to create and files to copy
 */
function collectFolders(folderToCopy, logger) {
  const collection = {directories: [], files: []};
  logger.trace("files in directory", fs.readdirSync(folderToCopy));
  fs.readdirSync(folderToCopy).forEach(file => {
    const fileToCopy = path.resolve(folderToCopy, file);
    if (fs.statSync(fileToCopy).isDirectory()) {
      logger.debug("Going through directory", fileToCopy);
      const result = collectFolders(fileToCopy, logger);
      if (result.files.length > 0) {
        collection.directories.push(fileToCopy);
        logger.debug("Adding directory", fileToCopy);
        collection.directories.push.apply(collection.directories, result.directories);
        collection.files.push.apply(collection.files, result.files);
      }
    } else if (!ignoredFileExtensions.includes(path.extname(fileToCopy))) {
      logger.debug("Adding file", fileToCopy);
      collection.files.push(fileToCopy);
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
    logger.log("no assets to handle no static assets were copied");
    return;
  }

  fs.mkdirSync(projectStaticAssetsOutputFolder, {
    recursive: true
  });
  const missingModules = checkModules(Object.keys(assets));
  if (missingModules.length > 0) {
    throw Error("Missing npm modules '" + missingModules.join("', '")
      + "' for assets marked in 'theme.json'.\n" +
      "Install package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm i'");
  }
  Object.keys(assets).forEach((module) => {

    const copyRules = assets[module];
    Object.keys(copyRules).forEach((copyRule) => {
      const nodeSources = path.resolve('node_modules/', module, copyRule);
      const files = glob.sync(nodeSources, {nodir: true});
      const targetFolder = path.resolve(projectStaticAssetsOutputFolder, "themes", themeName, copyRules[copyRule]);

      fs.mkdirSync(targetFolder, {
        recursive: true
      });
      files.forEach((file) => {
        const copyTarget = path.resolve(targetFolder, path.basename(file));
        copyFileIfAbsentOrNewer(file, copyTarget, logger);
      });
    });
  });
};

function checkModules(modules) {
  const missing = [];

  modules.forEach((module) => {
    if (!fs.existsSync(path.resolve('node_modules/', module))) {
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
  if (!fs.existsSync(copyTarget) || fs.statSync(copyTarget).mtime < fs.statSync(fileToCopy).mtime) {
    logger.trace("Copying: ", fileToCopy, '=>', copyTarget);
    fs.copyFileSync(fileToCopy, copyTarget);
  }
}

module.exports = {checkModules, copyStaticAssets, copyThemeResources};
