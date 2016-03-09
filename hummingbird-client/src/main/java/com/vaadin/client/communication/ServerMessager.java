package com.vaadin.client.communication;

import com.vaadin.client.Registry;
import com.vaadin.client.hummingbird.StateNode;
import com.vaadin.client.hummingbird.util.ClientJsonCodec;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Handles creating and sending messages to server using the
 * {@link ServerRpcQueue}.
 */
public class ServerMessager {

    private final Registry registry;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public ServerMessager(Registry registry) {
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
        JsonObject invocation = Json.createObject();
        invocation.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_POPSTATE);
        invocation.put(JsonConstants.RPC_POPSTATE_LOCATION, location);
        if (stateObject != null) {
            JsonValue stateJson = ClientJsonCodec
                    .encodeWithoutTypeInfo(stateObject);
            invocation.put(JsonConstants.RPC_POPSTATE_STATE, stateJson);
        }

        registry.getServerRpcQueue().add(invocation);
        registry.getServerRpcQueue().flush();
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

        ServerRpcQueue rpcQueue = registry.getServerRpcQueue();
        rpcQueue.add(message);
        rpcQueue.flush();
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

        ServerRpcQueue rpcQueue = registry.getServerRpcQueue();
        rpcQueue.add(message);
        rpcQueue.flush();
    }

}
