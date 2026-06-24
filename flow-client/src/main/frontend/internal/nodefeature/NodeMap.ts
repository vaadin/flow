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

// TypeScript port of com.vaadin.client.flow.nodefeature.NodeMap (and its
// property-add event/listener), built alongside the Java version on top of the
// TS reactive core and MapProperty.

import {
  ReactiveEventRouter,
  ReactiveValueChangeEvent,
  type EventRemover,
  type ReactiveValue,
  type ReactiveValueChangeListener
} from '../reactive/reactive';
import { MapProperty, type MapPropertyOwner } from './MapProperty';
import { NodeFeature, type JsonValue } from './NodeFeature';

// com.vaadin.flow.internal.nodefeature.NodeFeatures.ELEMENT_PROPERTIES
const ELEMENT_PROPERTIES = 1;

/** Fired when a new property is added to a map; mirrors MapPropertyAddEvent. */
export class MapPropertyAddEvent extends ReactiveValueChangeEvent {
  private readonly property: MapProperty;

  constructor(source: NodeMap, property: MapProperty) {
    super(source);
    this.property = property;
  }

  override getSource(): NodeMap {
    return super.getSource() as NodeMap;
  }

  getProperty(): MapProperty {
    return this.property;
  }
}

/** Listener for property additions; mirrors MapPropertyAddListener. */
export type MapPropertyAddListener = (event: MapPropertyAddEvent) => void;

/** A state node feature that structures data as a map; mirrors NodeMap.java. */
export class NodeMap extends NodeFeature implements ReactiveValue, MapPropertyOwner {
  private readonly properties = new Map<string, MapProperty>();

  private readonly eventRouter = new ReactiveEventRouter<MapPropertyAddListener, MapPropertyAddEvent>(
    this,
    (reactiveValueChangeListener) => reactiveValueChangeListener,
    (listener, event) => listener(event)
  );

  getProperty(name: string): MapProperty {
    let property = this.properties.get(name);
    if (property === undefined) {
      property = new MapProperty(name, this, name === 'innerHTML' && this.getId() === ELEMENT_PROPERTIES);
      this.properties.set(name, property);

      this.eventRouter.fireEvent(new MapPropertyAddEvent(this, property));
    }

    return property;
  }

  hasPropertyValue(name: string): boolean {
    const property = this.properties.get(name);
    if (property === undefined) {
      return false;
    }
    return property.hasValue();
  }

  forEachProperty(callback: (property: MapProperty, name: string) => void): void {
    this.eventRouter.registerRead();
    this.properties.forEach((property, name) => callback(property, name));
  }

  getPropertyNames(): string[] {
    const list: string[] = [];
    this.forEachProperty((_property, name) => list.push(name));
    return list;
  }

  override getDebugJson(): JsonValue {
    const json: Record<string, JsonValue> = {};

    this.properties.forEach((p, n) => {
      if (p.hasValue()) {
        json[n] = this.getAsDebugJson(p.getValue());
      }
    });

    if (Object.keys(json).length === 0) {
      return null;
    }

    return json;
  }

  override convert(converter: (value: unknown) => JsonValue): JsonValue {
    const json: Record<string, JsonValue> = {};

    this.properties.forEach((property, name) => {
      if (property.hasValue()) {
        json[name] = converter(property.getValue());
      }
    });

    return json;
  }

  addReactiveValueChangeListener(reactiveValueChangeListener: ReactiveValueChangeListener): EventRemover {
    return this.eventRouter.addReactiveListener(reactiveValueChangeListener);
  }

  addPropertyAddListener(listener: MapPropertyAddListener): EventRemover {
    return this.eventRouter.addListener(listener);
  }
}
