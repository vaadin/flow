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

package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.namespace.MapNamespace;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Change describing a changed value in a map namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public class MapPutChange extends NamespaceChange {

    private final String key;
    private final Object value;

    /**
     * Creates a new put change.
     *
     * @param namespace
     *            the changed namespace
     * @param key
     *            the key of the changed value
     * @param value
     *            the new value
     */
    public MapPutChange(MapNamespace namespace, String key, Object value) {
        super(namespace);
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key of the change.
     * 
     * @return the key of the change
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the new value.
     * 
     * @return the new value
     */
    public Object getValue() {
        return value;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "put");

        json.put("key", key);

        super.populateJson(json);

        if (value instanceof StateNode) {
            StateNode node = (StateNode) value;
            json.put("nodeValue", Json.create(node.getId()));
        } else {
            json.put("value", JsonCodec.encodePrimitiveValue(value));
        }
    }
}
