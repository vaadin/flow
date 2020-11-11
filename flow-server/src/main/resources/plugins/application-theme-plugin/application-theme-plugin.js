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

const fs = require('fs');
const path = require('path');
const generateThemeFile = require('./theme-generator');
const copyThemeResources = require('./theme-copy');

let logger;

/**
 * The application theme plugin is for generating, collecting and copying of theme files for the application theme.
 *
 * TODO: enable giving themes to handle #9383
 *
 * The plugin should be supplied with the paths for
 *
 *  themeJarFolder                  - theme folder inside a jar
 *  themeProjectFolders             - array of possible locations for theme folders inside the project
 *  projectStaticAssetsOutputFolder - path to where static assets should be put
 */
class ApplicationThemePlugin {
  constructor(options) {
    this.options = options;

    if(!this.options.themeJarFolder) {
      throw new Error("Missing themeJarFolder path");
    }
    if(!this.options.projectStaticAssetsOutputFolder) {
      throw new Error("Missing projectStaticAssetsOutputFolder path");
    }
    if(!this.options.themeProjectFolders) {
      throw new Error("Missing themeProjectFolders path array");
    }
  }

  apply(compiler) {
    logger = compiler.getInfrastructureLogger("ApplicationThemePlugin");

    compiler.hooks.afterEnvironment.tap("ApplicationThemePlugin", () => {
      if (fs.existsSync(this.options.themeJarFolder)) {
        logger.debug("Found themeFolder in jar file ", this.options.themeJarFolder);
        handleThemes(this.options.themeJarFolder, this.options.projectStaticAssetsOutputFolder);
      }

      this.options.themeProjectFolders.forEach((themeProjectFolder) => {
        if (fs.existsSync(themeProjectFolder)) {
          logger.debug("Found themeFolder from ", themeProjectFolder);
          handleThemes(themeProjectFolder, this.options.projectStaticAssetsOutputFolder);
        }
      });
    });
  }
}

module.exports = ApplicationThemePlugin;

/**
 * Copies static resources for theme and generates/writes the [theme-name].js for webpack to handle.
 *
 * @param {path} themesFolder folder containing application theme folders
 * @param {path} projectStaticAssetsOutputFolder folder to output files to
 */
function handleThemes(themesFolder, projectStaticAssetsOutputFolder) {
  const dir = getThemeFoldersSync(themesFolder);
  logger.debug("Found", dir.length, "theme directories");

  for (let i = 0; i < dir.length; i++) {
    const folder = dir[i];

    const themeName = folder;
    const themeFolder = path.resolve(themesFolder, themeName);
    logger.debug("Found theme ", themeName, " in folder ", themeFolder);

    copyThemeResources(themeName, themeFolder, projectStaticAssetsOutputFolder);

    const themeFile = generateThemeFile(
      themeFolder,
      themeName
    );

    fs.writeFileSync(path.resolve(themeFolder, themeName + '.js'), themeFile);
  }
};

/**
 * Collect all folders under the given theme project folder.
 * The found sub folders are the actual theme 'implementations'
 *
 * @param {string | Buffer | URL} folder theme project folder to collect folders from
 * @returns {string[]} array containing found folder names
 */
function getThemeFoldersSync(folder) {
  const themeFolders = [];
  fs.readdirSync(folder).forEach(file => {
    if (fs.statSync(path.resolve(folder, file)).isDirectory()) {
      themeFolders.push(file);
    }
  });
  return themeFolders;
}
