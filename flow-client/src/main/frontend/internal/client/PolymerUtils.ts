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

import type * as ElementUtil from './ElementUtil';
import { Console } from './Console';
import { NodeFeatures } from '../flow/internal/nodefeature/NodeFeatures';
import { NodeProperties } from '../flow/internal/nodefeature/NodeProperties';
import type { StateNode } from './flow/StateNode';

/**
 * Utils class, intended to ease working with Polymer related code on a client
 * side.
 *
 * The deprecated `PolymerUtils.hasTag(Node, String)` is intentionally omitted:
 * its Javadoc directs callers to the generic `ElementUtil.hasTag`, which is
 * ported in {@link ElementUtil.hasTag}.
 */

// DOM/Polymer probes and model-data writers migrated from PolymerUtils.java. The
// StateNode-coupled model-tree building (createModelTree and the change handlers)
// is ported separately in PolymerModelTree.ts.
//
// The ready-listener registry and custom-element-by-path lookup
// (addReadyListener/fireReadyEvent/getCustomElement) are used by the
// SimpleElementBindingStrategy attach machinery.
//

// Registry of "ready" listeners per (polymer) element; mirrors the static
// readyListeners JsWeakMap.
const readyListeners = new WeakMap<Element, Set<() => void>>();

// A node exposing the Polymer model-data API (set/get/splice).
interface PolymerModelNode {
  set(path: string, value: unknown): void;
  get?(path: string): unknown;
  splice(...args: unknown[]): unknown;
}

/**
 * Sets new value for list element for specified `htmlNode`.
 *
 * @param htmlNode - node to call set method on
 * @param path - polymer model path to property
 * @param listIndex - list index to set element into
 * @param newValue - new value to be set at desired index
 *
 * @see Polymer docs: https://www.polymer-project.org/2.0/docs/devguide/model-data
 */
export function setListValueByIndex(htmlNode: Element, path: string, listIndex: number, newValue: unknown): void {
  (htmlNode as unknown as PolymerModelNode).set(`${path}.${listIndex}`, newValue);
}

/**
 * Calls Polymer `splice` method on specified `htmlNode`.
 *
 * Splice call is made via `apply` method in order to force the method to treat
 * `itemsToAdd` as numerous parameters, not a single one.
 *
 * @param htmlNode - node to call splice method on
 * @param path - polymer model path to property
 * @param startIndex - start index of a list for splice operation
 * @param deleteCount - number of elements to delete from the list after startIndex
 * @param itemsToAdd - elements to add after startIndex
 *
 * @see Polymer docs: https://www.polymer-project.org/2.0/docs/devguide/model-data
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

/**
 * Store the StateNode.id into the polymer property under 'nodeId'
 *
 * @param domNode - polymer dom node
 * @param id - id of a state node
 * @param path - polymer model path to property
 */
export function storeNodeId(domNode: Node, id: number, path: string): void {
  const node = domNode as unknown as PolymerModelNode;
  if (typeof node.get !== 'undefined') {
    const polymerProperty = node.get!(path) as Record<string, unknown> | null;
    // Java tests only `typeof polymerProperty === 'object'` (PolymerUtils.java:119-120); since
    // `typeof null === 'object'`, that JSNI would throw on a null property. The `!== null` guard
    // is an intentional deviation that avoids the latent NPE, matching the documented guard in
    // ServerEventObject.getPolymerPropertyObject.
    if (typeof polymerProperty === 'object' && polymerProperty !== null && polymerProperty.nodeId === undefined) {
      polymerProperty.nodeId = id;
    }
  }
}

/**
 * Sets a property to an element by using the Polymer `set` method.
 *
 * @param element - the element to set the property to
 * @param path - the path of the property
 * @param value - the value
 */
export function setProperty(element: Element, path: string, value: unknown): void {
  (element as unknown as PolymerModelNode).set(path, value);
}

/**
 * Checks whether the `htmlNode` is a polymer 2 element.
 *
 * @param htmlNode - HTML element to check
 * @returns `true` if the `htmlNode` is a polymer element
 */
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

/**
 * Checks whether the `htmlNode` can turn into polymer 2 element later.
 *
 * Lazy loaded dependencies can load Polymer later than the element itself gets
 * processed by the Flow. This method helps to determine such elements.
 *
 * @param htmlNode - HTML element to check
 * @returns `true` if the `htmlNode` can become a polymer 2 element
 *
 * @deprecated This is not in use anywhere and can be removed
 */
export function mayBePolymerElement(htmlNode: Element): boolean {
  return !!(window as unknown as { customElements?: unknown }).customElements && htmlNode.localName.includes('-');
}

/**
 * Get first element by css query in the shadow root provided.
 *
 * @param shadowRoot - shadow root element
 * @param cssQuery - css query
 * @returns first element matching the query or `null` for no matches
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/Web_Components/Shadow_DOM
 *
 * @deprecated This is not in use anywhere and can be removed
 */
export function searchForElementInShadowRoot(shadowRoot: ShadowRoot, cssQuery: string): Node | null {
  return shadowRoot.querySelector(cssQuery);
}

/**
 * Get the element by id from the shadow root provided.
 *
 * @param shadowRoot - shadow root element
 * @param id - element id
 * @returns the element with id provided or `null` for no matches
 *
 * @see http://html5index.org/Shadow%20DOM%20-%20ShadowRoot.html
 *
 * @deprecated This is not in use anywhere and can be removed
 */
export function getElementInShadowRootById(shadowRoot: ShadowRoot, id: string): Node | null {
  return shadowRoot.getElementById(id);
}

/**
 * Find the DOM element inside shadow root of the `shadowRootParent`.
 *
 * @param shadowRootParent - the parent whose shadow root contains the element
 *            with the `id`
 * @param id - the identifier of the element to search for
 * @returns the element with the given `id` inside the shadow root of the parent
 * @deprecated This is Polymer specific. Use {@link ElementUtil.getElementById}
 *             for the generic version
 */
export function getDomElementById(shadowRootParent: Node, id: string): Element | null {
  return (shadowRootParent as unknown as { $: Record<string, Element> }).$[id] ?? null;
}

/**
 * Returns `true` if the DOM structure of the polymer custom element
 * `shadowRootParent` is ready (meaning that it has shadow root and its shadow
 * root may be queried for children referenced by id).
 *
 * @param shadowRootParent - the polymer custom element
 * @returns `true` if the `shadowRootParent` element is ready
 */
export function isReady(shadowRootParent: Node): boolean {
  return typeof (shadowRootParent as unknown as { $?: unknown }).$ !== 'undefined';
}

/**
 * Gets the custom element using `path` of indices starting from the `root`.
 *
 * @param root - the root element to start from
 * @param path - the indices path identifying the custom element.
 * @returns the element inside the `root` by the path of indices
 */
export function getCustomElement(root: Node, path: unknown[]): Element | null {
  let current: Node | null = root;
  for (const value of path) {
    // Java calls getChildIgnoringStyles unconditionally (PolymerUtils.java:513-515); once a
    // prior index is out of range it returns null and the next iteration NPEs. Mirror that
    // unguarded deref rather than silently propagating null.

    current = getChildIgnoringStyles(current!, value as number);
  }
  if (current instanceof Element) {
    return current;
  } else if (current === null) {
    Console.warn(`There is no element addressed by the path '${JSON.stringify(path)}'`);
  } else {
    Console.warn(`The node addressed by path ${JSON.stringify(path)} is not an Element`);
  }
  return null;
}

/**
 * Returns the shadow root of the `templateElement`.
 *
 * @param templateElement - the owner of the shadow root
 * @returns the shadow root of the element
 */
export function getDomRoot(templateElement: Node): Element | null {
  return (templateElement as unknown as { root?: Element }).root ?? null;
}

/**
 * Invokes the `runnable` when the custom element with the given `tagName` is
 * initialized (its DOM structure becomes available).
 *
 * @param tagName - the name of the custom element
 * @param runnable - the command to run when the element if initialized
 */
export function invokeWhenDefined(tagName: string, runnable: () => void): void {
  void window.customElements.whenDefined(tagName).then(runnable);
}

/**
 * Gets the tag name of the `node`.
 *
 * @param node - the node to get the tag name from
 * @returns the tag name of the node
 */
export function getTag(node: StateNode): string {
  return node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG).getValue() as string;
}

/**
 * Adds the `listener` which will be invoked when the `polymerElement` becomes
 * "ready" meaning that it's method `ready` is called.
 *
 * The listener won't be called if the element is already "ready" and the
 * listener will be removed immediately once it's executed.
 *
 * @param polymerElement - the custom (polymer) element to listen its readiness
 *            state
 * @param listener - the callback to execute once the element becomes ready
 */
export function addReadyListener(polymerElement: Element, listener: () => void): void {
  let set = readyListeners.get(polymerElement);
  if (set === undefined) {
    set = new Set();
    readyListeners.set(polymerElement, set);
  }
  set.add(listener);
}

/**
 * Fires the ready event for the `polymerElement`.
 *
 * @param polymerElement - the custom (polymer) element whose state is "ready"
 */
export function fireReadyEvent(polymerElement: Element): void {
  const listeners = readyListeners.get(polymerElement);
  if (listeners !== undefined) {
    readyListeners.delete(polymerElement);
    listeners.forEach((listener) => listener());
  }
}

// Returns the index-th child element of parent, ignoring <style> children.
function getChildIgnoringStyles(parent: Node, index: number): Node | null {
  const children = (parent as Element).children;
  let filteredIndex = -1;
  // eslint-disable-next-line @typescript-eslint/prefer-for-of -- indexed HTMLCollection access
  for (let i = 0; i < children.length; i++) {
    // Java asserts each child is an Element; the DOM `children` collection is
    // already typed as Element here, so the check is unreachable and dropped.
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
 * Returns true if and only if the element has a shadow root ancestor.
 *
 * @param element - the element to test
 * @returns whether the element is in a shadow root
 */
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
