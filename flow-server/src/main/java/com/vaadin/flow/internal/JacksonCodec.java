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
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.JsonNodeType;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

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
 * <li>{@link JsonNode} and all its sub types
 * <li>{@link Element} (encoded as a reference to the element)
 * <li>{@link Component} (encoded as a reference to the root element)
 * </ul>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.7
 */
public class JacksonCodec {
    /**
     * Type id for a complex type array containing an {@link Element}.
     */
    public static final int NODE_TYPE = 0;

    /**
     * Type id for a complex type array containing a {@link ArrayNode}.
     */
    public static final int ARRAY_TYPE = 1;

    /**
     * Type id for a complex type array identifying a
     * {@link ReturnChannelRegistration} reference.
     */
    public static final int RETURN_CHANNEL_TYPE = 2;

    private JacksonCodec() {
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
    public static JsonNode encodeWithTypeInfo(Object value) {

        if (value == null) {
            return encodeWithoutTypeInfo(value);
        } else if (value instanceof Component) {
            return encodeNode(((Component) value).getElement());
        } else if (value instanceof Node<?>) {
            return encodeNode((Node<?>) value);
        } else if (value instanceof ReturnChannelRegistration) {
            return encodeReturnChannel((ReturnChannelRegistration) value);
        } else if (canEncodeWithoutTypeInfo(value.getClass())) {
            JsonNode encoded = encodeWithoutTypeInfo(value);
            if (encoded.getNodeType() == JsonNodeType.ARRAY) {
                // Must "escape" arrays
                encoded = wrapComplexValue(ARRAY_TYPE, encoded);
            }
            return encoded;
        } else {
            // Encode as bean using Jackson serialization - send directly as
            // JSON
            return JacksonUtils.getMapper().valueToTree(value);
        }
    }

    private static ArrayNode encodeReturnChannel(
            ReturnChannelRegistration value) {
        return wrapComplexValue(RETURN_CHANNEL_TYPE,
                JacksonUtils.getMapper().valueToTree(value.getStateNodeId()),
                JacksonUtils.getMapper().valueToTree(value.getChannelId()));
    }

    private static JsonNode encodeNode(Node<?> node) {
        StateNode stateNode = node.getNode();
        if (stateNode.isAttached()) {
            return wrapComplexValue(NODE_TYPE,
                    JacksonUtils.getMapper().valueToTree(stateNode.getId()));
        } else {
            return JacksonUtils.getMapper().nullNode();
        }
    }

    private static ArrayNode wrapComplexValue(int typeId, JsonNode... values) {
        return Stream
                .concat(Stream.of(JacksonUtils.getMapper().valueToTree(typeId)),
                        Stream.of(values))
                .collect(JacksonUtils.asArray());
    }

    /**
     * Helper for checking whether the type is supported by
     * {@link #encodeWithoutTypeInfo(Object)}. Supported value types are
     * {@link String}, {@link Integer}, {@link Double}, {@link Boolean},
     * {@link JsonNode}.
     *
     * @param type
     *            the type to check
     * @return whether the type can be encoded
     */
    public static boolean canEncodeWithoutTypeInfo(Class<?> type) {
        assert type != null;
        return String.class.equals(type) || Integer.class.equals(type)
                || Double.class.equals(type) || Boolean.class.equals(type)
                || JsonNode.class.isAssignableFrom(type);
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
    public static JsonNode encodeWithConstantPool(Object value,
            ConstantPool constantPool) {
        if (value instanceof ConstantPoolKey) {
            ConstantPoolKey reference = (ConstantPoolKey) value;
            return JacksonUtils.getMapper()
                    .valueToTree(constantPool.getConstantId(reference));
        } else {
            return encodeWithoutTypeInfo(value);
        }
    }

    /**
     * Helper for encoding any "primitive" value that is directly supported in
     * JSON. Supported values types are {@link String}, {@link Number},
     * {@link Boolean}, {@link JsonNode}. <code>null</code> is also supported.
     *
     * @param value
     *            the value to encode
     * @return the value encoded as JSON
     */
    public static JsonNode encodeWithoutTypeInfo(Object value) {
        if (value == null) {
            return JacksonUtils.getMapper().nullNode();
        }

        assert canEncodeWithoutTypeInfo(value.getClass())
                : "this:_" + value.getClass();

        Class<?> type = value.getClass();
        if (String.class.equals(value.getClass())) {
            return JacksonUtils.getMapper().valueToTree(value);
        } else if (Integer.class.equals(type)) {
            return JacksonUtils.getMapper()
                    .valueToTree(((Number) value).intValue());
        } else if (Double.class.equals(type)) {
            return JacksonUtils.getMapper()
                    .valueToTree(((Number) value).doubleValue());
        } else if (Boolean.class.equals(type)) {
            return JacksonUtils.getMapper()
                    .valueToTree(((Boolean) value).booleanValue());
        } else if (JsonNode.class.isAssignableFrom(type)) {
            return (JsonNode) value;
        }
        throw new IllegalArgumentException(
                "Can't encode " + value.getClass() + " to json");
    }

    /**
     * Helper for decoding any "primitive" value that is directly supported in
     * JSON. Supported values types are {@link String}, {@link Number},
     * {@link Boolean}, {@link JsonNode}.
     * {@link tools.jackson.databind.node.NullNode} is also supported.
     *
     * @param json
     *            the JSON value to decode
     * @return the decoded value
     */
    public static Serializable decodeWithoutTypeInfo(BaseJsonNode json) {
        assert json != null;
        switch (json.getNodeType()) {
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
     * {@link Double}, primitives boolean, int, double, {@link JsonNode}, and
     * any bean object that can be deserialized from JSON.
     *
     * @param <T>
     *            the decoded type
     * @param json
     *            the JSON value
     * @param type
     *            the type to decode as
     * @return the value decoded as the given type
     * @throws IllegalArgumentException
     *             if the type was unsupported or deserialization failed
     */
    public static <T> T decodeAs(JsonNode json, Class<T> type) {
        assert json != null;
        if (json.getNodeType() == JsonNodeType.NULL && !type.isPrimitive()) {
            return null;
        }
        Class<?> convertedType = ReflectTools.convertPrimitiveType(type);
        if (type == String.class) {
            return type.cast(json.asText(""));
        } else if (convertedType == Boolean.class) {
            return (T) convertedType
                    .cast(Boolean.valueOf(json.asBoolean(false)));
        } else if (convertedType == Double.class) {
            return (T) convertedType.cast(Double.valueOf(json.asDouble(0.0)));
        } else if (convertedType == Integer.class) {
            return (T) convertedType.cast(Integer.valueOf(json.asInt(0)));
        } else if (JsonNode.class.isAssignableFrom(type)) {
            return type.cast(json);
        } else {
            // Try to deserialize as a bean using Jackson
            try {
                return JacksonUtils.getMapper().treeToValue(json, type);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Cannot deserialize JSON to type " + type.getName()
                                + ": " + e.getMessage(),
                        e);
            }
        }

    }

}
