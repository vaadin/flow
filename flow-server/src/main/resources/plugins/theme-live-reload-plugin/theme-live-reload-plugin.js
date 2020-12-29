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
 * This plugin handles adding/deleting theme resources events and triggers
 * theme meta data re-generation and application theme update on the fly.
 */
class ThemeLiveReloadPlugin {

    /**
     * Create a new instance of ThemeLiveReloadPlugin
     * @param themeName current theme name
     * @param processThemeResourcesCallback callback which is called on
     * adding/deleting of theme resource files to re-generate theme meta
     * data and apply theme changes to application.
     */
    constructor(themeName, processThemeResourcesCallback) {
      if (!themeName) {
        throw new Error("Missing theme name");
      }
      this.themeName = themeName;
      if (!processThemeResourcesCallback || typeof processThemeResourcesCallback !== 'function') {
        throw new Error("Couldn't instantiate a ThemeLiveReloadPlugin" +
          " instance, because theme resources process callback is not set" +
          " and, thus, no information provided what to do upon" +
          " adding/deleting theme resource files. Please provide this" +
          " callback as a ThemeLiveReloadPlugin constructor parameter.");
      }
      this.processThemeResourcesCallback = processThemeResourcesCallback;
    }

    apply(compiler) {
      // Adds a hook for theme files change event
      compiler.hooks.watchRun.tapAsync("ThemeLiveReloadPlugin", (compilation, callback) => {
        const logger = compiler.getInfrastructureLogger("ThemeLiveReloadPlugin");
        const changedFilesMap = compiler.watchFileSystem.watcher.mtimes;
        if (changedFilesMap !== {}) {
          let themeGeneratedFileChanged = false;
          const changedFilesPaths = Object.keys(changedFilesMap);
          logger.debug("Detected changes in the following files " + changedFilesPaths);
          changedFilesPaths.map(file => `${file}`).forEach(file => {
              if (file.indexOf(this.themeName + '.generated.js') > -1) {
                themeGeneratedFileChanged = true;
              }
            });
          if (!themeGeneratedFileChanged) {
            this.processThemeResourcesCallback(logger);
          }
        }
        callback();
      });
    }
}

module.exports = ThemeLiveReloadPlugin;
