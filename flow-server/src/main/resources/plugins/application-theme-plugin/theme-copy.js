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
 * This file handles copying of theme files to
 * [staticResourcesFolder]/theme/[theme-name]
 */

const fs = require('fs');
const path = require('path');

/**
 * create theme/themeName folders and copy theme files there.
 *
 * @param {String} themeName name of theme we are handling
 * @param {string} themeFolder Folder with theme file
 * @param {string} projectStaticAssetsOutputFolder resources output folder
 */
function copyThemeResources(themeName, themeFolder, projectStaticAssetsOutputFolder) {
  if (!fs.existsSync(path.resolve(projectStaticAssetsOutputFolder))) {
    require('mkdirp')(path.resolve(projectStaticAssetsOutputFolder));
  }
  if (!fs.existsSync(path.resolve(projectStaticAssetsOutputFolder, "theme"))) {
    fs.mkdirSync(path.resolve(projectStaticAssetsOutputFolder, "theme"));
  }
  if (!fs.existsSync(path.resolve(projectStaticAssetsOutputFolder, "theme", themeName))) {
    fs.mkdirSync(path.resolve(projectStaticAssetsOutputFolder, "theme", themeName));
  }
  copyThemeFiles(themeFolder, path.resolve(projectStaticAssetsOutputFolder, "theme", themeName));
}

const ignoredFileExtensions = [".css", ".js", ".json"];

/**
 * Recursively copy files found in theme folder excluding any with a extension found in the `ignoredFileExtensions` array.
 *
 * Any sub folders will be generated and recursed.
 *
 * @param {string} folderToCopy folder to copy files from
 * @param {string} targetFolder folder to copy files to
 */
function copyThemeFiles(folderToCopy, targetFolder) {
  fs.readdirSync(folderToCopy).forEach(file => {
    if (fs.statSync(path.resolve(folderToCopy, file)).isDirectory()) {
      if (!fs.existsSync(path.resolve(targetFolder, file))) {
        fs.mkdirSync(path.resolve(targetFolder, file));
      }
      copyThemeFiles(path.resolve(folderToCopy, file), path.resolve(targetFolder, file));
    } else if (!ignoredFileExtensions.includes(path.extname(file))) {
      fs.copyFileSync(path.resolve(folderToCopy, file), path.resolve(targetFolder, file));
    }
  });
}

module.exports = copyThemeResources;
