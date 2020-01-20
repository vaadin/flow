/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;

public class GwtJsArrayTest extends ClientEngineTestBase {

    public void testArray() {
        JsArray<String> array = JsCollections.array();

        assertEquals(0, array.length());

        array.push("foo");

        assertEquals(1, array.length());

        assertEquals("foo", array.get(0));

        array.set(0, "bar");

        assertEquals("bar", array.get(0));
    }

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
        assertTrue(array.isEmpty());
        // 1, 2, 3
        array.push("1");
        assertFalse(array.isEmpty());
        array.push("2");
        assertFalse(array.isEmpty());
        array.remove(0);
        assertFalse(array.isEmpty());
        array.remove(0);
        assertTrue(array.isEmpty());
    }

    @SafeVarargs
    private final <T> void assertArray(JsArray<T> array, T... values) {
        assertEquals(values.length, array.length());
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], array.get(i));
        }
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

    public void testArraySpliceArray() {
        JsArray<Object> array = JsCollections.array("1", "2");

        array.spliceArray(1, 1, JsCollections.array("3", "4"));

        assertArray(array, "1", "3", "4");
    }

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

    public void testArrayPushSelf() {
        JsArray<String> array = JsCollections.array();
        // 1, 2
        array.push("1");
        array.push("2");

        array.pushArray(array);

        assertArray(array, "1", "2", "1", "2");
    }

    /**
     * Tests that it's possible to cast an instance to its own type.
     *
     * Most of the JS produced by GWT does not make any assertions about types,
     * but explicit casts and some use of generics leads to code that might do a
     * JavaScript instanceof check for @JsType classes, thus failing if the type
     * defined in the annotation doesn't match the runtime type.
     */
    public void testCanCast() {
        // Ok if this doesn't throw ClassCastException
        JsArray<Object> array = WidgetUtil.crazyJsCast(JsCollections.array());
        assertNotNull(array);
        // Ok if this doesn't throw ClassCastException
        JsArray<Object> array2 = WidgetUtil
                .crazyJsCast(JsCollections.array("a", "b"));
        assertNotNull(array2);
    }

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
