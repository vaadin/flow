// @ts-check
const depcheck = require('depcheck');
const path = require('path');

const check = new Promise((resolve, reject) => {
    depcheck(
      path.join(__dirname, '..'),
      {
        ignoreDirs: ['test-fixtures'], ignoreMatches: [
          // "@types/*" dependencies are type declarations that are
          // automatically loaded by TypeScript during build. depcheck can't
          // detect this so we ignore them here.

          '@types/*',
          // Also it can't yet parse files that use async iteration.
          // TODO(rictic): remove these
          'mz',
          'multipipe',
          'polymer-bundler',
          'parse5',
          'dom5',
          '@babel/traverse',
          'stream',
          'html-minifier',
          '@polymer/esm-amd-loader',
          'babel-plugin-minify-guarded-expressions',
        ]
      },
      resolve);
  })
    .then((result) => {
      const invalidFiles = Object.keys(result.invalidFiles) || [];
      const invalidJsFiles = invalidFiles.filter((f) => f.endsWith('.js'));

      const unused = new Set(result.dependencies);
      if (unused.size > 0) {
        console.log('Unused dependencies:', unused);
        throw new Error('Unused dependencies');
      }
    });

check.catch((e) => {
  console.error(e);
  process.exit(1);
});
