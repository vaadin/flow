/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.xhr.client.XMLHttpRequest;

import elemental.json.JsonObject;

/**
 * Handles problems and other events which occur during communication with the
 * server.
 *
 * The handler is responsible for handling any problem in XHR, heartbeat and
 * push connections in a way it sees fit. The default implementation is
 * {@link DefaultConnectionStateHandler}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ConnectionStateHandler {

    /**
     * A string that, if found in a non-JSON response to a UIDL request, will
     * cause the browser to refresh the page. If followed by a colon, optional
     * whitespace, and a URI, causes the browser to synchronously load the URI.
     *
     * <p>
     * This allows, for instance, a servlet filter to redirect the application
     * to a custom login page when the session expires. For example:
     * </p>
     *
     * <pre>
     * if (sessionExpired) {
     *     response.setHeader(&quot;Content-Type&quot;, &quot;text/html&quot;);
     *     response.getWriter().write(myLoginPageHtml + &quot;&lt;!-- Vaadin-Refresh: &quot;
     *             + request.getContextPath() + &quot; --&gt;&quot;);
     * }
     * </pre>
     */
    String UIDL_REFRESH_TOKEN = "Vaadin-Refresh";

    /**
     * Called when an exception occurs during a {@link Heartbeat} request.
     *
     * @param request
     *            The heartbeat request
     * @param exception
     *            The exception which occurred
     */
    void heartbeatException(XMLHttpRequest request, Exception exception);

    /**
     * Called when a heartbeat request returns a status code other than OK
     * (200).
     *
     * @param xhr
     *            the heartbeat request
     */
    void heartbeatInvalidStatusCode(XMLHttpRequest xhr);

    /**
     * Called when a {@link Heartbeat} request succeeds.
     */
    void heartbeatOk();

    /**
     * Called when the push connection to the server is closed. This might
     * result in the push connection trying a fallback connection method, trying
     * to reconnect to the server or might just be an indication that the
     * connection was intentionally closed ("unsubscribe"),
     *
     * @param pushConnection
     *            The push connection which was closed
     * @param responseObject
     *            An object containing response data
     */
    void pushClosed(PushConnection pushConnection,
            JavaScriptObject responseObject);

    /**
     * Called when a client side timeout occurs before a push connection to the
     * server completes.
     *
     * The client side timeout causes a disconnection of the push connection and
     * no reconnect will be attempted after this method is called,
     *
     * @param pushConnection
     *            The push connection which timed out
     * @param response
     *            An object containing response data
     */
    void pushClientTimeout(PushConnection pushConnection,
            JavaScriptObject response);

    /**
     * Called when a fatal error fatal error occurs in the push connection.
     *
     * The push connection will not try to recover from this situation itself
     * and typically the problem handler should not try to do automatic recovery
     * either. The cause can be e.g. maximum number of reconnection attempts
     * have been reached, neither the selected transport nor the fallback
     * transport can be used or similar.
     *
     * @param pushConnection
     *            The push connection where the error occurred
     * @param response
     *            An object containing response data
     */
    void pushError(PushConnection pushConnection, JavaScriptObject response);

    /**
     * Called when the push connection has lost the connection to the server and
     * will proceed to try to re-establish the connection.
     *
     * @param pushConnection
     *            The push connection which will be reconnected
     */
    void pushReconnectPending(PushConnection pushConnection);

    /**
     * Called when the push connection to the server has been established.
     *
     * @param pushConnection
     *            The push connection which was established
     */
    void pushOk(PushConnection pushConnection);

    /**
     * Called when the required push script could not be loaded.
     *
     * @param resourceUrl
     *            The URL which was used for loading the script
     */
    void pushScriptLoadError(String resourceUrl);

    /**
     * Called when an exception occurs during an XmlHttpRequest request to the
     * server.
     *
     * @param xhrConnectionError
     *            An event containing what was being sent to the server and what
     *            exception occurred
     */
    void xhrException(XhrConnectionError xhrConnectionError);

    /**
     * Called when invalid content (not JSON) was returned from the server as
     * the result of an XmlHttpRequest request.
     *
     * @param xhrConnectionError
     *            An event containing what was being sent to the server and what
     *            was returned
     */
    void xhrInvalidContent(XhrConnectionError xhrConnectionError);

    /**
     * Called when invalid status code (not 200) was returned by the server as
     * the result of an XmlHttpRequest.
     *
     * @param xhrConnectionError
     *            An event containing what was being sent to the server and what
     *            was returned
     */
    void xhrInvalidStatusCode(XhrConnectionError xhrConnectionError);

    /**
     * Called whenever a XmlHttpRequest to the server completes successfully.
     */
    void xhrOk();

    /**
     * Called when a message is to be sent to the server through the push
     * channel but the push channel is not connected.
     *
     * @param payload
     *            The payload to send to the server
     */
    void pushNotConnected(JsonObject payload);

    /**
     * Called when invalid content (not JSON) was pushed from the server through
     * the push connection.
     *
     * @param pushConnection
     *            the push connection which was used
     * @param message
     *            the received message
     */
    void pushInvalidContent(PushConnection pushConnection, String message);

    /**
     * Called when some part of the reconnect dialog configuration has been
     * changed.
     */
    void configurationUpdated();

}
