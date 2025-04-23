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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;

import com.vaadin.client.ConnectionIndicator;
import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.flow.shared.ApplicationConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * MessageSender is responsible for sending messages to the server.
 * <p>
 * Internally uses {@link XhrConnection} and/or {@link PushConnection} for
 * delivering messages, depending on the application configuration.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MessageSender {

    public void sendUnloadBeacon() {
        JsonArray dummyEmptyJson = Json.createArray();
        JsonObject extraJson = Json.createObject();
        extraJson.put(ApplicationConstants.UNLOAD_BEACON, true);
        JsonObject payload = preparePayload(dummyEmptyJson, extraJson);
        sendBeacon(registry.getXhrConnection().getUri(), payload.toJson());
    }

    public static native void sendBeacon(String url, String payload) /*-{
        $wnd.navigator.sendBeacon(url, payload);
    }-*/;

    public enum ResynchronizationState {
        NOT_ACTIVE, SEND_TO_SERVER, WAITING_FOR_RESPONSE
    }

    /**
     * Counter for the messages send to the server. First sent message has id 0.
     */
    private int clientToServerMessageId = 0;
    private PushConnection push;

    private final Registry registry;
    private final PushConnectionFactory pushConnectionFactory;

    private ResynchronizationState resynchronizationState = ResynchronizationState.NOT_ACTIVE;

    private JsonObject pushPendingMessage;

    private List<JsonObject> messageQueue = new ArrayList<>();

    private Timer resendMessageTimer;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public MessageSender(Registry registry) {
        this.registry = registry;
        this.pushConnectionFactory = GWT.create(PushConnectionFactory.class);
        this.registry.getRequestResponseTracker()
                .addReconnectionAttemptHandler(ev -> {
                    Console.debug(
                            "Re-sending queued messages to the server (attempt "
                                    + ev.getAttempt() + ") ...");
                    // Try to reconnect by sending queued messages.
                    // Stops the resend timer, since it will anyway not make any
                    // request during reconnection process.
                    resetTimer();
                    doSendInvocationsToServer();
                });
    }

    /**
     * Sends any pending invocations to the server if there is no request in
     * progress and the application is running.
     * <p>
     * If a request is in progress, this method does nothing and assumes that it
     * is called again when the request completes.
     */
    public void sendInvocationsToServer() {
        if (!registry.getUILifecycle().isRunning()) {
            Console.warn(
                    "Trying to send RPC from not yet started or stopped application");
            return;
        }

        boolean hasActiveRequest = registry.getRequestResponseTracker()
                .hasActiveRequest();
        if (hasActiveRequest || (push != null && !push.isActive())) {
            // There is an active request or push is enabled but not active
            // -> send when current request completes or push becomes active
            Console.debug("Postpone sending invocations to server because of "
                    + (hasActiveRequest ? "active request"
                            : "PUSH not active"));
        } else {
            doSendInvocationsToServer();
        }
    }

    /**
     * Sends all pending method invocations (server RPC and legacy variable
     * changes) to the server.
     *
     */
    private void doSendInvocationsToServer() {
        // If there's a stored message, resend it and postpone processing the
        // rest of the queued messages to prevent resynchronization issues.
        if (pushPendingMessage != null) {
            Console.log("Sending pending push message "
                    + pushPendingMessage.toJson());
            JsonObject payload = pushPendingMessage;
            pushPendingMessage = null;
            registry.getRequestResponseTracker().startRequest();
            sendPayload(payload);
            return;
        } else if (hasQueuedMessages()) {
            Console.debug("Sending queued messages to server");
            if (resendMessageTimer != null) {
                // Stopping resend timer and re-send immediately
                resetTimer();
            }
            sendPayload(messageQueue.get(0));
            return;
        }

        ServerRpcQueue serverRpcQueue = registry.getServerRpcQueue();
        if (serverRpcQueue.isEmpty()
                && resynchronizationState != ResynchronizationState.SEND_TO_SERVER) {
            return;
        }

        boolean showLoadingIndicator = serverRpcQueue.showLoadingIndicator();
        JsonArray reqJson = serverRpcQueue.toJson();
        serverRpcQueue.clear();

        if (reqJson.length() == 0
                && resynchronizationState != ResynchronizationState.SEND_TO_SERVER) {
            // Nothing to send, all invocations were filtered out (for
            // non-existing connectors)
            Console.warn(
                    "All RPCs filtered out, not sending anything to the server");
            return;
        }

        JsonObject extraJson = Json.createObject();
        if (resynchronizationState == ResynchronizationState.SEND_TO_SERVER) {
            resynchronizationState = ResynchronizationState.WAITING_FOR_RESPONSE;
            Console.warn("Resynchronizing from server");
            messageQueue.clear();
            resetTimer();
            extraJson.put(ApplicationConstants.RESYNCHRONIZE_ID, true);
        }
        if (showLoadingIndicator) {
            ConnectionIndicator.setState(ConnectionIndicator.LOADING);
        }
        send(reqJson, extraJson);
    }

    /**
     * Makes an UIDL request to the server.
     *
     * @param reqInvocations
     *            Data containing RPC invocations and all related information.
     * @param extraJson
     *            Parameters that are added to the payload
     */
    protected void send(final JsonArray reqInvocations,
            final JsonObject extraJson) {
        registry.getRequestResponseTracker().startRequest();
        send(preparePayload(reqInvocations, extraJson));
    }

    private JsonObject preparePayload(final JsonArray reqInvocations,
            final JsonObject extraJson) {
        JsonObject payload = Json.createObject();
        String csrfToken = registry.getMessageHandler().getCsrfToken();
        if (!csrfToken.equals(ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE)) {
            payload.put(ApplicationConstants.CSRF_TOKEN, csrfToken);
        }
        payload.put(ApplicationConstants.RPC_INVOCATIONS, reqInvocations);
        if (extraJson != null) {
            for (String key : extraJson.keys()) {
                JsonValue value = extraJson.get(key);
                payload.put(key, value);
            }
        }
        return payload;
    }

    /**
     * Sends an asynchronous or synchronous UIDL request to the server using the
     * given URI. Adds message to message queue and postpones sending if queue
     * not empty.
     *
     * @param payload
     *            The contents of the request to send
     */
    public void send(final JsonObject payload) {
        if (hasQueuedMessages()) {
            // The sever sync id is set in the private sendPayload method.
            // If it is already present on the payload, it means the message has
            // been already sent and enqueued.
            if (!payload.hasKey(ApplicationConstants.SERVER_SYNC_ID)) {
                messageQueue.add(payload);
                Console.debug(
                        "Message not sent because other messages are pending. Added to the queue: "
                                + payload.toJson());
            } else {
                Console.debug("Message not sent because already queued: "
                        + payload.toJson());
            }
            return;
        }
        messageQueue.add(payload);
        sendPayload(payload);
    }

    /**
     * Sends an asynchronous or synchronous UIDL request to the server using the
     * given URI.
     *
     * @param payload
     *            The contents of the request to send
     */
    private void sendPayload(final JsonObject payload) {
        payload.put(ApplicationConstants.SERVER_SYNC_ID,
                registry.getMessageHandler().getLastSeenServerSyncId());
        // clientID should only be set and updated if payload doesn't contain
        // clientID. If one exists we are probably trying to resend.
        if (!payload.hasKey(ApplicationConstants.CLIENT_TO_SERVER_ID)) {
            payload.put(ApplicationConstants.CLIENT_TO_SERVER_ID,
                    clientToServerMessageId++);
        }

        if (!registry.getRequestResponseTracker().hasActiveRequest()) {
            // Direct calls to send from outside probably have not started
            // request.
            registry.getRequestResponseTracker().startRequest();
        }

        if (push != null && push.isBidirectional()) {
            // When using bidirectional transport, the payload is not resent
            // to the server during reconnection attempts.
            // Keep a copy of the message, so that it could be resent to the
            // server after a reconnection.
            // Reference will be cleaned up once the server confirms it has
            // seen this message
            Console.debug("send PUSH");
            pushPendingMessage = payload;
            push.push(payload);
        } else {
            Console.debug("send XHR");
            registry.getXhrConnection().send(payload);
            resetTimer();
            // resend last payload if response hasn't come in.
            resendMessageTimer = new Timer() {
                @Override
                public void run() {
                    resendMessageTimer
                            .schedule(registry.getApplicationConfiguration()
                                    .getMaxMessageSuspendTimeout() + 500);
                    // Avoid re-sending the message if a request is still in
                    // progress.
                    // If the response to the message has not yet been processed
                    // the reconnection attempt listener takes care of resending
                    // the queued message.
                    if (!registry.getRequestResponseTracker()
                            .hasActiveRequest()) {
                        registry.getRequestResponseTracker().startRequest();
                        registry.getXhrConnection().send(payload);
                    }
                }
            };
            resendMessageTimer.schedule(registry.getApplicationConfiguration()
                    .getMaxMessageSuspendTimeout() + 500);
        }
    }

    private void resetTimer() {
        if (resendMessageTimer != null) {
            resendMessageTimer.cancel();
            resendMessageTimer = null;
        }
    }

    /**
     * Sets the status for the push connection.
     *
     * @param enabled
     *            <code>true</code> to enable the push connection;
     *            <code>false</code> to disable the push connection.
     */
    public void setPushEnabled(boolean enabled) {
        setPushEnabled(enabled, true);
    }

    /**
     * Sets the status for the push connection.
     *
     * @param enabled
     *            <code>true</code> to enable the push connection;
     *            <code>false</code> to disable the push connection.
     * @param reEnableIfNeeded
     *            <code>true</code> if push should be re-enabled after
     *            disconnection if configuration changed; <code>false</code> to
     *            prevent reconnection.
     */
    public void setPushEnabled(boolean enabled, boolean reEnableIfNeeded) {
        if (enabled && (push == null || !push.isActive())) {
            push = pushConnectionFactory.create(registry);
        } else if (!enabled && push != null && push.isActive()) {
            push.disconnect(() -> {
                push = null;
                /*
                 * If push has been enabled again while we were waiting for the
                 * old connection to disconnect, now is the right time to open a
                 * new connection
                 */
                if (reEnableIfNeeded
                        && registry.getPushConfiguration().isPushEnabled()) {
                    setPushEnabled(true);
                }

                /*
                 * Send anything that was enqueued while we waited for the
                 * connection to close
                 */
                if (registry.getServerRpcQueue().isFlushPending()) {
                    registry.getServerRpcQueue().flush();
                }
            });
        }
    }

    /**
     * Returns a human readable string representation of the method used to
     * communicate with the server.
     *
     * @return A string representation of the current transport type
     */
    public String getCommunicationMethodName() {
        String clientToServer = "XHR";
        String serverToClient = "-";
        if (push != null) {
            serverToClient = push.getTransportType();
            if (push.isBidirectional()) {
                clientToServer = serverToClient;
            }
        }

        return "Client to server: " + clientToServer + ", "
                + "server to client: " + serverToClient;
    }

    /**
     * Resynchronize the client side, i.e. reload all component hierarchy and
     * state from the server
     */
    public void resynchronize() {
        if (requestResynchronize()) {
            messageQueue.clear();
            resetTimer();
            sendInvocationsToServer();
        }
    }

    /**
     * Used internally to update what id the server expects.
     *
     * @param nextExpectedId
     *            the new client id to set
     * @param force
     *            true if the id must be updated, false otherwise
     */
    public void setClientToServerMessageId(int nextExpectedId, boolean force) {
        if (nextExpectedId == clientToServerMessageId) {
            // Everything matches they way it should
            // Remove potential pending PUSH message if it has already been seen
            // by the server.
            if (pushPendingMessage != null
                    && (int) pushPendingMessage.getNumber(
                            ApplicationConstants.CLIENT_TO_SERVER_ID) < nextExpectedId) {
                pushPendingMessage = null;
            }
            if (hasQueuedMessages()) {
                // If queued message is the expected one. remove from queue
                // and send next message if any.
                if (messageQueue.get(0)
                        .getNumber(ApplicationConstants.CLIENT_TO_SERVER_ID)
                        + 1 == nextExpectedId) {
                    messageQueue.remove(0);
                    resetTimer();
                }
            }
            return;
        }
        if (force) {
            Console.debug(
                    "Forced update of clientId to " + clientToServerMessageId);
            clientToServerMessageId = nextExpectedId;
            messageQueue.clear();
            resetTimer();
            return;
        }

        if (nextExpectedId > clientToServerMessageId) {
            if (clientToServerMessageId == 0) {
                // We have never sent a message to the server, so likely the
                // server knows better (typical case is that we refreshed a
                // @PreserveOnRefresh UI)
                Console.debug("Updating client-to-server id to "
                        + nextExpectedId + " based on server");
            } else {
                Console.warn("Server expects next client-to-server id to be "
                        + nextExpectedId + " but we were going to use "
                        + clientToServerMessageId + ". Will use "
                        + nextExpectedId + ".");
            }
            clientToServerMessageId = nextExpectedId;
        } else {
            // Server has not yet seen all our messages
            // Do nothing as they will arrive eventually
        }
    }

    /**
     * Modifies the resynchronize state to indicate that resynchronization is
     * desired
     *
     * @return true if the resynchronize request still needs to be sent; false
     *         otherwise
     */
    boolean requestResynchronize() {
        switch (resynchronizationState) {
        case NOT_ACTIVE:
            Console.debug("Resynchronize from server requested");
            resynchronizationState = ResynchronizationState.SEND_TO_SERVER;
            return true;
        case SEND_TO_SERVER:
            // Resynchronize has already been requested, but hasn't been sent
            // yet
            return true;
        case WAITING_FOR_RESPONSE:
        default:
            // Resynchronize has already been requested, but response hasn't
            // been received yet
            return false;
        }
    }

    void clearResynchronizationState() {
        resynchronizationState = ResynchronizationState.NOT_ACTIVE;
    }

    ResynchronizationState getResynchronizationState() {
        return resynchronizationState;
    }

    public boolean hasQueuedMessages() {
        return !messageQueue.isEmpty();
    }
}
