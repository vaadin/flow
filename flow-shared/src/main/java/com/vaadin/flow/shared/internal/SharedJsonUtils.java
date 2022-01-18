package com.vaadin.flow.shared.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

/**
 * Json helpers shared between server and client.
 */
public final class SharedJsonUtils {

    private SharedJsonUtils() {
        // Only static
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
            return a.asBoolean() == b.asBoolean();
        case NUMBER:
            // FIXME
            return a.toJson().equals(b.toJson());
        case STRING:
            return a.asString().equals(b.asString());
        case OBJECT:
            return jsonObjectEquals((JsonObject) a, (JsonObject) b);
        case ARRAY:
            return jsonArrayEquals((JsonArray) a, (JsonArray) b);
        default:
            throw new IllegalArgumentException("Unsupported JsonType: " + type);
        }
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

}
