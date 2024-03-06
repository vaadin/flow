/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 *  frontendGeneratedFolder         - the path to where frontend auto-generated files are put
 *
 *  @throws Error in constructor if required option is not received
 */
class ApplicationThemePlugin {
  constructor(options) {
    this.options = options;

    if (!this.options.themeResourceFolder) {
      throw new Error('Missing themeResourceFolder path');
    }
    if (!this.options.projectStaticAssetsOutputFolder) {
      throw new Error('Missing projectStaticAssetsOutputFolder path');
    }
    if (!this.options.themeProjectFolders) {
      throw new Error('Missing themeProjectFolders path array');
    }
    if (!this.options.frontendGeneratedFolder) {
      throw new Error('Missing frontendGeneratedFolder path');
    }
  }

  apply(compiler) {
    const logger = compiler.getInfrastructureLogger('ApplicationThemePlugin');

    compiler.hooks.afterEnvironment.tap('ApplicationThemePlugin', () => processThemeResources(this.options, logger));
  }
}

module.exports = { ApplicationThemePlugin, processThemeResources, extractThemeName, findParentThemes };
