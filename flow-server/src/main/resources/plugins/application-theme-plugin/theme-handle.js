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
 * This file contains functions for look up and handle the theme resources
 * for application theme plugin.
 */
const fs = require('fs');
const path = require('path');
const generateThemeFile = require('./theme-generator');
const { copyStaticAssets } = require('./theme-copy');

// matches theme folder name in 'themes/my-theme/my-theme.generated.js'
const nameRegex = /themes\/(.*)\/\1.generated.js/;

/**
 * Looks up for a theme resources in a current project and in jar dependencies,
 * copies the found resources and generates/updates meta data for webpack
 * compilation.
 */
function generateTheme(options, logger) {
  const generatedThemeFile = getResolvedGeneratedThemeFile(options);
  if (fs.existsSync(generatedThemeFile)) {
    const themeName = extractThemeName(generatedThemeFile);

    if (!themeName) {
      throw new Error("Couldn't parse theme name from '" + generatedThemeFile + "'.");
    }

    let themeFound = false;
    for (let i = 0; i < options.themeProjectFolders.length; i++) {
      const themeProjectFolder = options.themeProjectFolders[i];
      if (fs.existsSync(themeProjectFolder)) {
        logger.info("Searching themes folder ", themeProjectFolder, " for theme ", themeName);
        const handled = handleThemes(themeName, themeProjectFolder, options.projectStaticAssetsOutputFolder, logger);
        if (handled) {
          if (themeFound) {
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
      handleThemes(themeName, options.themeResourceFolder, options.projectStaticAssetsOutputFolder, logger);
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
 * @param {object} logger plugin logger instance
 *
 * @returns true if theme was found else false.
 */
function handleThemes(themeName, themesFolder, projectStaticAssetsOutputFolder, logger) {
  const themeFolder = path.resolve(themesFolder, themeName);
  if (fs.existsSync(themeFolder)) {
    logger.debug("Found theme ", themeName, " in folder ", themeFolder);

    const themeProperties = getThemeProperties(themeFolder);

    copyStaticAssets(themeName, themeProperties, projectStaticAssetsOutputFolder, logger);

    const themeFile = generateThemeFile(themeFolder, themeName, themeProperties);

    fs.writeFileSync(path.resolve(themeFolder, getThemeGeneratedFileName(themeName)), themeFile);
    return true;
  }
  return false;
}

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
}

/**
 * Generates path to theme generated file with theme meta data
 * @param options object containing theme resource folder
 * @returns path to theme generated file
 */
function getResolvedGeneratedThemeFile(options) {
  return path.resolve(options.themeResourceFolder, "theme-generated.js");
}

/**
 * Extracts current theme name from theme generated file
 * @param generatedThemeFile file to extract from
 * @returns {string} current theme name
 */
function extractThemeName(generatedThemeFile) {
  // read theme name from the theme-generated.js as there we always mark the used theme for webpack to handle.
  return nameRegex.exec(fs.readFileSync(generatedThemeFile, {encoding: 'utf8'}))[1];
}

/**
 * Generates theme resources meta data file name
 * @param themeName current theme name
 * @returns {string} theme resources meta data file name
 */
function getThemeGeneratedFileName(themeName) {
  return themeName + '.generated.js';
}

module.exports = { generateTheme, getResolvedGeneratedThemeFile, extractThemeName };
