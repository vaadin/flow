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

let logger;

/**
 * This plugin handles adding/deleting theme resources events and triggers
 * theme meta data re-generation and application theme update on the fly.
 */
class ThemeLiveReloadPlugin {

    /**
     * Create a new instance of ThemeLiveReloadPlugin
     * @param themeNameGetter getter function for current theme name
     * @param generateThemeCallbackGetter getter function for generate theme
     * callback
     */
    constructor(themeNameGetter, generateThemeCallbackGetter) {
      if (!themeNameGetter) {
        throw new Error("Missing theme name");
      }
      this.themeNameGetter = themeNameGetter;
      this.generateThemeCallbackGetter = generateThemeCallbackGetter;
    }

    apply(compiler) {
      // Adds a hook for theme files change event
      compiler.plugin("watch-run", (compilation, callback) => {
        logger = compiler.getInfrastructureLogger("ThemeLiveReloadPlugin");
        const changedFilesMap = compiler.watchFileSystem.watcher.mtimes;
        if (changedFilesMap !== {}) {
          let themeGeneratedFileChanged = false;
          const changedFilesPaths = Object.keys(changedFilesMap);
          logger.debug("Detected changes in the following files " + changedFilesPaths);
          changedFilesPaths.map(file => `${file}`).forEach(file => {
              if (file.indexOf(this.themeNameGetter() + '.generated.js') > -1) {
                themeGeneratedFileChanged = true;
              }
            });
          if (!themeGeneratedFileChanged) {
            this.generateThemeCallbackGetter()(logger);
          }
        }
        callback();
      });
    }
}

module.exports = ThemeLiveReloadPlugin;