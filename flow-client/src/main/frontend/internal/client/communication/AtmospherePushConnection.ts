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

interface AtmosphereSocket {
  push(message: string): void;
}

interface AtmosphereLibrary {
  subscribe(config: AtmosphereSubscribeConfig): AtmosphereSocket;
  unsubscribeUrl(url: string): void;
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

export interface AtmosphereConnectCallbacks {
  onOpen: (response: unknown) => void;
  onReopen: (response: unknown) => void;
  onMessage: (response: unknown) => void;
  onError: (response: unknown) => void;
  onTransportFailure: (reason: unknown) => void;
  onClose: (response: unknown) => void;
  onReconnect: (request: unknown, response: unknown) => void;
  onClientTimeout: (request: unknown) => void;
  getLastSeenServerSyncId: () => unknown;
}

function atmosphere(): AtmosphereLibrary | undefined {
  return (globalThis as unknown as VaadinPushGlobal).vaadinPush?.atmosphere;
}

/**
 * Browser-touching helpers from
 * `com.vaadin.client.communication.AtmospherePushConnection`. Reached from
 * GWT-compiled code via the `NativeAtmospherePushConnection` JsType shim. The
 * `AtmosphereConfiguration` JSO subclass (with its `getStringValue` /
 * `setStringValue` / ... property accessors) stays Java — methods *are* on a
 * JS instance and don't fit a separate TS module.
 */
export const AtmospherePushConnection = {
  createConfig(messageDelimiter: number): AtmosphereSubscribeConfig {
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
  },

  doConnect(
    uri: string,
    config: AtmosphereSubscribeConfig,
    callbacks: AtmosphereConnectCallbacks
  ): AtmosphereSocket | null {
    const atm = atmosphere();
    if (!atm) {
      return null;
    }
    config.url = uri;
    config.onOpen = (r) => callbacks.onOpen(r);
    config.onReopen = (r) => callbacks.onReopen(r);
    config.onMessage = (r) => callbacks.onMessage(r);
    config.onError = (r) => callbacks.onError(r);
    config.onTransportFailure = (reason, _request) => callbacks.onTransportFailure(reason);
    config.onClose = (r) => callbacks.onClose(r);
    config.onReconnect = (req, resp) => callbacks.onReconnect(req, resp);
    config.onClientTimeout = (req) => callbacks.onClientTimeout(req);
    config.headers = {
      'X-Vaadin-LastSeenServerSyncId': () => callbacks.getLastSeenServerSyncId()
    };
    return atm.subscribe(config);
  },

  doPush(socket: AtmosphereSocket, message: string): void {
    socket.push(message);
  },

  doDisconnect(url: string): void {
    atmosphere()?.unsubscribeUrl(url);
  },

  isAtmosphereLoaded(): boolean {
    return atmosphere() != null;
  },

  getAtmosphereJSVersion(): string | null {
    return (atmosphere() as { version?: string } | null)?.version ?? null;
  }
};
