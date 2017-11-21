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

package com.vaadin.client.flow.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class JreMapTest {

    @Test
    public void testMap() {
        JsMap<String, Integer> map = JsCollections.map();

        assertEquals(0, map.size());

        map.set("One", 1).set("Two", 2);

        assertEquals(2, map.size());

        assertTrue(map.has("One"));
        assertTrue(map.has("Two"));
        assertFalse(map.has("Three"));

        assertEquals(1, (int) map.get("One"));
        assertEquals(2, (int) map.get("Two"));
        assertNull(map.get("Three"));

        assertTrue(map.delete("One"));
        assertFalse(map.delete("Three"));
        assertFalse(map.has("One"));

        map.clear();
        assertEquals(0, map.size());
        assertFalse(map.has("Two"));
    }

    @Test
    public void testMapValues() {
        JsMap<String, Integer> map = JsCollections.map();

        map.set("One", 1).set("Two", 2);

        JsArray<Integer> values = JsCollections.mapValues(map);

        assertEquals(2, values.length());

        assertEquals(1, values.get(0).intValue());
        assertEquals(2, values.get(1).intValue());

        map.delete("One");

        values = JsCollections.mapValues(map);
        assertEquals(1, values.length());
        assertEquals(2, values.get(0).intValue());

        map.clear();
        values = JsCollections.mapValues(map);
        assertEquals(0, values.length());
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

        assertEquals(expectedValues, seenValues);
    }
}
