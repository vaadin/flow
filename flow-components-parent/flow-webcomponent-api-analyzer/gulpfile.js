/*
 * Copyright 2000-2019 Vaadin Ltd.
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
"use strict";

const gulp = require('gulp');
const fs = require('fs-extra');
const globalVar = require('./lib/js/global-variables');
const ElementFilter = require('./lib/js/element-filter');
const VersionReader = require('./lib/js/version-transform');
const MixinCollector = require('./lib/js/mixin-collector');
const AnalyzerTransform = require('./lib/js/analyzer-transform');
const ElementJsonTransform = require('./lib/js/element-json-transform');
const VariantsTransform = require('./lib/js/variants-transform');
const gulpIgnore = require('gulp-ignore');

gulp.task('prepare', cb => {
  if (!fs.existsSync(globalVar.bowerSrcDir) || fs.readdirSync(globalVar.bowerSrcDir).length === 0) {
    console.error(`Source directory ${globalVar.bowerSrcDir} does not exists or empty`);
    process.exit(1)
  }

  console.log(`Cleaning output directory ${globalVar.targetDir}`);
  fs.removeSync(globalVar.targetDir);
  cb();
});

const variantsData = {};
gulp.task('gather-variants-data', gulp.series('prepare', () => {
  return gulp.src([globalVar.bowerSrcDir + "/*/theme/*/vaadin-*-styles.html"])
    .pipe(new VariantsTransform(variantsData));
}));

gulp.task('generate', gulp.series('gather-variants-data', () => {
  console.log(`Running generate task, for resources from: ${globalVar.bowerSrcDir}`);
  // the element filter reads the bower.json file and parses the dependencies
  const elementFilter = new ElementFilter();
  // the version reader reads the versions only for those elements that the json is created for
  const versionReader = new VersionReader();
  // stores mixin/behavior information of encountered components
  const mixinCollector = new MixinCollector();

  return gulp.src([globalVar.bowerSrcDir + "/*/*.html",
    // ignore Polymer itself
    "!" + globalVar.bowerSrcDir + "polymer/*",
    // ignore all demo.html, index.html and metadata.html files
    "!" + globalVar.bowerSrcDir + "*/demo.html",
    "!" + globalVar.bowerSrcDir + "*/index.html",
    "!" + globalVar.bowerSrcDir + "*/metadata.html",
    // includes a set of js files only, and some do not exist
    "!" + globalVar.bowerSrcDir + "web-animations-js/*",
    // Not useful in gwt and also has spurious event names
    "!" + globalVar.bowerSrcDir + "iron-jsonp-library/*",
  ])
    .pipe(gulpIgnore.include(file => elementFilter.acceptFile(file))) // ignores files not directly mentioned in the dependencies
    .pipe(versionReader) // Reads the versions of the elements
    .pipe(new AnalyzerTransform(elementFilter, mixinCollector)) // transforms out PolymerElements
    .pipe(new ElementJsonTransform(versionReader, mixinCollector, variantsData)) // transforms out json files
    .pipe(gulp.dest('.'));
}));

gulp.task('default', gulp.task('generate'));
