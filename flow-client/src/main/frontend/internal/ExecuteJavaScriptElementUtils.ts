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

// Implementation migrated from ExecuteJavaScriptElementUtils.java, registered on
// window.Vaadin.Flow.internal.ExecuteJavaScriptElementUtils by registerInternals;
// the Java method delegates here. Also bundled to ES5 for the HtmlUnit used by
// GwtTests.

/**
 * Checks whether the node's element class declares the given property with a
 * default value (Polymer-style static `properties` with a `value`).
 */
export function isPropertyDefined(node: Node, property: string): boolean {
  const ctor = (node as unknown as { constructor?: { properties?: Record<string, { value?: unknown }> } }).constructor;
  const declared = ctor && ctor.properties && ctor.properties[property];
  return !!declared && typeof declared.value !== 'undefined';
}

// The JS-initializer cleanup registry of ExecuteJavaScriptElementUtils, ported
// build-alongside. It stores per-node cleanup callbacks for JS initializers
// (Element#addJsInitializer) and drains them when the node leaves the tree. The
// remaining DOM-coupled methods (attachExistingElement, populateModelProperties)
// follow in a later installment and are integration-validated. The StateNode is
// a contract (addUnregisterListener / setNodeData).

import { wrap } from './dom/DomApi';
import { getTag, invokeWhenDefined } from './PolymerUtils';
import { Reactive } from './reactive/reactive';
import { getJsProperty } from './WidgetUtil';
import { UpdatableModelProperties } from './model/UpdatableModelProperties';

// NodeFeatures.ELEMENT_PROPERTIES / ELEMENT_CHILDREN
const ELEMENT_PROPERTIES = 1;
const ELEMENT_CHILDREN = 2;

/** The slice of MapProperty populateModelProperties uses. */
interface ModelProperty {
  setValue(value: unknown): void;
  syncToServer(newValue: unknown): void;
}

/** The slice of NodeMap populateModelProperties uses. */
interface ModelPropertiesMap {
  hasPropertyValue(name: string): boolean;
  getProperty(name: string): ModelProperty;
}

/** The slice of StateNode populateModelProperties uses. */
interface ModelNode {
  getDomNode(): Node | null;
  getMap(featureId: number): ModelPropertiesMap;
  getNodeData<T>(clazz: new (...args: never[]) => T): T | null;
}

/** The slice of StateNode registerUpdatableModelProperties uses. */
interface UpdatablePropertiesNode {
  setNodeData(object: object): void;
}

/**
 * Registers the model properties whose updates may be sent to the server
 * without explicit synchronization. Mirrors
 * ExecuteJavaScriptElementUtils.registerUpdatableModelProperties.
 */
export function registerUpdatableModelProperties(node: UpdatablePropertiesNode, properties: string[]): void {
  if (properties.length > 0) {
    node.setNodeData(new UpdatableModelProperties(properties));
  }
}

/** A child state node in the parent's element-children list. */
interface AttachChildNode {
  getDomNode(): Node;
  getId(): number;
}

/** The state tree slice attachExistingElement uses. */
interface AttachTree {
  sendExistingElementAttachToServer(
    parent: AttachParentNode,
    id: number,
    existingId: number,
    tagName: string,
    index: number
  ): void;
  getRegistry(): {
    getExistingElementMap(): { getId(element: Element): number | null; add(id: number, element: Element): void };
  };
}

/** The slice of StateNode attachExistingElement uses (the parent). */
interface AttachParentNode {
  getDomNode(): Node;
  getList(featureId: number): { length(): number; get(index: number): AttachChildNode };
  getTree(): AttachTree;
}

function hasTag(node: Node, tag: string): boolean {
  return node instanceof Element && tag.toLowerCase() === node.tagName.toLowerCase();
}

function getExistingIdOrUpdate(
  parent: AttachParentNode,
  serverSideId: number,
  existingElement: Element,
  existingId: number | null
): number {
  if (existingId === null) {
    const map = parent.getTree().getRegistry().getExistingElementMap();
    const fromMap = map.getId(existingElement);
    if (fromMap === null) {
      map.add(serverSideId, existingElement);
      return serverSideId;
    }
    return fromMap;
  }
  return existingId;
}

/**
 * Finds the existing DOM element matching the given tag after the previous
 * sibling among the parent's children, resolves its server-side id (registering
 * it if new), and reports the attachment back to the server. Mirrors
 * ExecuteJavaScriptElementUtils.attachExistingElement.
 */
export function attachExistingElement(
  parent: AttachParentNode,
  previousSibling: Element | null,
  tagName: string,
  id: number
): void {
  let existingElement: Element | null = null;
  const childNodes = wrap(parent.getDomNode()).childNodes;
  const indices = new Map<Node, number>();
  let afterSibling = previousSibling === null;
  let elementIndex = -1;
  for (let i = 0; i < childNodes.length; i++) {
    const node = childNodes[i];
    indices.set(node, i);
    if (node === previousSibling) {
      afterSibling = true;
    }
    if (afterSibling && hasTag(node, tagName)) {
      existingElement = node as Element;
      elementIndex = i;
      break;
    }
  }

  if (existingElement === null) {
    // Report an error (no matching element found).
    parent.getTree().sendExistingElementAttachToServer(parent, id, -1, tagName, -1);
    return;
  }

  const list = parent.getList(ELEMENT_CHILDREN);
  let existingId: number | null = null;
  let childIndex = 0;
  for (let i = 0; i < list.length(); i++) {
    const stateNode = list.get(i);
    const domNode = stateNode.getDomNode();
    const index = indices.get(domNode);
    if (index !== undefined && index < elementIndex) {
      childIndex++;
    }
    if (domNode === existingElement) {
      existingId = stateNode.getId();
      break;
    }
  }

  existingId = getExistingIdOrUpdate(parent, id, existingElement, existingId);
  parent.getTree().sendExistingElementAttachToServer(parent, id, existingId, existingElement.tagName, childIndex);
}

function populateModelProperty(node: ModelNode, map: ModelPropertiesMap, property: string): void {
  const domNode = node.getDomNode() as unknown as Node;
  if (!isPropertyDefined(domNode, property)) {
    if (!map.hasPropertyValue(property)) {
      map.getProperty(property).setValue(null);
    }
  } else {
    const updatableProperties = node.getNodeData(UpdatableModelProperties);
    if (updatableProperties === null || !updatableProperties.isUpdatableProperty(property)) {
      return;
    }
    map.getProperty(property).syncToServer(getJsProperty(domNode as unknown as Record<string, unknown>, property));
  }
}

/**
 * Populates the given model properties on the node's element, syncing
 * client-side values back to the server for updatable properties. If the element
 * is not yet upgraded, retries once its custom element is defined. Mirrors
 * ExecuteJavaScriptElementUtils.populateModelProperties.
 */
export function populateModelProperties(node: ModelNode, properties: string[]): void {
  const map = node.getMap(ELEMENT_PROPERTIES);
  if (node.getDomNode() === null) {
    invokeWhenDefined(getTag(node as never), () =>
      Reactive.addPostFlushListener(() => populateModelProperties(node, properties))
    );
    return;
  }
  for (const property of properties) {
    populateModelProperty(node, map, property);
  }
}

/** A JS cleanup callback for a registered initializer. */
type JsCallback = () => void;

/** The slice of StateNode the initializer registry uses. */
interface InitializerNode {
  addUnregisterListener(listener: () => void): unknown;
}

// Per-node map of initializer id -> cleanup callback.
const initializerCleanups = new Map<InitializerNode, Map<number, JsCallback>>();

function invokeSafely(fn: JsCallback): void {
  try {
    fn();
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
  }
}

function drainInitializers(node: InitializerNode): void {
  const entry = initializerCleanups.get(node);
  if (entry === undefined) {
    return;
  }
  initializerCleanups.delete(node);
  entry.forEach((fn) => invokeSafely(fn));
}

/**
 * Stores a cleanup callback for a JS initializer. If one was already stored for
 * the same id, it is invoked before being replaced (defensive against stale
 * state). The first registration for a node attaches an unregister listener that
 * drains all remaining cleanups when the node leaves the tree. Mirrors
 * ExecuteJavaScriptElementUtils.registerInitializer.
 */
export function registerInitializer(node: InitializerNode, id: number, cleanup: JsCallback): void {
  let entry = initializerCleanups.get(node);
  if (entry === undefined) {
    entry = new Map<number, JsCallback>();
    initializerCleanups.set(node, entry);
    node.addUnregisterListener(() => drainInitializers(node));
  }
  const existing = entry.get(id);
  // Install the new cleanup before invoking the previous one so a re-entrant
  // register/dispose from inside the existing callback sees the new state.
  entry.set(id, cleanup);
  if (existing !== undefined) {
    invokeSafely(existing);
  }
}

/**
 * Disposes a previously registered JS initializer cleanup (invoking it); a no-op
 * for an unknown id. Mirrors ExecuteJavaScriptElementUtils.disposeInitializer.
 */
export function disposeInitializer(node: InitializerNode, id: number): void {
  const entry = initializerCleanups.get(node);
  if (entry === undefined) {
    return;
  }
  const fn = entry.get(id);
  if (fn === undefined) {
    return;
  }
  entry.delete(id);
  invokeSafely(fn);
}

/** Clears the registry; for tests only. */
export function resetForTesting(): void {
  initializerCleanups.clear();
}
