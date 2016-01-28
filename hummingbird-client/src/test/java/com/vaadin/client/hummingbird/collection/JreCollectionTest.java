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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.hummingbird.collection.jre.JreJsArray;

public class JreCollectionTest {
    @Test
    public void testArray() {
        JsArray<String> array = JsCollections.array();

        Assert.assertEquals(0, array.length());

        array.push("foo");

        Assert.assertEquals(1, array.length());

        Assert.assertEquals("foo", array.get(0));

        array.set(0, "bar");

        Assert.assertEquals("bar", array.get(0));
    }

    @Test
    public void testArraySplice() {
        JsArray<String> array = JsCollections.array();

        // 1, 2
        array.push("1");
        array.push("2");

        // 1, 1.3, 1.7, 2
        JsArray<String> noneRemoved = array.splice(1, 0, "1.3", "1.7");

        Assert.assertEquals(0, noneRemoved.length());
        Assert.assertEquals(4, array.length());
        Assert.assertEquals("1.3", array.get(1));

        // 1, 2
        JsArray<String> twoRemoved = array.splice(1, 2);
        Assert.assertEquals(2, twoRemoved.length());
        Assert.assertEquals("1.7", twoRemoved.get(1));

        Assert.assertEquals(2, array.length());
        Assert.assertEquals("2", array.get(1));
    }

    @Test
    public void testJreJsArrayHasNoNative() {
        // Check that the JRE version overrides all native methods
        for (Method method : JreJsArray.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }

            Assert.assertFalse(JreJsArray.class.getSimpleName()
                    + " must override" + method,
                    Modifier.isNative(method.getModifiers()));
        }
    }

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
        Assert.assertNull(map.get("Threee"));

        Assert.assertTrue(map.delete("One"));
        Assert.assertFalse(map.delete("Three"));
        Assert.assertFalse(map.has("One"));

        map.clear();
        Assert.assertEquals(0, map.size());
        Assert.assertFalse(map.has("Two"));
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
