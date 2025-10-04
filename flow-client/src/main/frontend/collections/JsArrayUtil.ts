/*
 * Copyright 2000-2025 Vaadin Ltd.
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

/**
 * Simple TypeScript utility to validate TypeScript compilation works
 * alongside GWT. This is a minimal first step before full migration.
 *
 * This utility provides a simple array helper that can be used from
 * TypeScript code without depending on GWT collections.
 */

// Expose to window for GWT to call
declare global {
  interface Window {
    VaadinTypeScript?: {
      ArrayUtil?: typeof ArrayUtil;
    };
  }
}

export class ArrayUtil {
  /**
   * Checks if an array is empty.
   *
   * @param array the array to check
   * @returns true if the array is empty or null/undefined
   */
  static isEmpty<T>(array: T[] | null | undefined): boolean {
    return !array || array.length === 0;
  }

  /**
   * Creates a new array from varargs.
   *
   * @param values the values to include in the array
   * @returns a new array with the provided values
   */
  static of<T>(...values: T[]): T[] {
    return values;
  }

  /**
   * Removes an item from an array (mutates the array).
   *
   * @param array the array to modify
   * @param item the item to remove
   * @returns true if the item was found and removed
   */
  static remove<T>(array: T[], item: T): boolean {
    const index = array.indexOf(item);
    if (index !== -1) {
      array.splice(index, 1);
      return true;
    }
    return false;
  }

  /**
   * Clears an array (mutates the array).
   *
   * @param array the array to clear
   */
  static clear<T>(array: T[]): void {
    array.length = 0;
  }
}

// Expose ArrayUtil to window so GWT can call it
if (typeof window !== 'undefined') {
  window.VaadinTypeScript = window.VaadinTypeScript || {};
  window.VaadinTypeScript.ArrayUtil = ArrayUtil;
}
