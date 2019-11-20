/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Utility for encoding objects to and from JSON.
 * <p>
 * Supported types are
 * <ul>
 * <li>{@link String}
 * <li>{@link Boolean} and <code>boolean</code>
 * <li>{@link Integer} and <code>int</code>
 * <li>{@link Double} and <code>double</code> (<code>NaN</code> and infinity not
 * supported)
 * <li>{@link JsonValue} and all its sub types
 * <li>{@link Element} (encoded as a reference to the element)
 * <li>{@link Component} (encoded as a reference to the root element)
 * </ul>
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class JsonCodec {
    /**
     * Type id for a complex type array containing an {@link Element}.
     */
    public static final int NODE_TYPE = 0;

    /**
     * Type id for a complex type array containing a {@link JsonArray}.
     */
    public static final int ARRAY_TYPE = 1;

    /**
     * Type id for a complex type array identifying a
     * {@link ReturnChannelRegistration} reference.
     */
    public static final int RETURN_CHANNEL_TYPE = 2;

    private JsonCodec() {
        // Don't create instances
    }

    /**
     * Helper for encoding values that might not have a native representation in
     * JSON. Such types are encoded as an JSON array starting with an id
     * defining the actual type and followed by the actual data. Supported value
     * types are any native JSON type supported by
     * {@link #encodeWithoutTypeInfo(Object)}, {@link Element} and
     * {@link Component} (encoded as its root element).
     *
     * @param value
     *            the value to encode
     * @return the value encoded as JSON
     */
    public static JsonValue encodeWithTypeInfo(Object value) {
        assert value == null || canEncodeWithTypeInfo(value.getClass());

        if (value instanceof Component) {
            return encodeNode(((Component) value).getElement());
        } else if (value instanceof Node<?>) {
            return encodeNode((Node<?>) value);
        } else if (value instanceof ReturnChannelRegistration) {
            return encodeReturnChannel((ReturnChannelRegistration) value);
        } else {
            JsonValue encoded = encodeWithoutTypeInfo(value);
            if (encoded.getType() == JsonType.ARRAY) {
                // Must "escape" arrays
                encoded = wrapComplexValue(ARRAY_TYPE, encoded);
            }
            return encoded;
        }
    }

    private static JsonValue encodeReturnChannel(
            ReturnChannelRegistration value) {
        return wrapComplexValue(RETURN_CHANNEL_TYPE,
                Json.create(value.getStateNodeId()),
                Json.create(value.getChannelId()));
    }

    private static JsonValue encodeNode(Node<?> node) {
        StateNode stateNode = node.getNode();
        if (stateNode.isAttached()) {
            return wrapComplexValue(NODE_TYPE, Json.create(stateNode.getId()));
        } else {
            return Json.createNull();
        }
    }

    private static JsonArray wrapComplexValue(int typeId, JsonValue... values) {
        return Stream.concat(Stream.of(Json.create(typeId)), Stream.of(values))
                .collect(JsonUtils.asArray());
    }

    /**
     * Helper for checking whether the type is supported by
     * {@link #encodeWithoutTypeInfo(Object)}. Supported value types are
     * {@link String}, {@link Integer}, {@link Double}, {@link Boolean},
     * {@link JsonValue}.
     *
     * @param type
     *            the type to check
     * @return whether the type can be encoded
     */
    public static boolean canEncodeWithoutTypeInfo(Class<?> type) {
        assert type != null;
        return String.class.equals(type) || Integer.class.equals(type)
                || Double.class.equals(type) || Boolean.class.equals(type)
                || JsonValue.class.isAssignableFrom(type);
    }

    /**
     * Helper for checking whether the type is supported by
     * {@link #encodeWithTypeInfo(Object)}. Supported values types are
     * {@link Node}, {@link Component}, {@link ReturnChannelRegistration} and
     * anything accepted by {@link #canEncodeWithoutTypeInfo(Class)}.
     *
     * @param type
     *            the type to check
     * @return whether the type can be encoded
     */
    public static boolean canEncodeWithTypeInfo(Class<?> type) {
        return canEncodeWithoutTypeInfo(type)
                || Node.class.isAssignableFrom(type)
                || Component.class.isAssignableFrom(type)
                || ReturnChannelRegistration.class.isAssignableFrom(type);
    }

    /**
     * Encodes a "primitive" value or a constant pool reference to JSON. This
     * methods supports {@link ConstantPoolKey} in addition to the types
     * supported by {@link #encodeWithoutTypeInfo(Object)}.
     *
     * @param value
     *            the value to encode
     * @param constantPool
     *            the constant pool to use for encoding constant pool references
     * @return the value encoded as JSON
     */
    public static JsonValue encodeWithConstantPool(Object value,
            ConstantPool constantPool) {
        if (value instanceof ConstantPoolKey) {
            ConstantPoolKey reference = (ConstantPoolKey) value;
            return Json.create(constantPool.getConstantId(reference));
        } else {
            return encodeWithoutTypeInfo(value);
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
            return Json.createNull();
        }

        assert canEncodeWithoutTypeInfo(value.getClass());

        Class<?> type = value.getClass();
        if (String.class.equals(value.getClass())) {
            return Json.create((String) value);
        } else if (Integer.class.equals(type) || Double.class.equals(type)) {
            return Json.create(((Number) value).doubleValue());
        } else if (Boolean.class.equals(type)) {
            return Json.create(((Boolean) value).booleanValue());
        } else if (JsonValue.class.isAssignableFrom(type)) {
            return (JsonValue) value;
        }
        assert !canEncodeWithoutTypeInfo(type);
        throw new IllegalArgumentException(
                "Can't encode " + value.getClass() + " to json");
    }

    /**
     * Helper for decoding any "primitive" value that is directly supported in
     * JSON. Supported values types are {@link String}, {@link Number},
     * {@link Boolean}, {@link JsonValue}. <code>null</code> is also supported.
     *
     * @param json
     *            the JSON value to decode
     * @return the decoded value
     */
    public static Serializable decodeWithoutTypeInfo(JsonValue json) {
        assert json != null;
        switch (json.getType()) {
        case BOOLEAN:
            return decodeAs(json, Boolean.class);
        case STRING:
            return decodeAs(json, String.class);
        case NUMBER:
            return decodeAs(json, Double.class);
        case NULL:
            return null;
        default:
            return json;
        }

    }

    /**
     * Decodes the given JSON value as the given type.
     * <p>
     * Supported types are {@link String}, {@link Boolean}, {@link Integer},
     * {@link Double} and primitives boolean, int, double
     *
     * @param <T>
     *            the decoded type
     * @param json
     *            the JSON value
     * @param type
     *            the type to decode as
     * @return the value decoded as the given type
     * @throws IllegalArgumentException
     *             if the type was unsupported
     */
    public static <T> T decodeAs(JsonValue json, Class<T> type) {
        assert json != null;
        if (json.getType() == JsonType.NULL && !type.isPrimitive()) {
            return null;
        }
        Class<?> convertedType = ReflectTools.convertPrimitiveType(type);
        if (type == String.class) {
            return type.cast(json.asString());
        } else if (convertedType == Boolean.class) {
            return (T) convertedType.cast(Boolean.valueOf(json.asBoolean()));
        } else if (convertedType == Double.class) {
            return (T) convertedType.cast(Double.valueOf(json.asNumber()));
        } else if (convertedType == Integer.class) {
            return (T) convertedType
                    .cast(Integer.valueOf((int) json.asNumber()));
        } else if (JsonValue.class.isAssignableFrom(type)) {
            return type.cast(json);
        } else {
            assert !canEncodeWithoutTypeInfo(type);
            throw new IllegalArgumentException(
                    "Unknown type " + type.getName());
        }

    }

}
