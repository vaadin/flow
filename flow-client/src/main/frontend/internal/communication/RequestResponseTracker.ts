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

import type { EventRemover } from '../reactive/reactive';
import { ResynchronizationState } from './ResynchronizationState';

/** The slice of Registry that RequestResponseTracker uses. */
interface RequestResponseRegistry {
  getUILifecycle(): { isRunning(): boolean };
  getServerRpcQueue(): { isFlushPending(): boolean };
  getMessageSender(): {
    getResynchronizationState(): ResynchronizationState;
    hasQueuedMessages(): boolean;
    sendInvocationsToServer(): void;
  };
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
  private hasActiveRequestState = false;

  private readonly registry: RequestResponseRegistry;

  private readonly requestStartingListeners = new Set<VoidListener>();

  private readonly responseHandlingStartedListeners = new Set<VoidListener>();

  private readonly responseHandlingEndedListeners = new Set<VoidListener>();

  private readonly reconnectionAttemptListeners = new Set<ReconnectionAttemptListener>();

  constructor(registry: RequestResponseRegistry) {
    this.registry = registry;
  }

  /** Marks that a new request has started and fires the request-starting event. */
  startRequest(): void {
    if (this.hasActiveRequestState) {
      throw new Error('Trying to start a new request while another is active');
    }
    this.hasActiveRequestState = true;
    this.requestStartingListeners.forEach((listener) => listener());
  }

  /** Whether there is an active UIDL request. */
  hasActiveRequest(): boolean {
    return this.hasActiveRequestState;
  }

  /**
   * Marks that the current request has ended, sending any pending invocations
   * and firing the response-handling-ended event.
   */
  endRequest(): void {
    if (!this.hasActiveRequestState) {
      throw new Error('endRequest called when no request is active');
    }
    // sendInvocationsToServer() may start a new request, so clear the flag first.
    this.hasActiveRequestState = false;

    const messageSender = this.registry.getMessageSender();
    if (
      (this.registry.getUILifecycle().isRunning() && this.registry.getServerRpcQueue().isFlushPending()) ||
      messageSender.getResynchronizationState() === ResynchronizationState.SEND_TO_SERVER ||
      messageSender.hasQueuedMessages()
    ) {
      messageSender.sendInvocationsToServer();
    }

    this.responseHandlingEndedListeners.forEach((listener) => listener());
  }

  /** Fires the response-handling-started event (called by the message handler). */
  fireResponseHandlingStarted(): void {
    this.responseHandlingStartedListeners.forEach((listener) => listener());
  }

  /** Fires a reconnection-attempt event with the attempt count. */
  fireReconnectionAttempt(attempt: number): void {
    this.reconnectionAttemptListeners.forEach((listener) => listener(attempt));
  }

  addRequestStartingHandler(handler: VoidListener): EventRemover {
    return addListener(this.requestStartingListeners, handler);
  }

  addResponseHandlingStartedHandler(handler: VoidListener): EventRemover {
    return addListener(this.responseHandlingStartedListeners, handler);
  }

  addResponseHandlingEndedHandler(handler: VoidListener): EventRemover {
    return addListener(this.responseHandlingEndedListeners, handler);
  }

  addReconnectionAttemptHandler(handler: ReconnectionAttemptListener): EventRemover {
    return addListener(this.reconnectionAttemptListeners, handler);
  }
}
