/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

}
