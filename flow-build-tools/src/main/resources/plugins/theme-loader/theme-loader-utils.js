import { existsSync, readFileSync, globSync } from 'fs';
import { resolve, basename, join } from 'path';
import MagicString from 'magic-string';

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
        const files = globSync(resolve('node_modules/', module, copyRule), { withFileTypes: true })
          .filter((dirent) => !dirent.isDirectory() && dirent.parentPath)
          .map((dirent) => join(dirent.parentPath, dirent.name));

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

/**
 * Rewrites the urls of an application theme css file so that they point to the
 * location the referenced files are served from.
 *
 * @param source the contents of the css file
 * @param handledResourceFolder the folder the css file is in
 * @param themeFolder the folder of the application theme
 * @param logger the logger the rewritten urls are reported to
 * @param options the theme options, the devMode flag selects the target folder
 * @returns the rewritten css together with a sourcemap for it, or null when
 * there is no url to rewrite
 */
function rewriteCssUrls(source, handledResourceFolder, themeFolder, logger, options) {
  const magicString = new MagicString(source).replace(
    urlMatcher,
    function (match, url, quoteMark, replace, additionalDotSegments, fileUrl, endString) {
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
        const pathResolved = isAsset ? '/' + fileUrl : absolutePath.substring(themeFolder.length).replace(/\\/g, '/');

        // keep the url the same except replace the ./ or ../ to themes/[themeFolder]
        return url + (quoteMark ?? '') + frontendThemeFolder + pathResolved + endString;
      } else if (options.devMode) {
        logger.log("No rewrite for '", match, "' as the file was not found.");
      } else {
        // In production, the css is in VAADIN/build but the theme files are in .
        return url + (quoteMark ?? '') + '../../' + fileUrl + endString;
      }
      return match;
    }
  );

  // Handing back css without a sourcemap breaks the chain the caller keeps for
  // the file, so report having done nothing when there is nothing to rewrite,
  // and hand back a map of the rewriting when there is.
  if (!magicString.hasChanged()) {
    return null;
  }
  return { code: magicString.toString(), map: magicString.generateMap({ hires: true }) };
}

export { rewriteCssUrls };
