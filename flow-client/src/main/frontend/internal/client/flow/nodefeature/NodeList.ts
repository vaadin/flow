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
import { ReactiveEventRouter } from '../reactive/ReactiveEventRouter';
import { ListSpliceEvent } from './ListSpliceEvent';
import { NodeFeature } from './NodeFeature';

type AnyListener = any;

type StateNodeLike = any;

/**
 * State-node list feature. Migrated from
 * `com.vaadin.client.flow.nodefeature.NodeList`. Structural changes fire a
 * {@link ListSpliceEvent}; per-item updates do not (items are expected to be
 * immutable or reactive values of their own).
 */
export class NodeList extends NodeFeature {
  private readonly values: unknown[] = [];
  private hasBeenClearedFlag = false;
  private readonly eventRouter: ReactiveEventRouter;

  constructor(id: number, node: StateNodeLike) {
    super(id, node);
    // Mirrors Java NodeList: wrap turns a ReactiveValueChangeListener into a
    // ListSpliceListener whose `onSplice` fires the value-change handler.
    this.eventRouter = new ReactiveEventRouter(
      this,
      (listener: AnyListener) => ({
        onSplice: (event: unknown): void => listener.onValueChange(event)
      }),
      (listener: AnyListener, event: unknown): void => listener.onSplice(event)
    );
  }

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
    this.spliceArray(index, 0, [item]);
  }

  spliceRemove(index: number, remove: number): void {
    const removed = this.values.splice(index, remove);
    this.eventRouter.fireEvent(new ListSpliceEvent(this, index, removed, [], false));
  }

  clear(): void {
    this.hasBeenClearedFlag = true;
    const removed = this.values.splice(0, this.values.length);
    this.eventRouter.fireEvent(new ListSpliceEvent(this, 0, removed, [], true));
  }

  spliceArray(index: number, remove: number, add: any): void {
    const addArray = (Array.isArray(add) ? add : Array.from(add as Iterable<unknown>)) as unknown[];
    const removed = this.values.splice(index, remove, ...addArray);
    this.eventRouter.fireEvent(new ListSpliceEvent(this, index, removed, add, false));
  }

  hasBeenCleared(): boolean {
    return this.hasBeenClearedFlag;
  }

  override getDebugJson(): unknown {
    return this.values.map((v) => this.getAsDebugJson(v));
  }

  override convert(converter: (value: unknown) => unknown): unknown {
    return this.values.map((v) => converter(v));
  }

  addSpliceListener(listener: AnyListener): { remove(): void } {
    // Accept either a bare `(event) => void` callback (the common Java
    // `ListSpliceListener` lambda) or an object that already has
    // `onSplice`. Normalize to the latter.
    const adapted = typeof listener === 'function' ? { onSplice: listener } : listener;
    return this.eventRouter.addListener(adapted);
  }

  addReactiveValueChangeListener(listener: AnyListener): { remove(): void } {
    return this.eventRouter.addReactiveListener(listener);
  }

  forEach(callback: (value: unknown) => void): void {
    this.values.forEach((v) => callback(v));
  }
}
