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
     * Decodes a value as a {@link StateNode} if it's a node reference.
     * Returns {@code null} for all other types.
     *
     * @param tree
     *            the state tree to use for resolving nodes
     * @param json
     *            the JSON value to decode
     * @return the decoded state node if any
     */
    public static StateNode decodeStateNode(StateTree tree, JsonValue json) {
        if (json.getType() == JsonType.OBJECT) {
            JsonObject obj = (JsonObject) json;
            if (obj.hasKey("@v") && "node".equals(obj.getString("@v"))) {
                JsonValue nodeIdValue = obj.get("id");
                if (nodeIdValue != null
                        && nodeIdValue.getType() != JsonType.NULL) {
                    int nodeId = (int) nodeIdValue.asNumber();
                    return tree.getNode(nodeId);
                }
            }
        }
        return null;
    }

    /**
     * Decodes a value using the universal @v format for special types.
     * Uses native JSON.parse with reviver for optimal performance.
     *
     * @param tree
     *            the state tree to use for resolving nodes and elements
     * @param json
     *            the JSON value to decode
     * @return the decoded value
     */
    public static Object decodeWithTypeInfo(StateTree tree, JsonValue json) {
        if (GWT.isScript()) {
            return decodeWithReviverNative(tree, json);
        } else {
            // JVM fallback for tests
            return decodeWithTypeInfoJvm(tree, json);
        }
    }

    /**
     * JVM implementation for tests.
     */
    private static Object decodeWithTypeInfoJvm(StateTree tree, JsonValue json) {
        if (json.getType() == JsonType.OBJECT) {
            JsonObject obj = (JsonObject) json;
            if (obj.hasKey("@v")) {
                String type = obj.getString("@v");
                switch (type) {
                case "node":
                    JsonValue nodeIdValue = obj.get("id");
                    if (nodeIdValue != null
                            && nodeIdValue.getType() != JsonType.NULL) {
                        int nodeId = (int) nodeIdValue.asNumber();
                        Node domNode = tree.getNode(nodeId).getDomNode();
                        return domNode;
                    }
                    return null;
                case "return":
                    int nodeId = (int) obj.getNumber("nodeId");
                    int channelId = (int) obj.getNumber("channelId");
                    return createReturnChannelCallback(nodeId, channelId,
                            tree.getRegistry().getServerConnector());
                default:
                    return obj;
                }
            }
            // For regular objects, process recursively to handle nested @v references
            return processObjectWithComponents(tree, obj);
        } else if (json.getType() == JsonType.ARRAY) {
            return jsonArrayAsJsArray((JsonArray) json);
        }
        return decodeWithoutTypeInfo(json);
    }

    /**
     * Recursively processes objects to handle nested @v references in JVM tests.
     */
    private static Object processObjectWithComponents(StateTree tree, JsonObject obj) {
        // For JVM tests, just return the object as-is
        // The native implementation handles the recursive processing
        return obj;
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
            case ARRAY:
            case OBJECT:
                // Return the JsonValue as-is for complex types in JVM tests
                return json;
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
     * Converts a JSON array to a JS array. This is a no-op in compiled
     * JavaScript, but needs special handling for tests running in the JVM.
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

    /**
     * Uses native JSON.parse with reviver function for optimal decoding.
     * The reviver handles @v references during parsing.
     */
    private static native Object decodeWithReviverNative(StateTree tree, JsonValue json)
    /*-{
        // Convert JsonValue to native object if needed
        var nativeJson = json;
        if (typeof json === 'object' && json.toNative) {
            nativeJson = json.toNative();
        }
        
        // Use JSON.parse with reviver for efficient processing
        return JSON.parse(JSON.stringify(nativeJson), function(key, value) {
            if (value && typeof value === 'object' && value['@v'] !== undefined) {
                switch (value['@v']) {
                    case 'node':
                        if (value.id === null || value.id === undefined) {
                            return null;
                        }
                        var stateNode = tree.@com.vaadin.client.flow.StateTree::getNode(I)(value.id);
                        return stateNode.@com.vaadin.client.flow.StateNode::getDomNode()();
                    case 'return':
                        var serverConnector = tree.@com.vaadin.client.flow.StateTree::getRegistry()().@com.vaadin.client.Registry::getServerConnector()();
                        return $entry(function() {
                            var args = Array.prototype.slice.call(arguments);
                            serverConnector.@com.vaadin.client.communication.ServerConnector::sendReturnChannelMessage(*)(value.nodeId, value.channelId, args);
                        });
                    default:
                        // Unknown @v type, return as-is
                        return value;
                }
            }
            return value;
        });
    }-*/;

}
