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

// TypeScript port of com.vaadin.client.flow.nodefeature.NodeMap, on top of the
// TS reactive core and MapProperty.

import type { EventRemover } from '../EventRemover';
import { ReactiveEventRouter } from '../reactive/ReactiveEventRouter';
import type { ReactiveValue } from '../reactive/ReactiveValue';
import type { ReactiveValueChangeListener } from '../reactive/ReactiveValueChangeListener';
import { MapProperty, type MapPropertyOwner } from './MapProperty';
import { MapPropertyAddEvent } from './MapPropertyAddEvent';
import type { MapPropertyAddListener } from './MapPropertyAddListener';
import { NodeFeature, type JsonValue } from './NodeFeature';
import { NodeFeatures } from './NodeFeatures';

/**
 * A state node feature that structures data as a map.
 *
 * The feature works as a reactive value with regards to the set of available
 * properties. A {@link Computation} will get a dependency on this feature by
 * iterating the properties. Accessing a property by name does not create a
 * dependency. The `Computation` is invalidated when a property is
 * added (properties are never removed). It is not invalidated when the value of
 * a property changes since the property is a reactive values of its own.
 */
export class NodeMap extends NodeFeature implements ReactiveValue, MapPropertyOwner {
  readonly #properties = new Map<string, MapProperty>();

  readonly #eventRouter = new ReactiveEventRouter<MapPropertyAddListener, MapPropertyAddEvent>(
    this,
    (reactiveValueChangeListener) => reactiveValueChangeListener,
    (listener, event) => listener(event)
  );

  /**
   * Gets the property with a given name, creating it if necessary.
   *
   * A {@link MapPropertyAddEvent} is fired if a new property instance is
   * created.
   *
   * @param name - the name of the property
   * @returns the property instance
   */
  getProperty(name: string): MapProperty {
    let property = this.#properties.get(name);
    if (property === undefined) {
      property = new MapProperty(name, this, name === 'innerHTML' && this.getId() === NodeFeatures.ELEMENT_PROPERTIES);
      this.#properties.set(name, property);

      this.#eventRouter.fireEvent(new MapPropertyAddEvent(this, property));
    }

    return property;
  }

  /**
   * Checks if the given property is present and has a value.
   *
   * @param name - the name of the property to check
   * @returns true if the property exists and has a value, false otherwise
   */
  hasPropertyValue(name: string): boolean {
    const property = this.#properties.get(name);
    if (property === undefined) {
      return false;
    }
    return property.hasValue();
  }

  /**
   * Iterates all properties in this map.
   *
   * @param callback - the callback to invoke for each property
   */
  forEachProperty(callback: (property: MapProperty, name: string) => void): void {
    this.#eventRouter.registerRead();
    this.#properties.forEach((property, name) => callback(property, name));
  }

  /**
   * Gets all property names in this map.
   *
   * @returns a list with the property names, never `null`
   */
  getPropertyNames(): string[] {
    const list: string[] = [];
    this.forEachProperty((_property, name) => list.push(name));
    return list;
  }

  /**
   * Gets a JSON object representing the contents of this feature. Only
   * intended for debugging purposes.
   *
   * @returns a JSON representation
   */
  override getDebugJson(): JsonValue {
    const json: Record<string, JsonValue> = {};

    this.#properties.forEach((p, n) => {
      if (p.hasValue()) {
        json[n] = this.getAsDebugJson(p.getValue());
      }
    });

    if (Object.keys(json).length === 0) {
      return null;
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
    const json: Record<string, JsonValue> = {};

    this.#properties.forEach((property, name) => {
      if (property.hasValue()) {
        json[name] = converter(property.getValue());
      }
    });

    return json;
  }

  /**
   * Adds a listener that has a dependency to this value, and should be
   * notified when this value changes.
   *
   * @param reactiveValueChangeListener - the listener to add
   * @returns an event remover that can be used for removing the added listener
   */
  addReactiveValueChangeListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.#eventRouter.addReactiveListener(reactiveValueChangeListener);
  }

  /**
   * Adds a listener that is informed whenever a new property is added to this
   * map.
   *
   * @param listener - the property add listener
   * @returns an event remover that can be used for removing the added listener
   */
  addPropertyAddListener(listener: MapPropertyAddListener): EventRemover {
    return this.#eventRouter.addListener(listener);
  }
}
