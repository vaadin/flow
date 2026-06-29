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

// Atmosphere-wiring helpers migrated from AtmospherePushConnection.java,
// registered on window.Vaadin.Flow.internal.AtmospherePushConnection by
// registerInternals; the Java methods delegate here. The AbstractJSO config
// accessors stay in Java. Also bundled to ES5 for the HtmlUnit used by
// GwtTests.
//
// FragmentedMessage below is the build-alongside TS port of the websocket
// message-fragmentation helper from AtmospherePushConnection.java (pure logic).
// The rest of AtmospherePushConnection (the Atmosphere library connection state
// machine, push/connect/disconnect/onMessage) is library/network-bound and
// integration-validated at cutover.

import { parseJSONResponse } from './MessageHandler';
import type { ResourceLoadEvent, ResourceLoadListener } from './ResourceRegistry';
import { addGetParameter } from './SharedUtil';
import type { PushConnection } from './communication/PushConnection';

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
  private readonly message: string;

  private index = 0;

  constructor(message: string) {
    this.message = message;
  }

  /** Whether another fragment remains to be retrieved. */
  hasNextFragment(): boolean {
    return this.index < this.message.length;
  }

  /** Returns the next fragment, advancing the internal cursor. */
  getNextFragment(): string {
    let result: string;
    if (this.index === 0) {
      const header = `${this.message.length}${MESSAGE_DELIMITER}`;
      const fragmentLength = WEBSOCKET_FRAGMENT_SIZE - header.length;
      result = header + this.getFragment(0, fragmentLength);
      this.index += fragmentLength;
    } else {
      result = this.getFragment(this.index, this.index + WEBSOCKET_FRAGMENT_SIZE);
      this.index += WEBSOCKET_FRAGMENT_SIZE;
    }
    return result;
  }

  private getFragment(begin: number, end: number): string {
    return this.message.substring(begin, Math.min(this.message.length, end));
  }
}

interface Atmosphere {
  subscribe: (config: unknown) => unknown;
  unsubscribeUrl: (url: string) => void;
}

// The Atmosphere connection callbacks, supplied from the Java side already
// wrapped in $entry (so uncaught exceptions reach GWT's handler). The
// getLastSeenServerSyncId value supplier is deliberately not $entry-wrapped, to
// mirror the original JSNI.
interface AtmosphereCallbacks {
  onOpen: (response: unknown) => void;
  onReopen: (response: unknown) => void;
  onMessage: (response: unknown) => void;
  onError: (response: unknown) => void;
  onTransportFailure: (reason: unknown, request: unknown) => void;
  onClose: (response: unknown) => void;
  onReconnect: (request: unknown, response: unknown) => void;
  onClientTimeout: (request: unknown) => void;
  getLastSeenServerSyncId: () => unknown;
}

function atmosphere(): Atmosphere | undefined {
  return (window as unknown as { vaadinPush?: { atmosphere?: Atmosphere } }).vaadinPush?.atmosphere;
}

/** Whether the Atmosphere push library is loaded. */
export function isAtmosphereLoaded(): boolean {
  return !!atmosphere();
}

/**
 * Builds the default Atmosphere configuration. The message delimiter character
 * code is supplied from the Java PushConstants.MESSAGE_DELIMITER constant so
 * that it stays the single source of truth.
 */
export function createConfig(messageDelimiter: number): Record<string, unknown> {
  return {
    transport: 'websocket',
    maxStreamingLength: 1000000,
    fallbackTransport: 'long-polling',
    contentType: 'application/json; charset=UTF-8',
    reconnectInterval: 5000,
    withCredentials: true,
    maxWebsocketErrorRetries: 12,
    timeout: -1,
    maxReconnectOnClose: 10000000,
    trackMessageLength: true,
    enableProtocol: true,
    handleOnlineOffline: false,
    executeCallbackBeforeReconnect: true,
    messageDelimiter: String.fromCharCode(messageDelimiter)
  };
}

/** Pushes a message over the given Atmosphere socket. */
export function doPush(socket: unknown, message: string): void {
  (socket as { push: (message: string) => void }).push(message);
}

/** Unsubscribes the Atmosphere connection for the given url. */
export function doDisconnect(url: string): void {
  atmosphere()?.unsubscribeUrl(url);
}

/**
 * Wires the connection url and callbacks onto the Atmosphere config and
 * subscribes, returning the resulting socket. The header value supplier is
 * read on every request, so it is wrapped in a function rather than assigned
 * once.
 */
export function doConnect(uri: string, config: Record<string, unknown>, callbacks: AtmosphereCallbacks): unknown {
  config.url = uri;
  config.onOpen = callbacks.onOpen;
  config.onReopen = callbacks.onReopen;
  config.onMessage = callbacks.onMessage;
  config.onError = callbacks.onError;
  config.onTransportFailure = callbacks.onTransportFailure;
  config.onClose = callbacks.onClose;
  config.onReconnect = callbacks.onReconnect;
  config.onClientTimeout = callbacks.onClientTimeout;
  config.headers = {
    'X-Vaadin-LastSeenServerSyncId': (): unknown => callbacks.getLastSeenServerSyncId()
  };
  return atmosphere()!.subscribe(config);
}

// Connection states; mirrors AtmospherePushConnection.State.
const State = {
  CONNECT_PENDING: 'CONNECT_PENDING',
  CONNECTED: 'CONNECTED',
  DISCONNECT_PENDING: 'DISCONNECT_PENDING',
  DISCONNECTED: 'DISCONNECTED'
} as const;
type State = (typeof State)[keyof typeof State];

/** An Atmosphere response object (the subset used here). atmosphere.js exposes
 * these as plain properties, not the getX() overlay methods GWT's JSNI wrapped
 * them in. */
interface AtmosphereResponse {
  transport: string;
  responseBody: string;
}

/** The connection-state callbacks AtmospherePushConnection invokes. */
interface PushConnectionStateHandler {
  pushOk(connection: PushConnection): void;
  pushError(connection: PushConnection, response: unknown): void;
  pushClosed(connection: PushConnection, response: unknown): void;
  pushClientTimeout(connection: PushConnection, response: unknown): void;
  pushReconnectPending(connection: PushConnection): void;
  pushInvalidContent(connection: PushConnection, message: string): void;
  pushNotConnected(payload: Record<string, unknown>): void;
  pushScriptLoadError(resourceUrl: string): void;
}

/** The slice of Registry AtmospherePushConnection uses. */
interface AtmospherePushRegistry {
  getUILifecycle(): {
    addHandler(handler: (event: { getUiLifecycle(): { isTerminated(): boolean } }) => void): unknown;
  };
  getPushConfiguration(): {
    getParameters(): Map<string, string>;
    getPushServletMapping(): string | null;
    isAlwaysXhrToServer(): boolean;
  };
  getConnectionStateHandler(): PushConnectionStateHandler;
  getApplicationConfiguration(): {
    getServiceUrl(): string;
    getContextRootUrl(): string;
    getUIId(): number;
    isProductionMode(): boolean;
  };
  getURIResolver(): { resolveVaadinUri(uri: string): string | null };
  getMessageHandler(): {
    getPushId(): string | null;
    getLastSeenServerSyncId(): number;
    handleMessage(json: Record<string, unknown>): void;
  };
  getResourceLoader(): { loadScript(url: string, listener: ResourceLoadListener | null): void };
}

/**
 * Bidirectional server push over the Atmosphere library; mirrors
 * AtmospherePushConnection.java. Composes the Atmosphere-wiring helpers above and
 * the FragmentedMessage splitter; the Registry members are contracts.
 */
export class AtmospherePushConnection implements PushConnection {
  private readonly registry: AtmospherePushRegistry;

  private state: State = State.CONNECT_PENDING;

  private readonly config: Record<string, unknown>;

  private socket: unknown = null;

  private pushUri: string | null = null;

  private transport: string | null = null;

  private url = '';

  private pendingDisconnectCommand: (() => void) | null = null;

  constructor(registry: AtmospherePushRegistry) {
    this.registry = registry;
    registry.getUILifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        if (this.state === State.DISCONNECT_PENDING || this.state === State.DISCONNECTED) {
          return;
        }
        this.disconnect(() => {});
      }
    });

    this.config = createConfig(MESSAGE_DELIMITER.charCodeAt(0));
    // Always debug for now.
    this.config.logLevel = 'debug';

    this.registry
      .getPushConfiguration()
      .getParameters()
      .forEach((value, key) => {
        if (value.toLowerCase() === 'true' || value.toLowerCase() === 'false') {
          this.config[key] = value.toLowerCase() === 'true';
        } else {
          this.config[key] = value;
        }
      });

    this.url = this.computePushUrl();

    this.runWhenAtmosphereLoaded(() => setTimeout(() => this.connect(), 0));
  }

  private computePushUrl(): string {
    const pushConfiguration = this.registry.getPushConfiguration();
    const applicationConfiguration = this.registry.getApplicationConfiguration();
    const pushServletMapping = pushConfiguration.getPushServletMapping();

    if (pushServletMapping === null || pushServletMapping.trim() === '' || pushServletMapping === '/') {
      // Default push mapping + serviceUrl.
      let url = PUSH_MAPPING;
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

  private getConnectionStateHandler(): PushConnectionStateHandler {
    return this.registry.getConnectionStateHandler();
  }

  private connect(): void {
    let pushUrl = this.registry.getURIResolver().resolveVaadinUri(this.url) ?? this.url;
    pushUrl = addGetParameter(pushUrl, REQUEST_TYPE_PARAMETER, REQUEST_TYPE_PUSH);
    pushUrl = addGetParameter(pushUrl, UI_ID_PARAMETER, this.registry.getApplicationConfiguration().getUIId());

    const pushId = this.registry.getMessageHandler().getPushId();
    if (pushId !== null) {
      pushUrl = addGetParameter(pushUrl, PUSH_ID_PARAMETER, pushId);
    }

    this.pushUri = pushUrl;
    this.socket = doConnect(pushUrl, this.config, {
      onOpen: (response) => this.onOpen(response as AtmosphereResponse),
      onReopen: (response) => this.onReopen(response as AtmosphereResponse),
      onMessage: (response) => this.onMessage(response as AtmosphereResponse),
      onError: (response) => this.onError(response as AtmosphereResponse),
      onTransportFailure: () => this.onTransportFailure(),
      onClose: (response) => this.onClose(response as AtmosphereResponse),
      onReconnect: (request, response) => this.onReconnect(request, response as AtmosphereResponse),
      onClientTimeout: (response) => this.onClientTimeout(response as AtmosphereResponse),
      getLastSeenServerSyncId: () => this.registry.getMessageHandler().getLastSeenServerSyncId()
    });
  }

  isActive(): boolean {
    return this.state === State.CONNECT_PENDING || this.state === State.CONNECTED;
  }

  isBidirectional(): boolean {
    if (this.transport === null || this.transport !== 'websocket') {
      // Not using websockets -> send XHRs.
      return false;
    }
    if (this.registry.getPushConfiguration().isAlwaysXhrToServer()) {
      // The user has forced XHR.
      return false;
    }
    // CONNECT_PENDING still reports bidirectional: the message is delayed until
    // the connection is established, when bidirectionality is re-checked.
    return true;
  }

  getTransportType(): string {
    return this.transport ?? '';
  }

  push(message: Record<string, unknown>): void {
    if (!this.isBidirectional()) {
      throw new Error('This server to client push connection should not be used to send client to server messages');
    }
    if (this.state === State.CONNECTED) {
      const messageJson = JSON.stringify(message);
      if (this.transport === 'websocket') {
        const fragmented = new FragmentedMessage(messageJson);
        while (fragmented.hasNextFragment()) {
          doPush(this.socket, fragmented.getNextFragment());
        }
      } else {
        doPush(this.socket, messageJson);
      }
      return;
    }
    if (this.state === State.CONNECT_PENDING) {
      this.getConnectionStateHandler().pushNotConnected(message);
      return;
    }
    throw new Error('Can not push after disconnecting');
  }

  disconnect(command: () => void): void {
    switch (this.state) {
      case State.CONNECT_PENDING:
        // Let the connection callback initiate the disconnect once connected.
        this.state = State.DISCONNECT_PENDING;
        this.pendingDisconnectCommand = command;
        break;
      case State.CONNECTED:
        doDisconnect(this.pushUri!);
        this.state = State.DISCONNECTED;
        command();
        break;
      default:
        throw new Error('Can not disconnect more than once');
    }
  }

  private onReopen(response: AtmosphereResponse): void {
    this.onConnect(response);
  }

  private onOpen(response: AtmosphereResponse): void {
    this.onConnect(response);
  }

  private onConnect(response: AtmosphereResponse): void {
    this.transport = response.transport;
    switch (this.state) {
      case State.CONNECT_PENDING:
        this.state = State.CONNECTED;
        this.getConnectionStateHandler().pushOk(this);
        break;
      case State.DISCONNECT_PENDING:
        // Connected so the pending disconnect can actually close the connection.
        this.state = State.CONNECTED;
        this.disconnect(this.pendingDisconnectCommand!);
        break;
      case State.CONNECTED:
        // Some browsers open the same connection multiple times; ignore.
        break;
      default:
        throw new Error(`Got onOpen event when connection state is ${this.state}. This should never happen.`);
    }
  }

  private onMessage(response: AtmosphereResponse): void {
    const message = response.responseBody;
    // Like MessageHandler.parseJson, treat unparseable content as null.
    let json: Record<string, unknown> | null;
    try {
      json = parseJSONResponse(message) as Record<string, unknown> | null;
    } catch {
      json = null;
    }
    if (json === null) {
      this.getConnectionStateHandler().pushInvalidContent(this, message);
    } else {
      this.registry.getMessageHandler().handleMessage(json);
    }
  }

  private onTransportFailure(): void {
    console.warn('Push connection using the primary method failed. Trying the fallback transport.');
  }

  private onError(response: AtmosphereResponse): void {
    this.state = State.DISCONNECTED;
    this.getConnectionStateHandler().pushError(this, response);
  }

  private onClose(response: AtmosphereResponse): void {
    this.state = State.CONNECT_PENDING;
    this.getConnectionStateHandler().pushClosed(this, response);
  }

  private onClientTimeout(response: AtmosphereResponse): void {
    this.state = State.DISCONNECTED;
    this.getConnectionStateHandler().pushClientTimeout(this, response);
  }

  private onReconnect(_request: unknown, _response: AtmosphereResponse): void {
    if (this.state === State.CONNECTED) {
      this.state = State.CONNECT_PENDING;
    }
    this.getConnectionStateHandler().pushReconnectPending(this);
  }

  private runWhenAtmosphereLoaded(command: () => void): void {
    if (isAtmosphereLoaded()) {
      command();
      return;
    }
    const pushJs = this.registry.getApplicationConfiguration().isProductionMode()
      ? VAADIN_PUSH_JS
      : VAADIN_PUSH_DEBUG_JS;
    const pushScriptUrl = this.registry.getApplicationConfiguration().getServiceUrl() + pushJs;
    const listener: ResourceLoadListener = {
      onLoad: (event: ResourceLoadEvent) => {
        if (isAtmosphereLoaded()) {
          command();
        } else {
          // ResourceLoader assumes bootstrap's vaadinPush.js load succeeded even
          // if it failed (#11673).
          listener.onError(event);
        }
      },
      onError: (event: ResourceLoadEvent) => {
        this.getConnectionStateHandler().pushScriptLoadError(event.getResourceData());
      }
    };
    this.registry.getResourceLoader().loadScript(pushScriptUrl, listener);
  }
}

/** The default PushConnectionFactory: creates an AtmospherePushConnection. */
export const atmospherePushConnectionFactory = (registry: unknown): PushConnection =>
  new AtmospherePushConnection(registry as AtmospherePushRegistry);
