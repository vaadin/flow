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

import { BrowserInfo } from '../BrowserInfo';
import { Console } from '../Console';
import { Profiler } from '../Profiler';
import { MessageHandler } from './MessageHandler';
import { XhrConnectionError } from './XhrConnectionError';

// Mirrors ApplicationConstants. Strings are inlined so the TS module doesn't
// need to take a flow-shared bridge dependency.
const REQUEST_TYPE_PARAMETER = 'v-r';
const REQUEST_TYPE_UIDL = 'uidl';
const UI_ID_PARAMETER = 'v-uiId';
// Mirrors JsonConstants.
const JSON_CONTENT_TYPE = 'application/json; charset=UTF-8';

type JsonObject = Record<string, unknown>;

/** Minimal JSON value shape used by this module. */
interface MessageHandlerLike {
  handleMessage(json: JsonObject): void;
}

/** Minimal ConnectionStateHandler shape used by this module. */
interface ConnectionStateHandlerLike {
  xhrInvalidStatusCode(error: XhrConnectionError): void;
  xhrException(error: XhrConnectionError): void;
  xhrInvalidContent(error: XhrConnectionError): void;
  xhrOk(): void;
}

/** Minimal RequestResponseTracker shape used by this module. */
interface RequestResponseTrackerLike {
  addResponseHandlingEndedHandler(handler: () => void): unknown;
}

/** Minimal ApplicationConfiguration shape used by this module. */
interface ApplicationConfigurationLike {
  serviceUrl: string | null;
  uiId: number;
}

/**
 * Wiring required by {@link XhrConnection}: dependencies that need lazy
 * resolution (sibling MessageHandler, the connection-state handler) and
 * direct service references for everything that is already constructed by
 * the time XhrConnection is created.
 */
export interface XhrConnectionCallbacks {
  getMessageHandler(): MessageHandlerLike;
  getConnectionStateHandler(): ConnectionStateHandlerLike;
  getRequestResponseTracker(): RequestResponseTrackerLike;
  getApplicationConfiguration(): ApplicationConfigurationLike;
}

/**
 * Adds the given get parameter to the URI and returns the new URI.
 * Mirrors {@code com.vaadin.flow.shared.util.SharedUtil.addGetParameter}.
 */
function addGetParameter(uri: string, parameter: string, value: string | number): string {
  const extraParams = `${parameter}=${value}`;
  // RFC 3986: query starts at first '?', terminated by '#' or end of URI.
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

interface XhrPostCallback {
  onSuccess(xhr: XMLHttpRequest): void;
  onFail(xhr: XMLHttpRequest, error: Error | null): void;
}

/**
 * Internal XHR POST helper modelled on the vendored GWT
 * {@code com.vaadin.client.gwt.elemental.js.util.Xhr.post}. Creates a new
 * XMLHttpRequest, dispatches success/failure to the given callback, and
 * returns the request so the caller can poke at it (e.g. {@link resendRequest}
 * for the WebKit retry workaround).
 */
function postXhr(url: string, requestData: string, contentType: string, callback: XhrPostCallback): XMLHttpRequest {
  const xhr = new XMLHttpRequest();
  const handleReadyStateChange = (): void => {
    if (xhr.readyState !== XMLHttpRequest.DONE) {
      return;
    }
    // Clear handler so the listener can be garbage-collected.
    xhr.onreadystatechange = null;
    if (xhr.status === 200) {
      callback.onSuccess(xhr);
    } else {
      callback.onFail(xhr, null);
    }
  };
  try {
    xhr.onreadystatechange = handleReadyStateChange;
    xhr.open('POST', url);
    xhr.setRequestHeader('Content-type', contentType);
    xhr.withCredentials = true;
    xhr.send(requestData);
  } catch (e) {
    Console.error(e);
    const error = e instanceof Error ? e : new Error(String(e));
    callback.onFail(xhr, error);
    xhr.onreadystatechange = null;
  }
  return xhr;
}

/**
 * Provides a connection to the UIDL request handler on the server. Migrated
 * from `com.vaadin.client.communication.XhrConnection`.
 *
 * Wiring (the connection-state handler, sibling MessageHandler, the
 * request/response tracker, the application configuration) is delivered
 * through {@link XhrConnectionCallbacks} so the TS class does not reach back
 * through the Java {@code Registry} facade.
 */
export class XhrConnection {
  /**
   * Webkit will ignore outgoing requests while waiting for a response to a
   * navigation event (indicated by a {@code beforeunload} event). When this
   * happens, the request is re-sent periodically until either the response
   * arrives or {@code send()} throws because the request is actually running.
   */
  private webkitMaybeIgnoringRequests = false;

  private readonly callbacks: XhrConnectionCallbacks;

  constructor(callbacks: XhrConnectionCallbacks) {
    this.callbacks = callbacks;
    window.addEventListener(
      'beforeunload',
      () => {
        this.webkitMaybeIgnoringRequests = true;
      },
      false
    );
    callbacks.getRequestResponseTracker().addResponseHandlingEndedHandler(() => {
      this.webkitMaybeIgnoringRequests = false;
    });
  }

  /**
   * Sends an asynchronous UIDL request to the server.
   */
  send(payload: JsonObject): void {
    const requestStartTime = Profiler.getRelativeTimeMillis();

    const callback: XhrPostCallback = {
      onSuccess: (xhr: XMLHttpRequest): void => {
        Console.debug(`Server visit took ${Profiler.getRelativeTimeString(requestStartTime)}ms`);
        const responseText = xhr.responseText;
        const json = MessageHandler.parseJson(responseText);
        if (json == null) {
          // Invalid JSON string
          const error = new XhrConnectionError(xhr, payload, null);
          this.callbacks.getConnectionStateHandler().xhrInvalidContent(error);
          return;
        }
        this.callbacks.getConnectionStateHandler().xhrOk();
        Console.debug(`Received xhr message: ${responseText}`);
        this.callbacks.getMessageHandler().handleMessage(json);
      },
      onFail: (xhr: XMLHttpRequest, error: Error | null): void => {
        const errorEvent = new XhrConnectionError(xhr, payload, error);
        if (error == null) {
          // Response other than 200
          this.callbacks.getConnectionStateHandler().xhrInvalidStatusCode(errorEvent);
        } else {
          this.callbacks.getConnectionStateHandler().xhrException(errorEvent);
        }
      }
    };

    const payloadJson = JSON.stringify(payload);
    const xhr = postXhr(this.getUri(), payloadJson, JSON_CONTENT_TYPE, callback);

    Console.debug(`Sending xhr message to server: ${payloadJson}`);

    if (this.webkitMaybeIgnoringRequests && BrowserInfo.get().isWebkit()) {
      const retryTimeout = 250;
      const tick = (): void => {
        // Resend the request through the native send() while still in the
        // "opened" readyState; if send() throws or the readyState has moved
        // on we stop retrying.
        if (XhrConnection.resendRequest(xhr) && this.webkitMaybeIgnoringRequests) {
          setTimeout(tick, retryTimeout);
        }
      };
      setTimeout(tick, retryTimeout);
    }
  }

  /**
   * Retrieves the URI to use when sending RPCs to the server.
   */
  getUri(): string {
    const config = this.callbacks.getApplicationConfiguration();
    return addGetParameter(
      addGetParameter(config.serviceUrl ?? '', REQUEST_TYPE_PARAMETER, REQUEST_TYPE_UIDL),
      UI_ID_PARAMETER,
      config.uiId
    );
  }

  /**
   * Resends a pending XHR that was blocked by the WebKit beforeunload pause.
   * Returns true if the resend was issued (and may need to be repeated),
   * false if the request has moved past readyState 1 or the underlying send
   * threw because the original request is now actually running.
   */
  static resendRequest(xhr: XMLHttpRequest): boolean {
    if (xhr.readyState !== 1) {
      // Progressed to some other readyState -> no longer blocked
      return false;
    }
    try {
      xhr.send();
      return true;
    } catch {
      // send throws if it is actually running for real
      return false;
    }
  }
}
