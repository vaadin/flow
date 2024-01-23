/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import { existsSync, readFileSync } from 'fs';
import { resolve } from 'path';
import { writeThemeFiles } from './theme-generator.js';
import { copyStaticAssets, copyThemeResources } from './theme-copy.js';

// matches theme name in './theme-my-theme.generated.js'
const nameRegex = /theme-(.*)\.generated\.js/;

let prevThemeName = undefined;
let firstThemeName = undefined;

/**
 * Looks up for a theme resources in a current project and in jar dependencies,
 * copies the found resources and generates/updates meta data for webpack
 * compilation.
 *
 * @param {object} options application theme plugin mandatory options,
 * @see {@link ApplicationThemePlugin}
 *
 * @param logger application theme plugin logger
 */
function processThemeResources(options, logger) {
  const themeName = extractThemeName(options.frontendGeneratedFolder);
  if (themeName) {
    if (!prevThemeName && !firstThemeName) {
      firstThemeName = themeName;
    } else if (
      (prevThemeName && prevThemeName !== themeName && firstThemeName !== themeName) ||
      (!prevThemeName && firstThemeName !== themeName)
    ) {
      // Warning message is shown to the developer when:
      // 1. He is switching to any theme, which is differ from one being set up
      // on application startup, by changing theme name in `@Theme()`
      // 2. He removes or comments out `@Theme()` to see how the app
      // looks like without theming, and then again brings `@Theme()` back
      // with a themeName which is differ from one being set up on application
      // startup.
      const warning = `Attention: Active theme is switched to '${themeName}'.`;
      const description = `
      Note that adding new style sheet files to '/themes/${themeName}/components', 
      may not be taken into effect until the next application restart.
      Changes to already existing style sheet files are being reloaded as before.`;
      logger.warn('*******************************************************************');
      logger.warn(warning);
      logger.warn(description);
      logger.warn('*******************************************************************');
    }
    prevThemeName = themeName;

    findThemeFolderAndHandleTheme(themeName, options, logger);
  } else {
    // This is needed in the situation that the user decides to comment or
    // remove the @Theme(...) completely to see how the application looks
    // without any theme. Then when the user brings back one of the themes,
    // the previous theme should be undefined to enable us to detect the change.
    prevThemeName = undefined;
    logger.debug('Skipping Vaadin application theme handling.');
    logger.trace('Most likely no @Theme annotation for application or only themeClass used.');
  }
}

/**
 * Search for the given theme in the project and resource folders.
 *
 * @param {string} themeName name of theme to find
 * @param {object} options application theme plugin mandatory options,
 * @see {@link ApplicationThemePlugin}
 * @param logger application theme plugin logger
 * @return true or false for if theme was found
 */
function findThemeFolderAndHandleTheme(themeName, options, logger) {
  let themeFound = false;
  for (let i = 0; i < options.themeProjectFolders.length; i++) {
    const themeProjectFolder = options.themeProjectFolders[i];
    if (existsSync(themeProjectFolder)) {
      logger.debug("Searching themes folder '" + themeProjectFolder + "' for theme '" + themeName + "'");
      const handled = handleThemes(themeName, themeProjectFolder, options, logger);
      if (handled) {
        if (themeFound) {
          throw new Error(
            "Found theme files in '" +
              themeProjectFolder +
              "' and '" +
              themeFound +
              "'. Theme should only be available in one folder"
          );
        }
        logger.debug("Found theme files from '" + themeProjectFolder + "'");
        themeFound = themeProjectFolder;
      }
    }
  }

  if (existsSync(options.themeResourceFolder)) {
    if (themeFound && existsSync(resolve(options.themeResourceFolder, themeName))) {
      throw new Error(
        "Theme '" +
          themeName +
          "'should not exist inside a jar and in the project at the same time\n" +
          'Extending another theme is possible by adding { "parent": "my-parent-theme" } entry to the theme.json file inside your theme folder.'
      );
    }
    logger.debug(
      "Searching theme jar resource folder '" + options.themeResourceFolder + "' for theme '" + themeName + "'"
    );
    handleThemes(themeName, options.themeResourceFolder, options, logger);
    themeFound = true;
  }
  return themeFound;
}

/**
 * Copies static resources for theme and generates/writes the
 * [theme-name].generated.js for webpack to handle.
 *
 * Note! If a parent theme is defined it will also be handled here so that the parent theme generated file is
 * generated in advance of the theme generated file.
 *
 * @param {string} themeName name of theme to handle
 * @param {string} themesFolder folder containing application theme folders
 * @param {object} options application theme plugin mandatory options,
 * @see {@link ApplicationThemePlugin}
 * @param {object} logger plugin logger instance
 *
 * @throws Error if parent theme defined, but can't locate parent theme
 *
 * @returns true if theme was found else false.
 */
function handleThemes(themeName, themesFolder, options, logger) {
  const themeFolder = resolve(themesFolder, themeName);
  if (existsSync(themeFolder)) {
    logger.debug('Found theme ', themeName, ' in folder ', themeFolder);

    const themeProperties = getThemeProperties(themeFolder);

    // If theme has parent handle parent theme immediately.
    if (themeProperties.parent) {
      const found = findThemeFolderAndHandleTheme(themeProperties.parent, options, logger);
      if (!found) {
        throw new Error(
          "Could not locate files for defined parent theme '" +
            themeProperties.parent +
            "'.\n" +
            'Please verify that dependency is added or theme folder exists.'
        );
      }
    }
    copyStaticAssets(themeName, themeProperties, options.projectStaticAssetsOutputFolder, logger);
    copyThemeResources(themeFolder, options.projectStaticAssetsOutputFolder, logger);

    writeThemeFiles(themeFolder, themeName, themeProperties, options);
    return true;
  }
  return false;
}

function getThemeProperties(themeFolder) {
  const themePropertyFile = resolve(themeFolder, 'theme.json');
  if (!existsSync(themePropertyFile)) {
    return {};
  }
  const themePropertyFileAsString = readFileSync(themePropertyFile);
  if (themePropertyFileAsString.length === 0) {
    return {};
  }
  return JSON.parse(themePropertyFileAsString);
}

/**
 * Extracts current theme name from auto-generated 'theme.js' file located on a
 * given folder.
 * @param frontendGeneratedFolder folder in project containing 'theme.js' file
 * @returns {string} current theme name
 */
function extractThemeName(frontendGeneratedFolder) {
  if (!frontendGeneratedFolder) {
    throw new Error(
      "Couldn't extract theme name from 'theme.js'," +
        ' because the path to folder containing this file is empty. Please set' +
        ' the a correct folder path in ApplicationThemePlugin constructor' +
        ' parameters.'
    );
  }
  const generatedThemeFile = resolve(frontendGeneratedFolder, 'theme.js');
  if (existsSync(generatedThemeFile)) {
    // read theme name from the 'generated/theme.js' as there we always
    // mark the used theme for webpack to handle.
    const themeName = nameRegex.exec(readFileSync(generatedThemeFile, { encoding: 'utf8' }))[1];
    if (!themeName) {
      throw new Error("Couldn't parse theme name from '" + generatedThemeFile + "'.");
    }
    return themeName;
  } else {
    return '';
  }
}

/**
 * Finds all the parent themes located in the project themes folders and in
 * the JAR dependencies with respect to the given custom theme with
 * {@code themeName}.
 * @param {string} themeName given custom theme name to look parents for
 * @param {object} options application theme plugin mandatory options,
 * @see {@link ApplicationThemePlugin}
 * @returns {string[]} array of paths to found parent themes with respect to the
 * given custom theme
 */
function findParentThemes(themeName, options) {
  const existingThemeFolders = [options.themeResourceFolder, ...options.themeProjectFolders].filter((folder) =>
    existsSync(folder)
  );
  return collectParentThemes(themeName, existingThemeFolders, false);
}

function collectParentThemes(themeName, themeFolders, isParent) {
  let foundParentThemes = [];
  themeFolders.forEach((folder) => {
    const themeFolder = resolve(folder, themeName);
    if (existsSync(themeFolder)) {
      const themeProperties = getThemeProperties(themeFolder);

      if (themeProperties.parent) {
        foundParentThemes.push(...collectParentThemes(themeProperties.parent, themeFolders, true));
        if (!foundParentThemes.length) {
          throw new Error(
            "Could not locate files for defined parent theme '" +
              themeProperties.parent +
              "'.\n" +
              'Please verify that dependency is added or theme folder exists.'
          );
        }
      }
      // Add a theme path to result collection only if a given themeName
      // is supposed to be a parent theme
      if (isParent) {
        foundParentThemes.push(themeFolder);
      }
    }
  });
  return foundParentThemes;
}

export { processThemeResources, extractThemeName, findParentThemes };
