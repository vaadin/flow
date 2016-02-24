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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class JsonUtilTest {
    @Test
    public void testEquals() {
        // Equal
        Assert.assertTrue(
                JsonUtil.equals(Json.create(true), Json.create(true)));
        Assert.assertTrue(
                JsonUtil.equals(Json.create("foo"), Json.create("foo")));
        Assert.assertTrue(
                JsonUtil.equals(Json.create(3.14), Json.create(3.14)));
        Assert.assertTrue(
                JsonUtil.equals(Json.createNull(), Json.createNull()));
        Assert.assertTrue(
                JsonUtil.equals(createTestObject1(), createTestObject1()));
        Assert.assertTrue(
                JsonUtil.equals(createTestArray1(), createTestArray1()));

        // Non-equal with matching types
        Assert.assertFalse(
                JsonUtil.equals(Json.create(true), Json.create(false)));
        Assert.assertFalse(
                JsonUtil.equals(Json.create("foo"), Json.create("oof")));
        Assert.assertFalse(
                JsonUtil.equals(Json.create(3.14), Json.create(3.142)));
        Assert.assertFalse(
                JsonUtil.equals(createTestObject1(), createTestObject2()));
        Assert.assertFalse(
                JsonUtil.equals(createTestArray1(), createTestArray2()));

        // Non-equal with different types
        Assert.assertFalse(
                JsonUtil.equals(Json.create(true), Json.create("true")));
        Assert.assertFalse(
                JsonUtil.equals(Json.create(3.14), Json.create("3.14")));
        Assert.assertFalse(
                JsonUtil.equals(Json.createNull(), Json.create("null")));
        Assert.assertFalse(
                JsonUtil.equals(Json.createObject(), Json.create("{}")));
        Assert.assertFalse(JsonUtil.equals(Json.createArray(), Json.create(0)));
        Assert.assertFalse(
                JsonUtil.equals(createTestArray1(), createTestObject1()));
    }

    @Test(expected = AssertionError.class)
    public void testEquals_firstNull_throws() {
        JsonUtil.equals(null, Json.createNull());
    }

    @Test(expected = AssertionError.class)
    public void testEquals_secondNull_throws() {
        JsonUtil.equals(Json.createNull(), null);
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
                .collect(JsonUtil.asArray());
    }

    private static JsonArray createTestArray2() {
        return Stream.of(Json.create("bar"), Json.createArray())
                .collect(JsonUtil.asArray());
    }

    @Test
    public void testStream() {
        JsonArray array = createTestArray1();
        List<JsonValue> list = JsonUtil.stream(array)
                .collect(Collectors.toList());

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("foo", list.get(0).asString());
        Assert.assertTrue(JsonUtil.equals(list.get(1), Json.createObject()));
    }

    @Test
    public void testObjectStream() {
        JsonArray array = Stream.of(Json.createObject(), createTestObject1(),
                createTestObject2()).collect(JsonUtil.asArray());

        List<JsonObject> objects = JsonUtil.objectStream(array)
                .collect(Collectors.toList());

        Assert.assertEquals(3, objects.size());
        Assert.assertTrue(JsonUtil.equals(Json.createObject(), objects.get(0)));
        Assert.assertTrue(JsonUtil.equals(createTestObject1(), objects.get(1)));
        Assert.assertTrue(JsonUtil.equals(createTestObject2(), objects.get(2)));
    }

    @Test
    public void testNumberStream() {
        double[] values = new double[] { 3.14, 42, Double.MAX_VALUE };

        JsonArray array = DoubleStream.of(values).mapToObj(Json::create)
                .collect(JsonUtil.asArray());

        DoubleStream numberStream = JsonUtil.numberStream(array);

        Assert.assertArrayEquals(values, numberStream.toArray(), 0);
    }

    @Test
    public void testAsArray() {
        Stream<JsonValue> stream = JsonUtil.stream(createTestArray1());

        JsonArray array = stream.collect(JsonUtil.asArray());

        Assert.assertTrue(JsonUtil.equals(createTestArray1(), array));
    }

}
