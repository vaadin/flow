/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.collection;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class JreMapTest {

    @Test
    public void testMap() {
        JsMap<String, Integer> map = JsCollections.map();

        Assert.assertEquals(0, map.size());

        map.set("One", 1).set("Two", 2);

        Assert.assertEquals(2, map.size());

        Assert.assertTrue(map.has("One"));
        Assert.assertTrue(map.has("Two"));
        Assert.assertFalse(map.has("Three"));

        Assert.assertEquals(1, (int) map.get("One"));
        Assert.assertEquals(2, (int) map.get("Two"));
        Assert.assertNull(map.get("Three"));

        Assert.assertTrue(map.delete("One"));
        Assert.assertFalse(map.delete("Three"));
        Assert.assertFalse(map.has("One"));

        map.clear();
        Assert.assertEquals(0, map.size());
        Assert.assertFalse(map.has("Two"));
    }

    @Test
    public void testMapValues() {
        JsMap<String, Integer> map = JsCollections.map();

        map.set("One", 1).set("Two", 2);

        JsArray<Integer> values = map.mapValues();

        Assert.assertEquals(2, values.length());

        Assert.assertEquals(1, values.get(0).intValue());
        Assert.assertEquals(2, values.get(1).intValue());

        map.delete("One");

        values = map.mapValues();
        Assert.assertEquals(1, values.length());
        Assert.assertEquals(2, values.get(0).intValue());

        map.clear();
        values = map.mapValues();
        Assert.assertEquals(0, values.length());
    }

    @Test
    public void testMapForEach() {
        Map<String, Integer> seenValues = new HashMap<>();

        JsMap<String, Integer> map = JsCollections.map();

        map.set("One", 1).set("Two", 2);

        map.forEach((value, key) -> seenValues.put(key, value));

        Map<String, Integer> expectedValues = new HashMap<>();
        expectedValues.put("One", 1);
        expectedValues.put("Two", 2);

        Assert.assertEquals(expectedValues, seenValues);
    }
}
