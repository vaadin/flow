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

class ApplicationThemePlugin {
  constructor(options) {
    this.options = options;
  }

  apply(compiler) {
    logger = compiler.getInfrastructureLogger("application-theme-plugin");

    compiler.hooks.afterEnvironment.tap("FlowApplicationThemePlugin", () => {
      if (fs.existsSync(this.options.themeJarFolder)) {
        logger.debug("Found themeFolder to handle ", this.options.themeJarFolder);
        handleThemes(this.options.themeJarFolder, this.options.projectStaticAssetsOutputFolder);
      } else {
        logger.warn('Theme JAR folder not found from ', this.options.themeJarFolder);
      }

      this.options.themeProjectFolders.forEach((themeProjectFolder) => {
        if (fs.existsSync(themeProjectFolder)) {
          logger.debug("Found themeFolder to handle ", themeProjectFolder);
          handleThemes(themeProjectFolder, this.options.projectStaticAssetsOutputFolder);
        }
      });
    });
  }
}

module.exports = ApplicationThemePlugin;

function handleThemes(themesFolder, projectStaticAssetsOutputFolder) {
  const dir = fs.opendirSync(themesFolder);
  try {
    let dirent;
    while ((dirent = dir.readSync())) {
      if (!dirent.isDirectory()) {
        dirent.closeSync();
        continue;
      }
      const themeName = dirent.name;
      const themeFolder = path.resolve(themesFolder, themeName);
      logger.debug("Found theme ", themeName, " in folder ", themeFolder);

      copyThemeResources(themeName, themeFolder, projectStaticAssetsOutputFolder);

      const themeFile = generateThemeFile(
        themeFolder,
        themeName
      );

      fs.writeFileSync(path.resolve(themeFolder, themeName + '.js'), themeFile);
      dirent.closeSync();
    }
  } finally {
    dir.closeSync()
  }
};
