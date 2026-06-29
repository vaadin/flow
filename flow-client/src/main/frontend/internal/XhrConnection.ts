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

// Implementation migrated from XhrConnection.java, registered on
// window.Vaadin.Flow.internal.XhrConnection by registerInternals; the Java
// method delegates here. Also bundled to ES5 for the HtmlUnit used by GwtTests.
//
// The XhrConnection class below is the build-alongside TS port of the rest of
// XhrConnection.java: it sends UIDL requests to the server over XHR and routes
// the response to the MessageHandler or, on failure, to the
// ConnectionStateHandler. The Registry and its members are contracts satisfied
// at cutover. The actual XHR round-trip is integration-validated; getUri and the
// success/failure routing are the unit-tested logic.

import { XhrConnectionError } from './communication/XhrConnectionError';
import { parseJSONResponse } from './MessageHandler';
import { addGetParameter } from './SharedUtil';

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
  getRequestResponseTracker(): { addResponseHandlingEndedHandler(handler: () => void): unknown };
  getConnectionStateHandler(): {
    xhrInvalidStatusCode(error: XhrConnectionError): void;
    xhrException(error: XhrConnectionError): void;
    xhrInvalidContent(error: XhrConnectionError): void;
    xhrOk(): void;
  };
  getMessageHandler(): { handleMessage(json: unknown): void };
  getApplicationConfiguration(): { getServiceUrl(): string; getUIId(): number };
}

// Parses a server response, returning null if it is not valid JSON. Mirrors
// MessageHandler.parseJson.
function parseJson(responseText: string): unknown {
  try {
    return parseJSONResponse(responseText);
  } catch {
    return null;
  }
}

function isWebkit(): boolean {
  return /webkit/i.test(navigator.userAgent);
}

/** Sends UIDL requests to the server over XHR; mirrors XhrConnection.java. */
export class XhrConnection {
  // Webkit ignores outgoing requests while waiting for a navigation response
  // (beforeunload); when set, retry sending until there is a response.
  private webkitMaybeIgnoringRequests = false;

  private readonly registry: XhrConnectionRegistry;

  constructor(registry: XhrConnectionRegistry) {
    this.registry = registry;
    window.addEventListener(
      'beforeunload',
      () => {
        this.webkitMaybeIgnoringRequests = true;
      },
      false
    );
    this.registry.getRequestResponseTracker().addResponseHandlingEndedHandler(() => {
      this.webkitMaybeIgnoringRequests = false;
    });
  }

  /** Sends an asynchronous UIDL request to the server. */
  send(payload: Payload): void {
    const payloadJson = JSON.stringify(payload);
    const xhr = new XMLHttpRequest();
    xhr.open('POST', this.getUri(), true);
    // Mirror Java's Xhr.post, which always sends credentials so cross-origin/CORS
    // deployments keep their cookies and authentication headers.
    xhr.withCredentials = true;
    xhr.setRequestHeader('Content-Type', JSON_CONTENT_TYPE);
    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        if (xhr.status === 200) {
          this.onResponseSuccess(xhr, payload);
        } else {
          this.onResponseFail(xhr, payload, null);
        }
      }
    };
    xhr.onerror = () => this.onResponseFail(xhr, payload, new Error('XHR request failed'));
    xhr.send(payloadJson);

    if (this.webkitMaybeIgnoringRequests && isWebkit()) {
      const retryTimeout = 250;
      const retry = (): void => {
        if (resendRequest(xhr) && this.webkitMaybeIgnoringRequests) {
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
      this.registry.getConnectionStateHandler().xhrInvalidContent(new XhrConnectionError(xhr, payload, null));
      return;
    }
    this.registry.getConnectionStateHandler().xhrOk();
    this.registry.getMessageHandler().handleMessage(json);
  }

  /** Routes a failed response to the connection-state handler. */
  onResponseFail(xhr: XMLHttpRequest, payload: Payload, error: Error | null): void {
    const errorEvent = new XhrConnectionError(xhr, payload, error);
    if (error === null) {
      // Response other than 200.
      this.registry.getConnectionStateHandler().xhrInvalidStatusCode(errorEvent);
    } else {
      this.registry.getConnectionStateHandler().xhrException(errorEvent);
    }
  }

  /** The URI to use when sending RPCs to the server. */
  getUri(): string {
    const configuration = this.registry.getApplicationConfiguration();
    return addGetParameter(
      addGetParameter(configuration.getServiceUrl(), REQUEST_TYPE_PARAMETER, REQUEST_TYPE_UIDL),
      UI_ID_PARAMETER,
      configuration.getUIId()
    );
  }
}
