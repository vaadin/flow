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

// DOM/Polymer probes and model-data writers migrated from PolymerUtils.java,
// registered on window.Vaadin.Flow.internal.PolymerUtils by registerInternals;
// the Java methods delegate here. The StateNode-coupled model-tree building
// (createModelTree and the change handlers) is ported separately in
// PolymerModelTree.ts. Also bundled to ES5 for the HtmlUnit used by GwtTests.
//
// The ready-listener registry and custom-element-by-path lookup
// (addReadyListener/fireReadyEvent/getCustomElement) are build-alongside
// additions used by the SimpleElementBindingStrategy attach machinery; they are
// not registered (the Java versions stay live until cutover).

import { wrap } from './dom/DomApi';

// A node exposing the Polymer model-data API (set/get/splice).
interface PolymerModelNode {
  set(path: string, value: unknown): void;
  get?(path: string): unknown;
  splice(...args: unknown[]): unknown;
}

/** Whether the element is a Polymer 2 or Polymer 3 element. */
export function isPolymerElement(htmlNode: Element): boolean {
  const polymer = (window as unknown as { Polymer?: unknown }).Polymer as
    | (((...args: unknown[]) => unknown) & { Element?: new (...args: unknown[]) => unknown })
    | undefined;
  const isP2Element = typeof polymer === 'function' && !!polymer.Element && htmlNode instanceof polymer.Element;
  const isP3Element =
    (htmlNode as unknown as { constructor: { polymerElementVersion?: unknown } }).constructor.polymerElementVersion !==
    undefined;
  return isP2Element || isP3Element;
}

/** Whether the element could be a custom (and thus possibly Polymer) element. */
export function mayBePolymerElement(htmlNode: Element): boolean {
  return !!(window as unknown as { customElements?: unknown }).customElements && htmlNode.localName.includes('-');
}

/** Whether the element is inside a shadow root. */
export function isInShadowRoot(element: Element): boolean {
  let node: Node | null = element.parentNode;
  while (node) {
    if (node.toString() === '[object ShadowRoot]') {
      return true;
    }
    node = node.parentNode;
  }
  return false;
}

/** Whether the Polymer local-DOM ($) of the node is ready. */
export function isReady(shadowRootParent: Node): boolean {
  return typeof (shadowRootParent as unknown as { $?: unknown }).$ !== 'undefined';
}

/** The Polymer dom root of a template element, or null. */
export function getDomRoot(templateElement: Node): Element | null {
  return (templateElement as unknown as { root?: Element }).root ?? null;
}

/** The element with the given id from the Polymer local-DOM ($) map, or null. */
export function getDomElementById(shadowRootParent: Node, id: string): Element | null {
  return (shadowRootParent as unknown as { $: Record<string, Element> }).$[id] ?? null;
}

/** Finds the first element matching the CSS query inside the shadow root. */
export function searchForElementInShadowRoot(shadowRoot: ShadowRoot, cssQuery: string): Node | null {
  return shadowRoot.querySelector(cssQuery);
}

/** Finds the element with the given id inside the shadow root. */
export function getElementInShadowRootById(shadowRoot: ShadowRoot, id: string): Node | null {
  return shadowRoot.getElementById(id);
}

/** Runs the callback once a custom element with the given tag name is defined. */
export function invokeWhenDefined(tagName: string, callback: () => void): void {
  void window.customElements.whenDefined(tagName).then(callback);
}

// NodeFeatures.ELEMENT_DATA / NodeProperties.TAG
const ELEMENT_DATA = 0;
const TAG_PROPERTY = 'tag';

/** The slice of StateNode getTag reads. */
interface TagNode {
  getMap(featureId: number): { getProperty(name: string): { getValue(): unknown } };
}

/** The element tag name stored in the node's element data. Mirrors PolymerUtils.getTag. */
export function getTag(node: TagNode): string {
  return node.getMap(ELEMENT_DATA).getProperty(TAG_PROPERTY).getValue() as string;
}

/** Sets a single list element via the Polymer set method (path + "." + index). */
export function setListValueByIndex(htmlNode: Element, path: string, listIndex: number, newValue: unknown): void {
  (htmlNode as unknown as PolymerModelNode).set(`${path}.${listIndex}`, newValue);
}

/**
 * Calls the Polymer splice method via apply so that itemsToAdd is spread into
 * separate arguments rather than passed as a single array.
 */
// eslint-disable-next-line @typescript-eslint/max-params -- positional JSNI delegation mirrors the Java signature
export function splice(
  htmlNode: Element,
  path: string,
  startIndex: number,
  deleteCount: number,
  itemsToAdd: unknown[]
): void {
  const node = htmlNode as unknown as PolymerModelNode;
  node.splice.apply(node, ([path, startIndex, deleteCount] as unknown[]).concat(itemsToAdd));
}

/** Stores the StateNode id under the 'nodeId' key of the Polymer model object at path. */
export function storeNodeId(domNode: Node, id: number, path: string): void {
  const node = domNode as unknown as PolymerModelNode;
  if (typeof node.get !== 'undefined') {
    const polymerProperty = node.get!(path) as Record<string, unknown> | null;
    if (typeof polymerProperty === 'object' && polymerProperty !== null && polymerProperty.nodeId === undefined) {
      polymerProperty.nodeId = id;
    }
  }
}

/** Sets a property on an element via the Polymer set method. */
export function setProperty(element: Element, path: string, value: unknown): void {
  (element as unknown as PolymerModelNode).set(path, value);
}

// Registry of "ready" listeners per (polymer) element; mirrors the static
// readyListeners JsWeakMap.
const readyListeners = new WeakMap<Element, Set<() => void>>();

/** Registers a listener invoked when the polymer element fires its ready event. */
export function addReadyListener(polymerElement: Element, listener: () => void): void {
  let set = readyListeners.get(polymerElement);
  if (set === undefined) {
    set = new Set();
    readyListeners.set(polymerElement, set);
  }
  set.add(listener);
}

/** Fires the ready event for the element, running and clearing its listeners. */
export function fireReadyEvent(polymerElement: Element): void {
  const listeners = readyListeners.get(polymerElement);
  if (listeners !== undefined) {
    readyListeners.delete(polymerElement);
    listeners.forEach((listener) => listener());
  }
}

// Returns the index-th child element of parent, ignoring <style> children.
function getChildIgnoringStyles(parent: Node, index: number): Node | null {
  const children = wrap(parent).children;
  let filteredIndex = -1;
  // eslint-disable-next-line @typescript-eslint/prefer-for-of -- indexed HTMLCollection access
  for (let i = 0; i < children.length; i++) {
    const element = children[i];
    if (element.tagName.toLowerCase() !== 'style') {
      filteredIndex++;
    }
    if (filteredIndex === index) {
      return element;
    }
  }
  return null;
}

/**
 * Resolves the custom element addressed by a path of child indices (ignoring
 * style children) from the given root. Mirrors PolymerUtils.getCustomElement.
 */
export function getCustomElement(root: Node | null, path: unknown[]): Element | null {
  let current: Node | null = root;
  for (const value of path) {
    current = current === null ? null : getChildIgnoringStyles(current, value as number);
  }
  if (current instanceof Element) {
    return current;
  } else if (current === null) {
    console.warn(`There is no element addressed by the path '${JSON.stringify(path)}'`);
  } else {
    console.warn(`The node addressed by path ${JSON.stringify(path)} is not an Element`);
  }
  return null;
}
