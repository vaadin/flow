/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
 */

export interface AsyncInterface {
  // eslint-disable-next-line @typescript-eslint/ban-types
  run(fn: Function, delay?: number): number;
  cancel(handle: number): void;
}

/**
 * Not defined in the TypeScript DOM library.
 * See https://developer.mozilla.org/en-US/docs/Web/API/IdleDeadline
 */
export interface IdleDeadline {
  didTimeout: boolean;
  timeRemaining(): number;
}

/**
 * Async interface wrapper around `setTimeout`.
 */
declare namespace timeOut {
  /**
   * Returns a sub-module with the async interface providing the provided
   * delay.
   *
   * @returns An async timeout interface
   */
  function after(delay?: number): AsyncInterface;

  /**
   * Enqueues a function called in the next task.
   *
   * @returns Handle used for canceling task
   */
  // eslint-disable-next-line @typescript-eslint/ban-types
  function run(fn: Function, delay?: number): number;

  /**
   * Cancels a previously enqueued `timeOut` callback.
   */
  function cancel(handle: number): void;
}

export { timeOut };

/**
 * Async interface wrapper around `requestAnimationFrame`.
 */
declare namespace animationFrame {
  /**
   * Enqueues a function called at `requestAnimationFrame` timing.
   *
   * @returns Handle used for canceling task
   */
  function run(fn: (p0: number) => void): number;

  /**
   * Cancels a previously enqueued `animationFrame` callback.
   */
  function cancel(handle: number): void;
}

export { animationFrame };

/**
 * Async interface wrapper around `requestIdleCallback`. Falls back to
 * `setTimeout` on browsers that do not support `requestIdleCallback`.
 */
declare namespace idlePeriod {
  /**
   * Enqueues a function called at `requestIdleCallback` timing.
   *
   * @returns Handle used for canceling task
   */
  function run(fn: (p0: IdleDeadline) => void): number;

  /**
   * Cancels a previously enqueued `idlePeriod` callback.
   */
  function cancel(handle: number): void;
}

export { idlePeriod };

/**
 * Async interface for enqueuing callbacks that run at microtask timing.
 *
 * Note that microtask timing is achieved via a single `MutationObserver`,
 * and thus callbacks enqueued with this API will all run in a single
 * batch, and not interleaved with other microtasks such as promises.
 * Promises are avoided as an implementation choice for the time being
 * due to Safari bugs that cause Promises to lack microtask guarantees.
 */
declare namespace microTask {
  /**
   * Enqueues a function called at microtask timing.
   *
   * @returns Handle used for canceling task
   */
  // eslint-disable-next-line @typescript-eslint/ban-types
  function run(callback?: Function): number;

  /**
   * Cancels a previously enqueued `microTask` callback.
   */
  function cancel(handle: number): void;
}

export { microTask };
