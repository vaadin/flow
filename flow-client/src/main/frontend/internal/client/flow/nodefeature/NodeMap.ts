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
import { MapProperty } from './MapProperty';
import { MapPropertyAddEvent } from './MapPropertyAddEvent';
import { NodeFeature } from './NodeFeature';

// `NodeFeatures.ELEMENT_PROPERTIES` numeric id; mirrors the Java constant
// referenced when deciding whether the `innerHTML` property forces value
// updates. Source of truth lives server-side in
// com.vaadin.flow.internal.nodefeature.NodeFeatures.
const ELEMENT_PROPERTIES = 1;

type AnyListener = any;

type StateNodeLike = any;

/**
 * State-node map feature. Migrated from
 * `com.vaadin.client.flow.nodefeature.NodeMap`. Properties are keyed by name
 * (string); creation of a new property fires a {@link MapPropertyAddEvent} on
 * the internal reactive event router.
 */
export class NodeMap extends NodeFeature {
  private readonly properties = new Map<string, MapProperty>();
  private readonly eventRouter: ReactiveEventRouter;

  constructor(id: number, node: StateNodeLike) {
    super(id, node);
    // Mirrors Java NodeMap: wrap turns a ReactiveValueChangeListener into a
    // MapPropertyAddListener whose `onPropertyAdd` fires the value-change
    // handler — so iterating properties registers a reactive dependency that
    // is invalidated when a new property is added.
    this.eventRouter = new ReactiveEventRouter(
      this,
      (listener: AnyListener) => ({
        onPropertyAdd: (event: unknown): void => listener.onValueChange(event)
      }),
      (listener: AnyListener, event: unknown): void => listener.onPropertyAdd(event)
    );
  }

  getProperty(name: string): MapProperty {
    let property = this.properties.get(name);
    if (!property) {
      const forceValueUpdate = name === 'innerHTML' && this.getId() === ELEMENT_PROPERTIES;
      property = new MapProperty(name, this, forceValueUpdate);
      this.properties.set(name, property);
      this.eventRouter.fireEvent(new MapPropertyAddEvent(this, property));
    }
    return property;
  }

  hasPropertyValue(name: string): boolean {
    const p = this.properties.get(name);
    return p != null && p.hasValue();
  }

  forEachProperty(callback: (property: MapProperty, name: string) => void): void {
    this.eventRouter.registerRead();
    this.properties.forEach((property, name) => callback(property, name));
  }

  getPropertyNames(): string[] {
    const names: string[] = [];
    this.forEachProperty((_property, name) => names.push(name));
    return names;
  }

  override getDebugJson(): unknown {
    const json: Record<string, unknown> = {};
    let any = false;
    this.properties.forEach((p, n) => {
      if (p.hasPropertyValue()) {
        json[n] = this.getAsDebugJson(p.getValue());
        any = true;
      }
    });
    return any ? json : null;
  }

  override convert(converter: (value: unknown) => unknown): unknown {
    const json: Record<string, unknown> = {};
    this.properties.forEach((property, name) => {
      if (property.hasPropertyValue()) {
        json[name] = converter(property.getValue());
      }
    });
    return json;
  }

  addReactiveValueChangeListener(listener: AnyListener): { remove(): void } {
    return this.eventRouter.addReactiveListener(listener);
  }

  addPropertyAddListener(listener: AnyListener): { remove(): void } {
    // Accept either a bare `(event) => void` callback (the common Java
    // `MapPropertyAddListener` lambda) or an object that already has
    // `onPropertyAdd`. Normalize to the latter so dispatchFn finds the
    // expected method.
    const adapted = typeof listener === 'function' ? { onPropertyAdd: listener } : listener;
    return this.eventRouter.addListener(adapted);
  }
}
