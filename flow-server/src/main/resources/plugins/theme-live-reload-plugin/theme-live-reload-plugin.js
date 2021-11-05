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
   * @param processThemeResourcesCallback callback which is called on
   * adding/deleting of theme resource files to re-generate theme meta
   * data and apply theme changes to application.
   */
  constructor(processThemeResourcesCallback) {
    if (!processThemeResourcesCallback || typeof processThemeResourcesCallback !== 'function') {
      throw new Error(
        "Couldn't instantiate a ThemeLiveReloadPlugin" +
          ' instance, because theme resources process callback is not set' +
          ' and, thus, no information provided what to do upon' +
          ' adding/deleting theme resource files. Please provide this' +
          ' callback as a ThemeLiveReloadPlugin constructor parameter.'
      );
    }
    this.processThemeResourcesCallback = processThemeResourcesCallback;
    // Component style sheet might be deleted from parent theme folder, so
    // the regexp does not contain the exact theme name
    this.componentStyleFileRegexp = /(\\|\/)themes\1([\s\S]*)\1components\1(.*)\.css$/;
    // There might be several theme generated files in the generated
    // folder, so the regexp does not contain the exact theme name
    this.themeGeneratedFileRegexp = /theme-[\s\S]*?\.generated\.js$/;
  }

  apply(compiler) {
    // Adds a hook for theme files change event
    compiler.hooks.watchRun.tapAsync('ThemeLiveReloadPlugin', (compilation, callback) => {
      const logger = compiler.getInfrastructureLogger('ThemeLiveReloadPlugin');
      const changedFilesMap = compiler.watchFileSystem.watcher.mtimes;
      if (changedFilesMap !== {}) {
        let themeName = undefined;
        let themeGeneratedFileChanged = false;
        let themeGeneratedFileDeleted = false;
        let deletedComponentStyleFile = undefined;
        const changedFilesPaths = Object.keys(changedFilesMap);
        logger.debug('Detected changes in the following files ' + changedFilesPaths);
        changedFilesPaths.forEach((changedFilePath) => {
          const file = `${changedFilePath}`;
          const themeGeneratedFileChangedNow = file.match(this.themeGeneratedFileRegexp);
          const timestamp = changedFilesMap[changedFilePath];
          // null or negative timestamp means file delete
          const fileRemoved = timestamp === null || timestamp < 0;

          if (themeGeneratedFileChangedNow) {
            themeGeneratedFileChanged = true;
            if (fileRemoved) {
              themeGeneratedFileDeleted = true;
            }
          } else if (fileRemoved) {
            const matchResult = file.match(this.componentStyleFileRegexp);
            if (matchResult) {
              themeName = matchResult[2];
              deletedComponentStyleFile = file;
            }
          }
        });
        // This is considered as a workaround for
        // https://github.com/vaadin/flow/issues/9948: delete component
        // styles and theme generated file in one run to not have webpack
        // compile error
        if (deletedComponentStyleFile && !themeGeneratedFileDeleted) {
          logger.warn(
            "Custom theme component style sheet '" +
              deletedComponentStyleFile +
              "' has been deleted.\n\n" +
              "You should also delete './frontend/generated/theme-" +
              themeName +
              ".generated.js' (simultaneously) with the component stylesheet'.\n" +
              "Otherwise it will cause a webpack compilation error 'no such file or directory', as component style sheets are referenced from " +
              "'./frontend/generated/theme-" +
              themeName +
              ".generated.js'.\n\n" +
              "If you encounter a 'no such file or directory' error in your application, just click on the overlay (or refresh the browser page), and it should disappear.\n\n" +
              'It should then be possible to continue working on the application and theming.\n' +
              "If it doesn't help, you need to restart the application."
          );
        }

        // Webpack watches to the changes in theme-[my-theme].generated.js
        // because it is referenced from theme.js. Changes in this file
        // should not trigger the theme handling callback (which
        // re-generates theme-[my-theme].generated.js),
        // otherwise it will get into infinite re-compilation loop.
        if (themeGeneratedFileDeleted || !themeGeneratedFileChanged) {
          this.processThemeResourcesCallback(logger);
        }
      }
      callback();
    });
  }
}

module.exports = ThemeLiveReloadPlugin;
