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

// TypeScript port of com.vaadin.client.communication.DefaultConnectionStateHandler.
// It implements ConnectionStateHandler by composing the ported
// ReconnectStateMachine (the reconnect-decision core) and adding the retry
// mechanics (scheduleReconnect timer + doReconnect payload re-send / heartbeat),
// the heartbeat/xhr/push handler methods, online/offline handling, the reconnect
// dialog text, and unrecoverable-error handling.

import { setProperty, setState } from '../ConnectionIndicator';
import { ConnectionMessageType } from './ConnectionMessageType';
import type { ConnectionStateHandler } from './ConnectionStateHandler';
import type { PushConnection } from './PushConnection';
import { ReconnectStateMachine } from './ReconnectStateMachine';
import type { XhrConnectionError } from './XhrConnectionError';

const CONNECTION_LOST = 'connection-lost';
const UIDL_REFRESH_TOKEN = 'Vaadin-Refresh';
const SC_FORBIDDEN = 403;
const SC_NOT_FOUND = 404;
const SC_UNAUTHORIZED = 401;

// com.vaadin.client.UILifecycle.UIState
const TERMINATED = 'TERMINATED';

/** The slice of Registry DefaultConnectionStateHandler uses. */
interface DefaultConnectionStateRegistry {
  getUILifecycle(): {
    isRunning(): boolean;
    getState(): string;
    setState(state: string): void;
    addHandler(handler: (event: { getUiLifecycle(): { isTerminated(): boolean } }) => void): unknown;
  };
  getReconnectConfiguration(): {
    getReconnectAttempts(): number;
    getReconnectInterval(): number;
    getDialogText(): string | null;
    getDialogTextGaveUp(): string | null;
  };
  getRequestResponseTracker(): {
    hasActiveRequest(): boolean;
    endRequest(): void;
    fireReconnectionAttempt(attempt: number): void;
  };
  getLoadingIndicatorStateHandler(): { stopLoading(): void };
  getHeartbeat(): { setInterval(interval: number): void; getInterval(): number; send(): void };
  getApplicationConfiguration(): { getHeartbeatInterval(): number };
  getMessageSender(): { sendInvocationsToServer(): void };
  getSystemErrorHandler(): {
    handleSessionExpiredError(details: string | null): void;
    handleUnrecoverableError(
      caption: string,
      message: string,
      details: string,
      url: string,
      querySelector: string | null
    ): void;
  };
}

/** Handles connection state and reconnection; mirrors DefaultConnectionStateHandler.java. */
export class DefaultConnectionStateHandler implements ConnectionStateHandler {
  private readonly registry: DefaultConnectionStateRegistry;

  private readonly machine: ReconnectStateMachine;

  private scheduledReconnect: ReturnType<typeof setTimeout> | null = null;

  constructor(registry: DefaultConnectionStateRegistry) {
    this.registry = registry;
    this.machine = new ReconnectStateMachine(
      registry,
      (payload) => this.scheduleReconnect(payload),
      () => this.cancelScheduledReconnect()
    );

    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        if (this.machine.isReconnecting()) {
          this.machine.giveUp();
          this.stopApplication();
        }
        this.cancelScheduledReconnect();
      }
    });

    this.registerConnectionStateEventHandlers();
  }

  private scheduleReconnect(payload: unknown): void {
    if (this.machine.getReconnectAttempt() === 1) {
      // Try once immediately.
      this.doReconnect(payload);
    } else {
      this.scheduledReconnect = setTimeout(() => {
        this.scheduledReconnect = null;
        this.doReconnect(payload);
      }, this.registry.getReconnectConfiguration().getReconnectInterval());
    }
  }

  private cancelScheduledReconnect(): void {
    if (this.scheduledReconnect !== null) {
      clearTimeout(this.scheduledReconnect);
      this.scheduledReconnect = null;
    }
  }

  private doReconnect(payload: unknown): void {
    if (!this.registry.getUILifecycle().isRunning()) {
      console.warn('Trying to reconnect after application has been stopped. Giving up');
      return;
    }
    if (payload !== null && payload !== undefined) {
      // Re-send the queued UIDL via the reconnection-attempt listener.
      this.registry.getRequestResponseTracker().fireReconnectionAttempt(this.machine.getReconnectAttempt());
    } else {
      this.registry.getHeartbeat().send();
    }
  }

  // --- ConnectionStateHandler: heartbeat ---

  heartbeatException(_request: XMLHttpRequest, exception: Error): void {
    console.error(`Heartbeat exception: ${exception.message}`);
    this.machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
  }

  heartbeatInvalidStatusCode(xhr: XMLHttpRequest): void {
    const statusCode = xhr.status;
    console.warn(`Heartbeat request returned ${statusCode}`);
    if (statusCode === SC_FORBIDDEN) {
      this.registry.getSystemErrorHandler().handleSessionExpiredError(null);
      this.stopApplication();
    } else if (statusCode === SC_NOT_FOUND) {
      // UI closed; do nothing (the UI reacts to this).
    } else {
      this.machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
    }
  }

  heartbeatOk(): void {
    if (this.machine.isReconnecting()) {
      this.machine.resolveTemporaryError(ConnectionMessageType.HEARTBEAT);
    }
  }

  // --- ConnectionStateHandler: xhr ---

  xhrException(xhrConnectionError: XhrConnectionError): void {
    this.machine.handleRecoverableError(ConnectionMessageType.XHR, xhrConnectionError.getPayload());
  }

  xhrInvalidContent(xhrConnectionError: XhrConnectionError): void {
    this.registry.getRequestResponseTracker().endRequest();
    const responseText = xhrConnectionError.getXhr().responseText;
    if (!this.redirectIfRefreshToken(responseText)) {
      this.handleUnrecoverableCommunicationError(
        `Invalid JSON response from server: ${responseText}`,
        xhrConnectionError
      );
    }
  }

  xhrInvalidStatusCode(xhrConnectionError: XhrConnectionError): void {
    const statusCode = xhrConnectionError.getXhr().status;
    console.warn(`Server returned ${statusCode} for xhr`);
    if (statusCode === SC_UNAUTHORIZED) {
      this.registry.getRequestResponseTracker().endRequest();
      this.handleUnauthorized();
    } else {
      this.machine.handleRecoverableError(ConnectionMessageType.XHR, xhrConnectionError.getPayload());
    }
  }

  xhrOk(): void {
    if (this.machine.isReconnecting()) {
      this.machine.resolveTemporaryError(ConnectionMessageType.XHR);
    }
  }

  // --- ConnectionStateHandler: push ---

  pushOk(pushConnection: PushConnection): void {
    if (this.machine.isReconnecting()) {
      this.machine.resolveTemporaryError(ConnectionMessageType.PUSH);
      if (this.registry.getRequestResponseTracker().hasActiveRequest()) {
        this.registry.getRequestResponseTracker().endRequest();
        if (pushConnection.isBidirectional()) {
          this.registry.getMessageSender().sendInvocationsToServer();
        }
      }
    }
  }

  pushScriptLoadError(resourceUrl: string): void {
    this.handleCommunicationError(`${resourceUrl} could not be loaded. Push will not work.`);
  }

  pushNotConnected(payload: Record<string, unknown>): void {
    this.machine.handleRecoverableError(ConnectionMessageType.PUSH, payload);
  }

  pushReconnectPending(pushConnection: PushConnection): void {
    if (pushConnection.isBidirectional()) {
      this.machine.handleRecoverableError(ConnectionMessageType.PUSH, null);
    }
    // Otherwise wait; the reconnect dialog shows on the next failing XHR.
  }

  pushError(_pushConnection: PushConnection, response: unknown): void {
    const transport = (response as { transport?: string }).transport ?? 'unknown';
    this.handleCommunicationError(`Push connection using ${transport} failed!`);
  }

  pushClientTimeout(_pushConnection: PushConnection, _response: unknown): void {
    this.handleCommunicationError('Client unexpectedly disconnected. Ensure client timeout is disabled.');
  }

  pushClosed(_pushConnection: PushConnection, _response: unknown): void {
    console.debug('Push connection closed');
  }

  pushInvalidContent(pushConnection: PushConnection, message: string): void {
    if (pushConnection.isBidirectional()) {
      this.registry.getRequestResponseTracker().endRequest();
    }
    if (!this.redirectIfRefreshToken(message)) {
      this.handleUnrecoverableCommunicationError(`Invalid JSON from server: ${message}`, null);
    }
  }

  // --- ConnectionStateHandler: config ---

  configurationUpdated(): void {
    const dialogText = this.registry.getReconnectConfiguration().getDialogText();
    if (dialogText !== null) {
      setProperty('reconnectingText', dialogText);
    }
    const dialogTextGaveUp = this.registry.getReconnectConfiguration().getDialogTextGaveUp();
    if (dialogTextGaveUp !== null) {
      setProperty('offlineText', dialogTextGaveUp);
    }
  }

  // --- internal ---

  private handleUnauthorized(): void {
    // 401: assume the session has timed out.
    this.registry.getSystemErrorHandler().handleSessionExpiredError('');
    this.stopApplication();
  }

  private stopApplication(): void {
    const uiLifecycle = this.registry.getUILifecycle();
    if (uiLifecycle.getState() !== TERMINATED) {
      uiLifecycle.setState(TERMINATED);
    }
  }

  private handleUnrecoverableCommunicationError(details: string, _xhrConnectionError: XhrConnectionError | null): void {
    this.handleCommunicationError(details);
    this.stopApplication();
  }

  private handleCommunicationError(details: string): void {
    this.registry.getSystemErrorHandler().handleUnrecoverableError('', details, '', '', null);
  }

  private redirectIfRefreshToken(message: string): boolean {
    // A filter may have served non-UIDL content (e.g. a login page). If the
    // response carries the magic token, redirect.
    const match = new RegExp(`${UIDL_REFRESH_TOKEN}(:\\s*(.*?))?(\\s|$)`).exec(message);
    if (match !== null) {
      const url = match[2];
      if (url) {
        window.location.href = url;
      }
      return true;
    }
    return false;
  }

  private resumeHeartbeats(): void {
    // Resume only if not terminated (interval == -1).
    if (this.registry.getHeartbeat().getInterval() >= 0) {
      this.registry.getHeartbeat().setInterval(this.registry.getApplicationConfiguration().getHeartbeatInterval());
    }
  }

  private registerConnectionStateEventHandlers(): void {
    window.addEventListener('offline', () => {
      // Offline: CONNECTION_LOST and stop heartbeats.
      this.machine.giveUp();
      setState(CONNECTION_LOST);
    });
    window.addEventListener('online', () => {
      // Back online: verify the server connection via a heartbeat.
      this.resumeHeartbeats();
      this.machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
    });
  }
}
