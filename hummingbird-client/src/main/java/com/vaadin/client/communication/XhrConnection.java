/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import com.google.gwt.user.client.Timer;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ApplicationConnection.CommunicationHandler;
import com.vaadin.client.ApplicationConnection.RequestStartingEvent;
import com.vaadin.client.ApplicationConnection.ResponseHandlingEndedEvent;
import com.vaadin.client.ApplicationConnection.ResponseHandlingStartedEvent;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.Console;
import com.vaadin.client.Profiler;
import com.vaadin.client.ValueMap;
import com.vaadin.client.gwt.elemental.js.util.Xhr;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.JsonConstants;
import com.vaadin.shared.util.SharedUtil;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.json.JsonObject;

/**
 * Provides a connection to the /UIDL url on the server and knows how to send
 * messages to that end point
 *
 * @since 7.6
 * @author Vaadin Ltd
 */
public class XhrConnection {

    private ApplicationConnection connection;

    /**
     * Webkit will ignore outgoing requests while waiting for a response to a
     * navigation event (indicated by a beforeunload event). When this happens,
     * we should keep trying to send the request every now and then until there
     * is a response or until it throws an exception saying that it is already
     * being sent.
     */
    private boolean webkitMaybeIgnoringRequests = false;

    public XhrConnection() {
        Browser.getWindow().addEventListener("beforeunload",
                new EventListener() {
                    @Override
                    public void handleEvent(Event evt) {
                        webkitMaybeIgnoringRequests = true;
                    }
                }, false);
    }

    /**
     * Sets the application connection this instance is connected to. Called
     * internally by the framework.
     *
     * @param connection
     *            the application connection this instance is connected to
     */
    public void setConnection(ApplicationConnection connection) {
        this.connection = connection;

        connection.addHandler(ResponseHandlingEndedEvent.TYPE,
                new CommunicationHandler() {
                    @Override
                    public void onRequestStarting(RequestStartingEvent e) {
                    }

                    @Override
                    public void onResponseHandlingStarted(
                            ResponseHandlingStartedEvent e) {
                    }

                    @Override
                    public void onResponseHandlingEnded(
                            ResponseHandlingEndedEvent e) {
                        webkitMaybeIgnoringRequests = false;
                    }
                });

    }

    protected XhrResponseHandler createResponseHandler() {
        return new XhrResponseHandler();
    }

    public class XhrResponseHandler implements Xhr.Callback {

        private JsonObject payload;
        private double requestStartTime;

        public XhrResponseHandler() {
        }

        /**
         * Sets the payload which was sent to the server
         *
         * @param payload
         *            the payload which was sent to the server
         */
        public void setPayload(JsonObject payload) {
            this.payload = payload;
        }

        @Override
        public void onFail(XMLHttpRequest xhr, Exception e) {
            XhrConnectionError errorEvent = new XhrConnectionError(xhr, payload,
                    e);
            if (e == null) {
                // Response other than 200

                getConnectionStateHandler().xhrInvalidStatusCode(errorEvent);
                return;
            } else {
                getConnectionStateHandler().xhrException(errorEvent);
            }

        }

        @Override
        public void onSuccess(XMLHttpRequest xhr) {
            Console.log("Server visit took "
                    + Profiler.getRelativeTimeString(requestStartTime) + "ms");

            // for(;;);["+ realJson +"]"
            String responseText = xhr.getResponseText();

            ValueMap json = MessageHandler.parseWrappedJson(responseText);
            if (json == null) {
                // Invalid string (not wrapped as expected or can't parse)
                getConnectionStateHandler().xhrInvalidContent(
                        new XhrConnectionError(xhr, payload, null));
                return;
            }

            getConnectionStateHandler().xhrOk();
            Console.log("Received xhr message: " + responseText);
            getMessageHandler().handleMessage(json);
        }

        /**
         * Sets the relative time (see {@link Profiler#getRelativeTimeMillis()})
         * when the request was sent.
         *
         * @param requestStartTime
         *            the relative time when the request was sent
         */
        private void setRequestStartTime(double requestStartTime) {
            this.requestStartTime = requestStartTime;

        }
    };

    /**
     * Sends an asynchronous UIDL request to the server using the given URI.
     *
     * @param payload
     *            The URI to use for the request. May includes GET parameters
     */
    public void send(JsonObject payload) {
        XhrResponseHandler responseHandler = createResponseHandler();
        responseHandler.setPayload(payload);
        responseHandler.setRequestStartTime(Profiler.getRelativeTimeMillis());

        XMLHttpRequest xhr = Xhr.post(getUri(), payload.toJson(),
                JsonConstants.JSON_CONTENT_TYPE, responseHandler);

        Console.log("Sending xhr message to server: " + payload.toJson());

        if (webkitMaybeIgnoringRequests && BrowserInfo.get().isWebkit()) {
            final int retryTimeout = 250;
            new Timer() {
                @Override
                public void run() {
                    // Use native js to access private field in Request
                    if (resendRequest(xhr) && webkitMaybeIgnoringRequests) {
                        // Schedule retry if still needed
                        schedule(retryTimeout);
                    }
                }
            }.schedule(retryTimeout);
        }
    }

    /**
     * Retrieves the URI to use when sending RPCs to the server
     *
     * @return The URI to use for server messages.
     */
    protected String getUri() {
        String uri = connection
                .translateVaadinUri(ApplicationConstants.APP_PROTOCOL_PREFIX
                        + ApplicationConstants.UIDL_PATH + '/');

        uri = SharedUtil.addGetParameters(uri,
                ApplicationConstants.UI_ID_PARAMETER + "="
                        + connection.getConfiguration().getUIId());

        return uri;

    }

    private ConnectionStateHandler getConnectionStateHandler() {
        return connection.getConnectionStateHandler();
    }

    private MessageHandler getMessageHandler() {
        return connection.getMessageHandler();
    }

    private static native boolean resendRequest(XMLHttpRequest xhr)
    /*-{
        if (xhr.readyState != 1) {
            // Progressed to some other readyState -> no longer blocked
            return false;
        }
        try {
            xhr.send();
            return true;
        } catch (e) {
            // send throws exception if it is running for real
            return false;
        }
    }-*/;

}
