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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;

import com.vaadin.client.ClientEngineTestBase;

public class GwtJsSetTest extends ClientEngineTestBase {

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

    public void testSetIsEmpty() {
        JsSet<String> set = JsCollections.set();
        Assert.assertTrue(JsCollections.isEmpty(set));
        // 1, 2, 3
        set.add("1");
        assertFalse(JsCollections.isEmpty(set));
        set.add("2");
        assertFalse(JsCollections.isEmpty(set));
        set.delete("1");
        assertFalse(JsCollections.isEmpty(set));
        set.delete("2");
        assertTrue(JsCollections.isEmpty(set));
    }

}
