var tape = require('tape');
var minimatchAll = require('./');

tape('Match', function (t) {
  var patterns = [
    // match all js files
    '**/*.js',

    // except for files in the foo/ directory
    '!foo/**',

    // unless it's foo/bar.js
    'foo/bar.js',
  ];

  t.equals(
    minimatchAll('foo.js', patterns),
    true,
    'Match .js files');

  t.equals(
    minimatchAll('foo/foo.js', patterns),
    false,
    'Files in the foo/ directory should be excluded');

  t.equals(
    minimatchAll('foo/bar.js', patterns),
    true,
    'foo/bar.js is an exception to the rule');

  t.end();
});

tape('Multiple exclusions', function (t) {
  var patterns = [
    // match all files
    '**/*',

    // exclude everything in foo/
    '!**/foo/**',

    // and also exclude everything in bar/
    '!**/bar/**'
  ];

  t.equals(
    minimatchAll('important.exe', patterns),
    true,
    'Match all files');

  t.equals(
    minimatchAll('foo/one.js', patterns),
    false,
    'But exclude files in foo/');

  t.equals(
    minimatchAll('bar/two.js', patterns),
    false,
    'And exlude files in bar/ as well');

  t.end();
});
