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

// TypeScript port of com.vaadin.client.flow.nodefeature.MapPropertyChangeEvent.

import { ReactiveValueChangeEvent } from '../reactive/ReactiveValueChangeEvent';
import type { MapProperty } from './MapProperty';

/**
 * Event fired when the value of a map property changes.
 */
export class MapPropertyChangeEvent extends ReactiveValueChangeEvent {
  readonly #oldValue: unknown;

  readonly #newValue: unknown;

  /**
   * Creates a new map property change event.
   *
   * @param source - the changed map property
   * @param oldValue - the old value
   * @param newValue - the new value
   */
  constructor(source: MapProperty, oldValue: unknown, newValue: unknown) {
    super(source);
    this.#oldValue = oldValue;
    this.#newValue = newValue;
  }

  override getSource(): MapProperty {
    return super.getSource() as MapProperty;
  }

  /**
   * Gets the old property value.
   *
   * @returns the old value
   */
  getOldValue(): unknown {
    return this.#oldValue;
  }

  /**
   * Gets the new property value.
   *
   * @returns the new value
   */
  getNewValue(): unknown {
    return this.#newValue;
  }
}
