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

/** Base class for all state node features; mirrors NodeFeature.java. */
export abstract class NodeFeature {
  private readonly id: number;

  private readonly node: NodeFeatureNode;

  constructor(id: number, node: NodeFeatureNode) {
    this.id = id;
    this.node = node;
  }

  getId(): number {
    return this.id;
  }

  getNode(): NodeFeatureNode {
    return this.node;
  }

  abstract getDebugJson(): JsonValue;

  abstract convert(converter: (value: unknown) => JsonValue): JsonValue;

  protected getAsDebugJson(value: unknown): JsonValue {
    if (isStateNode(value)) {
      return value.getDebugJson();
    }
    return value;
  }
}

function isStateNode(value: unknown): value is NodeFeatureNode {
  return (
    value !== null &&
    typeof value === 'object' &&
    typeof (value as { getDebugJson?: unknown }).getDebugJson === 'function'
  );
}
