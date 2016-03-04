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
package com.vaadin.client.hummingbird.util;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.hummingbird.JsonCodec;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Static helpers for encoding and decoding JSON.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ClientJsonCodec {
    private ClientJsonCodec() {
        // Prevent instantiation
    }

    /**
     * Decodes a value encoded on the server using
     * {@link JsonCodec#encodeWithTypeInfo(Object)}.
     *
     * @param tree
     *            the state tree to use for resolving nodes and elements
     * @param json
     *            the JSON value to decode
     * @return the decoded value
     */
    public static Object decodeWithTypeInfo(StateTree tree, JsonValue json) {
        if (json.getType() == JsonType.ARRAY) {
            JsonArray array = (JsonArray) json;
            int typeId = (int) array.getNumber(0);
            switch (typeId) {
            case JsonCodec.ELEMENT_TYPE: {
                int nodeId = (int) array.getNumber(1);
                return tree.getNode(nodeId).getDomNode();
            }
            case JsonCodec.ARRAY_TYPE:
                return jsonArrayAsJsArray(array.getArray(1));
            default:
                throw new IllegalArgumentException(
                        "Unsupported complex type in " + array.toJson());
            }
        } else {
            return decodeWithoutTypeInfo(json);
        }
    }

    /**
     * Decodes a value encoded on the server using
     * {@link JsonCodec#encodeWithoutTypeInfo(Object)}. This is a no-op in
     * compiled JavaScipt since the JSON representation can be used as-is, but
     * some special handling is needed for tests running in the JVM.
     *
     * @param json
     *            the JSON value to convert
     * @return the decoded Java value
     */
    @SuppressWarnings("boxing")
    public static Object decodeWithoutTypeInfo(JsonValue json) {
        if (GWT.isScript()) {
            return json;
        } else {
            // JRE implementation for cases that have so far been needed
            switch (json.getType()) {
            case BOOLEAN:
                return json.asBoolean();
            case STRING:
                return json.asString();
            case NUMBER:
                return json.asNumber();
            case NULL:
                return null;
            default:
                throw new IllegalArgumentException(
                        "Can't (yet) convert " + json.getType());
            }
        }
    }

    /**
     * Helper for encoding any "primitive" value that is directly supported in
     * JSON. Supported values types are {@link String}, {@link Number},
     * {@link Boolean}, {@link JsonValue}. <code>null</code> is also supported.
     *
     * @param value
     *            the value to encode
     * @return the value encoded as JSON
     */
    public static JsonValue encodeWithoutTypeInfo(Object value) {
        if (GWT.isScript()) {
            return WidgetUtil.crazyJsoCast(value);
        } else {
            if (value instanceof String) {
                return Json.create((String) value);
            } else if (value instanceof Number) {
                return Json.create(((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                return Json.create(((Boolean) value).booleanValue());
            } else if (value instanceof JsonValue) {
                return (JsonValue) value;
            } else if (value == null) {
                return Json.createNull();
            } else {
                throw new IllegalArgumentException(
                        "Can't encode" + value.getClass() + " to json");
            }
        }
    }

    /**
     * Converts a JSON array to a JS array. This is a no-op in compiled
     * JavaScipt, but needs special handling for tests running in the JVM.
     *
     * @param jsonArray
     *            the JSON array to convert
     * @return the converted JS array
     */
    public static JsArray<Object> jsonArrayAsJsArray(JsonArray jsonArray) {
        JsArray<Object> jsArray;
        if (GWT.isScript()) {
            jsArray = WidgetUtil.crazyJsCast(jsonArray);
        } else {
            jsArray = JsCollections.array();
            for (int i = 0; i < jsonArray.length(); i++) {
                jsArray.push(decodeWithoutTypeInfo(jsonArray.get(i)));
            }
        }
        return jsArray;
    }

}
