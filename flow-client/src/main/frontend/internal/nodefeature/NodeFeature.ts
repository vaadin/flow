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

// TypeScript port of com.vaadin.client.flow.nodefeature.NodeFeature, built
// alongside the Java version. StateNode is not ported yet, so the slice the
// node features need is declared here as a contract the future TS StateNode
// will satisfy at cutover.

import type { MapPropertyNode } from './MapProperty';

/** A JSON value, mirroring elemental.json.JsonValue in loose form. */
export type JsonValue = unknown;

/** The slice of StateNode needed by node features. */
export interface NodeFeatureNode extends MapPropertyNode {
  getDebugJson(): JsonValue;
}

/**
 * Holder of the actual data in a state node. The state node data is isolated
 * into different features of related data.
 */
export abstract class NodeFeature {
  readonly #id: number;

  readonly #node: NodeFeatureNode;

  /**
   * Creates a new feature.
   *
   * @param id - the id of the feature
   * @param node - the node that the feature belongs to
   */
  constructor(id: number, node: NodeFeatureNode) {
    this.#id = id;
    this.#node = node;
  }

  /**
   * Gets the id of this feature.
   *
   * @returns the id
   */
  getId(): number {
    return this.#id;
  }

  /**
   * Gets the node of this feature.
   *
   * @returns the node
   */
  getNode(): NodeFeatureNode {
    return this.#node;
  }

  /**
   * Gets a JSON object representing the contents of this feature. Only
   * intended for debugging purposes.
   *
   * @returns a JSON representation
   */
  abstract getDebugJson(): JsonValue;

  /**
   * Convert the feature values into a {@link JsonValue} using provided
   * `converter` for the values stored in the feature (i.e. primitive types,
   * StateNodes).
   *
   * @param converter - converter to convert values stored in the feature
   * @returns resulting converted value
   */
  abstract convert(converter: (value: unknown) => JsonValue): JsonValue;

  /**
   * Helper for getting a JSON representation of a child value.
   *
   * @param value - the child value
   * @returns the JSON representation
   */
  protected getAsDebugJson(value: unknown): JsonValue {
    if (isStateNode(value)) {
      return value.getDebugJson();
    }
    return value;
  }
}

/**
 * Tells whether the given value is a state node, i.e. exposes a
 * `getDebugJson` method. Mirrors the Java `value instanceof StateNode` check
 * used by {@link NodeFeature.getAsDebugJson} before StateNode is ported.
 */
function isStateNode(value: unknown): value is NodeFeatureNode {
  return (
    value !== null &&
    typeof value === 'object' &&
    typeof (value as { getDebugJson?: unknown }).getDebugJson === 'function'
  );
}
