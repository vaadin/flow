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

import java.util.AbstractList;
import java.util.ArrayList;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.core.util.Separators.Spacing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BaseJsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonBoolean;
import elemental.json.JsonNull;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Helpers for using <code>jackson</code>.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.7
 */
public final class JacksonUtils {

    private static final String CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT = "Cannot convert null to JSON";

    private static final String CANNOT_CONVERT_NULL_TO_OBJECT = "Cannot convert null to Java object";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static ObjectMapper getMapper() {
        return objectMapper;
    }

    /**
     * Create a new ObjectNode.
     *
     * @return ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    /**
     * Create a new ArrayNode.
     *
     * @return ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    /**
     * Create a nullNode for null value.
     *
     * @return NullNode
     */
    public static ValueNode nullNode() {
        return (ValueNode) objectMapper.nullNode();
    }

    /**
     * Map JsonArray to ArrayNode.
     *
     * @param jsonArray
     *            JsonArray to change
     * @return ArrayNode of elemental json array object or null for null
     *         jsonArray
     */
    public static ArrayNode mapElemental(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray instanceof JsonNull) {
            return null;
        }
        try {
            return (ArrayNode) objectMapper.readTree(jsonArray.toJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Map JsonObject to ObjectNode.
     *
     * @param jsonObject
     *            JsonObject to change
     * @return ObjectNode of elemental json object object or null for null
     *         jsonObject
     */
    public static ObjectNode mapElemental(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return (ObjectNode) objectMapper.readTree(jsonObject.toJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Map JsonValue to ObjectNode.
     *
     * @param jsonValue
     *            JsonValue to change
     * @return ObjectNode of elemental json value
     */
    public static BaseJsonNode mapElemental(JsonValue jsonValue) {
        if (jsonValue == null || jsonValue instanceof JsonNull) {
            return nullNode();
        }
        if (jsonValue instanceof JsonObject) {
            return mapElemental((JsonObject) jsonValue);
        }
        if (jsonValue instanceof JsonArray) {
            return mapElemental((JsonArray) jsonValue);
        }
        if (jsonValue instanceof JsonNumber) {
            return objectMapper.valueToTree(jsonValue.asNumber());
        }
        if (jsonValue instanceof JsonBoolean) {
            return objectMapper.valueToTree(jsonValue.asBoolean());
        }
        return objectMapper.valueToTree(jsonValue.asString());
    }

    /**
     * Convert the contents of an ArrayNode into a JsonArray. This is mostly
     * needed for arrays that may contain arrays and values.
     *
     * @param jsonNodes
     *            ArrayNode to convert
     * @return JsonArray of ArrayNode content
     */
    public static JsonArray createElementalArray(ArrayNode jsonNodes) {
        return (JsonArray) parseNode(jsonNodes);
    }

    private static JsonValue parseNode(JsonNode node) {
        if (node instanceof ArrayNode) {
            JsonArray jsonArray = Json.createArray();
            node.forEach(arrayNode -> parseArrayNode(arrayNode, jsonArray));
            return jsonArray;
        }
        return Json.parse(node.toString());
    }

    private static void parseArrayNode(JsonNode node, JsonArray jsonArray) {
        if (JsonNodeType.NUMBER.equals(node.getNodeType())) {
            jsonArray.set(jsonArray.length(), Json.create(node.doubleValue()));
        } else if (JsonNodeType.STRING.equals(node.getNodeType())) {
            jsonArray.set(jsonArray.length(), Json.create(node.textValue()));
        } else if (JsonNodeType.ARRAY.equals(node.getNodeType())) {
            JsonArray array = Json.createArray();
            node.forEach(arrayNode -> parseArrayNode(arrayNode, array));
            jsonArray.set(jsonArray.length(), array);
        } else if (JsonNodeType.BOOLEAN.equals(node.getNodeType())) {
            jsonArray.set(jsonArray.length(), Json.create(node.booleanValue()));
        } else if (JsonNodeType.NULL.equals(node.getNodeType())) {
            jsonArray.set(jsonArray.length(), Json.createNull());
        } else {
            jsonArray.set(jsonArray.length(), Json.parse(node.toString()));
        }
    }

    /**
     * Read Json string to JsonNode.
     *
     * @return JsonNode representation of given json string
     */
    public static ObjectNode readTree(String json) {
        try {
            return (ObjectNode) objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new JsonDecodingException("Could not parse json content", e);
        }
    }

    /**
     * Create a JsonNode from value.
     *
     * @return JsonNode for given value content
     */
    public static JsonNode createNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    /**
     * Collects a stream of JSON values to a JSON array.
     *
     * @author Vaadin Ltd
     * @since 24.7
     */
    private static final class ArrayNodeCollector
            implements Collector<JsonNode, ArrayNode, ArrayNode> {
        @Override
        public Supplier<ArrayNode> supplier() {
            return objectMapper::createArrayNode;
        }

        @Override
        public BiConsumer<ArrayNode, JsonNode> accumulator() {
            return ArrayNode::add;
        }

        @Override
        public BinaryOperator<ArrayNode> combiner() {
            return (left, right) -> {
                for (int i = 0; i < right.size(); i++) {
                    left.set(left.size(), right.get(i));
                }
                return left;
            };
        }

        @Override
        public Function<ArrayNode, ArrayNode> finisher() {
            return Function.identity();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return arrayCollectorCharacteristics;
        }
    }

    private static final Set<Collector.Characteristics> arrayCollectorCharacteristics = Collections
            .unmodifiableSet(
                    EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

    private JacksonUtils() {
        // Static-only class
    }

    /**
     * Compares two json values for deep equality.
     * <p>
     * This is a helper for overcoming the fact that {@link ObjectNode} doesn't
     * override {@link Object#equals(Object)} and {
     * ObjectNode#jsEquals(ObjectNode)} is defined to use JavaScript semantics
     * where arrays and objects are equals only based on identity.
     *
     * @param a
     *            the first json value to check, may not be null
     * @param b
     *            the second json value to check, may not be null
     * @return <code>true</code> if both json values are the same;
     *         <code>false</code> otherwise
     */
    public static boolean jsonEquals(JsonNode a, JsonNode b) {
        assert a != null;
        assert b != null;

        if (a == b) {
            return true;
        }

        JsonNodeType type = a.getNodeType();
        if (type != b.getNodeType()) {
            return false;
        }

        return switch (type) {
        case NULL -> true;
        case BOOLEAN -> booleanEqual(a, b);
        case NUMBER -> numbersEqual(a, b);
        case STRING -> stringEqual(a, b);
        case OBJECT -> jsonObjectEquals(a, b);
        case ARRAY -> jsonArrayEquals((ArrayNode) a, (ArrayNode) b);
        default ->
            throw new IllegalArgumentException("Unsupported JsonType: " + type);
        };
    }

    /**
     * Compare String value of two JsonNode values.
     *
     * @param a
     *            Value one
     * @param b
     *            Value two
     * @return {@code true} if text content equals
     */
    public static boolean stringEqual(JsonNode a, JsonNode b) {
        assert a.getNodeType() == JsonNodeType.STRING;
        assert b.getNodeType() == JsonNodeType.STRING;
        return a.asText().equals(b.asText());
    }

    /**
     * Compare boolean value of two JsonNode values.
     *
     * @param a
     *            Value one
     * @param b
     *            Value two
     * @return {@code true} if text boolean equals
     */
    public static boolean booleanEqual(JsonNode a, JsonNode b) {
        assert a.getNodeType() == JsonNodeType.BOOLEAN;
        assert b.getNodeType() == JsonNodeType.BOOLEAN;
        return a.asBoolean() == b.asBoolean();
    }

    /**
     * Compare number value of two JsonNode values.
     *
     * @param a
     *            Value one
     * @param b
     *            Value two
     * @return {@code true} if number content equals
     */
    public static boolean numbersEqual(JsonNode a, JsonNode b) {
        assert a.getNodeType() == JsonNodeType.NUMBER;
        assert b.getNodeType() == JsonNodeType.NUMBER;
        return Double.doubleToRawLongBits(a.doubleValue()) == Double
                .doubleToRawLongBits(b.doubleValue());
    }

    private static boolean jsonObjectEquals(JsonNode a, JsonNode b) {
        assert a != null;
        assert b != null;

        if (a == b) {
            return true;
        }

        List<String> keys = getKeys(a);
        List<String> bkeys = getKeys(b);

        if (keys.size() != bkeys.size()) {
            return false;
        }

        for (String key : keys) {
            JsonNode value = b.get(key);
            if (value == null || !jsonEquals(a.get(key), value)) {
                return false;
            }
        }

        return true;
    }

    public static List<String> getKeys(JsonNode node) {
        List<String> keys = new ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    private static boolean jsonArrayEquals(ArrayNode a, ArrayNode b) {
        assert a != null;
        assert b != null;

        if (a == b) {
            return true;
        }

        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
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
    public static <T extends JsonNode> Stream<T> stream(ArrayNode array) {
        if (array == null) {
            return Stream.empty();
        }

        return new AbstractList<T>() {
            @Override
            public T get(int index) {
                return (T) array.get(index);
            }

            @Override
            public int size() {
                return array.size();
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
    public static Stream<JsonNode> objectStream(ArrayNode array) {
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
    public static DoubleStream numberStream(ArrayNode array) {
        return JacksonUtils.<DoubleNode> stream(array)
                .mapToDouble(DoubleNode::doubleValue);
    }

    /**
     * Creates a collector that collects values into a JSON array.
     *
     * @return the collector
     */
    public static Collector<JsonNode, ArrayNode, ArrayNode> asArray() {
        return new ArrayNodeCollector();
    }

    /**
     * Creates a new JSON array with the given values.
     *
     * @param values
     *            the values that should be in the created array
     * @return the created array
     */
    public static ArrayNode createArray(JsonNode... values) {
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
    public static <T> ObjectNode createObject(Map<String, T> map,
            Function<T, JsonNode> itemToJson) {
        ObjectNode object = objectMapper.createObjectNode();

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
    public static ObjectNode beanToJson(Object bean) {
        Objects.requireNonNull(bean, CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT);

        return objectMapper.valueToTree(bean);
    }

    /**
     * Converts the given list to JSON.
     *
     * @param list
     *            the list to convert, not {@code null}
     * @return a JSON representation of the bean
     */
    public static ArrayNode listToJson(List<?> list) {
        Objects.requireNonNull(list, CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT);
        return objectMapper.valueToTree(list);
    }

    /**
     * Converts the given map to JSON.
     *
     * @param map
     *            the map to convert, not {@code null}
     * @return a JSON representation of the bean
     */
    public static ObjectNode mapToJson(Map<String, ?> map) {
        Objects.requireNonNull(map, CANNOT_CONVERT_NULL_TO_A_JSON_OBJECT);
        return objectMapper.valueToTree(map);
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
    public static <T> T readToObject(JsonNode jsonObject, Class<T> tClass) {
        Objects.requireNonNull(jsonObject, CANNOT_CONVERT_NULL_TO_OBJECT);
        try {
            return objectMapper.treeToValue(jsonObject, tClass);
        } catch (JsonProcessingException e) {
            throw new JsonDecodingException(
                    "Error converting JsonObject to " + tClass.getName(), e);
        }
    }

    /**
     * Converts ObjectNode into Java object of given type.
     *
     * @param jsonValue
     *            JSON value to convert, not {@code null}
     * @param tClass
     *            class of converted object instance
     * @return converted object instance
     * @param <T>
     *            type of result instance
     */
    public static <T> T readValue(JsonNode jsonValue, Class<T> tClass) {
        return readToObject(jsonValue, tClass);
    }

    /**
     * Converts ObjectNode into Java object of given type.
     *
     * @param jsonValue
     *            JSON value to convert, not {@code null}
     * @param typeReference
     *            type reference of converted object instance
     * @return converted object instance
     * @param <T>
     *            type of result instance
     */
    public static <T> T readValue(JsonNode jsonValue,
            TypeReference<T> typeReference) {
        Objects.requireNonNull(jsonValue, CANNOT_CONVERT_NULL_TO_OBJECT);
        try {
            return objectMapper.treeToValue(jsonValue, typeReference);
        } catch (JsonProcessingException e) {
            throw new JsonDecodingException("Error converting ObjectNode to "
                    + typeReference.getType().getTypeName(), e);
        }
    }

    /**
     * Converts Java object into ObjectNode.
     *
     * @param object
     *            Java object to convert
     * @return converted JSON value
     */
    public static BaseJsonNode writeValue(Object object) {
        return objectMapper.valueToTree(object);
    }

    /**
     * Converts the given node into JSON suitable for writing into a file such
     * as {@literal package.json}.
     *
     * @param node
     *            the node to convert
     * @return the JSON string
     * @throws JsonProcessingException
     *             if the node cannot be converted
     */
    public static String toFileJson(JsonNode node)
            throws JsonProcessingException {
        DefaultPrettyPrinter filePrinter = new DefaultPrettyPrinter(
                Separators.createDefaultInstance()
                        .withObjectFieldValueSpacing(Spacing.AFTER));
        return objectMapper.writer().with(filePrinter).writeValueAsString(node);
    }
}
