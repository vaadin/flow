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

package com.vaadin.hummingbird.nodefeature;

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
import com.vaadin.server.Command;

import elemental.json.Json;
import elemental.json.JsonValue;

// Using ElementProperties since it closely maps to the underlying map
public class NodeMapTest extends AbstractNodeFeatureTest<ElementPropertyMap> {
    private static final String KEY = "key";
    private ElementPropertyMap nodeMap = createFeature();

    @Test
    public void testBasicFunctionality() {
        Assert.assertFalse(nodeMap.contains("key"));
        Assert.assertNull(nodeMap.get("key"));

        nodeMap.put("key", "value");
        Assert.assertTrue(nodeMap.contains("key"));
        Assert.assertEquals("value", nodeMap.get("key"));

        nodeMap.remove("key");
        Assert.assertFalse(nodeMap.contains("key"));
        Assert.assertNull(nodeMap.get("key"));
    }

    @Test
    public void testCollectChange() {
        List<NodeChange> initialChanges = collectChanges(nodeMap);
        Assert.assertEquals(0, initialChanges.size());

        nodeMap.put("key", "value");
        List<NodeChange> putChanges = collectChanges(nodeMap);
        Assert.assertEquals(1, putChanges.size());

        MapPutChange putChange = (MapPutChange) putChanges.get(0);
        Assert.assertEquals("key", putChange.getKey());
        Assert.assertEquals("value", putChange.getValue());

        nodeMap.put("key", null);
        List<NodeChange> putNullChanges = collectChanges(nodeMap);
        Assert.assertEquals(1, putNullChanges.size());

        MapPutChange putNullChange = (MapPutChange) putNullChanges.get(0);
        Assert.assertEquals("key", putNullChange.getKey());
        Assert.assertEquals(null, putNullChange.getValue());

        nodeMap.remove("key");

        List<NodeChange> removeChanges = collectChanges(nodeMap);
        Assert.assertEquals(1, removeChanges.size());

        MapRemoveChange removeChange = (MapRemoveChange) removeChanges.get(0);
        Assert.assertEquals("key", removeChange.getKey());
    }

    @Test
    public void testNoChangeEvent() {
        nodeMap.put("key", "value", false);
        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());
        nodeMap.put("key", "value", true);
        changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());
        nodeMap.put("key", "bar", true);
        changes = collectChanges(nodeMap);
        Assert.assertEquals(1, changes.size());
    }

    @Test
    public void testNoChangeOverwritesOldChanges() {
        nodeMap.put("key", "value", true);
        nodeMap.put("key", "foobar", false);
        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());

        nodeMap.put("key", "urk");
        changes = collectChanges(nodeMap);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("urk", ((MapPutChange) changes.get(0)).getValue());

    }

    @Test
    public void testCoalescePutRemove() {
        nodeMap.put("key", "value");
        nodeMap.remove("key");

        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testCoalesceDoublePut() {
        nodeMap.put("key", "value1");
        nodeMap.put("key", "value2");

        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("value2",
                ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    public void testCoalescePutSame() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.put("key", "otherValue");
        nodeMap.put("key", "value");
        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testCoalesceRemovePut() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.remove("key");
        nodeMap.put("key", "value");

        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testResetChanges() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.resetChanges();

        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals("value",
                ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    public void testCoalesceRemoveReset() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.resetChanges();
        nodeMap.remove("key");

        List<NodeChange> changes = collectChanges(nodeMap);
        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testNullKeyThrows() {
        assertFailsAssert("get(null)", () -> nodeMap.get(null));
        assertFailsAssert("contains(null)", () -> nodeMap.contains(null));
        assertFailsAssert("put(null, x)", () -> nodeMap.put(null, ""));
        assertFailsAssert("remove(null)", () -> nodeMap.remove(null));
    }

    private static void assertFailsAssert(String name, Command command) {
        boolean threw = false;
        try {
            command.execute();
        } catch (AssertionError expected) {
            threw = true;
        }
        Assert.assertTrue(name + " should throw AssertionError", threw);
    }

    @Test
    public void testPutAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        Assert.assertNull(child.getParent());

        nodeMap.put("key", child);

        Assert.assertSame(nodeMap.getNode(), child.getParent());

        nodeMap.put("key", "foo");

        Assert.assertNull(child.getParent());
    }

    @Test
    public void testRemoveDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        nodeMap.put("key", child);

        Assert.assertSame(nodeMap.getNode(), child.getParent());

        nodeMap.remove("key");

        Assert.assertNull(child.getParent());
    }

    @Test
    public void testSerializable() {
        nodeMap.put("string", "bar");
        nodeMap.put("null", null);
        nodeMap.put("boolean", Boolean.TRUE);
        nodeMap.put("number", Double.valueOf(5));

        nodeMap.put("jsonString", Json.create("bar"));
        nodeMap.put("jsonNull", Json.createNull());
        nodeMap.put("jsonBoolean", Json.create(true));
        nodeMap.put("jsonNumber", Json.create(5));
        nodeMap.put("jsonObject", Json.createObject());
        nodeMap.put("jsonArray", Json.createArray());

        Map<String, Object> values = new HashMap<>();
        nodeMap.keySet().forEach(key -> values.put(key, nodeMap.get(key)));

        NodeMap copy = SerializationUtils
                .deserialize(SerializationUtils.serialize(nodeMap));

        Assert.assertNotSame(nodeMap, copy);

        Assert.assertEquals(values.keySet(), copy.keySet());
        // Also verify that original value wasn't changed by the serialization
        Assert.assertEquals(values.keySet(), nodeMap.keySet());

        values.keySet().forEach(key -> {
            if (key.startsWith("json")) {
                // Json values are not equals
                JsonValue originalValue = (JsonValue) nodeMap.get(key);
                JsonValue copyValue = (JsonValue) copy.get(key);

                Assert.assertEquals(originalValue.toJson(), copyValue.toJson());
            } else {
                Assert.assertEquals(nodeMap.get(key), copy.get(key));
            }

            // Verify original was not touched
            Assert.assertSame(values.get(key), nodeMap.get(key));
        });
    }

    @Test
    public void testGetIntDefaultValue() {
        Assert.assertEquals(12, nodeMap.getOrDefault(KEY, 12));

        nodeMap.put(KEY, 24);
        Assert.assertEquals(24, nodeMap.getOrDefault(KEY, 12));

        nodeMap.put(KEY, null);
        Assert.assertEquals(12, nodeMap.getOrDefault(KEY, 12));

        nodeMap.remove(KEY);
        Assert.assertEquals(12, nodeMap.getOrDefault(KEY, 12));
    }

    @Test
    public void testGetBooleanDefaultValue() {
        Assert.assertTrue(nodeMap.getOrDefault(KEY, true));
        Assert.assertFalse(nodeMap.getOrDefault(KEY, false));

        nodeMap.put(KEY, true);
        Assert.assertTrue(nodeMap.getOrDefault(KEY, false));

        nodeMap.put(KEY, null);
        Assert.assertTrue(nodeMap.getOrDefault(KEY, true));
        Assert.assertFalse(nodeMap.getOrDefault(KEY, false));

        nodeMap.remove(KEY);
        Assert.assertTrue(nodeMap.getOrDefault(KEY, true));
        Assert.assertFalse(nodeMap.getOrDefault(KEY, false));
    }

    @Test
    public void testGetStringDefaultValue() {
        Assert.assertEquals("default", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.put(KEY, "assigned");
        Assert.assertEquals("assigned", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.put(KEY, null);
        Assert.assertEquals("default", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.remove(KEY);
        Assert.assertEquals("default", nodeMap.getOrDefault(KEY, "default"));
    }

    @Test
    public void testClear() {
        nodeMap.put("foo", 1);
        nodeMap.put("bar", 1);
        nodeMap.put("baz", 1);
        nodeMap.clear();
        Assert.assertEquals(0, nodeMap.getPropertyNames().count());
        Assert.assertFalse(nodeMap.hasProperty("foo"));
        Assert.assertFalse(nodeMap.hasProperty("bar"));
        Assert.assertFalse(nodeMap.hasProperty("baz"));
    }
}
