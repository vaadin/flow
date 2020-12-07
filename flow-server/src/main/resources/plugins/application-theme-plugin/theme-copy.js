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
 * This contains functions and features used to copy theme files.
 */

const fs = require('fs');
const path = require('path');
const glob = require('glob');

/**
 * Copy any static node_modules assets marked in theme.json to
 * project static assets folder.
 *
 * The theme.json content for assets is set up as:
 * {
 *   assets: {
 *     "node_module identifier": {
 *       "copy-rule": "target/folder",
 *     }
 *   }
 * }
 *
 * This would mean that an asset would be built as:
 * "@fortawesome/fontawesome-free": {
 *   "svgs/regular/**": "fortawesome/icons"
 * }
 * Where '@fortawesome/fontawesome-free' is the npm package, 'svgs/regular/**' is what should be copied
 * and 'fortawesome/icons' is the target directory under projectStaticAssetsOutputFolder where things
 * will get copied to.
 *
 * Note! there can be multiple copy-rules with target folders for one npm package asset.
 *
 * @param {string} themeName name of the theme we are copying assets for
 * @param {json} themeProperties theme properties json with data on assets
 * @param {string} projectStaticAssetsOutputFolder project output folder where we copy assets to under theme/[themeName]
 * @param {logger} theme plugin logger
 */
function copyStaticAssets(themeName, themeProperties, projectStaticAssetsOutputFolder, logger) {

  const assets = themeProperties['assets'];
  if (!assets) {
    logger.log("no assets to handle no static assets were copied");
    return;
  }

  fs.mkdirSync(projectStaticAssetsOutputFolder, {
    recursive: true
  });
  Object.keys(assets).forEach((module) => {

    const copyRules = assets[module];
    Object.keys(copyRules).forEach((copyRule) => {
      const nodeSources = path.resolve('node_modules/', module, copyRule);
      const files = glob.sync(nodeSources, { nodir: true });
      const targetFolder = path.resolve(projectStaticAssetsOutputFolder, "theme", themeName, copyRules[copyRule]);

      fs.mkdirSync(targetFolder, {
        recursive: true
      });
      files.forEach((file) => {
        logger.trace("Copying: ", file, '=>', targetFolder);
        fs.copyFileSync(file, path.resolve(targetFolder, path.basename(file)));
      });
    });
  });
};

module.exports = copyStaticAssets;
