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
import com.google.gwt.http.client.Response;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Timer;
import com.google.gwt.xhr.client.XMLHttpRequest;

import com.vaadin.client.ConnectionIndicator;
import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.UILifecycle;
import com.vaadin.client.UILifecycle.UIState;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.AtmospherePushConnection.AtmosphereResponse;

import elemental.client.Browser;
import elemental.json.JsonObject;

/**
 * Default implementation of the connection state handler.
 * <p>
 * Handles temporary errors by showing a reconnect dialog to the user while
 * trying to re-establish the connection to the server and re-send the pending
 * message.
 * <p>
 * Handles permanent errors by showing a critical system notification to the
 * user
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultConnectionStateHandler implements ConnectionStateHandler {

    private static final boolean DEBUG = false;
    private final Registry registry;
    private int reconnectAttempt = 0;
    private Type reconnectionCause = null;

    private Timer scheduledReconnect;

    protected enum Type {
        HEARTBEAT(0), PUSH(1), XHR(2);

        private int priority;

        private Type(int priority) {
            this.priority = priority;
        }

        public boolean isMessage() {
            return this == PUSH || this == XHR;
        }

        /**
         * Checks if this type is of higher priority than the given type.
         *
         * @param type
         *            the type to compare to
         * @return true if this type has higher priority than the given type,
         *         false otherwise
         */
        public boolean isHigherPriorityThan(Type type) {
            return priority > type.priority;
        }
    }

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public DefaultConnectionStateHandler(Registry registry) {
        this.registry = registry;
        registry.getUILifecycle().addHandler(e -> {
            if (e.getUiLifecycle().isTerminated()) {
                if (isReconnecting()) {
                    giveUp();
                    stopApplication();
                }
                if (scheduledReconnect != null
                        && scheduledReconnect.isRunning()) {
                    scheduledReconnect.cancel();
                }
            }
        });

        // Register online / offline handlers
        registerConnectionStateEventHandlers();
    }

    /**
     * Checks if we are currently trying to reconnect.
     *
     * @return true if we have noted a problem and are trying to re-establish
     *         server connection, false otherwise
     */
    private boolean isReconnecting() {
        return reconnectionCause != null;
    }

    @Override
    public void xhrException(XhrConnectionError xhrConnectionError) {
        debug("xhrException");
        handleRecoverableError(Type.XHR, xhrConnectionError.getPayload());
    }

    @Override
    public void heartbeatException(XMLHttpRequest request,
            Exception exception) {
        Console.error("Heartbeat exception: " + exception.getMessage());
        handleRecoverableError(Type.HEARTBEAT, null);
    }

    @Override
    public void heartbeatInvalidStatusCode(XMLHttpRequest xhr) {
        int statusCode = xhr.getStatus();
        Console.warn("Heartbeat request returned " + statusCode);

        if (statusCode == Response.SC_FORBIDDEN) {
            // Session expired
            registry.getSystemErrorHandler().handleSessionExpiredError(null);
            stopApplication();
        } else if (statusCode == Response.SC_NOT_FOUND) {
            // UI closed, do nothing as the UI will react to this
            // Should not trigger reconnect dialog as this will prevent user
            // input
        } else {
            handleRecoverableError(Type.HEARTBEAT, null);
        }
    }

    @Override
    public void heartbeatOk() {
        debug("heartbeatOk");
        if (isReconnecting()) {
            resolveTemporaryError(Type.HEARTBEAT);
        }
    }

    private void debug(String msg) {
        if (DEBUG) {
            Console.debug(msg);
        }
    }

    /**
     * Called whenever an error occurs in communication which should be handled
     * by showing the reconnect dialog and retrying communication until
     * successful again.
     *
     * @param type
     *            The type of failure detected
     * @param payload
     *            The message which did not reach the server, or null if no
     *            message was involved (heartbeat or push connection failed)
     */
    protected void handleRecoverableError(Type type, final JsonObject payload) {
        debug("handleTemporaryError(" + type + ")");
        if (!registry.getUILifecycle().isRunning()) {
            return;
        }

        ConnectionIndicator.setState(ConnectionIndicator.RECONNECTING);

        if (!isReconnecting()) {
            // First problem encounter
            reconnectionCause = type;
            Console.warn("Reconnecting because of " + type + " failure");
        } else {
            // We are currently trying to reconnect
            // Priority is HEARTBEAT -> PUSH -> XHR
            // If a higher priority issues is resolved, we can assume the lower
            // one will be also
            if (type.isHigherPriorityThan(reconnectionCause)) {
                Console.warn(
                        "Now reconnecting because of " + type + " failure");
                reconnectionCause = type;
            }
        }

        if (reconnectionCause != type) {
            return;
        }

        reconnectAttempt++;
        Console.debug("Reconnect attempt " + reconnectAttempt + " for " + type);

        if (reconnectAttempt >= getConfiguration().getReconnectAttempts()) {
            // Max attempts reached, stop trying and go back to CONNECTION_LOST
            giveUp();
        } else {
            scheduleReconnect(payload);
        }
    }

    /**
     * Called after a problem occurred.
     *
     * This method is responsible for re-sending the payload to the server (if
     * not null) or re-send a heartbeat request at some point
     *
     * @param payload
     *            the payload that did not reach the server, null if the problem
     *            was detected by a heartbeat
     */
    protected void scheduleReconnect(final JsonObject payload) {
        // Here and not in timer to avoid TB for getting in between

        // The request is still open at this point to avoid interference, so we
        // do not need to start a new one
        if (reconnectAttempt == 1) {
            // Try once immediately
            Console.debug("Immediate reconnect attempt for " + payload);
            doReconnect(payload);
        } else {
            scheduledReconnect = new Timer() {
                @Override
                public void run() {
                    if (scheduledReconnect != null) {
                        scheduledReconnect.cancel();
                    }
                    scheduledReconnect = null;
                    Console.debug("Scheduled reconnect attempt "
                            + reconnectAttempt + " for " + payload);
                    doReconnect(payload);
                }
            };
            scheduledReconnect
                    .schedule(getConfiguration().getReconnectInterval());
        }
    }

    /**
     * Re-sends the payload to the server (if not null) or re-sends a heartbeat
     * request immediately.
     *
     * @param payload
     *            the payload that did not reach the server, null if the problem
     *            was detected by a heartbeat
     */
    protected void doReconnect(JsonObject payload) {
        if (!registry.getUILifecycle().isRunning()) {
            // This should not happen as nobody should call this if the
            // application has been stopped
            Console.warn(
                    "Trying to reconnect after application has been stopped. Giving up");
            return;
        }
        if (payload != null) {
            Console.debug("Trying to re-establish server connection (UIDL)...");
            registry.getRequestResponseTracker()
                    .fireEvent(new ReconnectionAttemptEvent(reconnectAttempt));
        } else {
            // Use heartbeat
            Console.debug(
                    "Trying to re-establish server connection (heartbeat)...");
            registry.getHeartbeat().send();
        }
    }

    /**
     * Called when we should give up trying to reconnect and inform the user
     * that the application is in CONNECTION_LOST state.
     */
    protected final void giveUp() {
        reconnectionCause = null;

        if (registry.getRequestResponseTracker().hasActiveRequest()) {
            endRequest();
        }

        ConnectionIndicator.setState(ConnectionIndicator.CONNECTION_LOST);
        pauseHeartbeats();
    }

    /**
     * Gets the text to show in the reconnect dialog after giving up (reconnect
     * limit reached).
     *
     * @param reconnectAttempt
     *            The number of the current reconnection attempt
     * @return The text to show in the reconnect dialog after giving up
     */
    protected String getDialogTextGaveUp(int reconnectAttempt) {
        return getConfiguration().getDialogTextGaveUp().replace("{0}",
                reconnectAttempt + "");
    }

    /**
     * Gets the text to show in the reconnect dialog.
     *
     * @param reconnectAttempt
     *            The number of the current reconnection attempt
     * @return The text to show in the reconnect dialog
     */
    protected String getDialogText(int reconnectAttempt) {
        return getConfiguration().getDialogText().replace("{0}",
                reconnectAttempt + "");
    }

    @Override
    public void configurationUpdated() {
        // All other properties are fetched directly from the state when needed
        if (getConfiguration().getDialogText() != null) {
            ConnectionIndicator.setProperty("reconnectingText",
                    getConfiguration().getDialogText());
        }
        if (getConfiguration().getDialogTextGaveUp() != null) {
            ConnectionIndicator.setProperty("offlineText",
                    getConfiguration().getDialogTextGaveUp());
        }
    }

    private ReconnectConfiguration getConfiguration() {
        return registry.getReconnectConfiguration();
    }

    @Override
    public void xhrInvalidContent(XhrConnectionError xhrConnectionError) {
        debug("xhrInvalidContent");
        endRequest();

        String responseText = xhrConnectionError.getXhr().getResponseText();
        if (!redirectIfRefreshToken(responseText)) {
            handleUnrecoverableCommunicationError(
                    "Invalid JSON response from server: " + responseText,
                    xhrConnectionError);
        }

    }

    @Override
    public void pushInvalidContent(PushConnection pushConnection,
            String message) {
        debug("pushInvalidContent");
        if (pushConnection.isBidirectional()) {
            // We can't be sure that what was pushed was actually a response but
            // at this point it should not really matter, as something is
            // seriously broken.
            endRequest();
        }

        if (!redirectIfRefreshToken(message)) {
            handleUnrecoverableCommunicationError(
                    "Invalid JSON from server: " + message, null);
        }

    }

    @Override
    public void xhrInvalidStatusCode(XhrConnectionError xhrConnectionError) {
        debug("xhrInvalidStatusCode");

        int statusCode = xhrConnectionError.getXhr().getStatus();
        Console.warn("Server returned " + statusCode + " for xhr");

        if (statusCode == 401) {
            // Authentication/authorization failed, no need to re-try
            endRequest();
            handleUnauthorized(xhrConnectionError);
            return;
        } else {
            // 404, 408 and other 4xx codes CAN be temporary when you have a
            // proxy between the client and the server and e.g. restart the
            // server
            // 5xx codes may or may not be temporary
            handleRecoverableError(Type.XHR, xhrConnectionError.getPayload());
        }
    }

    private void endRequest() {
        registry.getRequestResponseTracker().endRequest();
    }

    protected void handleUnauthorized(XhrConnectionError xhrConnectionError) {
        /*
         * Authorization has failed (401). Assume that the session has timed
         * out.
         */
        registry.getSystemErrorHandler().handleSessionExpiredError("");
        stopApplication();
    }

    private void stopApplication() {
        // Consider application not running any more and prevent all
        // future requests

        UILifecycle uiLifecycle = registry.getUILifecycle();

        if (uiLifecycle.getState() != UIState.TERMINATED) {
            uiLifecycle.setState(UIState.TERMINATED);
        }
    }

    private void handleUnrecoverableCommunicationError(String details,
            XhrConnectionError xhrConnectionError) {
        int statusCode = -1;
        if (xhrConnectionError != null) {
            XMLHttpRequest xhr = xhrConnectionError.getXhr();
            if (xhr != null) {
                statusCode = xhr.getStatus();
            }
        }
        handleCommunicationError(details, statusCode);

        stopApplication();

    }

    /**
     * Called when a communication error occurs and we cannot recover from it.
     *
     * @param details
     *            message details or {@code null} if there are no details
     * @param statusCode
     *            the status code
     */
    protected void handleCommunicationError(String details, int statusCode) {
        registry.getSystemErrorHandler().handleUnrecoverableError("", details,
                "", null);
    }

    @Override
    public void xhrOk() {
        debug("xhrOk");
        if (isReconnecting()) {
            resolveTemporaryError(Type.XHR);
        }
    }

    private void resolveTemporaryError(Type type) {
        debug("resolveTemporaryError(" + type + ")");

        if (reconnectionCause != type) {
            // Waiting for some other problem to be resolved
            return;
        }

        reconnectionCause = null;
        reconnectAttempt = 0;
        if (scheduledReconnect != null) {
            scheduledReconnect.cancel();
            scheduledReconnect = null;
        }
        ConnectionIndicator.setState(ConnectionIndicator.CONNECTED);

        Console.debug("Re-established connection to server");
    }

    @Override
    public void pushOk(PushConnection pushConnection) {
        debug("pushOk()");
        if (isReconnecting()) {
            resolveTemporaryError(Type.PUSH);
            if (registry.getRequestResponseTracker().hasActiveRequest()) {
                debug("pushOk() Reset active request state when reconnecting PUSH because of a network error.");
                endRequest();
                // for bidirectional transport, the pending message is not sent
                // as reconnection payload, so immediately push the pending
                // changes on reconnect
                if (pushConnection.isBidirectional()) {
                    Console.debug(
                            "Flush pending messages after PUSH reconnection.");
                    registry.getMessageSender().sendInvocationsToServer();
                }
            }
        }
    }

    @Override
    public void pushScriptLoadError(String resourceUrl) {
        handleCommunicationError(
                resourceUrl + " could not be loaded. Push will not work.", 0);
    }

    @Override
    public void pushNotConnected(JsonObject payload) {
        debug("pushNotConnected()");
        handleRecoverableError(Type.PUSH, payload);
    }

    @Override
    public void pushReconnectPending(PushConnection pushConnection) {
        debug("pushReconnectPending(" + pushConnection.getTransportType()
                + ")");
        Console.debug("Reopening push connection");
        if (pushConnection.isBidirectional()) {
            // Lost connection for a connection which will tell us when the
            // connection is available again
            handleRecoverableError(Type.PUSH, null);
        } else {
            // Lost connection for a connection we do not necessarily know when
            // it is available again (long polling behind proxy). Do nothing and
            // show reconnect dialog if the user does something and the XHR
            // fails
        }
    }

    @Override
    public void pushError(PushConnection pushConnection,
            JavaScriptObject response) {
        debug("pushError()");
        handleCommunicationError("Push connection using "
                + ((AtmosphereResponse) response).getTransport() + " failed!",
                -1);
    }

    @Override
    public void pushClientTimeout(PushConnection pushConnection,
            JavaScriptObject response) {
        debug("pushClientTimeout()");
        // TODO Reconnect, allowing client timeout to be set
        // https://dev.vaadin.com/ticket/18429
        handleCommunicationError(
                "Client unexpectedly disconnected. Ensure client timeout is disabled.",
                -1);
    }

    @Override
    public void pushClosed(PushConnection pushConnection,
            JavaScriptObject response) {
        debug("pushClosed()");
        Console.debug("Push connection closed");
    }

    private void pauseHeartbeats() {
        registry.getHeartbeat().setInterval(0);
    }

    private void resumeHeartbeats() {
        // Resume heart beat only if it was not terminated (interval == -1)
        if (registry.getHeartbeat().getInterval() >= 0) {
            registry.getHeartbeat().setInterval(registry
                    .getApplicationConfiguration().getHeartbeatInterval());
        }
    }

    private boolean redirectIfRefreshToken(String message) {
        /*
         * A servlet filter or equivalent may have intercepted the request and
         * served non-UIDL content (for instance, a login page if the session
         * has expired.) If the response contains a magic substring, do a
         * synchronous refresh. See
         * https://github.com/vaadin/framework/issues/2059.
         */
        MatchResult refreshToken = RegExp
                .compile(UIDL_REFRESH_TOKEN + "(:\\s*(.*?))?(\\s|$)")
                .exec(message);
        if (refreshToken != null) {
            WidgetUtil.redirect(refreshToken.getGroup(2));
            return true;
        }

        return false;
    }

    private void registerConnectionStateEventHandlers() {
        Browser.getWindow().addEventListener("offline", event ->
        // Browser goes offline: CONNECTION_LOST and stop heartbeats
        giveUp());

        Browser.getWindow().addEventListener("online", event -> {
            // Browser goes back online: RECONNECTING while verifying
            // server connection using heartbeat
            resumeHeartbeats();
            handleRecoverableError(Type.HEARTBEAT, null);
        });
    }
}
