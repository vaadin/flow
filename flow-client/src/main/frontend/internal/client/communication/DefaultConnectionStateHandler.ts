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

import type { Registry } from '../Registry';
import { redirect } from '../WidgetUtil';
import { setProperty } from '../ConnectionIndicator';
import { ConnectionMessageType } from './ConnectionMessageType';
import type { ConnectionStateHandler } from './ConnectionStateHandler';
import type { PushConnection } from './PushConnection';
import { UIDL_REFRESH_TOKEN } from './ConnectionStateHandler';
import { ReconnectStateMachine } from './ReconnectStateMachine';
import type { XhrConnectionError } from './XhrConnectionError';
import { Console } from '../Console';

const SC_FORBIDDEN = 403;
const SC_NOT_FOUND = 404;
const SC_UNAUTHORIZED = 401;

// com.vaadin.client.UILifecycle.UIState
const TERMINATED = 'TERMINATED';

/**
 * Default implementation of the connection state handler. Handles temporary
 * errors by showing a reconnect dialog to the user while trying to re-establish
 * the connection to the server and re-send the pending message. Handles
 * permanent errors by showing a critical system notification to the user
 */
export class DefaultConnectionStateHandler implements ConnectionStateHandler {
  readonly #registry: Registry;

  readonly #machine: ReconnectStateMachine;

  #scheduledReconnect: ReturnType<typeof setTimeout> | null = null;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry) {
    this.#registry = registry;
    this.#machine = new ReconnectStateMachine(
      registry,
      (payload) => this.scheduleReconnect(payload),
      () => this.#cancelScheduledReconnect()
    );

    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        if (this.#machine.isReconnecting()) {
          this.#machine.giveUp();
          this.#stopApplication();
        }
        this.#cancelScheduledReconnect();
      }
    });

    // Register online / offline handlers
    this.#registerConnectionStateEventHandlers();
  }

  // --- ConnectionStateHandler: xhr ---

  xhrException(xhrConnectionError: XhrConnectionError): void {
    this.#machine.handleRecoverableError(ConnectionMessageType.XHR, xhrConnectionError.getPayload());
  }

  // --- ConnectionStateHandler: heartbeat ---

  heartbeatException(_request: XMLHttpRequest, exception: Error): void {
    Console.error(`Heartbeat exception: ${exception.message}`);
    this.#machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
  }

  heartbeatInvalidStatusCode(xhr: XMLHttpRequest): void {
    const statusCode = xhr.status;
    Console.warn(`Heartbeat request returned ${statusCode}`);
    if (statusCode === SC_FORBIDDEN) {
      // Session expired
      this.#registry.getSystemErrorHandler().handleSessionExpiredError(null);
      this.#stopApplication();
    } else if (statusCode === SC_NOT_FOUND) {
      // UI closed, do nothing as the UI will react to this
      // Should not trigger reconnect dialog as this will prevent user input
    } else {
      this.#machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
    }
  }

  heartbeatOk(): void {
    if (this.#machine.isReconnecting()) {
      this.#machine.resolveTemporaryError(ConnectionMessageType.HEARTBEAT);
    }
  }

  /**
   * Called after a problem occurred.
   *
   * This method is responsible for re-sending the payload to the server (if not
   * null) or re-send a heartbeat request at some point
   *
   * @param payload - the payload that did not reach the server, null if the
   *          problem was detected by a heartbeat
   */
  protected scheduleReconnect(payload: unknown): void {
    // Here and not in the timer to avoid TestBench getting in between.
    //
    // The request is still open at this point to avoid interference, so we do
    // not need to start a new one.
    if (this.#machine.getReconnectAttempt() === 1) {
      // Try once immediately
      Console.debug(`Immediate reconnect attempt for ${JSON.stringify(payload)}`);
      this.doReconnect(payload);
    } else {
      this.#scheduledReconnect = setTimeout(() => {
        // Overlapping failures each schedule a reconnect, but only one of them
        // should run: cancel whatever is scheduled now, as the Java
        // implementation did here.
        this.#cancelScheduledReconnect();
        Console.debug(
          `Scheduled reconnect attempt ${this.#machine.getReconnectAttempt()} for ${JSON.stringify(payload)}`
        );
        this.doReconnect(payload);
      }, this.#registry.getReconnectConfiguration().getReconnectInterval());
    }
  }

  #cancelScheduledReconnect(): void {
    if (this.#scheduledReconnect !== null) {
      clearTimeout(this.#scheduledReconnect);
      this.#scheduledReconnect = null;
    }
  }

  /**
   * Re-sends the payload to the server (if not null) or re-sends a heartbeat
   * request immediately.
   *
   * @param payload - the payload that did not reach the server, null if the
   *          problem was detected by a heartbeat
   */
  protected doReconnect(payload: unknown): void {
    if (!this.#registry.getUILifecycle().isRunning()) {
      // This should not happen as nobody should call this if the application has
      // been stopped
      Console.warn('Trying to reconnect after application has been stopped. Giving up');
      return;
    }
    if (payload !== null && payload !== undefined) {
      // Re-send the queued UIDL via the reconnection-attempt listener.
      Console.debug('Trying to re-establish server connection (UIDL)...');
      this.#registry.getRequestResponseTracker().fireReconnectionAttempt(this.#machine.getReconnectAttempt());
    } else {
      // Use heartbeat
      Console.debug('Trying to re-establish server connection (heartbeat)...');
      this.#registry.getHeartbeat().send();
    }
  }

  // --- internal ---

  /**
   * Gets the text to show in the reconnect dialog after giving up (reconnect
   * limit reached).
   *
   * @param reconnectAttempt - The number of the current reconnection attempt
   * @returns The text to show in the reconnect dialog after giving up
   */
  protected getDialogTextGaveUp(reconnectAttempt: number): string {
    return this.#registry.getReconnectConfiguration().getDialogTextGaveUp()!.replace('{0}', `${reconnectAttempt}`);
  }

  /**
   * Gets the text to show in the reconnect dialog.
   *
   * @param reconnectAttempt - The number of the current reconnection attempt
   * @returns The text to show in the reconnect dialog
   */
  protected getDialogText(reconnectAttempt: number): string {
    return this.#registry.getReconnectConfiguration().getDialogText()!.replace('{0}', `${reconnectAttempt}`);
  }

  // --- ConnectionStateHandler: config ---

  configurationUpdated(): void {
    // All other properties are fetched directly from the state when needed
    const dialogText = this.#registry.getReconnectConfiguration().getDialogText();
    if (dialogText !== null) {
      setProperty('reconnectingText', dialogText);
    }
    const dialogTextGaveUp = this.#registry.getReconnectConfiguration().getDialogTextGaveUp();
    if (dialogTextGaveUp !== null) {
      setProperty('offlineText', dialogTextGaveUp);
    }
  }

  xhrInvalidContent(xhrConnectionError: XhrConnectionError): void {
    this.#registry.getRequestResponseTracker().endRequest();
    const responseText = xhrConnectionError.getXhr().responseText;
    if (!this.#redirectIfRefreshToken(responseText)) {
      this.#handleUnrecoverableCommunicationError(
        `Invalid JSON response from server: ${responseText}`,
        xhrConnectionError
      );
    }
  }

  pushInvalidContent(pushConnection: PushConnection, message: string): void {
    if (pushConnection.isBidirectional()) {
      // We can't be sure that what was pushed was actually a response but at
      // this point it should not really matter, as something is seriously
      // broken.
      this.#registry.getRequestResponseTracker().endRequest();
    }
    if (!this.#redirectIfRefreshToken(message)) {
      this.#handleUnrecoverableCommunicationError(`Invalid JSON from server: ${message}`, null);
    }
  }

  xhrInvalidStatusCode(xhrConnectionError: XhrConnectionError): void {
    const statusCode = xhrConnectionError.getXhr().status;
    Console.warn(`Server returned ${statusCode} for xhr`);
    if (statusCode === SC_UNAUTHORIZED) {
      // Authentication/authorization failed, no need to re-try
      this.#registry.getRequestResponseTracker().endRequest();
      this.handleUnauthorized(xhrConnectionError);
    } else {
      // 404, 408 and other 4xx codes CAN be temporary when you have a proxy
      // between the client and the server and e.g. restart the server
      // 5xx codes may or may not be temporary
      this.#machine.handleRecoverableError(ConnectionMessageType.XHR, xhrConnectionError.getPayload());
    }
  }

  /**
   * Called when the server returns 401 Unauthorized.
   *
   * @param xhrConnectionError - the error that occurred
   */
  protected handleUnauthorized(_xhrConnectionError: XhrConnectionError): void {
    // Authorization has failed (401). Assume that the session has timed out.
    this.#registry.getSystemErrorHandler().handleSessionExpiredError('');
    this.#stopApplication();
  }

  #stopApplication(): void {
    // Consider application not running any more and prevent all future requests
    const uiLifecycle = this.#registry.getUILifecycle();
    if (uiLifecycle.getState() !== TERMINATED) {
      uiLifecycle.setState(TERMINATED);
    }
  }

  #handleUnrecoverableCommunicationError(details: string, xhrConnectionError: XhrConnectionError | null): void {
    let statusCode = -1;
    if (xhrConnectionError !== null) {
      const xhr = xhrConnectionError.getXhr();
      statusCode = xhr.status;
    }
    this.handleCommunicationError(details, statusCode);

    this.#stopApplication();
  }

  /**
   * Called when a communication error occurs and we cannot recover from it.
   *
   * @param details - message details or `null` if there are no details
   * @param statusCode - the status code
   */
  protected handleCommunicationError(details: string, _statusCode: number): void {
    this.#registry.getSystemErrorHandler().handleUnrecoverableError('', details, '', '', null);
  }

  xhrOk(): void {
    if (this.#machine.isReconnecting()) {
      this.#machine.resolveTemporaryError(ConnectionMessageType.XHR);
    }
  }

  // --- ConnectionStateHandler: push ---

  pushOk(pushConnection: PushConnection): void {
    if (this.#machine.isReconnecting()) {
      this.#machine.resolveTemporaryError(ConnectionMessageType.PUSH);
      if (this.#registry.getRequestResponseTracker().hasActiveRequest()) {
        this.#registry.getRequestResponseTracker().endRequest();
        // For a bidirectional transport the pending message is not sent as the
        // reconnection payload, so push the pending changes immediately on
        // reconnect.
        if (pushConnection.isBidirectional()) {
          Console.debug('Flush pending messages after PUSH reconnection.');
          this.#registry.getMessageSender().sendInvocationsToServer();
        }
      }
    }
  }

  pushScriptLoadError(resourceUrl: string): void {
    this.handleCommunicationError(`${resourceUrl} could not be loaded. Push will not work.`, 0);
  }

  pushNotConnected(payload: Record<string, unknown>): void {
    this.#machine.handleRecoverableError(ConnectionMessageType.PUSH, payload);
  }

  pushReconnectPending(pushConnection: PushConnection): void {
    Console.debug('Reopening push connection');
    if (pushConnection.isBidirectional()) {
      // Lost connection for a connection which will tell us when the connection
      // is available again
      this.#machine.handleRecoverableError(ConnectionMessageType.PUSH, null);
    } else {
      // Lost connection for a connection we do not necessarily know when it is
      // available again (long polling behind proxy). Do nothing and show the
      // reconnect dialog if the user does something and the XHR fails.
    }
  }

  pushError(_pushConnection: PushConnection, response: unknown): void {
    const transport = (response as { transport?: string }).transport ?? 'unknown';
    this.handleCommunicationError(`Push connection using ${transport} failed!`, -1);
  }

  pushClientTimeout(_pushConnection: PushConnection, _response: unknown): void {
    // TODO Reconnect, allowing client timeout to be set
    // https://dev.vaadin.com/ticket/18429
    this.handleCommunicationError('Client unexpectedly disconnected. Ensure client timeout is disabled.', -1);
  }

  pushClosed(_pushConnection: PushConnection, _response: unknown): void {
    Console.debug('Push connection closed');
  }

  #resumeHeartbeats(): void {
    // Resume only if not terminated (interval == -1).
    if (this.#registry.getHeartbeat().getInterval() >= 0) {
      this.#registry.getHeartbeat().setInterval(this.#registry.getApplicationConfiguration().getHeartbeatInterval());
    }
  }

  #redirectIfRefreshToken(message: string): boolean {
    // A filter may have served non-UIDL content (e.g. a login page). If the
    // response carries the magic token, redirect.
    const match = new RegExp(`${UIDL_REFRESH_TOKEN}(:\\s*(.*?))?(\\s|$)`).exec(message);
    if (match !== null) {
      // redirect() reloads the page when the token carries no url, which is the
      // session-expired case.
      redirect(match[2] ?? null);
      return true;
    }
    return false;
  }

  #registerConnectionStateEventHandlers(): void {
    window.addEventListener('offline', () => {
      // Offline: CONNECTION_LOST and stop heartbeats — giveUp does both.
      this.#machine.giveUp();
    });
    window.addEventListener('online', () => {
      // Back online: verify the server connection via a heartbeat.
      this.#resumeHeartbeats();
      this.#machine.handleRecoverableError(ConnectionMessageType.HEARTBEAT, null);
    });
  }
}
