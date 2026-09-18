var assert = require('assert');
var http = require('http');
var path = require('path');
var decache = require('decache');
var useragent = require('useragent');
var familyMapping = {
  canary: 'chrome',
  chromium: process.platform === 'darwin' ? 'chrome' : 'chromium',
  phantom: 'phantomjs',
  nodeWebkit: 'chrome'
};
var server = http.createServer(function (req, res) {
  res.writeHead(200, {'Content-Type': 'text/plain'});
  res.end('Hello World\n');
}).listen(6785);

describe('Local browser launcher tests', function() {

  describe('Default env settings', function () {

    var local = require('../lib/local');

    it('does local browser and version discovery', function (done) {
      local(function (error, launcher) {
        launcher.browsers(function (error, browsers) {
          assert.ok(!error, 'No error discovering browsers');
          assert.ok(browsers.length, 'Found at least one browser');
          assert.ok(browsers[0].version, 'First browser has a version');
          assert.ok(browsers[0].path, 'First browser has a path');
          assert.ok(browsers[0].binPath, 'First browser has a binPath');
          done();
        });
      });
    });
    
    Object.keys(local.platform).forEach(function (name) {
      it('Launches ' + name + ' browser on ' + process.platform, function (done) {
        local(function (error, launcher) {
          launcher[name]('http://localhost:6785', function (error, instance) {
            if (error) {
              // That's the only error we should get
              assert.equal(error.message, 'Browser ' + name + ' not available.');
              return done();
            }

            server.once('request', function (req) {
              var userAgent = useragent.parse(req.headers['user-agent']);
              var expected = familyMapping[name] || name;
              assert.equal(userAgent.family.toLowerCase(), expected, 'Got expected browser family');
              instance.stop(done);
            });
          });
        });
      });
    });
  });

  describe('Custom env settings', function () {

    var node_modules = path.join(__dirname, '..', 'node_modules');
    var local;

    beforeEach(function () {
      decache(path.join(__dirname, '..', 'lib', 'local', 'index.js'));
    });

    after(function () {
      delete process.env.LAUNCHPAD_BROWSERS;
    });

    describe('Electron', function () {

      beforeEach(function () {
        process.env.LAUNCHPAD_BROWSERS = 'electron';
        process.env.LAUNCHPAD_ELECTRON = /^win/.test(process.platform) ?
            path.join(node_modules, 'electron', 'dist', 'electron.exe') : process.platform == 'darwin' ?
            path.join(node_modules, 'electron', 'dist', 'Electron.app', 'Contents', 'MacOS', 'Electron') :
            path.join(node_modules, 'electron', 'dist', 'electron');

        local = require('../lib/local');
      });

      after(function () {
        delete process.env.LAUNCHPAD_ELECTRON;
      });

      it('is detected only due to env settings', function (done) {
        local(function (error, launcher) {
          launcher.browsers(function (error, browsers) {
            assert.ok(!error, 'No error discovering browsers');
            assert.equal(browsers.length, 1, 'Found Electron browser');
            assert.equal(browsers[0].path, process.env.LAUNCHPAD_ELECTRON, 'Found Electron at selected location');
            done();
          });
        });
      });
    });

    describe('PhantomJS', function () {

      beforeEach(function () {
        process.env.LAUNCHPAD_BROWSERS = 'phantom';
        process.env.LAUNCHPAD_PHANTOM = /^win/.test(process.platform) ?
            path.join(node_modules, 'phantomjs-prebuilt', 'lib', 'phantom', 'phantomjs.exe') :
            path.join(node_modules, 'phantomjs-prebuilt', 'bin', 'phantomjs');

        local = require('../lib/local');
      });

      after(function () {
        delete process.env.LAUNCHPAD_PHANTOM;
      });

      it('is detected only due to env settings', function (done) {
        local(function (error, launcher) {
          launcher.browsers(function (error, browsers) {
            assert.ok(!error, 'No error discovering browsers');
            assert.equal(browsers.length, 1, 'Found PhantomJS browser');
            assert.equal(browsers[0].path, process.env.LAUNCHPAD_PHANTOM, 'Found PhantomJS at selected location');
            done();
          });
        });
      });
    });
  });
  if (process.platform === 'linux') {
    describe('Clean option', function() {
      var local = require('../lib/local');
      var chrome;
  
      function makeTest(isHeadless, done) {
        local(function (error, launcher) {
          var args = isHeadless ? ['--headless'] : [];
          launcher.chrome('http://localhost:6785', {clean: true, args: args}, function(error, instance) {
  
            if (error) return done(error);
  
            server.once('request', function () {
              assert.ok(instance.options.clean);
              assert.ok(instance.options.tmpdir);
              assert.notEqual(chrome.id, instance.id);
              setTimeout(function () {
                instance.stop(done());
              }, 1000);
            });
          });
        });
      }
  
      beforeEach(function() {
        local(function (error, launcher) {
          launcher.chrome('http://localhost:6785', {clean: true}, function(error, instance) {
            chrome = instance;
          });
        });
      });
  
      afterEach(function() {
        chrome.stop();
      });
  
      it('Launches Chrome in clean mode', function(done) {
        makeTest(false, done);
      });
  
      it('Launches headless Chrome in clean mode', function(done) {
        makeTest(true, done);
      });
    });
  }
});
