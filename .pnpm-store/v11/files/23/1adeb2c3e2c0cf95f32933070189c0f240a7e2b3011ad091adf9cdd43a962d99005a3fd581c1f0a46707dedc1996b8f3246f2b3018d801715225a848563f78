# Launchpad

[![Greenkeeper badge](https://badges.greenkeeper.io/bitovi/launchpad.svg)](https://greenkeeper.io/)
[![Build Status](https://travis-ci.org/bitovi/launchpad.svg)](https://travis-ci.org/bitovi/launchpad)

You can launch browsers! With NodeJS!

* __Local browsers__ for MacOS, Windows and Linux (like) operating systems
* __[BrowserStack](http://browserstack.com)__ browsers using the BrowserStack API

## API

The general API for any launcher (`<type>`) looks like this:

```js
var launch = require('launchpad');
launch.<type>(configuration, function(error, launcher) {
  launcher.browsers(function(error, browsers) {
    // -> List of available browsers with version
  });

  launcher(url, configuration, function(error, instance) {
    instance // -> A browser instance
    instance.id // -> unique instance id
    instance.stop(callback) // -> Stop the instance
    instance.status(callback) // -> Get status information about the instance
  });

  launcher.<browsername>(url, function(error, instance) {
    // Same as above
  });
});
```

## Local launchers

Local launchers look up all currently installed browsers (unless limited by LAUNCHPAD_BROWSERS - see below for details) and allow you to start new browser processes.

```js
// Launch a local browser
launch.local(function(err, local) {
  local.browsers(function(error, browsers) {
    // -> List of all browsers found locally with version
  });
  
  local.firefox('http://url', function(err, instance) {
    // An instance is an event emitter
    instance.on('stop', function() {
      console.log('Terminated local firefox');
    });
  });
});
```

### Environment variables impacting local browsers detection

By default Launchpad looks up all installed browsers. To speed-up this process you can define the following env variables:
  * `LAUNCHPAD_BROWSERS` - comma delimited list of browsers you want to use, e.g. `LAUNCHPAD_BROWSERS=chrome,firefox,opera`. Other browsers will not be detected even if they are installed.
  * `LAUNCHPAD_<browser>` - specifies where given browser is installed so that Launchpad does not need to look for it, e.g.
    `LAUNCHPAD_CHROME=/usr/bin/chromium`

The following browser names are recognized: `chrome`, `firefox`, `safari`, `ie`, `edge`, `opera`, `canary`, `aurora`, `electron`, `phantom`, `nodeWebKit`.
Not all platforms support all browsers - see [platform](lib/local/platform) for details.

## Browserstack

BrowserStack is a great cross-browser testing tool and offers API access to any account that is on a monthly plan.
Launchpad allows you to start BrowserStack workers through its API like this:

```js
launch.browserstack({
    username : 'user',
    password : 'password'
  },
  function(err, browserstack) {
    browserstack.browsers(function(error, browsers) {
      // -> List of all Browserstack browsers
    });
    
    browserstack.ie('http://url', function(err, instance) {
      // Shut the instance down after 5 seconds
      setTimeout(function() {
        instance.stop(function (err) {
          if(err) {
            console.log(err);
          }
          console.log('Browser instance has stopped');
        });
      }, 5000);
  });
});
```

Behind the scenes we have the [node-browserstack](https://github.com/scottgonzalez/node-browserstack)
module do all the work (API calls) for us.

<!-- space -->
