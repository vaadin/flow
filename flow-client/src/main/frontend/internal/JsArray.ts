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

// Bulk JsArray helpers migrated from the JsniHelper in JsArray.java. These are
// per-mutation bulk operations. Per-element reads and writes (array[i]) are done
// inline on the arrays rather than through these helpers.

/** Appends every value (spread) onto the array, returning the new length. */
export function pushArray(array: unknown[], values: unknown[]): number {
  return array.push(...values);
}

/**
 * Splices the array at index, removing `remove` elements and inserting the
 * `add` values (spread), returning the removed elements.
 */
export function spliceArray(array: unknown[], index: number, remove: number, add: unknown[]): unknown[] {
  return array.splice(index, remove, ...add);
}

/** Empties the array. */
export function clear(array: unknown[]): void {
  array.length = 0;
}
