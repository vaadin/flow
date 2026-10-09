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
// state machine, an outgoing message queue, and a resend timer. Push connections
// are created through an injected factory (GWT.create in the Java version).
// Beyond the Java version, it tracks the delivery of each payload as a
// VaadinRequest that page scripts can follow through the event bus.

import type { Registry } from '../Registry';
import type { XhrConnection } from './XhrConnection';
import type { PushConnection } from './PushConnection';
import type { PushConnectionFactory } from './PushConnectionFactory';
import { Console } from '../Console';
import {
  dispatchError,
  dispatchRequestEnd,
  dispatchSent,
  VaadinRequest,
  type VaadinRequestFailure,
  type VaadinRequestOutcome
} from './VaadinRequest';

// com.vaadin.flow.shared.ApplicationConstants
const RPC_INVOCATIONS = 'rpc';
const CSRF_TOKEN = 'csrfToken';
const CSRF_TOKEN_DEFAULT_VALUE = 'init';
const SERVER_SYNC_ID = 'syncId';
const CLIENT_TO_SERVER_ID = 'clientId';
const RESYNCHRONIZE_ID = 'resynchronize';
const UNLOAD_BEACON = 'UNLOAD';

type Payload = Record<string, unknown>;

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

  readonly #registry: Registry;

  readonly #pushConnectionFactory: PushConnectionFactory | null;

  #resynchronizationState: ResynchronizationState = ResynchronizationState.NOT_ACTIVE;

  #pushPendingMessage: Payload | null = null;

  #messageQueue: Payload[] = [];

  #resendMessageTimer: ReturnType<typeof setTimeout> | null = null;

  // The request tracking each payload, from the first send until the payload is
  // no longer referenced.
  readonly #requests = new WeakMap<Payload, VaadinRequest>();

  // The request announced for the invocations queued in the ServerRpcQueue,
  // until they are sent as a payload.
  #openRequest: VaadinRequest | null = null;

  /**
   * Creates a new instance connected to the given registry.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry, pushConnectionFactory: PushConnectionFactory | null = null) {
    this.#registry = registry;
    this.#pushConnectionFactory = pushConnectionFactory;
    this.#registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        // Nothing is sent after the application stops.
        this.discardSentRequests();
        if (this.#openRequest !== null) {
          dispatchRequestEnd(this.#openRequest, 'discarded');
          this.#openRequest = null;
        }
      }
    });
  }

  /**
   * Re-sends the queued messages to the server as a reconnection attempt.
   *
   * @param attempt - the number of the reconnection attempt, starting from 1
   */
  resendQueuedMessages(attempt: number): void {
    Console.debug(`Re-sending queued messages to the server (attempt ${attempt}) ...`);
    // Try to reconnect by sending queued messages; stop the resend timer since
    // it will not make any request during reconnection anyway.
    this.#resetTimer();
    this.#doSendInvocationsToServer();
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
      Console.log(`Sending pending push message ${JSON.stringify(payload)}`);
      this.#pushPendingMessage = null;
      this.#sendPayload(payload);
      return;
    } else if (this.hasQueuedMessages()) {
      Console.debug('Sending queued messages to server');
      if (this.#resendMessageTimer !== null) {
        // Stopping resend timer and re-send immediately
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
      // Nothing to send, all invocations were filtered out (for non-existing
      // connectors)
      Console.warn('All RPCs filtered out, not sending anything to the server');
      if (this.#openRequest !== null) {
        dispatchRequestEnd(this.#openRequest, 'discarded');
        this.#openRequest = null;
      }
      return;
    }

    const extraJson: Payload = {};
    if (this.#resynchronizationState === ResynchronizationState.SEND_TO_SERVER) {
      this.#resynchronizationState = ResynchronizationState.WAITING_FOR_RESPONSE;
      Console.warn('Resynchronizing from server');
      this.#clearMessageQueue();
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
   * Java overloads `send` for this: the two-argument overload is `protected` and
   * the one-argument one, ported as `send`, is `public`. TypeScript cannot give
   * two overloads different visibility, so the protected one keeps this name.
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
    if (!this.#requests.has(payload)) {
      this.#requests.set(payload, this.#takeOpenRequest());
    }
    if (this.hasQueuedMessages()) {
      // The server sync id is set in sendPayload. If it is already present, the
      // message has already been sent and enqueued.
      if (!(SERVER_SYNC_ID in payload)) {
        this.#messageQueue.push(payload);
        Console.debug(
          `Message not sent because other messages are pending. Added to the queue: ${JSON.stringify(payload)}`
        );
      } else {
        Console.debug(`Message not sent because already queued: ${JSON.stringify(payload)}`);
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
      Console.debug('send PUSH');
      this.#pushPendingMessage = payload;
      this.#transmit(payload, true);
    } else {
      Console.debug('send XHR');
      this.#resetTimer();
      this.#transmit(payload, false);
      this.#scheduleResend(payload);
    }
  }

  // Every transmission of a payload goes through here, so that its request
  // counts each attempt, whatever made the client send it.
  #transmit(payload: Payload, overPush: boolean): void {
    const request = this.#requests.get(payload);
    if (request) {
      dispatchSent(request);
    }
    if (overPush) {
      this.#push!.push(payload);
    } else {
      this.#registry.getXhrConnection().send(payload);
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
        this.failAttempt(payload, { reason: 'timeout' });
        this.#registry.getRequestResponseTracker().startRequest();
        this.#transmit(payload, false);
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
      this.#clearMessageQueue();
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
      // Everything matches the way it should. Remove a potential pending PUSH
      // message if it has already been seen by the server.
      if (
        this.#pushPendingMessage !== null &&
        (this.#pushPendingMessage[CLIENT_TO_SERVER_ID] as number) < nextExpectedId
      ) {
        this.settleRequest(this.#pushPendingMessage, 'acknowledged');
        this.#pushPendingMessage = null;
      }
      if (this.hasQueuedMessages()) {
        // If the queued message is the expected one, remove it and send next.
        if ((this.#messageQueue[0][CLIENT_TO_SERVER_ID] as number) + 1 === nextExpectedId) {
          this.settleRequest(this.#messageQueue.shift()!, 'acknowledged');
          this.#resetTimer();
        }
      }
      return;
    }
    if (force) {
      Console.debug(`Forced update of clientId to ${this.#clientToServerMessageId}`);
      this.#clientToServerMessageId = nextExpectedId;
      this.#clearMessageQueue();
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

  /**
   * Announces a request for the invocations being queued, unless one is
   * already announced for them. Called when the first invocation is queued.
   */
  openRequest(): void {
    if (this.#openRequest === null) {
      this.#openRequest = new VaadinRequest();
      this.#registry.getEventBus().fireEvent('vaadin-request', this.#openRequest);
    }
  }

  // Hands the announced request over to the payload being sent, announcing one
  // first for a payload without invocations, such as a resynchronization.
  #takeOpenRequest(): VaadinRequest {
    this.openRequest();
    const request = this.#openRequest!;
    this.#openRequest = null;
    return request;
  }

  /**
   * Gets the request tracking the delivery of a payload.
   *
   * @param payload - a payload this sender has sent
   * @returns the request, or `undefined` if the payload was not sent by this sender
   */
  getRequest(payload: Payload): VaadinRequest | undefined {
    return this.#requests.get(payload);
  }

  /**
   * Finds the request that a reply received over push answers, among the
   * payloads still waiting for confirmation.
   *
   * @param nextClientId - the client-to-server id the reply says the server
   *          expects next, or `undefined` if it carries none, in which case the
   *          oldest unconfirmed payload is taken
   * @returns the request, or `undefined` if no unconfirmed payload matches
   */
  findRequest(nextClientId: number | undefined): VaadinRequest | undefined {
    const payload =
      nextClientId === undefined
        ? (this.#pushPendingMessage ?? this.#messageQueue[0])
        : [this.#pushPendingMessage, ...this.#messageQueue].find(
            (candidate) => candidate?.[CLIENT_TO_SERVER_ID] === nextClientId - 1
          );
    return payload ? this.#requests.get(payload) : undefined;
  }

  /**
   * Reports that an attempt to deliver a payload failed.
   *
   * @param payload - the payload whose attempt failed
   * @param failure - why the attempt failed
   */
  failAttempt(payload: Payload, failure: VaadinRequestFailure): void {
    const request = this.#requests.get(payload);
    if (request) {
      dispatchError(request, failure);
    }
  }

  /**
   * Reports that the attempt to deliver the message last sent over push
   * failed, if one is waiting for confirmation.
   *
   * @param failure - why the attempt failed
   */
  failPushAttempt(failure: VaadinRequestFailure): void {
    if (this.#pushPendingMessage !== null) {
      this.failAttempt(this.#pushPendingMessage, failure);
    }
  }

  /**
   * Settles the delivery of a payload.
   *
   * @param payload - the payload whose delivery settled
   * @param outcome - how it settled
   */
  settleRequest(payload: Payload, outcome: VaadinRequestOutcome): void {
    const request = this.#requests.get(payload);
    if (request) {
      dispatchRequestEnd(request, outcome);
    }
  }

  /**
   * Gives up on the payloads sent but not yet confirmed: their requests end as
   * discarded. The payloads themselves stay queued, so the client may still
   * send them again, but that is no longer reported.
   */
  discardSentRequests(): void {
    if (this.#pushPendingMessage !== null) {
      this.settleRequest(this.#pushPendingMessage, 'discarded');
    }
    this.#messageQueue.forEach((payload) => this.settleRequest(payload, 'discarded'));
  }

  #clearMessageQueue(): void {
    this.#messageQueue.forEach((payload) => this.settleRequest(payload, 'discarded'));
    this.#messageQueue = [];
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
