'use strict';

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var fs = _interopDefault(require('fs'));
var path = require('path');
var zlib = require('zlib');

const magicBytes = [
  [0x1F, 0x8B], // .gz
  [0xFD, 0x37, 0x7A, 0x58, 0x5A, 0x00, 0x00], // .xz
  [0x04, 0x22, 0x4D, 0x18], // .lz2
  [0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C] // .7z
];

function isCompressed (bundle) {
  if (/\.(gz|zip|xz|lz2|7z|woff|woff2|jpg|jpeg|png|webp)$/.test(bundle.fileName)) return true
  for (const bytes of magicBytes) {
    let matches = true;
    const sourceBytes = bundle.type === 'asset' ? bundle.source : Buffer.from(bundle.code);
    for (let i = 0; i < bytes.length; ++i) {
      matches = matches && bytes[i] === sourceBytes[i];
    }
    if (matches) return true
  }
  return false
}

function brotliCompressFile(file, options, minSize) {
  return new Promise(resolve => {
    fs.stat(file, (err, stats) => {
      if(err) {
        console.error('rollup-plugin-brotli: Error reading file ' + file);
        resolve();
        return
      }

      if(minSize && minSize > stats.size) {
        resolve();
      } else {
        fs.createReadStream(file)
          .pipe(zlib.createBrotliCompress(options))
          .pipe(fs.createWriteStream(file + '.br'))
          .on('close', () => resolve());
      }
    });
  })
}

function brotli(options = {}) {
  let _dir = '';
  options = Object.assign({
    test: /\.(js|css|html|txt|xml|json|svg|ico|ttf|otf|eot)$/,
    additional: [],
    minSize: 0,
    options: {},
  }, options);
  return {
    name: 'brotli',
    generateBundle: buildOpts => {
      _dir = (buildOpts.file && path.dirname(buildOpts.file)) || buildOpts.dir || '';
    },
    writeBundle: async (outputOptions, bundle) => {
      const compressCollection = [];
      const bundlesToCompress = 
        Object.keys(bundle)
          .filter(id => options.test.test(bundle[id].fileName))
          .filter(id => !isCompressed(bundle[id]))
          .map(id => bundle[id].fileName);
      const files = [...options.additional, ...bundlesToCompress.map(f => path.join(_dir, f))];
      for (const file of files) {
        compressCollection.push(brotliCompressFile(file, options.options, options.minSize));
      }
      await Promise.all(compressCollection);
    }
  }
}

module.exports = brotli;
