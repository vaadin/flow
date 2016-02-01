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

package com.vaadin.hummingbird.namespace;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.MapRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;

// Using ElementPropertiesNamespace since it closely maps to the underlying map
public class MapNamespaceTest
        extends AbstractNamespaceTest<ElementPropertiesNamespace> {
    private ElementPropertiesNamespace namespace = createNamespace();

    @Test
    public void testBasicFunctionality() {
        Assert.assertFalse(namespace.contains("key"));
        Assert.assertNull(namespace.get("key"));

        namespace.put("key", "value");
        Assert.assertTrue(namespace.contains("key"));
        Assert.assertEquals("value", namespace.get("key"));

        namespace.remove("key");
        Assert.assertFalse(namespace.contains("key"));
        Assert.assertNull(namespace.get("key"));
    }

    @Test
    public void testCollectChange() {
        List<NodeChange> initialChanges = collectChanges(namespace);
        Assert.assertEquals(0, initialChanges.size());

        namespace.put("key", "value");
        List<NodeChange> putChanges = collectChanges(namespace);
        Assert.assertEquals(1, putChanges.size());

        MapPutChange putChange = (MapPutChange) putChanges.get(0);
        Assert.assertEquals("key", putChange.getKey());
        Assert.assertEquals("value", putChange.getValue());

        namespace.put("key", null);
        List<NodeChange> putNullChanges = collectChanges(namespace);
        Assert.assertEquals(1, putNullChanges.size());

        MapPutChange putNullChange = (MapPutChange) putNullChanges.get(0);
        Assert.assertEquals("key", putNullChange.getKey());
        Assert.assertEquals(null, putNullChange.getValue());

        namespace.remove("key");

        List<NodeChange> removeChanges = collectChanges(namespace);
        Assert.assertEquals(1, removeChanges.size());

        MapRemoveChange removeChange = (MapRemoveChange) removeChanges.get(0);
        Assert.assertEquals("key", removeChange.getKey());
    }

    @Test
    public void testCoalescePutRemove() {
        namespace.put("key", "value");
        namespace.remove("key");

        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testCoalesceDoublePut() {
        namespace.put("key", "value1");
        namespace.put("key", "value2");

        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("value2",
                ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    public void testCoalescePutSame() {
        namespace.put("key", "value");
        collectChanges(namespace);

        namespace.put("key", "otherValue");
        namespace.put("key", "value");
        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testCoalesceRemovePut() {
        namespace.put("key", "value");
        collectChanges(namespace);

        namespace.remove("key");
        namespace.put("key", "value");

        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testResetChanges() {
        namespace.put("key", "value");
        collectChanges(namespace);

        namespace.resetChanges();

        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("value",
                ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    public void testCoalesceRemoveReset() {
        namespace.put("key", "value");
        collectChanges(namespace);

        namespace.resetChanges();
        namespace.remove("key");

        List<NodeChange> changes = collectChanges(namespace);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testNullKeyThrows() {
        assertFailsAssert("get(null)", () -> namespace.get(null));
        assertFailsAssert("contains(null)", () -> namespace.contains(null));
        assertFailsAssert("put(null, x)", () -> namespace.put(null, ""));
        assertFailsAssert("remove(null)", () -> namespace.remove(null));
    }

    private static void assertFailsAssert(String name, Runnable runnable) {
        boolean threw = false;
        try {
            runnable.run();
        } catch (AssertionError expected) {
            threw = true;
        }
        Assert.assertTrue(name + " should throw AssertionError", threw);
    }

}
