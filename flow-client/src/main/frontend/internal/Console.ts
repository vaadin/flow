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

// Helper migrated from Console.java, registered on
// window.Vaadin.Flow.internal.Console by registerInternals; the Java method
// delegates here. The browser-console logging itself, its GWT.isScript()
// gating and the uncaught-exception-handler machinery stay in Java. Also
// bundled to ES5 for the HtmlUnit used by GwtTests.

/**
 * Whether the localStorage override flag `vaadin.browserLog` is set to "true".
 * Used to force browser-console logging on in production mode. Returns false if
 * localStorage is unavailable or inaccessible.
 */
export function isLocalStorageFlagEnabled(): boolean {
  try {
    return !!window.localStorage && window.localStorage.getItem('vaadin.browserLog') === 'true';
  } catch (e) {
    // localStorage might not be available or accessible
    return false;
  }
}
