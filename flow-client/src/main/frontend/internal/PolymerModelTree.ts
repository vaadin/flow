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

// TypeScript port of the StateNode-coupled model-tree building from
// com.vaadin.client.PolymerUtils (createModelTree and its change handlers). It
// is kept out of internal/PolymerUtils.ts (which holds the DOM/Polymer probes
// and model-data writers) because it depends on the whole reactive state tree;
// it imports the Polymer model-data writers (splice/setProperty/isPolymerElement)
// from there.
//
// createModelTree converts a StateNode/MapProperty model into the plain JS model
// object Polymer binds to, recursively, tagging each object with its nodeId and
// registering reactive change handlers that push later model changes into the
// Polymer element (or the plain payload object when the host is not Polymer).

import type { JsonValue, NodeFeature } from './nodefeature/NodeFeature';
import { NodeFeatures, NodeProperties } from './nodefeature/NodeFeatures';
import { MapProperty } from './nodefeature/MapProperty';
import type { NodeList, ListSpliceEvent } from './nodefeature/NodeList';
import type { NodeMap } from './nodefeature/NodeMap';
import { isPolymerElement, setProperty, splice } from './PolymerUtils';
import { Reactive, type EventRemover } from './reactive/reactive';
import { StateNode } from './StateNode';
import { setJsProperty } from './WidgetUtil';

/**
 * Converts a model object (StateNode, MapProperty or scalar) into the plain JS
 * model tree Polymer binds to, tagging objects with their nodeId and registering
 * change handlers. Mirrors PolymerUtils.createModelTree.
 */
export function createModelTree(object: unknown): JsonValue {
  if (object instanceof StateNode) {
    const node = object;
    let feature: NodeFeature | null = null;
    if (node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
      feature = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
    } else if (node.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
      feature = node.getList(NodeFeatures.TEMPLATE_MODELLIST);
    } else if (node.hasFeature(NodeFeatures.BASIC_TYPE_VALUE)) {
      return createModelTree(node.getMap(NodeFeatures.BASIC_TYPE_VALUE).getProperty(NodeProperties.VALUE));
    }

    const convert = feature!.convert(createModelTree);
    // Register change handlers for both model objects (ELEMENT_PROPERTIES) and
    // model lists (TEMPLATE_MODELLIST). The Java original gates on
    // `convert instanceof JsonObject`, which in elemental.json is also true for a
    // JsonArray, so list nodes get their splice listener registered too — arrays
    // must NOT be excluded here, otherwise model-list splices never reach Polymer.
    if (typeof convert === 'object' && convert !== null && !('nodeId' in convert)) {
      (convert as Record<string, unknown>).nodeId = node.getId();
      registerChangeHandlers(node, feature!, convert);
    }
    return convert;
  } else if (object instanceof MapProperty) {
    const property = object;
    if ((property.getMap() as unknown as NodeMap).getId() === NodeFeatures.BASIC_TYPE_VALUE) {
      return createModelTree(property.getValue());
    }
    const convertedObject: Record<string, JsonValue> = {};
    convertedObject[property.getName()] = createModelTree(property.getValue());
    return convertedObject;
  }
  return object as JsonValue;
}

function registerChangeHandlers(node: StateNode, feature: NodeFeature, value: JsonValue): void {
  const registrations: EventRemover[] = [];
  if (node.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
    const map = feature as NodeMap;
    registerPropertyChangeHandlers(value, registrations, map);
    registerPropertyAddHandler(value, registrations, map);
  } else if (node.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
    const list = feature as NodeList;
    registrations.push(list.addSpliceListener((event) => handleListChange(event, value)));
  }

  registrations.push(node.addUnregisterListener(() => registrations.forEach((registration) => registration.remove())));
}

function registerPropertyAddHandler(value: JsonValue, registrations: EventRemover[], map: NodeMap): void {
  registrations.push(
    map.addPropertyAddListener((event) => {
      const property = event.getProperty();
      registrations.push(property.addChangeListener(() => handlePropertyChange(property, value)));
      handlePropertyChange(property, value);
    })
  );
}

function registerPropertyChangeHandlers(value: JsonValue, registrations: EventRemover[], map: NodeMap): void {
  map.forEachProperty((property) =>
    registrations.push(property.addChangeListener(() => handlePropertyChange(property, value)))
  );
}

function handleListChange(event: ListSpliceEvent, value: JsonValue): void {
  Reactive.addFlushListener(() => doHandleListChange(event, value));
}

function doHandleListChange(event: ListSpliceEvent, value: JsonValue): void {
  const add = event.getAdd();
  const index = event.getIndex();
  const remove = event.getRemove().length;
  const node = event.getSource().getNode() as StateNode;
  const root = getFirstParentWithDomNode(node);
  if (root === null) {
    console.warn(`Root node for node ${node.getId()} could not be found`);
    return;
  }

  const array = add.map((item) => createModelTree(item));

  if (isPolymerElement(root.getDomNode() as Element)) {
    const path = getNotificationPath(root, node, null);
    if (path !== null) {
      splice(root.getDomNode() as Element, path, index, remove, array);
      return;
    }
  }
  (value as JsonValue[]).splice(index, remove, ...array);
}

function handlePropertyChange(property: MapProperty, bean: JsonValue): void {
  Reactive.addFlushListener(() => doHandlePropertyChange(property, bean));
}

function doHandlePropertyChange(property: MapProperty, value: JsonValue): void {
  const propertyName = property.getName();
  const node = property.getMap().getNode() as unknown as StateNode;
  const root = getFirstParentWithDomNode(node);
  if (root === null) {
    console.warn(`Root node for node ${node.getId()} could not be found`);
    return;
  }
  const modelTree = createModelTree(property.getValue());

  if (isPolymerElement(root.getDomNode() as Element)) {
    const path = getNotificationPath(root, node, propertyName);
    if (path !== null) {
      setProperty(root.getDomNode() as Element, path, modelTree);
    }
    return;
  }
  setJsProperty(value as Record<string, unknown>, propertyName, modelTree);
}

function getNotificationPath(rootNode: StateNode, currentNode: StateNode, propertyName: string | null): string | null {
  const path: string[] = [];
  if (propertyName !== null) {
    path.push(propertyName);
  }
  return doGetNotificationPath(rootNode, currentNode, path);
}

function doGetNotificationPath(rootNode: StateNode, currentNode: StateNode, path: string[]): string | null {
  const parent = currentNode.getParent()!;
  if (parent.hasFeature(NodeFeatures.ELEMENT_PROPERTIES)) {
    const propertyPath = getPropertiesNotificationPath(currentNode);
    if (propertyPath === null) {
      return null;
    }
    path.push(propertyPath);
  } else if (parent.hasFeature(NodeFeatures.TEMPLATE_MODELLIST)) {
    const listPath = getListNotificationPath(currentNode);
    if (listPath === null) {
      return null;
    }
    path.push(listPath);
  }
  if (parent !== rootNode) {
    return doGetNotificationPath(rootNode, parent, path);
  }

  let result = '';
  let separator = '';
  for (let i = path.length - 1; i >= 0; i--) {
    result += separator + path[i];
    separator = '.';
  }
  return result;
}

function getListNotificationPath(currentNode: StateNode): string | null {
  let indexInTheList = -1;
  const children = currentNode.getParent()!.getList(NodeFeatures.TEMPLATE_MODELLIST);

  for (let i = 0; i < children.length(); i++) {
    if (currentNode === children.get(i)) {
      indexInTheList = i;
      break;
    }
  }

  if (indexInTheList < 0) {
    return null;
  }
  return String(indexInTheList);
}

function getPropertiesNotificationPath(currentNode: StateNode): string | null {
  let propertyNameInTheMap: string | null = null;
  const map = currentNode.getParent()!.getMap(NodeFeatures.ELEMENT_PROPERTIES);

  for (const propertyName of map.getPropertyNames()) {
    if (currentNode === map.getProperty(propertyName).getValue()) {
      propertyNameInTheMap = propertyName;
      break;
    }
  }
  return propertyNameInTheMap;
}

/**
 * Gets the first parent node that also has a DOM node attached, or null. Mirrors
 * getFirstParentWithDomNode.
 */
function getFirstParentWithDomNode(node: StateNode): StateNode | null {
  let parent = node.getParent();
  while (parent !== null && parent.getDomNode() === null) {
    parent = parent.getParent();
  }
  return parent;
}
