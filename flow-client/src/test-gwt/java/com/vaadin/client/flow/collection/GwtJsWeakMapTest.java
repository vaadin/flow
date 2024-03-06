/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
