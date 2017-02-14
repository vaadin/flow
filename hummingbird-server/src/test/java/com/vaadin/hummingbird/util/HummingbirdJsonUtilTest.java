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
import elemental.json.impl.JreJsonNull;

public class HummingbirdJsonUtilTest {
    @Test
    public void testEquals() {
        // Equal
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(Json.create(true), Json.create(true)));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(Json.create("foo"), Json.create("foo")));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(Json.create(3.14), Json.create(3.14)));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(Json.createNull(), Json.createNull()));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(createTestObject1(), createTestObject1()));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(createTestArray1(), createTestArray1()));

        // Non-equal with matching types
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.create(true), Json.create(false)));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.create("foo"), Json.create("oof")));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.create(3.14), Json.create(3.142)));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(createTestObject1(), createTestObject2()));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(createTestArray1(), createTestArray2()));

        // Non-equal with different types
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.create(true), Json.create("true")));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.create(3.14), Json.create("3.14")));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.createNull(), Json.create("null")));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.createObject(), Json.create("{}")));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(Json.createArray(), Json.create(0)));
        Assert.assertFalse(
                HummingbirdJsonUtil.jsonEquals(createTestArray1(), createTestObject1()));
    }

    @Test(expected = AssertionError.class)
    public void testEquals_firstNull_throws() {
        HummingbirdJsonUtil.jsonEquals(null, Json.createNull());
    }

    @Test(expected = AssertionError.class)
    public void testEquals_secondNull_throws() {
        HummingbirdJsonUtil.jsonEquals(Json.createNull(), null);
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
                .collect(HummingbirdJsonUtil.asArray());
    }

    private static JsonArray createTestArray2() {
        return Stream.of(Json.create("bar"), Json.createArray())
                .collect(HummingbirdJsonUtil.asArray());
    }

    @Test
    public void collectEmptyStream() {
        Stream<JsonValue> jsonValueStream = Stream.empty();
        JsonArray a = jsonValueStream.collect(HummingbirdJsonUtil.asArray());
        Assert.assertEquals(0, a.length());
    }

    @Test(expected = AssertionError.class)
    public void createObjectStreamForNull() {
        HummingbirdJsonUtil.objectStream(null);
    }

    @Test(expected = AssertionError.class)
    public void createNumberStreamForNull() {
        HummingbirdJsonUtil.numberStream(null);
    }

    @Test(expected = AssertionError.class)
    public void createStreamForNull() {
        HummingbirdJsonUtil.stream(null);
    }

    @Test
    public void testStream() {
        JsonArray array = createTestArray1();
        List<JsonValue> list = HummingbirdJsonUtil.stream(array)
                .collect(Collectors.toList());

        Assert.assertEquals(2, list.size());
        Assert.assertEquals("foo", list.get(0).asString());
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(list.get(1), Json.createObject()));
    }

    @Test
    public void testObjectStream() {
        JsonArray array = Stream.of(Json.createObject(), createTestObject1(),
                createTestObject2()).collect(HummingbirdJsonUtil.asArray());

        List<JsonObject> objects = HummingbirdJsonUtil.objectStream(array)
                .collect(Collectors.toList());

        Assert.assertEquals(3, objects.size());
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(Json.createObject(), objects.get(0)));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(createTestObject1(), objects.get(1)));
        Assert.assertTrue(
                HummingbirdJsonUtil.jsonEquals(createTestObject2(), objects.get(2)));
    }

    @Test
    public void testNumberStream() {
        double[] values = new double[] { 3.14, 42, Double.MAX_VALUE };

        JsonArray array = DoubleStream.of(values).mapToObj(Json::create)
                .collect(HummingbirdJsonUtil.asArray());

        DoubleStream numberStream = HummingbirdJsonUtil.numberStream(array);

        Assert.assertArrayEquals(values, numberStream.toArray(), 0);
    }

    @Test
    public void testAsArray() {
        Stream<JsonValue> stream = HummingbirdJsonUtil.stream(createTestArray1());

        JsonArray array = stream.collect(HummingbirdJsonUtil.asArray());

        Assert.assertTrue(HummingbirdJsonUtil.jsonEquals(createTestArray1(), array));
    }

    @Test
    public void testCreateArray() {
        JsonArray array = HummingbirdJsonUtil.createArray(Json.create("string"),
                Json.createNull());

        Assert.assertEquals(2, array.length());
        Assert.assertEquals("string", array.getString(0));
        Assert.assertSame(JreJsonNull.class, array.get(1).getClass());
    }

    @Test
    public void testCreateEmptyArray() {
        JsonArray array = HummingbirdJsonUtil.createArray();

        Assert.assertEquals(0, array.length());
    }

}
