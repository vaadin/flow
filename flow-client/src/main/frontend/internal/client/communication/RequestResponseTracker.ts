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

// TypeScript port of com.vaadin.client.communication.RequestResponseTracker.
// It ensures a single active server request at a time. The Java version fires
// its lifecycle events through the GWT EventBus; their only listeners are in the
// engine, which the port calls directly instead.

import type { Registry } from '../Registry';
import { ResynchronizationState } from './MessageSender';

/** Tracks active server UIDL requests; mirrors RequestResponseTracker.java. */
export class RequestResponseTracker {
  #hasActiveRequestState = false;

  readonly #registry: Registry;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry) {
    this.#registry = registry;
  }

  /** Marks that a new request has started. */
  startRequest(): void {
    if (this.#hasActiveRequestState) {
      throw new Error('Trying to start a new request while another is active');
    }
    this.#hasActiveRequestState = true;
  }

  /**
   * Checks is there is an active UIDL request.
   *
   * @returns true if there is an active request, false otherwise
   */
  hasActiveRequest(): boolean {
    return this.#hasActiveRequestState;
  }

  /**
   * Marks that the current request has ended, sending any pending invocations.
   */
  endRequest(): void {
    if (!this.#hasActiveRequestState) {
      throw new Error('endRequest called when no request is active');
    }
    // After sendInvocationsToServer() there may be a new active request, so the
    // flag must be cleared before, not after, the call.
    this.#hasActiveRequestState = false;

    const messageSender = this.#registry.getMessageSender();
    if (
      (this.#registry.getUILifecycle().isRunning() && this.#registry.getServerRpcQueue().isFlushPending()) ||
      messageSender.getResynchronizationState() === ResynchronizationState.SEND_TO_SERVER ||
      messageSender.hasQueuedMessages()
    ) {
      // Send the pending RPCs immediately. This might be an unnecessary
      // optimization, as ServerRpcQueue has a finally-scheduled command which
      // triggers the send if we do not do it here.
      messageSender.sendInvocationsToServer();
    }

    // Java's XhrConnection listens to the response-handling-ended event for this.
    this.#registry.getXhrConnection().clearWebkitMaybeIgnoringRequests();
  }
}
