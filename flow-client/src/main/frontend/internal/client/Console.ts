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

const LOCAL_STORAGE_FLAG = 'vaadin.browserLog';

let productionMode = false;

function shouldLog(): boolean {
  if (!productionMode) {
    return true;
  }
  try {
    return globalThis.localStorage?.getItem(LOCAL_STORAGE_FLAG) === 'true';
  } catch {
    // localStorage may be unavailable (e.g. sandboxed iframe)
    return false;
  }
}

/**
 * Helper for routed access to `window.console`. In production mode no message
 * is logged unless `localStorage['vaadin.browserLog']` is `'true'`.
 *
 * Migrated from `com.vaadin.client.Console`. Reached from GWT-compiled code
 * via the `NativeConsole` JsType shim.
 */
export const Console = {
  setProductionMode(value: boolean): void {
    productionMode = value;
  },

  debug(message: unknown): void {
    if (shouldLog()) {
      globalThis.console.debug(message);
    }
  },

  log(message: unknown): void {
    if (shouldLog()) {
      globalThis.console.log(message);
    }
  },

  warn(message: unknown): void {
    if (shouldLog()) {
      globalThis.console.warn(message);
    }
  },

  error(message: unknown): void {
    if (shouldLog()) {
      globalThis.console.error(message);
    }
  },

  /**
   * Logs an exception's stacktrace to the browser console. Always logged,
   * regardless of production mode.
   */
  reportStacktrace(exception: unknown): void {
    globalThis.console.error(exception);
  }
};
