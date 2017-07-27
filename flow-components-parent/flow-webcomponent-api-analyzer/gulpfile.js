/*
 * Copyright 2000-2017 Vaadin Ltd.
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

const args = require('minimist')(process.argv.slice(2));
const gulp = require('gulp');
const bower = require('gulp-bower');
const map = require('map-stream');
const path = require('path');
const fs = require('fs-extra');
const globalVar = require('./lib/js/global-variables');
const ElementFilter = require('./lib/js/element-filter');
const VersionReader = require('./lib/js/version-transform');
const MixinCollector = require('./lib/js/mixin-collector');
const AnalyzerTransform = require('./lib/js/analyzer-transform');
const ElementJsonTransform = require('./lib/js/element-json-transform');
const gutil = require('gulp-util');
const ejs = require('ejs');
const runSequence = require('run-sequence');
const jsonfile = require('jsonfile');
const rename = require("gulp-rename");
const marked = require('marked');
const gulpIgnore = require('gulp-ignore');

gulp.task('clean:target', function() {
  fs.removeSync(globalVar.targetDir);
});

gulp.task('clean:resources', function() {
  fs.removeSync(globalVar.bowerTargetDir);
});

gulp.task('clean', ['clean:target', 'clean:resources']);

gulp.task('bower:configure', ['clean:resources'], function(done) {
  jsonfile.readFile('.bowerrc', function (err, obj) {
    if (!err) {
      fs.copySync('.bowerrc', globalVar.bowerTargetDir + '/.bowerrc');
      if(obj.directory) {
        globalVar.bowerTargetDir = globalVar.bowerTargetDir+ '/' + obj.directory;
      }
    }
    done();
  });
});

gulp.task('bower:install', ['clean', 'bower:configure'], function() {
  if (globalVar.bowerPackages) {
    return bower({ cmd: 'install', cwd: globalVar.bowerTargetDir}, [globalVar.bowerPackages]);
  } else {
    gutil.log(`No --package provided. Using package(s) from ${globalVar.bowerSrcDir} folder.`);
    return gulp.src(globalVar.bowerSrcDir + '/**/*').pipe(gulp.dest(globalVar.bowerTargetDir));
  }
});

gulp.task('generate', ['clean:target'], function() {
  console.log('Running generate task, for resources from: ' + globalVar.bowerTargetDir);

  // the element filter reads the bower.json file and parses the dependecies
  const elementFilter = new ElementFilter();
  // the version reader reads the versions only for those elements that the json is created for
  const versionReader = new VersionReader();
  // stores mixin/behavior information of encountered components
  const mixinCollector = new MixinCollector();

  return gulp.src([globalVar.bowerTargetDir + "*/*.html",
    // ignore Polymer itself
    "!" + globalVar.bowerTargetDir + "polymer/*",
    // ignore all demo.html, index.html and metadata.html files
    "!" + globalVar.bowerTargetDir + "*/demo.html",
    "!" + globalVar.bowerTargetDir + "*/index.html",
    "!" + globalVar.bowerTargetDir + "*/metadata.html",
    // includes a set of js files only, and some do not exist
    "!" + globalVar.bowerTargetDir + "web-animations-js/*",
    // Not useful in gwt and also has spurious event names
    "!" + globalVar.bowerTargetDir + "iron-jsonp-library/*",
    ])
    .pipe(gulpIgnore.include((file) => elementFilter.acceptFile(file))) // ignores files not directly mentioned in the dependencies
    .pipe(versionReader) // Reads the versions of the elements
    .pipe(new AnalyzerTransform(elementFilter, mixinCollector)) // transforms out PolymerElements
    .pipe(new ElementJsonTransform(versionReader, mixinCollector)) // transforms out json files
    .pipe(gulp.dest('.'));
});

gulp.task('default', function(){
  runSequence('clean', 'bower:install', 'generate');
});
