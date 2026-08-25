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

// Implementations migrated from ReactUtils.java.

/**
 * Add a callback to the react component that is called when the component
 * initialization is ready for binding flow.
 *
 * @param element - react component element
 * @param name - name of container to bind to
 * @param runnable - callback function runnable
 */
export function addReadyCallback(element: Element, name: string, callback: () => void): void {
  const el = element as unknown as {
    addReadyCallback?: (name: string, callback: () => void) => void;
  };
  if (el.addReadyCallback) {
    el.addReadyCallback(name, callback);
  }
}

/**
 * Check if the react element is initialized and functional.
 *
 * @param elementLookup - react element lookup supplier
 * @returns `true` if Flow binding can already be done
 *
 * Mirrors ReactUtils.isInitialized.
 */
export function isInitialized(elementLookup: () => Element | null): boolean {
  return elementLookup() !== null;
}
