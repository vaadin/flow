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

// TypeScript port of com.vaadin.client.flow.reactive.InvalidateEvent.

import type { Computation } from './Computation';

/**
 * Event fired when a computation is invalidated.
 */
export class InvalidateEvent {
  readonly #source: Computation;

  /**
   * Creates a new event for computation.
   *
   * @param source - the invalidated computation
   */
  constructor(source: Computation) {
    this.#source = source;
  }

  /**
   * Gets the invalidated computation.
   *
   * @returns the invalidated computation
   */
  getSource(): Computation {
    return this.#source;
  }
}
