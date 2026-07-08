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

// TypeScript port of the com.vaadin.client.communication.ConnectionStateHandler
// interface. It is notified of heartbeat / XHR / push successes and failures and
// of reconnect-configuration changes, and drives the reconnection UI/logic
// (DefaultConnectionStateHandler implements it). Consolidates the contract
// previously inlined across Heartbeat, XhrConnection and MessageSender.

import type { PushConnection } from './PushConnection';
import type { XhrConnectionError } from './XhrConnectionError';

/** Reacts to connection successes/failures and reconnect-config changes. */
export interface ConnectionStateHandler {
  /** A heartbeat request failed with an exception. */
  heartbeatException(request: XMLHttpRequest, exception: Error): void;

  /** A heartbeat request returned a non-OK status code. */
  heartbeatInvalidStatusCode(xhr: XMLHttpRequest): void;

  /** A heartbeat request succeeded. */
  heartbeatOk(): void;

  /** A push connection was closed. */
  pushClosed(pushConnection: PushConnection, responseObject: unknown): void;

  /** A client-side timeout occurred before the push connection completed. */
  pushClientTimeout(pushConnection: PushConnection, response: unknown): void;

  /** A push connection error occurred. */
  pushError(pushConnection: PushConnection, response: unknown): void;

  /** A push reconnection is pending. */
  pushReconnectPending(pushConnection: PushConnection): void;

  /** A push connection was (re)established successfully. */
  pushOk(pushConnection: PushConnection): void;

  /** The push script failed to load from the given URL. */
  pushScriptLoadError(resourceUrl: string): void;

  /** An XHR request failed with an exception. */
  xhrException(xhrConnectionError: XhrConnectionError): void;

  /** An XHR response could not be parsed as valid content. */
  xhrInvalidContent(xhrConnectionError: XhrConnectionError): void;

  /** An XHR request returned a non-OK status code. */
  xhrInvalidStatusCode(xhrConnectionError: XhrConnectionError): void;

  /** An XHR request succeeded. */
  xhrOk(): void;

  /** A message could not be pushed because push is not connected. */
  pushNotConnected(payload: Record<string, unknown>): void;

  /** A push message could not be parsed as valid content. */
  pushInvalidContent(pushConnection: PushConnection, message: string): void;

  /** The reconnect configuration changed. */
  configurationUpdated(): void;
}
