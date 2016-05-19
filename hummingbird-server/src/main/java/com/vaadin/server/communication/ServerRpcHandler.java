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

package com.vaadin.server.communication;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.nodefeature.ComponentMapping;
import com.vaadin.hummingbird.nodefeature.ElementListenerMap;
import com.vaadin.hummingbird.nodefeature.ElementPropertyMap;
import com.vaadin.server.Constants;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.shared.JsonConstants;
import com.vaadin.shared.Version;
import com.vaadin.ui.Component;
import com.vaadin.ui.History;
import com.vaadin.ui.History.HistoryStateChangeEvent;
import com.vaadin.ui.History.HistoryStateChangeHandler;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * Handles a client-to-server message containing serialized RPC invocations.
 *
 * @author Vaadin Ltd
 * @since 7.1
 */
public class ServerRpcHandler implements Serializable {

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
        private String widgetsetVersion = null;

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
            if (json.hasKey(ApplicationConstants.WIDGETSET_VERSION_ID)) {
                widgetsetVersion = json
                        .getString(ApplicationConstants.WIDGETSET_VERSION_ID);
            }

            if (json.hasKey(ApplicationConstants.CLIENT_TO_SERVER_ID)) {
                clientToServerMessageId = (int) json
                        .getNumber(ApplicationConstants.CLIENT_TO_SERVER_ID);
            } else {
                getLogger()
                        .warning("Server message without client id received");
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

        /**
         * Gets the widget set version reported by the client
         *
         * @since 7.6
         * @return The widget set version reported by the client or null if the
         *         message did not contain a widget set version
         */
        public String getWidgetsetVersion() {
            return widgetsetVersion;
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

        checkWidgetsetVersion(rpcRequest.getWidgetsetVersion());

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
                        .fine("Ignoring old message from the client. Expected: "
                                + expectedId + ", got: "
                                + rpcRequest.getClientToServerId());
            } else {
                getLogger().warning(
                        "Unexpected message id from the client. Expected: "
                                + expectedId + ", got: "
                                + rpcRequest.getClientToServerId());
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

    static void invokeMethod(Component instance, Class<?> clazz,
            String methodName, JsonArray args) {
        assert instance != null;
        Collection<Method> methods = Stream.of(clazz.getDeclaredMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> method
                        .isAnnotationPresent(EventHandler.class))
                .collect(Collectors.toList());
        if (methods.size() > 1) {
            StringBuilder builder = new StringBuilder("Class '");
            builder.append(instance.getClass());
            builder.append(
                    "' contains several event handler method with the same name '");
            builder.append(methodName).append("'");
            throw new IllegalStateException(builder.toString());
        } else if (methods.size() == 1) {
            invokeMethod(instance, methodName, methods.iterator().next(), args);
        } else if (!Component.class.equals(clazz)) {
            invokeMethod(instance, clazz.getSuperclass(), methodName, args);
        } else {
            StringBuilder builder = new StringBuilder("Neither class '");
            builder.append(instance.getClass());
            builder.append(
                    "' nor its super classes declare event handler method '");
            builder.append(methodName).append("'");
            throw new IllegalStateException(builder.toString());
        }
    }

    private static void invokeMethod(Component instance, String methodName,
            Method method, JsonArray args) {
        try {
            method.setAccessible(true);
            method.invoke(instance, decodeArgs(method, args));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Logger.getLogger(ServerRpcHandler.class.getName()).log(Level.FINE,
                    null, e);
            throw new RuntimeException(e.getCause());
        }
    }

    private static Object[] decodeArgs(Method method, JsonArray args) {
        if (args.length() == 0) {
            return new Object[0];
        }
        if (args.length() < method.getParameterCount()) {
            StringBuilder builder = new StringBuilder(
                    "The number of received values is lesss than arguments length in the method '");
            builder.append(method.getName());
            builder.append("' declared in '");
            builder.append(method.getDeclaringClass());
            throw new IllegalArgumentException(builder.toString());
        }
        List<Object> decoded = new ArrayList<>(method.getParameterCount());
        boolean hasVarargs = args.length() != method.getParameterCount();
        int argsCount = hasVarargs ? method.getParameterCount() - 1
                : method.getParameterCount();
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < argsCount; i++) {
            Class<?> type = types[i];
            decoded.add(decodeArg(method, type, i, args.get(i)));
        }
        if (hasVarargs) {
            Class<?> type = types[types.length - 1];
            if (!type.isArray()) {
                StringBuilder builder = new StringBuilder(
                        "The number of received values is greater than arguments length in the method '");
                builder.append(method.getName());
                builder.append("' declared in '");
                builder.append(method.getDeclaringClass());
                builder.append(
                        " and the last argument of the method has type '");
                builder.append(type);
                builder.append(" which is not vararg or has an array type.");
                throw new IllegalArgumentException(builder.toString());
            }
            JsonArray rest = Json.createArray();
            int newIndex = 0;
            for (int i = method.getParameterCount() - 1; i < args
                    .length(); i++, newIndex++) {
                JsonValue value = args.get(i);
                rest.set(newIndex, value);
            }
            decoded.add(decodeArray(method, type,
                    method.getParameterCount() - 1, rest));
        }
        return decoded.toArray(new Object[method.getParameterCount()]);
    }

    private static Object decodeArg(Method method, Class<?> type, int index,
            JsonValue argValue) {
        if (type.isPrimitive()
                && (argValue == null || argValue.getType() == JsonType.NULL)) {
            StringBuilder builder = new StringBuilder(
                    "The 'null' value is received for ");
            builder.append(index);
            builder.append("-th parameter which refers to primitive type ");
            builder.append(type);
            builder.append(" in the method '");
            builder.append(method.getName());
            builder.append(
                    "' defined in the class " + method.getDeclaringClass());
            builder.append(index);
            throw new IllegalArgumentException(builder.toString());
        } else if (type.isArray()) {
            return decodeArray(method, type, index, argValue);
        } else {
            Class<?> convertedType = ReflectTools.convertPrimitiveType(type);
            if (!JsonCodec.canEncodeWithoutTypeInfo(convertedType)) {
                StringBuilder builder = new StringBuilder("Class ");
                builder.append(method.getDeclaringClass());
                builder.append(" has the method '");
                builder.append(method.getName());
                builder.append("' whose ");
                builder.append(index);
                builder.append("-th parameter refers to unsupported type ");
                builder.append(type);
                throw new IllegalArgumentException(builder.toString());
            }
            return JsonCodec.decodeAs(argValue, convertedType);
        }
    }

    private static Object decodeArray(Method method, Class<?> type, int index,
            JsonValue argValue) {
        if (argValue.getType() != JsonType.ARRAY) {
            StringBuilder builder = new StringBuilder("Class ");
            builder.append(method.getDeclaringClass());
            builder.append(" has the method '");
            builder.append(method.getName());
            builder.append("' whose ");
            builder.append(index);
            builder.append("-th parameter refers to the array type ");
            builder.append(type);
            builder.append(" but received value is not an array, its type is ");
            builder.append(argValue.getType());
            throw new IllegalArgumentException(builder.toString());
        }
        Class<?> componentType = type.getComponentType();
        JsonArray array = (JsonArray) argValue;
        Object result = Array.newInstance(componentType, array.length());
        for (int i = 0; i < array.length(); i++) {
            Array.set(result, i,
                    decodeArg(method, componentType, index, array.get(i)));
        }
        return result;
    }

    /**
     * Checks that the version reported by the client (widgetset) matches that
     * of the server.
     *
     * @param widgetsetVersion
     *            the widget set version reported by the client or null
     */
    private void checkWidgetsetVersion(String widgetsetVersion) {
        if (widgetsetVersion == null) {
            // Only check when the widgetset version is reported. It is reported
            // in the first UIDL request (not the initial request as it is a
            // plain GET /)
            return;
        }

        if (!Version.getFullVersion().equals(widgetsetVersion)) {
            getLogger().warning(String.format(Constants.WIDGETSET_MISMATCH_INFO,
                    Version.getFullVersion(), widgetsetVersion));
        }
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

        for (int i = 0; i < invocationsData.length(); i++) {
            JsonObject invocationJson = invocationsData.getObject(i);
            String type = invocationJson.getString(JsonConstants.RPC_TYPE);
            assert type != null;
            if (JsonConstants.RPC_TYPE_PROPERTY_SYNC.equals(type)) {
                // Handle these before any RPC
                handlePropertySync(ui, invocationJson);
            }
        }

        for (int i = 0; i < invocationsData.length(); i++) {
            JsonObject invocationJson = invocationsData.getObject(i);
            String type = invocationJson.getString(JsonConstants.RPC_TYPE);
            assert type != null;

            switch (type) {
            case JsonConstants.RPC_TYPE_EVENT:
                handleEventInvocation(ui, invocationJson);
                break;
            case JsonConstants.RPC_TYPE_PROPERTY_SYNC:
                // Handled above
                break;
            case JsonConstants.RPC_TYPE_NAVIGATION:
                handleNavigation(ui, invocationJson);
                break;
            case JsonConstants.RPC_TYPE_TEMPLATE_EVENT:
                handleTemplateEventHandler(ui, invocationJson);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported event type: " + type);
            }
        }
    }

    private static void handleTemplateEventHandler(UI ui,
            JsonObject invocationJson) {
        assert invocationJson
                .hasKey(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);

        StateNode node = getNode(ui, invocationJson);
        if (node == null) {
            return;
        }
        String methodName = invocationJson
                .getString(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME);
        if (methodName == null) {
            throw new IllegalArgumentException(
                    "Event handler method name may not be null");
        }
        JsonValue args = invocationJson
                .get(JsonConstants.RPC_TEMPLATE_EVENT_ARGS);
        if (args == null) {
            throw new IllegalArgumentException(
                    "Event handler argument values may not be null");
        }
        if (args.getType() != JsonType.ARRAY) {
            throw new IllegalArgumentException(
                    "Incorrect type for method arguments :" + args.getClass());
        }
        assert node.hasFeature(ComponentMapping.class);
        Optional<Component> component = node.getFeature(ComponentMapping.class)
                .getComponent();
        if (!component.isPresent()) {
            throw new IllegalStateException(
                    "Unable to handle RPC template event JSON message: "
                            + "there is no component available for the target node.");
        }

        invokeMethod(component.get(), component.get().getClass(), methodName,
                (JsonArray) args);
    }

    private static void handleNavigation(UI ui, JsonObject invocationJson) {
        History history = ui.getPage().getHistory();

        HistoryStateChangeHandler historyStateChangeHandler = history
                .getHistoryStateChangeHandler();
        if (historyStateChangeHandler != null) {
            JsonValue state = invocationJson
                    .get(JsonConstants.RPC_NAVIGATION_STATE);
            String location = invocationJson
                    .getString(JsonConstants.RPC_NAVIGATION_LOCATION);

            HistoryStateChangeEvent event = new HistoryStateChangeEvent(history,
                    state, location);
            historyStateChangeHandler.onHistoryStateChange(event);
        }
    }

    private static void handlePropertySync(UI ui, JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_NODE);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY);
        assert invocationJson.hasKey(JsonConstants.RPC_PROPERTY_VALUE);

        StateNode node = getNode(ui, invocationJson);
        if (node == null) {
            return;
        }
        String property = invocationJson.getString(JsonConstants.RPC_PROPERTY);
        Serializable value = JsonCodec.decodeWithoutTypeInfo(
                invocationJson.get(JsonConstants.RPC_PROPERTY_VALUE));
        node.getFeature(ElementPropertyMap.class).setProperty(property, value,
                false);

    }

    private static int getNodeId(JsonObject invocationJson) {
        return (int) invocationJson.getNumber(JsonConstants.RPC_NODE);
    }

    private static StateNode getNode(UI ui, JsonObject invocationJson) {
        StateNode node = ui.getInternals().getStateTree()
                .getNodeById(getNodeId(invocationJson));

        if (node == null) {
            getLogger().warning("Got an RPC for non-existent node: "
                    + getNodeId(invocationJson));
            return null;
        }

        if (!node.isAttached()) {
            getLogger().warning("Got an RPC for detached node: "
                    + getNodeId(invocationJson));
            return null;
        }
        return node;

    }

    private static void handleEventInvocation(UI ui,
            JsonObject invocationJson) {
        assert invocationJson.hasKey(JsonConstants.RPC_NODE);
        assert invocationJson.hasKey(JsonConstants.RPC_EVENT_TYPE);

        StateNode node = getNode(ui, invocationJson);
        if (node == null) {
            return;
        }

        String eventType = invocationJson
                .getString(JsonConstants.RPC_EVENT_TYPE);

        JsonObject eventData = invocationJson
                .getObject(JsonConstants.RPC_EVENT_DATA);
        if (eventData == null) {
            eventData = Json.createObject();
        }

        DomEvent event = new DomEvent(Element.get(node), eventType, eventData);

        node.getFeature(ElementListenerMap.class).fireEvent(event);
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

    private static final Logger getLogger() {
        return Logger.getLogger(ServerRpcHandler.class.getName());
    }

}
