const loaderUtils = require("loader-utils");
const fs = require('fs');
const path = require('path');

// Collect groups [url(] [ |'|"]optional './', file part and end of url
const urlMatcher = /(url\()(\'|\")?(\.\/)(\S*)(\2\))/g;

module.exports = function (source, map) {
  const options = loaderUtils.getOptions(this);
  const handledResourceFolder = path.dirname(this._module.resource);
  const logger = this.getLogger("theme-loader");

  let themeFolder = handledResourceFolder;
  while(themeFolder && path.basename(path.resolve(themeFolder, "..")) !== "theme") {
    themeFolder = path.resolve(themeFolder, "..");
  }
  logger.log("Using '", themeFolder, "' for the application theme base folder.");

  source = source.replace(urlMatcher, function (match, url, quoteMark, replace, fileUrl, endString) {
    let absolutePath = path.resolve(handledResourceFolder, fileUrl);
    if (fs.existsSync(absolutePath) && absolutePath.startsWith(themeFolder)) {
      logger.debug("Updating url for file '",fileUrl,"' to use 'VAADIN/static'");
      const pathResolved = absolutePath.substring(themeFolder.length).replace(/\\/g,'/');

      // keep the url the same except replace the ./ to VAADIN/static
      if (quoteMark) {
        return url + quoteMark + 'VAADIN/static' + pathResolved + endString;
      }
      return url + 'VAADIN/static' +pathResolved + endString;
    } else if(options.devMode) {
      logger.info("No rewrite for '", match, "' as the file was not found.");
    }
    return match;
  });

  this.callback(null, source, map);
}
