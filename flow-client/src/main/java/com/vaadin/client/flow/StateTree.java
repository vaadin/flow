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
package com.vaadin.client.flow;

import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * A client-side representation of a server-side state tree.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StateTree {
    // Double instead of Integer since GWT 2.8 doesn't box doubles
    private final JsMap<Double, StateNode> idToNode = JsCollections.map();

    private final StateNode rootNode = new StateNode(1, this);

    private final Registry registry;

    private JsMap<Integer, String> nodeFeatureDebugName;

    private boolean updateInProgress;

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
     * Mark this tree as being updated.
     *
     * @param updateInProgress
     *            <code>true</code> if the tree is being updated,
     *            <code>false</code> if not
     * @see #isUpdateInProgress()
     */
    public void setUpdateInProgress(boolean updateInProgress) {
        assert this.updateInProgress != updateInProgress : "Inconsistent state tree updating status, expected "
                + (updateInProgress ? "no " : "") + " updates in progress.";
        this.updateInProgress = updateInProgress;

        getRegistry().getInitialPropertiesHandler().flushPropertyUpdates();
    }

    /**
     * Returns whether this tree is currently being updated by
     * {@link TreeChangeProcessor#processChanges(StateTree, JsonArray)}.
     *
     * @return <code>true</code> if being updated, <code>false</code> if not
     */
    public boolean isUpdateInProgress() {
        return updateInProgress;
    }

    /**
     * Registers a node with this tree.
     *
     * @param node
     *            the node to register
     */
    public final void registerNode(StateNode node) {
        assert node != null;
        assert node.getTree() == this;
        assert !node.isUnregistered() : "Can't re-register a node";

        Double key = getKey(node);
        assert !idToNode.has(key) : "Node " + key + " is already registered";

        idToNode.set(key, node);

        if (isUpdateInProgress()) {
            getRegistry().getInitialPropertiesHandler().nodeRegistered(node);
        }
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
     * Validates that the provided node is not null and is properly registered
     * for this state tree.
     * <p>
     * Logs a warning if there was a problem with the node.
     *
     * @param node
     *            node to test
     * @return node is valid
     */
    private boolean isValidNode(StateNode node) {
        boolean isValid = true;
        if (node == null) {
            Console.warn("Node is null");
            isValid = false;
        } else if (!node.getTree().equals(this)) {
            Console.warn("Node is not created for this tree");
            isValid = false;
        } else if (!node.equals(getNode(node.getId()))) {
            Console.warn("Node id is not registered with this tree");
            isValid = false;
        }

        return isValid;
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
        if (isValidNode(node)) {
            registry.getServerConnector().sendEventMessage(node, eventType,
                    eventData);
        }
    }

    /**
     * Sends a map property sync to the server.
     *
     * @param property
     *            the property that should have its value synced to the server,
     *            not <code>null</code>
     */
    public void sendNodePropertySyncToServer(MapProperty property) {
        assert property != null;

        NodeMap nodeMap = property.getMap();
        StateNode node = nodeMap.getNode();

        if (getRegistry().getInitialPropertiesHandler()
                .handlePropertyUpdate(property) || !isValidNode(node)) {
            return;
        }

        registry.getServerConnector().sendNodeSyncMessage(node, nodeMap.getId(),
                property.getName(), property.getValue());
    }

    /**
     * Sends a request to call server side method with {@code methodName} using
     * {@code argsArray} as argument values.
     * <p>
     * In cases when the state tree has been changed and we receive a delayed or
     * deferred template event the event is just ignored.
     *
     * @param node
     *            the node referring to the server side instance containing the
     *            method
     * @param methodName
     *            the method name
     * @param argsArray
     *            the arguments array for the method
     * @param promiseId
     *            the promise id to use for getting the result back, or -1 if no
     *            result is expected
     */
    public void sendTemplateEventToServer(StateNode node, String methodName,
            JsArray<?> argsArray, int promiseId) {
        if (isValidNode(node)) {
            JsonArray array = WidgetUtil.crazyJsCast(argsArray);
            registry.getServerConnector().sendTemplateEventMessage(node,
                    methodName, array, promiseId);
        }
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
        assert assertValidNode(parent);

        registry.getServerConnector().sendExistingElementAttachToServer(parent,
                requestedId, assignedId, tagName, index);
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
        assert assertValidNode(parent);

        registry.getServerConnector().sendExistingElementWithIdAttachToServer(
                parent, requestedId, assignedId, id);
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
     * Returns the visibility state of the {@code node}.
     *
     * @param node
     *            the node whose visibility is tested
     * @return {@code true} is the node is visible, {@code false} otherwise
     */
    public boolean isVisible(StateNode node) {
        if (!node.hasFeature(NodeFeatures.ELEMENT_DATA)) {
            return true;
        }
        NodeMap visibilityMap = node.getMap(NodeFeatures.ELEMENT_DATA);
        Boolean visibility = (Boolean) visibilityMap
                .getProperty(NodeProperties.VISIBLE).getValue();

        /*
         * Absence of value or "true" means that the node should be visible. So
         * only "false" means "hide".
         */
        return !Boolean.FALSE.equals(visibility);
    }

    /**
     * Checks whether the {@code node} is active.
     * <p>
     * The node is active if it's visible and all its ancestors are visible.
     *
     * @param node
     *            the node whose activity is tested
     * @return {@code true} is the node is active, {@code false} otherwise
     */
    public boolean isActive(StateNode node) {
        boolean isVisible = isVisible(node);
        if (!isVisible || node.getParent() == null) {
            return isVisible;
        }
        return isActive(node.getParent());
    }

    /**
     * Returns a human readable string for the name space with the given id.
     *
     * @param id
     *            the node feature id
     * @return a human readable string describing the node feature
     */
    String getFeatureDebugName(int id) {
        if (nodeFeatureDebugName == null) {
            nodeFeatureDebugName = JsCollections.map();
            // GWT does not allow to call Class::getFields method, so we cannot
            // use reflection to fill the map automatically
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_DATA, "elementData");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_PROPERTIES,
                    "elementProperties");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_CHILDREN,
                    "elementChildren");
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_ATTRIBUTES,
                    "elementAttributes");
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
            nodeFeatureDebugName.set(NodeFeatures.ELEMENT_STYLE_PROPERTIES,
                    "elementStyleProperties");
            nodeFeatureDebugName.set(NodeFeatures.SYNCHRONIZED_PROPERTIES,
                    "synchronizedProperties");
            nodeFeatureDebugName.set(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS,
                    "synchronizedPropertyEvents");
            nodeFeatureDebugName.set(NodeFeatures.COMPONENT_MAPPING,
                    "componentMapping");
            nodeFeatureDebugName.set(NodeFeatures.TEMPLATE_MODELLIST,
                    "modelList");
            nodeFeatureDebugName.set(NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS,
                    "polymerServerEventHandlers");
            nodeFeatureDebugName.set(NodeFeatures.POLYMER_EVENT_LISTENERS,
                    "polymerEventListenerMap");
            nodeFeatureDebugName.set(NodeFeatures.CLIENT_DELEGATE_HANDLERS,
                    "clientDelegateHandlers");
            nodeFeatureDebugName.set(NodeFeatures.SHADOW_ROOT_DATA,
                    "shadowRootData");
            nodeFeatureDebugName.set(NodeFeatures.SHADOW_ROOT_HOST,
                    "shadowRootHost");
            nodeFeatureDebugName.set(NodeFeatures.ATTACH_EXISTING_ELEMENT,
                    "attachExistingElementFeature");
            nodeFeatureDebugName.set(NodeFeatures.VIRTUAL_CHILDREN,
                    "virtualChildrenList");
            nodeFeatureDebugName.set(NodeFeatures.BASIC_TYPE_VALUE,
                    "basicTypeValue");
        }
        if (nodeFeatureDebugName.has(id)) {
            return nodeFeatureDebugName.get(id);
        } else {
            return "Unknown node feature: " + id;
        }
    }

}
