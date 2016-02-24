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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class SerializableJsonTest {
    @Test
    public void testSerializeDeserialize() {
        JsonArray testArray = Json.createArray();
        testArray.set(0, Json.createObject());
        testArray.set(1, Json.create("[]"));
        testArray.set(1, Json.createNull());

        JsonObject testObject = Json.createObject();
        testObject.put("foo", Json.createObject());
        testObject.put("bar", Json.createNull());

        JsonValue[] testValues = new JsonValue[] { Json.create(false),
                Json.create(3.14), Json.createNull(), Json.create("{}"),
                testArray, testObject };

        for (JsonValue value : testValues) {
            SerializableJson serializableJson = new SerializableJson(value);
            SerializableJson deserialized = SerializationUtils.deserialize(
                    SerializationUtils.serialize(serializableJson));

            Assert.assertNotSame(serializableJson, deserialized);

            if (value.getType() != JsonType.NULL) {
                // null is a singleton
                Assert.assertNotSame(
                        "Value was same as before serialization "
                                + value.toJson(),
                        value, deserialized.getValue());
            }

            Assert.assertEquals(value.toJson(),
                    deserialized.getValue().toJson());
        }
    }

    @Test(expected = AssertionError.class)
    public void testNullConstructor() {
        new SerializableJson(null);
    }

    @Test
    public void testSerializableMapJsonWrapped() {
        Map<String, Object> input = Collections.singletonMap("foo",
                Json.create(true));

        HashMap<String, Object> result = SerializableJson
                .createSerializableMap(input);

        // Input should not be modified
        Assert.assertNotEquals(input, result);

        Assert.assertEquals(input.keySet(), result.keySet());

        SerializableJson wrapped = (SerializableJson) result.get("foo");
        Assert.assertEquals("true", wrapped.getValue().toJson());
    }

    @Test
    public void testSerializableMapObjectNotWrapped() {
        Map<String, Object> input = Collections.singletonMap("foo",
                new SerializableJson(Json.create("bar")));

        HashMap<String, Object> result = SerializableJson
                .createSerializableMap(input);

        Assert.assertNotSame(input, result);
        Assert.assertEquals(input, result);
    }

    @Test
    public void testUnwrapMap() {
        HashMap<Object, Object> map = new HashMap<Object, Object>();

        Object object = new Object();
        map.put("foo", object);
        map.put("bar", new SerializableJson(Json.createObject()));
        map.put("baz", Json.createObject());

        SerializableJson.unwrapMap(map);

        Assert.assertEquals(3, map.size());
        Assert.assertSame(object, map.get("foo"));
        Assert.assertEquals("{}", ((JsonValue) map.get("bar")).toJson());
        Assert.assertEquals("{}", ((JsonValue) map.get("baz")).toJson());
    }

    @Test
    public void testSerializableListJsonWrapped() {
        List<Object> input = Collections.singletonList(Json.create(true));

        ArrayList<Object> result = SerializableJson
                .createSerializableList(input);

        // Input should not be modified
        Assert.assertNotEquals(input, result);

        SerializableJson wrapped = (SerializableJson) result.get(0);
        Assert.assertEquals("true", wrapped.getValue().toJson());
    }

    @Test
    public void testSerializableListObjectNotWrapped() {
        List<Object> input = Collections
                .singletonList(new SerializableJson(Json.create("bar")));

        ArrayList<Object> result = SerializableJson
                .createSerializableList(input);

        Assert.assertNotSame(input, result);
        Assert.assertEquals(input, result);
    }

    @Test
    public void testUnwrapList() {
        ArrayList<Object> list = new ArrayList<Object>();

        Object object = new Object();
        list.add(object);
        list.add(new SerializableJson(Json.createObject()));
        list.add(Json.createObject());

        SerializableJson.unwrapList(list);

        Assert.assertEquals(3, list.size());
        Assert.assertSame(object, list.get(0));
        Assert.assertEquals("{}", ((JsonValue) list.get(1)).toJson());
        Assert.assertEquals("{}", ((JsonValue) list.get(2)).toJson());
    }

}
