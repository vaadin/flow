import * as Transport from 'winston-transport';
export declare type Level = 'error' | 'warn' | 'info' | 'verbose' | 'debug' | 'silly';
export declare type Options = {
    /** The minimum severity to log, defaults to 'info' */
    readonly level?: Level;
    readonly name?: string;
    readonly colorize?: boolean;
};
export declare class PolymerLogger {
    private readonly _logger;
    private readonly _transport;
    /**
     * Constructs a new instance of PolymerLogger. This creates a new internal
     * `winston` logger, which is what we use to handle most of our logging logic.
     *
     * Should generally called with getLogger() instead of calling directly.
     */
    constructor(options: Options);
    /**
     * Logs an ERROR message, if the log level allows it. These should be used
     * to give the user information about a serious error that occurred. Usually
     * used right before the process exits.
     */
    error: (...valsToLog: any[]) => void;
    /**
     * Logs a WARN message, if the log level allows it. These should be used
     * to give the user information about some unexpected issue that was
     * encountered. Usually the process is able to continue, but the user should
     * still be concerned and hopefully investigate further.
     */
    warn: (...valsToLog: any[]) => void;
    /**
     * Logs an INFO message, if the log level allows it. These should be used
     * to give the user generatl information about the process, including progress
     * updates and status messages.
     */
    info: (...valsToLog: any[]) => void;
    /**
     * Logs a DEBUG message, if the log level allows it. These should be used
     * to give the user useful information for debugging purposes. These will
     * generally only be displayed when the user is are troubleshooting an
     * issue.
     */
    debug: (...valsToLog: any[]) => void;
    /**
     * Read the instance's level from our internal logger.
     */
    /**
    * Sets a new logger level on the internal winston logger. The level dictates
    * the minimum level severity that you will log to the console.
    */
    level: string | undefined;
    /**
     * Logs a message of any level. Used internally by the public logging methods.
     */
    private _log;
}
export declare const defaultConfig: {
    level: Level;
    /**
     * Replace this to replace the default transport factor for all future
     * loggers.
     */
    transportFactory(options: Transport.TransportStreamOptions): Transport;
};
/**
 * Set all future loggers created, across the application, to be verbose.
 */
export declare function setVerbose(): void;
/**
 * Set all future loggers created, across the application, to be quiet.
 */
export declare function setQuiet(): void;
/**
 * Create a new logger with the given name label. It will inherit the global
 * level if one has been set within the application.
 *
 * @param  {string} name The name of the logger, useful for grouping messages
 * @return {PolymerLogger}
 */
export declare function getLogger(name?: string): PolymerLogger;
