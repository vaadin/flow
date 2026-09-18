/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */
'use strict';
Object.defineProperty(exports, "__esModule", { value: true });
const util_1 = require("util");
const triple_beam_1 = require("triple-beam");
const winston = require("winston");
const logform_1 = require("logform");
const plylogPrettify = logform_1.format((info, opts) => {
    if (info[triple_beam_1.SPLAT]) {
        for (const splat of info[triple_beam_1.SPLAT]) {
            info.message += '\n' + util_1.inspect(splat, false, opts.depth || null, opts.colorize);
        }
    }
    info[triple_beam_1.MESSAGE] = `${info.level}:${info.message}`;
    return info;
});
class PolymerLogger {
    /**
     * Constructs a new instance of PolymerLogger. This creates a new internal
     * `winston` logger, which is what we use to handle most of our logging logic.
     *
     * Should generally called with getLogger() instead of calling directly.
     */
    constructor(options) {
        options = Object.assign({ colorize: true }, options);
        const formats = [];
        if (options.colorize) {
            formats.push(winston.format.colorize());
        }
        formats.push(winston.format.align());
        formats.push(plylogPrettify({ colorize: options.colorize }));
        this._transport = exports.defaultConfig.transportFactory({
            level: options.level || 'info',
            format: winston.format.combine(...formats),
        });
        this._logger = winston.createLogger({ transports: [this._transport] });
        this.error = this._log.bind(this, 'error');
        this.warn = this._log.bind(this, 'warn');
        this.info = this._log.bind(this, 'info');
        this.debug = this._log.bind(this, 'debug');
    }
    /**
     * Read the instance's level from our internal logger.
     */
    get level() {
        return this._transport.level;
    }
    /**
     * Sets a new logger level on the internal winston logger. The level dictates
     * the minimum level severity that you will log to the console.
     */
    set level(newLevel) {
        this._transport.level = newLevel;
    }
    /**
     * Logs a message of any level. Used internally by the public logging methods.
     */
    _log(_level, _msg, _metadata) {
        this._logger.log.apply(this._logger, arguments);
    }
}
exports.PolymerLogger = PolymerLogger;
exports.defaultConfig = {
    level: 'info',
    /**
     * Replace this to replace the default transport factor for all future
     * loggers.
     */
    transportFactory(options) {
        return new winston.transports.Console(options);
    }
};
/**
 * Set all future loggers created, across the application, to be verbose.
 */
function setVerbose() {
    exports.defaultConfig.level = 'debug';
}
exports.setVerbose = setVerbose;
;
/**
 * Set all future loggers created, across the application, to be quiet.
 */
function setQuiet() {
    exports.defaultConfig.level = 'error';
}
exports.setQuiet = setQuiet;
/**
 * Create a new logger with the given name label. It will inherit the global
 * level if one has been set within the application.
 *
 * @param  {string} name The name of the logger, useful for grouping messages
 * @return {PolymerLogger}
 */
function getLogger(name) {
    return new PolymerLogger({
        level: exports.defaultConfig.level,
        name: name,
    });
}
exports.getLogger = getLogger;
//# sourceMappingURL=index.js.map