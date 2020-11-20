module.exports = function (source, map) {
  // Collect groups [url(] [ |'|"]optional './' and end of url
  source = source.replace(/(url\()(\'|\")?(\.\/)(.*\2\))/g, function (match, url, quoteMark, replace, endString) {
    // keep the url the same except replace the ./ to VAADIN/static
    if (quoteMark) {
      return url + quoteMark + 'VAADIN/static/' + endString;
    }
    return url + 'VAADIN/static/' + endString;
  });

  this.callback(null, source, map);
}
