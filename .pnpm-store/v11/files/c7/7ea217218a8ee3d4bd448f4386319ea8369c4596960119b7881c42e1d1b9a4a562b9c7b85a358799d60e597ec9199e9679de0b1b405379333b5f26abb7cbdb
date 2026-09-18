"use strict";
/**
 * @license
 * Copyright (c) 2014 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const cleankill = require("cleankill");
const _ = require("lodash");
const wd = require("wd");
// Browser abstraction, responsible for spinning up a browser instance via wd.js
// and executing runner.html test files passed in options.files
class BrowserRunner {
    /**
     * @param emitter The emitter to send updates about test progress to.
     * @param def A BrowserDef describing and defining the browser to be run.
     *     Includes both metadata and a method for connecting/launching the
     *     browser.
     * @param options WCT options.
     * @param url The url of the generated index.html file that the browser should
     *     point at.
     * @param waitFor Optional. If given, we won't try to start/connect to the
     *     browser until this promise resolves. Used for serializing access to
     *     Safari webdriver, which can only have one instance running at once.
     */
    constructor(emitter, def, options, url, waitFor) {
        this.emitter = emitter;
        this.def = def;
        this.options = options;
        this.timeout = options.testTimeout;
        this.emitter = emitter;
        this.url = url;
        this.stats = { status: 'initializing' };
        this.donePromise = new Promise((resolve, reject) => {
            this._resolve = resolve;
            this._reject = reject;
        });
        waitFor = waitFor || Promise.resolve();
        waitFor.then(() => {
            this.browser = wd.remote(this.def.url);
            // never retry selenium commands
            this.browser.configureHttp({ retries: -1 });
            cleankill.onInterrupt(() => {
                return new Promise((resolve) => {
                    if (!this.browser) {
                        return resolve();
                    }
                    this.donePromise.then(() => resolve(), () => resolve());
                    this.done('Interrupting');
                });
            });
            this.browser.on('command', (method, context) => {
                emitter.emit('log:debug', this.def, chalk.cyan(method), context);
            });
            this.browser.on('http', (method, path, data) => {
                if (data) {
                    emitter.emit('log:debug', this.def, chalk.magenta(method), chalk.cyan(path), data);
                }
                else {
                    emitter.emit('log:debug', this.def, chalk.magenta(method), chalk.cyan(path));
                }
            });
            this.browser.on('connection', (code, message, error) => {
                emitter.emit('log:warn', this.def, 'Error code ' + code + ':', message, error);
            });
            this.emitter.emit('browser-init', this.def, this.stats);
            // Make sure that we are passing a pristine capabilities object to
            // webdriver. None of our screwy custom properties!
            const webdriverCapabilities = _.clone(this.def);
            delete webdriverCapabilities.id;
            delete webdriverCapabilities.url;
            delete webdriverCapabilities.sessionId;
            // Reusing a session?
            if (this.def.sessionId) {
                this.browser.attach(this.def.sessionId, (error) => {
                    this._init(error, this.def.sessionId);
                });
            }
            else {
                this.browser.init(webdriverCapabilities, this._init.bind(this));
            }
        });
    }
    _init(error, sessionId) {
        if (!this.browser) {
            return; // When interrupted.
        }
        if (error) {
            // TODO(nevir): BEGIN TEMPORARY CHECK.
            // https://github.com/Polymer/web-component-tester/issues/51
            if (this.def.browserName === 'safari' && error.data) {
                // debugger;
                try {
                    const data = JSON.parse(error.data);
                    if (data.value && data.value.message &&
                        /Failed to connect to SafariDriver/i.test(data.value.message)) {
                        error = 'Until Selenium\'s SafariDriver supports ' +
                            'Safari 6.2+, 7.1+, & 8.0+, you must\n' +
                            'manually install it. Follow the steps at:\n' +
                            'https://github.com/SeleniumHQ/selenium/' +
                            'wiki/SafariDriver#getting-started';
                    }
                }
                catch (error) {
                    // Show the original error.
                }
            }
            // END TEMPORARY CHECK
            this.done(error.data || error);
        }
        else {
            this.sessionId = sessionId;
            this.startTest();
            this.extendTimeout();
        }
    }
    startTest() {
        const paramDelim = (this.url.indexOf('?') === -1 ? '?' : '&');
        const extra = `${paramDelim}cli_browser_id=${this.def.id}`;
        this.browser.get(this.url + extra, (error) => {
            if (error) {
                this.done(error.data || error);
            }
            else {
                this.extendTimeout();
            }
        });
    }
    onEvent(event, data) {
        this.extendTimeout();
        if (event === 'browser-start') {
            // Always assign, to handle re-runs (no browser-init).
            this.stats = {
                status: 'running',
                passing: 0,
                pending: 0,
                failing: 0,
            };
        }
        else if (event === 'test-end') {
            this.stats[data.state] = this.stats[data.state] + 1;
        }
        if (event === 'browser-end' || event === 'browser-fail') {
            this.done(data);
        }
        else {
            this.emitter.emit(event, this.def, data, this.stats, this.browser);
        }
    }
    done(error) {
        // No quitting for you!
        if (this.options.persistent) {
            return;
        }
        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
        }
        // Don't double-quit.
        if (!this.browser) {
            return;
        }
        const browser = this.browser;
        this.browser = null;
        this.stats.status = error ? 'error' : 'complete';
        if (!error && this.stats.failing > 0) {
            error = this.stats.failing + ' failed tests';
        }
        this.emitter.emit('browser-end', this.def, error, this.stats, this.sessionId, browser);
        // Nothing to quit.
        if (!this.sessionId) {
            error ? this._reject(error) : this._resolve();
        }
        browser.quit((quitError) => {
            if (quitError) {
                this.emitter.emit('log:warn', this.def, 'Failed to quit:', quitError.data || quitError);
            }
            if (error) {
                this._reject(error);
            }
            else {
                this._resolve();
            }
        });
    }
    extendTimeout() {
        if (this.options.persistent) {
            return;
        }
        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
        }
        this.timeoutId = setTimeout(() => {
            this.done('Timed out');
        }, this.timeout);
    }
    quit() {
        this.done('quit was called');
    }
}
// HACK
BrowserRunner.BrowserRunner = BrowserRunner;
exports.BrowserRunner = BrowserRunner;
module.exports = BrowserRunner;
