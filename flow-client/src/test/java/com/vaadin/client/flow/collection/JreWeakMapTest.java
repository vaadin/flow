/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

public class JreWeakMapTest {

    @Test
    public void testBasicMapOperations() {
        Object one = new Object();
        Object two = new Object();
        Object three = new Object();

        JsWeakMap<Object, Integer> map = JsCollections.weakMap();

        map.set(one, 1).set(two, 2);

        Assert.assertTrue(map.has(one));
        Assert.assertTrue(map.has(two));
        Assert.assertFalse(map.has(three));

        Assert.assertEquals(1, (int) map.get(one));
        Assert.assertEquals(2, (int) map.get(two));
        Assert.assertNull(map.get(three));

        Assert.assertTrue(map.delete(one));
        Assert.assertFalse(map.delete(three));
        Assert.assertFalse(map.has(one));
    }

    @Test
    public void testOnlyObjectKeysAllowed() {
        // All types directly mapped to native counterparts in GWT (Integer is
        // still boxed)
        assertBadKey("string");
        assertBadKey(Double.valueOf(0));
        assertBadKey(Boolean.TRUE);
    }

    private static void assertBadKey(Object key) {
        try {
            JsWeakMap<Object, String> map = JsCollections.weakMap();
            map.set(key, "value");
            Assert.fail("set should throw for " + key);
        } catch (Exception expected) {
            // All is ok
        }
    }
}
