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

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;
import com.vaadin.client.hummingbird.collection.JsSet;
import com.vaadin.client.hummingbird.namespace.AbstractNamespace;
import com.vaadin.client.hummingbird.namespace.ListNamespace;
import com.vaadin.client.hummingbird.namespace.MapNamespace;
import com.vaadin.hummingbird.shared.Namespaces;

import elemental.dom.Element;
import elemental.events.EventRemover;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * A client-side representation of a server-side state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class StateNode {
    private final StateTree tree;
    private final int id;

    private boolean unregistered = false;

    private final JsMap<Double, AbstractNamespace> namespaces = JsCollections
            .map();

    private final JsSet<NodeUnregisterListener> unregisterListeners = JsCollections
            .set();

    private Element element;

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
     * Gets the list namespace with the given id. Creates a new namespace if one
     * doesn't already exist.
     *
     * @param id
     *            the id of the namespace
     * @return the list namespace with the given id
     */
    public ListNamespace getListNamespace(int id) {
        Double key = Double.valueOf(id);
        AbstractNamespace namespace = namespaces.get(key);
        if (namespace == null) {
            namespace = new ListNamespace(id, this);
            namespaces.set(key, namespace);
        }

        assert namespace instanceof ListNamespace;

        return (ListNamespace) namespace;
    }

    /**
     * Gets the map namespace with the given id. Creates a new namespace if one
     * doesn't already exist.
     *
     * @param id
     *            the id of the namespace
     * @return the map namespace with the given id
     */
    public MapNamespace getMapNamespace(int id) {
        Double key = Double.valueOf(id);
        AbstractNamespace namespace = namespaces.get(key);
        if (namespace == null) {
            namespace = new MapNamespace(id, this);
            namespaces.set(key, namespace);
        }

        assert namespace instanceof MapNamespace;

        return (MapNamespace) namespace;
    }

    /**
     * Iterates all namespaces in this node.
     *
     * @param callback
     *            the callback to invoke for each namespace
     */
    public void forEachNamespace(
            ForEachCallback<Double, AbstractNamespace> callback) {
        namespaces.forEach(callback);
    }

    /**
     * Gets a JSON object representing the contents of this namespace. Only
     * intended for debugging purposes.
     *
     * @return a JSON representation
     */
    public JsonObject getDebugJson() {
        JsonObject object = WidgetUtil.createJsonObjectWithoutPrototype();

        forEachNamespace((ns, nsId) -> {
            JsonValue json = ns.getDebugJson();
            if (json != null) {
                object.put(getNamespaceDebugName(nsId.intValue()), json);
            }
        });

        return object;
    }

    private static String getNamespaceDebugName(int id) {
        switch (id) {
        case Namespaces.ELEMENT_DATA:
            return "elementData";
        case Namespaces.ELEMENT_PROPERTIES:
            return "elementProperties";
        case Namespaces.ELEMENT_ATTRIBUTES:
            return "elementAttributes";
        case Namespaces.ELEMENT_CHILDREN:
            return "elementChildren";
        default:
            return Integer.toString(id);
        }
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
        copy.forEach(l -> l.onUnregsiter(event));
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
     * Gets the element associated with this node.
     *
     * @return the element, or <code>null</code> if no element has been
     *         associated with this state node
     */
    public Element getElement() {
        return element;
    }

    /**
     * Sets the element associated with this node.
     *
     * @param element
     *            the associated element
     */
    public void setElement(Element element) {
        assert this.element == null
                || element == null : "Node already has an element";

        this.element = element;
    }
}
