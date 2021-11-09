const loaderUtils = require('loader-utils');
const fs = require('fs');
const path = require('path');

// Collect groups [url(] [ |'|"]optional './|../', file part and end of url
const urlMatcher = /(url\()(\'|\")?(\.\/|\.\.\/)(\S*)(\2\))/g;

/**
 * This custom loader handles rewriting urls for the application theme css files.
 * URLs starting with ./ or ../ are checked against the filesystem and converted if a file exists.
 * URLs going outside of the application theme folder are not accepted and will not be rewritten.
 *
 * @param source file contents to handle
 * @param map source map for file
 */
module.exports = function (source, map) {
  const options = loaderUtils.getOptions(this);
  const handledResourceFolder = path.dirname(this._module.resource);
  const logger = this.getLogger('theme-loader');

  let themeFolder = handledResourceFolder;
  // Recurse up until we find the themes folder or don't have 'themes' on the path.
  while (themeFolder.indexOf('themes') > 1 && path.basename(path.resolve(themeFolder, '..')) !== 'themes') {
    themeFolder = path.resolve(themeFolder, '..');
  }
  // If we have found no themes folder return without doing anything.
  if (path.basename(path.resolve(themeFolder, '..')) !== 'themes') {
    this.callback(null, source, map);
    return;
  }

  logger.log("Using '", themeFolder, "' for the application theme base folder.");

  source = source.replace(urlMatcher, function (match, url, quoteMark, replace, fileUrl, endString) {
    let absolutePath = path.resolve(handledResourceFolder, replace, fileUrl);
    if (
      (fs.existsSync(absolutePath) && absolutePath.startsWith(themeFolder)) ||
      assetsContains(fileUrl, themeFolder, logger)
    ) {
      // Adding ./ will skip css-loader, which should be done for asset files
      const skipLoader = fs.existsSync(absolutePath) && absolutePath.startsWith(themeFolder) ? '' : './';
      const frontendThemeFolder = skipLoader + 'themes/' + path.basename(themeFolder);
      logger.debug(
        'Updating url for file',
        "'" + replace + fileUrl + "'",
        'to use',
        "'" + frontendThemeFolder + '/' + fileUrl + "'"
      );
      const pathResolved = absolutePath.substring(themeFolder.length).replace(/\\/g, '/');

      // keep the url the same except replace the ./ or ../ to themes/[themeFolder]
      if (quoteMark) {
        return url + quoteMark + frontendThemeFolder + pathResolved + endString;
      }
      return url + frontendThemeFolder + pathResolved + endString;
    } else if (options.devMode) {
      logger.log("No rewrite for '", match, "' as the file was not found.");
    }
    return match;
  });

  this.callback(null, source, map);
};

function assetsContains(fileUrl, themeFolder, logger) {
  const themeProperties = getThemeProperties(themeFolder);
  if (!themeProperties) {
    logger.debug('No theme properties found.');
    return false;
  }
  const assets = themeProperties['assets'];
  if (!assets) {
    logger.debug('No defined assets in theme properties');
    return false;
  }
  // Go through each asset module
  for (let module of Object.keys(assets)) {
    const copyRules = assets[module];
    // Go through each copy rule
    for (let copyRule of Object.keys(copyRules)) {
      // if file starts with copyRule target check if file with path after copy target can be found
      if (fileUrl.startsWith(copyRules[copyRule])) {
        const targetFile = fileUrl.replace(copyRules[copyRule], '');
        const files = require('glob').sync(path.resolve('node_modules/', module, copyRule), { nodir: true });

        for (let file of files) {
          if (file.endsWith(targetFile)) return true;
        }
      }
    }
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
