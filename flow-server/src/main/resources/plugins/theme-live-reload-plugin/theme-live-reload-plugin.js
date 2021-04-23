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
      this.componentStyleFileRegexp = new RegExp('(\\\\|\/)themes(\\\\|\/)' +
        this.getEscapedThemeName() + '(\\\\|\/)components(\\\\|\/)(.*)\\.css$');
      this.themeGeneratedFileRegexp = /theme-[\s\S]*?\.generated\.js$/;
    }

    apply(compiler) {
      // Adds a hook for theme files change event
      compiler.hooks.watchRun.tapAsync("ThemeLiveReloadPlugin", (compilation, callback) => {
        const logger = compiler.getInfrastructureLogger("ThemeLiveReloadPlugin");
        const changedFilesMap = compiler.watchFileSystem.watcher.mtimes;
        if (changedFilesMap !== {}) {
          let themeGeneratedFileChanged = false;
          let themeGeneratedFileDeleted = false;
          let componentStyleFileDeleted = false;
          const changedFilesPaths = Object.keys(changedFilesMap);
          logger.debug("Detected changes in the following files " + changedFilesPaths);
          changedFilesPaths.forEach(changedFilePath => {
            const file = `${changedFilePath}`;
            // Webpack watches to the changes in theme-[my-theme].generated.js
            // because it is referenced from theme.js. Changes in this file
            // should not trigger the theme handling callback (which
            // re-generates theme-[my-theme].generated.js),
            // otherwise it will get into infinite re-compilation loop.
            // There might be several theme generated files in the
            // generated folder, so the condition does not contain the exact
            // theme name
            const themeGeneratedFileChangedNow = file.match(this.themeGeneratedFileRegexp);
            if (!themeGeneratedFileChanged && themeGeneratedFileChangedNow) {
              themeGeneratedFileChanged = true;
            }

            const timestamp = changedFilesMap[changedFilePath];
            // null or negative timestamp means file delete
            if (timestamp === null || timestamp < 0) {
              if (themeGeneratedFileChangedNow) {
                themeGeneratedFileDeleted = true;
              }
              componentStyleFileDeleted = file.match(this.componentStyleFileRegexp);
            }
          });
          // This is considered as a workaround for
          // https://github.com/vaadin/flow/issues/9948: delete component
          // styles and theme generated file in one run to not have webpack
          // compile error
          if (componentStyleFileDeleted && !themeGeneratedFileDeleted) {
            logger.warn("Custom theme component stylesheet file has been " +
              "deleted, but it is still referenced in 'generated/theme-" + this.themeName + ".generated.js' file.\n" +
              "This causes webpack compilation error.\n" +
              "Please refresh your app page in the browser, or, if it doesn't help, please restart your app.\n" +
              "In your further work, please delete 'generated/theme-" + this.themeName + ".generated.js' " +
              "in one run (simultaneously) with component stylesheet file.");
          }

          if (themeGeneratedFileDeleted || !themeGeneratedFileChanged) {
            this.processThemeResourcesCallback(logger);
          }
        }
        callback();
      });
    }

  getEscapedThemeName() {
    return this.themeName.replace(/[-[\]{}()*+!<=:?.\/\\^$|#\s,]/g, '\\$&');
  }
}

module.exports = ThemeLiveReloadPlugin;
