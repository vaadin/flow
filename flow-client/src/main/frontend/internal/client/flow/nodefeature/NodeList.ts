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

// TypeScript port of com.vaadin.client.flow.nodefeature.NodeList, on top of the
// TS reactive core.

import type { Computation } from '../reactive/Computation';
import type { EventRemover } from '../../../EventRemover';
import { ReactiveEventRouter } from '../reactive/ReactiveEventRouter';
import type { ReactiveValue } from '../reactive/ReactiveValue';
import type { ReactiveValueChangeListener } from '../reactive/ReactiveValueChangeListener';
import { ListSpliceEvent } from './ListSpliceEvent';
import type { ListSpliceListener } from './ListSpliceListener';
import { NodeFeature, type JsonValue } from './NodeFeature';

/**
 * A state node feature that structures data as a list.
 *
 * The list works as a reactive value with regards to its structure. A
 * {@link Computation} will get a dependency on this list for any read operation
 * that depends on the list structure, such as querying the length, iterating
 * the list or finding the index of an item. Accessing an item by index does not
 * create a dependency. The {@link Computation} is invalidated when items
 * are added, removed, reordered or replaced. It is not invalidated when the
 * contents of an item is updated since all items are expected to be either
 * immutable or reactive values of their own.
 */
export class NodeList extends NodeFeature implements ReactiveValue {
  readonly #values: unknown[] = [];

  #hasBeenClearedState = false;

  readonly #eventRouter = new ReactiveEventRouter<ListSpliceListener, ListSpliceEvent>(
    this,
    (reactiveValueChangeListener) => reactiveValueChangeListener,
    (listener, event) => listener(event)
  );

  /**
   * Gets the number of items in this list.
   *
   * @returns the number of items
   */
  length(): number {
    this.#eventRouter.registerRead();
    return this.#values.length;
  }

  /**
   * Gets the item at the given index.
   *
   * @param index - the index
   * @returns the item at the index
   */
  get(index: number): unknown {
    return this.#values[index];
  }

  /**
   * Sets the value at the given index.
   *
   * @param index - the index
   * @param value - the value to set
   */
  set(index: number, value: unknown): void {
    this.#values[index] = value;
  }

  /**
   * Shorthand for adding the given item at the given index. This method
   * delegates to {@link splice} which updates the list
   * contents and fires the appropriate event.
   *
   * @param index - the index where the item should be added
   * @param item - the new item to add
   */
  add(index: number, item: unknown): void {
    this.splice(index, 0, [item]);
  }

  /**
   * Removes and adds a number of items at the given index.
   *
   * This causes a {@link ListSpliceEvent} to be fired.
   *
   * Port deviation: merges the Java `splice(int, int)` and
   * `splice(int, int, JsArray)` overloads into one method with an optional
   * `add` argument; omitting `add` removes items without adding any.
   *
   * @param index - the index at which do do the operation
   * @param remove - the number of items to remove
   * @param add - an array of new items to add
   */
  splice(index: number, remove: number, add?: unknown[]): void {
    const removed = add === undefined ? this.#values.splice(index, remove) : this.#values.splice(index, remove, ...add);
    this.#eventRouter.fireEvent(new ListSpliceEvent(this, index, removed, add ?? [], false));
  }

  /**
   * Removes all the nodes from the list. This causes a
   * {@link ListSpliceEvent} to be fired, with
   * {@link ListSpliceEvent.isClear} as `true`.
   */
  clear(): void {
    this.#hasBeenClearedState = true;
    const removed = this.#values.splice(0, this.#values.length);
    this.#eventRouter.fireEvent(new ListSpliceEvent(this, 0, removed, [], true));
  }

  /**
   * Gets a JSON object representing the contents of this feature. Only
   * intended for debugging purposes.
   *
   * @returns a JSON representation
   */
  override getDebugJson(): JsonValue {
    const json: JsonValue[] = [];
    for (const value of this.#values) {
      json.push(this.getAsDebugJson(value));
    }
    return json;
  }

  /**
   * Convert the feature values into a {@link JsonValue} using provided
   * `converter` for the values stored in the feature (i.e. primitive
   * types, StateNodes).
   *
   * @param converter - converter to convert values stored in the feature
   * @returns resulting converted value
   */
  override convert(converter: (value: unknown) => JsonValue): JsonValue {
    const json: JsonValue[] = [];
    for (const value of this.#values) {
      json.push(converter(value));
    }
    return json;
  }

  /**
   * Adds a listener that will be notified when the list structure changes.
   *
   * @param listener - the list change listener
   * @returns an event remover that can be used for removing the added listener
   */
  addSpliceListener(listener: ListSpliceListener): EventRemover {
    return this.#eventRouter.addListener(listener);
  }

  addReactiveValueChangeListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.#eventRouter.addReactiveListener(reactiveValueChangeListener);
  }

  /**
   * Iterates all values in this list.
   *
   * @param callback - the callback to invoke for each value
   */
  forEach(callback: (value: unknown) => void): void {
    this.#eventRouter.registerRead();
    this.#values.forEach((value) => callback(value));
  }

  /**
   * Returns `true` if the list instance has been cleared at some point.
   *
   * @returns `true` if the list instance has been cleared
   */
  hasBeenCleared(): boolean {
    return this.#hasBeenClearedState;
  }
}
