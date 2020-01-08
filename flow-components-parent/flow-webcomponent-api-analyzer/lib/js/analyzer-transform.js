/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
'use strict';

const {Transform} = require('readable-stream');
const path = require('path');
const globalVar = require('./global-variables');
const {Analyzer, FSUrlLoader} = require('polymer-analyzer');
console.info("Analyzer setup to load files from Bower dir: " + globalVar.bowerSrcDir);
let analyzer = new Analyzer({
  urlLoader: new FSUrlLoader(globalVar.bowerSrcDir), // relative to where import paths are
});

/**
 * Get relative path to the file from the bower_components directory.
 *
 * @param filePath String absolute path to the file
 * @return String relative path to the file
 */
const getBowerPath = (filePath) => path.relative(globalVar.bowerSrcDir, filePath);

/**
 * Analyzer gulp transform.
 * <p>
 * Runs Analyzer for all the files and emits the Element (?) instance with the analysis data for every element found.
 */
module.exports = class AnalyzerTransform extends Transform {
  constructor(elementFilter, mixinCollector) {
    const options = {};
    options.objectMode = true;
    super(options);
    this._importPaths = [];
    this._elementFilter = elementFilter;
    this._mixinCollector = mixinCollector;
  }

  _transform(file, encoding, callback) {
    this._importPaths.push(file.path);
    callback();
  }

  _flush(callback) {
    const transform = this;

    // Convert import paths array to HTML imports content
    const importsContent = this._importPaths
      .map((file) => getBowerPath(file));

    // Run Analyzer once for the HTML imports
    console.info("Running analyzer with paths: " + importsContent.join('\n    '));
    analyzer.analyze(importsContent)
      .then((analysis) => {

        ["element", "element-mixin", "behavior"].forEach(kind => {
          analysis.getFeatures({kind})
          .forEach(component => {
            const name = component.name ? component.name : component.tagName;
            this._mixinCollector
                .putBehaviors(
                  name,
                  component.behaviorAssignments.map(behavior => behavior.name));
            this._mixinCollector
                .putMixins(
                  name,
                  component.mixins.map(mixin => mixin.identifier));
          });
        });

        const elementSet = analysis.getFeatures({kind: 'element'});
        const elements = [];
        for (const element of elementSet) {
          const baseUrl = element._parsedDocument.baseUrl;

          // Skip elements not in the directories referenced in the dependency file (bower.json)
          if (!this._elementFilter.acceptPath(baseUrl)) {
            console.info("Skipping element of tagName " + element.tagName);
            continue;
          }
          elements.push(element);
          console.info("Element: " + element.tagName
            + " baseUrl: " + baseUrl
            + " extends: " + element.extends
            + ", template: " + element._parsedDocument.contents.includes('<template>')
            + "\n    behaviors: " + element.behaviorAssignments.map(behavior => "" + behavior.name).join(', ')
            + "\n    kinds: " + [...element.kinds].map(kind => "" + kind).join(',')
            + "\n    identifiers: " + [...element.identifiers].map(identifier => "" + identifier).join(',')
            + "\n    mixins: " + element.mixins.map(mixin => "" + mixin).join(','));

          transform.push(element);
        }
        console.info(`Analyzed ${elements.length} UI-elements out of total ${elementSet.size} found elements from ${importsContent.length} files.`);
        callback();
      }).catch((error) => {
        console.error("Error: " + error);
        callback(error);
    });
  }
};
