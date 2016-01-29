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

import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;
import com.vaadin.client.hummingbird.collection.JsMap.ForEachCallback;

/**
 * A client-side representation of a server-side state node.
 *
 * @since
 * @author Vaadin Ltd
 */
public class StateNode {
    private final StateTree tree;
    private final int id;

    private final JsMap<Double, AbstractNamespace> namespaces = JsCollections
            .map();

    /**
     * Creates a new state node
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
     * Gets the state tree that this node belongs to
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
}
