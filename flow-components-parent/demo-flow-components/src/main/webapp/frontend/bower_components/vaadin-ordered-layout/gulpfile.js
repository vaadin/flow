'use strict';

var gulp = require('gulp');
var eslint = require('gulp-eslint');
var htmlExtract = require('gulp-html-extract');
var stylelint = require('gulp-stylelint');
var log = require('gulp-util').log;
var polyserve = require('polyserve');
var spawn = require('child_process').spawn;
var which = require('which');

var perf = {
  lighthouse: './node_modules/.bin/lighthouse',
  port: 7777,
  element: require('./package.json').name,
  dir: 'test/performance',
  tests: require('./test/performance/config.json')
};

gulp.task('lint', ['lint:js', 'lint:html', 'lint:css']);

gulp.task('lint:js', function() {
  return gulp.src([
    '*.js',
    'test/**/*.js'
  ])
    .pipe(eslint())
    .pipe(eslint.format())
    .pipe(eslint.failAfterError('fail'));
});

gulp.task('lint:html', function() {
  return gulp.src([
    '*.html',
    'demo/**/*.html',
    'test/**/*.html'
  ])
    .pipe(htmlExtract({
      sel: 'script, code-example code',
      strip: true
    }))
    .pipe(eslint())
    .pipe(eslint.format())
    .pipe(eslint.failAfterError('fail'));
});

gulp.task('lint:css', function() {
  return gulp.src([
    '*.html',
    'demo/**/*.html',
    'test/**/*.html'
  ])
    .pipe(htmlExtract({
      sel: 'style'
    }))
    .pipe(stylelint({
      reporters: [
        {formatter: 'string', console: true}
      ]
    }));
});

function runCommand(cmd, cb) {
  var args = cmd.split(/\s+/);
  var proc = spawn(args.shift(), args);
  var stdout = '';
  proc.stdout.on('data', data => stdout += data);
  proc.on('close', status => cb(status, stdout));
}

function runTest(test, threshold, cb) {
  const url = `http://localhost:${perf.port}/components/${perf.element}/${perf.dir}/${test}`;
  const command = `node ${perf.lighthouse} --perf --output=json ${url}`;

  log(`Performance running test=${test} threshold=${threshold} ...`);
  runCommand(command, (status, stdout) => {
    var error;
    if (status) {
      error = `Error: status ${status} while running lighthouse CLI`;
    } else {
      var results = JSON.parse(stdout);
      var total = results.aggregations[0].score[0].overall;

      if (total === undefined) {
        error = `Error: lighthouse has not produced a valid JSON output`;
      } else if (total == 1) {
        error = `Error: lighthouse reported score=1. It might be server not running or bad url: ${url}`;
      } else if (total < threshold) {
        error = `Error: low performance scored ${total}`;
      } else {
        log(`Performance done test=${test} scored with ${total}`);
      }
    }
    if (error) {
      log(error);
      perf.failed = true;
    }
    cb();
  });
}

gulp.task('perf:run:server', function() {
  polyserve.startServer({port: perf.port});
});

gulp.task('perf:env', function(cb) {
  which('google-chrome-stable', (err, path) => {
    process.env.LIGHTHOUSE_CHROMIUM_PATH = path;
    cb();
  });
});

gulp.task('perf:run:tests', ['perf:run:server', 'perf:env'], function(cb) {
  perf.tests.reduce((prev, test) => () => runTest(test.name, test.threshold, prev), cb)();
});

gulp.task('perf:run', ['perf:run:tests'], function() {
  log('Finished \'perf:run\' with ' + (perf.failed ? 'error' : 'great success'));
  process.exitCode = perf.failed ? 1 : 0;
  process.exit();
});
