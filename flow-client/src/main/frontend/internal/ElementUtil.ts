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

/**
 * DOM element helpers, migrated from `com.vaadin.client.ElementUtil`. The Java
 * class is now a native `@JsType` shim that calls into these functions, which
 * are registered on `window.Vaadin.Flow.internal.ElementUtil`.
 */

/** Checks whether the {@code node} has the required {@code tag}. */
export function hasTag(node: Node, tag: string): boolean {
  return node instanceof Element && node.tagName.toLowerCase() === tag.toLowerCase();
}

/**
 * Searches the shadow root of the given context element for the given id, or the
 * light DOM if the element has no shadow root.
 */
export function getElementById(context: Node, id: string): Element | null {
  const bodyExports = (document.body as any).$;
  if (bodyExports && bodyExports.hasOwnProperty && bodyExports.hasOwnProperty(id)) {
    // Exported web components add their id to body.$ and cannot be found using
    // a real id attribute.
    return bodyExports[id];
  }
  const ctx = context as any;
  if (ctx.shadowRoot) {
    return ctx.shadowRoot.getElementById(id);
  } else if (ctx.getElementById) {
    return ctx.getElementById(id);
  } else if (id && id.match('^[a-zA-Z0-9-_]*$')) {
    // No funky characters in id so querySelector can be used directly.
    return ctx.querySelector('#' + id);
  }
  // Find all elements with an id attribute and filter out the correct one.
  return Array.from(ctx.querySelectorAll('[id]') as NodeListOf<Element>).find((e) => e.id === id) ?? null;
}

/** Searches the context for an element with the given {@code name} attribute. */
export function getElementByName(context: Node, name: string): Element | null {
  return (
    Array.from((context as Element).querySelectorAll('[name]')).find((e) => e.getAttribute('name') === name) ?? null
  );
}
