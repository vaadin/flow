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

type StateNodeLike = any;

/**
 * Abstract base for state-node features (NodeMap, NodeList, etc.).
 * Migrated from `com.vaadin.client.flow.nodefeature.NodeFeature`.
 */
export abstract class NodeFeature {
  readonly id: number;
  readonly node: StateNodeLike;

  constructor(id: number, node: StateNodeLike) {
    this.id = id;
    this.node = node;
  }

  getId(): number {
    return this.id;
  }

  getNode(): StateNodeLike {
    return this.node;
  }

  abstract getDebugJson(): unknown;

  abstract convert(converter: (value: unknown) => unknown): unknown;

  /**
   * Returns the debug JSON for a child value. If the value is a StateNode
   * (duck-typed via getDebugJson), delegates to it; otherwise returns the
   * value verbatim (matches `WidgetUtil.crazyJsoCast`).
   */
  protected getAsDebugJson(value: unknown): unknown {
    if (
      value != null &&
      typeof value === 'object' &&
      typeof (value as { getDebugJson?: () => unknown }).getDebugJson === 'function'
    ) {
      return (value as { getDebugJson(): unknown }).getDebugJson();
    }
    return value;
  }
}
