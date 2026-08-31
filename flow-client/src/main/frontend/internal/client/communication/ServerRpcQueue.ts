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

// TypeScript port of com.vaadin.client.communication.ServerRpcQueue, built
// alongside the Java version. It accumulates pending RPC invocations and flushes
// them to the server (via the MessageSender) deferred, so all event handlers run
// before the queue is sent. The Registry/UILifecycle/MessageSender are contracts
// satisfied at cutover.

import type { MessageSender } from './MessageSender';
import type { UILifecycle } from '../UILifecycle';
import { getScheduler } from '../TrackingScheduler';
import { Console } from '../Console';

// Sentinel flush strategy; identity-compared to detect a scheduled flush.
const NO_OP = (): void => {};

/** The slice of Registry that ServerRpcQueue uses. */
interface ServerRpcQueueRegistry {
  getUILifecycle(): Pick<UILifecycle, 'isRunning'>;
  getMessageSender(): Pick<MessageSender, 'sendInvocationsToServer'>;
}

/** Accumulates and flushes server RPC invocations; mirrors ServerRpcQueue.java. */
export class ServerRpcQueue {
  #pendingInvocations: unknown[] = [];

  #flushPending = false;

  readonly #registry: ServerRpcQueueRegistry;

  #doFlushStrategy: () => void = NO_OP;

  constructor(registry: ServerRpcQueueRegistry) {
    this.#registry = registry;
  }

  /**
   * Adds an explicit RPC method invocation to the send queue.
   *
   * @param invocation - RPC method invocation
   */
  add(invocation: unknown): void {
    if (!this.#registry.getUILifecycle().isRunning()) {
      Console.warn('Trying to invoke method on not yet started or stopped application');
      return;
    }
    this.#pendingInvocations.push(invocation);
  }

  /** Clears the queue and cancels any scheduled flush. */
  clear(): void {
    this.#pendingInvocations = [];
    this.#flushPending = false;
    this.#doFlushStrategy = NO_OP;
  }

  /**
   * Returns the current size of the queue.
   *
   * @returns the number of invocations in the queue
   */
  size(): number {
    return this.#pendingInvocations.length;
  }

  /**
   * Checks if the queue is empty.
   *
   * @returns true if the queue is empty, false otherwise
   */
  isEmpty(): boolean {
    return this.size() === 0;
  }

  /** Triggers a deferred send of the queued invocations to the server. */
  flush(): void {
    if (this.#isFlushScheduled() || this.isEmpty()) {
      return;
    }
    this.#flushPending = true;

    this.#doFlushStrategy = () => this.#doFlush();
    // Deferred so all event handlers run before the queue is flushed. Scheduled
    // through the shared TrackingScheduler (mirrors GWT's Scheduler.get()) so the
    // pending flush keeps ApplicationConnection.isActive true until the resulting
    // request completes — otherwise TestBench's waitForVaadin returns before the
    // event is even sent.
    getScheduler().scheduleDeferred(() => this.#doFlushStrategy());
  }

  /**
   * Checks if a flush operation is pending.
   *
   * @returns true if a flush is pending, false otherwise
   */
  isFlushPending(): boolean {
    return this.#flushPending;
  }

  /**
   * Checks if a loading indicator should be shown when the RPCs have been sent to the
   * server and we are waiting for a response.
   *
   * @returns true if a loading indicator should be shown, false otherwise
   */
  showLoadingIndicator(): boolean {
    return true;
  }

  /**
   * Returns the current invocations as JSON.
   *
   * @returns the current invocations in a JSON format ready to be sent to the * server
   */
  toJson(): unknown[] {
    return this.#pendingInvocations;
  }

  #isFlushScheduled(): boolean {
    return this.#doFlushStrategy !== NO_OP;
  }

  #doFlush(): void {
    this.#doFlushStrategy = NO_OP;
    if (!this.isFlushPending()) {
      // Somebody else cleared the queue before we had the chance.
      return;
    }
    this.#registry.getMessageSender().sendInvocationsToServer();
  }
}
