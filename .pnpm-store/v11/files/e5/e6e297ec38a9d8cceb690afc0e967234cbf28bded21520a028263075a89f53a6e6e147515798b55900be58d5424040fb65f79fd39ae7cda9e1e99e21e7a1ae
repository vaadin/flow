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
const stacky = require("stacky");
const util = require("util");
const STACKY_CONFIG = {
    indent: '    ',
    locationStrip: [
        /^https?:\/\/[^\/]+/,
        /\?[\d\.]+$/,
    ],
    unimportantLocation: [
        /^\/web-component-tester\//,
    ]
};
const STATE_ICONS = {
    passing: '✓',
    pending: '✖',
    failing: '✖',
    unknown: '?',
};
const STATE_COLORS = {
    passing: chalk.green,
    pending: chalk.yellow,
    failing: chalk.red,
    unknown: chalk.red,
    error: chalk.red,
};
const SHORT = {
    'internet explorer': 'IE',
};
const BROWSER_PAD = 24;
const STATUS_PAD = 38;
class CliReporter {
    constructor(emitter, stream, options) {
        this.prettyBrowsers = {};
        this.browserStats = {};
        this.emitter = emitter;
        this.stream = stream;
        this.options = options;
        cleankill.onInterrupt(() => {
            return new Promise((resolve) => {
                this.flush();
                resolve();
            });
        });
        emitter.on('log:error', this.log.bind(this, chalk.red));
        if (!this.options.quiet) {
            emitter.on('log:warn', this.log.bind(this, chalk.yellow));
            emitter.on('log:info', this.log.bind(this));
            if (this.options.verbose) {
                emitter.on('log:debug', this.log.bind(this, chalk.dim));
            }
        }
        emitter.on('browser-init', (browser, stats) => {
            this.browserStats[browser.id] = stats;
            this.prettyBrowsers[browser.id] = this.prettyBrowser(browser);
            this.updateStatus();
        });
        emitter.on('browser-start', (browser, data, stats) => {
            this.browserStats[browser.id] = stats;
            this.log(browser, 'Beginning tests via', chalk.magenta(data.url));
            this.updateStatus();
        });
        emitter.on('test-end', (browser, data, stats) => {
            this.browserStats[browser.id] = stats;
            if (data.state === 'failing') {
                this.writeTestError(browser, data);
            }
            else if (this.options.expanded || this.options.verbose) {
                this.log(browser, this.stateIcon(data.state), this.prettyTest(data));
            }
            this.updateStatus();
        });
        emitter.on('browser-end', (browser, error, stats) => {
            this.browserStats[browser.id] = stats;
            if (error) {
                this.log(chalk.red, browser, 'Tests failed:', error);
            }
            else {
                this.log(chalk.green, browser, 'Tests passed');
            }
        });
        emitter.on('run-end', (error) => {
            if (error) {
                this.log(chalk.red, 'Test run ended in failure:', error);
            }
            else {
                this.log(chalk.green, 'Test run ended with great success');
            }
            if (!this.options.ttyOutput) {
                this.updateStatus(true);
            }
        });
    }
    // Specialized Reporting
    updateStatus(force) {
        if (!this.options.ttyOutput && !force) {
            return;
        }
        // EXTREME TERMINOLOGY FAIL, but here's a glossary:
        //
        // stats:  An object containing test stats (total, passing, failing, etc).
        // state:  The state that the run is in (running, etc).
        // status: A string representation of above.
        const statuses = Object.keys(this.browserStats).map((browserIdStr) => {
            const browserId = parseInt(browserIdStr, 10);
            const pretty = this.prettyBrowsers[browserId];
            const stats = this.browserStats[browserId];
            let status = '';
            const counts = [stats.passing, stats.pending, stats.failing];
            if (counts[0] > 0 || counts[1] > 0 || counts[2] > 0) {
                if (counts[0] > 0) {
                    counts[0] = chalk.green(counts[0].toString());
                }
                if (counts[1] > 0) {
                    counts[1] = chalk.yellow(counts[1].toString());
                }
                if (counts[2] > 0) {
                    counts[2] = chalk.red(counts[2].toString());
                }
                status = counts.join('/');
            }
            if (stats.status === 'error') {
                status = status + (status === '' ? '' : ' ') + chalk.red('error');
            }
            return padRight(pretty + ' (' + status + ')', STATUS_PAD);
        });
        this.writeWrapped(statuses, '  ');
    }
    writeTestError(browser, data) {
        this.log(browser, this.stateIcon(data.state), this.prettyTest(data));
        const error = data.error || {};
        this.write('\n');
        let prettyMessage = error.message || error;
        if (typeof prettyMessage !== 'string') {
            prettyMessage = util.inspect(prettyMessage);
        }
        this.write(chalk.red('  ' + prettyMessage));
        if (error.stack) {
            try {
                this.write(stacky.pretty(data.error.stack, STACKY_CONFIG));
            }
            catch (err) {
                // If we couldn't extract a stack (i.e. there was no stack), the message
                // is enough.
            }
        }
        this.write('\n');
    }
    // Object Formatting
    stateIcon(state) {
        const color = STATE_COLORS[state] || STATE_COLORS['unknown'];
        return color(STATE_ICONS[state] || STATE_ICONS.unknown);
    }
    prettyTest(data) {
        const color = STATE_COLORS[data.state] || STATE_COLORS['unknown'];
        return color(data.test.join(' » ') || '<unknown test>');
    }
    prettyBrowser(browser) {
        const parts = [];
        if (browser.platform && !browser.deviceName) {
            parts.push(browser.platform);
        }
        const name = browser.deviceName || browser.browserName;
        parts.push(SHORT[name] || name);
        if (browser.version) {
            parts.push(browser.version);
        }
        if (browser.variant) {
            parts.push(`[${browser.variant}]`);
        }
        return chalk.blue(parts.join(' '));
    }
    log() {
        let values = Array.from(arguments);
        let format;
        if (_.isFunction(values[0])) {
            format = values[0];
            values = values.slice(1);
        }
        if (values[0] && values[0].browserName) {
            values[0] = padRight(this.prettyBrowser(values[0]), BROWSER_PAD);
        }
        let line = _.toArray(values)
            .map((value) => _.isString(value) ? value : util.inspect(value))
            .join(' ');
        line = line.replace(/[\s\n\r]+$/, '');
        if (format) {
            line = format(line);
        }
        this.write(line);
    }
    writeWrapped(blocks, separator) {
        if (blocks.length === 0) {
            return;
        }
        const lines = [''];
        const width = this.stream.columns || 0;
        for (const block of blocks) {
            const line = lines[lines.length - 1];
            const combined = line + separator + block;
            if (line === '') {
                lines[lines.length - 1] = block;
            }
            else if (chalk.stripColor(combined).length <= width) {
                lines[lines.length - 1] = combined;
            }
            else {
                lines.push(block);
            }
        }
        this.writeLines(['\n'].concat(lines));
        if (this.options.ttyOutput) {
            this.stream.write('\r');
            this.stream.write('\u001b[' + (lines.length + 1) + 'A');
        }
    }
    write(line) {
        this.writeLines([line]);
        this.updateStatus();
    }
    writeLines(lines) {
        for (let line of lines) {
            if (line[line.length - 1] !== '\n') {
                line = line + '\n';
            }
            if (this.options.ttyOutput) {
                line = '\u001b[J' + line;
            }
            this.stream.write(line);
        }
        this.linesWritten = lines.length;
    }
    flush() {
        if (!this.options.ttyOutput) {
            return;
        }
        // Add an extra line for padding.
        for (let i = 0; i <= this.linesWritten; i++) {
            this.stream.write('\n');
        }
    }
}
// HACK
CliReporter.CliReporter = CliReporter;
exports.CliReporter = CliReporter;
// Yeah, yeah.
function padRight(str, length) {
    let currLength = chalk.stripColor(str).length;
    while (currLength < length) {
        currLength = currLength + 1;
        str = str + ' ';
    }
    return str;
}
module.exports = CliReporter;
