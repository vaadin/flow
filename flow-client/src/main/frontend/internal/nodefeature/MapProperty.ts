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

// TypeScript port of com.vaadin.client.flow.nodefeature.MapProperty (and its
// change event/listener), on top of the TS reactive core. The thin slice of the
// state-tree classes it touches is declared here as contracts that NodeMap,
// StateNode and StateTree satisfy.

import {
  Reactive,
  ReactiveEventRouter,
  ReactiveValueChangeEvent,
  type EventRemover,
  type ReactiveValue,
  type ReactiveValueChangeListener
} from '../reactive/reactive';

/** The slice of StateTree that MapProperty uses. */
export interface MapPropertyTree {
  isActive(node: MapPropertyNode): boolean;
  sendNodePropertySyncToServer(property: MapProperty): void;
}

/** The slice of StateNode that MapProperty uses. */
export interface MapPropertyNode {
  getTree(): MapPropertyTree;
}

/** The slice of NodeMap that MapProperty uses. */
export interface MapPropertyOwner {
  getNode(): MapPropertyNode;
}

/** Fired when a property value changes; mirrors MapPropertyChangeEvent. */
export class MapPropertyChangeEvent extends ReactiveValueChangeEvent {
  private readonly oldValue: unknown;

  private readonly newValue: unknown;

  constructor(source: MapProperty, oldValue: unknown, newValue: unknown) {
    super(source);
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  override getSource(): MapProperty {
    return super.getSource() as MapProperty;
  }

  getOldValue(): unknown {
    return this.oldValue;
  }

  getNewValue(): unknown {
    return this.newValue;
  }
}

/** Listener for property value changes; mirrors MapPropertyChangeListener. */
export type MapPropertyChangeListener = (event: MapPropertyChangeEvent) => void;

/** A property in a node map; mirrors MapProperty.java. */
export class MapProperty implements ReactiveValue {
  static readonly NO_OP = (): void => {};

  private readonly name: string;

  private readonly map: MapPropertyOwner;

  // Indicates that a server update is in progress. While this is true we don't
  // accept any changes via syncToServer().
  private isServerUpdate = false;

  private readonly eventRouter = new ReactiveEventRouter<MapPropertyChangeListener, MapPropertyChangeEvent>(
    this,
    (listener) => listener,
    (listener, event) => listener(event)
  );

  private value: unknown = null;

  private hasValueState = false;

  private readonly forceValueUpdate: boolean;

  private previousDomValue: unknown = undefined;

  constructor(name: string, map: MapPropertyOwner, forceValueUpdate = false) {
    this.name = name;
    this.map = map;
    this.forceValueUpdate = forceValueUpdate;
  }

  getName(): string {
    return this.name;
  }

  getMap(): MapPropertyOwner {
    return this.map;
  }

  getValue(): unknown {
    this.eventRouter.registerRead();
    return this.value;
  }

  hasValue(): boolean {
    this.eventRouter.registerRead();
    return this.hasValueState;
  }

  setValue(value: unknown): void {
    // mark as server update is in progress
    this.isServerUpdate = true;
    this.doSetValue(value);
    // unmark server update at the end of flush, i.e. at the end of the current
    // server request processing
    Reactive.addPostFlushListener(() => {
      this.isServerUpdate = false;
    });
  }

  removeValue(): void {
    if (this.hasValueState) {
      this.isServerUpdate = true;
      this.updateValue(null, false);
      Reactive.addPostFlushListener(() => {
        this.isServerUpdate = false;
      });
    }
  }

  private doSetValue(value: unknown): void {
    if (!this.forceValueUpdate && this.hasValueState && value === this.value) {
      // Nothing to do
      return;
    }
    this.updateValue(value, true);
  }

  private updateValue(value: unknown, hasValue: boolean): void {
    const oldValue = this.value;

    this.hasValueState = hasValue;
    this.value = value;

    this.eventRouter.fireEvent(new MapPropertyChangeEvent(this, oldValue, value));
  }

  addChangeListener(listener: MapPropertyChangeListener): EventRemover {
    return this.eventRouter.addListener(listener);
  }

  addReactiveValueChangeListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.eventRouter.addReactiveListener(reactiveValueChangeListener);
  }

  getValueOrDefault(defaultValue: number): number;
  getValueOrDefault(defaultValue: boolean): boolean;
  getValueOrDefault(defaultValue: string): string;
  getValueOrDefault(defaultValue: number | boolean | string): number | boolean | string {
    if (this.hasValue()) {
      const v = this.getValue();
      if (v === null || v === undefined) {
        return defaultValue;
      }
      if (typeof defaultValue === 'number') {
        // Server side sets everything as double; mirror Double.intValue()
        return Math.trunc(v as number);
      }
      return v as boolean | string;
    }
    return defaultValue;
  }

  syncToServer(newValue: unknown): void {
    this.getSyncToServerCommand(newValue)();
  }

  getSyncToServerCommand(newValue: unknown): () => void {
    const currentValue = this.hasValue() ? this.getValue() : null;

    if (newValue === currentValue) {
      // Unlock client side updates here so that another client side change for
      // the same property can be propagated once the server value is set.
      this.isServerUpdate = false;
    }
    if (!(newValue === currentValue && this.hasValueState) && !this.isServerUpdate) {
      const node = this.getMap().getNode();
      const tree = node.getTree();
      if (tree.isActive(node)) {
        this.doSetValue(newValue);

        return () => tree.sendNodePropertySyncToServer(this);
      }
      // Fire a fake event to reset the property value back in the DOM element:
      // it has to be set to the property value because of the listener added to
      // the property during binding.
      this.eventRouter.fireEvent(new MapPropertyChangeEvent(this, currentValue, currentValue));
      // Flush is needed because we are out of the normal lifecycle which calls
      // flush() automatically.
      Reactive.flush();
    }
    return MapProperty.NO_OP;
  }

  setPreviousDomValue(previousDomValue: unknown): void {
    this.previousDomValue = previousDomValue === null ? undefined : previousDomValue;
  }

  getPreviousDomValue(): unknown {
    return this.previousDomValue;
  }

  clearPreviousDomValue(): void {
    this.previousDomValue = undefined;
  }
}
