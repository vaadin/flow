/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import elemental.json.impl.JreJsonNull;

public class JsonUtilsTest {
    @Test
    public void testEquals() {
        // Equal
        assertTrue(
                JsonUtils.jsonEquals(Json.create(true), Json.create(true)));
        assertTrue(
                JsonUtils.jsonEquals(Json.create("foo"), Json.create("foo")));
        assertTrue(
                JsonUtils.jsonEquals(Json.create(3.14), Json.create(3.14)));
        assertTrue(
                JsonUtils.jsonEquals(Json.createNull(), Json.createNull()));
        assertTrue(
                JsonUtils.jsonEquals(createTestObject1(), createTestObject1()));
        assertTrue(
                JsonUtils.jsonEquals(createTestArray1(), createTestArray1()));

        // Non-equal with matching types
        assertFalse(
                JsonUtils.jsonEquals(Json.create(true), Json.create(false)));
        assertFalse(
                JsonUtils.jsonEquals(Json.create("foo"), Json.create("oof")));
        assertFalse(
                JsonUtils.jsonEquals(Json.create(3.14), Json.create(3.142)));
        assertFalse(
                JsonUtils.jsonEquals(createTestObject1(), createTestObject2()));
        assertFalse(
                JsonUtils.jsonEquals(createTestArray1(), createTestArray2()));

        // Non-equal with different types
        assertFalse(
                JsonUtils.jsonEquals(Json.create(true), Json.create("true")));
        assertFalse(
                JsonUtils.jsonEquals(Json.create(3.14), Json.create("3.14")));
        assertFalse(
                JsonUtils.jsonEquals(Json.createNull(), Json.create("null")));
        assertFalse(
                JsonUtils.jsonEquals(Json.createObject(), Json.create("{}")));
        assertFalse(
                JsonUtils.jsonEquals(Json.createArray(), Json.create(0)));
        assertFalse(
                JsonUtils.jsonEquals(createTestArray1(), createTestObject1()));
    }

    @Test(expected = AssertionError.class)
    public void testEquals_firstNull_throws() {
        JsonUtils.jsonEquals(null, Json.createNull());
    }

    @Test(expected = AssertionError.class)
    public void testEquals_secondNull_throws() {
        JsonUtils.jsonEquals(Json.createNull(), null);
    }

    private static JsonObject createTestObject1() {
        JsonObject object = Json.createObject();

        object.put("foo", "foo");
        object.put("bar", createTestArray1());
        object.put("baz", Json.createObject());

        return object;
    }

    private static JsonObject createTestObject2() {
        JsonObject object = Json.createObject();

        object.put("foo", "oof");
        object.put("bar", createTestArray2());
        object.put("baz", Json.createArray());

        return object;
    }

    private static JsonArray createTestArray1() {
        return Stream.of(Json.create("foo"), Json.createObject())
                .collect(JsonUtils.asArray());
    }

    private static JsonArray createTestArray2() {
        return Stream.of(Json.create("bar"), Json.createArray())
                .collect(JsonUtils.asArray());
    }

    @Test
    public void collectEmptyStream() {
        Stream<JsonValue> jsonValueStream = Stream.empty();
        JsonArray a = jsonValueStream.collect(JsonUtils.asArray());
        assertEquals(0, a.length());
    }

    public void createObjectStreamForNull() {
        assertEquals(Stream.empty(), JsonUtils.objectStream(null));
    }

    public void createNumberStreamForNull() {
        assertEquals(Stream.empty(), JsonUtils.numberStream(null));
    }

    public void createStreamForNull() {
        assertEquals(Stream.empty(), JsonUtils.stream(null));
    }

    @Test
    public void testStream() {
        JsonArray array = createTestArray1();
        List<JsonValue> list = JsonUtils.stream(array)
                .collect(Collectors.toList());

        assertEquals(2, list.size());
        assertEquals("foo", list.get(0).asString());
        assertTrue(
                JsonUtils.jsonEquals(list.get(1), Json.createObject()));
    }

    @Test
    public void testObjectStream() {
        JsonArray array = Stream.of(Json.createObject(), createTestObject1(),
                createTestObject2()).collect(JsonUtils.asArray());

        List<JsonObject> objects = JsonUtils.objectStream(array)
                .collect(Collectors.toList());

        assertEquals(3, objects.size());
        assertTrue(
                JsonUtils.jsonEquals(Json.createObject(), objects.get(0)));
        assertTrue(
                JsonUtils.jsonEquals(createTestObject1(), objects.get(1)));
        assertTrue(
                JsonUtils.jsonEquals(createTestObject2(), objects.get(2)));
    }

    @Test
    public void testNumberStream() {
        double[] values = new double[] { 3.14, 42, Double.MAX_VALUE };

        JsonArray array = DoubleStream.of(values).mapToObj(Json::create)
                .collect(JsonUtils.asArray());

        DoubleStream numberStream = JsonUtils.numberStream(array);

        assertArrayEquals(values, numberStream.toArray(), 0);
    }

    @Test
    public void testAsArray() {
        Stream<JsonValue> stream = JsonUtils.stream(createTestArray1());

        JsonArray array = stream.collect(JsonUtils.asArray());

        assertTrue(JsonUtils.jsonEquals(createTestArray1(), array));
    }

    @Test
    public void testCreateArray() {
        JsonArray array = JsonUtils.createArray(Json.create("string"),
                Json.createNull());

        assertEquals(2, array.length());
        assertEquals("string", array.getString(0));
        assertSame(JreJsonNull.class, array.get(1).getClass());
    }

    @Test
    public void testCreateEmptyArray() {
        JsonArray array = JsonUtils.createArray();

        assertEquals(0, array.length());
    }

}
