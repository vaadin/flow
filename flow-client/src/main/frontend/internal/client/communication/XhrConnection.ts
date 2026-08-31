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

import { stringify } from '../WidgetUtil';
import { Console } from '../Console';
import type { ValueMap } from './MessageOrdering';
import type { ApplicationConfiguration } from '../ApplicationConfiguration';
import type { ConnectionStateHandler } from './ConnectionStateHandler';
import type { MessageHandler } from './MessageHandler';
import type { RequestResponseTracker } from './RequestResponseTracker';
import { BrowserInfo } from '../BrowserInfo';
import { XhrConnectionError } from './XhrConnectionError';
import { parseJson } from './MessageHandler';
import { addGetParameter } from '../../flow/shared/util/SharedUtil';

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
export function resendRequest(xhr: XMLHttpRequest): boolean {
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

/** The slice of Registry that XhrConnection uses. */
export interface XhrConnectionRegistry {
  getRequestResponseTracker(): Pick<RequestResponseTracker, 'addResponseHandlingEndedHandler'>;
  getConnectionStateHandler(): Pick<
    ConnectionStateHandler,
    'xhrInvalidStatusCode' | 'xhrException' | 'xhrInvalidContent' | 'xhrOk'
  >;
  getMessageHandler(): Pick<MessageHandler, 'handleMessage'>;
  getApplicationConfiguration(): Pick<ApplicationConfiguration, 'getServiceUrl' | 'getUIId'>;
}

/** Sends UIDL requests to the server over XHR; mirrors XhrConnection.java. */
export class XhrConnection {
  // Webkit ignores outgoing requests while waiting for a navigation response
  // (beforeunload); when set, retry sending until there is a response.
  #webkitMaybeIgnoringRequests = false;

  readonly #registry: XhrConnectionRegistry;

  constructor(registry: XhrConnectionRegistry) {
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
   * Sends an asynchronous UIDL request to the server using the given URI.
   *
   * @param payload - The URI to use for the request. May includes GET parameters
   */
  send(payload: Payload): void {
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
          this.onResponseSuccess(xhr, payload);
          xhr.onreadystatechange = null;
          return;
        }
        this.onResponseFail(xhr, payload, null);
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
      this.onResponseFail(xhr, payload, error as Error);
      xhr.onreadystatechange = null;
    }

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

  /** Routes a successful response to the MessageHandler (or invalid-content failure). */
  onResponseSuccess(xhr: XMLHttpRequest, payload: Payload): void {
    const json = parseJson(xhr.responseText);
    if (json === null) {
      this.#registry.getConnectionStateHandler().xhrInvalidContent(new XhrConnectionError(xhr, payload, null));
      return;
    }
    this.#registry.getConnectionStateHandler().xhrOk();
    this.#registry.getMessageHandler().handleMessage(json as ValueMap);
  }

  /** Routes a failed response to the connection-state handler. */
  onResponseFail(xhr: XMLHttpRequest, payload: Payload, error: Error | null): void {
    const errorEvent = new XhrConnectionError(xhr, payload, error);
    if (error === null) {
      // Response other than 200.
      this.#registry.getConnectionStateHandler().xhrInvalidStatusCode(errorEvent);
    } else {
      this.#registry.getConnectionStateHandler().xhrException(errorEvent);
    }
  }

  /**
   * Retrieves the URI to use when sending RPCs to the server
   *
   * @returns The URI to use for server messages.
   */
  getUri(): string {
    const configuration = this.#registry.getApplicationConfiguration();
    return addGetParameter(
      addGetParameter(configuration.getServiceUrl(), REQUEST_TYPE_PARAMETER, REQUEST_TYPE_UIDL),
      UI_ID_PARAMETER,
      configuration.getUIId()
    );
  }
}
