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

import type { ApplicationConfiguration } from '../ApplicationConfiguration';
import type { LoadingIndicatorStateHandler } from './LoadingIndicatorStateHandler';
import type { MessageHandler } from './MessageHandler';
import type { PushConfiguration } from './PushConfiguration';
import type { RequestResponseTracker } from './RequestResponseTracker';
import type { ServerRpcQueue } from './ServerRpcQueue';
import type { UILifecycle } from '../UILifecycle';
import type { XhrConnection } from './XhrConnection';
import type { PushConnection } from './PushConnection';
import type { PushConnectionFactory } from './PushConnectionFactory';
import { Console } from '../Console';

// com.vaadin.flow.shared.ApplicationConstants
const RPC_INVOCATIONS = 'rpc';
const CSRF_TOKEN = 'csrfToken';
const CSRF_TOKEN_DEFAULT_VALUE = 'init';
const SERVER_SYNC_ID = 'syncId';
const CLIENT_TO_SERVER_ID = 'clientId';
const RESYNCHRONIZE_ID = 'resynchronize';
const UNLOAD_BEACON = 'UNLOAD';

type Payload = Record<string, unknown>;

/** The slice of Registry that MessageSender uses. */
export interface MessageSenderRegistry {
  getUILifecycle(): Pick<UILifecycle, 'isRunning'>;
  getRequestResponseTracker(): Pick<
    RequestResponseTracker,
    'hasActiveRequest' | 'startRequest' | 'addReconnectionAttemptHandler'
  >;
  getServerRpcQueue(): Pick<ServerRpcQueue, 'isEmpty' | 'toJson' | 'clear' | 'isFlushPending' | 'flush'>;
  getLoadingIndicatorStateHandler(): Pick<LoadingIndicatorStateHandler, 'startLoading'>;
  getMessageHandler(): Pick<MessageHandler, 'getCsrfToken' | 'getLastSeenServerSyncId'>;
  getXhrConnection(): Pick<XhrConnection, 'send' | 'getUri'>;
  getApplicationConfiguration(): Pick<ApplicationConfiguration, 'getMaxMessageSuspendTimeout'>;
  getPushConfiguration(): Pick<PushConfiguration, 'isPushEnabled'>;
}

/** The state of a resynchronization request; mirrors MessageSender.ResynchronizationState. */
export const ResynchronizationState = {
  NOT_ACTIVE: 'NOT_ACTIVE',
  SEND_TO_SERVER: 'SEND_TO_SERVER',
  WAITING_FOR_RESPONSE: 'WAITING_FOR_RESPONSE'
} as const;

export type ResynchronizationState = (typeof ResynchronizationState)[keyof typeof ResynchronizationState];

/**
 * MessageSender is responsible for sending messages to the server.
 *
 * Internally uses {@link XhrConnection} and/or {@link PushConnection} for
 * delivering messages, depending on the application configuration.
 */
export class MessageSender {
  // Counter for the messages sent to the server. First sent message has id 0.
  #clientToServerMessageId = 0;

  #push: PushConnection | null = null;

  readonly #registry: MessageSenderRegistry;

  readonly #pushConnectionFactory: PushConnectionFactory | null;

  #resynchronizationState: ResynchronizationState = ResynchronizationState.NOT_ACTIVE;

  #pushPendingMessage: Payload | null = null;

  #messageQueue: Payload[] = [];

  #resendMessageTimer: ReturnType<typeof setTimeout> | null = null;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: MessageSenderRegistry, pushConnectionFactory: PushConnectionFactory | null = null) {
    this.#registry = registry;
    this.#pushConnectionFactory = pushConnectionFactory;
    this.#registry.getRequestResponseTracker().addReconnectionAttemptHandler((event) => {
      Console.debug(`Re-sending queued messages to the server (attempt ${event.getAttempt()}) ...`);
      // Try to reconnect by sending queued messages; stop the resend timer since
      // it will not make any request during reconnection anyway.
      this.#resetTimer();
      this.#doSendInvocationsToServer();
    });
  }

  sendUnloadBeacon(): void {
    const payload = this.#preparePayload([], { [UNLOAD_BEACON]: true });
    sendBeacon(this.#registry.getXhrConnection().getUri(), JSON.stringify(payload));
  }

  /**
   * Sends any pending invocations to the server if there is no request in
   * progress and the application is running.
   */
  sendInvocationsToServer(): void {
    if (!this.#registry.getUILifecycle().isRunning()) {
      Console.warn('Trying to send RPC from not yet started or stopped application');
      return;
    }

    const hasActiveRequest = this.#registry.getRequestResponseTracker().hasActiveRequest();
    if (hasActiveRequest || (this.#push !== null && !this.#push.isActive())) {
      // Active request, or push enabled but not active: send when the current
      // request completes or push becomes active.
      Console.debug(
        `Postpone sending invocations to server because of ${hasActiveRequest ? 'active request' : 'PUSH not active'}`
      );
    } else {
      this.#doSendInvocationsToServer();
    }
  }

  #doSendInvocationsToServer(): void {
    // If there's a stored message, resend it and postpone the rest of the queue
    // to prevent resynchronization issues.
    if (this.#pushPendingMessage !== null) {
      const payload = this.#pushPendingMessage;
      this.#pushPendingMessage = null;
      this.#sendPayload(payload);
      return;
    } else if (this.hasQueuedMessages()) {
      Console.debug('Sending queued messages to server');
      if (this.#resendMessageTimer !== null) {
        this.#resetTimer();
      }
      this.#sendPayload(this.#messageQueue[0]);
      return;
    }

    const serverRpcQueue = this.#registry.getServerRpcQueue();
    if (serverRpcQueue.isEmpty() && this.#resynchronizationState !== ResynchronizationState.SEND_TO_SERVER) {
      return;
    }

    const reqJson = serverRpcQueue.toJson();
    serverRpcQueue.clear();

    if (reqJson.length === 0 && this.#resynchronizationState !== ResynchronizationState.SEND_TO_SERVER) {
      // Nothing to send, all invocations were filtered out.
      Console.warn('All RPCs filtered out, not sending anything to the server');
      return;
    }

    const extraJson: Payload = {};
    if (this.#resynchronizationState === ResynchronizationState.SEND_TO_SERVER) {
      this.#resynchronizationState = ResynchronizationState.WAITING_FOR_RESPONSE;
      Console.warn('Resynchronizing from server');
      this.#messageQueue = [];
      this.#resetTimer();
      extraJson[RESYNCHRONIZE_ID] = true;
    }
    this.#registry.getLoadingIndicatorStateHandler().startLoading();
    this.sendRequest(reqJson, extraJson);
  }

  /**
   * Sends an asynchronous or synchronous UIDL request to the server using the
   * given URI.
   *
   * Java overloads `send` for this, one overload `protected` and the other
   * `public`; TypeScript cannot give two overloads different visibility, so the
   * protected one keeps this name.
   *
   * @param reqInvocations - Data containing RPC invocations and all related
   *          information.
   * @param extraJson - Parameters that are added to the payload
   */
  protected sendRequest(reqInvocations: unknown[], extraJson: Payload | null): void {
    this.send(this.#preparePayload(reqInvocations, extraJson));
  }

  /**
   * Sends an asynchronous or synchronous UIDL request to the server using the
   * given URI. Adds message to message queue and postpones sending if queue not
   * empty.
   *
   * @param payload - The contents of the request to send
   */
  send(payload: Payload): void {
    if (this.hasQueuedMessages()) {
      // The server sync id is set in sendPayload. If it is already present, the
      // message has already been sent and enqueued.
      if (!(SERVER_SYNC_ID in payload)) {
        this.#messageQueue.push(payload);
      }
      return;
    }
    this.#messageQueue.push(payload);
    this.#sendPayload(payload);
  }

  #preparePayload(reqInvocations: unknown[], extraJson: Payload | null): Payload {
    const payload: Payload = {};
    const csrfToken = this.#registry.getMessageHandler().getCsrfToken();
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
   * Sends an asynchronous or synchronous UIDL request to the server using the
   * given URI.
   *
   * @param payload - The contents of the request to send
   */
  #sendPayload(payload: Payload): void {
    // Do not update server sync id for enqueued messages.
    if (!(SERVER_SYNC_ID in payload)) {
      payload[SERVER_SYNC_ID] = this.#registry.getMessageHandler().getLastSeenServerSyncId();
    }
    // clientId should only be set if absent; if present we are resending.
    if (!(CLIENT_TO_SERVER_ID in payload)) {
      payload[CLIENT_TO_SERVER_ID] = this.#clientToServerMessageId++;
    }

    if (!this.#registry.getRequestResponseTracker().hasActiveRequest()) {
      // Direct calls from outside have probably not started a request.
      this.#registry.getRequestResponseTracker().startRequest();
    }

    if (this.#push !== null && this.#push.isBidirectional()) {
      // With bidirectional transport the payload is not resent during
      // reconnection; keep a copy to resend after a reconnection until the
      // server confirms it.
      this.#pushPendingMessage = payload;
      this.#push.push(payload);
    } else {
      this.#resetTimer();
      this.#registry.getXhrConnection().send(payload);
      this.#scheduleResend(payload);
    }
  }

  #resetTimer(): void {
    if (this.#resendMessageTimer !== null) {
      clearTimeout(this.#resendMessageTimer);
      this.#resendMessageTimer = null;
    }
  }

  // Resends the last payload if a response hasn't come in; reschedules itself.
  #scheduleResend(payload: Payload): void {
    const timeout = this.#registry.getApplicationConfiguration().getMaxMessageSuspendTimeout() + 500;
    this.#resendMessageTimer = setTimeout(() => {
      this.#scheduleResend(payload);
      // Avoid re-sending while a request is still in progress; if the response
      // has not been processed, the reconnection-attempt listener resends.
      if (!this.#registry.getRequestResponseTracker().hasActiveRequest()) {
        this.#registry.getRequestResponseTracker().startRequest();
        this.#registry.getXhrConnection().send(payload);
      }
    }, timeout);
  }

  /**
   * Sets the status for the push connection.
   *
   * @param enabled - `true` to enable the push connection; `false` to disable
   *          the push connection.
   * @param reEnableIfNeeded - whether a disable that finds the configuration
   *          still enabling push may re-enable it; `false` on the recursive call
   */
  setPushEnabled(enabled: boolean, reEnableIfNeeded = true): void {
    if (enabled && (this.#push === null || !this.#push.isActive())) {
      this.#push = this.#pushConnectionFactory ? this.#pushConnectionFactory(this.#registry) : null;
    } else if (!enabled && this.#push !== null && this.#push.isActive()) {
      this.#push.disconnect(() => {
        this.#push = null;
        // If push was re-enabled while waiting to disconnect, reconnect now.
        if (reEnableIfNeeded && this.#registry.getPushConfiguration().isPushEnabled()) {
          this.setPushEnabled(true);
        }
        // Send anything enqueued while waiting for the connection to close.
        if (this.#registry.getServerRpcQueue().isFlushPending()) {
          this.#registry.getServerRpcQueue().flush();
        }
      });
    }
  }

  /**
   * Returns a human readable string representation of the method used to
   * communicate with the server.
   *
   * @returns A string representation of the current transport type
   */
  getCommunicationMethodName(): string {
    let clientToServer: string | null = 'XHR';
    // Java concatenates the transport type into the string, so a null transport
    // reads as "null" there too.
    let serverToClient: string | null = '-';
    if (this.#push !== null) {
      serverToClient = this.#push.getTransportType();
      if (this.#push.isBidirectional()) {
        clientToServer = serverToClient;
      }
    }
    return `Client to server: ${clientToServer}, server to client: ${serverToClient}`;
  }

  /**
   * Resynchronize the client side, i.e. reload all component hierarchy and state
   * from the server
   */
  resynchronize(): void {
    if (this.requestResynchronize()) {
      this.#messageQueue = [];
      this.#resetTimer();
      this.sendInvocationsToServer();
    }
  }

  /**
   * Used internally to update what id the server expects.
   *
   * @param nextExpectedId - the new client id to set
   * @param force - true if the id must be updated, false otherwise
   */
  setClientToServerMessageId(nextExpectedId: number, force: boolean): void {
    if (nextExpectedId === this.#clientToServerMessageId) {
      // Remove a pending PUSH message already seen by the server.
      if (
        this.#pushPendingMessage !== null &&
        (this.#pushPendingMessage[CLIENT_TO_SERVER_ID] as number) < nextExpectedId
      ) {
        this.#pushPendingMessage = null;
      }
      if (this.hasQueuedMessages()) {
        // If the queued message is the expected one, remove it and send next.
        if ((this.#messageQueue[0][CLIENT_TO_SERVER_ID] as number) + 1 === nextExpectedId) {
          this.#messageQueue.shift();
          this.#resetTimer();
        }
      }
      return;
    }
    if (force) {
      Console.debug(`Forced update of clientId to ${this.#clientToServerMessageId}`);
      this.#clientToServerMessageId = nextExpectedId;
      this.#messageQueue = [];
      this.#resetTimer();
      return;
    }

    if (nextExpectedId > this.#clientToServerMessageId) {
      if (this.#clientToServerMessageId === 0) {
        // Never sent a message, so the server knows better (e.g. a refreshed
        // @PreserveOnRefresh UI).
        Console.debug(`Updating client-to-server id to ${nextExpectedId} based on server`);
      } else {
        Console.warn(
          `Server expects next client-to-server id to be ${nextExpectedId} but we were going to use ${
            this.#clientToServerMessageId
          }. Will use ${nextExpectedId}.`
        );
      }
      this.#clientToServerMessageId = nextExpectedId;
    }
    // else the server has not yet seen all our messages; they will arrive.
  }

  /**
   * Modifies the resynchronize state to indicate that resynchronization is
   * desired
   *
   * @returns true if the resynchronize request still needs to be sent; false
   *          otherwise
   */
  requestResynchronize(): boolean {
    switch (this.#resynchronizationState) {
      case ResynchronizationState.NOT_ACTIVE:
        Console.debug('Resynchronize from server requested');
        this.#resynchronizationState = ResynchronizationState.SEND_TO_SERVER;
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
    this.#resynchronizationState = ResynchronizationState.NOT_ACTIVE;
  }

  getResynchronizationState(): ResynchronizationState {
    return this.#resynchronizationState;
  }

  hasQueuedMessages(): boolean {
    return this.#messageQueue.length !== 0;
  }
}

// Java declares sendBeacon right after sendUnloadBeacon; a module function
// cannot live inside the class body, so it follows it here.
/**
 * Sends the `payload` to the `url` as a beacon, surviving page unload.
 *
 * @param url - the url to send the payload to
 * @param payload - the payload to send
 */
export function sendBeacon(url: string, payload: string): void {
  window.navigator.sendBeacon(url, payload);
}
