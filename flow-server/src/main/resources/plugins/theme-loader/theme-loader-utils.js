import { existsSync, readFileSync } from 'fs';
import { resolve, basename } from 'path';
import { globSync } from 'glob';

// Collect groups [url(] ['|"]optional './|../', other '../' segments optional, file part and end of url
// The additional dot segments could be URL referencing assets in nested imported CSS
// When Vite inlines CSS import it does not rewrite relative URL for not-resolvable resource
// so the final CSS ends up with wrong relative URLs (r.g. ../../pkg/icon.svg)
// If the URL is relative, we should try to check if it is an asset by ignoring the additional dot segments
const urlMatcher = /(url\(\s*)(\'|\")?(\.\/|\.\.\/)((?:\3)*)?(\S*)(\2\s*\))/g;

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
        const files = globSync(resolve('node_modules/', module, copyRule), { nodir: true });

        for (let file of files) {
          if (file.endsWith(targetFile)) return true;
        }
      }
    }
  }
  return false;
}

function getThemeProperties(themeFolder) {
  const themePropertyFile = resolve(themeFolder, 'theme.json');
  if (!existsSync(themePropertyFile)) {
    return {};
  }
  const themePropertyFileAsString = readFileSync(themePropertyFile);
  if (themePropertyFileAsString.length === 0) {
    return {};
  }
  return JSON.parse(themePropertyFileAsString);
}

function rewriteCssUrls(source, handledResourceFolder, themeFolder, logger, options) {
  source = source.replace(urlMatcher, function (match, url, quoteMark, replace, additionalDotSegments, fileUrl, endString) {
    let absolutePath = resolve(handledResourceFolder, replace, additionalDotSegments || '', fileUrl);
    let existingThemeResource = absolutePath.startsWith(themeFolder) && existsSync(absolutePath);
    if (!existingThemeResource && additionalDotSegments) {
      // Try to resolve path without dot segments as it may be an unresolvable
      // relative URL from an inlined nested CSS
      absolutePath = resolve(handledResourceFolder, replace, fileUrl);
      existingThemeResource = absolutePath.startsWith(themeFolder) && existsSync(absolutePath);
    }
    const isAsset = assetsContains(fileUrl, themeFolder, logger);
    if (existingThemeResource || isAsset) {
      // Adding ./ will skip css-loader, which should be done for asset files
      // In a production build, the css file is in VAADIN/build and static files are in VAADIN/static, so ../static needs to be added
      const replacement = options.devMode ? './' : '../static/';

      const skipLoader = existingThemeResource ? '' : replacement;
      const frontendThemeFolder = skipLoader + 'themes/' + basename(themeFolder);
      logger.log(
        'Updating url for file',
        "'" + replace + fileUrl + "'",
        'to use',
        "'" + frontendThemeFolder + '/' + fileUrl + "'"
      );
      // assets are always relative to theme folder
      const pathResolved = isAsset ? '/' + fileUrl
          : absolutePath.substring(themeFolder.length).replace(/\\/g, '/');

      // keep the url the same except replace the ./ or ../ to themes/[themeFolder]
      return url + (quoteMark ?? '') + frontendThemeFolder + pathResolved + endString;
    } else if (options.devMode) {
      logger.log("No rewrite for '", match, "' as the file was not found.");
    } else {
      // In production, the css is in VAADIN/build but the theme files are in .
      return url + (quoteMark ?? '') + '../../' + fileUrl + endString;
    }
    return match;
  });
  return source;
}

export { rewriteCssUrls };
