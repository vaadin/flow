"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt The complete set of authors may be found
 * at http://polymer.github.io/AUTHORS.txt The complete set of contributors may
 * be found at http://polymer.github.io/CONTRIBUTORS.txt Code distributed by
 * Google as part of the polymer project is also subject to an additional IP
 * rights grant found at http://polymer.github.io/PATENTS.txt
 */
const launchpad = require("launchpad");
const util_1 = require("util");
const LAUNCHPAD_TO_SELENIUM = {
    chrome: chrome,
    canary: chrome,
    firefox: firefox,
    aurora: firefox,
    ie: internetExplorer,
    safari: safari,
};
function normalize(browsers) {
    return (browsers || []).map(function (browser) {
        if (typeof browser === 'string') {
            return browser;
        }
        return browser.browserName;
    });
}
exports.normalize = normalize;
/**
 * Expands an array of browser identifiers for locally installed browsers into
 * their webdriver capabilities objects.
 *
 * If `names` is empty, or contains `all`, all installed browsers will be used.
 */
function expand(names, browserOptions) {
    return __awaiter(this, void 0, void 0, function* () {
        if (names.indexOf('all') !== -1) {
            names = [];
        }
        const unsupported = difference(names, exports.supported());
        if (unsupported.length > 0) {
            throw new Error(`The following browsers are unsupported: ${unsupported.join(', ')}. ` +
                `(All supported browsers: ${exports.supported().join(', ')})`);
        }
        const installedByName = yield exports.detect(browserOptions);
        const installed = Object.keys(installedByName);
        // Opting to use everything?
        if (names.length === 0) {
            names = installed;
        }
        const missing = difference(names, installed);
        if (missing.length > 0) {
            throw new Error(`The following browsers were not found: ${missing.join(', ')}. ` +
                `(All installed browsers found: ${installed.join(', ')})`);
        }
        return names.map(function (n) {
            return installedByName[n];
        });
    });
}
exports.expand = expand;
/**
 * Detects any locally installed browsers that we support.
 *
 * Exported and declared as `let` variables for testabilty in wct.
 */
exports.detect = function detect(browserOptions) {
    return __awaiter(this, void 0, void 0, function* () {
        const launcher = yield util_1.promisify(launchpad.local)();
        const browsers = yield util_1.promisify(launcher.browsers)();
        const results = {};
        for (const browser of browsers) {
            if (!LAUNCHPAD_TO_SELENIUM[browser.name]) {
                continue;
            }
            const converter = LAUNCHPAD_TO_SELENIUM[browser.name];
            const convertedBrowser = converter(browser, browserOptions && browserOptions[browser.name]);
            if (convertedBrowser) {
                results[browser.name] = convertedBrowser;
            }
        }
        return results;
    });
};
/**
 * Exported and declared as `let` variables for testabilty in wct.
 *
 * @return A list of local browser names that are supported by
 *     the current environment.
 */
exports.supported = function supported() {
    return Object.keys(launchpad.local.platform)
        .filter((key) => key in LAUNCHPAD_TO_SELENIUM);
};
// Launchpad -> Selenium
/**
 * @param browser A launchpad browser definition.
 * @return A selenium capabilities object.
 */
function chrome(browser, browserOptions) {
    return {
        'browserName': 'chrome',
        'version': browser.version.match(/\d+/)[0],
        'chromeOptions': {
            'binary': browser.binPath,
            'args': browserOptions || ['start-maximized']
        }
    };
}
/**
 * @param browser A launchpad browser definition.
 * @return A selenium capabilities object.
 */
function firefox(browser, browserOptions) {
    const version = parseInt(browser.version.match(/\d+/)[0], 10);
    const marionette = version >= 47;
    return {
        'browserName': 'firefox',
        'version': `${version}`,
        'firefox_binary': browser.binPath,
        'moz:firefoxOptions': { args: browserOptions || [''] },
        'marionette': marionette
    };
}
/**
 * @param browser A launchpad browser definition.
 * @return A selenium capabilities object.
 */
function safari(browser, browserOptions) {
    // SafariDriver doesn't appear to support custom binary paths. Does Safari?
    return {
        'browserName': 'safari',
        'version': browser.version,
        // TODO(nevir): TEMPORARY.
        // https://github.com/Polymer/web-component-tester/issues/51
        'safari.options': {
            'skipExtensionInstallation': true,
        },
    };
}
/**
 * @param browser A launchpad browser definition.
 * @return A selenium capabilities object.
 */
function phantom(browser, browserOptions) {
    return {
        'browserName': 'phantomjs',
        'version': browser.version,
        'phantomjs.binary.path': browser.binPath,
    };
}
/**
 * @param browser A launchpad browser definition.
 * @return A selenium capabilities object.
 */
function internetExplorer(browser, browserOptions) {
    return {
        'browserName': 'internet explorer',
        'version': browser.version,
    };
}
/** Filter out all elements from toRemove from source. */
function difference(source, toRemove) {
    return source.filter((value) => toRemove.indexOf(value) < 0);
}
