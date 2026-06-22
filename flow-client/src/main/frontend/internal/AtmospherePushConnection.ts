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
