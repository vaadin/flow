/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.collection.jre.JreJsArray;

@SuppressWarnings("deprecation")
public class JreArrayTest {
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
    public void testArrayWithValues() {
        JsArray<String> array = JsCollections.array("1", "2");

        assertArray(array, "1", "2");
    }

    @Test
    public void testAppendUsingSet() {
        JsArray<String> array = JsCollections.array();
        array.set(0, "0");
        assertArray(array, "0");
        array.set(1, "1");
        assertArray(array, "0", "1");
    }

    @Test
    public void testArrayRemove() {
        JsArray<String> array = JsCollections.array();

        // 1, 2, 3
        array.push("1");
        array.push("2");
        array.push("3");

        array.remove(1);
        assertArray(array, "1", "3");
        array.remove(1);
        assertArray(array, "1");
        array.remove(0);
        assertArray(array);
    }

    @Test
    public void testArrayClear() {
        JsArray<String> array = JsCollections.array();

        // 1, 2, 3
        array.push("1");
        array.push("2");
        array.push("3");

        array.clear();
        assertArray(array);
    }

    @Test
    public void testEmptyArrayClear() {
        JsArray<String> array = JsCollections.array();
        array.clear();
        assertArray(array);
    }

    @Test
    public void testArrayIsEmpty() {
        JsArray<String> array = JsCollections.array();
        Assert.assertTrue(array.isEmpty());
        // 1, 2, 3
        array.push("1");
        Assert.assertFalse(array.isEmpty());
        array.push("2");
        Assert.assertFalse(array.isEmpty());
        array.remove(0);
        Assert.assertFalse(array.isEmpty());
        array.remove(0);
        Assert.assertTrue(array.isEmpty());
    }

    @SafeVarargs
    private final <T> void assertArray(JsArray<T> array, T... values) {
        Assert.assertEquals(values.length, array.length());
        for (int i = 0; i < values.length; i++) {
            Assert.assertEquals(values[i], array.get(i));
        }
    }

    @Test
    public void testArraySplice() {
        JsArray<String> array = JsCollections.array();

        // 1, 2
        array.splice(0, 0, "1", "2");

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
    public void testArraySpliceArray() {
        JsArray<Object> array = JsCollections.array("1", "2");

        array.spliceArray(1, 1, JsCollections.array("3", "4"));

        assertArray(array, "1", "3", "4");
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
    public void testArrayPush() {
        JsArray<String> array = JsCollections.array();
        JsArray<String> source = JsCollections.array();

        // 1, 2
        source.push("1");
        source.push("2");

        array.pushArray(source);
        assertArray(array, "1", "2");

        array.pushArray(source);
        assertArray(array, "1", "2", "1", "2");

        array.push("3", "4");
        assertArray(array, "1", "2", "1", "2", "3", "4");
    }

    @Test
    public void testArrayAddAllSelf() {
        JsArray<String> array = JsCollections.array();
        // 1, 2
        array.push("1");
        array.push("2");

        array.pushArray(array);

        assertArray(array, "1", "2", "1", "2");
    }

    @Test
    public void testShift() {
        JsArray<String> array = JsCollections.array();
        array.push("1");
        array.push("2");
        array.push("3");
        assertEquals("1", array.shift());
        assertEquals(2, array.length());
        assertEquals("2", array.shift());
        assertEquals(1, array.length());
        assertEquals("3", array.shift());
        assertEquals(0, array.length());
        assertEquals(null, array.shift());
        assertEquals(0, array.length());
    }

    @Test
    public void testForEach() {
        List<Integer> seenValues = new ArrayList<>();

        JsArray<Integer> array = JsCollections.array();

        array.push(1, 2, 3, 4);

        array.forEach((value) -> seenValues.add(value));

        List<Integer> expectedValues = new ArrayList<>();
        expectedValues.addAll(Arrays.asList(new Integer[] { 1, 2, 3, 4 }));

        assertEquals(expectedValues, seenValues);
    }

}
