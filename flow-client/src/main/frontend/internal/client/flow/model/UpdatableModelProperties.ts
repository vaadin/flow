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

// TypeScript port of com.vaadin.client.flow.model.UpdatableModelProperties.
// Stored in a StateNode via setNodeData when there is any data;
// SimpleElementBindingStrategy uses it to decide whether a Polymer
// model-property update should be sent to the server.

import type { StateNode } from '../StateNode';

/**
 * The storage class for set of updatable model properties.
 *
 * This class is stored inside a {@link StateNode} via
 * {@link StateNode.setNodeData} if there is any data to store at all.
 * Once it's stored in the {@link StateNode} the code which sends updates to the
 * server side when a polymer property is updated uses this data to detect
 * whether server expects the update to be sent(see
 * `SimpleElementBindingStrategy`).
 */
export class UpdatableModelProperties {
  readonly #properties: Set<string>;

  /**
   * Creates a new instance of storage class based on given
   * `properties`.
   *
   * @param properties - updatable properties array
   */
  constructor(properties: string[]) {
    this.#properties = new Set(properties);
  }

  /**
   * Tests whether the `property` is updatable.
   *
   * @param property - the property to test
   * @returns `true` if property is updatable
   */
  isUpdatableProperty(property: string): boolean {
    return this.#properties.has(property);
  }
}
