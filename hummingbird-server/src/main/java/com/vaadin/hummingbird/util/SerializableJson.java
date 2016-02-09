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
package com.vaadin.hummingbird.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import elemental.json.Json;
import elemental.json.JsonValue;
import elemental.json.impl.JsonUtil;

/**
 * JSON value wrapper that serializes as the value's string representation.
 *
 * @since
 * @author Vaadin Ltd
 */
public final class SerializableJson implements Serializable {
    private transient JsonValue value;

    /**
     * Creates a serializable wrapper for a JSON value.
     *
     * @param value
     *            the JSON value to wrap
     */
    public SerializableJson(JsonValue value) {
        assert value != null;
        this.value = value;
    }

    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        String jsonString = (String) stream.readObject();
        if (jsonString == null) {
            value = Json.createNull();
        } else {
            value = JsonUtil.parse(jsonString);
        }
    }

    /**
     * Gets the wrapped JSON value.
     *
     * @return the JSON value
     */
    public JsonValue getValue() {
        return value;
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(value.toJson());
    }

    /**
     * Creates a new map with the same contents as the original map, except that
     * all JSON values are wrapped as {@link SerializableJson}. Map values of
     * other types are included without modification. The created map can be
     * serialized using {@link ObjectOutputStream#writeObject(Object)}.
     *
     * @param map
     *            the map to make serializable
     * @return a new serializable map
     */
    public static <K> Map<K, Object> createSerializableMap(Map<K, Object> map) {
        HashMap<K, Object> output = new HashMap<>();

        map.forEach((key, value) -> {
            if (value instanceof JsonValue) {
                JsonValue jsonValue = (JsonValue) value;
                value = new SerializableJson(jsonValue);
            }
            output.put(key, value);
        });

        return output;
    }

    /**
     * Unwraps any {@link SerializableJson} value in place in the provided map.
     * Map values of other types are not modified. The provided map is typically
     * received from {@link ObjectInputStream#readObject()}.
     *
     * @param deserializedMap
     *            the map with JSON values to unwrap.
     */
    public static <K> void unwrapMap(Map<K, Object> deserializedMap) {
        deserializedMap.replaceAll((key, value) -> {
            if (value instanceof SerializableJson) {
                SerializableJson json = (SerializableJson) value;
                return json.getValue();
            } else {
                return value;
            }
        });
    }
}
