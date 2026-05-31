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
import { Console } from './Console';
import { WidgetUtil } from './WidgetUtil';
import { Reactive } from './flow/reactive/Reactive';

// Mirrors NodeFeatures + NodeProperties constants the helpers need.
const ELEMENT_PROPERTIES = 1;
const TEMPLATE_MODELLIST = 14;
const BASIC_TYPE_VALUE = 22;
const ELEMENT_DATA = 0;
const TAG = 'tag';
const VALUE = 'value';

interface PolymerElementLike {
  set?: (path: string, value: unknown) => void;
  get?: (path: string) => unknown;
  splice?: (path: string, ...args: unknown[]) => unknown;
  root?: Element | null;
  $?: Record<string, Element>;
  localName?: string;
}

interface PolymerGlobal {
  Polymer?: { Element?: unknown } & ((...args: unknown[]) => unknown);
  customElements?: CustomElementRegistry;
}

function polymerGlobal(): PolymerGlobal {
  return globalThis as unknown as PolymerGlobal;
}

type StateNodeLike = {
  getId(): number;
  getParent(): StateNodeLike | null;
  getDomNode(): unknown;
  hasFeature(id: number): boolean;
  getMap(id: number): NodeMapLike;
  getList(id: number): NodeListLike;
  addUnregisterListener(listener: (event: unknown) => void): { remove(): void };
};

type NodeMapLike = {
  getId(): number;
  getProperty(name: string): MapPropertyLike;
  getPropertyNames(): string[];
  forEachProperty(callback: (property: MapPropertyLike, key: string) => void): void;
  addPropertyAddListener(listener: (event: { getProperty(): MapPropertyLike }) => void): { remove(): void };
  convert(converter: (value: unknown) => unknown): unknown;
};

type NodeListLike = {
  getId(): number;
  length(): number;
  get(index: number): unknown;
  convert(converter: (value: unknown) => unknown): unknown;
  spliceArray(index: number, remove: number, add: unknown): void;
  addSpliceListener(listener: (event: ListSpliceEventLike) => void): { remove(): void };
};

type ListSpliceEventLike = {
  getAdd(): unknown[];
  getIndex(): number;
  getRemove(): unknown[];
  getSource(): NodeListLike & { getNode(): StateNodeLike };
};

type MapPropertyLike = {
  getName(): string;
  getValue(): unknown;
  getMap(): NodeMapLike & { getNode(): StateNodeLike };
  addChangeListener(listener: (event: unknown) => void): { remove(): void };
};

// Per-element queues of "ready" callbacks. WeakMap so detached elements can
// be GC'd. Matches the JsWeakMap in the original Java implementation.
const readyListeners = new WeakMap<Element, Set<() => void>>();

/**
 * Polymer integration helpers migrated from `com.vaadin.client.PolymerUtils`.
 */
export const PolymerUtils = {
  setListValueByIndex(htmlNode: Element, path: string, listIndex: number, newValue: unknown): void {
    (htmlNode as PolymerElementLike).set?.(path + '.' + listIndex, newValue);
  },

  // eslint-disable-next-line @typescript-eslint/max-params
  splice(htmlNode: Element, path: string, startIndex: number, deleteCount: number, itemsToAdd: unknown[]): void {
    const node = htmlNode as PolymerElementLike;
    node.splice?.apply(node, [path, startIndex, deleteCount, ...(itemsToAdd ?? [])]);
  },

  storeNodeId(domNode: Node, id: number, path: string): void {
    const polymerNode = domNode as PolymerElementLike;
    if (typeof polymerNode.get === 'function') {
      const polymerProperty = polymerNode.get(path);
      if (
        typeof polymerProperty === 'object' &&
        polymerProperty !== null &&
        (polymerProperty as Record<string, unknown>).nodeId === undefined
      ) {
        (polymerProperty as Record<string, unknown>).nodeId = id;
      }
    }
  },

  createModelTree(object: unknown): unknown {
    if (isStateNode(object)) {
      let feature: NodeMapLike | NodeListLike | null = null;
      if (object.hasFeature(ELEMENT_PROPERTIES)) {
        feature = object.getMap(ELEMENT_PROPERTIES);
      } else if (object.hasFeature(TEMPLATE_MODELLIST)) {
        feature = object.getList(TEMPLATE_MODELLIST);
      } else if (object.hasFeature(BASIC_TYPE_VALUE)) {
        return PolymerUtils.createModelTree(object.getMap(BASIC_TYPE_VALUE).getProperty(VALUE));
      }
      if (feature === null) {
        throw new Error("Don't know how to convert node without map or list features");
      }
      const convert = feature.convert((v) => PolymerUtils.createModelTree(v));
      if (isPlainObject(convert) && !('nodeId' in (convert as Record<string, unknown>))) {
        (convert as Record<string, unknown>).nodeId = object.getId();
        registerChangeHandlers(object, feature, convert);
      }
      return convert;
    } else if (isMapProperty(object)) {
      if (object.getMap().getId() === BASIC_TYPE_VALUE) {
        return PolymerUtils.createModelTree(object.getValue());
      }
      return { [object.getName()]: PolymerUtils.createModelTree(object.getValue()) };
    }
    return WidgetUtil.crazyJsoCast(object);
  },

  isPolymerElement(htmlNode: Element): boolean {
    const globalEnv = polymerGlobal();
    const Polymer = globalEnv.Polymer;
    const polymerElementCtor = Polymer?.Element as (new (...args: unknown[]) => unknown) | undefined;
    const isP2Element =
      typeof Polymer === 'function' && polymerElementCtor != null && htmlNode instanceof polymerElementCtor;
    const ctor = (htmlNode as unknown as { constructor: { polymerElementVersion?: unknown } }).constructor;
    const isP3Element = ctor?.polymerElementVersion !== undefined;
    return isP2Element || isP3Element;
  },

  mayBePolymerElement(htmlNode: Element): boolean {
    return polymerGlobal().customElements != null && (htmlNode.localName?.indexOf('-') ?? -1) > -1;
  },

  searchForElementInShadowRoot(shadowRoot: ShadowRoot, cssQuery: string): Node | null {
    return shadowRoot.querySelector(cssQuery);
  },

  getElementInShadowRootById(shadowRoot: ShadowRoot, id: string): Node | null {
    return shadowRoot.getElementById(id);
  },

  getDomElementById(shadowRootParent: Node, id: string): Element | undefined {
    return (shadowRootParent as PolymerElementLike).$?.[id];
  },

  isReady(shadowRootParent: Node): boolean {
    return (shadowRootParent as PolymerElementLike).$ !== undefined;
  },

  hasTag(node: Node, tag: string): boolean {
    const el = node as unknown as { tagName?: string };
    return typeof el.tagName === 'string' && el.tagName.toLowerCase() === tag.toLowerCase();
  },

  getCustomElement(root: Node, path: number[]): Element | null {
    let current: Node | null = root;
    for (const index of path) {
      current = getChildIgnoringStyles(current, index);
    }
    if (current instanceof Element) {
      return current;
    }
    if (current === null) {
      Console.warn(`There is no element addressed by the path '${JSON.stringify(path)}'`);
    } else {
      Console.warn(`The node addressed by path ${JSON.stringify(path)} is not an Element`);
    }
    return null;
  },

  getDomRoot(templateElement: Node): Element | null {
    return (templateElement as PolymerElementLike).root ?? null;
  },

  invokeWhenDefined(tagName: string, runnable: () => void): void {
    polymerGlobal()
      .customElements?.whenDefined(tagName)
      .then(() => runnable());
  },

  getTag(node: StateNodeLike): string {
    return node.getMap(ELEMENT_DATA).getProperty(TAG).getValue() as string;
  },

  addReadyListener(polymerElement: Element, listener: () => void): void {
    let set = readyListeners.get(polymerElement);
    if (set === undefined) {
      set = new Set();
      readyListeners.set(polymerElement, set);
    }
    set.add(listener);
  },

  fireReadyEvent(polymerElement: Element): void {
    const listeners = readyListeners.get(polymerElement);
    if (listeners !== undefined) {
      readyListeners.delete(polymerElement);
      listeners.forEach((l) => l());
    }
  },

  setProperty(element: Element, path: string, value: unknown): void {
    (element as PolymerElementLike).set?.(path, value);
  },

  isInShadowRoot(element: Element): boolean {
    let current: Node | null = element.parentNode;
    while (current != null) {
      if (Object.prototype.toString.call(current) === '[object ShadowRoot]') {
        return true;
      }
      current = current.parentNode;
    }
    return false;
  }
};

function isStateNode(o: unknown): o is StateNodeLike {
  return (
    typeof o === 'object' &&
    o !== null &&
    typeof (o as StateNodeLike).hasFeature === 'function' &&
    typeof (o as StateNodeLike).getMap === 'function' &&
    typeof (o as StateNodeLike).getList === 'function' &&
    typeof (o as StateNodeLike).addUnregisterListener === 'function'
  );
}

function isMapProperty(o: unknown): o is MapPropertyLike {
  return (
    typeof o === 'object' &&
    o !== null &&
    typeof (o as MapPropertyLike).getMap === 'function' &&
    typeof (o as MapPropertyLike).getName === 'function' &&
    typeof (o as MapPropertyLike).getValue === 'function' &&
    typeof (o as MapPropertyLike).addChangeListener === 'function'
  );
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function registerChangeHandlers(node: StateNodeLike, feature: NodeMapLike | NodeListLike, value: unknown): void {
  const registrations: Array<{ remove(): void }> = [];
  if (node.hasFeature(ELEMENT_PROPERTIES)) {
    const map = feature as NodeMapLike;
    registerPropertyChangeHandlers(value, registrations, map);
    registerPropertyAddHandler(value, registrations, map);
  } else if (node.hasFeature(TEMPLATE_MODELLIST)) {
    const list = feature as NodeListLike;
    registrations.push(list.addSpliceListener((event) => handleListChange(event, value)));
  }
  if (registrations.length === 0) {
    throw new Error('Node should have ELEMENT_PROPERTIES or TEMPLATE_MODELLIST feature');
  }
  registrations.push(
    node.addUnregisterListener(() => {
      registrations.forEach((r) => r.remove());
    })
  );
}

function registerPropertyAddHandler(value: unknown, registrations: Array<{ remove(): void }>, map: NodeMapLike): void {
  registrations.push(
    map.addPropertyAddListener((event) => {
      const property = event.getProperty();
      registrations.push(property.addChangeListener(() => handlePropertyChange(property, value)));
      handlePropertyChange(property, value);
    })
  );
}

function registerPropertyChangeHandlers(
  value: unknown,
  registrations: Array<{ remove(): void }>,
  map: NodeMapLike
): void {
  map.forEachProperty((property) => {
    registrations.push(property.addChangeListener(() => handlePropertyChange(property, value)));
  });
}

function handleListChange(event: ListSpliceEventLike, value: unknown): void {
  Reactive.addFlushListener(() => doHandleListChange(event, value));
}

function doHandleListChange(event: ListSpliceEventLike, value: unknown): void {
  const add = event.getAdd();
  const index = event.getIndex();
  const remove = event.getRemove().length;
  const node = event.getSource().getNode();
  const root = getFirstParentWithDomNode(node);
  if (root === null) {
    Console.warn(`Root node for node ${node.getId()} could not be found`);
    return;
  }
  const array = add.map((item) => PolymerUtils.createModelTree(item));
  if (PolymerUtils.isPolymerElement(root.getDomNode() as Element)) {
    const path = getNotificationPath(root, node, null);
    if (path !== null) {
      PolymerUtils.splice(root.getDomNode() as Element, path, index, remove, WidgetUtil.crazyJsoCast(array));
      return;
    }
  }
  // Fall back to mutating the JS array directly.
  (value as unknown[]).splice(index, remove, ...array);
}

function handlePropertyChange(property: MapPropertyLike, bean: unknown): void {
  Reactive.addFlushListener(() => doHandlePropertyChange(property, bean));
}

function doHandlePropertyChange(property: MapPropertyLike, value: unknown): void {
  const propertyName = property.getName();
  const node = property.getMap().getNode();
  const root = getFirstParentWithDomNode(node);
  if (root === null) {
    Console.warn(`Root node for node ${node.getId()} could not be found`);
    return;
  }
  const modelTree = PolymerUtils.createModelTree(property.getValue());
  if (PolymerUtils.isPolymerElement(root.getDomNode() as Element)) {
    const path = getNotificationPath(root, node, propertyName);
    if (path !== null) {
      PolymerUtils.setProperty(root.getDomNode() as Element, path, modelTree);
    }
    return;
  }
  WidgetUtil.setJsProperty(value as Record<string, unknown>, propertyName, modelTree);
}

function getNotificationPath(
  rootNode: StateNodeLike,
  currentNode: StateNodeLike,
  propertyName: string | null
): string | null {
  const path: string[] = [];
  if (propertyName !== null) {
    path.push(propertyName);
  }
  return doGetNotificationPath(rootNode, currentNode, path);
}

function doGetNotificationPath(rootNode: StateNodeLike, currentNode: StateNodeLike, path: string[]): string | null {
  const parent = currentNode.getParent();
  if (parent === null) {
    return null;
  }
  if (parent.hasFeature(ELEMENT_PROPERTIES)) {
    const propertyPath = getPropertiesNotificationPath(currentNode);
    if (propertyPath === null) {
      return null;
    }
    path.push(propertyPath);
  } else if (parent.hasFeature(TEMPLATE_MODELLIST)) {
    const listPath = getListNotificationPath(currentNode);
    if (listPath === null) {
      return null;
    }
    path.push(listPath);
  }
  if (parent !== rootNode) {
    return doGetNotificationPath(rootNode, parent, path);
  }
  const segments: string[] = [];
  for (let i = path.length - 1; i >= 0; i--) {
    segments.push(path[i]);
  }
  return segments.join('.');
}

function getListNotificationPath(currentNode: StateNodeLike): string | null {
  const parent = currentNode.getParent();
  if (parent === null) {
    return null;
  }
  const children = parent.getList(TEMPLATE_MODELLIST);
  for (let i = 0; i < children.length(); i++) {
    if (children.get(i) === currentNode) {
      return String(i);
    }
  }
  return null;
}

function getPropertiesNotificationPath(currentNode: StateNodeLike): string | null {
  const parent = currentNode.getParent();
  if (parent === null) {
    return null;
  }
  const map = parent.getMap(ELEMENT_PROPERTIES);
  const propertyNames = map.getPropertyNames();
  for (const name of propertyNames) {
    if (map.getProperty(name).getValue() === currentNode) {
      return name;
    }
  }
  return null;
}

function getFirstParentWithDomNode(node: StateNodeLike): StateNodeLike | null {
  let parent = node.getParent();
  while (parent !== null && parent.getDomNode() == null) {
    parent = parent.getParent();
  }
  return parent;
}

function getChildIgnoringStyles(parent: Node | null, index: number): Node | null {
  if (parent === null) {
    return null;
  }
  const children = (parent as Element).children;
  if (children === undefined) {
    return null;
  }
  let filteredIndex = -1;
  for (const element of Array.from(children)) {
    if (element.tagName.toLowerCase() !== 'style') {
      filteredIndex++;
    }
    if (filteredIndex === index) {
      return element;
    }
  }
  return null;
}
