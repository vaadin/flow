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

// The push connection on top of atmosphere.js 5, whose ES module build is
// served as vaadinPush.js and loaded on demand.
//
// FragmentedMessage below is the TS port of the websocket
// message-fragmentation helper from AtmospherePushConnection.java (pure logic).
// The rest of AtmospherePushConnection is the Atmosphere library connection state
// machine (push/connect/disconnect/onMessage), which is library/network-bound.

import type { ConnectionStateHandler } from './ConnectionStateHandler';
import type { Registry } from '../Registry';
import { assert } from '../../assert';
import { stringify } from '../WidgetUtil';
import { parseJson } from './MessageHandler';
import { loadAtmosphere } from './AtmosphereLoader';
import { addGetParameter } from '../../flow/shared/util/SharedUtil';
import type { Command } from '../Command';
import type { PushConnection } from './PushConnection';
import type { PushConnectionFactory } from './PushConnectionFactory';
import { Console } from '../Console';

// com.vaadin.flow.shared.communication.PushConstants
const WEBSOCKET_FRAGMENT_SIZE = 16384 / 4 - 1; // 4095
const MESSAGE_DELIMITER = '|';

// com.vaadin.flow.server.Constants / shared.ApplicationConstants
const PUSH_MAPPING = 'VAADIN/push';
const VAADIN_PUSH_JS = 'VAADIN/static/push/vaadinPush-min.js';
const VAADIN_PUSH_DEBUG_JS = 'VAADIN/static/push/vaadinPush.js';
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_PUSH = 'push';
const UI_ID_PARAMETER = 'v-uiId';
const PUSH_ID_PARAMETER = 'v-pushId';

/**
 * Splits a message into websocket fragments of at most WEBSOCKET_FRAGMENT_SIZE
 * characters; the first fragment is prefixed with `<length><delimiter>` so the
 * receiver can reassemble it. Mirrors AtmospherePushConnection.FragmentedMessage.
 */
export class FragmentedMessage {
  readonly #message: string;

  #index = 0;

  /**
   * Creates a new instance based on the given message.
   *
   * @param message - the message to wrap
   */
  constructor(message: string) {
    this.#message = message;
  }

  /**
   * Checks if there is another fragment which can be retrieved using
   * {@link FragmentedMessage.getNextFragment} or if all fragments have been
   * retrieved.
   *
   * @returns true if there is another fragment to retrieve, false otherwise
   */
  hasNextFragment(): boolean {
    return this.#index < this.#message.length;
  }

  /**
   * Gets the following fragment and increments the internal fragment counter so the
   * following call to this method will return the following fragment. This method
   * should not be called if all fragments have been received ({@link FragmentedMessage.hasNextFragment}
   * returns false).
   *
   * @returns the next fragment
   */
  getNextFragment(): string {
    assert(this.hasNextFragment(), 'No fragments left');
    let result: string;
    if (this.#index === 0) {
      const header = `${this.#message.length}${MESSAGE_DELIMITER}`;
      const fragmentLength = WEBSOCKET_FRAGMENT_SIZE - header.length;
      result = header + this.#getFragment(0, fragmentLength);
      this.#index += fragmentLength;
    } else {
      result = this.#getFragment(this.#index, this.#index + WEBSOCKET_FRAGMENT_SIZE);
      this.#index += WEBSOCKET_FRAGMENT_SIZE;
    }
    return result;
  }

  #getFragment(begin: number, end: number): string {
    return this.#message.substring(begin, Math.min(this.#message.length, end));
  }
}

/** The atmosphere.js module, vaadinPush.js (the subset used here). */
interface AtmosphereModule {
  Atmosphere: new (config: { logLevel?: unknown }) => AtmosphereClient;
}

interface AtmosphereClient {
  subscribe: (request: AtmosphereRequest, handlers: AtmosphereHandlers) => Promise<AtmosphereSubscription>;
}

interface AtmosphereSubscription {
  push: (message: string) => void;
  close: () => Promise<void>;
}

/** The request atmosphere.js connects with. */
type AtmosphereRequest = Record<string, unknown>;

interface AtmosphereHandlers {
  open: (response: AtmosphereResponse) => void;
  reopen: (response: AtmosphereResponse) => void;
  message: (response: AtmosphereResponse) => void;
  error: (error: Error) => void;
  transportFailure: (reason: string, request: AtmosphereRequest) => void;
  close: (response: AtmosphereResponse) => void;
  reconnect: (request: AtmosphereRequest, response: AtmosphereResponse) => void;
  failureToReconnect: (request: AtmosphereRequest, response: AtmosphereResponse) => void;
  clientTimeout: (request: AtmosphereRequest) => void;
}

/**
 * Creates the request atmosphere.js subscribes with from the connection url
 * and the Atmosphere config. atmosphere.js calls a header function for every
 * request it makes, so the last seen sync id is read anew rather than fixed
 * when the connection was made.
 */
function createRequest(
  uri: string,
  config: Record<string, unknown>,
  getLastSeenServerSyncId: () => unknown
): AtmosphereRequest {
  return {
    ...config,
    url: uri,
    // atmosphere.js only reconnects a lost connection when asked to
    reconnect: true,
    // Retry every reconnectInterval, instead of the exponential backoff
    // atmosphere.js applies by default
    retryStrategy: { strategy: 'custom', customCalculate: () => Number(config.reconnectInterval) },
    headers: {
      'X-Vaadin-LastSeenServerSyncId': () => String(getLastSeenServerSyncId())
    }
  };
}

// Connection states; mirrors AtmospherePushConnection.State.
const State = {
  CONNECT_PENDING: 'CONNECT_PENDING',
  CONNECTED: 'CONNECTED',
  DISCONNECT_PENDING: 'DISCONNECT_PENDING',
  DISCONNECTED: 'DISCONNECTED'
} as const;
type State = (typeof State)[keyof typeof State];

/** The atmosphere configuration/response key holding the transport name. */
export const TRANSPORT_KEY = 'transport';

/** An Atmosphere response object (the subset used here). */
interface AtmosphereResponse {
  [TRANSPORT_KEY]: string;
  responseBody: unknown;
  request?: AtmosphereRequest;
}

/**
 * The default {@link PushConnection} implementation that uses Atmosphere for
 * handling the communication channel.
 *
 * Composes the Atmosphere-wiring helpers above and the FragmentedMessage
 * splitter.
 */
export class AtmospherePushConnection implements PushConnection {
  readonly #registry: Registry;

  #state: State = State.CONNECT_PENDING;

  readonly #config: Record<string, unknown>;

  #socket: AtmosphereSubscription | null = null;

  #transport: string | null = null;

  #url = '';

  #pendingDisconnectCommand: Command | null = null;

  constructor(registry: Registry) {
    this.#registry = registry;
    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        if (this.#state === State.DISCONNECT_PENDING || this.#state === State.DISCONNECTED) {
          return;
        }
        this.disconnect(() => {});
      }
    });

    this.#config = this.createConfig();
    // Always debug for now.
    this.#config.logLevel = 'debug';

    this.#registry
      .getPushConfiguration()
      .getParameters()
      .forEach((value, key) => {
        if (value.toLowerCase() === 'true' || value.toLowerCase() === 'false') {
          this.#config[key] = value.toLowerCase() === 'true';
        } else {
          this.#config[key] = value;
        }
      });

    this.#url = this.#computePushUrl();

    this.#loadAtmosphere();
  }

  #getConnectionStateHandler(): ConnectionStateHandler {
    return this.#registry.getConnectionStateHandler();
  }

  #connect(atmosphere: AtmosphereModule): void {
    let pushUrl = this.#registry.getURIResolver().resolveVaadinUri(this.#url) ?? this.#url;
    pushUrl = addGetParameter(pushUrl, REQUEST_TYPE_PARAMETER, REQUEST_TYPE_PUSH);
    pushUrl = addGetParameter(pushUrl, UI_ID_PARAMETER, this.#registry.getApplicationConfiguration().getUIId());

    const pushId = this.#registry.getMessageHandler().getPushId();
    if (pushId !== null) {
      pushUrl = addGetParameter(pushUrl, PUSH_ID_PARAMETER, pushId);
    }

    Console.debug('Establishing push connection');
    const request = createRequest(pushUrl, this.#config, () =>
      this.#registry.getMessageHandler().getLastSeenServerSyncId()
    );
    const client = new atmosphere.Atmosphere({ logLevel: this.#config.logLevel });
    client
      .subscribe(request, {
        open: (response) => this.onOpen(response),
        reopen: (response) => this.onReopen(response),
        message: (response) => this.onMessage(response),
        // atmosphere.js reports every failed attempt here and goes on to reconnect
        // or fall back, so only a connection that is given up on is an error.
        error: (error) => Console.debug(`Push connection error: ${error.message}`),
        transportFailure: () => this.onTransportFailure(),
        close: (response) => this.onClose(response),
        reconnect: (req, response) => this.onReconnect(req, response),
        failureToReconnect: (_req, response) => this.onError(response),
        clientTimeout: (req) =>
          this.onClientTimeout({ transport: String(req.transport), responseBody: '', request: req })
      })
      .then(
        (subscription) => {
          this.#socket = subscription;
        },
        (error: unknown) => {
          Console.debug(`Push connection could not be established: ${String(error)}`);
          // Neither the transport nor the fallback could connect
          this.onError({
            transport: String(this.#config.fallbackTransport ?? this.#config.transport),
            responseBody: ''
          });
        }
      );
  }

  #computePushUrl(): string {
    const pushConfiguration = this.#registry.getPushConfiguration();
    const applicationConfiguration = this.#registry.getApplicationConfiguration();
    const pushServletMapping = pushConfiguration.getPushServletMapping();

    if (pushServletMapping === null || pushServletMapping.trim() === '' || pushServletMapping === '/') {
      // Handle null, empty and "/" mapping using just default push mapping and
      // serviceUrl.
      let url = PUSH_MAPPING;
      // If a specific serviceUrl is defined, prepend pushUrl with it.
      let serviceUrl = applicationConfiguration.getServiceUrl();
      if (serviceUrl !== '.') {
        if (!serviceUrl.endsWith('/')) {
          serviceUrl += '/';
        }
        url = serviceUrl + url;
      }
      return url;
    }

    // Append the specific mapping directly to the context root URL.
    let mapping = pushServletMapping;
    const contextRootUrl = applicationConfiguration.getContextRootUrl();
    if (contextRootUrl.endsWith('/') && mapping.startsWith('/')) {
      mapping = mapping.substring(1);
    }
    return contextRootUrl + mapping + PUSH_MAPPING;
  }

  isActive(): boolean {
    return this.#state === State.CONNECT_PENDING || this.#state === State.CONNECTED;
  }

  isBidirectional(): boolean {
    if (this.#transport === null || this.#transport !== 'websocket') {
      // Not using websockets -> send XHRs.
      return false;
    }
    if (this.#registry.getPushConfiguration().isAlwaysXhrToServer()) {
      // The user has forced XHR.
      return false;
    }
    // CONNECT_PENDING still reports bidirectional: the message is delayed until
    // the connection is established, when bidirectionality is re-checked.
    return true;
  }

  push(message: Record<string, unknown>): void {
    if (!this.isBidirectional()) {
      throw new Error('This server to client push connection should not be used to send client to server messages');
    }
    if (this.#state === State.CONNECTED) {
      const messageJson = stringify(message);
      Console.debug(`Sending push (${this.#transport}) message to server: ${messageJson}`);
      if (this.#transport === 'websocket') {
        const fragmented = new FragmentedMessage(messageJson);
        while (fragmented.hasNextFragment()) {
          this.#socket!.push(fragmented.getNextFragment());
        }
      } else {
        this.#socket!.push(messageJson);
      }
      return;
    }
    if (this.#state === State.CONNECT_PENDING) {
      this.#getConnectionStateHandler().pushNotConnected(message);
      return;
    }
    throw new Error('Can not push after disconnecting');
  }

  protected getConfig(): Record<string, unknown> {
    return this.#config;
  }

  protected onReopen(response: AtmosphereResponse): void {
    Console.debug(`Push connection re-established using ${response.transport}`);
    this.onConnect(response);
  }

  protected onOpen(response: AtmosphereResponse): void {
    Console.debug(`Push connection established using ${response.transport}`);
    this.onConnect(response);
  }

  /**
   * Called whenever a server push connection is established (or re-established).
   *
   * @param response - the response
   */
  protected onConnect(response: AtmosphereResponse): void {
    this.#transport = response.transport;
    switch (this.#state) {
      case State.CONNECT_PENDING:
        this.#state = State.CONNECTED;
        this.#getConnectionStateHandler().pushOk(this);
        break;
      case State.DISCONNECT_PENDING:
        // Connected so the pending disconnect can actually close the connection.
        this.#state = State.CONNECTED;
        assert(this.#pendingDisconnectCommand !== null, 'No pending disconnect command');
        this.disconnect(this.#pendingDisconnectCommand!);
        break;
      case State.CONNECTED:
        // Some browsers open the same connection multiple times; ignore.
        break;
      default:
        throw new Error(`Got onOpen event when connection state is ${this.#state}. This should never happen.`);
    }
  }

  disconnect(command: Command): void {
    // Java asserts command != null; the parameter is non-nullable here, so the
    // check is unreachable and dropped.
    switch (this.#state) {
      case State.CONNECT_PENDING:
        // Let the connection callback initiate the disconnect once connected.
        this.#state = State.DISCONNECT_PENDING;
        this.#pendingDisconnectCommand = command;
        break;
      case State.CONNECTED:
        // Normal disconnect
        Console.debug('Closing push connection');
        // Also sends Atmosphere's close message, so the server releases a
        // long-polling request instead of pushing into it
        this.#socket!.close().catch(() => {
          // The connection is gone either way
        });
        this.#state = State.DISCONNECTED;
        command();
        break;
      default:
        throw new Error('Can not disconnect more than once');
    }
  }

  /**
   * Called whenever a message is received by Atmosphere.
   *
   * @param response - the Atmosphere response object, which contains the message
   */
  protected onMessage(response: AtmosphereResponse): void {
    const message = String(response.responseBody);
    const json = parseJson(message);
    if (json === null) {
      // Invalid JSON string
      this.#getConnectionStateHandler().pushInvalidContent(this, message);
    } else {
      Console.debug(`Received push (${this.getTransportType()}) message: ${message}`);
      this.#registry.getMessageHandler().handleMessage(json);
    }
  }

  protected onTransportFailure(): void {
    Console.warn('Push connection using the primary method failed. Trying the fallback transport.');
  }

  /**
   * Called if the push connection fails.
   *
   * Atmosphere will automatically retry the connection until successful.
   *
   * @param response - the Atmosphere response for the failed connection
   */
  protected onError(response: AtmosphereResponse): void {
    this.#state = State.DISCONNECTED;
    this.#getConnectionStateHandler().pushError(this, response);
  }

  /**
   * Called when the push connection has been closed.
   *
   * This does not necessarily indicate an error and Atmosphere might try to
   * reconnect or downgrade to the fallback transport automatically.
   *
   * @param response - the Atmosphere response which was closed
   */
  protected onClose(response: AtmosphereResponse): void {
    this.#state = State.CONNECT_PENDING;
    this.#getConnectionStateHandler().pushClosed(this, response);
  }

  /**
   * Called when the Atmosphere client side timeout occurs.
   *
   * The connection will be closed at this point and reconnect will not happen
   * automatically.
   *
   * @param response - the Atmosphere response which was used when the timeout
   *          occurred
   */
  protected onClientTimeout(response: AtmosphereResponse): void {
    this.#state = State.DISCONNECTED;
    this.#getConnectionStateHandler().pushClientTimeout(this, response);
  }

  /**
   * Called when the push connection has lost the connection to the server and
   * will proceed to try to re-establish the connection.
   *
   * @param _request - the Atmosphere request
   * @param _response - the Atmosphere response
   */
  protected onReconnect(_request: unknown, _response: AtmosphereResponse): void {
    if (this.#state === State.CONNECTED) {
      this.#state = State.CONNECT_PENDING;
    }
    this.#getConnectionStateHandler().pushReconnectPending(this);
  }

  /**
   * Creates the default Atmosphere configuration object.
   *
   * @returns the Atmosphere configuration object
   */
  protected createConfig(): Record<string, unknown> {
    return {
      transport: 'websocket',
      fallbackTransport: 'long-polling',
      contentType: 'application/json; charset=UTF-8',
      reconnectInterval: 5000,
      withCredentials: true,
      timeout: -1,
      maxReconnectOnClose: 10000000,
      trackMessageLength: true,
      enableProtocol: true,
      messageDelimiter: MESSAGE_DELIMITER
    };
  }

  #loadAtmosphere(): void {
    const pushJs = this.#registry.getApplicationConfiguration().isProductionMode()
      ? VAADIN_PUSH_JS
      : VAADIN_PUSH_DEBUG_JS;
    const pushScriptUrl = this.#registry.getApplicationConfiguration().getServiceUrl() + pushJs;
    loadAtmosphere(pushScriptUrl).then(
      (atmosphere) => this.#connect(atmosphere as AtmosphereModule),
      (error: unknown) => {
        Console.debug(`Loading ${pushScriptUrl} failed: ${String(error)}`);
        this.#getConnectionStateHandler().pushScriptLoadError(pushScriptUrl);
      }
    );
  }

  getTransportType(): string | null {
    return this.#transport;
  }
}

/** The default {@link PushConnectionFactory}: creates an AtmospherePushConnection. */
export const atmospherePushConnectionFactory: PushConnectionFactory = (registry: unknown): PushConnection =>
  new AtmospherePushConnection(registry as Registry);
