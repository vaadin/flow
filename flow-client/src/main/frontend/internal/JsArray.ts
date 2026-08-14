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

// Bulk JsArray operations migrated from JsArray.java. These mirror the public
// `@JsOverlay` methods pushArray/spliceArray/clear (exported here because they
// are public in Java), implemented as free functions over native arrays since a
// TypeScript array already is the "native JS Array" that JsArray wrapped. The
// remaining public JsArray surface (get/set/push/length/splice/remove/isEmpty/
// shift/forEach) is expressible directly with native array syntax, so it needs
// no dedicated helper. The private `JsniHelper` statics behind these overlays
// are not ported.

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
