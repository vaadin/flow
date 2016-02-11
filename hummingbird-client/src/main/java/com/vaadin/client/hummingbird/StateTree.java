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
package com.vaadin.client.hummingbird;

import com.vaadin.client.Registry;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.hummingbird.shared.Namespaces;
import com.vaadin.shared.JsonConstants;

import elemental.events.Event;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * A client-side representation of a server-side state tree.
 *
 * @since
 * @author Vaadin Ltd
 */
public class StateTree {
    // Double instead of Integer since GWT 2.8 doesn't box doubles
    private final JsMap<Double, StateNode> idToNode = JsCollections.map();

    private final StateNode rootNode = new StateNode(1, this);

    private final Registry registry;

    private JsMap<Integer, String> namespaceDebugName;

    /**
     * Creates a new instance connected to the given registry.
     *
     * @param registry
     *            the global registry
     */
    public StateTree(Registry registry) {
        this.registry = registry;
        registerNode(rootNode);
    }

    /**
     * Registers a node with this tree.
     *
     * @param node
     *            the node to regsiter
     */
    public final void registerNode(StateNode node) {
        assert node != null;
        assert node.getTree() == this;
        assert !node.isUnregistered() : "Can't re-register a node";

        Double key = getKey(node);
        assert !idToNode.has(key) : "Node " + key + " is already registered";

        idToNode.set(key, node);
    }

    private static Double getKey(StateNode node) {
        return Double.valueOf(node.getId());
    }

    /**
     * Unregisters a node from this tree. Once the node has been unregistered,
     * it can't be registered again.
     *
     * @param node
     *            the node to unregister
     */
    public void unregisterNode(StateNode node) {
        assert assertValidNode(node);
        assert node != rootNode : "Root node can't be unregistered";

        idToNode.delete(getKey(node));
        node.unregister();
    }

    /**
     * Verifies that the provided node is not null and properly registered with
     * this state tree.
     *
     * @param node
     *            the node to test
     * @return always <code>true</code>, for use with the <code>assert</code>
     *         keyword
     */
    private boolean assertValidNode(StateNode node) {
        assert node != null : "Node is null";
        assert node.getTree() == this : "Node is not created for this tree";
        assert node == getNode(
                node.getId()) : "Node id is not registered with this tree";

        return true;
    }

    /**
     * Finds the node with the given id.
     *
     * @param id
     *            the id
     * @return the node with the given id, or <code>null</code> if no such node
     *         is registered.
     */
    public StateNode getNode(int id) {
        Double key = Double.valueOf(id);

        return idToNode.get(key);
    }

    /**
     * Gets the root node of this tree.
     *
     * @return the root node
     */
    public StateNode getRootNode() {
        return rootNode;
    }

    /**
     * Sends a DOM event to the server.
     *
     * @param node
     *            the node that listened to the event
     * @param event
     *            the fired event
     */
    public void sendEventToServer(StateNode node, Event event) {
        assert assertValidNode(node);

        JsonObject message = Json.createObject();
        message.put(JsonConstants.RPC_TYPE, JsonConstants.RPC_TYPE_EVENT);
        message.put(JsonConstants.RPC_NODE, node.getId());
        message.put(JsonConstants.RPC_EVENT_TYPE, event.getType());

        ServerRpcQueue rpcQueue = registry.getServerRpcQueue();
        rpcQueue.add(message);
        rpcQueue.flush();
    }

    /**
     * Returns a human readable string for the name space with the given id.
     *
     * @param id
     *            the namespace id
     * @return a human readable string describing the namespace
     */
    public String getNamespaceDebugName(int id) {
        if (namespaceDebugName == null) {
            namespaceDebugName = JsCollections.map();
            namespaceDebugName.set(Namespaces.ELEMENT_DATA, "elementData");
            namespaceDebugName.set(Namespaces.ELEMENT_PROPERTIES,
                    "elementProperties");
            namespaceDebugName.set(Namespaces.ELEMENT_ATTRIBUTES,
                    "elementAttributes");
            namespaceDebugName.set(Namespaces.ELEMENT_CHILDREN,
                    "elementChildren");
            namespaceDebugName.set(Namespaces.ELEMENT_LISTENERS,
                    "elementListeners");
            namespaceDebugName.set(Namespaces.UI_PUSHCONFIGURATION,
                    "pushConfiguration");
            namespaceDebugName.set(Namespaces.UI_PUSHCONFIGURATION_PARAMETERS,
                    "pushConfigurationParameters");
        }
        if (namespaceDebugName.has(id)) {
            return namespaceDebugName.get(id);
        } else {
            return "Unknown namespace: " + id;
        }
    }

}
