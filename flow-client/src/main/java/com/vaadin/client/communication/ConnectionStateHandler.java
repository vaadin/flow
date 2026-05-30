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
package com.vaadin.client.communication;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

import com.google.gwt.xhr.client.XMLHttpRequest;

import elemental.json.JsonObject;

/**
 * Handles problems and other events which occur during communication with the
 * server. The default implementation is {@link DefaultConnectionStateHandler},
 * implemented in TypeScript.
 *
 * <p>
 * Declared as {@code @JsType} so the method names are preserved when Java
 * callers (XhrConnection, AtmospherePushConnection, Heartbeat callbacks, etc.)
 * invoke them on the TS-side implementation.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.communication", name = "ConnectionStateHandler")
public interface ConnectionStateHandler {

    /**
     * A string that, if found in a non-JSON response to a UIDL request, will
     * cause the browser to refresh the page. If followed by a colon, optional
     * whitespace, and a URI, causes the browser to synchronously load the URI.
     *
     * <p>
     * This allows, for instance, a servlet filter to redirect the application
     * to a custom login page when the session expires.
     */
    @JsOverlay
    String UIDL_REFRESH_TOKEN = "Vaadin-Refresh";

    /**
     * Called when an exception occurs during a {@link Heartbeat} request.
     * Exception detail is delivered as a string because the JS-side TS class
     * has no use for the underlying Java {@code Exception} object.
     */
    void heartbeatException(XMLHttpRequest request, String message);

    /** Called when a heartbeat request returns a non-OK status code. */
    void heartbeatInvalidStatusCode(XMLHttpRequest xhr);

    /** Called when a {@link Heartbeat} request succeeds. */
    void heartbeatOk();

    /** Called when the push connection to the server is closed. */
    void pushClosed(PushConnection pushConnection, Object responseObject);

    /**
     * Called when a client side timeout occurs before a push connection to the
     * server completes.
     */
    void pushClientTimeout(PushConnection pushConnection, Object response);

    /** Called when a fatal error occurs in the push connection. */
    void pushError(PushConnection pushConnection, Object response);

    /**
     * Called when the push connection has lost the connection to the server and
     * will proceed to try to re-establish the connection.
     */
    void pushReconnectPending(PushConnection pushConnection);

    /** Called when the push connection to the server has been established. */
    void pushOk(PushConnection pushConnection);

    /** Called when the required push script could not be loaded. */
    void pushScriptLoadError(String resourceUrl);

    /** Called when an exception occurs during an XmlHttpRequest. */
    void xhrException(XhrConnectionError xhrConnectionError);

    /** Called when invalid content (not JSON) was returned from the server. */
    void xhrInvalidContent(XhrConnectionError xhrConnectionError);

    /**
     * Called when an invalid status code (not 200) was returned by the server.
     */
    void xhrInvalidStatusCode(XhrConnectionError xhrConnectionError);

    /**
     * Called whenever an XmlHttpRequest to the server completes successfully.
     */
    void xhrOk();

    /**
     * Called when a message is to be sent to the server through the push
     * channel but the push channel is not connected.
     */
    void pushNotConnected(JsonObject payload);

    /**
     * Called when invalid content (not JSON) was pushed from the server through
     * the push connection.
     */
    void pushInvalidContent(PushConnection pushConnection, String message);

    /** Called when some part of the reconnect dialog configuration changed. */
    void configurationUpdated();
}
