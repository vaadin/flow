/**
 * CleanKill hooks the interrupt handler, and provides callbacks for your code
 * to cleanly shut down before the process exits.
 */
/**
 * The type of a cleankill interrupt handler.
 */
export declare type Handler = () => Promise<void>;
/**
 * Register a handler to occur on SIGINT. The handler must return a promise if
 * it has any asynchronous work to do. The process will be terminated once
 * all handlers complete. If a handler throws or the promise it returns rejects
 * then the process will be terminated immediately.
 */
export declare function onInterrupt(handler: Handler): void;
/**
 * Waits for all promises to settle, then rejects with the first error, if any.
 */
export declare function promiseAllStrict(promises: Promise<any>[]): Promise<void>;
/**
 * Call all interrupt handlers, and call the callback when they all complete.
 *
 * Clears the list of interrupt handlers.
 */
export declare function close(): Promise<void>;
/**
 * Calls all interrupt handlers, then exits with exit code 0.
 *
 * If called more than once it skips waiting for the interrupt handlers to
 * finish and exits with exit code 1. If there are any errors with interrupt
 * handlers, the process exits immediately with exit code 2.
 *
 * This function is called when a SIGINT is received.
 */
export declare function interrupt(): void;
