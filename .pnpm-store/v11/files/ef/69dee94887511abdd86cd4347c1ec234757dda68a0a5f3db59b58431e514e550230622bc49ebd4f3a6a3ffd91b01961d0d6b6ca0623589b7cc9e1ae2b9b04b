# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

<!-- ## Unreleased -->
<!-- Add unreleased changes here. -->

## [v2.1.7] - 2022-01-11
- Use an https URL rather than a git URL for launchpad, hopefully that will be more compatible with https://github.blog/2021-09-01-improving-git-protocol-security-github/

## [v2.1.6] - 2022-01-06
- Pin to a version of launchpad without CVE-2021-23330

## [v2.1.5] - 2019-07-09
- Added `javaArgs` to plugin options.

## [v2.1.4] - 2019-06-05
- Updated selenium version to 3.141.59 to support latest JDK versions.

## [v2.1.3] - 2018-10-24
- Updated postinstall.js script to specify latest geckodriver to support Firefox 63.

## [v2.1.2] - 2018-09-17
- Changed postinstall.js script to accept a SELENIUM_OVERRIDES_CONFIG environment variable to allow specifying an alternate location for a json file containing a 'selenium-overrides' key with configuration details for the `selenium.install()` step.  Thanks to @bernardoVale for contribution.

## [v2.1.1] - 2018-06-25
- Converted code from promisify-node to native promisify. (This officially drops support for Node v6)

## [v2.1.0] - 2018-01-12
- Update selenium-standalone to v6
- Update chalk to v2
- Remove overrides to selenium-standalone installation
- Add optional `browserArguments` setting to configuration
  - `browserArguments` is an object mapping browser name to an array of string arguments

## [v2.0.16] - 2017-11-16
- Update to cleankill@^2.0.0. This fixes an issue in WCT v6.4.0 where the tests would finish and the browsers would close, but the WCT process would not end. This was because the versions of cleankill diverged and so they used different event queues for handling cleanup.

## [v2.0.15] - 2017-05-09

- Update launchpad@v0.6.0


