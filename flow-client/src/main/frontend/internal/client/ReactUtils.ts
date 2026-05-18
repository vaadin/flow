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

interface ReactReadyElement {
  addReadyCallback?: (name: string, callback: () => void) => void;
}

/**
 * Helpers for interacting with React-based custom elements from the binding code.
 *
 * Migrated from `com.vaadin.client.ReactUtils`. Reached from GWT-compiled code
 * via the `NativeReactUtils` JsType shim. The pure-Java `isInitialized` helper
 * was left in place — there is no browser API to delegate to.
 */
export const ReactUtils = {
  addReadyCallback(element: Element, name: string, runnable: () => void): void {
    const candidate = element as ReactReadyElement;
    if (typeof candidate.addReadyCallback === 'function') {
      candidate.addReadyCallback(name, () => runnable());
    }
  }
};
