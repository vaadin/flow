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

package com.vaadin.util;

import java.util.AbstractList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Helpers for using <code>elemental.json</code> together with the Stream API.
 *
 * @since
 * @author Vaadin Ltd
 */
public class JsonStream {
    private static final Set<Collector.Characteristics> arrayCollectorCharacteristics = Collections
            .unmodifiableSet(
                    EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

    private JsonStream() {
        // Static-only class
    }

    /**
     * Creates a stream from a JSON array.
     *
     * @param array
     *            the JSON array to create a stream from
     * @return a stream of JSON values
     */
    public static <T extends JsonValue> Stream<T> stream(JsonArray array) {
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
        return JsonStream.<JsonNumber> stream(array)
                .mapToDouble(JsonNumber::getNumber);
    }

    /**
     * Creates a collector that collects values into a JSON array.
     *
     * @return the collector
     */
    public static Collector<JsonValue, ?, JsonArray> asArray() {
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
        };
    }
}
