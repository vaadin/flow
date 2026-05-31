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
 * Mapping between server-side node identifiers and their attached existing
 * client-side elements. Migrated from `com.vaadin.client.ExistingElementMap`.
 */
export class ExistingElementMap {
  private readonly elementToId = new Map<Element, number>();
  private readonly idToElement: Array<Element | null> = [];

  getElement(id: number): Element | null {
    if (id < 0 || id >= this.idToElement.length) {
      return null;
    }
    return this.idToElement[id];
  }

  getId(element: Element): number | null {
    return this.elementToId.get(element) ?? null;
  }

  remove(id: number): void {
    if (id < 0 || id >= this.idToElement.length) {
      return;
    }
    const element = this.idToElement[id];
    if (element != null) {
      this.idToElement[id] = null;
      this.elementToId.delete(element);
    }
  }

  add(id: number, element: Element): void {
    while (this.idToElement.length <= id) {
      this.idToElement.push(null);
    }
    this.idToElement[id] = element;
    this.elementToId.set(element, id);
  }
}
