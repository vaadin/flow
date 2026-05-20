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

interface JsonObjectLike {
  keys(): string[];
  get(key: string): unknown;
}

/**
 * Map of constant values received from the server. Migrated from
 * `com.vaadin.client.flow.ConstantPool`.
 */
export class ConstantPool {
  private readonly constants = new Map<string, unknown>();

  /** Imports new constants into this pool. */
  importFromJson(json: JsonObjectLike): void {
    for (const key of json.keys()) {
      // Server-side never re-publishes the same key with a different value.
      this.constants.set(key, json.get(key));
    }
  }

  /** Checks whether this constant pool contains a value for the given key. */
  has(key: string): boolean {
    return this.constants.has(key);
  }

  /** Gets the constant with a given key. */
  get(key: string): unknown {
    return this.constants.get(key);
  }
}
