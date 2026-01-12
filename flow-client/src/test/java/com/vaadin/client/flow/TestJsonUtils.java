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
package com.vaadin.client.flow;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Test utilities for JSON operations, replacing the removed JsonUtils from
 * flow-server.
 */
public class TestJsonUtils {

    /**
     * Creates an array with the provided values.
     *
     * @param values
     *            the values to add to the array
     * @return a new JSON array
     */
    public static JsonArray createArray(JsonValue... values) {
        JsonArray array = Json.createArray();
        for (JsonValue value : values) {
            array.set(array.length(), value);
        }
        return array;
    }

    /**
     * Collector for JSON values to a JSON array.
     *
     * @return a collector that collects JSON values into a JSON array
     */
    public static Collector<JsonValue, JsonArray, JsonArray> asArray() {
        return new Collector<JsonValue, JsonArray, JsonArray>() {
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
                        JsonValue value = right.get(i);
                        left.set(left.length(), value);
                    }
                    return left;
                };
            }

            @Override
            public Function<JsonArray, JsonArray> finisher() {
                return Function.identity();
            }

            @Override
            public Set<Characteristics> characteristics() {
                return EnumSet.of(Characteristics.IDENTITY_FINISH);
            }
        };
    }
}
