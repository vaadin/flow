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

// TypeScript port of com.vaadin.client.communication.MessageSender, built
// alongside the Java version. It sends UIDL requests to the server over XHR
// and/or push, managing the client-to-server message id, the resynchronization
// state machine, an outgoing message queue, and a resend timer. The
// XhrConnection / PushConnection / MessageHandler and the rest of the Registry
// are contracts satisfied at cutover; push connections are created through an
// injected factory (GWT.create in the Java version).

import { sendBeacon } from '../MessageSender';
import { ResynchronizationState } from './ResynchronizationState';

// com.vaadin.flow.shared.ApplicationConstants
const RPC_INVOCATIONS = 'rpc';
const CSRF_TOKEN = 'csrfToken';
const CSRF_TOKEN_DEFAULT_VALUE = 'init';
const SERVER_SYNC_ID = 'syncId';
const CLIENT_TO_SERVER_ID = 'clientId';
const RESYNCHRONIZE_ID = 'resynchronize';
const UNLOAD_BEACON = 'UNLOAD';

type Payload = Record<string, unknown>;

/** A bidirectional/one-way push connection (Atmosphere); contract satisfied at cutover. */
export interface PushConnection {
  isActive(): boolean;
  isBidirectional(): boolean;
  push(payload: Payload): void;
  disconnect(callback: () => void): void;
  getTransportType(): string;
}

/** Creates a push connection for the registry; mirrors GWT-created PushConnectionFactory. */
export type PushConnectionFactory = (registry: MessageSenderRegistry) => PushConnection;

/** The slice of Registry that MessageSender uses. */
export interface MessageSenderRegistry {
  getUILifecycle(): { isRunning(): boolean };
  getRequestResponseTracker(): {
    hasActiveRequest(): boolean;
    startRequest(): void;
    addReconnectionAttemptHandler(handler: (attempt: number) => void): unknown;
  };
  getServerRpcQueue(): {
    isEmpty(): boolean;
    toJson(): unknown[];
    clear(): void;
    isFlushPending(): boolean;
    flush(): void;
  };
  getLoadingIndicatorStateHandler(): { startLoading(): void };
  getMessageHandler(): { getCsrfToken(): string; getLastSeenServerSyncId(): number };
  getXhrConnection(): { send(payload: Payload): void; getUri(): string };
  getApplicationConfiguration(): { getMaxMessageSuspendTimeout(): number };
  getPushConfiguration(): { isPushEnabled(): boolean };
}

/** Sends messages to the server over XHR and/or push; mirrors MessageSender.java. */
export class MessageSender {
  // Counter for the messages sent to the server. First sent message has id 0.
  private clientToServerMessageId = 0;

  private push: PushConnection | null = null;

  private readonly registry: MessageSenderRegistry;

  private readonly pushConnectionFactory: PushConnectionFactory | null;

  private resynchronizationState: ResynchronizationState = ResynchronizationState.NOT_ACTIVE;

  private pushPendingMessage: Payload | null = null;

  private messageQueue: Payload[] = [];

  private resendMessageTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(registry: MessageSenderRegistry, pushConnectionFactory: PushConnectionFactory | null = null) {
    this.registry = registry;
    this.pushConnectionFactory = pushConnectionFactory;
    this.registry.getRequestResponseTracker().addReconnectionAttemptHandler((attempt) => {
      console.debug(`Re-sending queued messages to the server (attempt ${attempt}) ...`);
      // Try to reconnect by sending queued messages; stop the resend timer since
      // it will not make any request during reconnection anyway.
      this.resetTimer();
      this.doSendInvocationsToServer();
    });
  }

  sendUnloadBeacon(): void {
    const payload = this.preparePayload([], { [UNLOAD_BEACON]: true });
    sendBeacon(this.registry.getXhrConnection().getUri(), JSON.stringify(payload));
  }

  /**
   * Sends any pending invocations to the server if there is no request in
   * progress and the application is running.
   */
  sendInvocationsToServer(): void {
    if (!this.registry.getUILifecycle().isRunning()) {
      console.warn('Trying to send RPC from not yet started or stopped application');
      return;
    }

    const hasActiveRequest = this.registry.getRequestResponseTracker().hasActiveRequest();
    if (hasActiveRequest || (this.push !== null && !this.push.isActive())) {
      // Active request, or push enabled but not active: send when the current
      // request completes or push becomes active.
      console.debug(
        `Postpone sending invocations to server because of ${hasActiveRequest ? 'active request' : 'PUSH not active'}`
      );
    } else {
      this.doSendInvocationsToServer();
    }
  }

  private doSendInvocationsToServer(): void {
    // If there's a stored message, resend it and postpone the rest of the queue
    // to prevent resynchronization issues.
    if (this.pushPendingMessage !== null) {
      const payload = this.pushPendingMessage;
      this.pushPendingMessage = null;
      this.sendPayload(payload);
      return;
    } else if (this.hasQueuedMessages()) {
      console.debug('Sending queued messages to server');
      if (this.resendMessageTimer !== null) {
        this.resetTimer();
      }
      this.sendPayload(this.messageQueue[0]);
      return;
    }

    const serverRpcQueue = this.registry.getServerRpcQueue();
    if (serverRpcQueue.isEmpty() && this.resynchronizationState !== ResynchronizationState.SEND_TO_SERVER) {
      return;
    }

    const reqJson = serverRpcQueue.toJson();
    serverRpcQueue.clear();

    if (reqJson.length === 0 && this.resynchronizationState !== ResynchronizationState.SEND_TO_SERVER) {
      // Nothing to send, all invocations were filtered out.
      console.warn('All RPCs filtered out, not sending anything to the server');
      return;
    }

    const extraJson: Payload = {};
    if (this.resynchronizationState === ResynchronizationState.SEND_TO_SERVER) {
      this.resynchronizationState = ResynchronizationState.WAITING_FOR_RESPONSE;
      console.warn('Resynchronizing from server');
      this.messageQueue = [];
      this.resetTimer();
      extraJson[RESYNCHRONIZE_ID] = true;
    }
    this.registry.getLoadingIndicatorStateHandler().startLoading();
    this.sendRequest(reqJson, extraJson);
  }

  private sendRequest(reqInvocations: unknown[], extraJson: Payload): void {
    this.send(this.preparePayload(reqInvocations, extraJson));
  }

  private preparePayload(reqInvocations: unknown[], extraJson: Payload | null): Payload {
    const payload: Payload = {};
    const csrfToken = this.registry.getMessageHandler().getCsrfToken();
    if (csrfToken !== CSRF_TOKEN_DEFAULT_VALUE) {
      payload[CSRF_TOKEN] = csrfToken;
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
   * Sends a UIDL request to the server. Adds the message to the queue and
   * postpones sending if the queue is not empty.
   */
  send(payload: Payload): void {
    if (this.hasQueuedMessages()) {
      // The server sync id is set in sendPayload. If it is already present, the
      // message has already been sent and enqueued.
      if (!(SERVER_SYNC_ID in payload)) {
        this.messageQueue.push(payload);
      }
      return;
    }
    this.messageQueue.push(payload);
    this.sendPayload(payload);
  }

  private sendPayload(payload: Payload): void {
    // Do not update server sync id for enqueued messages.
    if (!(SERVER_SYNC_ID in payload)) {
      payload[SERVER_SYNC_ID] = this.registry.getMessageHandler().getLastSeenServerSyncId();
    }
    // clientId should only be set if absent; if present we are resending.
    if (!(CLIENT_TO_SERVER_ID in payload)) {
      payload[CLIENT_TO_SERVER_ID] = this.clientToServerMessageId++;
    }

    if (!this.registry.getRequestResponseTracker().hasActiveRequest()) {
      // Direct calls from outside have probably not started a request.
      this.registry.getRequestResponseTracker().startRequest();
    }

    if (this.push !== null && this.push.isBidirectional()) {
      // With bidirectional transport the payload is not resent during
      // reconnection; keep a copy to resend after a reconnection until the
      // server confirms it.
      this.pushPendingMessage = payload;
      this.push.push(payload);
    } else {
      this.resetTimer();
      this.registry.getXhrConnection().send(payload);
      this.scheduleResend(payload);
    }
  }

  // Resends the last payload if a response hasn't come in; reschedules itself.
  private scheduleResend(payload: Payload): void {
    const timeout = this.registry.getApplicationConfiguration().getMaxMessageSuspendTimeout() + 500;
    this.resendMessageTimer = setTimeout(() => {
      this.scheduleResend(payload);
      // Avoid re-sending while a request is still in progress; if the response
      // has not been processed, the reconnection-attempt listener resends.
      if (!this.registry.getRequestResponseTracker().hasActiveRequest()) {
        this.registry.getRequestResponseTracker().startRequest();
        this.registry.getXhrConnection().send(payload);
      }
    }, timeout);
  }

  private resetTimer(): void {
    if (this.resendMessageTimer !== null) {
      clearTimeout(this.resendMessageTimer);
      this.resendMessageTimer = null;
    }
  }

  /** Enables or disables the push connection. */
  setPushEnabled(enabled: boolean, reEnableIfNeeded = true): void {
    if (enabled && (this.push === null || !this.push.isActive())) {
      this.push = this.pushConnectionFactory ? this.pushConnectionFactory(this.registry) : null;
    } else if (!enabled && this.push !== null && this.push.isActive()) {
      this.push.disconnect(() => {
        this.push = null;
        // If push was re-enabled while waiting to disconnect, reconnect now.
        if (reEnableIfNeeded && this.registry.getPushConfiguration().isPushEnabled()) {
          this.setPushEnabled(true);
        }
        // Send anything enqueued while waiting for the connection to close.
        if (this.registry.getServerRpcQueue().isFlushPending()) {
          this.registry.getServerRpcQueue().flush();
        }
      });
    }
  }

  /** A human-readable description of the current transport. */
  getCommunicationMethodName(): string {
    let clientToServer = 'XHR';
    let serverToClient = '-';
    if (this.push !== null) {
      serverToClient = this.push.getTransportType();
      if (this.push.isBidirectional()) {
        clientToServer = serverToClient;
      }
    }
    return `Client to server: ${clientToServer}, server to client: ${serverToClient}`;
  }

  /** Resynchronizes the client side from the server. */
  resynchronize(): void {
    if (this.requestResynchronize()) {
      this.messageQueue = [];
      this.resetTimer();
      this.sendInvocationsToServer();
    }
  }

  /** Updates the id the server expects, reconciling the queue. */
  setClientToServerMessageId(nextExpectedId: number, force: boolean): void {
    if (nextExpectedId === this.clientToServerMessageId) {
      // Remove a pending PUSH message already seen by the server.
      if (
        this.pushPendingMessage !== null &&
        (this.pushPendingMessage[CLIENT_TO_SERVER_ID] as number) < nextExpectedId
      ) {
        this.pushPendingMessage = null;
      }
      if (this.hasQueuedMessages()) {
        // If the queued message is the expected one, remove it and send next.
        if ((this.messageQueue[0][CLIENT_TO_SERVER_ID] as number) + 1 === nextExpectedId) {
          this.messageQueue.shift();
          this.resetTimer();
        }
      }
      return;
    }
    if (force) {
      console.debug(`Forced update of clientId to ${this.clientToServerMessageId}`);
      this.clientToServerMessageId = nextExpectedId;
      this.messageQueue = [];
      this.resetTimer();
      return;
    }

    if (nextExpectedId > this.clientToServerMessageId) {
      if (this.clientToServerMessageId === 0) {
        // Never sent a message, so the server knows better (e.g. a refreshed
        // @PreserveOnRefresh UI).
        console.debug(`Updating client-to-server id to ${nextExpectedId} based on server`);
      } else {
        console.warn(
          `Server expects next client-to-server id to be ${nextExpectedId} but we were going to use ${this.clientToServerMessageId}. Will use ${nextExpectedId}.`
        );
      }
      this.clientToServerMessageId = nextExpectedId;
    }
    // else the server has not yet seen all our messages; they will arrive.
  }

  /** Marks that a resynchronization is desired; returns whether it still needs sending. */
  requestResynchronize(): boolean {
    switch (this.resynchronizationState) {
      case ResynchronizationState.NOT_ACTIVE:
        console.debug('Resynchronize from server requested');
        this.resynchronizationState = ResynchronizationState.SEND_TO_SERVER;
        return true;
      case ResynchronizationState.SEND_TO_SERVER:
        // Already requested but not yet sent.
        return true;
      case ResynchronizationState.WAITING_FOR_RESPONSE:
      default:
        // Already requested, response not yet received.
        return false;
    }
  }

  clearResynchronizationState(): void {
    this.resynchronizationState = ResynchronizationState.NOT_ACTIVE;
  }

  getResynchronizationState(): ResynchronizationState {
    return this.resynchronizationState;
  }

  hasQueuedMessages(): boolean {
    return this.messageQueue.length !== 0;
  }
}
