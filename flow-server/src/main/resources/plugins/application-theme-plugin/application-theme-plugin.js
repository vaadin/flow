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
const { copyThemeResources, copyStaticAssets } = require('./theme-copy');

let logger;

// matches theme folder name in 'theme/my-theme/my-theme.js'
const nameRegex = /theme\/(.*)\/\1.js/g;

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
      throw new Error("Missing themeResourceFolder path");
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
        const themeName = nameRegex.exec(fs.readFileSync(generatedThemeFile, {encoding: 'utf8'}))[1];
        if (!themeName) {
          throw new Error("Couldn't parse theme name from '" + generatedThemeFile + "'.");
        }

        let themeFound = false;
        for (let i = 0; i<this.options.themeProjectFolders.length; i++) {
          const themeProjectFolder = this.options.themeProjectFolders[i];
          if (fs.existsSync(themeProjectFolder)) {
            logger.info("Searching theme folder ", themeProjectFolder, " for theme ", themeName);
            const handled = handleThemes(themeName, themeProjectFolder, this.options.projectStaticAssetsOutputFolder);
            if (handled) {
              if(themeFound) {
                throw new Error("Found theme filed in '" + themeProjectFolder + "' and '"
                  + themeFound + "'. Theme should only be available in one folder");
              }
              logger.info("Found theme files from '", themeProjectFolder, "'");
              themeFound = themeProjectFolder;
            }
          }
        }

        if (fs.existsSync(this.options.themeResourceFolder)) {
          if (themeFound && fs.existsSync(path.resolve(this.options.themeResourceFolder, themeName))) {
            throw new Error("Theme '" + themeName + "'should not exist inside a jar and in the project at the same time\n" +
              "Extending another theme is possible by adding { \"parent\": \"my-parent-theme\" } entry to the theme.json file inside your theme folder.");
          }
          logger.debug("Searching theme jar resource folder ", this.options.themeResourceFolder, " for theme ", themeName);
          handleThemes(themeName, this.options.themeResourceFolder, this.options.projectStaticAssetsOutputFolder);
        }
      } else {
        logger.debug("Skipping Vaadin application theme handling.");
        logger.trace("Most likely no @Theme annotation for application or only themeClass used.");
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
 *
 * @returns true if theme was found else false.
 */
function handleThemes(themeName, themesFolder, projectStaticAssetsOutputFolder) {
  const themeFolder = path.resolve(themesFolder, themeName);
  if (fs.existsSync(themeFolder)) {
    logger.debug("Found theme ", themeName, " in folder ", themeFolder);

    const themeProperties = getThemeProperties(themeFolder);

    copyThemeResources(themeFolder, projectStaticAssetsOutputFolder);
    copyStaticAssets(themeProperties, projectStaticAssetsOutputFolder, logger);

    const themeFile = generateThemeFile(themeFolder, themeName, themeProperties);

    fs.writeFileSync(path.resolve(themeFolder, themeName + '.js'), themeFile);
    return true;
  }
  return false;
};

function getThemeProperties(themeFolder) {
  const themePropertyFile = path.resolve(themeFolder, 'theme.json');
  if (!fs.existsSync(themePropertyFile)) {
    return {};
  }
  return JSON.parse(fs.readFileSync(themePropertyFile));
};
