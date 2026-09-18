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
const browsers = require("./browsers");
const selenium = require("./selenium");
/** WCT plugin that enables support for local browsers via Selenium. */
const plugin = (wct, pluginOptions) => {
    // The capabilities objects for browsers to run. We don't know the port
    // until `prepare`, so we've gotta hang onto them.
    let eachCapabilities = [];
    // Convert any local browser names into Webdriver capabilities objects.
    //
    // Note that we run this hook late to allow other plugins to append their
    // browsers. We don't want the default behavior (run all local browsers)
    // to kick in if someone has specified browsers via another plugin.
    const onConfigure = () => __awaiter(this, void 0, void 0, function* () {
        pluginOptions.seleniumArgs = pluginOptions.seleniumArgs || [];
        pluginOptions.javaArgs = pluginOptions.javaArgs || [];
        pluginOptions.skipSeleniumInstall =
            pluginOptions.skipSeleniumInstall || false;
        let names = browsers.normalize(pluginOptions.browsers);
        if (names.length > 0) {
            // We support comma separated browser identifiers for convenience.
            names = names.join(',').split(',');
        }
        const activeBrowsers = wct.options.activeBrowsers;
        if (activeBrowsers.length === 0 && names.length === 0) {
            names = ['all'];
        }
        // No local browsers for you :(
        if (names.length === 0) {
            return;
        }
        // Note that we **do not** append the browsers to `activeBrowsers`
        // until we've got a port chosen for the Selenium server.
        const expanded = yield browsers.expand(names, pluginOptions.browserOptions);
        wct.emit('log:debug', 'Expanded local browsers:', names, 'into capabilities:', expanded, 'with browserOptions:', pluginOptions.browserOptions);
        eachCapabilities = expanded;
        // We are careful to append these to the configuration object, even
        // though we don't know the selenium port yet. This allows WCT to give a
        // useful error if no browsers were configured.
        activeBrowsers.push.apply(activeBrowsers, expanded);
    });
    wct.hookLate('configure', function (done) {
        onConfigure().then(() => done(), (err) => done(err));
    });
    const onPrepare = () => __awaiter(this, void 0, void 0, function* () {
        if (!eachCapabilities.length) {
            return;
        }
        yield new Promise((resolve, reject) => {
            wct.emitHook('prepare:selenium', (e) => e ? reject(e) : resolve());
        });
        yield selenium.checkSeleniumEnvironment();
        let start = selenium.installAndStartSeleniumServer;
        if (pluginOptions.skipSeleniumInstall) {
            start = selenium.startSeleniumServer;
        }
        const port = yield start(wct, pluginOptions.seleniumArgs, pluginOptions.javaArgs);
        updatePort(eachCapabilities, port);
    });
    wct.hook('prepare', function (done) {
        onPrepare().then(() => done(), (err) => done(err));
    });
    // NOTE(rictic): I can't actually find the code that emits this event...
    //     There doesn't seem to be an obvious source in either wct or this
    //     plugin.
    wct.on('browser-start', (def, data, stats, browser /* TODO(rictic): what is browser here? */) => {
        if (!browser) {
            return;
        }
        browser.maximize(function (err) {
            if (err) {
                wct.emit('log:error', def.browserName + ' failed to maximize');
            }
            else {
                wct.emit('log:debug', def.browserName + ' maximized');
            }
        });
    });
};
// Utility
function updatePort(capabilities, port) {
    capabilities.forEach(function (capabilities) {
        capabilities.url = {
            hostname: '127.0.0.1',
            port: port,
        };
    });
}
module.exports = plugin;
