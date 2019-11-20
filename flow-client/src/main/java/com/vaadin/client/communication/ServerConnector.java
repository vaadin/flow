/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.util.ClientJsonCodec;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Handles creating and sending messages to the server using
 * {@link ServerRpcQueue}.
 *
 * @since 1.0
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
     * @param routerLinkEvent
     *            <code>true</code> if this event was triggered by interaction
     *            with a router link; <code>false</code> if triggered by history
     *            navigation
     */
    public void sendNavigationMessage(String location, Object stateObject,
            boolean routerLinkEvent) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_NAVIGATION);
        message.put(JsonConstants.RPC_NAVIGATION_LOCATION, location);
        if (stateObject != null) {
            JsonValue stateJson = ClientJsonCodec
                    .encodeWithoutTypeInfo(stateObject);
            message.put(JsonConstants.RPC_NAVIGATION_STATE, stateJson);
        }
        if (routerLinkEvent) {
            // Only presence of key is checked, so use a possibly short value
            message.put(JsonConstants.RPC_NAVIGATION_ROUTERLINK, 1);
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
        sendEventMessage(node.getId(), eventType, eventData);
    }

    /**
     * Sends an event message to the server.
     *
     * @param nodeId
     *            the id of the node that listened to the event
     * @param eventType
     *            the type of event
     * @param eventData
     *            extra data associated with the event
     */
    public void sendEventMessage(int nodeId, String eventType,
            JsonObject eventData) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        message.put(JsonConstants.RPC_NODE, nodeId);
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
     * @param promiseId
     *            the promise id to use for getting the result back, or -1 if no
     *            result is expected
     */
    public void sendTemplateEventMessage(StateNode node, String methodName,
            JsonArray argsArray, int promiseId) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_PUBLISHED_SERVER_EVENT_HANDLER);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_TEMPLATE_EVENT_METHOD_NAME, methodName);
        message.put(JsonConstants.RPC_TEMPLATE_EVENT_ARGS, argsArray);
        if (promiseId != -1) {
            message.put(JsonConstants.RPC_TEMPLATE_EVENT_PROMISE, promiseId);
        }
        sendMessage(message);
    }

    /**
     * Sends a node value sync message to the server.
     *
     * @param node
     *            the node to update
     * @param feature
     *            the id of the node map feature to update
     * @param key
     *            the map key to update
     * @param value
     *            the new value
     */
    public void sendNodeSyncMessage(StateNode node, int feature, String key,
            Object value) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_MAP_SYNC);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_FEATURE, feature);
        message.put(JsonConstants.RPC_PROPERTY, key);
        message.put(JsonConstants.RPC_PROPERTY_VALUE,
                ClientJsonCodec.encodeWithoutTypeInfo(value));

        sendMessage(message);
    }

    /**
     * Sends a data for attach existing element server side callback.
     *
     * @param parent
     *            parent of the node to attach
     * @param requestedId
     *            originally requested id of a server side node
     * @param assignedId
     *            identifier which should be used on the server side for the
     *            element (instead of requestedId)
     * @param tagName
     *            the requested tagName
     * @param index
     *            the index of the element on the server side
     */
    public void sendExistingElementAttachToServer(StateNode parent,
            int requestedId, int assignedId, String tagName, int index) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_ATTACH_EXISTING_ELEMENT);
        message.put(JsonConstants.RPC_NODE, parent.getId());
        message.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        message.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, assignedId);
        message.put(JsonConstants.RPC_ATTACH_TAG_NAME, tagName);
        message.put(JsonConstants.RPC_ATTACH_INDEX, index);

        sendMessage(message);
    }

    /**
     * Sends a data for attach existing element with id server side callback.
     *
     * @param parent
     *            parent of the node to attach
     * @param requestedId
     *            originally requested id of a server side node
     * @param assignedId
     *            identifier which should be used on the server side for the
     *            element (instead of requestedId)
     * @param id
     *            id of requested element
     */
    public void sendExistingElementWithIdAttachToServer(StateNode parent,
            int requestedId, int assignedId, String id) {
        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE,
                JsonConstants.RPC_ATTACH_EXISTING_ELEMENT_BY_ID);
        message.put(JsonConstants.RPC_NODE, parent.getId());
        message.put(JsonConstants.RPC_ATTACH_REQUESTED_ID, requestedId);
        message.put(JsonConstants.RPC_ATTACH_ASSIGNED_ID, assignedId);
        message.put(JsonConstants.RPC_ATTACH_ID, id);

        sendMessage(message);
    }

    /**
     * Sends a return channel message to the server.
     *
     * @param stateNodeId
     *            the id of the state node that owns the channel.
     * @param channelId
     *            the id of the channel.
     * @param arguments
     *            array of arguments passed to the channel, not
     *            <code>null</code>.
     */
    public void sendReturnChannelMessage(int stateNodeId, int channelId,
            JsonArray arguments) {
        JsonObject message = Json.createObject();

        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_CHANNEL);
        message.put(JsonConstants.RPC_NODE, stateNodeId);
        message.put(JsonConstants.RPC_CHANNEL, channelId);
        message.put(JsonConstants.RPC_CHANNEL_ARGUMENTS, arguments);

        sendMessage(message);
    }

    private void sendMessage(JsonObject message) {
        ServerRpcQueue rpcQueue = registry.getServerRpcQueue();
        rpcQueue.add(message);
        rpcQueue.flush();
    }

}
