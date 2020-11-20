const fs = require('fs');
const path = require('path');

const urlMatcher = /(url\()(\'|\")?(\.\/)(.*\2\))/g;

module.exports = function (source, map) {
  const handledResourceFolder = path.dirname(this._module.resource);
  const logger = this.getLogger("theme-loader");

  // Collect groups [url(] [ |'|"]optional './' and end of url
  source = source.replace(urlMatcher, function (match, url, quoteMark, replace, endString) {
    // for some reason this matcher can not be reused as a constant
    let fileUrl = /url\((\'|\")?(.*)\1\)/g.exec(match)[2];
    if (fs.existsSync(path.resolve(handledResourceFolder, fileUrl))) {
      logger.debug("Updating url for file '",fileUrl,"' to use 'VAADIN/static'");
      // keep the url the same except replace the ./ to VAADIN/static
      if (quoteMark) {
        return url + quoteMark + 'VAADIN/static/' + endString;
      }
      return url + 'VAADIN/static/' + endString;
    }
    return match;
  });

  this.callback(null, source, map);
}
