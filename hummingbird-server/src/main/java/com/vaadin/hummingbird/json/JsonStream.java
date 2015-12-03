package com.vaadin.hummingbird.json;

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

public class JsonStream {
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

    public static Stream<JsonObject> objectStream(JsonArray array) {
        return stream(array);
    }

    public static DoubleStream numberStream(JsonArray array) {
        return JsonStream.<JsonNumber> stream(array)
                .mapToDouble(JsonNumber::getNumber);
    }

    private static final Set<Collector.Characteristics> characteristics = Collections
            .unmodifiableSet(
                    EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

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
                return characteristics;
            }
        };
    }
}
