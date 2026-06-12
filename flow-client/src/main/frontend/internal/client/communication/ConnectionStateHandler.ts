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

import type { XhrConnectionError } from './XhrConnectionError';

/**
 * Minimal shape for a push connection used by the connection state handler.
 * Matches the Java {@code PushConnection} interface and the
 * {@code AtmospherePushConnection} implementation.
 */
export interface PushConnectionLike {
  isBidirectional(): boolean;
  getTransportType(): string | null;
}

/**
 * A string that, if found in a non-JSON response to a UIDL request, will cause
 * the browser to refresh the page. Mirrors the Java {@code UIDL_REFRESH_TOKEN}
 * constant.
 */
export const UIDL_REFRESH_TOKEN = 'Vaadin-Refresh';

/**
 * Handles problems and other events which occur during communication with the
 * server. Migrated from {@code com.vaadin.client.communication.ConnectionStateHandler}.
 *
 * The default implementation is {@link DefaultConnectionStateHandler}.
 */
export interface ConnectionStateHandler {
  /** Called when an exception occurs during a {@link Heartbeat} request. */
  heartbeatException(request: XMLHttpRequest, message: string): void;

  /** Called when a heartbeat request returns a non-OK status code. */
  heartbeatInvalidStatusCode(xhr: XMLHttpRequest): void;

  /** Called when a heartbeat request succeeds. */
  heartbeatOk(): void;

  /** Called when the push connection to the server is closed. */
  pushClosed(pushConnection: PushConnectionLike, responseObject: unknown): void;

  /**
   * Called when a client side timeout occurs before a push connection to the
   * server completes.
   */
  pushClientTimeout(pushConnection: PushConnectionLike, response: unknown): void;

  /** Called when a fatal error occurs in the push connection. */
  pushError(pushConnection: PushConnectionLike, response: unknown): void;

  /**
   * Called when the push connection has lost the connection to the server and
   * will proceed to try to re-establish the connection.
   */
  pushReconnectPending(pushConnection: PushConnectionLike): void;

  /** Called when the push connection to the server has been established. */
  pushOk(pushConnection: PushConnectionLike): void;

  /** Called when the required push script could not be loaded. */
  pushScriptLoadError(resourceUrl: string): void;

  /** Called when an exception occurs during an XmlHttpRequest. */
  xhrException(xhrConnectionError: XhrConnectionError): void;

  /** Called when invalid content (not JSON) was returned from the server. */
  xhrInvalidContent(xhrConnectionError: XhrConnectionError): void;

  /** Called when an invalid status code was returned by the server. */
  xhrInvalidStatusCode(xhrConnectionError: XhrConnectionError): void;

  /** Called whenever an XmlHttpRequest to the server completes successfully. */
  xhrOk(): void;

  /**
   * Called when a message is to be sent to the server through the push channel
   * but the push channel is not connected.
   */
  pushNotConnected(payload: unknown): void;

  /**
   * Called when invalid content (not JSON) was pushed from the server through
   * the push connection.
   */
  pushInvalidContent(pushConnection: PushConnectionLike, message: string): void;

  /** Called when some part of the reconnect dialog configuration has been changed. */
  configurationUpdated(): void;
}
