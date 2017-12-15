/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.communication.rpc.AttachExistingElementRpcHandler;
import com.vaadin.flow.server.communication.rpc.AttachTemplateChildRpcHandler;
import com.vaadin.flow.server.communication.rpc.EventRpcHandler;
import com.vaadin.flow.server.communication.rpc.MapSyncRpcHandler;
import com.vaadin.flow.server.communication.rpc.NavigationRpcHandler;
import com.vaadin.flow.server.communication.rpc.PublishedServerEventHandlerRpcHandler;
import com.vaadin.flow.server.communication.rpc.RpcInvocationHandler;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.ui.UI;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * Handles a client-to-server message containing serialized RPC invocations.
 *
 * @author Vaadin Ltd
 * @since 7.1
 */
public class ServerRpcHandler implements Serializable {

    public static final String WIDGETSET_MISMATCH_INFO = "\n"
            + "=================================================================\n"
            + "The widgetset in use does not seem to be built for the Vaadin\n"
            + "version in use. This might cause strange problems - a\n"
            + "recompile/deploy is strongly recommended.\n"
            + " Vaadin version: %s\n" + " Widgetset version: %s\n"
            + "=================================================================";

    /**
     * A data transfer object representing an RPC request sent by the client
     * side.
     *
     * @since 7.2
     * @author Vaadin Ltd
     */
    public static class RpcRequest implements Serializable {

        private final String csrfToken;
        private final JsonArray invocations;
        private final int syncId;
        private final JsonObject json;
        private final boolean resynchronize;
        private final int clientToServerMessageId;

        /**
         * Creates an instance based on the given JSON received through the
         * given request.
         *
         * @param jsonString
         *            the JSON containing the RPC invocations
         * @param request
         *            the request through which the JSON was received
         */
        public RpcRequest(String jsonString, VaadinRequest request) {
            json = JsonUtil.parse(jsonString);

            JsonValue token = json.get(ApplicationConstants.CSRF_TOKEN);
            if (token == null) {
                csrfToken = ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE;
            } else {
                String csrfToken = token.asString();
                if (csrfToken.equals("")) {
                    csrfToken = ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE;
                }
                this.csrfToken = csrfToken;
            }

            if (request.getService().getDeploymentConfiguration()
                    .isSyncIdCheckEnabled()) {
                syncId = (int) json
                        .getNumber(ApplicationConstants.SERVER_SYNC_ID);
            } else {
                syncId = -1;
            }

            if (json.hasKey(ApplicationConstants.RESYNCHRONIZE_ID)) {
                resynchronize = json
                        .getBoolean(ApplicationConstants.RESYNCHRONIZE_ID);
            } else {
                resynchronize = false;
            }

            if (json.hasKey(ApplicationConstants.CLIENT_TO_SERVER_ID)) {
                clientToServerMessageId = (int) json
                        .getNumber(ApplicationConstants.CLIENT_TO_SERVER_ID);
            } else {
                getLogger()
                        .warn("Server message without client id received");
                clientToServerMessageId = -1;
            }
            invocations = json.getArray(ApplicationConstants.RPC_INVOCATIONS);
        }

        /**
         * Gets the CSRF security token (double submit cookie) for this request.
         *
         * @return the CSRF security token for this current change request
         */
        public String getCsrfToken() {
            return csrfToken;
        }

        /**
         * Gets the data to recreate the RPC as requested by the client side.
         *
         * @return the data describing which RPC should be made, and all their
         *         data
         */
        public JsonArray getRpcInvocationsData() {
            return invocations;
        }

        /**
         * Gets the sync id last seen by the client.
         *
         * @return the last sync id given by the server, according to the
         *         client's request
         */
        public int getSyncId() {
            return syncId;
        }

        /**
         * Checks if this is a request to resynchronize the client side.
         *
         * @return true if this is a resynchronization request, false otherwise
         */
        public boolean isResynchronize() {
            return resynchronize;
        }

        /**
         * Gets the id of the client to server message
         *
         * @since 7.6
         * @return the server message id
         */
        public int getClientToServerId() {
            return clientToServerMessageId;
        }

        /**
         * Gets the entire request in JSON format, as it was received from the
         * client.
         * <p>
         * <em>Note:</em> This is a shared reference - any modifications made
         * will be shared.
         *
         * @return the raw JSON object that was received from the client
         *
         */
        public JsonObject getRawJson() {
            return json;
        }

    }

    private static final int MAX_BUFFER_SIZE = 64 * 1024;

    /**
     * Exception thrown then the security key sent by the client does not match
     * the expected one.
     *
     * @author Vaadin Ltd
     */
    public static class InvalidUIDLSecurityKeyException
            extends GeneralSecurityException {

        /**
         * Default constructor for the exception.
         */
        public InvalidUIDLSecurityKeyException() {
            super();
        }
    }

    /**
     * Reads JSON containing zero or more serialized RPC calls (including legacy
     * variable changes) and executes the calls.
     *
     * @param ui
     *            The {@link UI} receiving the calls. Cannot be null.
     * @param reader
     *            The {@link Reader} used to read the JSON.
     * @param request
     *            The request through which the RPC was received
     * @throws IOException
     *             If reading the message fails.
     * @throws InvalidUIDLSecurityKeyException
     *             If the received security key does not match the one stored in
     *             the session.
     */
    public void handleRpc(UI ui, Reader reader, VaadinRequest request)
            throws IOException, InvalidUIDLSecurityKeyException {
        ui.getSession().setLastRequestTimestamp(System.currentTimeMillis());

        String changeMessage = getMessage(reader);

        if (changeMessage == null || changeMessage.equals("")) {
            // The client sometimes sends empty messages, this is probably a bug
            return;
        }

        RpcRequest rpcRequest = new RpcRequest(changeMessage, request);

        // Security: double cookie submission pattern unless disabled by
        // property
        if (!VaadinService.isCsrfTokenValid(ui.getSession(),
                rpcRequest.getCsrfToken())) {
            throw new InvalidUIDLSecurityKeyException();
        }

        int expectedId = ui.getInternals().getLastProcessedClientToServerId()
                + 1;
        if (rpcRequest.getClientToServerId() != -1
                && rpcRequest.getClientToServerId() != expectedId) {
            // Invalid message id, skip RPC processing but force a full
            // re-synchronization of the client as it might have not received
            // the previous response (e.g. due to a bad connection)

            // Must resync also for duplicate messages because the server might
            // have generated a response for the first message but the response
            // did not reach the client. When the client re-sends the message,
            // it would only get an empty response (because the dirty flags have
            // been cleared on the server) and would be out of sync

            // FIXME Implement resync

            if (rpcRequest.getClientToServerId() < expectedId) {
                // Just a duplicate message due to a bad connection or similar
                // It has already been handled by the server so it is safe to
                // ignore
                getLogger()
                        .debug("Ignoring old message from the client. Expected: {}, got: {}",
                            expectedId, rpcRequest.getClientToServerId());
            } else {
                getLogger().warn(
                        "Unexpected message id from the client. Expected: {}, got: {}",
                                expectedId,
                                rpcRequest.getClientToServerId());
            }

            throw new UnsupportedOperationException(
                    "FIXME: Implement resync and call it above");
        } else {
            // Message id ok, process RPCs
            ui.getInternals().setLastProcessedClientToServerId(expectedId);
            handleInvocations(ui, rpcRequest.getSyncId(),
                    rpcRequest.getRpcInvocationsData());
        }

        if (rpcRequest.isResynchronize()) {
            // FIXME Implement
            throw new UnsupportedOperationException("FIXME: Implement resync");
        }

    }

    /**
     * Gets {@link RpcInvocationHandler}s map where the key is the type of the
     * handler gotten via {@link RpcInvocationHandler#getRpcType()}.
     * <p>
     * This map is used to delegate RPC requests to a specific invocation
     * handler using the type of the request.
     * <p>
     * Subclasses can overwrite this method to return custom invocation
     * handlers.
     *
     * @return invocation handlers map
     */
    protected Map<String, RpcInvocationHandler> getInvocationHandlers() {
        return Collections.unmodifiableMap(LazyInvocationHandlers.HANDLERS);
    }

    /**
     * Processes invocations data received from the client.
     * <p>
     * The invocations data can contain any number of RPC calls.
     *
     * @param ui
     *            the UI receiving the invocations data
     * @param lastSyncIdSeenByClient
     *            the most recent sync id the client has seen at the time the
     *            request was sent
     * @param invocationsData
     *            JSON containing all information needed to execute all
     *            requested RPC calls.
     */
    private void handleInvocations(UI ui, int lastSyncIdSeenByClient,
            JsonArray invocationsData) {

        List<JsonObject> data = new ArrayList<>(invocationsData.length());
        RpcInvocationHandler mapSyncHandler = getInvocationHandlers()
                .get(JsonConstants.RPC_TYPE_MAP_SYNC);
        for (int i = 0; i < invocationsData.length(); i++) {
            JsonObject invocationJson = invocationsData.getObject(i);
            String type = invocationJson.getString(JsonConstants.RPC_TYPE);
            assert type != null;
            if (JsonConstants.RPC_TYPE_MAP_SYNC.equals(type)) {
                // Handle these before any RPC
                mapSyncHandler.handle(ui, invocationJson);
            } else {
                data.add(invocationJson);
            }
        }

        if (mapSyncHandler instanceof MapSyncRpcHandler) {
            ((MapSyncRpcHandler) mapSyncHandler).flushPendingChangeEvents();
        }
        data.forEach(json -> handleInvocationData(ui, json));
    }

    private void handleInvocationData(UI ui, JsonObject invocationJson) {
        String type = invocationJson.getString(JsonConstants.RPC_TYPE);
        RpcInvocationHandler handler = getInvocationHandlers().get(type);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unsupported event type: " + type);
        }
        handler.handle(ui, invocationJson);
    }

    protected String getMessage(Reader reader) throws IOException {

        StringBuilder sb = new StringBuilder(MAX_BUFFER_SIZE);
        char[] buffer = new char[MAX_BUFFER_SIZE];

        while (true) {
            int read = reader.read(buffer);
            if (read == -1) {
                break;
            }
            sb.append(buffer, 0, read);
        }

        return sb.toString();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ServerRpcHandler.class.getName());
    }

    private static RpcInvocationHandler resolveHandlerConflicts(
            RpcInvocationHandler handler1, RpcInvocationHandler handler2) {
        String msg = String.format(
                "There are two Rpc invocation handlers for the same type '%s' : '%s and %s",
                handler1.getRpcType(), handler1.getClass().getName(),
                handler2.getClass().getName());
        throw new IllegalStateException(msg);
    }

    private static class LazyInvocationHandlers {
        private static final Map<String, RpcInvocationHandler> HANDLERS = loadHandlers()
                .stream()
                .collect(Collectors.toMap(RpcInvocationHandler::getRpcType,
                        Function.identity(),
                        ServerRpcHandler::resolveHandlerConflicts,
                        HashMap::new));

        private static List<RpcInvocationHandler> loadHandlers() {
            List<RpcInvocationHandler> list = new ArrayList<>();
            list.add(new EventRpcHandler());
            list.add(new NavigationRpcHandler());
            list.add(new MapSyncRpcHandler());
            list.add(new PublishedServerEventHandlerRpcHandler());
            list.add(new AttachExistingElementRpcHandler());
            list.add(new AttachTemplateChildRpcHandler());
            return list;
        }
    }
}
