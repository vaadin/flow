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
package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.communication.rpc.AttachExistingElementRpcHandler;
import com.vaadin.flow.server.communication.rpc.AttachTemplateChildRpcHandler;
import com.vaadin.flow.server.communication.rpc.EventRpcHandler;
import com.vaadin.flow.server.communication.rpc.MapSyncRpcHandler;
import com.vaadin.flow.server.communication.rpc.NavigationRpcHandler;
import com.vaadin.flow.server.communication.rpc.PublishedServerEventHandlerRpcHandler;
import com.vaadin.flow.server.communication.rpc.RpcInvocationHandler;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.FlowDauIntegration;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * Handles a client-to-server message containing serialized RPC invocations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ServerRpcHandler implements Serializable {

    /**
     * A data transfer object representing an RPC request sent by the client
     * side.
     *
     * @author Vaadin Ltd
     * @since 1.0
     */
    public static class RpcRequest implements Serializable {

        private final String csrfToken;
        private final ArrayNode invocations;
        private final int syncId;
        private final JsonNode json;
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
            this(jsonString, request.getService().getDeploymentConfiguration()
                    .isSyncIdCheckEnabled());
        }

        public RpcRequest(String jsonString, boolean isSyncIdCheckEnabled) {
            json = JacksonUtils.readTree(jsonString);

            JsonNode token = json.get(ApplicationConstants.CSRF_TOKEN);
            if (token == null) {
                csrfToken = ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE;
            } else {
                String csrfToken = token.asText();
                if (csrfToken.isEmpty()) {
                    csrfToken = ApplicationConstants.CSRF_TOKEN_DEFAULT_VALUE;
                }
                this.csrfToken = csrfToken;
            }

            if (isSyncIdCheckEnabled && !isUnloadBeaconRequest()) {
                syncId = json.get(ApplicationConstants.SERVER_SYNC_ID)
                        .intValue();
            } else {
                syncId = -1;
            }

            if (json.has(ApplicationConstants.RESYNCHRONIZE_ID)) {
                resynchronize = json.get(ApplicationConstants.RESYNCHRONIZE_ID)
                        .booleanValue();
            } else {
                resynchronize = false;
            }

            if (json.has(ApplicationConstants.CLIENT_TO_SERVER_ID)) {
                clientToServerMessageId = json
                        .get(ApplicationConstants.CLIENT_TO_SERVER_ID)
                        .intValue();
            } else {
                if (!isUnloadBeaconRequest()) {
                    getLogger()
                            .warn("Server message without client id received");
                }
                clientToServerMessageId = -1;
            }
            invocations = (ArrayNode) json
                    .get(ApplicationConstants.RPC_INVOCATIONS);
        }

        /**
         * Gets the CSRF security token (synchronizer token pattern) for this
         * request.
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
        public ArrayNode getRpcInvocationsData() {
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
         * Gets the id of the client to server message.
         *
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
         */
        public JsonNode getRawJson() {
            return json;
        }

        private boolean isUnloadBeaconRequest() {
            return json.has(ApplicationConstants.UNLOAD_BEACON);
        }

    }

    /**
     * Exception thrown then the security key sent by the client does not match
     * the expected one.
     *
     * @author Vaadin Ltd
     * @since 1.0
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
     * Exception thrown then the client side resynchronization is required.
     */
    public static class ResynchronizationRequiredException
            extends RuntimeException {

        /**
         * Default constructor for the exception.
         */
        public ResynchronizationRequiredException() {
            super();
        }
    }

    /**
     * Exception thrown when the client side re-sends the same request.
     */
    public static class ClientResentPayloadException extends RuntimeException {

        /**
         * Default constructor for the exception.
         */
        public ClientResentPayloadException() {
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
        handleRpc(ui, SynchronizedRequestHandler.getRequestBody(reader),
                request);
    }

    /**
     * Reads JSON containing zero or more serialized RPC calls (including legacy
     * variable changes) and executes the calls.
     *
     * @param ui
     *            The {@link UI} receiving the calls. Cannot be null.
     * @param message
     *            The JSON message from the request.
     * @param request
     *            The request through which the RPC was received
     * @throws InvalidUIDLSecurityKeyException
     *             If the received security key does not match the one stored in
     *             the session.
     */
    public void handleRpc(UI ui, String message, VaadinRequest request)
            throws InvalidUIDLSecurityKeyException {
        ui.getSession().setLastRequestTimestamp(System.currentTimeMillis());

        if (message == null || message.isEmpty()) {
            // The client sometimes sends empty messages, this is probably a bug
            return;
        }

        RpcRequest rpcRequest = new RpcRequest(message, request.getService()
                .getDeploymentConfiguration().isSyncIdCheckEnabled());

        // Security: double cookie submission pattern unless disabled by
        // property
        if (!VaadinService.isCsrfTokenValid(ui, rpcRequest.getCsrfToken())) {
            throw new InvalidUIDLSecurityKeyException();
        }

        String hashMessage = message;
        if (hashMessage.length() > 64 * 1024) {
            hashMessage = message.substring(0, 64 * 1024);
        }
        byte[] messageHash = MessageDigestUtil.sha256(hashMessage);

        int expectedId = ui.getInternals().getLastProcessedClientToServerId()
                + 1;
        int requestId = rpcRequest.getClientToServerId();

        if (requestId != -1 && requestId != expectedId) {
            // Invalid message id, skip RPC processing but force a full
            // re-synchronization of the client as it might have not received
            // the previous response (e.g. due to a bad connection)

            // Must resync also for duplicate messages because the server might
            // have generated a response for the first message but the response
            // did not reach the client. When the client re-sends the message,
            // it would only get an empty response (because the dirty flags have
            // been cleared on the server) and would be out of sync
            if (requestId == expectedId - 1 && Arrays.equals(messageHash,
                    ui.getInternals().getLastProcessedMessageHash())) {
                /*
                 * Last message was received again. This indicates that this
                 * situation is most likely triggered by a timeout or such
                 * causing a message to be resent.
                 */
                getLogger().debug(
                        "Received old duplicate message from the client. Expected: "
                                + expectedId + ", got: " + requestId
                                + ". Resending previous response.");
                throw new ClientResentPayloadException();
            } else if (rpcRequest.isUnloadBeaconRequest()) {
                getLogger().debug(
                        "Ignoring unexpected message id from the client on UNLOAD request. "
                                + "This could happen for example during login process, if concurrent requests "
                                + "are sent to the server and one of those changes the session identifier, "
                                + "causing an UIDL request to be rejected because of session expiration. "
                                + "Expected client id: {}, got {}.",
                        expectedId, requestId);
            } else {
                /*
                 * If the reason for ending up here is intermittent, then we
                 * should just issue a full resync since we cannot know the
                 * state of the client engine.
                 *
                 * There are reasons to believe that there are deterministic
                 * issues that trigger this condition, and we'd like to collect
                 * more data to uncover anything such before actually
                 * implementing the resync that would thus hide most symptoms of
                 * the actual root cause bugs.
                 */
                String messageDetails = getMessageDetails(rpcRequest);
                getLogger().debug("Unexpected message id from the client."
                        + " Expected client id: " + expectedId + ", got "
                        + requestId + ". Message start: " + messageDetails);
                throw new UnsupportedOperationException(
                        "Unexpected message id from the client."
                                + " Expected client id: " + expectedId
                                + ", got " + requestId
                                + ". more details logged on DEBUG level.");
            }
        } else {
            // Message id ok, process RPCs
            ui.getInternals().setLastProcessedClientToServerId(expectedId,
                    messageHash);
            enforceIfNeeded(request, rpcRequest);
            handleInvocations(ui, rpcRequest.getRpcInvocationsData());
        }

        if (rpcRequest.isResynchronize()) {
            getLogger().warn("Resynchronizing UI by client's request. "
                    + "A network message was lost before reaching the client and the client is reloading the full UI state. "
                    + "This typically happens because of a bad network connection with packet loss or because of some part of"
                    + " the network infrastructure (load balancer, proxy) terminating a push (websocket or long-polling) connection."
                    + " If you are using push with a proxy, make sure the push timeout is set to be smaller than the proxy connection timeout");

            if (request.getWrappedSession().getAttributeNames().stream()
                    .anyMatch(name -> name
                            .startsWith("com.vaadin.server.VaadinSession"))) {
                getLogger().warn(
                        "MPR is in use, so full page reload will be done to achieve re-sync.");
                ui.getPage().reload();
                return;
            }

            // Run detach listeners and re-attach all nodes again to the
            // state tree, in order to send changes for a full re-build of
            // the client-side state tree in the response
            ui.getInternals().getStateTree().prepareForResync();

            // At this point, make no assumptions about which dependencies have
            // been accepted by the client
            ui.getInternals().getDependencyList().clearPendingSendToClient();

            // Signal by exception instead of return value to keep the method
            // signature for source and binary compatibility
            throw new ResynchronizationRequiredException();
        }
        if (rpcRequest.isUnloadBeaconRequest()) {
            if (isPreserveOnRefreshTarget(ui)) {
                getLogger().debug(
                        "Eager UI close ignored for @PreserveOnRefresh view");
            } else {
                ui.close();
                getLogger().debug("UI closed with a beacon request");
            }
        }
    }

    private void enforceIfNeeded(VaadinRequest request, RpcRequest rpcRequest) {
        if (DAUUtils.isDauEnabled(request.getService())) {
            FlowDauIntegration.applyEnforcement(request,
                    shouldApplyEnforcement(rpcRequest));
        }
    }

    private Predicate<VaadinRequest> shouldApplyEnforcement(
            RpcRequest rpcRequest) {
        return request -> {
            // do not apply enforcement when the browser is closing, allow
            // potential resources be released.
            if (rpcRequest.isUnloadBeaconRequest()) {
                return false;
            }
            // do not apply enforcement during a resync, user will be blocked
            // anyway on next request.
            if (rpcRequest.isResynchronize()) {
                return false;
            }
            ArrayNode invocations = rpcRequest.getRpcInvocationsData();
            if (invocations == null) {
                // not a user interaction
                return false;
            }
            // Do not enforce if RPC requests contains only poll or return
            // channel events
            for (int i = 0; i < invocations.size(); i++) {
                JsonNode json = invocations.get(i);
                String type = json.has("type") ? json.get("type").asText() : "";
                String event = json.has("event") ? json.get("event").asText()
                        : "";
                if (!JsonConstants.RPC_TYPE_CHANNEL.equals(type)
                        && (!JsonConstants.RPC_TYPE_EVENT.equals(type)
                                || !PollEvent.DOM_EVENT_NAME.equals(event))) {
                    return true;
                }
            }
            return false;
        };
    }

    // Kind of same as in AbstractNavigationStateRenderer, but gets
    // "routeLayoutTypes" & class from UI instance.
    private static boolean isPreserveOnRefreshTarget(UI ui) {
        return ui.getInternals().getActiveRouterTargetsChain().stream()
                .anyMatch(rt -> rt.getClass()
                        .isAnnotationPresent(PreserveOnRefresh.class));
    }

    private String getMessageDetails(RpcRequest rpcRequest) {
        StringBuilder messageDetails = new StringBuilder();
        ArrayNode rpcArray = rpcRequest.getRpcInvocationsData();
        if (rpcArray == null) {
            return "{ no data }";
        }

        for (int i = 0; i < rpcArray.size(); i++) {
            JsonNode json = rpcArray.get(i);
            String type = json.has("type") ? json.get("type").asText() : "";
            Double node = json.has("node") ? json.get("node").doubleValue()
                    : null;
            Double feature = json.has("feature")
                    ? json.get("feature").doubleValue()
                    : null;
            appendAll(messageDetails, "{ type: ", type, " node: ",
                    String.valueOf(node), " feature: ", String.valueOf(feature),
                    " } ");
        }
        return messageDetails.toString();
    }

    private static void appendAll(StringBuilder builder, String... strings) {
        for (String string : strings) {
            builder.append(string);
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
     * @param invocationsData
     *            JSON containing all information needed to execute all
     *            requested RPC calls.
     */
    private void handleInvocations(UI ui, ArrayNode invocationsData) {
        List<JsonNode> data = new ArrayList<>(invocationsData.size());
        List<Runnable> pendingChangeEvents = new ArrayList<>();

        RpcInvocationHandler mapSyncHandler = getInvocationHandlers()
                .get(JsonConstants.RPC_TYPE_MAP_SYNC);

        for (int i = 0; i < invocationsData.size(); i++) {
            JsonNode invocationJson = invocationsData.get(i);
            String type = invocationJson.get(JsonConstants.RPC_TYPE).asText();
            assert type != null;
            if (JsonConstants.RPC_TYPE_MAP_SYNC.equals(type)) {
                // Handle these before any RPC invocations.
                mapSyncHandler.handle(ui, invocationJson)
                        .ifPresent(runnable -> pendingChangeEvents.add(() -> {
                            try {
                                runnable.run();
                            } catch (Throwable throwable) {
                                callErrorHandler(ui, invocationJson, throwable);
                            }
                        }));
            } else {
                data.add(invocationJson);
            }
        }

        pendingChangeEvents.forEach(runnable -> runMapSyncTask(ui, runnable));
        data.forEach(json -> handleInvocationData(ui, json));
    }

    private void runMapSyncTask(UI ui, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            ui.getSession().getErrorHandler().error(new ErrorEvent(throwable));
        }
    }

    private void handleInvocationData(UI ui, JsonNode invocationJson) {
        String type = invocationJson.get(JsonConstants.RPC_TYPE).asText();
        RpcInvocationHandler handler = getInvocationHandlers().get(type);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unsupported event type: " + type);
        }
        try {
            Optional<Runnable> handle = handler.handle(ui, invocationJson);
            assert !handle.isPresent()
                    : "RPC handler " + handler.getClass().getName()
                            + " returned a Runnable even though it shouldn't";
        } catch (Throwable throwable) {
            callErrorHandler(ui, invocationJson, throwable);
        }
    }

    private static void callErrorHandler(UI ui, JsonNode invocationJson,
            Throwable throwable) {
        StateNode node = ui.getInternals().getStateTree().getNodeById(
                invocationJson.get(JsonConstants.RPC_NODE).intValue());
        ErrorEvent event;
        if (node != null) {
            event = new ErrorEvent(throwable, node);
        } else {
            event = new ErrorEvent(throwable);
        }
        ui.getSession().getErrorHandler().error(event);
    }

    protected String getMessage(Reader reader) throws IOException {

        StringBuilder sb = new StringBuilder(
                SynchronizedRequestHandler.MAX_BUFFER_SIZE);
        char[] buffer = new char[SynchronizedRequestHandler.MAX_BUFFER_SIZE];

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
            list.add(new ReturnChannelHandler());
            return list;
        }
    }
}
