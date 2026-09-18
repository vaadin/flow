/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
var fs = require('fs');
var path = require('path');

function isTravisSauceConnectRunning() {
  // https://docs.travis-ci.com/user/environment-variables/#Default-Environment-Variables
  if (!process.env.TRAVIS) {
    return false;
  }

  try {
    // when using the travis sauce_connect addon, the file
    // /home/travis/sauce-connect.log is written to with the sauce logs.
    // If this file exists, then the sauce_connect addon is in use
    // If fs.statSync throws, then the file does not exist
    var travisScLog = path.join(process.env.HOME, 'sauce-connect.log');
    if (fs.statSync(travisScLog)) {
      return true;
    }
    return false;
  } catch (e) {
    return false;
  }
}

module.exports = {
  isTravisSauceConnectRunning: isTravisSauceConnectRunning
};
