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
 * The plugin should be supplied with the paths for
 *
 *  themeResourceFolder             - theme folder where flow copies local and jar resource frontend files
 *  themeProjectFolders             - array of possible locations for theme folders inside the project
 *  projectStaticAssetsOutputFolder - path to where static assets should be put
 */
class ApplicationThemePlugin {
  constructor(options) {
    this.options = options;

    if (!this.options.themeResourceFolder) {
      throw new Error("Missing themeJarFolder path");
    }
    if (!this.options.projectStaticAssetsOutputFolder) {
      throw new Error("Missing projectStaticAssetsOutputFolder path");
    }
    if (!this.options.themeProjectFolders) {
      throw new Error("Missing themeProjectFolders path array");
    }
  }

  apply(compiler) {
    logger = compiler.getInfrastructureLogger("ApplicationThemePlugin");

    compiler.hooks.afterEnvironment.tap("ApplicationThemePlugin", () => {
      const generatedThemeFile = path.resolve(this.options.themeResourceFolder, "theme-generated.js");
      if (fs.existsSync(generatedThemeFile)) {

        // read theme name from the theme-generated.js as there we always mark the used theme for webpack to handle.
        const nameRegex = /theme\/(.*)\/\1.js/g; // matches theme folder name in 'theme/my-theme/my-theme.js'
        const themeName = nameRegex.exec(fs.readFileSync(generatedThemeFile, {encoding: 'utf8'}))[1];
        if (!themeName) {
          throw new Error("Couldn't parse theme name from '" + generatedThemeFile + "'.");
        }

        if (fs.existsSync(this.options.themeResourceFolder)) {
          logger.debug("Found themeFolder in jar file ", this.options.themeResourceFolder);
          handleThemes(themeName, this.options.themeResourceFolder, this.options.projectStaticAssetsOutputFolder);
        }

        this.options.themeProjectFolders.forEach((themeProjectFolder) => {
          if (fs.existsSync(themeProjectFolder)) {
            logger.debug("Found themeFolder from ", themeProjectFolder);
            handleThemes(themeName, themeProjectFolder, this.options.projectStaticAssetsOutputFolder);
          }
        });
      } else {
        logger.log("No '", generatedThemeFile, "' found. Skipping application theme handling.");
      }
    });
  }
}

module.exports = ApplicationThemePlugin;

/**
 * Copies static resources for theme and generates/writes the [theme-name].js for webpack to handle.
 *
 * @param {string} themeName name of theme to handle
 * @param {string} themesFolder folder containing application theme folders
 * @param {string} projectStaticAssetsOutputFolder folder to output files to
 */
function handleThemes(themeName, themesFolder, projectStaticAssetsOutputFolder) {

    const themeFolder = path.resolve(themesFolder, themeName);
    if(fs.existsSync(themeFolder)) {
      logger.debug("Found theme ", themeName, " in folder ", themeFolder);

      copyThemeResources(themeName, themeFolder, projectStaticAssetsOutputFolder);

      const themeFile = generateThemeFile(
        themeFolder,
        themeName
      );

      fs.writeFileSync(path.resolve(themeFolder, themeName + '.js'), themeFile);
    }
};
