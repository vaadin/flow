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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
const chalk = require("chalk");
const events = require("events");
const _ = require("lodash");
const clireporter_1 = require("./clireporter");
const config = require("./config");
const context_1 = require("./context");
const plugin_1 = require("./plugin");
const test_1 = require("./test");
const PACKAGE_INFO = require('../package.json');
const noopNotifier = {
    notify: () => { }
};
let updateNotifier = noopNotifier;
(function () {
    try {
        updateNotifier = require('update-notifier')({ pkg: PACKAGE_INFO });
    }
    catch (error) {
        // S'ok if we don't have update-notifier. It's optional.
    }
})();
function run(_env, args, output) {
    return __awaiter(this, void 0, void 0, function* () {
        yield wrapResult(output, _run(args, output));
    });
}
exports.run = run;
function _run(args, output) {
    return __awaiter(this, void 0, void 0, function* () {
        // If the "--version" or "-V" flag is ever present, just print
        // the current version. Useful for globally installed CLIs.
        if (args.includes('--version') || args.includes('-V')) {
            output.write(`${PACKAGE_INFO.version}\n`);
            return Promise.resolve();
        }
        // Options parsing is a two phase affair. First, we need an initial set of
        // configuration so that we know which plugins to load, etc:
        let options = config.preparseArgs(args);
        // Depends on values from the initial merge:
        options = config.merge(options, {
            output: output,
            ttyOutput: !process.env.CI && output['isTTY'] && !options.simpleOutput,
        });
        const context = new context_1.Context(options);
        if (options.skipUpdateCheck) {
            updateNotifier = noopNotifier;
        }
        // `parseArgs` merges any new configuration into `context.options`.
        yield config.parseArgs(context, args);
        yield test_1.test(context);
    });
}
// Note that we're cheating horribly here. Ideally all of this logic is within
// wct-sauce. The trouble is that we also want WCT's configuration lookup logic,
// and that's not (yet) cleanly exposed.
function runSauceTunnel(_env, args, output) {
    return __awaiter(this, void 0, void 0, function* () {
        yield wrapResult(output, _runSauceTunnel(args, output));
    });
}
exports.runSauceTunnel = runSauceTunnel;
function _runSauceTunnel(args, output) {
    return __awaiter(this, void 0, void 0, function* () {
        const cmdOptions = config.preparseArgs(args);
        const context = new context_1.Context(cmdOptions);
        const diskOptions = context.options;
        const baseOptions = (diskOptions.plugins && diskOptions.plugins['sauce']) ||
            diskOptions.sauce || {};
        const plugin = yield plugin_1.Plugin.get('sauce');
        const parser = require('nomnom');
        parser.script('wct-st');
        parser.options(_.omit(plugin.cliConfig, 'browsers', 'tunnelId'));
        const options = _.merge(baseOptions, parser.parse(args));
        const wctSauce = require('wct-sauce');
        wctSauce.expandOptions(options);
        const emitter = new events.EventEmitter();
        new clireporter_1.CliReporter(emitter, output, {});
        const tunnelId = yield new Promise((resolve, reject) => {
            wctSauce.startTunnel(options, emitter, (error, tunnelId) => error ? reject(error) : resolve(tunnelId));
        });
        output.write('\n');
        output.write('The tunnel will remain active while this process is running.\n');
        output.write('To use this tunnel for other WCT runs, export the following:\n');
        output.write('\n');
        output.write(chalk.cyan('export SAUCE_TUNNEL_ID=' + tunnelId) + '\n');
        output.write('Press CTRL+C to close the sauce tunnel\n');
    });
}
function wrapResult(output, promise) {
    return __awaiter(this, void 0, void 0, function* () {
        let error;
        try {
            yield promise;
        }
        catch (e) {
            error = e;
        }
        if (!process.env.CI) {
            updateNotifier.notify();
        }
        if (error) {
            output.write('\n');
            output.write(chalk.red(error) + '\n');
            output.write('\n');
            throw error;
        }
    });
}
