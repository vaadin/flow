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

/**
 * Polymer-specific browser helpers migrated from `com.vaadin.client.PolymerUtils`.
 * Reached from GWT code via the `NativePolymerUtils` JsType shim. Pure-Java
 * helpers (`createModelTree`, `addReadyListener`, `fireReadyEvent`,
 * `getCustomElement`, `getTag`, the deprecated `hasTag` alias, etc.) stay in
 * `PolymerUtils.java`.
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

  getDomRoot(templateElement: Node): Element | null {
    return (templateElement as PolymerElementLike).root ?? null;
  },

  invokeWhenDefined(tagName: string, runnable: () => void): void {
    polymerGlobal()
      .customElements?.whenDefined(tagName)
      .then(() => runnable());
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
