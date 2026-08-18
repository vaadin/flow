/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// TypeScript port of com.vaadin.client.Console: the engine's logging facade.
// Nothing is logged to the browser console in production mode, unless the
// localStorage flag `vaadin.browserLog` is set to "true". Engine code must log
// through this module rather than calling `console` directly, so the production
// suppression applies everywhere; ApplicationConfiguration.setProductionMode
// feeds the flag in.

// Mirrors the static Console.isProductionMode field.
let isProductionMode = false;

/**
 * Whether the localStorage override flag `vaadin.browserLog` is set to "true".
 * Used to force browser-console logging on in production mode. Returns false if
 * localStorage is unavailable or inaccessible.
 *
 * Private in Console.java, so module-local here; exercised through the public
 * logging methods rather than imported directly.
 *
 * @return true if the flag is set to "true", false otherwise
 */
function isLocalStorageFlagEnabled(): boolean {
  try {
    return !!window.localStorage && window.localStorage.getItem('vaadin.browserLog') === 'true';
  } catch (e) {
    // localStorage might not be available or accessible
    return false;
  }
}

/**
 * Whether logging to the browser console should be enabled: true if either not
 * in production mode, or the `vaadin.browserLog` override flag is set. Mirrors
 * Console.shouldLogToBrowserConsole.
 *
 * @return true if browser console logging should be enabled
 */
function shouldLogToBrowserConsole(): boolean {
  if (!isProductionMode) {
    return true;
  }
  // Check localStorage for the override flag in production mode.
  return isLocalStorageFlagEnabled();
}

/**
 * The engine's logging facade; mirrors the static Console.java. The log methods
 * are no-ops in production mode unless the `vaadin.browserLog` localStorage flag
 * is set, so that a production application does not write to the browser
 * console.
 */
export const Console = {
  /**
   * Changes logger behavior, making it skip all browser logging for production
   * mode. Mirrors Console.setProductionMode.
   *
   * @param productionMode if an application is in the production mode or not
   */
  setProductionMode(productionMode: boolean): void {
    isProductionMode = productionMode;
  },

  /**
   * Logs the message using the debug log level, unless suppressed.
   *
   * @param message the message to log
   */
  debug(message: unknown): void {
    if (shouldLogToBrowserConsole()) {
      console.debug(message);
    }
  },

  /**
   * Logs the message using the info log level, unless suppressed.
   *
   * @param message the message to log
   */
  log(message: unknown): void {
    if (shouldLogToBrowserConsole()) {
      console.log(message);
    }
  },

  /**
   * Logs the message using the warning log level, unless suppressed.
   *
   * @param message the message to log
   */
  warn(message: unknown): void {
    if (shouldLogToBrowserConsole()) {
      console.warn(message);
    }
  },

  /**
   * Logs the message using the error log level, unless suppressed.
   *
   * @param message the message to log
   */
  error(message: unknown): void {
    if (shouldLogToBrowserConsole()) {
      console.error(message);
    }
  },

  /**
   * Logs the stack trace of an exception to the browser console. The exception
   * is rethrown asynchronously (after the current task) so the browser reports
   * it through its global error handler with the highest possible fidelity.
   * Mirrors Console.reportStacktrace; the GWT version deferred the throw to
   * bypass GWT's own uncaught-exception handling, which has no equivalent here,
   * so a plain deferred rethrow is the faithful port. Not gated by production
   * mode, matching Console.java.
   *
   * @param exception the exception for which
   */
  reportStacktrace(exception: unknown): void {
    window.setTimeout(() => {
      throw exception;
    }, 0);
  }
};
