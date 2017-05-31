"use strict";

var args = require('minimist')(process.argv.slice(2));
var gulp = require('gulp');
var bower = require('gulp-bower');
var map = require('map-stream');
var File = require('vinyl');
var path = require('path');
var fs = require('fs-extra');
var globalVar = require('./lib/js/global-variables');
var HydrolysisTransform = require('./lib/js/hydrolysis-transform');
var gutil = require('gulp-util');
var _ = require('lodash');
var ejs = require('ejs');
var runSequence = require('run-sequence');
var jsonfile = require('jsonfile');
var StreamFromArray = require('stream-from-array');
var rename = require("gulp-rename");
var marked = require('marked');

var libDir = __dirname + '/lib/java/';
var tplDir = __dirname + '/template/';

// var helpers = require(tplDir + "helpers");
// require('require-dir')(tplDir + 'tasks');

// Using global because if we try to pass it to templates via the helper or any object
// we need to call merge which makes a copy of the structure per template slowing down
// the performance.
global.parsed = []; // we store all parsed objects so as we can iterate or find behaviors
global.templates = {};

gulp.task('clean:target', function() {
  fs.removeSync(globalVar.clientDir + 'element');
  fs.removeSync(globalVar.clientDir + 'widget');
});

gulp.task('clean:resources', function() {
  fs.removeSync(globalVar.publicDir);
});

gulp.task('clean', ['clean:target', 'clean:resources']);

gulp.task('bower:configure', ['clean:resources'], function(done) {
  jsonfile.readFile('.bowerrc', function (err, obj) {
    if (!err) {
      fs.copySync('.bowerrc', globalVar.publicDir + '/.bowerrc');
      if(obj.directory) {
        globalVar.bowerDir = globalVar.publicDir + '/' + obj.directory;
      }
    }
    done();
  });
});

gulp.task('bower:install', ['clean', 'bower:configure'], function() {
  if (globalVar.bowerPackages) {
    return bower({ cmd: 'install', cwd: globalVar.publicDir}, [globalVar.bowerPackages]);
  } else {
    gutil.log('No --package provided. Using package(s) from bower_components folder.');
    return gulp.src('./bower_components/**/*', {base: '.'}).pipe(gulp.dest(globalVar.publicDir));
  }
});

gulp.task('analyze', ['clean:target'], function() {
});

gulp.task('templates', function() {
  return gulp.src([tplDir + '**/*.java.ejs'])
    .pipe(map(function(file, cb) {
      var name = path.relative(tplDir, file.path).replace(/\.java\.ejs$/, '');
      global.templates[name] = ejs.compile(fs.readFileSync(file.path).toString(), {
        filename: file.path
      });
      cb(null, file);
    }));
});

gulp.task('generate', ['templates', 'clean:target'], function() {
  return gulp.src([globalVar.bowerDir + "*/*.html",
    // ignore Polymer itself
    "!" + globalVar.bowerDir + "polymer/*",
    // ignore all demo.html, index.html and metadata.html files
    "!" + globalVar.bowerDir + "*/demo.html",
    "!" + globalVar.bowerDir + "*/index.html",
    "!" + globalVar.bowerDir + "*/metadata.html",
    // includes a set of js files only, and some do not exist
    "!" + globalVar.bowerDir + "web-animations-js/*",
    // Not useful in gwt and also has spurious event names
    "!" + globalVar.bowerDir + "iron-jsonp-library/*",
    ], {base: globalVar.clientDir})
    .pipe(new HydrolysisTransform())
    .pipe(map(function(item, cb) {
      // Render the Java code template
      var fileBaseName = path.join(item.package, item.name);
      var template = global.templates[fileBaseName]
          || global.templates[item.type];
      var render = template(item);
      var file = new File({
        path: globalVar.clientDir + fileBaseName + '.java',
        contents: new Buffer(render)
      });
      cb(null, file);
    }))
    .pipe(gulp.dest('.'));
});

gulp.task('default', function(){
  runSequence('clean', 'bower:install', 'generate');
});
