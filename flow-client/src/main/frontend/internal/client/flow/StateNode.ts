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

// TypeScript port of com.vaadin.client.flow.StateNode, on top of the TS node
// features and the ported StateTree. The original Java Class<?>-keyed nodeData
// map becomes a map keyed by JS constructor function.

import { assert } from '../../assert';
import type { EventRemover } from '../../EventRemover';
import type { JsonValue, NodeFeature, NodeFeatureNode } from './nodefeature/NodeFeature';
import { NodeList } from './nodefeature/NodeList';
import { NodeMap } from './nodefeature/NodeMap';
import { NodeUnregisterEvent } from './NodeUnregisterEvent';
import type { NodeUnregisterListener } from './NodeUnregisterListener';
import type { StateTree } from './StateTree';

type Constructor<T> = abstract new (...args: never[]) => T;

/**
 * A client-side representation of a server-side state node.
 */
export class StateNode implements NodeFeatureNode {
  readonly #tree: StateTree;

  readonly #id: number;

  #parent: StateNode | null = null;

  #unregistered = false;

  readonly #features = new Map<number, NodeFeature>();

  readonly #unregisterListeners = new Set<NodeUnregisterListener>();

  readonly #domNodeSetListeners = new Set<(node: StateNode) => boolean>();

  readonly #nodeData = new Map<unknown, unknown>();

  #domNode: Node | null = null;

  /**
   * Creates a new state node.
   *
   * @param id - the id of the node
   * @param tree - the state tree that the node belongs to
   */
  constructor(id: number, tree: StateTree) {
    this.#id = id;
    this.#tree = tree;
  }

  /**
   * Gets the state tree that this node belongs to.
   *
   * @returns the state tree
   */
  getTree(): StateTree {
    return this.#tree;
  }

  /**
   * Gets the id of this state node.
   *
   * @returns the id
   */
  getId(): number {
    return this.#id;
  }

  /**
   * Gets the node list with the given id. Creates a new node list if one
   * doesn't already exist.
   *
   * @param id - the id of the list
   * @returns the list with the given id
   */
  getList(id: number): NodeList {
    let feature = this.#features.get(id);
    if (feature === undefined) {
      feature = new NodeList(id, this);
      this.#features.set(id, feature);
    }

    assert(feature instanceof NodeList, 'Feature is not a NodeList');

    return feature;
  }

  /**
   * Gets the node map with the given id. Creates a new map if one doesn't
   * already exist.
   *
   * @param id - the id of the map
   * @returns the map with the given id
   */
  getMap(id: number): NodeMap {
    let feature = this.#features.get(id);
    if (feature === undefined) {
      feature = new NodeMap(id, this);
      this.#features.set(id, feature);
    }

    assert(feature instanceof NodeMap, 'Feature is not a NodeMap');

    return feature;
  }

  /**
   * Checks whether this node has a feature with the given id.
   *
   * @param id - the id of the feature
   * @returns `true` if this node has the given feature; otherwise `false`
   */
  hasFeature(id: number): boolean {
    return this.#features.has(id);
  }

  /**
   * Iterates all features in this node.
   *
   * @param callback - the callback to invoke for each feature
   */
  forEachFeature(callback: (feature: NodeFeature, id: number) => void): void {
    this.#features.forEach((feature, id) => callback(feature, id));
  }

  /**
   * Gets a JSON object representing the contents of this node. Only intended
   * for debugging purposes.
   *
   * @returns a JSON representation
   */
  getDebugJson(): JsonValue {
    const object: Record<string, JsonValue> = {};

    this.forEachFeature((feature, featureId) => {
      const json = feature.getDebugJson();
      if (json !== null && json !== undefined) {
        object[this.#tree.getFeatureDebugName(featureId)] = json;
      }
    });

    return object;
  }

  /**
   * Checks whether this node has been unregistered.
   *
   * @see {@link StateTree.unregisterNode}
   *
   * @returns `true` if this node has been unregistered; `false` if the node is
   *          still registered
   */
  isUnregistered(): boolean {
    return this.#unregistered;
  }

  /**
   * Unregisters this node, causing all registered node unregister listeners to
   * be notified.
   *
   * @see {@link addUnregisterListener}
   */
  unregister(): void {
    assert(this.#tree.getNode(this.getId()) === null, 'Node should no longer be findable from the tree');
    assert(!this.#unregistered, 'Node is already unregistered');
    this.#unregistered = true;

    const event = new NodeUnregisterEvent(this);

    const copy = new Set(this.#unregisterListeners);
    copy.forEach((l) => l(event));
    // Don't refer to the listeners which won't be ever used again
    this.#unregisterListeners.clear();
  }

  /**
   * Adds a listener that will be notified when this node is unregistered.
   *
   * @param listener - the node unregister listener to add
   * @returns an event remover that can be used for removing the added listener
   */
  addUnregisterListener(listener: NodeUnregisterListener): EventRemover {
    this.#unregisterListeners.add(listener);
    return {
      remove: () => {
        this.#unregisterListeners.delete(listener);
      }
    };
  }

  /**
   * Gets the DOM node associated with this state node.
   *
   * @returns the DOM node, or `null` if no DOM node has been associated with
   *          this state node
   */
  getDomNode(): Node | null {
    return this.#domNode;
  }

  /**
   * Sets the DOM node associated with this state node.
   *
   * @param node - the associated DOM node
   */
  setDomNode(node: Node | null): void {
    assert(this.#domNode === null || node === null, 'StateNode already has a DOM node');
    this.#domNode = node;

    const copy = new Set(this.#domNodeSetListeners);
    copy.forEach((listener) => {
      if (listener(this)) {
        this.#domNodeSetListeners.delete(listener);
      }
    });
  }

  /**
   * Adds a listener to get a notification when the DOM Node is set for this
   * {@link StateNode}.
   *
   * The listener return value is used to decide whether the listener should be
   * removed immediately if it returns `true`.
   *
   * @param listener - listener to add
   * @returns an event remover that can be used for removing the added listener
   */
  addDomNodeSetListener(listener: (node: StateNode) => boolean): EventRemover {
    this.#domNodeSetListeners.add(listener);
    return {
      remove: () => {
        this.#domNodeSetListeners.delete(listener);
      }
    };
  }

  /**
   * Get the parent {@link StateNode} if set.
   *
   * @returns parent state node
   */
  getParent(): StateNode | null {
    return this.#parent;
  }

  /**
   * Set the parent {@link StateNode} for this node.
   *
   * @param parent - the parent state node
   */
  setParent(parent: StateNode | null): void {
    this.#parent = parent;
  }

  /**
   * Stores the `object` in the {@link StateNode} instance.
   *
   * The `object` may represent any kind of data. This data can be retrieved
   * later on via the {@link getNodeData} providing the class of the object. So
   * make sure you are using some custom type for your data to avoid clash with
   * other types.
   *
   * @see {@link getNodeData}
   *
   * @param object - the object to store
   * @typeParam T - the type of the node data to set
   */
  setNodeData(object: object): void {
    this.#nodeData.set(object.constructor, object);
  }

  /**
   * Gets the object previously stored by the {@link setNodeData} by its type.
   *
   * If there is no stored object with the given type then the method returns
   * `null`.
   *
   * @param clazz - the type of the object to get
   * @typeParam T - the type of the node data to get
   * @returns the object by its `clazz`
   */
  getNodeData<T>(clazz: Constructor<T>): T | null {
    const value = this.#nodeData.get(clazz);
    return value === undefined ? null : (value as T);
  }

  /**
   * Removes the `object` from the stored data.
   *
   * @param object - the object to remove
   * @typeParam T - the type of the object to remove
   */
  clearNodeData(object: object): void {
    this.#nodeData.delete(object.constructor);
  }
}
