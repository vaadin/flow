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
// It ensures a single active server request at a time and fires
// request-starting / response-handling-started/ended and reconnection-attempt
// events. The GWT EventBus is replaced by per-event-type listener sets.

import type { MessageSender } from './MessageSender';
import type { ServerRpcQueue } from './ServerRpcQueue';
import type { UILifecycle } from '../UILifecycle';
import type { EventRemover } from '../../EventRemover';
import { ResynchronizationState } from './ResynchronizationState';

/** The slice of Registry that RequestResponseTracker uses. */
interface RequestResponseRegistry {
  getUILifecycle(): Pick<UILifecycle, 'isRunning'>;
  getServerRpcQueue(): Pick<ServerRpcQueue, 'isFlushPending'>;
  getMessageSender(): Pick<
    MessageSender,
    'getResynchronizationState' | 'hasQueuedMessages' | 'sendInvocationsToServer'
  >;
}

type VoidListener = () => void;
type ReconnectionAttemptListener = (attempt: number) => void;

function addListener<T>(listeners: Set<T>, listener: T): EventRemover {
  listeners.add(listener);
  return {
    remove: () => {
      listeners.delete(listener);
    }
  };
}

/** Tracks active server UIDL requests and fires their lifecycle events; mirrors RequestResponseTracker.java. */
export class RequestResponseTracker {
  #hasActiveRequestState = false;

  readonly #registry: RequestResponseRegistry;

  readonly #requestStartingListeners = new Set<VoidListener>();

  readonly #responseHandlingStartedListeners = new Set<VoidListener>();

  readonly #responseHandlingEndedListeners = new Set<VoidListener>();

  readonly #reconnectionAttemptListeners = new Set<ReconnectionAttemptListener>();

  constructor(registry: RequestResponseRegistry) {
    this.#registry = registry;
  }

  /** Marks that a new request has started and fires the request-starting event. */
  startRequest(): void {
    if (this.#hasActiveRequestState) {
      throw new Error('Trying to start a new request while another is active');
    }
    this.#hasActiveRequestState = true;
    this.#requestStartingListeners.forEach((listener) => listener());
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
   * Marks that the current request has ended, sending any pending invocations
   * and firing the response-handling-ended event.
   */
  endRequest(): void {
    if (!this.#hasActiveRequestState) {
      throw new Error('endRequest called when no request is active');
    }
    // sendInvocationsToServer() may start a new request, so clear the flag first.
    this.#hasActiveRequestState = false;

    const messageSender = this.#registry.getMessageSender();
    if (
      (this.#registry.getUILifecycle().isRunning() && this.#registry.getServerRpcQueue().isFlushPending()) ||
      messageSender.getResynchronizationState() === ResynchronizationState.SEND_TO_SERVER ||
      messageSender.hasQueuedMessages()
    ) {
      messageSender.sendInvocationsToServer();
    }

    this.#responseHandlingEndedListeners.forEach((listener) => listener());
  }

  /** Fires the response-handling-started event (called by the message handler). */
  fireResponseHandlingStarted(): void {
    this.#responseHandlingStartedListeners.forEach((listener) => listener());
  }

  /** Fires a reconnection-attempt event with the attempt count. */
  fireReconnectionAttempt(attempt: number): void {
    this.#reconnectionAttemptListeners.forEach((listener) => listener(attempt));
  }

  /**
   * Adds a handler for {@link RequestStartingEvent}s.
   *
   * @param handler - the handler to add
   * @returns a registration object which can be used to remove the handler
   */
  addRequestStartingHandler(handler: VoidListener): EventRemover {
    return addListener(this.#requestStartingListeners, handler);
  }

  /**
   * Adds a handler for {@link ResponseHandlingStartedEvent}s.
   *
   * @param handler - the handler to add
   * @returns a registration object which can be used to remove the handler
   */
  addResponseHandlingStartedHandler(handler: VoidListener): EventRemover {
    return addListener(this.#responseHandlingStartedListeners, handler);
  }

  /**
   * Adds a handler for {@link ResponseHandlingEndedEvent}s.
   *
   * @param handler - the handler to add
   * @returns a registration object which can be used to remove the handler
   */
  addResponseHandlingEndedHandler(handler: VoidListener): EventRemover {
    return addListener(this.#responseHandlingEndedListeners, handler);
  }

  /**
   * Adds a handler for {@link ReconnectionAttemptEvent}s.
   *
   * @param handler - the handler to add
   * @returns a registration object which can be used to remove the handler
   */
  addReconnectionAttemptHandler(handler: ReconnectionAttemptListener): EventRemover {
    return addListener(this.#reconnectionAttemptListeners, handler);
  }
}
