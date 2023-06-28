/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import java.util.AbstractList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Helpers for using <code>elemental.json</code>.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public final class JsonUtils {

    private static final String CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT = "Cannot convert null to JSON";

    private static final String CANNOT_CONVERT_NULL_TO_OBJECT = "Cannot convert null to Java object";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Collects a stream of JSON values to a JSON array.
     *
     * @author Vaadin Ltd
     * @since 1.0
     */
    private static final class JsonArrayCollector
            implements Collector<JsonValue, JsonArray, JsonArray> {
        @Override
        public Supplier<JsonArray> supplier() {
            return Json::createArray;
        }

        @Override
        public BiConsumer<JsonArray, JsonValue> accumulator() {
            return (array, value) -> array.set(array.length(), value);
        }

        @Override
        public BinaryOperator<JsonArray> combiner() {
            return (left, right) -> {
                for (int i = 0; i < right.length(); i++) {
                    left.set(left.length(), right.<JsonValue> get(i));
                }
                return left;
            };
        }

        @Override
        public Function<JsonArray, JsonArray> finisher() {
            return Function.identity();
        }

        @Override
        public Set<Collector.Characteristics> characteristics() {
            return arrayCollectorCharacteristics;
        }
    }

    private static final Set<Collector.Characteristics> arrayCollectorCharacteristics = Collections
            .unmodifiableSet(
                    EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

    private JsonUtils() {
        // Static-only class
    }

    /**
     * Compares two json values for deep equality.
     * <p>
     * This is a helper for overcoming the fact that {@link JsonValue} doesn't
     * override {@link Object#equals(Object)} and
     * {@link JsonValue#jsEquals(JsonValue)} is defined to use JavaScript
     * semantics where arrays and objects are equals only based on identity.
     *
     * @param a
     *            the first json value to check, may not be null
     * @param b
     *            the second json value to check, may not be null
     * @return <code>true</code> if both json values are the same;
     *         <code>false</code> otherwise
     */
    public static boolean jsonEquals(JsonValue a, JsonValue b) {
        assert a != null;
        assert b != null;

        if (a == b) {
            return true;
        }

        JsonType type = a.getType();
        if (type != b.getType()) {
            return false;
        }

        switch (type) {
        case NULL:
            return true;
        case BOOLEAN:
            return booleanEqual(a, b);
        case NUMBER:
            return numbersEqual(a, b);
        case STRING:
            return stringEqual(a, b);
        case OBJECT:
            return jsonObjectEquals((JsonObject) a, (JsonObject) b);
        case ARRAY:
            return jsonArrayEquals((JsonArray) a, (JsonArray) b);
        default:
            throw new IllegalArgumentException("Unsupported JsonType: " + type);
        }
    }

    public static boolean stringEqual(JsonValue a, JsonValue b) {
        assert a.getType() == JsonType.STRING;
        assert b.getType() == JsonType.STRING;
        return a.asString().equals(b.asString());
    }

    public static boolean booleanEqual(JsonValue a, JsonValue b) {
        assert a.getType() == JsonType.BOOLEAN;
        assert b.getType() == JsonType.BOOLEAN;
        return a.asBoolean() == b.asBoolean();
    }

    public static boolean numbersEqual(JsonValue a, JsonValue b) {
        assert a.getType() == JsonType.NUMBER;
        assert b.getType() == JsonType.NUMBER;
        return Double.doubleToRawLongBits(a.asNumber()) == Double
                .doubleToRawLongBits(b.asNumber());
    }

    private static boolean jsonObjectEquals(JsonObject a, JsonObject b) {
        assert a != null;
        assert b != null;

        if (a == b) {
            return true;
        }

        String[] keys = a.keys();

        if (keys.length != b.keys().length) {
            return false;
        }

        for (String key : keys) {
            JsonValue value = b.get(key);
            if (value == null || !jsonEquals(a.get(key), value)) {
                return false;
            }
        }

        return true;
    }

    private static boolean jsonArrayEquals(JsonArray a, JsonArray b) {
        assert a != null;
        assert b != null;

        if (a == b) {
            return true;
        }

        if (a.length() != b.length()) {
            return false;
        }
        for (int i = 0; i < a.length(); i++) {
            if (!jsonEquals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a stream from a JSON array.
     *
     * @param <T>
     *            the stream type
     * @param array
     *            the JSON array to create a stream from
     * @return a stream of JSON values
     */
    public static <T extends JsonValue> Stream<T> stream(JsonArray array) {
        if (array == null) {
            return Stream.empty();
        }

        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                return array.get(index);
            }

            @Override
            public int size() {
                return array.length();
            }
        }.stream();
    }

    /**
     * Creates a stream from a JSON array of objects. This method does not
     * verify that all items in the array are actually JSON objects instead of
     * some other JSON type.
     *
     * @param array
     *            the JSON array to create a stream from
     * @return a stream of JSON objects
     */
    public static Stream<JsonObject> objectStream(JsonArray array) {
        return stream(array);
    }

    /**
     * Creates a double stream from a JSON array of numbers. This method does
     * not verify that all items in the array are actually JSON numbers instead
     * of some other JSON type.
     *
     * @param array
     *            the JSON array to create a stream from
     * @return a double stream of the values in the array
     */
    public static DoubleStream numberStream(JsonArray array) {
        return JsonUtils.<JsonNumber> stream(array)
                .mapToDouble(JsonNumber::getNumber);
    }

    /**
     * Creates a collector that collects values into a JSON array.
     *
     * @return the collector
     */
    public static Collector<JsonValue, JsonArray, JsonArray> asArray() {
        return new JsonArrayCollector();
    }

    /**
     * Creates a new JSON array with the given values.
     *
     * @param values
     *            the values that should be in the created array
     * @return the created array
     */
    public static JsonArray createArray(JsonValue... values) {
        return Stream.of(values).collect(asArray());
    }

    /**
     * Converts the given map into a JSON object by converting each map value to
     * a JSON value.
     *
     * @param <T>
     *            the type of the map values
     * @param map
     *            the map to convert into a JSON object
     * @param itemToJson
     *            callback for converting map values to JSON
     * @return the created object
     */
    public static <T> JsonObject createObject(Map<String, T> map,
            Function<T, JsonValue> itemToJson) {
        JsonObject object = Json.createObject();

        map.forEach((key, value) -> object.put(key, itemToJson.apply(value)));

        return object;
    }

    /**
     * Converts the given bean to JSON.
     *
     * @param bean
     *            the bean to convert, not {@code null}
     * @return a JSON representation of the bean
     */
    public static JsonObject beanToJson(Object bean) {
        Objects.requireNonNull(bean, CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT);

        try {
            return Json.parse(objectMapper.writeValueAsString(bean));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting bean to JSON", e);
        }
    }

    /**
     * Converts the given list to JSON.
     *
     * @param list
     *            the list to convert, not {@code null}
     * @return a JSON representation of the bean
     */
    public static JsonArray listToJson(List<?> list) {
        Objects.requireNonNull(list, CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT);
        try {
            return Json.instance().parse(objectMapper.writeValueAsString(list));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting list to JSON", e);
        }
    }

    /**
     * Converts the given map to JSON.
     *
     * @param map
     *            the map to convert, not {@code null}
     * @return a JSON representation of the bean
     */
    public static JsonObject mapToJson(Map<String, ?> map) {
        Objects.requireNonNull(map, CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT);
        try {
            return Json.instance().parse(objectMapper.writeValueAsString(map));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting map to JSON", e);
        }
    }

    /**
     * Converts JsonObject into Java object of given type.
     *
     * @param jsonObject
     *            JSON object to convert, not {@code null}
     * @param tClass
     *            class of converted object instance
     * @return converted object instance
     * @param <T>
     *            type of result instance
     */
    public static <T> T readToObject(JsonObject jsonObject, Class<T> tClass) {
        Objects.requireNonNull(jsonObject, CANNOT_CONVERT_NULL_TO_OBJECT);
        try {
            return objectMapper.readValue(jsonObject.toJson(), tClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Error converting JsonObject to " + tClass.getName(), e);
        }
    }
}
