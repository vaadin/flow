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

// TypeScript port of com.vaadin.client.flow.StateNode (with its node-unregister
// event/listener), on top of the TS node features. The slice of StateTree that
// StateNode needs is declared here as a contract that StateTree satisfies. The
// original Java Class<?>-keyed nodeData map becomes a map keyed by JS constructor
// function.

import { assert } from './assert';
import type { EventRemover } from './reactive/reactive';
import type { MapProperty } from './nodefeature/MapProperty';
import type { JsonValue, NodeFeature, NodeFeatureNode } from './nodefeature/NodeFeature';
import { NodeList } from './nodefeature/NodeList';
import { NodeMap } from './nodefeature/NodeMap';

/** Fired when a node is unregistered; mirrors NodeUnregisterEvent. */
export class NodeUnregisterEvent {
  private readonly node: StateNode;

  constructor(node: StateNode) {
    this.node = node;
  }

  getNode(): StateNode {
    return this.node;
  }
}

/** Listener for node unregistration; mirrors NodeUnregisterListener. */
export type NodeUnregisterListener = (event: NodeUnregisterEvent) => void;

/** The slice of StateTree that StateNode and the node features use. */
export interface StateTree {
  getNode(id: number): StateNode | null;
  getFeatureDebugName(id: number): string;
  isActive(node: StateNode): boolean;
  sendNodePropertySyncToServer(property: MapProperty): void;
}

type Constructor<T> = abstract new (...args: never[]) => T;

/** A client-side representation of a server-side state node; mirrors StateNode.java. */
export class StateNode implements NodeFeatureNode {
  private readonly tree: StateTree;

  private readonly id: number;

  private parent: StateNode | null = null;

  private unregistered = false;

  private readonly features = new Map<number, NodeFeature>();

  private readonly unregisterListeners = new Set<NodeUnregisterListener>();

  private readonly domNodeSetListeners = new Set<(node: StateNode) => boolean>();

  private readonly nodeData = new Map<unknown, unknown>();

  private domNode: Node | null = null;

  constructor(id: number, tree: StateTree) {
    this.id = id;
    this.tree = tree;
  }

  getTree(): StateTree {
    return this.tree;
  }

  getId(): number {
    return this.id;
  }

  getList(id: number): NodeList {
    let feature = this.features.get(id);
    if (feature === undefined) {
      feature = new NodeList(id, this);
      this.features.set(id, feature);
    }
    return feature as NodeList;
  }

  getMap(id: number): NodeMap {
    let feature = this.features.get(id);
    if (feature === undefined) {
      feature = new NodeMap(id, this);
      this.features.set(id, feature);
    }
    return feature as NodeMap;
  }

  hasFeature(id: number): boolean {
    return this.features.has(id);
  }

  forEachFeature(callback: (feature: NodeFeature, id: number) => void): void {
    this.features.forEach((feature, id) => callback(feature, id));
  }

  getDebugJson(): JsonValue {
    const object: Record<string, JsonValue> = {};

    this.forEachFeature((feature, featureId) => {
      const json = feature.getDebugJson();
      if (json !== null && json !== undefined) {
        object[this.tree.getFeatureDebugName(featureId)] = json;
      }
    });

    return object;
  }

  isUnregistered(): boolean {
    return this.unregistered;
  }

  unregister(): void {
    assert(this.tree.getNode(this.getId()) === null, 'Node should no longer be findable from the tree');
    assert(!this.unregistered, 'Node is already unregistered');
    this.unregistered = true;

    const event = new NodeUnregisterEvent(this);

    const copy = new Set(this.unregisterListeners);
    copy.forEach((l) => l(event));
    // Don't refer to the listeners which won't be ever used again
    this.unregisterListeners.clear();
  }

  addUnregisterListener(listener: NodeUnregisterListener): EventRemover {
    this.unregisterListeners.add(listener);
    return {
      remove: () => {
        this.unregisterListeners.delete(listener);
      }
    };
  }

  getDomNode(): Node | null {
    return this.domNode;
  }

  setDomNode(node: Node | null): void {
    assert(this.domNode === null || node === null, 'StateNode already has a DOM node');
    this.domNode = node;

    const copy = new Set(this.domNodeSetListeners);
    copy.forEach((listener) => {
      if (listener(this)) {
        this.domNodeSetListeners.delete(listener);
      }
    });
  }

  addDomNodeSetListener(listener: (node: StateNode) => boolean): EventRemover {
    this.domNodeSetListeners.add(listener);
    return {
      remove: () => {
        this.domNodeSetListeners.delete(listener);
      }
    };
  }

  getParent(): StateNode | null {
    return this.parent;
  }

  setParent(parent: StateNode | null): void {
    this.parent = parent;
  }

  setNodeData(object: object): void {
    this.nodeData.set(object.constructor, object);
  }

  getNodeData<T>(clazz: Constructor<T>): T | null {
    const value = this.nodeData.get(clazz);
    return value === undefined ? null : (value as T);
  }

  clearNodeData(object: object): void {
    this.nodeData.delete(object.constructor);
  }
}
