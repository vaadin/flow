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

const SIMPLE_ID = /^[a-zA-Z0-9-_]*$/;

interface DocumentRegistrationRoot {
  $?: Record<string, Element>;
}

/**
 * DOM lookup helpers migrated from `com.vaadin.client.ElementUtil`. The Java
 * class is a pure `@JsType(isNative=true)` facade onto this module.
 */
export const ElementUtil = {
  hasTag(node: Node | null, tag: string): boolean {
    const tagged = node as unknown as { tagName?: string } | null;
    return tagged !== null && typeof tagged.tagName === 'string' && tagged.tagName.toLowerCase() === tag.toLowerCase();
  },

  getElementById(context: Node, id: string): Element | undefined {
    const exported = (document.body as unknown as DocumentRegistrationRoot).$;
    if (exported && Object.prototype.hasOwnProperty.call(exported, id)) {
      // Exported WCs register their id on body.$ and cannot be found via attribute lookup
      return exported[id];
    }
    const root = (context as unknown as { shadowRoot?: ShadowRoot }).shadowRoot;
    if (root) {
      return root.getElementById(id) ?? undefined;
    }
    const docLike = context as unknown as { getElementById?: (id: string) => Element | null };
    if (typeof docLike.getElementById === 'function') {
      return docLike.getElementById(id) ?? undefined;
    }
    if (SIMPLE_ID.test(id)) {
      return (context as Element).querySelector('#' + id) ?? undefined;
    }
    return Array.from((context as Element).querySelectorAll('[id]')).find((e) => e.id === id);
  },

  getElementByName(context: Node, name: string): Element | undefined {
    return Array.from((context as Element).querySelectorAll('[name]')).find((e) => e.getAttribute('name') === name);
  }
};
