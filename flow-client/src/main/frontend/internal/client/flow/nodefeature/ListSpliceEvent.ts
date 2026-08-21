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

// TypeScript port of com.vaadin.client.flow.nodefeature.ListSpliceEvent.

import { ReactiveValueChangeEvent } from '../reactive/ReactiveValueChangeEvent';
import type { NodeList } from './NodeList';

/**
 * Event fired when the structure of a {@link NodeList} changes.
 */
export class ListSpliceEvent extends ReactiveValueChangeEvent {
  readonly #index: number;

  readonly #remove: unknown[];

  readonly #add: unknown[];

  readonly #clear: boolean;

  /**
   * Creates a new list splice event.
   *
   * @param source - the changed list
   * @param index - the start index of the changes
   * @param remove - the removed items, not `null`
   * @param add - the added items, not `null`
   * @param clear - `true` when this is an event triggered upon removing all the
   *   nodes of the given list, `false` otherwise
   */
  // The five positional parameters deliberately match the Java ListSpliceEvent
  // constructor (see PORTING.md: match the Java parameter list, do not bundle
  // positional parameters into an object), so max-params is disabled here.
  // eslint-disable-next-line @typescript-eslint/max-params
  constructor(source: NodeList, index: number, remove: unknown[], add: unknown[], clear: boolean) {
    super(source);
    this.#index = index;
    this.#remove = remove;
    this.#add = add;
    this.#clear = clear;
  }

  override getSource(): NodeList {
    return super.getSource() as NodeList;
  }

  /**
   * Gets the start index of the changes.
   *
   * @returns the start index of the changes
   */
  getIndex(): number {
    return this.#index;
  }

  /**
   * Gets an array of removed items.
   *
   * @returns array of removed items, not `null`
   */
  getRemove(): unknown[] {
    return this.#remove;
  }

  /**
   * Gets an array of added items.
   *
   * @returns array of added items, not `null`
   */
  getAdd(): unknown[] {
    return this.#add;
  }

  /**
   * Gets whether this event is a `clear` event.
   *
   * @returns `true` if the event was triggered after a full clear,
   *         `false` otherwise.
   */
  isClear(): boolean {
    return this.#clear;
  }
}
