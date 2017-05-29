'use strict';

var Transform = require('readable-stream').Transform;
var path = require('path');
var hydrolysis = require('hydrolysis');

var globalVar = require('./global-variables');
var Behavior = require('./behavior');
var Element = require('./element');


/**
 * Get relative path to the file from the bower_components directory
 * @param String filePath absolute path to the file
 * @return String relative path to the file
 */
var getBowerPath = (filePath) => path.relative(globalVar.bowerDir, filePath);


/**
 * Exctacts the directory name under bower_components for the given file
 * @param String filePath Absolute path to the file
 * @return String Name of the bower package
 */
var getBowerPackageName = (filePath) => getBowerPath(filePath).split(path.sep)[0];


/**
 * Hydrolysis gulp transform plugin. Runs Hydrolysis for all the files and
 * emits the Element instance with the analysis data for every element found.
 */
module.exports = class HydrolysisTransform extends Transform {
  constructor(options) {
    if (!options) options = {};
    options.objectMode = true;
    super(options);
    this._importPaths = [];
  }

  _transform(file, encoding, callback) {
    this._importPaths.push(file.path);
    callback();
  }

  _flush(callback) {
    var transform = this;

    // Convert import paths array to HTML imports content
    var importsPath = path.join(globalVar.bowerDir, 'all-imports.html');
    var importsContent = this._importPaths
        .map((file) => '<link rel="import" href="' + getBowerPath(file) + '">\n')
        .join('');

    // Run Hydrolysis once for the HTML imports
    hydrolysis.Analyzer.analyze(importsPath, {
      clean: true,
      filter: (file) => false,
      content: importsContent
    }).then(function(analyzer) {
      analyzer.elements.map((descriptor) => {

        if (!descriptor.behaviors) {
          // The element does not register a prototype, skipping.
          return;
        }

        if (getBowerPackageName(descriptor.contentHref) === 'polymer') {
          // The element is defined in Polymer, skipping.
          return;
        }

        try {
          var element = new Element(getBowerPath(descriptor.contentHref), descriptor);
          transform.push(element);
        } catch(e) {
          console.log(e);
          callback(e);
        }

      });
      callback();
    }).catch(callback);
  }
}
