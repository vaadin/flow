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

// TypeScript port of com.vaadin.client.flow.TreeChangeProcessor, built alongside
// the Java version on top of the TS state tree. It applies a batch of
// server-sent JSON changes (attach/detach, map put/remove, list splice/clear,
// feature populate) to the StateTree. Changes are plain JS objects; values are
// passed through ClientJsonCodec.decodeWithoutTypeInfo (a no-op in JS).

import { assert } from './assert';
import { decodeWithoutTypeInfo } from './ClientJsonCodec';
import { JsonConstants } from './JsonConstants';
import { StateNode } from './StateNode';
import type { StateTree } from './StateTree';

type Change = Record<string, unknown>;

function isAttach(change: Change): boolean {
  return change[JsonConstants.CHANGE_TYPE] === JsonConstants.CHANGE_TYPE_ATTACH;
}

function processAttachChanges(tree: StateTree, changes: Change[]): Set<StateNode> {
  const nodes = new Set<StateNode>();
  for (const change of changes) {
    if (isAttach(change)) {
      const nodeId = change[JsonConstants.CHANGE_NODE] as number;
      if (nodeId !== tree.getRootNode().getId()) {
        const node = new StateNode(nodeId, tree);
        tree.registerNode(node);
        nodes.add(node);
      }
    }
  }
  return nodes;
}

function populateFeature(change: Change, node: StateNode): void {
  const featureId = change[JsonConstants.CHANGE_FEATURE] as number;
  if (change[JsonConstants.CHANGE_FEATURE_TYPE] as boolean) {
    // list feature
    node.getList(featureId);
  } else {
    node.getMap(featureId);
  }
}

function findProperty(change: Change, node: StateNode) {
  const nsId = change[JsonConstants.CHANGE_FEATURE] as number;
  const map = node.getMap(nsId);
  return map.getProperty(change[JsonConstants.CHANGE_MAP_KEY] as string);
}

function processPutChange(change: Change, node: StateNode): void {
  const property = findProperty(change, node);

  if (JsonConstants.CHANGE_PUT_VALUE in change) {
    property.setValue(decodeWithoutTypeInfo(change[JsonConstants.CHANGE_PUT_VALUE]));
  } else if (JsonConstants.CHANGE_PUT_NODE_VALUE in change) {
    const childId = change[JsonConstants.CHANGE_PUT_NODE_VALUE] as number;
    const child = node.getTree().getNode(childId)!;
    child.setParent(node);
    property.setValue(child);
  } else {
    assert(false, 'Put change should have either a value or a node value');
  }
}

function processRemoveChange(change: Change, node: StateNode): void {
  findProperty(change, node).removeValue();
}

function processSpliceChange(change: Change, node: StateNode): void {
  const nsId = change[JsonConstants.CHANGE_FEATURE] as number;
  const list = node.getList(nsId);

  const index = change[JsonConstants.CHANGE_SPLICE_INDEX] as number;
  const remove =
    JsonConstants.CHANGE_SPLICE_REMOVE in change ? (change[JsonConstants.CHANGE_SPLICE_REMOVE] as number) : 0;

  if (JsonConstants.CHANGE_SPLICE_ADD in change) {
    // In JS the JSON array is used as-is.
    list.splice(index, remove, change[JsonConstants.CHANGE_SPLICE_ADD] as unknown[]);
  } else if (JsonConstants.CHANGE_SPLICE_ADD_NODES in change) {
    const addNodes = change[JsonConstants.CHANGE_SPLICE_ADD_NODES] as number[];
    const tree = node.getTree();
    const add = addNodes.map((childId) => {
      const child = tree.getNode(childId)!;
      child.setParent(node);
      return child;
    });
    list.splice(index, remove, add);
  } else {
    list.splice(index, remove);
  }
}

function processClearChange(change: Change, node: StateNode): void {
  node.getList(change[JsonConstants.CHANGE_FEATURE] as number).clear();
}

function processDetachChange(tree: StateTree, node: StateNode): void {
  tree.unregisterNode(node);
  node.setParent(null);
}

/**
 * Applies a single JSON change to the tree, returning the affected node. Public
 * for testing. Mirrors TreeChangeProcessor.processChange.
 */
export function processChange(tree: StateTree, change: Change): StateNode | null {
  const type = change[JsonConstants.CHANGE_TYPE] as string;
  const nodeId = change[JsonConstants.CHANGE_NODE] as number;

  const node = tree.getNode(nodeId);
  if (node === null && tree.isResync()) {
    // Resync should not stop handling changes.
    return node;
  }

  switch (type) {
    case JsonConstants.CHANGE_TYPE_NOOP:
      populateFeature(change, node!);
      break;
    case JsonConstants.CHANGE_TYPE_SPLICE:
      processSpliceChange(change, node!);
      break;
    case JsonConstants.CHANGE_TYPE_PUT:
      processPutChange(change, node!);
      break;
    case JsonConstants.CHANGE_TYPE_REMOVE:
      processRemoveChange(change, node!);
      break;
    case JsonConstants.CHANGE_TYPE_DETACH:
      processDetachChange(tree, node!);
      break;
    case JsonConstants.CHANGE_TYPE_CLEAR:
      processClearChange(change, node!);
      break;
    default:
      assert(false, `Unsupported change type: ${type}`);
  }
  return node;
}

/**
 * Applies a batch of JSON changes to the tree: all attaches first, then the
 * rest. Returns the set of nodes addressed by the changes. Mirrors
 * TreeChangeProcessor.processChanges.
 */
export function processChanges(tree: StateTree, changes: Change[]): Set<StateNode> {
  try {
    tree.setUpdateInProgress(true);

    // Attach all nodes before doing anything else.
    const nodes = processAttachChanges(tree, changes);

    for (const change of changes) {
      if (!isAttach(change)) {
        const value = processChange(tree, change);
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
}
