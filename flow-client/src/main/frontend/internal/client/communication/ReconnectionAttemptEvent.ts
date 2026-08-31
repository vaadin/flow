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

// TypeScript port of com.vaadin.client.communication.ReconnectionAttemptEvent. The GWT
// Event/EventBus plumbing it inherits (`getType`, `getAssociatedType`,
// `dispatch`) has no counterpart here: RequestResponseTracker notifies its
// handler lists directly, so only the event and its handler are ported.

/**
 * Event fired when a reconnection attempt is requested.
 */
export class ReconnectionAttemptEvent {
  readonly #attempt: number;

  /**
   * Creates an event object.
   *
   * @param attempt - the reconnection attempt number
   */
  constructor(attempt: number) {
    this.#attempt = attempt;
  }

  /**
   * Gets the number of the current reconnection attempt.
   *
   * @returns the number of the current reconnection attempt.
   */
  getAttempt(): number {
    return this.#attempt;
  }
}

/**
 * Handler for {@link ReconnectionAttemptEvent}s.
 *
 * @param event - the event object
 */
export type ReconnectionAttemptEventHandler = (event: ReconnectionAttemptEvent) => void;
