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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JreSetTest {

    @Test
    public void testSet() {
        JsSet<Integer> set = JsCollections.set();

        assertEquals(0, set.size());

        set.add(1).add(2);

        assertEquals(2, set.size());

        assertTrue(set.has(1));
        assertTrue(set.has(2));
        assertFalse(set.has(3));

        assertTrue(set.delete(1));
        assertFalse(set.delete(3));
        assertFalse(set.has(1));

        set.clear();
        assertEquals(0, set.size());
        assertFalse(set.has(2));
    }

    @Test
    public void testSetForEach() {
        Set<Integer> seenValues = new HashSet<>();

        JsSet<Integer> set = JsCollections.set();

        set.add(1).add(2);

        set.forEach((value) -> seenValues.add(value));

        Set<Integer> expectedValues = new HashSet<>();
        expectedValues.add(1);
        expectedValues.add(2);

        assertEquals(expectedValues, seenValues);
    }

    @Test
    public void testSetIsEmpty() {
        JsSet<String> set = JsCollections.set();
        assertTrue(set.isEmpty());
        // 1, 2, 3
        set.add("1");
        assertFalse(set.isEmpty());
        set.add("2");
        assertFalse(set.isEmpty());
        set.delete("1");
        assertFalse(set.isEmpty());
        set.delete("2");
        assertTrue(set.isEmpty());
    }

    @Test
    public void testCopyConstructor() {
        JsSet<String> set = JsCollections.set();
        set.add("1").add("2");

        JsSet<String> copy = JsCollections.set(set);

        assertEquals(2, copy.size());
        assertTrue(copy.has("1"));
        assertTrue(copy.has("2"));
        assertFalse(copy.has("3"));
    }
}
