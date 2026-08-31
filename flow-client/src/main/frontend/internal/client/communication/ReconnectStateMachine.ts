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

// The reconnect-decision core of
// com.vaadin.client.communication.DefaultConnectionStateHandler, extracted as a
// testable unit. It decides, when a recoverable communication error occurs,
// which transport's failure is the active reconnection cause (by
// ConnectionMessageType priority), counts the attempts, gives up after the
// configured maximum, and resolves the temporary error when a connection
// succeeds. The actual reconnect retry (timer + payload re-send) and the full
// ConnectionStateHandler push/xhr handlers compose this kernel.

import type { Command } from '../Command';
import type { Heartbeat } from './Heartbeat';
import type { LoadingIndicatorStateHandler } from './LoadingIndicatorStateHandler';
import type { ReconnectConfiguration } from './ReconnectConfiguration';
import type { RequestResponseTracker } from './RequestResponseTracker';
import type { UILifecycle } from '../UILifecycle';
import { CONNECTED, CONNECTION_LOST, RECONNECTING, setState } from '../ConnectionIndicator';
import { ConnectionMessageType, isHigherPriorityThan } from './ConnectionMessageType';
import { Console } from '../Console';

/** The slice of Registry the reconnect state machine uses. */
interface ReconnectRegistry {
  getUILifecycle(): Pick<UILifecycle, 'isRunning'>;
  getReconnectConfiguration(): Pick<ReconnectConfiguration, 'getReconnectAttempts'>;
  getRequestResponseTracker(): Pick<RequestResponseTracker, 'hasActiveRequest' | 'endRequest'>;
  getLoadingIndicatorStateHandler(): Pick<LoadingIndicatorStateHandler, 'stopLoading'>;
  getHeartbeat(): Pick<Heartbeat, 'setInterval'>;
}

/**
 * Tracks reconnection state and decides retry/give-up; mirrors the
 * handleRecoverableError / resolveTemporaryError / giveUp logic of
 * DefaultConnectionStateHandler.
 */
export class ReconnectStateMachine {
  #reconnectionCause: ConnectionMessageType | null = null;

  #reconnectAttempt = 0;

  readonly #registry: ReconnectRegistry;

  // Performs the actual reconnect retry (timer + payload re-send) — supplied by
  // the full handler; cancels any scheduled retry on resolution.
  readonly #scheduleReconnect: (payload: unknown) => void;

  readonly #cancelScheduledReconnect: Command;

  /**
   * Creates a new instance connected to the given registry, driving the retry
   * mechanics the full handler owns.
   *
   * @param registry - the registry, narrowed to the members this machine reads
   * @param scheduleReconnect - schedules the next reconnect attempt, receiving
   *          the message which did not reach the server, or null if no message
   *          was involved (heartbeat or push connection failed)
   * @param cancelScheduledReconnect - cancels a reconnect this machine has
   *          already scheduled; defaults to doing nothing, for a caller that
   *          schedules nothing cancellable
   */
  constructor(
    registry: ReconnectRegistry,
    scheduleReconnect: (payload: unknown) => void,
    cancelScheduledReconnect: Command = () => {}
  ) {
    this.#registry = registry;
    this.#scheduleReconnect = scheduleReconnect;
    this.#cancelScheduledReconnect = cancelScheduledReconnect;
  }

  /**
   * Checks if we are currently trying to reconnect.
   *
   * @returns true if we have noted a problem and are trying to re-establish
   *          server connection, false otherwise
   */
  isReconnecting(): boolean {
    return this.#reconnectionCause !== null;
  }

  /** The current reconnection cause, or null if not reconnecting. */
  getReconnectionCause(): ConnectionMessageType | null {
    return this.#reconnectionCause;
  }

  /** The number of reconnection attempts made for the current cause. */
  getReconnectAttempt(): number {
    return this.#reconnectAttempt;
  }

  /**
   * Called whenever an error occurs in communication which should be handled by
   * showing the reconnect dialog and retrying communication until successful
   * again.
   *
   * @param type - The type of failure detected
   * @param payload - The message which did not reach the server, or null if no
   *          message was involved (heartbeat or push connection failed)
   */
  handleRecoverableError(type: ConnectionMessageType, payload: unknown): void {
    if (!this.#registry.getUILifecycle().isRunning()) {
      return;
    }
    setState(RECONNECTING);

    if (!this.isReconnecting()) {
      // First problem encountered.
      this.#reconnectionCause = type;
    } else if (isHigherPriorityThan(type, this.#reconnectionCause!)) {
      // A higher-priority failure (HEARTBEAT < PUSH < XHR) takes over; resolving
      // it implies the lower-priority one is resolved too.
      this.#reconnectionCause = type;
    }

    if (this.#reconnectionCause !== type) {
      return;
    }

    this.#reconnectAttempt++;
    Console.debug(`Reconnect attempt ${this.#reconnectAttempt} for ${type}`);
    if (this.#reconnectAttempt >= this.#registry.getReconnectConfiguration().getReconnectAttempts()) {
      // Max attempts reached -> give up (CONNECTION_LOST).
      this.giveUp();
    } else {
      this.#scheduleReconnect(payload);
    }
  }

  /** Resolves the temporary error for the given type if it is the active cause. Mirrors resolveTemporaryError. */
  resolveTemporaryError(type: ConnectionMessageType): void {
    if (this.#reconnectionCause !== type) {
      // Waiting for some other (higher-priority) problem to be resolved.
      return;
    }
    this.#reconnectionCause = null;
    this.#reconnectAttempt = 0;
    this.#cancelScheduledReconnect();
    if (type === ConnectionMessageType.HEARTBEAT) {
      // Heartbeat has no loading indication; safe to set CONNECTED directly.
      setState(CONNECTED);
    } else {
      this.#registry.getLoadingIndicatorStateHandler().stopLoading();
    }
    Console.debug('Re-established connection to server');
  }

  /** Stops reconnecting and goes to CONNECTION_LOST. Mirrors giveUp. */
  giveUp(): void {
    this.#reconnectionCause = null;
    if (this.#registry.getRequestResponseTracker().hasActiveRequest()) {
      this.#registry.getRequestResponseTracker().endRequest();
    }
    setState(CONNECTION_LOST);
    // pauseHeartbeats: 0 pauses but stays resumable (-1 means terminated).
    this.#registry.getHeartbeat().setInterval(0);
  }
}
