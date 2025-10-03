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
package com.vaadin.client.flow.util;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.flow.internal.JsonCodec;

import elemental.dom.Node;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Static helpers for encoding and decoding JSON.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ClientJsonCodec {
    private ClientJsonCodec() {
        // Prevent instantiation
    }

    /**
     * Decodes a value as a {@link StateNode} encoded on the server using
     * {@link JsonCodec#encodeWithTypeInfo(Object)} if it's possible. Otherwise
     * returns {@code null}.
     * <p>
     * It does the same as {@link #decodeWithTypeInfo(StateTree, JsonValue)} for
     * the encoded json value if the encoded object is a {@link StateNode}
     * except it returns the node itself instead of a DOM element associated
     * with it.
     *
     * @see #decodeWithTypeInfo(StateTree, JsonValue)
     * @param tree
     *            the state tree to use for resolving nodes and elements
     * @param json
     *            the JSON value to decode
     * @return the decoded state node if any
     */
    public static StateNode decodeStateNode(StateTree tree, JsonValue json) {
        if (json.getType() == JsonType.OBJECT) {
            // Check for @v-node format
            JsonValue nodeIdValue = ((JsonObject) json).get("@v-node");
            if (nodeIdValue != null) {
                if (nodeIdValue.getType() != JsonType.NUMBER) {
                    throw new IllegalArgumentException(
                            "@v-node value must be a number, got "
                                    + nodeIdValue.getType() + " in "
                                    + json.toJson());
                }
                int nodeId = (int) nodeIdValue.asNumber();
                return tree.getNode(nodeId);
            }

            return null;
        } else {
            return null;
        }
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
        if (json.getType() == JsonType.OBJECT) {
            JsonObject jsonObject = (JsonObject) json;
            // Check for @v-node format
            JsonValue nodeIdValue = jsonObject.get("@v-node");
            if (nodeIdValue != null) {
                if (nodeIdValue.getType() != JsonType.NUMBER) {
                    throw new IllegalArgumentException(
                            "@v-node value must be a number, got "
                                    + nodeIdValue.getType() + " in "
                                    + json.toJson());
                }
                int nodeId = (int) nodeIdValue.asNumber();
                Node domNode = tree.getNode(nodeId).getDomNode();
                return domNode;
            }

            // Check for @v-return format
            JsonValue returnArray = jsonObject.get("@v-return");
            if (returnArray != null) {
                if (returnArray.getType() != JsonType.ARRAY) {
                    throw new IllegalArgumentException(
                            "@v-return value must be an array, got "
                                    + returnArray.getType() + " in "
                                    + json.toJson());
                }
                JsonArray array = (JsonArray) returnArray;
                if (array.length() < 2) {
                    throw new IllegalArgumentException(
                            "@v-return array must have at least 2 elements, got "
                                    + array.length() + " in " + json.toJson());
                }
                int returnNodeId = (int) array.getNumber(0);
                int channelId = (int) array.getNumber(1);
                return createReturnChannelCallback(returnNodeId, channelId,
                        tree.getRegistry().getServerConnector());
            }

            // Check for unknown @v- types
            for (String key : jsonObject.keys()) {
                if (key.startsWith("@v-")) {
                    throw new IllegalArgumentException("Unsupported @v type '"
                            + key + "' in " + json.toJson());
                }
            }

            return decodeObjectWithTypeInfo(tree, jsonObject);
        } else if (json.getType() == JsonType.ARRAY) {
            return decodeArrayWithTypeInfo(tree, (JsonArray) json);
        } else {
            return decodeWithoutTypeInfo(json);
        }
    }

    private static native NativeFunction createReturnChannelCallback(int nodeId,
            int channelId, ServerConnector serverConnector)
    /*-{
        return $entry(function() {
          var args = Array.prototype.slice.call(arguments);
          serverConnector.@ServerConnector::sendReturnChannelMessage(*)(nodeId, channelId, args);
        });
    }-*/;

    /**
     * Decodes a value encoded on the server using
     * {@link JsonCodec#encodeWithoutTypeInfo(Object)}. This is a no-op in
     * compiled JavaScript since the JSON representation can be used as-is, but
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
        if (value == null) {
            // undefined shouln't go as undefined, it should be encoded as null
            return Json.createNull();
        } else if (GWT.isScript()) {
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
            }
            throw new IllegalArgumentException(
                    "Can't encode" + value.getClass() + " to json");
        }
    }

    /**
     * Recursively decodes a JSON object, processing any nested @v references.
     * Returns a native JS object that can store decoded values including DOM
     * elements.
     */
    private static JsonObject decodeObjectWithTypeInfo(StateTree tree,
            JsonObject jsonObject) {

        for (String key : jsonObject.keys()) {
            JsonValue orignalValue = jsonObject.get(key);
            Object decoded = decodeWithTypeInfo(tree, orignalValue);
            JsonValue newValue = WidgetUtil.crazyJsoCast(decoded);
            jsonObject.put(key, newValue);
        }
        return jsonObject;
    }

    /**
     * Recursively decodes a JSON array, processing any nested @v references.
     */
    private static JsArray<Object> decodeArrayWithTypeInfo(StateTree tree,
            JsonArray jsonArray) {
        JsArray<Object> jsArray = JsCollections.array();
        for (int i = 0; i < jsonArray.length(); i++) {
            JsonValue originalValue = jsonArray.get(i);
            Object decoded = decodeWithTypeInfo(tree, originalValue);
            jsArray.push(decoded);
        }
        return jsArray;
    }

}
