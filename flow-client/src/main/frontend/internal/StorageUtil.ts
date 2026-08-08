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

// HTML Storage helpers migrated from StorageUtil.java.

/** Gets an item value from the local storage, or null if absent. */
export function getLocalItem(key: string): string | null {
  return window.localStorage.getItem(key);
}

/** Sets an item value in the local storage. */
export function setLocalItem(key: string, value: string): void {
  window.localStorage.setItem(key, value);
}

/** Gets an item value from the session storage, or null if absent. */
export function getSessionItem(key: string): string | null {
  return window.sessionStorage.getItem(key);
}

/** Sets an item value in the session storage. */
export function setSessionItem(key: string, value: string): void {
  window.sessionStorage.setItem(key, value);
}
