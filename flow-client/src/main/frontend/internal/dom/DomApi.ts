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

// TypeScript port of the com.vaadin.client.flow.dom package (DomApi, DomApiImpl,
// DomNode, DomElement and PolymerDomApiImpl).
// All DOM access in the binding layer goes through DomApi.wrap(node): by default
// it returns the node itself (native DOM), but once Polymer 1.x ("polymer micro")
// is detected it returns Polymer.dom(node) so the local-DOM/shadow traversal is
// the flat tree Polymer exposes. DomNode/DomElement mirror the Polymer-overridden
// subset of the Node/Element APIs using native member names, so a native
// Element and a Polymer.dom wrapper are both usable through the same type.

// The DOMTokenList members Polymer overrides; mirrors DomElement.DomTokenList.
export interface DomTokenList {
  readonly length: number;
  item(index: number): string | null;
  contains(token: string): boolean;
  add(token: string): void;
  remove(token: string): void;
  toggle(token: string): boolean;
}

// The Node members Polymer overrides; mirrors DomNode.
export interface DomNode {
  readonly childNodes: ArrayLike<Node>;
  readonly firstChild: Node | null;
  readonly lastChild: Node | null;
  readonly nextSibling: Node | null;
  readonly previousSibling: Node | null;
  textContent: string | null;
  readonly parentNode: Node | null;
  appendChild(node: Node): void;
  insertBefore(newChild: Node, refChild: Node | null): void;
  removeChild(childNode: Node): void;
  replaceChild(newChild: Node, oldChild: Node): void;
  cloneNode(deep: boolean): Node;
  isSameNode(node: Node | null): boolean;
}

// The Element members Polymer overrides; mirrors DomElement.
export interface DomElement extends DomNode {
  readonly classList: DomTokenList;
  readonly firstElementChild: Element | null;
  readonly lastElementChild: Element | null;
  innerHTML: string;
  readonly children: HTMLCollection;
  querySelector(selectors: string): Element | null;
  querySelectorAll(selectors: string): ArrayLike<Node>;
  setAttribute(name: string, value: string): void;
  removeAttribute(name: string): void;
}

// A DOM API implementation used via wrap(); mirrors the DomApiImpl functional
// interface.
type DomApiImpl = (node: Node) => DomElement;

// The native Polymer object, present once polymer-micro has loaded.
interface Polymer {
  dom(node: Node): DomElement;
  readonly version: string;
}

function getPolymer(): Polymer | undefined {
  return (window as { Polymer?: Polymer }).Polymer;
}

// The Polymer DomApiImpl; mirrors PolymerDomApiImpl.wrap.
const polymerDomApiImpl: DomApiImpl = (node) => getPolymer()!.dom(node);

/**
 * Checks whether polymer-micro has been loaded. Polymer 2 is excluded on
 * purpose. Mirrors PolymerDomApiImpl.isPolymerMicroLoaded.
 */
export function isPolymerMicroLoaded(): boolean {
  const polymer = getPolymer();
  return polymer !== undefined && polymer.version.startsWith('1.');
}

// Tracks whether polymer-micro has been loaded (so the impl is only swapped in
// once). Mirrors the package-protected DomApi.polymerMicroLoaded.
let polymerMicroLoaded = false;

// The currently used DOM API implementation; null returns the node as-is.
// Mirrors the package-protected DomApi.impl.
let impl: DomApiImpl | null = null;

/**
 * Wraps the given DOM node so that any DomNode/DomElement method is safe to
 * invoke. Mirrors DomApi.wrap.
 */
export function wrap(node: Node): DomElement {
  if (impl === null) {
    return node as unknown as DomElement;
  }
  return impl(node);
}

/**
 * Updates the DOM API implementation in use, switching to the Polymer DOM API
 * the first time polymer-micro is detected. Mirrors
 * DomApi.updateApiImplementation.
 */
export function updateApiImplementation(): void {
  if (!polymerMicroLoaded && isPolymerMicroLoaded()) {
    polymerMicroLoaded = true;
    console.debug('Polymer micro is now loaded, using Polymer DOM API');
    impl = polymerDomApiImpl;
  }
}

/**
 * Resets the DOM API state. Mirrors the package-protected DomApi fields that the
 * Java tests reset; intended for tests only.
 */
export function resetForTesting(): void {
  polymerMicroLoaded = false;
  impl = null;
}
