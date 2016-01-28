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

package com.vaadin.client.hummingbird.collection;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.client.ClientEngineTestBase;

public class GwtCollectionTest extends ClientEngineTestBase {

    public void testArray() {
        JsArray<String> array = JsCollections.array();

        assertEquals(0, array.length());

        array.push("foo");

        assertEquals(1, array.length());

        assertEquals("foo", array.get(0));

        array.set(0, "bar");

        assertEquals("bar", array.get(0));
    }

    public void testArraySplice() {
        JsArray<String> array = JsCollections.array();

        // 1, 2
        array.splice(0, 0, "1", "2");

        // 1, 1.3, 1.7, 2
        JsArray<String> noneRemoved = array.splice(1, 0, "1.3", "1.7");

        assertEquals(0, noneRemoved.length());
        assertEquals(4, array.length());
        assertEquals("1.3", array.get(1));

        // 1, 2
        JsArray<String> twoRemoved = array.splice(1, 2);
        assertEquals(2, twoRemoved.length());
        assertEquals("1.7", twoRemoved.get(1));

        assertEquals(2, array.length());
        assertEquals("2", array.get(1));
    }

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
        assertNull(map.get("Threee"));

        assertTrue(map.delete("One"));
        assertFalse(map.delete("Three"));
        assertFalse(map.has("One"));

        map.clear();
        assertEquals(0, map.size());
        assertFalse(map.has("Two"));
    }

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
