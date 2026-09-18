var path = require('path');

module.exports = {
  chrome: {
    pathQuery: 'which google-chrome',
    process: 'chrome',
    opensTab: true,
    args: ['--no-first-run']
  },
  chromium: {
    pathQuery: 'which chromium-browser',
    process: 'chromium-browser',
    opensTab: true
  },
  firefox: {
    pathQuery: 'which firefox',
    process: 'firefox',
    multi: true,
    // See https://developer.mozilla.org/en-US/docs/Mozilla/Command_Line_Options
    // Assuming Firefox >= 20
    args: ['--private-window']
  },
  opera: {
    pathQuery: 'which opera',
    process: 'opera'
  },
  electron: {
    pathQuery: 'which electron',
    process: 'electron',
    args: [path.join(__dirname, '..', '..', '..', 'resources', 'electron.js')],
    multi: true,
    defaultLocation: [
      path.join(process.cwd(), 'node_modules', '.bin', 'electron'),
      '/usr/local/bin/electron'
    ],
  },
  phantom: {
    pathQuery: 'which phantomjs',
    process: 'phantomjs',
    args: [path.join(__dirname, '..', '..', '..', 'resources', 'phantom.js')],
    multi: true,
    defaultLocation: [
      path.join(process.cwd(), 'node_modules', '.bin', 'phantomjs'),
      '/usr/local/bin/phantomjs'
    ],
  }
};
