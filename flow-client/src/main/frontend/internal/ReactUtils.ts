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

// Implementations migrated from ReactUtils.java, registered on
// window.Vaadin.Flow.internal.ReactUtils by registerInternals; the Java methods
// delegate here. Also bundled to ES5 for the HtmlUnit used by GwtTests.

/**
 * Adds a callback to the React component that is invoked once it is ready for
 * Flow binding. No-op if the element exposes no addReadyCallback. The Java
 * caller passes an exception-guarded ($entry) callback.
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
 * Whether the element looked up by the given supplier already exists. Mirrors
 * ReactUtils.isInitialized.
 */
export function isInitialized(elementLookup: () => Element | null): boolean {
  return elementLookup() !== null;
}
