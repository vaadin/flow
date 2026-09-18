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

import { inspect } from 'util';
import { MESSAGE, SPLAT } from 'triple-beam';
import * as winston from 'winston';
import { format ,  TransformableInfo } from 'logform';
import * as Transport from 'winston-transport';

export type Level = 'error'|  'warn' | 'info'| 'verbose'| 'debug'| 'silly';
export type Options = {
  /** The minimum severity to log, defaults to 'info' */
  readonly level?: Level;
  readonly name?: string;
  readonly colorize?: boolean;
}

const plylogPrettify = format((info: TransformableInfo, opts: any) => {
  if (info[SPLAT]) {
    for (const splat of info[SPLAT]) {
      info.message += '\n' + inspect(splat, false, opts.depth || null, opts.colorize);
    }
  }
  info[MESSAGE] = `${info.level}:${info.message}`;
  return info;
});

export class PolymerLogger {
  private readonly _logger: winston.Logger;
  private readonly _transport: Transport;

  /**
   * Constructs a new instance of PolymerLogger. This creates a new internal
   * `winston` logger, which is what we use to handle most of our logging logic.
   *
   * Should generally called with getLogger() instead of calling directly.
   */
  constructor(options: Options) {
    options = Object.assign({colorize: true}, options);

    const formats = [];
    if (options.colorize) {
      formats.push(winston.format.colorize());
    }
    formats.push(winston.format.align());
    formats.push(plylogPrettify({ colorize: options.colorize }));
    this._transport = defaultConfig.transportFactory({
      level: options.level || 'info',
      format: winston.format.combine(...formats),
    });

    this._logger = winston.createLogger({transports: [this._transport]});

    this.error = this._log.bind(this, 'error');
    this.warn = this._log.bind(this, 'warn');
    this.info = this._log.bind(this, 'info');
    this.debug = this._log.bind(this, 'debug');
  }
  /**
   * Logs an ERROR message, if the log level allows it. These should be used
   * to give the user information about a serious error that occurred. Usually
   * used right before the process exits.
   */
  error: (...valsToLog:any[]) => void;
  /**
   * Logs a WARN message, if the log level allows it. These should be used
   * to give the user information about some unexpected issue that was
   * encountered. Usually the process is able to continue, but the user should
   * still be concerned and hopefully investigate further.
   */
  warn: (...valsToLog:any[]) => void;
  /**
   * Logs an INFO message, if the log level allows it. These should be used
   * to give the user generatl information about the process, including progress
   * updates and status messages.
   */
  info: (...valsToLog:any[]) => void;
  /**
   * Logs a DEBUG message, if the log level allows it. These should be used
   * to give the user useful information for debugging purposes. These will
   * generally only be displayed when the user is are troubleshooting an
   * issue.
   */
  debug: (...valsToLog:any[]) => void;

  /**
   * Read the instance's level from our internal logger.
   */
  get level(): string|undefined {
    return this._transport.level;
  }

  /**
   * Sets a new logger level on the internal winston logger. The level dictates
   * the minimum level severity that you will log to the console.
   */
  set level(newLevel: string|undefined) {
    this._transport.level = newLevel;
  }

  /**
   * Logs a message of any level. Used internally by the public logging methods.
   */
  private _log(_level: Level, _msg: string, _metadata?: any) {
    this._logger.log.apply(this._logger, arguments);
  }

}

export const defaultConfig = {
  level: 'info' as Level,

  /**
   * Replace this to replace the default transport factor for all future
   * loggers.
   */
  transportFactory(options: Transport.TransportStreamOptions): Transport {
    return new winston.transports.Console(options);
  }
}

/**
 * Set all future loggers created, across the application, to be verbose.
 */
export function setVerbose() {
  defaultConfig.level = 'debug';
};

/**
 * Set all future loggers created, across the application, to be quiet.
 */
export function setQuiet() {
  defaultConfig.level = 'error';
}

/**
 * Create a new logger with the given name label. It will inherit the global
 * level if one has been set within the application.
 *
 * @param  {string} name The name of the logger, useful for grouping messages
 * @return {PolymerLogger}
 */
export function getLogger(name?: string): PolymerLogger {
  return new PolymerLogger({
    level: defaultConfig.level,
    name: name,
  });
}

