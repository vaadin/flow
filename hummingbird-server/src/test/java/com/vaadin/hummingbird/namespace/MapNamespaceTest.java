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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.MapRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;

import elemental.json.Json;
import elemental.json.JsonValue;

// Using ElementPropertiesNamespace since it closely maps to the underlying map
public class MapNamespaceTest
        extends AbstractNamespaceTest<ElementPropertyNamespace> {
    private static final String KEY = "key";
    private ElementPropertyNamespace namespace = createNamespace();

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

    @Test
    public void testPutAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        Assert.assertNull(child.getParent());

        namespace.put("key", child);

        Assert.assertSame(namespace.getNode(), child.getParent());

        namespace.put("key", "foo");

        Assert.assertNull(child.getParent());
    }

    @Test
    public void testRemoveDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        namespace.put("key", child);

        Assert.assertSame(namespace.getNode(), child.getParent());

        namespace.remove("key");

        Assert.assertNull(child.getParent());
    }

    @Test
    public void testSerializable() {
        namespace.put("string", "bar");
        namespace.put("null", null);
        namespace.put("boolean", Boolean.TRUE);
        namespace.put("number", Double.valueOf(5));

        namespace.putJson("jsonString", Json.create("bar"));
        namespace.putJson("jsonNull", Json.createNull());
        namespace.putJson("jsonBoolean", Json.create(true));
        namespace.putJson("jsonNumber", Json.create(5));
        namespace.putJson("jsonObject", Json.createObject());
        namespace.putJson("jsonArray", Json.createArray());

        Map<String, Object> values = new HashMap<>();
        namespace.keySet().forEach(key -> values.put(key, namespace.get(key)));

        MapNamespace copy = SerializationUtils
                .deserialize(SerializationUtils.serialize(namespace));

        Assert.assertNotSame(namespace, copy);

        Assert.assertEquals(values.keySet(), copy.keySet());
        // Also verify that original value wasn't changed by the serialization
        Assert.assertEquals(values.keySet(), namespace.keySet());

        values.keySet().forEach(key -> {
            if (key.startsWith("json")) {
                // Json values are not equals
                JsonValue originalValue = (JsonValue) namespace.get(key);
                JsonValue copyValue = (JsonValue) copy.get(key);

                Assert.assertEquals(originalValue.toJson(), copyValue.toJson());
            } else {
                Assert.assertEquals(namespace.get(key), copy.get(key));
            }

            // Verify original was not touched
            Assert.assertSame(values.get(key), namespace.get(key));
        });
    }

    @Test
    public void testGetIntDefaultValue() {
        Assert.assertEquals(12, namespace.getOrDefault(KEY, 12));

        namespace.put(KEY, 24);
        Assert.assertEquals(24, namespace.getOrDefault(KEY, 12));

        namespace.put(KEY, null);
        Assert.assertEquals(12, namespace.getOrDefault(KEY, 12));

        namespace.remove(KEY);
        Assert.assertEquals(12, namespace.getOrDefault(KEY, 12));
    }

    @Test
    public void testGetBooleanDefaultValue() {
        Assert.assertTrue(namespace.getOrDefault(KEY, true));
        Assert.assertFalse(namespace.getOrDefault(KEY, false));

        namespace.put(KEY, true);
        Assert.assertTrue(namespace.getOrDefault(KEY, false));

        namespace.put(KEY, null);
        Assert.assertTrue(namespace.getOrDefault(KEY, true));
        Assert.assertFalse(namespace.getOrDefault(KEY, false));

        namespace.remove(KEY);
        Assert.assertTrue(namespace.getOrDefault(KEY, true));
        Assert.assertFalse(namespace.getOrDefault(KEY, false));
    }

    @Test
    public void testGetStringDefaultValue() {
        Assert.assertEquals("default", namespace.getOrDefault(KEY, "default"));

        namespace.put(KEY, "assigned");
        Assert.assertEquals("assigned", namespace.getOrDefault(KEY, "default"));

        namespace.put(KEY, null);
        Assert.assertEquals("default", namespace.getOrDefault(KEY, "default"));

        namespace.remove(KEY);
        Assert.assertEquals("default", namespace.getOrDefault(KEY, "default"));
    }

}
