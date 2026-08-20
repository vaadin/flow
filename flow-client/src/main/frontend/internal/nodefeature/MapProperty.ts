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

// TypeScript port of com.vaadin.client.flow.nodefeature.MapProperty, on top of
// the TS reactive core. The thin slice of the state-tree classes it touches is
// declared here as contracts that NodeMap, StateNode and StateTree satisfy.

import type { EventRemover } from '../EventRemover';
import { Reactive } from '../reactive/Reactive';
import { ReactiveEventRouter } from '../reactive/ReactiveEventRouter';
import type { ReactiveValue } from '../reactive/ReactiveValue';
import type { ReactiveValueChangeListener } from '../reactive/ReactiveValueChangeListener';
import { MapPropertyChangeEvent } from './MapPropertyChangeEvent';
import type { MapPropertyChangeListener } from './MapPropertyChangeListener';

/**
 * Port deviation: the slice of `StateTree` that `MapProperty` uses. This
 * interface has no Java counterpart; it decouples `MapProperty` from the
 * concrete state tree, and is satisfied by the state-tree classes.
 */
export interface MapPropertyTree {
  isActive(node: MapPropertyNode): boolean;
  sendNodePropertySyncToServer(property: MapProperty): void;
}

/**
 * Port deviation: the slice of `StateNode` that `MapProperty` uses. This
 * interface has no Java counterpart; it decouples `MapProperty` from the
 * concrete state node, and is satisfied by the state-tree classes.
 */
export interface MapPropertyNode {
  getTree(): MapPropertyTree;
}

/**
 * Port deviation: the slice of `NodeMap` that `MapProperty` uses. This
 * interface has no Java counterpart; it decouples `MapProperty` from the
 * concrete node map, and is satisfied by the state-tree classes.
 */
export interface MapPropertyOwner {
  getNode(): MapPropertyNode;
}

/**
 * A property in a node map.
 */
export class MapProperty implements ReactiveValue {
  /**
   * A command that does nothing, returned when there is nothing to synchronize
   * to the server.
   */
  static readonly NO_OP = (): void => {};

  readonly #name: string;

  readonly #map: MapPropertyOwner;

  // Indicates that a server update is in progress. While this is true we don't
  // accept any changes via syncToServer().
  #isServerUpdate = false;

  readonly #eventRouter = new ReactiveEventRouter<MapPropertyChangeListener, MapPropertyChangeEvent>(
    this,
    (listener) => listener,
    (listener, event) => listener(event)
  );

  #value: unknown = null;

  #hasValueState = false;

  readonly #forceValueUpdate: boolean;

  #previousDomValue: unknown = undefined;

  /**
   * Creates a new property.
   *
   * @param name - the name of the property
   * @param map - the map that the property belongs to
   * @param forceValueUpdate - whether value update for `name` property should be
   *            applied regardless of previous value
   */
  constructor(name: string, map: MapPropertyOwner, forceValueUpdate = false) {
    this.#name = name;
    this.#map = map;
    this.#forceValueUpdate = forceValueUpdate;
  }

  /**
   * Gets the name of this property.
   *
   * @returns the property name
   */
  getName(): string {
    return this.#name;
  }

  /**
   * Gets the map that this property belongs to.
   *
   * @returns the map
   */
  getMap(): MapPropertyOwner {
    return this.#map;
  }

  /**
   * Gets the property value.
   *
   * @returns the property value
   */
  getValue(): unknown {
    this.#eventRouter.registerRead();
    return this.#value;
  }

  /**
   * Checks whether this property has a value. A property has a value if
   * {@link setValue} has been invoked after the property was created or
   * {@link removeValue} was invoked.
   *
   * @see {@link removeValue}
   *
   * @returns `true` if the property has a value, `false` if the property has no
   *         value.
   */
  hasValue(): boolean {
    this.#eventRouter.registerRead();
    return this.#hasValueState;
  }

  /**
   * Sets the property value. Changing the value fires a
   * {@link MapPropertyChangeEvent}.
   *
   * @see {@link addChangeListener}
   *
   * @param value - the new property value
   */
  setValue(value: unknown): void {
    // mark as server update is in progress
    this.#isServerUpdate = true;
    this.#doSetValue(value);
    // unmark server update at the end of flush, i.e. at the end of the current
    // server request processing
    Reactive.addPostFlushListener(() => {
      this.#isServerUpdate = false;
    });
  }

  /**
   * Removes the value of this property so that {@link hasValue} will return
   * `false` and {@link getValue} will return `null` until the next time
   * {@link setValue} is run. A {@link MapPropertyChangeEvent} will be fired if
   * this property has a value.
   *
   * Once a property has been created, it can no longer be removed from its map.
   * The same semantics as e.g. `Map#remove(Object)` is instead provided by
   * marking the value of the property as removed to distinguish it from
   * assigning `null` as the value.
   */
  removeValue(): void {
    if (this.#hasValueState) {
      this.#isServerUpdate = true;
      this.#updateValue(null, false);
      Reactive.addPostFlushListener(() => {
        this.#isServerUpdate = false;
      });
    }
  }

  #doSetValue(value: unknown): void {
    if (!this.#forceValueUpdate && this.#hasValueState && value === this.#value) {
      // Nothing to do
      return;
    }
    this.#updateValue(value, true);
  }

  #updateValue(value: unknown, hasValue: boolean): void {
    const oldValue = this.#value;

    this.#hasValueState = hasValue;
    this.#value = value;

    this.#eventRouter.fireEvent(new MapPropertyChangeEvent(this, oldValue, value));
  }

  /**
   * Adds a listener that gets notified when the value of this property changes.
   *
   * @param listener - the property change listener to add
   * @returns an event remover for unregistering the listener
   */
  addChangeListener(listener: MapPropertyChangeListener): EventRemover {
    return this.#eventRouter.addListener(listener);
  }

  addReactiveValueChangeListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.#eventRouter.addReactiveListener(reactiveValueChangeListener);
  }

  /**
   * Returns the value, or the given defaultValue if the property does not have
   * a value or the property value is null.
   *
   * @param defaultValue - the default value
   * @returns the value of the property or the default value if the property
   *         does not have a value or the property value is null
   */
  getValueOrDefault(defaultValue: number): number;
  /**
   * Returns the value, or the given defaultValue if the property does not have
   * a value or the property value is null.
   *
   * @param defaultValue - the default value
   * @returns the value of the property or the default value if the property
   *         does not have a value or the property value is null
   */
  getValueOrDefault(defaultValue: boolean): boolean;
  /**
   * Returns the value, or the given defaultValue if the property does not have
   * a value or the property value is null.
   *
   * @param defaultValue - the default value
   * @returns the value of the property or the default value if the property
   *         does not have a value or the property value is null
   */
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

  /**
   * Sets the value of this property and synchronizes the value to the server.
   *
   * @param newValue - the new value to set.
   * @see {@link getSyncToServerCommand}
   */
  syncToServer(newValue: unknown): void {
    this.getSyncToServerCommand(newValue)();
  }

  /**
   * Sets the value of this property and returns a synch to server command.
   *
   * @param newValue - the new value to set.
   * @returns a command that synchronizes the value to the server
   * @see {@link syncToServer}
   */
  getSyncToServerCommand(newValue: unknown): () => void {
    const currentValue = this.hasValue() ? this.getValue() : null;

    if (newValue === currentValue) {
      // Unlock client side updates here so that another client side change for
      // the same property can be propagated once the server value is set.
      this.#isServerUpdate = false;
    }
    if (!(newValue === currentValue && this.#hasValueState) && !this.#isServerUpdate) {
      const node = this.getMap().getNode();
      const tree = node.getTree();
      if (tree.isActive(node)) {
        this.#doSetValue(newValue);

        return () => tree.sendNodePropertySyncToServer(this);
      }
      // Fire a fake event to reset the property value back in the DOM element:
      // it has to be set to the property value because of the listener added to
      // the property during binding.
      this.#eventRouter.fireEvent(new MapPropertyChangeEvent(this, currentValue, currentValue));
      // Flush is needed because we are out of the normal lifecycle which calls
      // flush() automatically.
      Reactive.flush();
    }
    return MapProperty.NO_OP;
  }

  /**
   * Stores previous DOM value of this property for detection of value
   * modification by the user during the server round-trip.
   *
   * @param previousDomValue - DOM value of property prior to server round-trip
   *            start. Can be `null`;
   */
  setPreviousDomValue(previousDomValue: unknown): void {
    this.#previousDomValue = previousDomValue === null ? undefined : previousDomValue;
  }

  /**
   * Returns previous DOM value of this property for detection of value
   * modification by the user during the server round-trip.
   *
   * @returns previous DOM value, or `undefined` if not stored.
   */
  getPreviousDomValue(): unknown {
    return this.#previousDomValue;
  }

  /**
   * Clears the previous DOM value of this property.
   */
  clearPreviousDomValue(): void {
    this.#previousDomValue = undefined;
  }
}
