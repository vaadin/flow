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

import com.vaadin.client.Registry;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.util.ClientJsonCodec;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Handles creating and sending messages to the server using
 * {@link ServerRpcQueue}.
 */
public class ServerConnector {

    private final Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public ServerConnector(Registry registry) {
        this.registry = registry;
    }

    /**
     * Sends a navigation message to server.
     *
     * @param location
     *            the relative location of the navigation
     * @param stateObject
     *            the state object or <code>null</code> if none applicable
     */
    public void sendNavigationMessage(String location, Object stateObject) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_NAVIGATION);
        message.put(JsonConstants.RPC_NAVIGATION_LOCATION, location);
        if (stateObject != null) {
            JsonValue stateJson = ClientJsonCodec
                    .encodeWithoutTypeInfo(stateObject);
            message.put(JsonConstants.RPC_NAVIGATION_STATE, stateJson);
        }
        sendMessage(message);
    }

    /**
     * Sends an event message to the server.
     *
     * @param node
     *            the node that listened to the event
     * @param eventType
     *            the type of event
     * @param eventData
     *            extra data associated with the event
     */
    public void sendEventMessage(StateNode node, String eventType,
            JsonObject eventData) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_EVENT_TYPE, eventType);

        if (eventData != null) {
            message.put(JsonConstants.RPC_EVENT_DATA, eventData);
        }

        sendMessage(message);
    }

    /**
     * Sends a template event message to the server.
     * 
     * @param node
     *            the node that listened to the event
     * @param methodName
     *            the event handler method name to execute on the server side
     * @param argsArray
     *            the arguments array for the method
     */
    public void sendTemplateEventMessage(StateNode node, String methodName,
            JsonArray argsArray) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_TYPE_TEMPLATE_EVENT);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME, methodName);
        sendMessage(message);
    }

    /**
     * Sends a property sync message to the server.
     *
     * @param node
     *            the node containing the property
     * @param property
     *            the property name
     * @param value
     *            the property value
     */
    public void sendPropertySyncMessage(StateNode node, String property,
            Object value) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_TYPE_PROPERTY_SYNC);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_PROPERTY, property);
        message.put(JsonConstants.RPC_PROPERTY_VALUE,
                ClientJsonCodec.encodeWithoutTypeInfo(value));

        sendMessage(message);
    }

    private void sendMessage(JsonObject message) {
        ServerRpcQueue rpcQueue = registry.getServerRpcQueue();
        rpcQueue.add(message);
        rpcQueue.flush();
    }

}
