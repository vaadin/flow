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

/** The set of model properties the server allows the client to update. */
export class UpdatableModelProperties {
  private readonly properties: Set<string>;

  constructor(properties: string[]) {
    this.properties = new Set(properties);
  }

  /** Tests whether the given property is updatable. */
  isUpdatableProperty(property: string): boolean {
    return this.properties.has(property);
  }
}
