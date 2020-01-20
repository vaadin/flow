/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.function.Function;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsMap.ForEachCallback;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.client.flow.nodefeature.NodeFeature;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;

import elemental.dom.Node;
import elemental.events.EventRemover;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A client-side representation of a server-side state node.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StateNode {
    private final StateTree tree;
    private final int id;

    private StateNode parent;

    private boolean unregistered = false;

    private final JsMap<Double, NodeFeature> features = JsCollections.map();

    private final JsSet<NodeUnregisterListener> unregisterListeners = JsCollections
            .set();

    private final JsSet<Function<StateNode, Boolean>> domNodeSetListeners = JsCollections
            .set();

    private final JsMap<Class<?>, Object> nodeData = JsCollections.map();

    private Node domNode;

    /**
     * Creates a new state node.
     *
     * @param id
     *            the id of the node
     * @param tree
     *            the state tree that the node belongs to
     */
    public StateNode(int id, StateTree tree) {
        this.id = id;
        this.tree = tree;
    }

    /**
     * Gets the state tree that this node belongs to.
     *
     * @return the state tree
     */
    public StateTree getTree() {
        return tree;
    }

    /**
     * Gets the id of this state node.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the node list with the given id. Creates a new node list if one
     * doesn't already exist.
     *
     * @param id
     *            the id of the list
     * @return the list with the given id
     */
    public NodeList getList(int id) {
        Double key = Double.valueOf(id);
        NodeFeature feature = features.get(key);
        if (feature == null) {
            feature = new NodeList(id, this);
            features.set(key, feature);
        }

        assert feature instanceof NodeList;

        return (NodeList) feature;
    }

    /**
     * Gets the node map with the given id. Creates a new map if one doesn't
     * already exist.
     *
     * @param id
     *            the id of the map
     * @return the map with the given id
     */
    public NodeMap getMap(int id) {
        Double key = Double.valueOf(id);
        NodeFeature feature = features.get(key);
        if (feature == null) {
            feature = new NodeMap(id, this);
            features.set(key, feature);
        }

        assert feature instanceof NodeMap;

        return (NodeMap) feature;
    }

    /**
     * Checks whether this node has a feature with the given id.
     *
     * @param id
     *            the id of the feature
     * @return <code>true</code> if this node has the given feature; otherwise
     *         <code>false</code>
     */
    public boolean hasFeature(int id) {
        return features.has(Double.valueOf(id));
    }

    /**
     * Iterates all features in this node.
     *
     * @param callback
     *            the callback to invoke for each feature
     */
    public void forEachFeature(ForEachCallback<Double, NodeFeature> callback) {
        features.forEach(callback);
    }

    /**
     * Gets a JSON object representing the contents of this node. Only intended
     * for debugging purposes.
     *
     * @return a JSON representation
     */
    public JsonObject getDebugJson() {
        JsonObject object = WidgetUtil.createJsonObjectWithoutPrototype();

        forEachFeature((feature, featureId) -> {
            JsonValue json = feature.getDebugJson();
            if (json != null) {
                object.put(tree.getFeatureDebugName(featureId.intValue()),
                        json);
            }
        });

        return object;
    }

    /**
     * Checks whether this node has been unregistered.
     *
     *
     * @see StateTree#unregisterNode(StateNode)
     *
     * @return <code>true</code> if this node has been unregistered;
     *         <code>false</code> if the node is still registered
     */
    public boolean isUnregistered() {
        return unregistered;
    }

    /**
     * Unregisters this node, causing all registered node unregister listeners
     * to be notified.
     *
     * @see #addUnregisterListener(NodeUnregisterListener)
     */
    public void unregister() {
        assert tree.getNode(
                id) == null : "Node should no longer be findable from the tree";

        assert !unregistered : "Node is already unregistered";

        unregistered = true;

        NodeUnregisterEvent event = new NodeUnregisterEvent(this);

        JsSet<NodeUnregisterListener> copy = JsCollections
                .set(unregisterListeners);
        copy.forEach(l -> l.onUnregister(event));
        // Don't refer to the listeners which won't be ever used again
        unregisterListeners.clear();
    }

    /**
     * Adds a listener that will be notified when this node is unregistered.
     *
     * @param listener
     *            the node unregister listener to add
     * @return an event remover that can be used for removing the added listener
     */
    public EventRemover addUnregisterListener(NodeUnregisterListener listener) {
        unregisterListeners.add(listener);

        return () -> unregisterListeners.delete(listener);
    }

    /**
     * Gets the DOM node associated with this state node.
     *
     * @return the DOM node, or <code>null</code> if no DOM node has been
     *         associated with this state node
     */
    public Node getDomNode() {
        return domNode;
    }

    /**
     * Sets the DOM node associated with this state node.
     *
     * @param node
     *            the associated DOM node
     */
    public void setDomNode(Node node) {
        assert domNode == null
                || node == null : "StateNode already has a DOM node";

        domNode = node;
        JsSet<Function<StateNode, Boolean>> copy = JsCollections
                .set(domNodeSetListeners);
        copy.forEach(listener -> {
            if (listener.apply(this) == Boolean.TRUE) {
                domNodeSetListeners.delete(listener);
            }
        });
    }

    /**
     * Adds a listener to get a notification when the DOM Node is set for this
     * {@link StateNode}.
     * <p>
     * The listener return value is used to decide whether the listener should
     * be removed immediately if it returns {@code true}.
     *
     * @param listener
     *            listener to add
     * @return an event remover that can be used for removing the added listener
     */
    public EventRemover addDomNodeSetListener(
            Function<StateNode, Boolean> listener) {
        domNodeSetListeners.add(listener);
        return () -> domNodeSetListeners.delete(listener);
    }

    /**
     * Get the parent {@link StateNode} if set.
     *
     * @return parent state node
     */
    public StateNode getParent() {
        return parent;
    }

    /**
     * Set the parent {@link StateNode} for this node.
     *
     * @param parent
     *            the parent state node
     */
    public void setParent(StateNode parent) {
        this.parent = parent;
    }

    /**
     * Stores the {@code object} in the {@link StateNode} instance.
     * <p>
     * The {@code object} may represent any kind of data. This data can be
     * retrieved later on via the {@link #getNodeData(Class)} providing the
     * class of the object. So make sure you are using some custom type for your
     * data to avoid clash with other types.
     *
     * @see #getNodeData(Class)
     *
     * @param object
     *            the object to store
     * @param <T>
     *            the type of the node data to set
     */
    public <T> void setNodeData(T object) {
        nodeData.set(object.getClass(), object);
    }

    /**
     * Gets the object previously stored by the {@link #setNodeData(Object)} by
     * its type.
     * <p>
     * If there is no stored object with the given type then the method returns
     * {@code null}.
     *
     * @param clazz
     *            the type of the object to get
     * @param <T>
     *            the type of the node data to get
     * @return the object by its {@code clazz}
     */
    public <T> T getNodeData(Class<T> clazz) {
        return (T) nodeData.get(clazz);
    }

    /**
     * Removes the {@code object} from the stored data.
     * 
     * @param object
     *            the object to remove
     * @param <T>
     *            the type of the object to remove
     */
    public <T> void clearNodeData(T object) {
        nodeData.delete(object.getClass());
    }
}
