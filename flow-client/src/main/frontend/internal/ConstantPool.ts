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

// TypeScript port of com.vaadin.client.flow.ConstantPool, built alongside the
// Java version.

import { assert } from './assert';

/** Map of constant values received from the server; mirrors ConstantPool.java. */
export class ConstantPool {
  private readonly constants = new Map<string, unknown>();

  /**
   * Imports new constants into this pool from a JSON object mapping constant
   * keys to constant values.
   */
  importFromJson(json: Record<string, unknown>): void {
    for (const key of Object.keys(json)) {
      assert(!this.constants.has(key), 'ConstantPool already contains a value for the imported key');
      const value = json[key];
      assert(value !== null && value !== undefined, 'ConstantPool constant value must not be null');
      this.constants.set(key, value);
    }
  }

  /** Checks whether this constant pool contains a value for the given key. */
  has(key: string): boolean {
    return this.constants.has(key);
  }

  /**
   * Gets the constant with a given key, or <code>undefined</code> if there is
   * no constant with the given key. Returns any type to make it easier to use
   * constants as typed values.
   */
  get<T>(key: string): T {
    return this.constants.get(key) as T;
  }
}
