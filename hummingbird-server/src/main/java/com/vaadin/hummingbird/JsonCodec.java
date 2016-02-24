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

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.util.JsonUtil;

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
public class JsonCodec {
    private JsonCodec() {
        // Don't create instances
    }

    /**
     * Type id for a complex type array containing an {@link Element}.
     */
    public static final int ELEMENT_TYPE = 0;

    /**
     * Type id for a complex type array containing a {@link JsonArray}.
     */
    public static final int ARRAY_TYPE = 1;

    /**
     * Helper for encoding complex values as JSON. Some complex types are
     * encoded as an JSON array starting with an id defining the actual type and
     * followed by the actual data. Supported value types are any "simple" type
     * (see {@link #encodeSimpleValue(Object)}) and {@link Element}.
     *
     * @param value
     *            the value to encode
     * @return the value encoded as JSON
     */
    public static JsonValue encodeComplexValue(Object value) {
        if (value instanceof Element) {
            Element element = (Element) value;
            StateNode node = element.getNode();
            if (node.isAttached()) {
                return wrapComplexValue(ELEMENT_TYPE,
                        Json.create(node.getId()));
            } else {
                return Json.createNull();
            }
        } else {
            JsonValue encoded = encodeSimpleValue(value);
            if (encoded.getType() == JsonType.ARRAY) {
                // Must "escape" arrays
                encoded = wrapComplexValue(ARRAY_TYPE, encoded);
            }
            return encoded;
        }
    }

    private static JsonArray wrapComplexValue(int typeId, JsonValue value) {
        return JsonUtil.createArray(Json.create(typeId), value);
    }

    /**
     * Helper for encoding any simple value as JSON. Supported values types are
     * {@link String}, {@link Number}, {@link Boolean}, {@link JsonValue}.
     * <code>null</code> is also supported.
     *
     * @param value
     *            the value to encode
     * @return the value encoded as JSON
     */
    public static JsonValue encodeSimpleValue(Object value) {
        if (value instanceof JsonValue) {
            return (JsonValue) value;
        } else if (value instanceof String) {
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
