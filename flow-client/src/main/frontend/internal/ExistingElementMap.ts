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

// TypeScript port of com.vaadin.client.ExistingElementMap, built alongside the
// Java version. Maps between a server-side node id requested to attach to an
// existing client-side element and that element.

/** Bidirectional mapping between server-side node ids and existing elements. */
export class ExistingElementMap {
  private readonly elementToId = new Map<Element, number>();

  // Indexed by id; mirrors the Java JsArray used as a Map<Integer, Element>.
  private readonly idToElement: Array<Element | null> = [];

  /** Gets the element added under the given id, or null if there is none. */
  getElement(id: number): Element | null {
    return this.idToElement[id] ?? null;
  }

  /** Gets the id the given element was added under, or null if there is none. */
  getId(element: Element): number | null {
    const id = this.elementToId.get(element);
    return id === undefined ? null : id;
  }

  /** Removes the id and its associated element from the mapping. */
  remove(id: number): void {
    const element = this.idToElement[id];
    if (element !== null && element !== undefined) {
      this.idToElement[id] = null;
      this.elementToId.delete(element);
    }
  }

  /** Adds the id and the element to the mapping. */
  add(id: number, element: Element): void {
    this.idToElement[id] = element;
    this.elementToId.set(element, id);
  }
}
