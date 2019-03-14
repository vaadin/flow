const path = require('path');
const fs = require('fs');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const NpmInstallPlugin = require('webpack-plugin-install-deps');
const { BabelMultiTargetPlugin } = require('webpack-babel-multi-target-plugin');

const baseDir = path.resolve(__dirname);
const inputFolder = baseDir + '/src/main/webapp/frontend';
const outputFolder = baseDir + '/src/main/webapp';
const build = 'build';

fs.mkdirSync(`${outputFolder}/${build}`, { recursive: true });
const statsFile = `${outputFolder}/${build}/stats.json`;

module.exports = {
  mode: 'production',
  context: inputFolder,
  entry: {
    index: './main.js'
  },

  output: {
    filename: `${build}/[name].js`,
    path: outputFolder
  },

  module: {
    rules: [{
      test: /\.js$/,
      use: [BabelMultiTargetPlugin.loader()]
    }]
  },

  plugins: [

    // Transpile with babel, and produce different bundles per browser
    new BabelMultiTargetPlugin({
      babel: {
        presetOptions: {
          useBuiltIns: false // polyfills are provided from webcomponents-loader.js
        }
      },
      targets: {
        'es6': {
          browsers: [
            'last 2 Chrome major versions',
            'last 2 ChromeAndroid major versions',
            'last 2 Edge major versions',
            'last 2 Firefox major versions',
            'last 2 Safari major versions',
            'last 2 iOS major versions'
          ],
        },
        'es5': {
          browsers: [
            'ie 11'
          ],
          tagAssetsWithKey: true, // append a suffix to the file name
        }
      }
    }),

    // Automatically execute `npm install` to download & install missing dependencies.
    new NpmInstallPlugin(),

    // Generates the `stats.json` file which is used by flow to read templates for
    // server `@Id` binding
    function (compiler) {
      compiler.plugin('after-emit', function (compilation, done) {
        console.log("Emitted " + statsFile)
        fs.writeFile(statsFile, JSON.stringify(compilation.getStats().toJson(), null, 1), done);
      });
    },

    // Copy webcomponents polyfills. They are not bundled because they
    // have its own loader based on browser quirks.
    new CopyWebpackPlugin([{
      from: `${baseDir}/node_modules/@webcomponents/webcomponentsjs`,
      to: `${build}/webcomponentsjs/`
    }]),
  ]
};
