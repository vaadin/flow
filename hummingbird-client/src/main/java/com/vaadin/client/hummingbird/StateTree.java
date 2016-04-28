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
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.hummingbird.shared.NodeFeatures;

import elemental.json.JsonObject;

/**
 * A client-side representation of a server-side state tree.
 *
 * @author Vaadin Ltd
 */
public class StateTree {
    // Double instead of Integer since GWT 2.8 doesn't box doubles
    private final JsMap<Double, StateNode> idToNode = JsCollections.map();

    private final StateNode rootNode = new StateNode(1, this);

    private final Registry registry;

    private JsMap<Integer, String> nodeFeatureDebugName;

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
     * Sends an event to the server.
     *
     * @param node
     *            the node that listened to the event
     * @param eventType
     *            the type of event
     * @param eventData
     *            extra data associated with the event
     */
    public void sendEventToServer(StateNode node, String eventType,
            JsonObject eventData) {
        assert assertValidNode(node);
        registry.getServerConnector().sendEventMessage(node, eventType,
                eventData);
    }

    /**
     * Sends a property sync to the server.
     *
     * @param node
     *            the node containing the property
     * @param property
     *            the property name
     * @param value
     *            the property value
     */
    public void sendPropertySyncToServer(StateNode node, String property,
            Object value) {
        assert assertValidNode(node);
        registry.getServerConnector().sendPropertySyncMessage(node, property,
                value);
    }

    /**
     * Gets the {@link Registry} that this state tree belongs to.
     *
     * @return the registry of this tree, not <code>null</code>
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * Returns a human readable string for the name space with the given id.
     *
     * @param id
     *            the node feature id
     * @return a human readable string describing the node feature
     */
    public String getFeatureDebugName(int id) {
        if (nodeFeatureDebugName == null) {
            nodeFeatureDebugName = JsCollections.map();
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_DATA, "elementData");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_PROPERTIES,
                    "elementProperties");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_ATTRIBUTES,
                    "elementAttributes");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_CHILDREN,
                    "elementChildren");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_LISTENERS,
                    "elementListeners");
            nodeFeatureDebugName.set(NodeFeatures.UI_PUSHCONFIGURATION,
                    "pushConfiguration");
            nodeFeatureDebugName.set(
                    NodeFeatures.UI_PUSHCONFIGURATION_PARAMETERS,
                    "pushConfigurationParameters");
            nodeFeatureDebugName.set(NodeFeatures.TEXT_NODE, "textNode");
            nodeFeatureDebugName.set(NodeFeatures.POLL_CONFIGURATION,
                    "pollConfiguration");
            nodeFeatureDebugName.set(
                    NodeFeatures.RECONNECT_DIALOG_CONFIGURATION,
                    "reconnectDialogConfiguration");
            nodeFeatureDebugName.set(
                    NodeFeatures.LOADING_INDICATOR_CONFIGURATION,
                    "loadingIndicatorConfiguration");
            nodeFeatureDebugName.set(NodeFeatures.CLASS_LIST, "classList");
            nodeFeatureDebugName.set(NodeFeatures.DEPENDENCY_LIST,
                    "dependencyList");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_STYLE_PROPERTIES,
                    "elementStyleProperties");
            nodeFeatureDebugName.set(NodeFeatures.TEMPLATE, "template");
            nodeFeatureDebugName.set(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS,
                    "synchronizedPropertyEvents");
            nodeFeatureDebugName.set(NodeFeatures.TEMPLATE_OVERRIDES,
                    "templateOverrides");
            nodeFeatureDebugName.set(NodeFeatures.OVERRIDE_DATA,
                    "overideNodeData");
        }
        if (nodeFeatureDebugName.has(id)) {
            return nodeFeatureDebugName.get(id);
        } else {
            return "Unknown node feature: " + id;
        }
    }

}
