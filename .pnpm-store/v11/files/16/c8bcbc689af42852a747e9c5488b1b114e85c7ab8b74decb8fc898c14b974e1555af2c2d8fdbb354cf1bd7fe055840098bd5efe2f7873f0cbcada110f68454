# sauce-connect-launcher

[![Build Status](https://api.travis-ci.org/bermi/sauce-connect-launcher.svg)](http://travis-ci.org/bermi/sauce-connect-launcher)  [![Dependency Status](https://david-dm.org/bermi/sauce-connect-launcher.svg)](https://david-dm.org/bermi/sauce-connect-launcher) [![](http://img.shields.io/npm/v/sauce-connect-launcher.svg) ![](http://img.shields.io/npm/dm/sauce-connect-launcher.svg)](https://www.npmjs.org/package/sauce-connect-launcher)

A library to download and launch Sauce Connect.

## Installation

```sh
npm install sauce-connect-launcher
```

If you wish to also download Sauce Connect at this stage, rather than on first run, use the `SAUCE_CONNECT_DOWNLOAD_ON_INSTALL` environment variable.

```sh
SAUCE_CONNECT_DOWNLOAD_ON_INSTALL=true npm install
```

## Usage


### Simple Usage

```javascript
var sauceConnectLauncher = require('sauce-connect-launcher');

sauceConnectLauncher({
  username: 'bermi',
  accessKey: '12345678-1234-1234-1234-1234567890ab'
}, function (err, sauceConnectProcess) {
  if (err) {
    console.error(err.message);
    return;
  }
  console.log("Sauce Connect ready");

  sauceConnectProcess.close(function () {
    console.log("Closed Sauce Connect process");
  })
});
```

### Advanced Usage

```javascript

var sauceConnectLauncher = require('sauce-connect-launcher'),
  options = {

    // Sauce Labs username.  You can also pass this through the
    // SAUCE_USERNAME environment variable
    username: 'bermi',

    // Sauce Labs access key.  You can also pass this through the
    // SAUCE_ACCESS_KEY environment variable
    accessKey: '12345678-1234-1234-1234-1234567890ab',

    // Log output from the `sc` process to stdout?
    verbose: false,

    // Enable verbose debugging (optional)
    verboseDebugging: false,

    // Together with verbose debugging will output HTTP headers as well (optional)
    vv: false,

    // Port on which Sauce Connect's Selenium relay will listen for
    // requests. Default 4445. (optional)
    port: null,

    // Proxy host and port that Sauce Connect should use to connect to
    // the Sauce Labs cloud. e.g. "localhost:1234" (optional)
    proxy: null,

    // Change sauce connect logfile location (optional)
    logfile: null,

    // Period to log statistics about HTTP traffic in seconds (optional)
    logStats: null,

    // Maximum size before which the logfile is rotated (optional)
    maxLogsize: null,

    // Set to true to perform checks to detect possible misconfiguration or problems (optional)
    doctor: null,

    // Identity the tunnel for concurrent tunnels (optional)
    tunnelIdentifier: null,

    // an array or comma-separated list of regexes whose matches
    // will not go through the tunnel. (optional)
    fastFailRegexps: null,

    // an array or comma-separated list of domains that will not go
    // through the tunnel. (optional)
    directDomains: null,

    // A function to optionally write sauce-connect-launcher log messages.
    // e.g. `console.log`.  (optional)
    logger: function (message) {},

    // an optional suffix to be appended to the `readyFile` name.
    // useful when running multiple tunnels on the same machine,
    // such as in a continuous integration environment. (optional)
    readyFileId: null,

    // retry to establish a tunnel multiple times. (optional)
    connectRetries: 0

    // time to wait between connection retries in ms. (optional)
    connectRetryTimeout: 2000

    // retry to download the sauce connect archive multiple times. (optional)
    downloadRetries: 0

    // time to wait between download retries in ms. (optional)
    downloadRetryTimeout: 1000

    // path to a sauce connect executable (optional)
    // by default the latest sauce connect version is downloaded
    exe: null

    // keep sc running after the node process exited, this means you need to close
    // the process manually once you are done using the pidfile
    // Attention: This works only on macOS and linux at the moment
    detached: null

    // specify a connect version instead of fetching the latest version, this currently
    // does not support checksum verification
    connectVersion: 'latest'
  };

sauceConnectLauncher(options, function (err, sauceConnectProcess) {
  console.log("Started Sauce Connect Process");
  sauceConnectProcess.close(function () {
    console.log("Closed Sauce Connect process");
  });
});

```

Additional Sauce Connect options not specified above can still be passed.  `additionalArg: "foo"` options will be converted to `--addtional-arg foo` args (camelCase to kebab-case). Additional option starting with dash `"-additionalArg": "foo"` will be passed as it is `-additionalArg foo`. Arrays will be `join()`ed (like `directDomains`) and boolean options will be passed as flags.  See [Sauce Connect's docs](https://docs.saucelabs.com/reference/sauce-connect/) for a full list of arguments.

### Credentials

You can pass the Sauce Labs credentials as `SAUCE_USERNAME` and `SAUCE_ACCESS_KEY` environment variables. (reccommended)

You can also create a user.json file in your current working directory with the username and key

user.json
```
{"username": "bermi", "accessKey": "12345678-1234-1234-1234-1234567890ab"}
```

### Sauce Connect Version

You can override the default Sauce Connect version with the `SAUCE_CONNECT_VERSION` environment variable.

```sh
$ SAUCE_CONNECT_VERSION=4.2 node myTestApp.js
```


## Development

Clone the repository and run

```
make setup
```

You will be prompted for Sauce Labs credentials (used for testing).  Run
```
make dev
```
to start the watcher.


## Testing

```
npm test
```

or

```
make test
```

## License

(The MIT License)

Copyright (c) 2013 Bermi Ferrer &lt;bermi@bermilabs.com&gt;

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
