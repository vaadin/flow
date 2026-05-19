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

// Reusable helpers for mocha tests that exercise migrated flow-client
// modules. See MIGRATION_PLAN.md P2.
//
// Three areas:
//
//   - UIDL change-object builders. Mirror the per-key constants from
//     `com.vaadin.flow.shared.JsonConstants` so test cases read like
//     "the server sent a SPLICE change adding [..] at index N".
//   - RPC message builders for the client→server direction.
//   - DOM fixture helper that creates a detached or document-attached
//     element and registers a cleanup hook with mocha's `afterEach`.

// --- UIDL change constants (mirrors com.vaadin.flow.shared.JsonConstants) ---

export const CHANGE = Object.freeze({
  NODE: 'node',
  TYPE: 'type',
  FEATURE: 'feat',
  FEATURE_TYPE: 'featType',
  MAP_KEY: 'key',
  PUT_VALUE: 'value',
  PUT_NODE_VALUE: 'nodeValue',
  SPLICE_INDEX: 'index',
  SPLICE_ADD: 'add',
  SPLICE_ADD_NODES: 'addNodes',
  SPLICE_REMOVE: 'remove'
});

export const CHANGE_TYPE = Object.freeze({
  NOOP: 'empty',
  ATTACH: 'attach',
  DETACH: 'detach',
  SPLICE: 'splice',
  PUT: 'put',
  REMOVE: 'remove',
  CLEAR: 'clear'
});

export const RPC = Object.freeze({
  TYPE: 'type',
  TYPE_EVENT: 'event',
  TYPE_NAVIGATION: 'navigation',
  TYPE_MAP_SYNC: 'mSync',
  NODE: 'node',
  EVENT_TYPE: 'event',
  EVENT_DATA: 'data',
  FEATURE: 'feature',
  PROPERTY: 'property',
  PROPERTY_VALUE: 'value',
  NAVIGATION_LOCATION: 'location',
  NAVIGATION_STATE: 'state'
});

// --- UIDL change builders ---------------------------------------------------

export interface AttachChange {
  type: 'attach';
  node: number;
}

export interface DetachChange {
  type: 'detach';
  node: number;
}

export interface PutChange {
  type: 'put';
  node: number;
  feat: number;
  key: string;
  value?: unknown;
  nodeValue?: number;
}

export interface RemoveMapKeyChange {
  type: 'remove';
  node: number;
  feat: number;
  key: string;
}

export interface SpliceChange {
  type: 'splice';
  node: number;
  feat: number;
  index: number;
  add?: unknown[];
  addNodes?: number[];
  remove?: number;
}

export interface ClearListChange {
  type: 'clear';
  node: number;
  feat: number;
}

export type UidlChange = AttachChange | DetachChange | PutChange | RemoveMapKeyChange | SpliceChange | ClearListChange;

export function attach(nodeId: number): AttachChange {
  return { type: CHANGE_TYPE.ATTACH as 'attach', node: nodeId };
}

export function detach(nodeId: number): DetachChange {
  return { type: CHANGE_TYPE.DETACH as 'detach', node: nodeId };
}

export function put(nodeId: number, feature: number, key: string, value: unknown): PutChange {
  return { type: CHANGE_TYPE.PUT as 'put', node: nodeId, feat: feature, key, value };
}

export function putNode(nodeId: number, feature: number, key: string, childNodeId: number): PutChange {
  return { type: CHANGE_TYPE.PUT as 'put', node: nodeId, feat: feature, key, nodeValue: childNodeId };
}

export function removeMapKey(nodeId: number, feature: number, key: string): RemoveMapKeyChange {
  return { type: CHANGE_TYPE.REMOVE as 'remove', node: nodeId, feat: feature, key };
}

export interface SpliceOptions {
  add?: unknown[];
  addNodes?: number[];
  remove?: number;
}

export function splice(nodeId: number, feature: number, index: number, options: SpliceOptions = {}): SpliceChange {
  const change: SpliceChange = {
    type: CHANGE_TYPE.SPLICE as 'splice',
    node: nodeId,
    feat: feature,
    index
  };
  if (options.add !== undefined) {
    change.add = options.add;
  }
  if (options.addNodes !== undefined) {
    change.addNodes = options.addNodes;
  }
  if (options.remove !== undefined) {
    change.remove = options.remove;
  }
  return change;
}

export function clearList(nodeId: number, feature: number): ClearListChange {
  return { type: CHANGE_TYPE.CLEAR as 'clear', node: nodeId, feat: feature };
}

// --- Client→server RPC builders --------------------------------------------

export interface PropertySyncRpc {
  type: 'mSync';
  node: number;
  feature: number;
  property: string;
  value: unknown;
}

export interface EventRpc {
  type: 'event';
  node: number;
  event: string;
  data: unknown;
}

export interface NavigationRpc {
  type: 'navigation';
  location: string;
  state?: unknown;
}

export function eventRpc(nodeId: number, eventName: string, data: unknown = {}): EventRpc {
  return { type: 'event', node: nodeId, event: eventName, data };
}

export function propertySyncRpc(nodeId: number, feature: number, property: string, value: unknown): PropertySyncRpc {
  return { type: 'mSync', node: nodeId, feature, property, value };
}

export function navigationRpc(location: string, state?: unknown): NavigationRpc {
  return { type: 'navigation', location, state };
}

// --- DOM fixtures -----------------------------------------------------------

/**
 * Creates an element attached to `document.body`. Registers a `mocha`
 * cleanup via `afterEach` to remove it. Call from a `describe` block.
 */
export function attachedFixture<K extends keyof HTMLElementTagNameMap>(tag: K): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  document.body.appendChild(element);
  registerFixtureCleanup(() => element.remove());
  return element;
}

/**
 * Creates a detached element. Useful for tests that exercise binding
 * before the element is in the document.
 */
export function detachedFixture<K extends keyof HTMLElementTagNameMap>(tag: K): HTMLElementTagNameMap[K] {
  return document.createElement(tag);
}

const fixtureCleanups: Array<() => void> = [];

function registerFixtureCleanup(cleanup: () => void): void {
  fixtureCleanups.push(cleanup);
  // Mocha's afterEach is global; chain a single hook lazily.
  if (fixtureCleanups.length === 1) {
    afterEach(() => {
      while (fixtureCleanups.length > 0) {
        try {
          fixtureCleanups.pop()?.();
        } catch {
          // best-effort cleanup
        }
      }
    });
  }
}
