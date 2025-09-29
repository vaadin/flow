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
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Static helpers for encoding and decoding JSON.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ClientJsonCodec {
    /**
     * Type id for a complex type array containing a bean object.
     */
    public static final int BEAN_TYPE = 5;

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
        if (json.getType() == JsonType.ARRAY) {
            JsonArray array = (JsonArray) json;
            int typeId = (int) array.getNumber(0);
            switch (typeId) {
            case JsonCodec.NODE_TYPE: {
                int nodeId = (int) array.getNumber(1);
                return tree.getNode(nodeId);
            }
            case JsonCodec.ARRAY_TYPE:
            case JsonCodec.RETURN_CHANNEL_TYPE:
            case BEAN_TYPE:
                return null;
            default:
                throw new IllegalArgumentException(
                        "Unsupported complex type in " + array.toJson());
            }
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
        if (json.getType() == JsonType.ARRAY) {
            JsonArray array = (JsonArray) json;
            int typeId = (int) array.getNumber(0);
            switch (typeId) {
            case JsonCodec.NODE_TYPE: {
                int nodeId = (int) array.getNumber(1);
                Node domNode = tree.getNode(nodeId).getDomNode();
                return domNode;
            }
            case JsonCodec.ARRAY_TYPE:
                return jsonArrayAsJsArray(array.getArray(1));
            case JsonCodec.RETURN_CHANNEL_TYPE:
                return createReturnChannelCallback((int) array.getNumber(1),
                        (int) array.getNumber(2),
                        tree.getRegistry().getServerConnector());
            case BEAN_TYPE:
                return decodeBeanWithComponents(tree, array.get(1));
            default:
                throw new IllegalArgumentException(
                        "Unsupported complex type in " + array.toJson());
            }
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
     * Decodes a bean object containing component references with lazy
     * resolution. Component lookups happen when the value is accessed, not
     * during parsing.
     *
     * @param tree
     *            the state tree to use for resolving component references
     * @param beanJson
     *            the JSON representation of the bean
     * @return the decoded bean with lazy component resolution
     */
    private static Object decodeBeanWithComponents(StateTree tree,
            JsonValue beanJson) {
        if (GWT.isScript()) {
            return createNativeBeanWithComponents(tree, beanJson);
        } else {
            // JVM test implementation - return JsonValue as-is
            // In real GWT/browser, this would be a JavaScript object with
            // resolved components
            return beanJson;
        }
    }

    /**
     * Creates a native JavaScript object from the bean JSON with lazy component
     * resolution. Component references are resolved when accessed, not during
     * parsing.
     */
    private static native Object createNativeBeanWithComponents(StateTree tree,
            JsonValue beanJson)
    /*-{
        function resolveValue(value) {
            if (value && typeof value === 'object') {
                // Check if it's a Vaadin type reference
                if (value.__vaadinType !== undefined) {
                    switch (value.__vaadinType) {
                        case 'component':
                            if (value.nodeId === null || value.nodeId === undefined) {
                                return null;
                            }
                            var stateNode = tree.@com.vaadin.client.flow.StateTree::getNode(I)(value.nodeId);
                            return stateNode.@com.vaadin.client.flow.StateNode::getDomNode()();
                        // Future types can be added here
                        // case 'returnChannel':
                        // case 'template':
                        default:
                            // Unknown type, return as-is
                            return value;
                    }
                }

                // Handle arrays
                if (Array.isArray(value)) {
                    return value.map(resolveValue);
                }

                // Handle nested objects
                var result = {};
                for (var key in value) {
                    if (value.hasOwnProperty(key)) {
                        result[key] = resolveValue(value[key]);
                    }
                }
                return result;
            }
            return value;
        }

        // Parse and resolve in one pass
        var parsed = beanJson;
        return resolveValue(parsed);
    }-*/;

}
