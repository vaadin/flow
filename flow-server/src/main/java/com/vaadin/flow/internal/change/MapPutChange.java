/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.internal.change;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Change describing a changed value in a map feature.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MapPutChange extends NodeFeatureChange {

    private final String key;
    private final Object value;

    /**
     * Creates a new put change.
     *
     * @param map
     *            the changed map
     * @param key
     *            the key of the changed value
     * @param value
     *            the new value
     */
    public MapPutChange(NodeFeature map, String key, Object value) {
        super(map);

        assert key != null;

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
    protected void populateJson(JsonObject json, ConstantPool constantPool) {
        // Set the type and key before calling super to make the keys appear in
        // a more logical order
        json.put(JsonConstants.CHANGE_TYPE, JsonConstants.CHANGE_TYPE_PUT);
        json.put(JsonConstants.CHANGE_MAP_KEY, key);

        super.populateJson(json, constantPool);

        if (value instanceof StateNode node) {
            json.put(JsonConstants.CHANGE_PUT_NODE_VALUE,
                    Json.create(node.getId()));
        } else if (value instanceof ObjectNode node) {
            json.put(JsonConstants.CHANGE_PUT_VALUE, Json.parse(JacksonCodec
                    .encodeWithConstantPool(node, constantPool).toString()));
        } else if (value instanceof NumericNode node) {
            json.put(JsonConstants.CHANGE_PUT_VALUE, Json.create(JacksonCodec
                    .encodeWithConstantPool(node, constantPool).doubleValue()));
        } else if (value instanceof BooleanNode node) {
            json.put(JsonConstants.CHANGE_PUT_VALUE,
                    Json.create(JacksonCodec
                            .encodeWithConstantPool(node, constantPool)
                            .booleanValue()));
        } else if (value instanceof TextNode node) {
            json.put(JsonConstants.CHANGE_PUT_VALUE, Json.create(JacksonCodec
                    .encodeWithConstantPool(node, constantPool).textValue()));
        } else if (value instanceof ValueNode node) {
            json.put(JsonConstants.CHANGE_PUT_VALUE, Json.create(JacksonCodec
                    .encodeWithConstantPool(node, constantPool).toString()));
        } else if (value instanceof ArrayNode node) {
            json.put(JsonConstants.CHANGE_PUT_VALUE,
                    JacksonUtils.createElementalArray((ArrayNode) JacksonCodec
                            .encodeWithConstantPool(node, constantPool)));
        } else {
            json.put(JsonConstants.CHANGE_PUT_VALUE,
                    JsonCodec.encodeWithConstantPool(value, constantPool));
        }
    }
}
