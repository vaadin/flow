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
const { copyStaticAssets } = require('./theme-copy');

// matches theme folder name in 'themes/my-theme/my-theme.generated.js'
const nameRegex = /themes\/(.*)\/\1.generated.js/;

let logger;
let themeName;
let themeGeneratedFileName;

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

    compiler.hooks.afterEnvironment.tap("ApplicationThemePlugin", () => generateTheme(this.options));

    // Adds the active application theme folder for webpack watching
    compiler.plugin("after-compile", function (compilation, callback) {
      if (themeName) {
        compilation.contextDependencies.add('frontend/themes/' + themeName);
      }
      callback();
    });

    // Adds a hook for theme files change event
    compiler.plugin("watch-run", (compilation, callback) => {
      const changedFilesMap = compiler.watchFileSystem.watcher.mtimes;
      if (changedFilesMap !== {}) {
        let themeGeneratedFileChanged = false;
        const changedFiles = Object.keys(changedFilesMap)
            .map(file => `${file}`)
            .forEach(file => {
              if (file.indexOf(themeGeneratedFileName) > -1) {
                themeGeneratedFileChanged = true;
              }
            });
        logger.debug("Detected changes in the following files " + changedFiles);
        if (!themeGeneratedFileChanged) {
          generateTheme(this.options);
        }
      }
      callback();
    });
  }
}

module.exports = ApplicationThemePlugin;

/**
 * Looks up for a theme in a current project and in jar dependencies, copies
 * static resources of the found theme and generates/writes the
 * [theme-name].generated.js for webpack to handle.
 */
function generateTheme(options) {
  const generatedThemeFile = path.resolve(options.themeResourceFolder, "theme-generated.js");
  if (fs.existsSync(generatedThemeFile)) {
    // read theme name from the theme-generated.js as there we always mark the used theme for webpack to handle.
    themeName = nameRegex.exec(fs.readFileSync(generatedThemeFile, {encoding: 'utf8'}))[1];
    themeGeneratedFileName = themeName + '.generated.js';

    if (!themeName) {
      throw new Error("Couldn't parse theme name from '" + generatedThemeFile + "'.");
    }

    let themeFound = false;
    for (let i = 0; i < options.themeProjectFolders.length; i++) {
      const themeProjectFolder = options.themeProjectFolders[i];
      if (fs.existsSync(themeProjectFolder)) {
        logger.info("Searching themes folder ", themeProjectFolder, " for theme ", themeName);
        const handled = handleThemes(themeName, themeProjectFolder, options.projectStaticAssetsOutputFolder);
        if (handled) {
          if(themeFound) {
            throw new Error("Found theme files in '" + themeProjectFolder + "' and '"
              + themeFound + "'. Theme should only be available in one folder");
          }
          logger.info("Found theme files from '", themeProjectFolder, "'");
          themeFound = themeProjectFolder;
        }
      }
    }

    if (fs.existsSync(options.themeResourceFolder)) {
      if (themeFound && fs.existsSync(path.resolve(options.themeResourceFolder, themeName))) {
        throw new Error("Theme '" + themeName + "'should not exist inside a jar and in the project at the same time\n" +
          "Extending another theme is possible by adding { \"parent\": \"my-parent-theme\" } entry to the theme.json file inside your theme folder.");
      }
      logger.debug("Searching theme jar resource folder ", options.themeResourceFolder, " for theme ", themeName);
      handleThemes(themeName, options.themeResourceFolder, options.projectStaticAssetsOutputFolder);
    }
  } else {
    logger.debug("Skipping Vaadin application theme handling.");
    logger.trace("Most likely no @Theme annotation for application or only themeClass used.");
  }
}

/**
 * Copies static resources for theme and generates/writes the
 * [theme-name].generated.js for webpack to handle.
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

    copyStaticAssets(themeName, themeProperties, projectStaticAssetsOutputFolder, logger);

    const themeFile = generateThemeFile(themeFolder, themeName, themeProperties);

    fs.writeFileSync(path.resolve(themeFolder, themeGeneratedFileName), themeFile);
    return true;
  }
  return false;
};

function getThemeProperties(themeFolder) {
  const themePropertyFile = path.resolve(themeFolder, 'theme.json');
  if (!fs.existsSync(themePropertyFile)) {
    return {};
  }
  const themePropertyFileAsString = fs.readFileSync(themePropertyFile);
  if (themePropertyFileAsString.length === 0) {
    return {};
  }
  return JSON.parse(themePropertyFileAsString);
};
