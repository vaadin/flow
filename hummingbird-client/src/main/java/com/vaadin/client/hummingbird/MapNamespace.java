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
 * A state node namespace that structures data as a map.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapNamespace extends AbstractNamespace {
    private final JsMap<String, MapProperty> properties = JsCollections.map();

    /**
     * Creates a new map namespace.
     *
     * @param id
     *            the id of the namespace
     * @param node
     *            the node of the namespace
     */
    public MapNamespace(int id, StateNode node) {
        super(id, node);
    }

    /**
     * Gets the property with a given name, creating it if necessary.
     *
     * @param name
     *            the name of the property
     * @return the property instance
     */
    public MapProperty getProperty(String name) {
        MapProperty property = properties.get(name);
        if (property == null) {
            property = new MapProperty(name, this);
            properties.set(name, property);
        }

        return property;
    }

    /**
     * Iterates all properties in this namespace.
     *
     * @param callback
     *            the callback to invoke for each property
     */
    public void forEachProperty(ForEachCallback<String, MapProperty> callback) {
        properties.forEach(callback);
    }
}
