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

import { Console } from '../Console';

// Mirrors ApplicationConstants. Strings are inlined so the TS module doesn't
// take a flow-shared bridge dependency.
const CSRF_TOKEN_DEFAULT_VALUE = 'init';
const CSRF_TOKEN_KEY = 'csrfToken';
const SERVER_SYNC_ID = 'syncId';
const CLIENT_TO_SERVER_ID = 'clientId';
const RESYNCHRONIZE_ID = 'resynchronize';
const RPC_INVOCATIONS = 'rpc';
const UNLOAD_BEACON = 'UNLOAD';

/** Resynchronization state. Matches the values of the Java enum. */
export type ResynchronizationState = 'NOT_ACTIVE' | 'SEND_TO_SERVER' | 'WAITING_FOR_RESPONSE';

type JsonObject = Record<string, unknown>;

/** Minimal MessageHandler shape used by this module (the sibling TS class). */
interface MessageHandlerLike {
  getCsrfToken(): string;
  getLastSeenServerSyncId(): number;
}

/** Minimal RequestResponseTracker shape used by this module. */
interface RequestResponseTrackerLike {
  hasActiveRequest(): boolean;
  startRequest(): void;
  addReconnectionAttemptHandler(handler: (attempt: number) => void): unknown;
}

/** Minimal UILifecycle shape used by this module. */
interface UILifecycleLike {
  isRunning(): boolean;
}

/** Minimal LoadingIndicatorStateHandler shape. */
interface LoadingIndicatorStateHandlerLike {
  startLoading(): void;
}

/** Minimal PushConfiguration shape used by this module. */
interface PushConfigurationLike {
  isPushEnabled(): boolean;
}

/** Minimal ServerRpcQueue shape used by this module. */
interface ServerRpcQueueLike {
  isEmpty(): boolean;
  toJson(): unknown[];
  clear(): void;
  isFlushPending(): boolean;
  flush(): void;
}

/** Minimal ApplicationConfiguration shape used by this module. */
interface ApplicationConfigurationLike {
  maxMessageSuspendTimeout: number;
}

/** Push connection abstraction; matches the Java {@code PushConnection} API. */
export interface PushConnectionLike {
  isActive(): boolean;
  isBidirectional(): boolean;
  push(payload: JsonObject): void;
  disconnect(command: () => void): void;
  getTransportType(): string | null;
}

/**
 * Wiring required by {@link MessageSender}: dependencies that need lazy
 * resolution (sibling MessageHandler), still-Java services that have to be
 * reached through @JsFunction callbacks (XhrConnection), and direct TS
 * service references for everything that is already constructed by the time
 * MessageSender is created.
 */
export interface MessageSenderCallbacks {
  getMessageHandler(): MessageHandlerLike;
  getUiLifecycle(): UILifecycleLike;
  getRequestResponseTracker(): RequestResponseTrackerLike;
  getLoadingIndicatorStateHandler(): LoadingIndicatorStateHandlerLike;
  getPushConfiguration(): PushConfigurationLike;
  getServerRpcQueue(): ServerRpcQueueLike;
  getApplicationConfiguration(): ApplicationConfigurationLike;
  /** Sends an XHR with the given payload. */
  sendXhr(payload: JsonObject): void;
  /** Returns the URI used for unload beacons. */
  getXhrUri(): string;
  /** Creates a new push connection. */
  createPushConnection(): PushConnectionLike;
}

function hasKey(obj: JsonObject, key: string): boolean {
  return Object.prototype.hasOwnProperty.call(obj, key);
}

/**
 * Sends messages to the server. Internally uses XHR and/or push, depending on
 * the application configuration. Migrated from
 * `com.vaadin.client.communication.MessageSender`.
 *
 * Wiring (sibling MessageHandler, the XHR send entry point, the push
 * connection factory, the various other services) is delivered through
 * {@link MessageSenderCallbacks} so the TS class does not reach back through
 * the Java {@code Registry} facade.
 */
export class MessageSender {
  /** Sends a beacon request (typically on page unload). Kept as static so the
   * `MessageSender.sendBeacon(...)` call site in the Java facade keeps
   * working without an instance reference.
   */
  static sendBeacon(url: string, payload: string): void {
    navigator.sendBeacon(url, payload);
  }

  private readonly callbacks: MessageSenderCallbacks;
  // Counter for messages sent to the server. The first sent message has id 0.
  private clientToServerMessageId = 0;
  private push: PushConnectionLike | null = null;
  private resynchronizationState: ResynchronizationState = 'NOT_ACTIVE';
  private pushPendingMessage: JsonObject | null = null;
  private messageQueue: JsonObject[] = [];
  private resendMessageTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(callbacks: MessageSenderCallbacks) {
    this.callbacks = callbacks;
    this.callbacks.getRequestResponseTracker().addReconnectionAttemptHandler((attempt) => {
      Console.debug(`Re-sending queued messages to the server (attempt ${attempt}) ...`);
      // Try to reconnect by sending queued messages. Stops the resend timer
      // since it won't make any request during reconnection anyway.
      this.resetTimer();
      this.doSendInvocationsToServer();
    });
  }

  /** Sends a beacon request indicating the page is unloading. */
  sendUnloadBeacon(): void {
    const dummyEmptyJson: unknown[] = [];
    const extraJson: JsonObject = {};
    extraJson[UNLOAD_BEACON] = true;
    const payload = this.preparePayload(dummyEmptyJson, extraJson);
    MessageSender.sendBeacon(this.callbacks.getXhrUri(), JSON.stringify(payload));
  }

  /**
   * Sends any pending invocations to the server if there is no request in
   * progress and the application is running. If a request is in progress
   * this method does nothing and assumes that it is called again when the
   * request completes.
   */
  sendInvocationsToServer(): void {
    if (!this.callbacks.getUiLifecycle().isRunning()) {
      Console.warn('Trying to send RPC from not yet started or stopped application');
      return;
    }

    const hasActiveRequest = this.callbacks.getRequestResponseTracker().hasActiveRequest();
    if (hasActiveRequest || (this.push !== null && !this.push.isActive())) {
      // Active request, or push enabled but not active -> defer.
      Console.debug(
        `Postpone sending invocations to server because of ${hasActiveRequest ? 'active request' : 'PUSH not active'}`
      );
    } else {
      this.doSendInvocationsToServer();
    }
  }

  /**
   * Sends all pending method invocations (server RPC and legacy variable
   * changes) to the server.
   */
  private doSendInvocationsToServer(): void {
    // If there's a stored message, resend it and postpone processing the
    // rest of the queued messages to prevent resynchronization issues.
    if (this.pushPendingMessage !== null) {
      Console.log(`Sending pending push message ${JSON.stringify(this.pushPendingMessage)}`);
      const payload = this.pushPendingMessage;
      this.pushPendingMessage = null;
      this.sendPayload(payload);
      return;
    } else if (this.hasQueuedMessages()) {
      Console.debug('Sending queued messages to server');
      if (this.resendMessageTimer !== null) {
        // Stop the resend timer and resend immediately.
        this.resetTimer();
      }
      this.sendPayload(this.messageQueue[0]);
      return;
    }

    const serverRpcQueue = this.callbacks.getServerRpcQueue();
    if (serverRpcQueue.isEmpty() && this.resynchronizationState !== 'SEND_TO_SERVER') {
      return;
    }

    const reqJson = serverRpcQueue.toJson();
    serverRpcQueue.clear();

    if (reqJson.length === 0 && this.resynchronizationState !== 'SEND_TO_SERVER') {
      // Nothing to send, all invocations were filtered out (for non-existing
      // connectors).
      Console.warn('All RPCs filtered out, not sending anything to the server');
      return;
    }

    const extraJson: JsonObject = {};
    if (this.resynchronizationState === 'SEND_TO_SERVER') {
      this.resynchronizationState = 'WAITING_FOR_RESPONSE';
      Console.warn('Resynchronizing from server');
      this.messageQueue = [];
      this.resetTimer();
      extraJson[RESYNCHRONIZE_ID] = true;
    }
    this.callbacks.getLoadingIndicatorStateHandler().startLoading();
    this.sendWithPayload(reqJson, extraJson);
  }

  /** Makes a UIDL request to the server with the given invocations and extras. */
  private sendWithPayload(reqInvocations: unknown[], extraJson: JsonObject): void {
    this.send(this.preparePayload(reqInvocations, extraJson));
  }

  private preparePayload(reqInvocations: unknown[], extraJson: JsonObject | null): JsonObject {
    const payload: JsonObject = {};
    const csrfToken = this.callbacks.getMessageHandler().getCsrfToken();
    if (csrfToken !== CSRF_TOKEN_DEFAULT_VALUE) {
      payload[CSRF_TOKEN_KEY] = csrfToken;
    }
    payload[RPC_INVOCATIONS] = reqInvocations;
    if (extraJson !== null) {
      for (const key of Object.keys(extraJson)) {
        payload[key] = extraJson[key];
      }
    }
    return payload;
  }

  /**
   * Sends an asynchronous or synchronous UIDL request to the server. Adds the
   * message to the queue and postpones sending if the queue is not empty.
   */
  send(payload: JsonObject): void {
    if (this.hasQueuedMessages()) {
      // The server sync id is set in the private sendPayload method. If it
      // is already on the payload, the message has been sent and enqueued.
      if (!hasKey(payload, SERVER_SYNC_ID)) {
        this.messageQueue.push(payload);
        Console.debug(
          `Message not sent because other messages are pending. Added to the queue: ${JSON.stringify(payload)}`
        );
      } else {
        Console.debug(`Message not sent because already queued: ${JSON.stringify(payload)}`);
      }
      return;
    }
    this.messageQueue.push(payload);
    this.sendPayload(payload);
  }

  /** Sends an asynchronous or synchronous UIDL request to the server. */
  private sendPayload(payload: JsonObject): void {
    const messageHandler = this.callbacks.getMessageHandler();
    // Do not update server sync id for enqueued messages.
    if (!hasKey(payload, SERVER_SYNC_ID)) {
      payload[SERVER_SYNC_ID] = messageHandler.getLastSeenServerSyncId();
    }
    // clientID should only be set and updated if payload doesn't contain
    // clientID. If one exists we are probably trying to resend.
    if (!hasKey(payload, CLIENT_TO_SERVER_ID)) {
      payload[CLIENT_TO_SERVER_ID] = this.clientToServerMessageId++;
    }

    if (!this.callbacks.getRequestResponseTracker().hasActiveRequest()) {
      // Direct calls to send from outside probably have not started the request.
      this.callbacks.getRequestResponseTracker().startRequest();
    }

    if (this.push !== null && this.push.isBidirectional()) {
      // When using bidirectional transport, the payload is not resent to
      // the server during reconnection attempts. Keep a copy so it can be
      // resent after reconnection; the reference is cleaned up once the
      // server confirms it has seen this message.
      Console.debug('send PUSH');
      this.pushPendingMessage = payload;
      this.push.push(payload);
    } else {
      Console.debug('send XHR');
      this.resetTimer();
      this.callbacks.sendXhr(payload);
      // Resend the last payload if no response comes in.
      const interval = this.callbacks.getApplicationConfiguration().maxMessageSuspendTimeout + 500;
      const tick = (): void => {
        this.resendMessageTimer = setTimeout(tick, interval);
        // Avoid resending if a request is still in progress. If the
        // response hasn't been processed yet, the reconnection-attempt
        // listener will take care of resending the queued message.
        if (!this.callbacks.getRequestResponseTracker().hasActiveRequest()) {
          this.callbacks.getRequestResponseTracker().startRequest();
          this.callbacks.sendXhr(payload);
        }
      };
      this.resendMessageTimer = setTimeout(tick, interval);
    }
  }

  private resetTimer(): void {
    if (this.resendMessageTimer !== null) {
      clearTimeout(this.resendMessageTimer);
      this.resendMessageTimer = null;
    }
  }

  /**
   * Sets the status for the push connection.
   * @param enabled true to enable, false to disable
   * @param reEnableIfNeeded true if push should be re-enabled after
   *        disconnection if the configuration changed; false to prevent
   *        reconnection. Defaults to true.
   */
  setPushEnabled(enabled: boolean, reEnableIfNeeded = true): void {
    if (enabled && (this.push === null || !this.push.isActive())) {
      this.push = this.callbacks.createPushConnection();
    } else if (!enabled && this.push !== null && this.push.isActive()) {
      this.push.disconnect(() => {
        this.push = null;
        // If push has been enabled again while we were waiting for the old
        // connection to disconnect, now is the right time to open a new
        // connection.
        if (reEnableIfNeeded && this.callbacks.getPushConfiguration().isPushEnabled()) {
          this.setPushEnabled(true);
        }

        // Send anything that was enqueued while we waited for the
        // connection to close.
        if (this.callbacks.getServerRpcQueue().isFlushPending()) {
          this.callbacks.getServerRpcQueue().flush();
        }
      });
    }
  }

  /** Returns a human readable description of the transport(s) being used. */
  getCommunicationMethodName(): string {
    let clientToServer = 'XHR';
    let serverToClient = '-';
    if (this.push !== null) {
      serverToClient = this.push.getTransportType() ?? '';
      if (this.push.isBidirectional()) {
        clientToServer = serverToClient;
      }
    }

    return `Client to server: ${clientToServer}, server to client: ${serverToClient}`;
  }

  /** Resynchronize the client side. */
  resynchronize(): void {
    if (this.requestResynchronize()) {
      this.messageQueue = [];
      this.resetTimer();
      this.sendInvocationsToServer();
    }
  }

  /** Updates what id the server expects. */
  setClientToServerMessageId(nextExpectedId: number, force: boolean): void {
    if (nextExpectedId === this.clientToServerMessageId) {
      // Everything matches. Remove a potential pending PUSH message if it
      // has already been seen by the server.
      if (
        this.pushPendingMessage !== null &&
        (this.pushPendingMessage[CLIENT_TO_SERVER_ID] as number) < nextExpectedId
      ) {
        this.pushPendingMessage = null;
      }
      if (this.hasQueuedMessages()) {
        // If the queued message is the expected one, remove from the queue
        // and send the next one if any.
        if ((this.messageQueue[0][CLIENT_TO_SERVER_ID] as number) + 1 === nextExpectedId) {
          this.messageQueue.shift();
          this.resetTimer();
        }
      }
      return;
    }
    if (force) {
      Console.debug(`Forced update of clientId to ${this.clientToServerMessageId}`);
      this.clientToServerMessageId = nextExpectedId;
      this.messageQueue = [];
      this.resetTimer();
      return;
    }

    if (nextExpectedId > this.clientToServerMessageId) {
      if (this.clientToServerMessageId === 0) {
        // We have never sent a message to the server, so likely the server
        // knows better (typical case is refreshing a @PreserveOnRefresh UI).
        Console.debug(`Updating client-to-server id to ${nextExpectedId} based on server`);
      } else {
        Console.warn(
          `Server expects next client-to-server id to be ${nextExpectedId} but we were going to use ${this.clientToServerMessageId}. Will use ${nextExpectedId}.`
        );
      }
      this.clientToServerMessageId = nextExpectedId;
    } else {
      // Server has not yet seen all our messages. Do nothing as they will
      // arrive eventually.
    }
  }

  /** Modifies the resynchronize state to indicate that resync is desired.
   * @returns true if the resynchronize request still needs to be sent
   */
  requestResynchronize(): boolean {
    switch (this.resynchronizationState) {
      case 'NOT_ACTIVE':
        Console.debug('Resynchronize from server requested');
        this.resynchronizationState = 'SEND_TO_SERVER';
        return true;
      case 'SEND_TO_SERVER':
        // Resynchronize has already been requested, but hasn't been sent yet.
        return true;
      case 'WAITING_FOR_RESPONSE':
      default:
        // Resynchronize has already been requested, but response hasn't
        // been received yet.
        return false;
    }
  }

  clearResynchronizationState(): void {
    this.resynchronizationState = 'NOT_ACTIVE';
  }

  getResynchronizationState(): ResynchronizationState {
    return this.resynchronizationState;
  }

  /**
   * Alias used by the Java {@code @JsOverlay} wrapper that adapts the TS
   * string return value into the Java {@code ResynchronizationState} enum.
   * Kept as a separate method so the Java enum-typed
   * {@code getResynchronizationState()} can keep its name.
   */
  getResynchronizationStateName(): string {
    return this.resynchronizationState;
  }

  hasQueuedMessages(): boolean {
    return this.messageQueue.length > 0;
  }
}
