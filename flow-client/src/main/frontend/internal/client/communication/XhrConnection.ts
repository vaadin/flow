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

// TypeScript port of XhrConnection.java. The XhrConnection class below sends UIDL
// requests to the server over XHR and routes the response to the MessageHandler
// or, on failure, to the ConnectionStateHandler.

import type { Registry } from '../Registry';
import { stringify } from '../WidgetUtil';
import { Console } from '../Console';
import type { ConnectionStateHandler } from './ConnectionStateHandler';
import type { MessageHandler } from './MessageHandler';
import { BrowserInfo } from '../BrowserInfo';
import { XhrConnectionError } from './XhrConnectionError';
import { parseJson } from './MessageHandler';
import { addGetParameter } from '../../flow/shared/util/SharedUtil';
import { getRelativeTimeMillis, getRelativeTimeString } from '../Profiler';

// com.vaadin.flow.shared.ApplicationConstants / JsonConstants
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_UIDL = 'uidl';
const UI_ID_PARAMETER = 'v-uiId';
const JSON_CONTENT_TYPE = 'application/json; charset=UTF-8';

/**
 * Attempts to resend a request that is still in its initial (OPENED, readyState
 * 1) state. Returns true if the request was still blocked and got re-sent, or
 * false if it had already progressed or send() threw (it is running for real).
 */
function resendRequest(xhr: XMLHttpRequest): boolean {
  if (xhr.readyState !== 1) {
    // Progressed to some other readyState -> no longer blocked
    return false;
  }
  try {
    xhr.send();
    return true;
  } catch {
    // send throws if it is running for real
    return false;
  }
}

type Payload = Record<string, unknown>;

/**
 * Handles the response from the server by forwarding the received message to
 * {@link MessageHandler} or failures to the appropriate method in
 * {@link ConnectionStateHandler}.
 */
export class XhrResponseHandler {
  readonly #registry: Registry;

  #payload: Payload | null = null;

  #requestStartTime = 0;

  /**
   * Creates a new instance connected to the given registry.
   *
   * Java declares this handler as an inner class, so it reads the connection's
   * registry field; TypeScript has no inner classes, so the registry is passed
   * in and the Java constructor's no-arg signature cannot be mirrored.
   *
   * @param registry - the global registry
   */
  constructor(registry: Registry) {
    this.#registry = registry;
  }

  /**
   * Sets the payload which was sent to the server.
   *
   * @param payload - the payload which was sent to the server
   */
  setPayload(payload: Payload): void {
    this.#payload = payload;
  }

  /**
   * Sets the relative time (see {@link getRelativeTimeMillis}) when the request
   * was sent.
   *
   * @param requestStartTime - the relative time when the request was sent
   */
  setRequestStartTime(requestStartTime: number): void {
    this.#requestStartTime = requestStartTime;
  }

  /**
   * Reports a failed request to the connection-state handler.
   *
   * @param xhr - the request that failed
   * @param error - the exception that a synchronous failure threw, or `null` for
   *          a response other than 200
   */
  onFail(xhr: XMLHttpRequest, error: Error | null): void {
    // Java passes the field, and documents the payload as never null: send()
    // always sets it before the request goes out.
    const errorEvent = new XhrConnectionError(xhr, this.#payload!, error);
    if (error === null) {
      // Response other than 200
      this.#registry.getConnectionStateHandler().xhrInvalidStatusCode(errorEvent);
      return;
    }
    this.#registry.getConnectionStateHandler().xhrException(errorEvent);
  }

  /**
   * Routes a successful response to the message handler, or reports invalid
   * content when it does not parse.
   *
   * @param xhr - the request that succeeded
   */
  onSuccess(xhr: XMLHttpRequest): void {
    Console.debug(`Server visit took ${getRelativeTimeString(this.#requestStartTime)}ms`);

    const responseText = xhr.responseText;

    const json = parseJson(responseText);
    if (json === null) {
      // Invalid JSON string
      this.#registry.getConnectionStateHandler().xhrInvalidContent(new XhrConnectionError(xhr, this.#payload!, null));
      return;
    }

    this.#registry.getConnectionStateHandler().xhrOk();
    Console.debug(`Received xhr message: ${responseText}`);
    this.#registry.getMessageHandler().handleMessage(json);
  }
}

/**
 * Provides a connection to the UIDL request handler on the server and knows how
 * to send messages to that end point.
 */
export class XhrConnection {
  // Webkit ignores outgoing requests while waiting for a navigation response
  // (beforeunload); when set, retry sending until there is a response.
  #webkitMaybeIgnoringRequests = false;

  readonly #registry: Registry;

  constructor(registry: Registry) {
    this.#registry = registry;
    window.addEventListener(
      'beforeunload',
      () => {
        this.#webkitMaybeIgnoringRequests = true;
      },
      false
    );
    this.#registry.getRequestResponseTracker().addResponseHandlingEndedHandler(() => {
      this.#webkitMaybeIgnoringRequests = false;
    });
  }

  /**
   * Creates the handler that routes this connection's responses.
   *
   * @returns the response handler
   */
  protected createResponseHandler(): XhrResponseHandler {
    return new XhrResponseHandler(this.#registry);
  }

  /**
   * Sends an asynchronous UIDL request to the server using the given URI.
   *
   * @param payload - The URI to use for the request. May includes GET parameters
   */
  send(payload: Payload): void {
    const responseHandler = this.createResponseHandler();
    responseHandler.setPayload(payload);
    responseHandler.setRequestStartTime(getRelativeTimeMillis());

    const payloadJson = stringify(payload);
    const xhr = new XMLHttpRequest();
    // Mirrors Xhr.request and its Handler: the ready-state handler is the only
    // asynchronous failure path and reports a null exception, and it is cleared
    // once it has fired (clearOnReadyStateChange). A non-null exception comes
    // only from a synchronous throw in open()/send(), hence the catch below —
    // listening for the "error" event instead would report a network failure
    // twice, because that event follows the DONE ready-state change.
    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        if (xhr.status === 200) {
          responseHandler.onSuccess(xhr);
          xhr.onreadystatechange = null;
          return;
        }
        responseHandler.onFail(xhr, null);
        xhr.onreadystatechange = null;
      }
    };
    try {
      xhr.open('POST', this.getUri(), true);
      xhr.setRequestHeader('Content-Type', JSON_CONTENT_TYPE);
      // Java's Xhr always sends credentials so cross-origin/CORS deployments
      // keep their cookies and authentication headers.
      xhr.withCredentials = true;
      xhr.send(payloadJson);
    } catch (error) {
      Console.error(error);
      responseHandler.onFail(xhr, error as Error);
      xhr.onreadystatechange = null;
    }

    Console.debug(`Sending xhr message to server: ${payloadJson}`);

    if (this.#webkitMaybeIgnoringRequests && BrowserInfo.get().isWebkit()) {
      const retryTimeout = 250;
      const retry = (): void => {
        if (resendRequest(xhr) && this.#webkitMaybeIgnoringRequests) {
          setTimeout(retry, retryTimeout);
        }
      };
      setTimeout(retry, retryTimeout);
    }
  }

  /**
   * Retrieves the URI to use when sending RPCs to the server
   *
   * @returns The URI to use for server messages.
   */
  // Protected in Java, where MessageSender.sendUnloadBeacon reads it through
  // package access; TypeScript has no package visibility, so the port makes it
  // public rather than moving the caller.
  getUri(): string {
    const configuration = this.#registry.getApplicationConfiguration();
    return addGetParameter(
      addGetParameter(configuration.getServiceUrl(), REQUEST_TYPE_PARAMETER, REQUEST_TYPE_UIDL),
      UI_ID_PARAMETER,
      configuration.getUIId()
    );
  }
}
