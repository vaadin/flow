var minimatch = require('minimatch');

module.exports = function minimatchAll (path, patterns, opts) {
  var match = false;

  patterns.forEach(function (pattern) {
    var isExclusion = pattern[0] === '!';

    // If we've got a match, only re-test for exclusions.
    // if we don't have a match, only re-test for inclusions.
    if (match !== isExclusion) { return; }

    match = minimatch(path, pattern, opts);
  });
  return match;
};
