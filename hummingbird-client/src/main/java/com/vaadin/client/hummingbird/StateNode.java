/*
 * Copyright 2000-2014 Vaadin Ltd.
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

import elemental.json.Json;
import elemental.json.JsonObject;

public class StateNode {
    private final StateTree tree;
    private final int id;

    private final JsMap<Double, AbstractNamespace> namespaces = JsCollections
            .map();

    public StateNode(int id, StateTree tree) {
        this.id = id;
        this.tree = tree;
    }

    public StateTree getTree() {
        return tree;
    }

    public int getId() {
        return id;
    }

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

    public void forEachNamespace(
            ForEachCallback<Double, AbstractNamespace> callback) {
        namespaces.forEach(callback);
    }

    public JsonObject getDebugJson() {
        JsonObject object = Json.createObject();

        forEachNamespace((ns, id) -> {
            object.put(String.valueOf(id), ns.getDebugJson());
        });

        return object;
    }
}
