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
import { Reactive } from './flow/reactive/Reactive';

const ELEMENT_PROPERTIES = 1;

type StateNodeLike = {
  getId(): number;
  hasFeature(id: number): boolean;
  getMap(id: number): {
    forEachProperty(callback: (property: MapPropertyLike, name: string) => void): void;
  };
};

type MapPropertyLike = {
  getName(): string;
  getValue(): unknown;
  setValue(value: unknown): void;
  getMap(): { getNode(): StateNodeLike };
};

type StateTreeLike = {
  isUpdateInProgress(): boolean;
  getNode(id: number): StateNodeLike;
  sendNodePropertySyncToServer(property: MapPropertyLike): void;
};

/**
 * Prevents client-side default property values from overriding the server's
 * initial values. Migrated from `com.vaadin.client.InitialPropertiesHandler`.
 *
 * Properties pushed from the server during the initial update are recorded;
 * any client-side sync for those exact (node, property) pairs is suppressed
 * (and the server value reapplied) so the server stays authoritative.
 *
 * Construction takes the state tree directly (rather than the registry) so the
 * TS class only depends on the already-migrated `StateTree` surface and does
 * not need to dispatch through the Java `Registry` facade.
 */
export class InitialPropertiesHandler {
  private readonly tree: StateTreeLike;
  private readonly newNodeDuringUpdate = new Set<number>();
  private readonly propertyUpdateQueue: MapPropertyLike[] = [];

  constructor(tree: StateTreeLike) {
    this.tree = tree;
  }

  flushPropertyUpdates(): void {
    if (!this.tree.isUpdateInProgress()) {
      const map = new Map<number, Map<string, unknown>>();
      this.newNodeDuringUpdate.forEach((id) => this.collectInitialProperties(id, map));
      Reactive.addPostFlushListener(() => this.doFlushPropertyUpdates(map));
    }
  }

  nodeRegistered(node: StateNodeLike): void {
    this.newNodeDuringUpdate.add(node.getId());
  }

  handlePropertyUpdate(property: MapPropertyLike): boolean {
    if (this.isNodeNewlyCreated(property.getMap().getNode())) {
      this.propertyUpdateQueue.push(property);
      return true;
    }
    return false;
  }

  private resetProperty(property: MapPropertyLike, properties: Map<number, Map<string, unknown>>): boolean {
    const ignoreProperties = properties.get(property.getMap().getNode().getId());
    if (ignoreProperties && ignoreProperties.has(property.getName())) {
      property.setValue(ignoreProperties.get(property.getName()));
      return true;
    }
    return false;
  }

  private isNodeNewlyCreated(node: StateNodeLike): boolean {
    return this.newNodeDuringUpdate.has(node.getId());
  }

  private doFlushPropertyUpdates(properties: Map<number, Map<string, unknown>>): void {
    this.newNodeDuringUpdate.clear();
    while (this.propertyUpdateQueue.length > 0) {
      const property = this.propertyUpdateQueue.shift()!;
      if (!this.resetProperty(property, properties)) {
        this.tree.sendNodePropertySyncToServer(property);
      }
      // Flush after every property in case a sync triggered other defaults.
      // See https://github.com/vaadin/flow/issues/2304
      Reactive.flush();
    }
  }

  private collectInitialProperties(id: number, properties: Map<number, Map<string, unknown>>): void {
    const node = this.tree.getNode(id);
    if (node.hasFeature(ELEMENT_PROPERTIES)) {
      const map = new Map<string, unknown>();
      node.getMap(ELEMENT_PROPERTIES).forEachProperty((property, name) => map.set(name, property.getValue()));
      properties.set(id, map);
    }
  }
}
