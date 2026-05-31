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
import type { NodeFeature } from './nodefeature/NodeFeature';
import { NodeList } from './nodefeature/NodeList';
import { NodeMap } from './nodefeature/NodeMap';
import { NodeUnregisterEvent } from './NodeUnregisterEvent';

type StateTreeLike = any;

type DomNodeLike = any;

type NodeUnregisterListener = any;

type DomNodeSetListener = (node: StateNodeLike) => any;

type StateNodeLike = any;

/**
 * Client-side representation of a server-side state node. Migrated from
 * `com.vaadin.client.flow.StateNode`. `nodeData` is keyed by string.
 */
export class StateNode {
  readonly tree: StateTreeLike;
  readonly id: number;
  private parent: StateNode | null = null;
  private unregisteredFlag = false;
  private readonly features = new Map<number, NodeFeature>();
  private readonly unregisterListeners = new Set<NodeUnregisterListener>();
  private readonly domNodeSetListeners = new Set<DomNodeSetListener>();
  private readonly nodeData = new Map<string, unknown>();
  private domNode: DomNodeLike = null;

  constructor(id: number, tree: StateTreeLike) {
    this.id = id;
    this.tree = tree;
  }

  getTree(): StateTreeLike {
    return this.tree;
  }

  getId(): number {
    return this.id;
  }

  getList(id: number): NodeList {
    let feature = this.features.get(id);
    if (!feature) {
      feature = new NodeList(id, this);
      this.features.set(id, feature);
    }
    return feature as NodeList;
  }

  getMap(id: number): NodeMap {
    let feature = this.features.get(id);
    if (!feature) {
      feature = new NodeMap(id, this);
      this.features.set(id, feature);
    }
    return feature as NodeMap;
  }

  hasFeature(id: number): boolean {
    return this.features.has(id);
  }

  forEachFeature(callback: (feature: NodeFeature, featureId: number) => void): void {
    this.features.forEach((feature, featureId) => callback(feature, featureId));
  }

  getDebugJson(): unknown {
    const result: Record<string, unknown> = {};
    this.forEachFeature((feature, featureId) => {
      const json = feature.getDebugJson();
      if (json != null) {
        const name = this.tree.getFeatureDebugName(featureId);
        result[name as string] = json;
      }
    });
    return result;
  }

  isUnregistered(): boolean {
    return this.unregisteredFlag;
  }

  unregister(): void {
    this.unregisteredFlag = true;
    const event = new NodeUnregisterEvent(this);
    const copy = Array.from(this.unregisterListeners);
    copy.forEach((l) => {
      if (typeof l === 'function') {
        l(event);
      } else if (l && typeof l.onUnregister === 'function') {
        l.onUnregister(event);
      }
    });
    this.unregisterListeners.clear();
  }

  addUnregisterListener(listener: NodeUnregisterListener): { remove(): void } {
    this.unregisterListeners.add(listener);
    return {
      remove: (): void => {
        this.unregisterListeners.delete(listener);
      }
    };
  }

  getDomNode(): DomNodeLike {
    return this.domNode;
  }

  setDomNode(node: DomNodeLike): void {
    this.domNode = node;
    const copy = Array.from(this.domNodeSetListeners);
    copy.forEach((listener) => {
      if (listener(this) === true) {
        this.domNodeSetListeners.delete(listener);
      }
    });
  }

  addDomNodeSetListener(listener: DomNodeSetListener): { remove(): void } {
    this.domNodeSetListeners.add(listener);
    return {
      remove: (): void => {
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

  setNodeData(key: string, object: unknown): void {
    this.nodeData.set(key, object);
  }

  getNodeData(key: string): unknown {
    return this.nodeData.get(key);
  }

  clearNodeData(key: string): void {
    this.nodeData.delete(key);
  }
}
