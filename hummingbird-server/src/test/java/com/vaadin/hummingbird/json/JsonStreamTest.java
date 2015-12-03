package com.vaadin.hummingbird.json;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

public class JsonStreamTest {

    @Test
    public void streamJsonArray() {
        JsonArray array = Json.createArray();

        array.set(0, Json.create(true));
        array.set(1, Json.create(42));

        List<JsonValue> values = JsonStream.stream(array)
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(array.get(0), array.get(1)), values);
    }

    @Test
    public void streamNumberArray() {
        JsonArray numbers = IntStream.range(0, 10).mapToObj(Json::create)
                .collect(JsonStream.asArray());

        double sum = JsonStream.numberStream(numbers).sum();

        Assert.assertEquals(IntStream.range(0, 10).sum(), (int) sum);
    }

    @Test
    public void streamObjectArray() {
        JsonArray objects = Stream.generate(Json::createObject).limit(5)
                .collect(JsonStream.asArray());

        JsonStream.objectStream(objects).forEach(o -> o.put("hello", "world"));

        List<String> list = JsonStream.objectStream(objects)
                .map(o -> o.getString("hello")).collect(Collectors.toList());
        Assert.assertEquals(Stream.generate(() -> "world").limit(5)
                .collect(Collectors.toList()), list);
    }

    @Test
    public void collectToArray() {
        List<JsonValue> list = Arrays.asList(Json.create(true),
                Json.create(42));

        JsonArray array = list.stream().collect(JsonStream.asArray());

        Assert.assertEquals(2, array.length());
        for (int i = 0; i < list.size(); i++) {
            Assert.assertSame(list.get(i), array.get(i));
        }
    }
}
