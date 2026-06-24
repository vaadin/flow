import { getOptions } from 'loader-utils';
import { dirname, basename, resolve } from 'path';
import { rewriteCssUrls } from './theme-loader-utils';

/**
 * This custom loader handles rewriting urls for the application theme css files.
 * URLs starting with ./ or ../ are checked against the filesystem and converted if a file exists.
 * URLs going outside of the application theme folder are not accepted and will not be rewritten.
 *
 * @param source file contents to handle
 * @param map source map for file
 */
export default function (source, map) {
  const options = getOptions(this);
  const handledResourceFolder = dirname(this._module.resource);
  const logger = this.getLogger('theme-loader');

  let themeFolder = handledResourceFolder;
  // Recurse up until we find the themes folder or don't have 'themes' on the path.
  while (themeFolder.indexOf('themes') > 1 && basename(resolve(themeFolder, '..')) !== 'themes') {
    themeFolder = resolve(themeFolder, '..');
  }
  // If we have found no themes folder return without doing anything.
  if (basename(resolve(themeFolder, '..')) !== 'themes') {
    this.callback(null, source, map);
    return;
  }

  logger.log("Using '", themeFolder, "' for the application theme base folder.");

  source = rewriteCssUrls(source, handledResourceFolder, themeFolder, logger, options);
  this.callback(null, source, map);
};
