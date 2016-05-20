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
package com.vaadin.hummingbird;

import java.io.Serializable;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.util.JsonUtil;
import com.vaadin.ui.Component;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Static helpers for encoding and decoding JSON.
 *
 * @author Vaadin Ltd
 */
public class JsonCodec {
    /**
     * Type id for a complex type array containing an {@link Element}.
     */
    public static final int ELEMENT_TYPE = 0;

    /**
     * Type id for a complex type array containing a {@link JsonArray}.
     */
    public static final int ARRAY_TYPE = 1;

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
        if (value instanceof Component) {
            return encodeElement(((Component) value).getElement());
        } else if (value instanceof Element) {
            return encodeElement((Element) value);
        } else {
            JsonValue encoded = encodeWithoutTypeInfo(value);
            if (encoded.getType() == JsonType.ARRAY) {
                // Must "escape" arrays
                encoded = wrapComplexValue(ARRAY_TYPE, encoded);
            }
            return encoded;
        }
    }

    private static JsonValue encodeElement(Element element) {
        StateNode node = element.getNode();
        if (node.isAttached()) {
            return wrapComplexValue(ELEMENT_TYPE, Json.create(node.getId()));
        } else {
            return Json.createNull();
        }
    }

    private static JsonArray wrapComplexValue(int typeId, JsonValue value) {
        return JsonUtil.createArray(Json.create(typeId), value);
    }

    /**
     * Helper for checking whether the type is supported by
     * {@link #encodeWithoutTypeInfo(Object)}. Supported values types are
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
                "Can't encode" + value.getClass() + " to json");
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
            throw new IllegalArgumentException(
                    "Can't (yet) decode " + json.getType());
        }

    }

    /**
     * Decodes the given JSON value as the given type.
     * <p>
     * Supported types are {@link String}, {@link Boolean}, {@link Integer},
     * {@link Double}.
     *
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
        if (json.getType() == JsonType.NULL) {
            return null;
        }

        if (type == String.class) {
            return type.cast(json.asString());
        } else if (type == Boolean.class) {
            return type.cast(Boolean.valueOf(json.asBoolean()));
        } else if (type == Double.class) {
            return type.cast(Double.valueOf(json.asNumber()));
        } else if (type == Integer.class) {
            return type.cast(Integer.valueOf((int) json.asNumber()));
        } else {
            assert !canEncodeWithoutTypeInfo(type);
            throw new IllegalArgumentException(
                    "Unknown type " + type.getName());
        }

    }

}
