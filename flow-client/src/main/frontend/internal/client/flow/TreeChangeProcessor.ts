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
import { StateNode } from './StateNode';

// Mirrors com.vaadin.flow.shared.JsonConstants.* literals used in tree-change
// messages from the server.
const CHANGE_NODE = 'node';
const CHANGE_TYPE = 'type';
const CHANGE_TYPE_NOOP = 'empty';
const CHANGE_TYPE_ATTACH = 'attach';
const CHANGE_TYPE_DETACH = 'detach';
const CHANGE_TYPE_SPLICE = 'splice';
const CHANGE_TYPE_PUT = 'put';
const CHANGE_TYPE_REMOVE = 'remove';
const CHANGE_TYPE_CLEAR = 'clear';
const CHANGE_FEATURE = 'feat';
const CHANGE_FEATURE_TYPE = 'featType';
const CHANGE_MAP_KEY = 'key';
const CHANGE_SPLICE_ADD_NODES = 'addNodes';
const CHANGE_SPLICE_ADD = 'add';
const CHANGE_SPLICE_REMOVE = 'remove';
const CHANGE_SPLICE_INDEX = 'index';
const CHANGE_PUT_VALUE = 'value';
const CHANGE_PUT_NODE_VALUE = 'nodeValue';

type Change = Record<string, unknown>;

type StateTreeLike = {
  isUpdateInProgress(): boolean;
  setUpdateInProgress(updateInProgress: boolean): void;
  setResync(resync: boolean): void;
  isResync(): boolean;
  getNode(id: number): StateNode | null;
  getRootNode(): StateNode;
  registerNode(node: StateNode): void;
  unregisterNode(node: StateNode): void;
};

/**
 * Updates a state tree based on a JSON array of changes received from the
 * server. Migrated from `com.vaadin.client.flow.TreeChangeProcessor`.
 *
 * `decodeWithoutTypeInfo` is inlined: in compiled JS the elemental.json
 * accessors collapse to the same primitive the JSON value already is, so the
 * historical unwrap is a no-op here.
 */
export const TreeChangeProcessor = {
  processChanges(tree: StateTreeLike, changes: Change[]): Set<StateNode> {
    if (tree.isUpdateInProgress()) {
      throw new Error('Previous tree change processing has not completed');
    }
    try {
      tree.setUpdateInProgress(true);
      // Attach all nodes before doing anything else.
      const nodes = processAttachChanges(tree, changes);
      // Then process all non-attach changes.
      for (const change of changes) {
        if (!isAttach(change)) {
          const value = TreeChangeProcessor.processChange(tree, change);
          if (value !== null) {
            nodes.add(value);
          }
        }
      }
      return nodes;
    } finally {
      tree.setUpdateInProgress(false);
      tree.setResync(false);
    }
  },

  processChange(tree: StateTreeLike, change: Change): StateNode | null {
    const type = change[CHANGE_TYPE] as string;
    const nodeId = change[CHANGE_NODE] as number;
    const node = tree.getNode(nodeId);
    if (node === null && tree.isResync()) {
      // Resync should not stop handling changes.
      return node;
    }
    if (node === null) {
      throw new Error('No attached node found');
    }

    switch (type) {
      case CHANGE_TYPE_NOOP:
        populateFeature(change, node);
        break;
      case CHANGE_TYPE_SPLICE:
        processSpliceChange(change, node);
        break;
      case CHANGE_TYPE_PUT:
        processPutChange(change, node);
        break;
      case CHANGE_TYPE_REMOVE:
        processRemoveChange(change, node);
        break;
      case CHANGE_TYPE_DETACH:
        processDetachChange(node);
        break;
      case CHANGE_TYPE_CLEAR:
        processClearChange(change, node);
        break;
      default:
        throw new Error(`Unsupported change type: ${type}`);
    }
    return node;
  }
};

function isAttach(change: Change): boolean {
  return change[CHANGE_TYPE] === CHANGE_TYPE_ATTACH;
}

function processAttachChanges(tree: StateTreeLike, changes: Change[]): Set<StateNode> {
  const nodes = new Set<StateNode>();
  for (const change of changes) {
    if (isAttach(change)) {
      const nodeId = change[CHANGE_NODE] as number;
      if (nodeId !== tree.getRootNode().getId()) {
        const node = new StateNode(nodeId, tree);
        tree.registerNode(node);
        nodes.add(node);
      }
    }
  }
  return nodes;
}

function processDetachChange(node: StateNode): void {
  (node as unknown as { getTree(): StateTreeLike }).getTree().unregisterNode(node);
  node.setParent(null);
}

function populateFeature(change: Change, node: StateNode): void {
  if (!(CHANGE_FEATURE_TYPE in change)) {
    throw new Error("Change doesn't contain feature type. Don't know how to populate feature");
  }
  const featureId = change[CHANGE_FEATURE] as number;
  if (change[CHANGE_FEATURE_TYPE] as boolean) {
    node.getList(featureId);
  } else {
    node.getMap(featureId);
  }
}

function processPutChange(change: Change, node: StateNode): void {
  const property = findProperty(change, node);
  if (CHANGE_PUT_VALUE in change) {
    property.setValue(change[CHANGE_PUT_VALUE]);
  } else if (CHANGE_PUT_NODE_VALUE in change) {
    const childId = change[CHANGE_PUT_NODE_VALUE] as number;
    const child = (node as unknown as { getTree(): StateTreeLike }).getTree().getNode(childId);
    if (child === null) {
      throw new Error(`No node found with id ${childId}`);
    }
    child.setParent(node);
    property.setValue(child);
  } else {
    throw new Error(`Change should have either value or nodeValue property: ${JSON.stringify(change)}`);
  }
}

function processRemoveChange(change: Change, node: StateNode): void {
  findProperty(change, node).removeValue();
}

type MapPropertyLike = { setValue(v: unknown): void; removeValue(): void };

function findProperty(change: Change, node: StateNode): MapPropertyLike {
  const nsId = change[CHANGE_FEATURE] as number;
  const key = change[CHANGE_MAP_KEY] as string;
  return node.getMap(nsId).getProperty(key) as unknown as MapPropertyLike;
}

function processSpliceChange(change: Change, node: StateNode): void {
  const nsId = change[CHANGE_FEATURE] as number;
  const list = node.getList(nsId);
  const index = change[CHANGE_SPLICE_INDEX] as number;
  const remove = (CHANGE_SPLICE_REMOVE in change ? change[CHANGE_SPLICE_REMOVE] : 0) as number;

  if (CHANGE_SPLICE_ADD in change) {
    const addJson = change[CHANGE_SPLICE_ADD] as unknown[];
    // decodeWithoutTypeInfo on each element is a no-op in JS (the JsonValue
    // accessors collapse to the primitive the value already is).
    list.spliceArray(index, remove, addJson);
  } else if (CHANGE_SPLICE_ADD_NODES in change) {
    const addNodes = change[CHANGE_SPLICE_ADD_NODES] as number[];
    const add: StateNode[] = [];
    const tree = (node as unknown as { getTree(): StateTreeLike }).getTree();
    for (const childId of addNodes) {
      const child = tree.getNode(childId);
      if (child === null) {
        throw new Error(`No child node found with id ${childId}`);
      }
      child.setParent(node);
      add.push(child);
    }
    list.spliceArray(index, remove, add);
  } else {
    list.spliceRemove(index, remove);
  }
}

function processClearChange(change: Change, node: StateNode): void {
  const nsId = change[CHANGE_FEATURE] as number;
  node.getList(nsId).clear();
}
