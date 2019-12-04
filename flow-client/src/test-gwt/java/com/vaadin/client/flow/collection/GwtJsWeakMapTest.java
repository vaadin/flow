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

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;

public class GwtJsWeakMapTest extends ClientEngineTestBase {

    public void testBasicMapOperations() {
        Object one = new Object();
        Object two = new Object();
        Object three = new Object();

        JsWeakMap<Object, Integer> map = JsCollections.weakMap();

        map.set(one, 1).set(two, 2);

        assertTrue(map.has(one));
        assertTrue(map.has(two));
        assertFalse(map.has(three));

        assertEquals(1, (int) map.get(one));
        assertEquals(2, (int) map.get(two));
        assertNull(map.get(three));

        assertTrue(map.delete(one));
        assertFalse(map.delete(three));
        assertFalse(map.has(one));
    }

    public void testOnlyObjectKeysAllowed() {
        assertBadKey("string");
        // Other numeric types than Double are still boxed by GWT
        assertBadKey(Double.valueOf(0));
        assertBadKey(Boolean.TRUE);
    }

    private static void assertBadKey(Object key) {
        try {
            JsWeakMap<Object, String> map = JsCollections.weakMap();
            map.set(key, "value");
            fail("set should throw for " + key);
        } catch (Exception expected) {
            // All is ok
        }
    }

    public void testCanCast() {
        // Ok if this doesn't throw ClassCastException
        JsWeakMap<Object, Object> map = WidgetUtil
                .crazyJsCast(JsCollections.weakMap());
        assertNotNull(map);
    }

}
