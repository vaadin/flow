const fs = require('fs');
const path = require('path');
const glob = require('glob');

// Collect groups [url(] ['|"]optional './|../', file part and end of url
const urlMatcher = /(url\(\s*)(\'|\")?(\.\/|\.\.\/)(\S*)(\2\s*\))/g;


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
        const files = glob.sync(path.resolve('node_modules/', module, copyRule), { nodir: true });

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


function rewriteCssUrls(source, handledResourceFolder, themeFolder, logger, options) {
  source = source.replace(urlMatcher, function (match, url, quoteMark, replace, fileUrl, endString) {
    let absolutePath = path.resolve(handledResourceFolder, replace, fileUrl);
    const existingThemeResource = absolutePath.startsWith(themeFolder) && fs.existsSync(absolutePath);
    if (
      existingThemeResource || assetsContains(fileUrl, themeFolder, logger)
    ) {
      // Adding ./ will skip css-loader, which should be done for asset files
      const skipLoader = existingThemeResource ? '' : './';
      const frontendThemeFolder = skipLoader + 'themes/' + path.basename(themeFolder);
      logger.debug(
        'Updating url for file',
        "'" + replace + fileUrl + "'",
        'to use',
        "'" + frontendThemeFolder + '/' + fileUrl + "'"
      );
      const pathResolved = absolutePath.substring(themeFolder.length).replace(/\\/g, '/');

      // keep the url the same except replace the ./ or ../ to themes/[themeFolder]
      return url + (quoteMark??'') + frontendThemeFolder + pathResolved + endString;
    } else if (options.devMode) {
      logger.log("No rewrite for '", match, "' as the file was not found.");
    }
    return match;
  });
  return source;
}

module.exports = { rewriteCssUrls };
