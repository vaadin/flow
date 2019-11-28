/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.google.gwt.core.client.GWT;
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

    /**
     * Counter for the messages send to the server. First sent message has id 0.
     */
    private int clientToServerMessageId = 0;
    private PushConnection push;

    private final Registry registry;
    private final PushConnectionFactory pushConnectionFactory;

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

        ServerRpcQueue serverRpcQueue = registry.getServerRpcQueue();
        if (serverRpcQueue.isEmpty()) {
            return;
        }

        boolean showLoadingIndicator = serverRpcQueue.showLoadingIndicator();
        JsonArray reqJson = serverRpcQueue.toJson();
        serverRpcQueue.clear();

        if (reqJson.length() == 0) {
            // Nothing to send, all invocations were filtered out (for
            // non-existing connectors)
            Console.warn(
                    "All RPCs filtered out, not sending anything to the server");
            return;
        }

        JsonObject extraJson = Json.createObject();
        if (showLoadingIndicator) {
            registry.getLoadingIndicator().trigger();
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

        send(payload);

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
        Console.log("Resynchronizing from server");
        JsonObject resyncParam = Json.createObject();
        resyncParam.put(ApplicationConstants.RESYNCHRONIZE_ID, true);
        send(Json.createArray(), resyncParam);
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
            // No op as everything matches they way it should
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
}
