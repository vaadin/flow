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

type Registration = { removeHandler(): void };
type IntHandler = (attempt: number) => void;

/**
 * Tracks active server UIDL requests, with simple per-event callback queues
 * replacing the GWT EventBus the Java original used. Migrated from
 * `com.vaadin.client.communication.RequestResponseTracker`.
 *
 * Construction takes a `maybeFlushInvocations` callback that DefaultRegistry
 * wires to the original endRequest "should we send queued invocations now?"
 * decision; the TS class doesn't need to dispatch back through the still-Java
 * MessageSender / ServerRpcQueue facades.
 */
export class RequestResponseTracker {
  private activeRequest = false;
  private readonly maybeFlushInvocations: () => void;
  private readonly requestStartingHandlers = new Set<() => void>();
  private readonly responseHandlingStartedHandlers = new Set<() => void>();
  private readonly responseHandlingEndedHandlers = new Set<() => void>();
  private readonly reconnectionAttemptHandlers = new Set<IntHandler>();

  constructor(maybeFlushInvocations: () => void) {
    this.maybeFlushInvocations = maybeFlushInvocations;
  }

  startRequest(): void {
    if (this.activeRequest) {
      throw new Error('Trying to start a new request while another is active');
    }
    this.activeRequest = true;
    this.requestStartingHandlers.forEach((h) => h());
  }

  hasActiveRequest(): boolean {
    return this.activeRequest;
  }

  endRequest(): void {
    if (!this.activeRequest) {
      throw new Error('endRequest called when no request is active');
    }
    // Must be cleared before maybeFlushInvocations so a freshly-started
    // follow-up request can succeed.
    this.activeRequest = false;
    this.maybeFlushInvocations();
    this.responseHandlingEndedHandlers.forEach((h) => h());
  }

  fireResponseHandlingStarted(): void {
    this.responseHandlingStartedHandlers.forEach((h) => h());
  }

  fireReconnectionAttempt(attempt: number): void {
    this.reconnectionAttemptHandlers.forEach((h) => h(attempt));
  }

  addRequestStartingHandler(handler: () => void): Registration {
    this.requestStartingHandlers.add(handler);
    return { removeHandler: () => this.requestStartingHandlers.delete(handler) };
  }

  addResponseHandlingStartedHandler(handler: () => void): Registration {
    this.responseHandlingStartedHandlers.add(handler);
    return { removeHandler: () => this.responseHandlingStartedHandlers.delete(handler) };
  }

  addResponseHandlingEndedHandler(handler: () => void): Registration {
    this.responseHandlingEndedHandlers.add(handler);
    return { removeHandler: () => this.responseHandlingEndedHandlers.delete(handler) };
  }

  addReconnectionAttemptHandler(handler: IntHandler): Registration {
    this.reconnectionAttemptHandlers.add(handler);
    return { removeHandler: () => this.reconnectionAttemptHandlers.delete(handler) };
  }
}
