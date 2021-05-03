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

const { processThemeResources, extractThemeName, findParentThemes } = require('./theme-handle');

/**
 * The application theme plugin is for generating, collecting and copying of theme files for the application theme.
 *
 * The plugin should be supplied with the paths for
 *
 *  themeResourceFolder             - theme folder where flow copies local and jar resource frontend files
 *  themeProjectFolders             - array of possible locations for theme folders inside the project
 *  projectStaticAssetsOutputFolder - path to where static assets should be put
 *
 *  @throws Error in constructor if required option is not received
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
    const logger = compiler.getInfrastructureLogger("ApplicationThemePlugin");

    compiler.hooks.afterEnvironment.tap("ApplicationThemePlugin",
        () => processThemeResources(this.options, logger));
  }

}

module.exports = { ApplicationThemePlugin, processThemeResources, extractThemeName, findParentThemes };

