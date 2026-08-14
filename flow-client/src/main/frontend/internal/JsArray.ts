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

// JsArray operations migrated from JsArray.java. `JsArray<T>` was a native JS
// Array wrapper, so a plain TypeScript array already is the thing it wrapped.
// The full public Java API maps as follows:
//
//   Native TypeScript array syntax (intentionally NOT wrapped here):
//     get(index)                -> array[index]
//     set(index, value)         -> array[index] = value
//     push(...values)           -> array.push(...values)
//     length()                  -> array.length
//     splice(index, remove, ...add) -> array.splice(index, remove, ...add)
//     shift()                   -> array.shift()
//     forEach(callback)         -> array.forEach(callback)
//
//   Exported free-function helpers (below), because they are genuinely more
//   than a native operation or bundle several native calls:
//     pushArray, spliceArray, clear, isEmpty, remove, removeItem
//
// The private `JsniHelper` statics behind the Java `@JsOverlay` methods are not
// ported.

/**
 * Appends every value (spread) onto the array, returning the new length.
 * Mirrors the public JsArray.pushArray overlay.
 */
export function pushArray(array: unknown[], values: unknown[]): number {
  return array.push(...values);
}

/**
 * Splices the array at index, removing `remove` elements and inserting the
 * `add` values (spread), returning the removed elements. Mirrors the public
 * JsArray.spliceArray overlay.
 */
export function spliceArray(array: unknown[], index: number, remove: number, add: unknown[]): unknown[] {
  return array.splice(index, remove, ...add);
}

/**
 * Empties the array and returns it, mirroring the public JsArray.clear overlay
 * (which returns the cleared array).
 */
export function clear(array: unknown[]): unknown[] {
  array.length = 0;
  return array;
}

/**
 * Checks if the array is empty (length === 0). Mirrors the public
 * JsArray.isEmpty overlay.
 */
export function isEmpty(array: unknown[]): boolean {
  return array.length === 0;
}

/**
 * Removes the item at the given index and returns it. Mirrors the public
 * JsArray.remove(int index) overlay (which is `splice(index, 1).get(0)`).
 */
export function remove(array: unknown[], index: number): unknown {
  return array.splice(index, 1)[0];
}

/**
 * Removes the first item that is identical (`===`, matching Java reference
 * `==`) to the given value, returning whether one was found and removed. Maps
 * to the by-value Java overload JsArray.remove(T toRemove); it is named
 * `removeItem` here because TypeScript cannot overload `remove` on a single
 * argument whose type may itself be a number.
 */
export function removeItem(array: unknown[], toRemove: unknown): boolean {
  for (let i = 0; i < array.length; i++) {
    if (array[i] === toRemove) {
      array.splice(i, 1);
      return true;
    }
  }
  return false;
}
