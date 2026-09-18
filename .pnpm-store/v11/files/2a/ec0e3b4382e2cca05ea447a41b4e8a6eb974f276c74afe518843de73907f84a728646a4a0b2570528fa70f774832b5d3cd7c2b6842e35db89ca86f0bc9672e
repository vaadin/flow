var execSync = require('child_process').execSync;
var join = require('path').join;
var _ = require('underscore');
var Q = require('q');
var debug = require('debug')('launchpad:local');
var mkdirp = require('mkdirp');

var instance = require('./instance');
var getBrowser = require('./browser');
var getVersion = require('./version');
var platform = require('./platform');

var normalize = function(browser) {
  return _.pick(browser, 'name', 'version', 'path', 'binPath');
};


function makeNixTmpDir() {
  return execSync('mktemp -d -t launchpad.XXXXXXX').toString().trim();
}

function makeWin32TmpDir() {
  var winTmpPath = process.env.TEMP || process.env.TMP ||
      (process.env.SystemRoot || process.env.windir) + '\\temp';
  var randomNumber = Math.floor(Math.random() * 9e7 + 1e7);
  var tmpdir = join(winTmpPath, 'launchpad.' + randomNumber);

  mkdirp.sync(tmpdir);
  return tmpdir;
}

var tmpdir = process.platform === "win32" ? makeWin32TmpDir() : makeNixTmpDir();

var cleanLaunch = {
  firefox: ['-no-remote', '-silent', '-p', 'launchpad-firefox'],
  opera: ['-nosession'],
  chrome: [
      // Disable built-in Google Translate service
    '--disable-features=TranslateUI',
    // Disable all chrome extensions
    '--disable-extensions',
    // Disable some extensions that aren't affected by --disable-extensions
    '--disable-component-extensions-with-background-pages',
    // Disable various background network services, including extension updating,
    //   safe browsing service, upgrade detector, translate, UMA
    '--disable-background-networking',
    // Disable syncing to a Google account
    '--disable-sync',
    // Disable reporting to UMA, but allows for collection
    '--metrics-recording-only',
    // Disable installation of default apps on first run
    '--disable-default-apps',
    // Mute any audio
    '--mute-audio',
    // Skip first run wizards
    '--no-first-run',
    // Disable backgrounding renders for occluded windows
    '--disable-backgrounding-occluded-windows',
    // Disable renderer process backgrounding
    '--disable-renderer-backgrounding',
    // Disable task throttling of timer tasks from background pages.
    '--disable-background-timer-throttling',
    '--user-data-dir=' + tmpdir
  ]
};

module.exports = function (settings, callback) {
	if (!callback) {
		callback = settings;
		settings = undefined;
	}

  debug('Initializing local launchers', settings);
  var api = function(url, options, callback) {
    var name = options.browser;

    debug('Launching browser', url, options);
    getBrowser(_.extend({ name: name }, platform[name])).then(function(browser) {
      if(browser === null) {
        return Q.reject(new Error('Browser ' + name + ' not available.'));
      }

      var args = [];
      if(options.clean) {
        if (cleanLaunch[name]) {
          args = args.concat(cleanLaunch[name]);
        }

        //Clean instances needs to be run with bin file
        browser.command = browser.binPath;

        // keep the tmpdir in options
        // in order to be cleaned after closing the browser
        browser.tmpdir = tmpdir;
        browser.clean = true;

        // The settings to be passed to childProcess.spawn
        // We need to have a detached clean process instance
        settings = {
          detached: true
        };
      } else if (browser.args) {
        args = browser.args;
      }

      if(options.args) {
        args = args.concat(options.args);
      }
      
      // Convert the command if set (some browsers need some customization)
      if(browser.getCommand) {
        browser.command = browser.getCommand(browser, url, args, options);
        args = null;
      } else {
        args = args.concat(url);
      }

      debug('Starting Browser', browser.name);
      return Q.ninvoke(instance, 'start', browser.command, args, settings, browser);
    }).nodeify(callback);
  };

  api.browsers = function(callback) {
    var deferreds = _.map(platform, function(browser, name) {
      return getBrowser(_.extend({ name: name }, browser)).then(getVersion);
    });

    Q.all(deferreds).then(function(browsers) {
      debug('Listed all browsers (before normalization)', browsers);
      return _.map(_.compact(browsers), normalize);
    }).nodeify(callback);
  };

  _.each(platform, function(browser, name) {
    api[name] = function(url, options, callback) {
      if(!callback) {
        callback = options;
        options = {};
      }

      options.browser = name;

      return api(url, options, callback);
    };
  });

  callback(null, api);
};

module.exports.platform = platform;
