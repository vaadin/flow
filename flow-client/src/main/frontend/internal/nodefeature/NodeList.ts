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

// TypeScript port of com.vaadin.client.flow.nodefeature.NodeList (and its
// splice event/listener), on top of the TS reactive core.

import {
  ReactiveEventRouter,
  ReactiveValueChangeEvent,
  type EventRemover,
  type ReactiveValue,
  type ReactiveValueChangeListener
} from '../reactive/reactive';
import { NodeFeature, type JsonValue } from './NodeFeature';

/** Fired when a list's structure changes; mirrors ListSpliceEvent. */
export class ListSpliceEvent extends ReactiveValueChangeEvent {
  private readonly index: number;

  private readonly remove: unknown[];

  private readonly add: unknown[];

  private readonly clear: boolean;

  constructor(source: NodeList, details: { index: number; remove: unknown[]; add: unknown[]; clear: boolean }) {
    super(source);
    this.index = details.index;
    this.remove = details.remove;
    this.add = details.add;
    this.clear = details.clear;
  }

  override getSource(): NodeList {
    return super.getSource() as NodeList;
  }

  getIndex(): number {
    return this.index;
  }

  getRemove(): unknown[] {
    return this.remove;
  }

  getAdd(): unknown[] {
    return this.add;
  }

  isClear(): boolean {
    return this.clear;
  }
}

/** Listener for list structure changes; mirrors ListSpliceListener. */
export type ListSpliceListener = (event: ListSpliceEvent) => void;

/** A state node feature that structures data as a list; mirrors NodeList.java. */
export class NodeList extends NodeFeature implements ReactiveValue {
  private readonly values: unknown[] = [];

  private hasBeenClearedState = false;

  private readonly eventRouter = new ReactiveEventRouter<ListSpliceListener, ListSpliceEvent>(
    this,
    (reactiveValueChangeListener) => reactiveValueChangeListener,
    (listener, event) => listener(event)
  );

  length(): number {
    this.eventRouter.registerRead();
    return this.values.length;
  }

  get(index: number): unknown {
    return this.values[index];
  }

  set(index: number, value: unknown): void {
    this.values[index] = value;
  }

  add(index: number, item: unknown): void {
    this.splice(index, 0, [item]);
  }

  splice(index: number, remove: number, add?: unknown[]): void {
    const removed = add === undefined ? this.values.splice(index, remove) : this.values.splice(index, remove, ...add);
    this.eventRouter.fireEvent(new ListSpliceEvent(this, { index, remove: removed, add: add ?? [], clear: false }));
  }

  clear(): void {
    this.hasBeenClearedState = true;
    const removed = this.values.splice(0, this.values.length);
    this.eventRouter.fireEvent(new ListSpliceEvent(this, { index: 0, remove: removed, add: [], clear: true }));
  }

  override getDebugJson(): JsonValue {
    const json: JsonValue[] = [];
    for (const value of this.values) {
      json.push(this.getAsDebugJson(value));
    }
    return json;
  }

  override convert(converter: (value: unknown) => JsonValue): JsonValue {
    const json: JsonValue[] = [];
    for (const value of this.values) {
      json.push(converter(value));
    }
    return json;
  }

  addSpliceListener(listener: ListSpliceListener): EventRemover {
    return this.eventRouter.addListener(listener);
  }

  addReactiveValueChangeListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.eventRouter.addReactiveListener(reactiveValueChangeListener);
  }

  forEach(callback: (value: unknown) => void): void {
    this.eventRouter.registerRead();
    this.values.forEach((value) => callback(value));
  }

  hasBeenCleared(): boolean {
    return this.hasBeenClearedState;
  }
}
