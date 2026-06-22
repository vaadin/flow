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

// DOM/Polymer probes migrated from PolymerUtils.java, registered on
// window.Vaadin.Flow.internal.PolymerUtils by registerInternals; the Java
// methods delegate here. The StateNode-coupled model-data methods stay in Java
// for now. Also bundled to ES5 for the HtmlUnit used by GwtTests.

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
