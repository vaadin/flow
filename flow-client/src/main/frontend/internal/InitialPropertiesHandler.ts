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

// TypeScript port of com.vaadin.client.InitialPropertiesHandler, built alongside
// the Java version on top of the TS reactive core. It prevents client-side
// default property values of newly created nodes from overriding the initial
// values the server sent: property updates for a node created during a server
// update are queued, and on flush each is either reset to the server's initial
// value or sent to the server (if the server had no initial value for it).
//
// The Registry/StateTree it talks to are not ported yet, so the slices it needs
// are declared here as contracts the future TS Registry/StateTree satisfy at
// cutover; this is the real implementation of the InitialPropertiesHandler
// contract that StateTree.ts already declares.

import { Reactive } from './reactive/reactive';
import { NodeFeatures } from './nodefeature/NodeFeatures';

/** The slice of MapProperty the handler reads and resets. */
interface InitialPropertiesProperty {
  getName(): string;
  getValue(): unknown;
  setValue(value: unknown): void;
  getMap(): { getNode(): InitialPropertiesNode };
}

/** The slice of NodeMap the handler iterates. */
interface InitialPropertiesMap {
  forEachProperty(callback: (property: InitialPropertiesProperty, name: string) => void): void;
}

/** The slice of StateNode the handler reads. */
interface InitialPropertiesNode {
  getId(): number;
  hasFeature(featureId: number): boolean;
  getMap(featureId: number): InitialPropertiesMap;
}

/** The slice of StateTree the handler uses. */
interface InitialPropertiesTree {
  isUpdateInProgress(): boolean;
  getNode(id: number): InitialPropertiesNode | null;
  sendNodePropertySyncToServer(property: InitialPropertiesProperty): void;
}

/** The slice of Registry the handler uses. */
interface InitialPropertiesRegistry {
  getStateTree(): InitialPropertiesTree;
}

/**
 * Handles server initial property values so client-side defaults don't override
 * them; mirrors InitialPropertiesHandler.java.
 */
export class InitialPropertiesHandler {
  private readonly registry: InitialPropertiesRegistry;

  private readonly newNodeDuringUpdate = new Set<number>();

  private readonly propertyUpdateQueue: InitialPropertiesProperty[] = [];

  constructor(registry: InitialPropertiesRegistry) {
    this.registry = registry;
  }

  /**
   * Flushes the collected property update queue. Supposed to be called at the
   * end of tree change processing.
   */
  flushPropertyUpdates(): void {
    if (!this.registry.getStateTree().isUpdateInProgress()) {
      const properties = new Map<number, Map<string, unknown>>();
      this.newNodeDuringUpdate.forEach((node) => this.collectInitialProperties(node, properties));
      Reactive.addPostFlushListener(() => this.doFlushPropertyUpdates(properties));
    }
  }

  /** Notifies the handler about a newly registered node. */
  nodeRegistered(node: InitialPropertiesNode): void {
    this.newNodeDuringUpdate.add(node.getId());
  }

  /**
   * Handles a property update before it is sent to the server. Returns true if
   * the update is handled here (queued and possibly sent later), false if the
   * caller should send it normally.
   */
  handlePropertyUpdate(property: InitialPropertiesProperty): boolean {
    if (this.isNodeNewlyCreated(property.getMap().getNode())) {
      this.propertyUpdateQueue.push(property);
      return true;
    }
    return false;
  }

  private resetProperty(property: InitialPropertiesProperty, properties: Map<number, Map<string, unknown>>): boolean {
    const ignoreProperties = properties.get(property.getMap().getNode().getId());
    if (ignoreProperties !== undefined && ignoreProperties.has(property.getName())) {
      property.setValue(ignoreProperties.get(property.getName()));
      return true;
    }
    return false;
  }

  private isNodeNewlyCreated(node: InitialPropertiesNode): boolean {
    return this.newNodeDuringUpdate.has(node.getId());
  }

  private doFlushPropertyUpdates(properties: Map<number, Map<string, unknown>>): void {
    this.newNodeDuringUpdate.clear();
    while (this.propertyUpdateQueue.length > 0) {
      const property = this.propertyUpdateQueue.shift()!;
      if (!this.resetProperty(property, properties)) {
        this.registry.getStateTree().sendNodePropertySyncToServer(property);
      }
      /*
       * Do flush after each property update. There may be several properties
       * and it looks like a property update may trigger default values of other
       * properties back. See https://github.com/vaadin/flow/issues/2304
       */
      Reactive.flush();
    }
  }

  private collectInitialProperties(id: number, properties: Map<number, Map<string, unknown>>): void {
    const node = this.registry.getStateTree().getNode(id);
    if (node !== null && node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
      const map = new Map<string, unknown>();
      node
        .getMap(NodeFeatures.ELEMENT_PROPERTIES)
        .forEachProperty((property, name) => map.set(name, property.getValue()));
      properties.set(id, map);
    }
  }
}
