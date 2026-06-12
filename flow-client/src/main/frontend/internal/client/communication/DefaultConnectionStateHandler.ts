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

import { ConnectionIndicator } from '../ConnectionIndicator';
import { Console } from '../Console';
import { WidgetUtil } from '../WidgetUtil';
import type { ConnectionStateHandler, PushConnectionLike } from './ConnectionStateHandler';
import { UIDL_REFRESH_TOKEN } from './ConnectionStateHandler';
import type { XhrConnectionError } from './XhrConnectionError';

const DEBUG = false;

/** Recoverable-error type; matches the inner Java enum. */
const enum Type {
  HEARTBEAT = 0,
  PUSH = 1,
  XHR = 2
}

function typeName(t: Type): string {
  switch (t) {
    case Type.HEARTBEAT:
      return 'HEARTBEAT';
    case Type.PUSH:
      return 'PUSH';
    case Type.XHR:
      return 'XHR';
  }
}

/** Minimal UILifecycle shape used by this module. */
interface UILifecycleLike {
  isRunning(): boolean;
  getStateName(): string;
  setStateName(state: string): void;
  addHandler(handler: (event: { getUiLifecycle(): UILifecycleLike }) => void): unknown;
}

/** Minimal SystemErrorHandler shape used by this module. */
interface SystemErrorHandlerLike {
  handleSessionExpiredError(details: string | null): void;
  handleUnrecoverableError(
    caption: string | null,
    message: string | null,
    details: string | null,
    url: string | null,
    querySelector: string | null
  ): void;
}

/** Minimal Heartbeat shape used by this module. */
interface HeartbeatLike {
  send(): void;
  getInterval(): number;
  setInterval(seconds: number): void;
}

/** Minimal ReconnectConfiguration shape used by this module. */
interface ReconnectConfigurationLike {
  getDialogText(): string | null;
  getDialogTextGaveUp(): string | null;
  getReconnectAttempts(): number;
  getReconnectInterval(): number;
}

/** Minimal RequestResponseTracker shape used by this module. */
interface RequestResponseTrackerLike {
  hasActiveRequest(): boolean;
  endRequest(): void;
  fireReconnectionAttempt(attempt: number): void;
}

/** Minimal LoadingIndicatorStateHandler shape used by this module. */
interface LoadingIndicatorStateHandlerLike {
  stopLoading(): void;
}

/** Minimal MessageSender shape used by this module. */
interface MessageSenderLike {
  sendInvocationsToServer(): void;
}

/** Minimal ApplicationConfiguration shape used by this module. */
interface ApplicationConfigurationLike {
  heartbeatInterval: number;
}

/**
 * Wiring required by {@link DefaultConnectionStateHandler}: all of the
 * connection-state plumbing it needs to reach. The Java {@code DefaultRegistry}
 * supplies this object at construction so the TS class does not need to
 * dispatch back through the Java {@code Registry} facade.
 */
export interface DefaultConnectionStateHandlerCallbacks {
  getUiLifecycle(): UILifecycleLike;
  getSystemErrorHandler(): SystemErrorHandlerLike;
  getHeartbeat(): HeartbeatLike;
  getReconnectConfiguration(): ReconnectConfigurationLike;
  getRequestResponseTracker(): RequestResponseTrackerLike;
  getLoadingIndicatorStateHandler(): LoadingIndicatorStateHandlerLike;
  getApplicationConfiguration(): ApplicationConfigurationLike;
  getMessageSender(): MessageSenderLike;
}

/**
 * Default implementation of {@link ConnectionStateHandler}. Migrated from
 * `com.vaadin.client.communication.DefaultConnectionStateHandler`.
 *
 * Handles temporary errors by showing a reconnect dialog while trying to
 * re-establish the connection and resend the pending message; handles
 * permanent errors by showing a critical system notification.
 */
export class DefaultConnectionStateHandler implements ConnectionStateHandler {
  private readonly callbacks: DefaultConnectionStateHandlerCallbacks;
  private reconnectAttempt = 0;
  private reconnectionCause: Type | null = null;
  private scheduledReconnect: ReturnType<typeof setTimeout> | null = null;

  constructor(callbacks: DefaultConnectionStateHandlerCallbacks) {
    this.callbacks = callbacks;
    callbacks.getUiLifecycle().addHandler((event) => {
      if (event.getUiLifecycle().getStateName() === 'TERMINATED') {
        if (this.isReconnecting()) {
          this.giveUp();
          this.stopApplication();
        }
        if (this.scheduledReconnect !== null) {
          clearTimeout(this.scheduledReconnect);
          this.scheduledReconnect = null;
        }
      }
    });

    // Register online / offline handlers.
    this.registerConnectionStateEventHandlers();
  }

  /** Checks if we are currently trying to reconnect. */
  private isReconnecting(): boolean {
    return this.reconnectionCause !== null;
  }

  xhrException(xhrConnectionError: XhrConnectionError): void {
    this.debug('xhrException');
    this.handleRecoverableError(Type.XHR, xhrConnectionError.getPayload() as Record<string, unknown> | null);
  }

  heartbeatException(_request: XMLHttpRequest, message: string): void {
    Console.error(`Heartbeat exception: ${message}`);
    this.handleRecoverableError(Type.HEARTBEAT, null);
  }

  heartbeatInvalidStatusCode(xhr: XMLHttpRequest): void {
    const statusCode = xhr.status;
    Console.warn(`Heartbeat request returned ${statusCode}`);

    if (statusCode === 403) {
      // Session expired.
      this.callbacks.getSystemErrorHandler().handleSessionExpiredError(null);
      this.stopApplication();
    } else if (statusCode === 404) {
      // UI closed; do nothing as the UI will react. Should not trigger the
      // reconnect dialog as that would prevent user input.
    } else {
      this.handleRecoverableError(Type.HEARTBEAT, null);
    }
  }

  heartbeatOk(): void {
    this.debug('heartbeatOk');
    if (this.isReconnecting()) {
      this.resolveTemporaryError(Type.HEARTBEAT);
    }
  }

  private debug(msg: string): void {
    if (DEBUG) {
      Console.debug(msg);
    }
  }

  /**
   * Called whenever an error occurs in communication that should be handled
   * by showing the reconnect dialog and retrying communication.
   *
   * @param type the type of failure detected
   * @param payload the message that did not reach the server, or null if no
   *        message was involved (heartbeat or push connection failed)
   */
  protected handleRecoverableError(type: Type, payload: Record<string, unknown> | null): void {
    this.debug(`handleTemporaryError(${typeName(type)})`);
    if (!this.callbacks.getUiLifecycle().isRunning()) {
      return;
    }

    ConnectionIndicator.setState('reconnecting');

    if (!this.isReconnecting()) {
      // First problem encountered.
      this.reconnectionCause = type;
      Console.warn(`Reconnecting because of ${typeName(type)} failure`);
    } else {
      // We are currently trying to reconnect.
      // Priority: HEARTBEAT > PUSH > XHR. If a higher-priority issue
      // resolves we assume the lower one will resolve too.
      if (this.reconnectionCause !== null && type > this.reconnectionCause) {
        Console.warn(`Now reconnecting because of ${typeName(type)} failure`);
        this.reconnectionCause = type;
      }
    }

    if (this.reconnectionCause !== type) {
      return;
    }

    this.reconnectAttempt++;
    Console.debug(`Reconnect attempt ${this.reconnectAttempt} for ${typeName(type)}`);

    if (this.reconnectAttempt >= this.getConfiguration().getReconnectAttempts()) {
      // Max attempts reached; stop trying and go back to CONNECTION_LOST.
      this.giveUp();
    } else {
      this.scheduleReconnect(payload);
    }
  }

  /**
   * Called after a problem occurred. Re-sends the payload to the server (if
   * not null) or schedules a heartbeat request.
   */
  protected scheduleReconnect(payload: Record<string, unknown> | null): void {
    // Here, not in the timer, to avoid TB for getting in between. The
    // request is still open at this point to avoid interference, so we do
    // not need to start a new one.
    if (this.reconnectAttempt === 1) {
      // Try once immediately.
      Console.debug(`Immediate reconnect attempt for ${JSON.stringify(payload)}`);
      this.doReconnect(payload);
    } else {
      this.scheduledReconnect = setTimeout(() => {
        this.scheduledReconnect = null;
        Console.debug(`Scheduled reconnect attempt ${this.reconnectAttempt} for ${JSON.stringify(payload)}`);
        this.doReconnect(payload);
      }, this.getConfiguration().getReconnectInterval());
    }
  }

  /** Re-sends the payload or re-sends a heartbeat immediately. */
  protected doReconnect(payload: Record<string, unknown> | null): void {
    if (!this.callbacks.getUiLifecycle().isRunning()) {
      Console.warn('Trying to reconnect after application has been stopped. Giving up');
      return;
    }
    if (payload !== null) {
      Console.debug('Trying to re-establish server connection (UIDL)...');
      this.callbacks.getRequestResponseTracker().fireReconnectionAttempt(this.reconnectAttempt);
    } else {
      // Use heartbeat.
      Console.debug('Trying to re-establish server connection (heartbeat)...');
      this.callbacks.getHeartbeat().send();
    }
  }

  /** Gives up trying to reconnect and shows CONNECTION_LOST state. */
  protected giveUp(): void {
    this.reconnectionCause = null;

    if (this.callbacks.getRequestResponseTracker().hasActiveRequest()) {
      this.endRequest();
    }

    ConnectionIndicator.setState('connection-lost');
    this.pauseHeartbeats();
  }

  /** Gets the text to show in the reconnect dialog after giving up. */
  protected getDialogTextGaveUp(reconnectAttempt: number): string {
    return (this.getConfiguration().getDialogTextGaveUp() ?? '').replace('{0}', String(reconnectAttempt));
  }

  /** Gets the text to show in the reconnect dialog. */
  protected getDialogText(reconnectAttempt: number): string {
    return (this.getConfiguration().getDialogText() ?? '').replace('{0}', String(reconnectAttempt));
  }

  configurationUpdated(): void {
    // All other properties are fetched directly from the state when needed.
    const dialogText = this.getConfiguration().getDialogText();
    if (dialogText !== null) {
      ConnectionIndicator.setProperty('reconnectingText', dialogText);
    }
    const dialogTextGaveUp = this.getConfiguration().getDialogTextGaveUp();
    if (dialogTextGaveUp !== null) {
      ConnectionIndicator.setProperty('offlineText', dialogTextGaveUp);
    }
  }

  private getConfiguration(): ReconnectConfigurationLike {
    return this.callbacks.getReconnectConfiguration();
  }

  xhrInvalidContent(xhrConnectionError: XhrConnectionError): void {
    this.debug('xhrInvalidContent');
    this.endRequest();

    const xhr = xhrConnectionError.getXhr() as XMLHttpRequest;
    const responseText = xhr.responseText;
    if (!this.redirectIfRefreshToken(responseText)) {
      this.handleUnrecoverableCommunicationError(
        `Invalid JSON response from server: ${responseText}`,
        xhrConnectionError
      );
    }
  }

  pushInvalidContent(pushConnection: PushConnectionLike, message: string): void {
    this.debug('pushInvalidContent');
    if (pushConnection.isBidirectional()) {
      // Can't be sure that what was pushed was actually a response, but at
      // this point it should not really matter, as something is seriously
      // broken.
      this.endRequest();
    }

    if (!this.redirectIfRefreshToken(message)) {
      this.handleUnrecoverableCommunicationError(`Invalid JSON from server: ${message}`, null);
    }
  }

  xhrInvalidStatusCode(xhrConnectionError: XhrConnectionError): void {
    this.debug('xhrInvalidStatusCode');

    const xhr = xhrConnectionError.getXhr() as XMLHttpRequest;
    const statusCode = xhr.status;
    Console.warn(`Server returned ${statusCode} for xhr`);

    if (statusCode === 401) {
      // Authentication/authorization failed; no need to retry.
      this.endRequest();
      this.handleUnauthorized(xhrConnectionError);
      return;
    }
    // 404, 408 and other 4xx codes CAN be temporary when a proxy sits
    // between client and server and the server restarts. 5xx codes may or
    // may not be temporary.
    this.handleRecoverableError(Type.XHR, xhrConnectionError.getPayload() as Record<string, unknown> | null);
  }

  private endRequest(): void {
    this.callbacks.getRequestResponseTracker().endRequest();
  }

  protected handleUnauthorized(_xhrConnectionError: XhrConnectionError): void {
    // Authorization has failed (401). Assume that the session has timed out.
    this.callbacks.getSystemErrorHandler().handleSessionExpiredError('');
    this.stopApplication();
  }

  private stopApplication(): void {
    // Consider application not running any more and prevent all future
    // requests.
    const lifecycle = this.callbacks.getUiLifecycle();
    if (lifecycle.getStateName() !== 'TERMINATED') {
      lifecycle.setStateName('TERMINATED');
    }
  }

  private handleUnrecoverableCommunicationError(details: string, xhrConnectionError: XhrConnectionError | null): void {
    let statusCode = -1;
    if (xhrConnectionError !== null) {
      const xhr = xhrConnectionError.getXhr() as XMLHttpRequest | null;
      if (xhr !== null) {
        statusCode = xhr.status;
      }
    }
    this.handleCommunicationError(details, statusCode);

    this.stopApplication();
  }

  /** Called when a communication error occurs and we cannot recover. */
  protected handleCommunicationError(details: string, _statusCode: number): void {
    this.callbacks.getSystemErrorHandler().handleUnrecoverableError('', details, '', null, null);
  }

  xhrOk(): void {
    this.debug('xhrOk');
    if (this.isReconnecting()) {
      this.resolveTemporaryError(Type.XHR);
    }
  }

  private resolveTemporaryError(type: Type): void {
    this.debug(`resolveTemporaryError(${typeName(type)})`);

    if (this.reconnectionCause !== type) {
      // Waiting for some other problem to be resolved.
      return;
    }

    this.reconnectionCause = null;
    this.reconnectAttempt = 0;
    if (this.scheduledReconnect !== null) {
      clearTimeout(this.scheduledReconnect);
      this.scheduledReconnect = null;
    }
    if (type === Type.HEARTBEAT) {
      // Heartbeat never has loading indication; it is safe to assume that
      // no other requests are in progress and set the `CONNECTED` state
      // directly.
      ConnectionIndicator.setState('connected');
    } else {
      // Let the loading indicator state handler check and remove the
      // prior loading indication if necessary.
      this.callbacks.getLoadingIndicatorStateHandler().stopLoading();
    }

    Console.debug('Re-established connection to server');
  }

  pushOk(pushConnection: PushConnectionLike): void {
    this.debug('pushOk()');
    if (this.isReconnecting()) {
      this.resolveTemporaryError(Type.PUSH);
      if (this.callbacks.getRequestResponseTracker().hasActiveRequest()) {
        this.debug('pushOk() Reset active request state when reconnecting PUSH because of a network error.');
        this.endRequest();
        // For bidirectional transport the pending message is not sent as
        // reconnection payload; immediately push the pending changes on
        // reconnect.
        if (pushConnection.isBidirectional()) {
          Console.debug('Flush pending messages after PUSH reconnection.');
          this.callbacks.getMessageSender().sendInvocationsToServer();
        }
      }
    }
  }

  pushScriptLoadError(resourceUrl: string): void {
    this.handleCommunicationError(`${resourceUrl} could not be loaded. Push will not work.`, 0);
  }

  pushNotConnected(payload: unknown): void {
    this.debug('pushNotConnected()');
    this.handleRecoverableError(Type.PUSH, payload as Record<string, unknown>);
  }

  pushReconnectPending(pushConnection: PushConnectionLike): void {
    this.debug(`pushReconnectPending(${pushConnection.getTransportType()})`);
    Console.debug('Reopening push connection');
    if (pushConnection.isBidirectional()) {
      // Lost connection for a transport that will tell us when the
      // connection is available again.
      this.handleRecoverableError(Type.PUSH, null);
    } else {
      // Lost connection for a transport we do not necessarily know when
      // it is available again (long polling behind proxy). Do nothing and
      // show the reconnect dialog if the user does something and the XHR
      // fails.
    }
  }

  pushError(_pushConnection: PushConnectionLike, response: unknown): void {
    this.debug('pushError()');
    const transport = (response as { getTransport?(): string } | null)?.getTransport?.() ?? '';
    this.handleCommunicationError(`Push connection using ${transport} failed!`, -1);
  }

  pushClientTimeout(_pushConnection: PushConnectionLike, _response: unknown): void {
    this.debug('pushClientTimeout()');
    // TODO Reconnect, allowing client timeout to be set
    // https://dev.vaadin.com/ticket/18429
    this.handleCommunicationError('Client unexpectedly disconnected. Ensure client timeout is disabled.', -1);
  }

  pushClosed(_pushConnection: PushConnectionLike, _response: unknown): void {
    this.debug('pushClosed()');
    Console.debug('Push connection closed');
  }

  private pauseHeartbeats(): void {
    this.callbacks.getHeartbeat().setInterval(0);
  }

  private resumeHeartbeats(): void {
    // Resume heartbeat only if it was not terminated (interval == -1).
    if (this.callbacks.getHeartbeat().getInterval() >= 0) {
      this.callbacks.getHeartbeat().setInterval(this.callbacks.getApplicationConfiguration().heartbeatInterval);
    }
  }

  private redirectIfRefreshToken(message: string): boolean {
    // A servlet filter or equivalent may have intercepted the request and
    // served non-UIDL content (for instance, a login page if the session
    // has expired). If the response contains a magic substring, do a
    // synchronous refresh. See https://github.com/vaadin/framework/issues/2059.
    const regex = new RegExp(`${UIDL_REFRESH_TOKEN}(:\\s*(.*?))?(\\s|$)`);
    const match = regex.exec(message);
    if (match !== null) {
      WidgetUtil.redirect(match[2] ?? null);
      return true;
    }
    return false;
  }

  private registerConnectionStateEventHandlers(): void {
    window.addEventListener('offline', () => {
      // Browser goes offline: CONNECTION_LOST and stop heartbeats.
      this.giveUp();
    });
    window.addEventListener('online', () => {
      // Browser goes back online: RECONNECTING while verifying server
      // connection using heartbeat.
      this.resumeHeartbeats();
      this.handleRecoverableError(Type.HEARTBEAT, null);
    });
  }
}
