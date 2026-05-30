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
import type { ResourceLoadEvent } from '../ResourceLoader';
import { MessageHandler } from './MessageHandler';
import type { JsonObject, PushConnection } from './PushConnection';

// Mirrors ApplicationConstants. Strings are inlined so the TS module doesn't
// need to take a flow-shared bridge dependency.
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_PUSH = 'push';
const UI_ID_PARAMETER = 'v-uiId';
const PUSH_ID_PARAMETER = 'v-pushId';
const VAADIN_STATIC_FILES_PATH = 'VAADIN/static/';
const VAADIN_PUSH_JS = `${VAADIN_STATIC_FILES_PATH}push/vaadinPush-min.js`;
const VAADIN_PUSH_DEBUG_JS = `${VAADIN_STATIC_FILES_PATH}push/vaadinPush.js`;
// Mirrors Constants.PUSH_MAPPING.
const PUSH_MAPPING = 'VAADIN/push';

// Mirrors PushConstants.WEBSOCKET_FRAGMENT_SIZE / MESSAGE_DELIMITER.
const WEBSOCKET_BUFFER_SIZE = 16384;
const FRAGMENT_LENGTH = WEBSOCKET_BUFFER_SIZE / 4;
const MESSAGE_DELIMITER_CODE = '|'.charCodeAt(0);

/** Connection state of the push connection. Matches the Java enum order. */
type State = 'CONNECT_PENDING' | 'CONNECTED' | 'DISCONNECT_PENDING' | 'DISCONNECTED';

interface AtmosphereSocket {
  push(message: string): void;
}

interface AtmosphereLibrary {
  subscribe(config: AtmosphereSubscribeConfig): AtmosphereSocket;
  unsubscribeUrl(url: string): void;
  version?: string;
}

interface VaadinPushGlobal {
  vaadinPush?: { atmosphere?: AtmosphereLibrary };
}

interface AtmosphereSubscribeConfig {
  [property: string]: unknown;
  url?: string;
  onOpen?: (response: unknown) => void;
  onReopen?: (response: unknown) => void;
  onMessage?: (response: unknown) => void;
  onError?: (response: unknown) => void;
  onTransportFailure?: (reason: unknown, request: unknown) => void;
  onClose?: (response: unknown) => void;
  onReconnect?: (request: unknown, response: unknown) => void;
  onClientTimeout?: (request: unknown) => void;
  headers?: Record<string, () => unknown>;
}

/** Atmosphere response object shape used by this module. */
interface AtmosphereResponse {
  status?: number;
  responseBody?: string;
  state?: string;
  transport?: string;
}

function atmosphere(): AtmosphereLibrary | undefined {
  return (globalThis as unknown as VaadinPushGlobal).vaadinPush?.atmosphere;
}

/**
 * Represents a message split into multiple fragments of maximum length
 * {@link FRAGMENT_LENGTH}.
 */
class FragmentedMessage {
  private readonly message: string;
  private index = 0;

  constructor(message: string) {
    this.message = message;
  }

  hasNextFragment(): boolean {
    return this.index < this.message.length;
  }

  getNextFragment(): string {
    let result: string;
    if (this.index === 0) {
      const header = `${this.message.length}${String.fromCharCode(MESSAGE_DELIMITER_CODE)}`;
      const fragmentLen = FRAGMENT_LENGTH - header.length;
      result = header + this.getFragment(0, fragmentLen);
      this.index += fragmentLen;
    } else {
      result = this.getFragment(this.index, this.index + FRAGMENT_LENGTH);
      this.index += FRAGMENT_LENGTH;
    }
    return result;
  }

  private getFragment(begin: number, end: number): string {
    return this.message.substring(begin, Math.min(this.message.length, end));
  }
}

/** Minimal UILifecycle shape used by this module. */
interface UILifecycleEvent {
  getUiLifecycle(): { isTerminated(): boolean };
}

interface UILifecycleLike {
  addHandler(handler: (event: UILifecycleEvent) => void): unknown;
}

/** Minimal PushConfiguration shape used by this module. */
interface PushConfigurationLike {
  getParameters(): Map<string, string>;
  getPushServletMapping(): string | null;
  isAlwaysXhrToServer(): boolean;
}

/** Minimal ApplicationConfiguration shape used by this module. */
interface ApplicationConfigurationLike {
  serviceUrl: string | null;
  contextRootUrl: string | null;
  uiId: number;
  productionMode: boolean;
}

/** Minimal URIResolver shape used by this module. */
interface URIResolverLike {
  resolveVaadinUri(uri: string | null): string | null;
}

/** Minimal MessageHandler shape used by this module. */
interface MessageHandlerLike {
  getPushId(): string | null;
  getLastSeenServerSyncId(): number;
  handleMessage(json: JsonObject): void;
}

/** Minimal ConnectionStateHandler shape used by this module. */
interface ConnectionStateHandlerLike {
  pushOk(pushConnection: PushConnection): void;
  pushError(pushConnection: PushConnection, response: unknown): void;
  pushClosed(pushConnection: PushConnection, response: unknown): void;
  pushClientTimeout(pushConnection: PushConnection, response: unknown): void;
  pushReconnectPending(pushConnection: PushConnection): void;
  pushNotConnected(payload: JsonObject): void;
  pushInvalidContent(pushConnection: PushConnection, message: string): void;
  pushScriptLoadError(resourceUrl: string): void;
}

/** Minimal ResourceLoader shape used by this module. */
interface ResourceLoaderLike {
  loadScript(
    scriptUrl: string,
    onLoad: (event: ResourceLoadEvent) => void,
    onError: (event: ResourceLoadEvent) => void
  ): void;
}

/**
 * Adds the given get parameter to the URI and returns the new URI.
 * Mirrors {@code com.vaadin.flow.shared.util.SharedUtil.addGetParameter}.
 */
function addGetParameter(uri: string, parameter: string, value: string | number): string {
  const extraParams = `${parameter}=${value}`;
  let fragment: string | null = null;
  const hashPosition = uri.indexOf('#');
  let base = uri;
  if (hashPosition !== -1) {
    fragment = uri.substring(hashPosition);
    base = uri.substring(0, hashPosition);
  }
  base += base.includes('?') ? '&' : '?';
  base += extraParams;
  if (fragment !== null) {
    base += fragment;
  }
  return base;
}

/**
 * Wiring required by {@link AtmospherePushConnection}: the various services
 * the constructor reaches into, delivered through this callback bundle so the
 * TS class does not need to dispatch through the Java {@code Registry}
 * facade. All lookups go through getters because some of the services (notably
 * {@code PushConfiguration} and {@code ConnectionStateHandler}) are created
 * by the same {@code DefaultRegistry} routine that creates the push
 * connection, and only finished initialising once we actually open a socket.
 */
export interface AtmospherePushConnectionCallbacks {
  getUiLifecycle(): UILifecycleLike;
  getPushConfiguration(): PushConfigurationLike;
  getApplicationConfiguration(): ApplicationConfigurationLike;
  getURIResolver(): URIResolverLike;
  getMessageHandler(): MessageHandlerLike;
  getConnectionStateHandler(): ConnectionStateHandlerLike;
  getResourceLoader(): ResourceLoaderLike;
}

/**
 * The default {@link PushConnection} implementation, backed by the Atmosphere
 * JavaScript library. Migrated from
 * `com.vaadin.client.communication.AtmospherePushConnection`.
 *
 * Wiring is delivered through {@link AtmospherePushConnectionCallbacks} so the
 * TS class does not reach back through the Java {@code Registry} facade.
 */
export class AtmospherePushConnection implements PushConnection {
  // Static helpers retained on the class so the Java facade can keep its
  // `static native` declarations and the bootstrap-time
  // `JsoConfiguration.getAtmosphereJSVersion()` lookup keeps working.

  /** Checks whether the Atmosphere push JS library is loaded. */
  static isAtmosphereLoaded(): boolean {
    return atmosphere() != null;
  }

  /**
   * Gets the Atmosphere push JS library version.
   * @returns the loaded library version, or null if the library is not loaded
   */
  static getAtmosphereJSVersion(): string | null {
    return atmosphere()?.version ?? null;
  }

  private readonly callbacks: AtmospherePushConnectionCallbacks;
  private state: State = 'CONNECT_PENDING';
  private socket: AtmosphereSocket | null = null;
  private config: AtmosphereSubscribeConfig;
  private pushUri: string | null = null;
  private transport: string | null = null;
  // Disconnect callback that fires once a connection finishes opening, used
  // when {@code disconnect()} was called while still in CONNECT_PENDING.
  private pendingDisconnectCommand: (() => void) | null = null;
  // The path to use for push requests (without the service-url prefix).
  private readonly url: string;

  constructor(callbacks: AtmospherePushConnectionCallbacks) {
    this.callbacks = callbacks;

    callbacks.getUiLifecycle().addHandler((event) => {
      if (event.getUiLifecycle().isTerminated()) {
        if (this.state === 'DISCONNECT_PENDING' || this.state === 'DISCONNECTED') {
          return;
        }
        this.disconnect(() => {
          // noop -- best-effort cleanup on UI shutdown
        });
      }
    });

    this.config = AtmospherePushConnection.createConfig();
    // Always debug for now (matches the Java implementation).
    this.config.logLevel = 'debug';

    const pushConfig = callbacks.getPushConfiguration();
    pushConfig.getParameters().forEach((value, key) => {
      const lower = value.toLowerCase();
      if (lower === 'true' || lower === 'false') {
        this.config[key] = lower === 'true';
      } else {
        this.config[key] = value;
      }
    });

    const appConfig = callbacks.getApplicationConfiguration();
    let pushServletMapping = pushConfig.getPushServletMapping();
    if (pushServletMapping == null || pushServletMapping.trim().length === 0 || pushServletMapping === '/') {
      // Handle null, empty and "/" mapping using just default push mapping
      // and serviceUrl.
      let url = PUSH_MAPPING;
      let serviceUrl = appConfig.serviceUrl ?? '';
      if (serviceUrl !== '.') {
        if (!serviceUrl.endsWith('/')) {
          serviceUrl += '/';
        }
        url = serviceUrl + url;
      }
      this.url = url;
    } else {
      // Append specific mapping directly to the context-root URL.
      let contextRootUrl = appConfig.contextRootUrl ?? '';
      if (contextRootUrl.endsWith('/') && pushServletMapping.startsWith('/')) {
        pushServletMapping = pushServletMapping.substring(1);
      }
      this.url = contextRootUrl + pushServletMapping + PUSH_MAPPING;
    }

    this.runWhenAtmosphereLoaded(() => {
      // The original code schedules a deferred command; setTimeout(0)
      // achieves the same "run after the current event loop" semantics.
      setTimeout(() => this.connect(), 0);
    });
  }

  private connect(): void {
    const appConfig = this.callbacks.getApplicationConfiguration();
    let pushUrl = this.callbacks.getURIResolver().resolveVaadinUri(this.url) ?? '';
    pushUrl = addGetParameter(pushUrl, REQUEST_TYPE_PARAMETER, REQUEST_TYPE_PUSH);
    pushUrl = addGetParameter(pushUrl, UI_ID_PARAMETER, appConfig.uiId);

    const pushId = this.callbacks.getMessageHandler().getPushId();
    if (pushId != null) {
      pushUrl = addGetParameter(pushUrl, PUSH_ID_PARAMETER, pushId);
    }

    Console.debug('Establishing push connection');
    this.pushUri = pushUrl;
    this.socket = this.doConnect(pushUrl, this.config);
  }

  isActive(): boolean {
    return this.state === 'CONNECT_PENDING' || this.state === 'CONNECTED';
  }

  isBidirectional(): boolean {
    if (this.transport == null) {
      return false;
    }
    if (this.transport !== 'websocket') {
      // If we are not using websockets, we want to send XHRs.
      return false;
    }
    if (this.callbacks.getPushConfiguration().isAlwaysXhrToServer()) {
      // If the user has forced XHR, let's abide.
      return false;
    }
    // For CONNECT_PENDING: still go for websockets because the message will
    // be delayed until the connection is established. Once that happens,
    // bi-directionality is checked again to be sure.
    return true;
  }

  push(message: JsonObject): void {
    if (!this.isBidirectional()) {
      throw new Error('This server to client push connection should not be used to send client to server messages');
    }
    if (this.state === 'CONNECTED') {
      const messageJson = JSON.stringify(message);
      Console.debug(`Sending push (${this.transport ?? ''}) message to server: ${messageJson}`);
      const socket = this.socket;
      if (socket != null) {
        if (this.transport === 'websocket') {
          const fragmented = new FragmentedMessage(messageJson);
          while (fragmented.hasNextFragment()) {
            socket.push(fragmented.getNextFragment());
          }
        } else {
          socket.push(messageJson);
        }
      }
      return;
    }

    if (this.state === 'CONNECT_PENDING') {
      this.callbacks.getConnectionStateHandler().pushNotConnected(message);
      return;
    }

    throw new Error('Can not push after disconnecting');
  }

  disconnect(command: () => void): void {
    switch (this.state) {
      case 'CONNECT_PENDING':
        // Make the connection callback initiate disconnection again.
        this.state = 'DISCONNECT_PENDING';
        this.pendingDisconnectCommand = command;
        return;
      case 'CONNECTED':
        Console.debug('Closing push connection');
        if (this.pushUri != null) {
          atmosphere()?.unsubscribeUrl(this.pushUri);
        }
        this.state = 'DISCONNECTED';
        command();
        return;
      case 'DISCONNECT_PENDING':
      case 'DISCONNECTED':
        throw new Error('Can not disconnect more than once');
    }
  }

  getTransportType(): string | null {
    return this.transport;
  }

  private onConnect(response: AtmosphereResponse): void {
    this.transport = response.transport ?? null;
    switch (this.state) {
      case 'CONNECT_PENDING':
        this.state = 'CONNECTED';
        this.callbacks.getConnectionStateHandler().pushOk(this);
        return;
      case 'DISCONNECT_PENDING':
        // Set state to connected so disconnect closes the connection.
        this.state = 'CONNECTED';
        if (this.pendingDisconnectCommand != null) {
          this.disconnect(this.pendingDisconnectCommand);
        }
        return;
      case 'CONNECTED':
        // IE likes to open the same connection multiple times, ignore.
        return;
      default:
        throw new Error(`Got onOpen event when connection state is ${this.state}. This should never happen.`);
    }
  }

  private onOpen(response: AtmosphereResponse): void {
    Console.debug(`Push connection established using ${response.transport ?? ''}`);
    this.onConnect(response);
  }

  private onReopen(response: AtmosphereResponse): void {
    Console.debug(`Push connection re-established using ${response.transport ?? ''}`);
    this.onConnect(response);
  }

  private onMessage(response: AtmosphereResponse): void {
    const message = response.responseBody ?? '';
    const json = MessageHandler.parseJson(message);
    if (json == null) {
      this.callbacks.getConnectionStateHandler().pushInvalidContent(this, message);
      return;
    }
    Console.debug(`Received push (${this.getTransportType() ?? ''}) message: ${message}`);
    this.callbacks.getMessageHandler().handleMessage(json);
  }

  private onTransportFailure(): void {
    Console.warn(
      `Push connection using primary method (${String(this.config.transport)}) failed. Trying with ${String(
        this.config.fallbackTransport
      )}`
    );
  }

  private onError(response: AtmosphereResponse): void {
    this.state = 'DISCONNECTED';
    this.callbacks.getConnectionStateHandler().pushError(this, response);
  }

  private onClose(response: AtmosphereResponse): void {
    this.state = 'CONNECT_PENDING';
    this.callbacks.getConnectionStateHandler().pushClosed(this, response);
  }

  private onClientTimeout(response: AtmosphereResponse): void {
    this.state = 'DISCONNECTED';
    this.callbacks.getConnectionStateHandler().pushClientTimeout(this, response);
  }

  private onReconnect(): void {
    if (this.state === 'CONNECTED') {
      this.state = 'CONNECT_PENDING';
    }
    this.callbacks.getConnectionStateHandler().pushReconnectPending(this);
  }

  /** Builds the default Atmosphere configuration object. */
  private static createConfig(): AtmosphereSubscribeConfig {
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
      messageDelimiter: String.fromCharCode(MESSAGE_DELIMITER_CODE)
    };
  }

  private doConnect(uri: string, config: AtmosphereSubscribeConfig): AtmosphereSocket | null {
    const atm = atmosphere();
    if (atm == null) {
      return null;
    }
    config.url = uri;
    config.onOpen = (r) => this.onOpen(r as AtmosphereResponse);
    config.onReopen = (r) => this.onReopen(r as AtmosphereResponse);
    config.onMessage = (r) => this.onMessage(r as AtmosphereResponse);
    config.onError = (r) => this.onError(r as AtmosphereResponse);
    config.onTransportFailure = (_reason, _request) => this.onTransportFailure();
    config.onClose = (r) => this.onClose(r as AtmosphereResponse);
    config.onReconnect = (_req, _resp) => this.onReconnect();
    config.onClientTimeout = (r) => this.onClientTimeout(r as AtmosphereResponse);
    config.headers = {
      'X-Vaadin-LastSeenServerSyncId': () => this.callbacks.getMessageHandler().getLastSeenServerSyncId()
    };
    return atm.subscribe(config);
  }

  private runWhenAtmosphereLoaded(command: () => void): void {
    if (AtmospherePushConnection.isAtmosphereLoaded()) {
      command();
      return;
    }
    const pushJs = this.getVersionedPushJs();
    Console.debug(`Loading ${pushJs}`);
    const loader = this.callbacks.getResourceLoader();
    const pushScriptUrl = (this.callbacks.getApplicationConfiguration().serviceUrl ?? '') + pushJs;
    loader.loadScript(
      pushScriptUrl,
      (event) => {
        if (AtmospherePushConnection.isAtmosphereLoaded()) {
          Console.debug(`${pushJs} loaded`);
          command();
        } else {
          // If bootstrap tried to load vaadinPush.js, ResourceLoader
          // assumes it succeeded even if it failed (#11673).
          this.callbacks.getConnectionStateHandler().pushScriptLoadError(event.getResourceData());
        }
      },
      (event) => {
        this.callbacks.getConnectionStateHandler().pushScriptLoadError(event.getResourceData());
      }
    );
  }

  private getVersionedPushJs(): string {
    return this.callbacks.getApplicationConfiguration().productionMode ? VAADIN_PUSH_JS : VAADIN_PUSH_DEBUG_JS;
  }
}
