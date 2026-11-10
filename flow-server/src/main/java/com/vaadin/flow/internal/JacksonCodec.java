/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.Objects;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsSnippet;

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
 * <li>{@link JsFunction} (encoded as a JS function literal that closes over its
 * captured parameters on the client, or for one declared by a
 * {@link com.vaadin.flow.js.JsDefinition JsDefinition} interface as the
 * identifier of the function in the bundle and the captured parameters)
 * <li>an implementation of an interface that extends {@link JsSnippet}, which
 * {@link com.vaadin.flow.js.JsSnippets JsSnippets} hands out (encoded like a
 * declared {@link JsFunction})
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
     * The key of the JSON object that a {@link JsFunction} is encoded as.
     * 
     * @since 25.4
     */
    public static final String FUNCTION_KEY = "@v-fn";

    private JacksonCodec() {
        // Don't create instances
    }

    /**
     * Helper for encoding values that might not have a native representation in
     * JSON. Such types are encoded as an JSON array starting with an id
     * defining the actual type and followed by the actual data. Supported value
     * types are any native JSON type supported by
     * {@link #encodeWithoutTypeInfo(Object)}, and all other types are handled
     * by Jackson serialization with custom serializers for {@link Component}
     * and Node types.
     *
     * @param value
     *            the value to encode
     * @return the value encoded as JSON
     */
    public static JsonNode encodeWithTypeInfo(Object value) {

        if (value == null) {
            return encodeWithoutTypeInfo(value);
        } else if (value instanceof ReturnChannelRegistration) {
            return encodeReturnChannel((ReturnChannelRegistration) value);
        } else if (value instanceof JsFunction) {
            return encodeJsFunction((JsFunction) value);
        } else if (value instanceof JsSnippet snippet && JsDefinitionProxy
                .findDeclaredCall(snippet) instanceof JsCall declaredCall) {
            return encodeDeclaredSnippet(declaredCall);
        } else if (canEncodeWithoutTypeInfo(value.getClass())) {
            // Native JSON types - no wrapping needed
            return encodeWithoutTypeInfo(value);
        } else {
            // All other types (including Components, Nodes, arrays and beans)
            // use standard Jackson
            // serialization with custom serializers for Components and Nodes
            return JacksonUtils.getMapper().valueToTree(value);
        }
    }

    private static JsonNode encodeReturnChannel(
            ReturnChannelRegistration value) {
        ObjectMapper mapper = JacksonUtils.getMapper();
        ObjectNode obj = mapper.createObjectNode();
        ArrayNode channelArray = mapper.createArrayNode();
        channelArray.add(value.getStateNodeId());
        channelArray.add(value.getChannelId());
        obj.set("@v-return", channelArray);
        return obj;
    }

    @SuppressWarnings("removal")
    private static JsonNode encodeJsFunction(JsFunction value) {
        JsCall declaredCall = value.getDeclaredCall();
        if (declaredCall != null) {
            return encodeDeclaredSnippet(declaredCall);
        }
        ObjectMapper mapper = JacksonUtils.getMapper();
        ObjectNode obj = mapper.createObjectNode();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("body", value.getBody());
        ArrayNode captures = mapper.createArrayNode();
        for (Object capture : value.getCaptures()) {
            captures.add(encodeWithTypeInfo(capture));
        }
        payload.set("captures", captures);
        ArrayNode args = mapper.createArrayNode();
        for (String name : value.getArgumentNames()) {
            args.add(name);
        }
        payload.set("args", args);
        obj.set(FUNCTION_KEY, payload);
        return obj;
    }

    /**
     * Encodes a snippet that a {@link com.vaadin.flow.js.JsDefinition
     * JsDefinition} interface declares, a {@link JsFunction} or one of a type
     * that a component or an application declares. The bundle has the
     * JavaScript and the parameter names, so all the client needs is which
     * function to call with the captures.
     */
    private static JsonNode encodeDeclaredSnippet(JsCall declaredCall) {
        ObjectMapper mapper = JacksonUtils.getMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", declaredCall.getFunctionId());
        ArrayNode captures = mapper.createArrayNode();
        for (Object capture : declaredCall.flattenArguments()) {
            captures.add(encodeWithTypeInfo(capture));
        }
        payload.set("captures", captures);
        ObjectNode obj = mapper.createObjectNode();
        obj.set(FUNCTION_KEY, payload);
        return obj;
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
     * methods supports {@link ConstantPoolKey} and
     * {@link ParameterizedConstantPoolKey} in addition to the types supported
     * by {@link #encodeWithoutTypeInfo(Object)}.
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
        } else if (value instanceof ParameterizedConstantPoolKey reference) {
            return reference.encode(constantPool);
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
     * {@link Boolean}, {@link JsonNode}. {@link NullNode} is also supported.
     *
     * @param json
     *            the JSON value to decode
     * @return the decoded value
     * @since 25.0
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
     * Decodes a JSON value as an instance of the given class, for example
     * {@link String}, {@link Integer}, a primitive type such as {@code int}, a
     * bean class or a {@link JsonNode} type.
     * <p>
     * A JSON null is decoded as {@code null}, except for a primitive type,
     * which gets the primitive's default value. For a JSON node type, such as
     * {@link JsonNode} or {@link tools.jackson.databind.node.ObjectNode}, the
     * value is returned as is if it is of that node type.
     *
     * @param <T>
     *            the type to decode as
     * @param json
     *            the JSON value to decode, not {@code null}
     * @param type
     *            the class to decode as, not {@code null}
     * @return the value decoded as the given type
     * @throws IllegalArgumentException
     *             if deserialization failed, or if the type is a JSON node type
     *             that the value is not, such as an object node for an array
     * @throws NullPointerException
     *             if the JSON value or the type is {@code null}
     */
    public static <T> T decodeAs(JsonNode json, Class<T> type) {
        Objects.requireNonNull(type, "Type cannot be null");
        return decodeAs(json,
                JacksonUtils.getMapper().getTypeFactory().constructType(type));
    }

    /**
     * Decodes a JSON value as an instance of the given type. The type can be a
     * generic type, for example {@code List<MyBean>}.
     * <p>
     * A JSON null is decoded as {@code null}, except for a primitive type,
     * which gets the primitive's default value. For a JSON node type, such as
     * {@link JsonNode} or {@link tools.jackson.databind.node.ObjectNode}, the
     * value is returned as is if it is of that node type.
     *
     * @param <T>
     *            the type to decode as
     * @param json
     *            the JSON value to decode, not {@code null}
     * @param type
     *            the type to decode as, not {@code null}
     * @return the value decoded as the given type
     * @throws IllegalArgumentException
     *             if deserialization failed, or if the type is a JSON node type
     *             that the value is not, such as an object node for an array
     * @throws NullPointerException
     *             if the JSON value or the type is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T decodeAs(JsonNode json, JavaType type) {
        Objects.requireNonNull(json, "JSON cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        if (json.getNodeType() == JsonNodeType.NULL && !type.isPrimitive()) {
            return null;
        }
        if (type.isTypeOrSubTypeOf(JsonNode.class)) {
            if (!type.getRawClass().isInstance(json)) {
                throw new IllegalArgumentException("Cannot decode JSON of type "
                        + json.getNodeType() + " as " + type.toCanonical());
            }
            return (T) json;
        }
        try {
            return JacksonUtils.getMapper().treeToValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot deserialize JSON to type " + type.toCanonical()
                            + ": " + e.getMessage(),
                    e);
        }
    }

    /**
     * Decodes a JSON value as an instance of the type described by the given
     * type reference. The type can be a generic type, for example
     * {@code List<MyBean>}:
     *
     * <pre>
     * List&lt;MyBean&gt; result = JacksonCodec.decodeAs(json,
     *         new TypeReference&lt;List&lt;MyBean&gt;&gt;() {
     *         });
     * </pre>
     * <p>
     * A JSON null is decoded as {@code null}, except for a primitive type,
     * which gets the primitive's default value. For a JSON node type, such as
     * {@link JsonNode} or {@link tools.jackson.databind.node.ObjectNode}, the
     * value is returned as is if it is of that node type.
     *
     * @param <T>
     *            the type to decode as
     * @param json
     *            the JSON value to decode, not {@code null}
     * @param typeReference
     *            the type reference describing the type to decode as, not
     *            {@code null}
     * @return the value decoded as the given type
     * @throws IllegalArgumentException
     *             if deserialization failed, or if the type is a JSON node type
     *             that the value is not, such as an object node for an array
     * @throws NullPointerException
     *             if the JSON value or the type reference is {@code null}
     * @since 25.0
     */
    public static <T> T decodeAs(JsonNode json,
            TypeReference<T> typeReference) {
        Objects.requireNonNull(typeReference, "Type reference cannot be null");
        return decodeAs(json, JacksonUtils.getMapper().getTypeFactory()
                .constructType(typeReference));
    }

}
