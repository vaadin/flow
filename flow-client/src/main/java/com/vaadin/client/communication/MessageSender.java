/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.communication;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.Console;
import com.vaadin.client.ConnectionIndicator;
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

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public MessageSender(Registry registry) {
        this.registry = registry;
        this.pushConnectionFactory = GWT.create(PushConnectionFactory.class);
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

        if (registry.getRequestResponseTracker().hasActiveRequest()
                || (push != null && !push.isActive())) {
            // There is an active request or push is enabled but not active
            // -> send when current request completes or push becomes active
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
            send(payload);
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
            Console.log("Resynchronizing from server");
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
        payload.put(ApplicationConstants.SERVER_SYNC_ID,
                registry.getMessageHandler().getLastSeenServerSyncId());
        payload.put(ApplicationConstants.CLIENT_TO_SERVER_ID,
                clientToServerMessageId++);
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
     * given URI.
     *
     * @param payload
     *            The contents of the request to send
     */
    public void send(final JsonObject payload) {
        if (push != null && push.isBidirectional()) {
            // When using bidirectional transport, the payload is not resent
            // to the server during reconnection attempts.
            // Keep a copy of the message, so that it could be resent to the
            // server after a reconnection.
            // Reference will be cleaned up once the server confirms it has
            // seen this message
            pushPendingMessage = payload;
            push.push(payload);
        } else {
            registry.getXhrConnection().send(payload);
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
        if (enabled && push == null) {
            push = pushConnectionFactory.create(registry);
        } else if (!enabled && push != null && push.isActive()) {
            push.disconnect(() -> {
                push = null;
                /*
                 * If push has been enabled again while we were waiting for the
                 * old connection to disconnect, now is the right time to open a
                 * new connection
                 */
                if (registry.getPushConfiguration().isPushEnabled()) {
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
            return;
        }
        if (force) {
            Console.log(
                    "Forced update of clientId to " + clientToServerMessageId);
            clientToServerMessageId = nextExpectedId;
            return;
        }

        if (nextExpectedId > clientToServerMessageId) {
            if (clientToServerMessageId == 0) {
                // We have never sent a message to the server, so likely the
                // server knows better (typical case is that we refreshed a
                // @PreserveOnRefresh UI)
                Console.log("Updating client-to-server id to " + nextExpectedId
                        + " based on server");
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
            Console.log("Resynchronize from server requested");
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
}
