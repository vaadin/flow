/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateNodeTest;
import com.vaadin.flow.change.MapPutChange;
import com.vaadin.flow.change.MapRemoveChange;
import com.vaadin.flow.change.NodeChange;
import com.vaadin.server.Command;

import elemental.json.Json;
import elemental.json.JsonValue;

// Using ElementProperties since it closely maps to the underlying map
public class NodeMapTest extends AbstractNodeFeatureTest<ElementPropertyMap> {
    private static final String KEY = "key";
    private ElementPropertyMap nodeMap = createFeature();

    @Test
    public void testBasicFunctionality() {
        assertFalse(nodeMap.contains("key"));
        assertNull(nodeMap.get("key"));

        nodeMap.put("key", "value");
        assertTrue(nodeMap.contains("key"));
        assertEquals("value", nodeMap.get("key"));

        nodeMap.remove("key");
        assertFalse(nodeMap.contains("key"));
        assertNull(nodeMap.get("key"));
    }

    @Test
    public void testCollectChange() {
        List<NodeChange> initialChanges = collectChanges(nodeMap);
        assertEquals(0, initialChanges.size());

        nodeMap.put("key", "value");
        List<NodeChange> putChanges = collectChanges(nodeMap);
        assertEquals(1, putChanges.size());

        MapPutChange putChange = (MapPutChange) putChanges.get(0);
        assertEquals("key", putChange.getKey());
        assertEquals("value", putChange.getValue());

        nodeMap.put("key", null);
        List<NodeChange> putNullChanges = collectChanges(nodeMap);
        assertEquals(1, putNullChanges.size());

        MapPutChange putNullChange = (MapPutChange) putNullChanges.get(0);
        assertEquals("key", putNullChange.getKey());
        assertEquals(null, putNullChange.getValue());

        nodeMap.remove("key");

        List<NodeChange> removeChanges = collectChanges(nodeMap);
        assertEquals(1, removeChanges.size());

        MapRemoveChange removeChange = (MapRemoveChange) removeChanges.get(0);
        assertEquals("key", removeChange.getKey());
    }

    @Test
    public void testNoChangeEvent() {
        nodeMap.put("key", "value", false);
        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
        nodeMap.put("key", "value", true);
        changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
        nodeMap.put("key", "bar", true);
        changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
    }

    @Test
    public void testNoChangeOverwritesOldChanges() {
        nodeMap.put("key", "value", true);
        nodeMap.put("key", "foobar", false);
        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());

        nodeMap.put("key", "urk");
        changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
        assertEquals("urk", ((MapPutChange) changes.get(0)).getValue());

    }

    @Test
    public void testCoalescePutRemove() {
        nodeMap.put("key", "value");
        nodeMap.remove("key");

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    public void testCoalesceDoublePut() {
        nodeMap.put("key", "value1");
        nodeMap.put("key", "value2");

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
        assertEquals("value2",
                ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    public void testCoalescePutSame() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.put("key", "otherValue");
        nodeMap.put("key", "value");
        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    public void testCoalesceRemovePut() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.remove("key");
        nodeMap.put("key", "value");

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    public void testResetChanges() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.generateChangesFromEmpty();

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
        assertEquals("value",
                ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    public void testCoalesceRemoveReset() {
        nodeMap.put("key", "value");
        collectChanges(nodeMap);

        nodeMap.generateChangesFromEmpty();
        nodeMap.remove("key");

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
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
        assertTrue(name + " should throw AssertionError", threw);
    }

    @Test
    public void testPutAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        assertNull(child.getParent());

        nodeMap.put("key", child);

        assertSame(nodeMap.getNode(), child.getParent());

        nodeMap.put("key", "foo");

        assertNull(child.getParent());
    }

    @Test
    public void testRemoveDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        nodeMap.put("key", child);

        assertSame(nodeMap.getNode(), child.getParent());

        nodeMap.remove("key");

        assertNull(child.getParent());
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

        assertNotSame(nodeMap, copy);

        assertEquals(values.keySet(), copy.keySet());
        // Also verify that original value wasn't changed by the serialization
        assertEquals(values.keySet(), nodeMap.keySet());

        values.keySet().forEach(key -> {
            if (key.startsWith("json")) {
                // Json values are not equals
                JsonValue originalValue = (JsonValue) nodeMap.get(key);
                JsonValue copyValue = (JsonValue) copy.get(key);

                assertEquals(originalValue.toJson(), copyValue.toJson());
            } else {
                assertEquals(nodeMap.get(key), copy.get(key));
            }

            // Verify original was not touched
            assertSame(values.get(key), nodeMap.get(key));
        });
    }

    @Test
    public void testGetIntDefaultValue() {
        assertEquals(12, nodeMap.getOrDefault(KEY, 12));

        nodeMap.put(KEY, 24);
        assertEquals(24, nodeMap.getOrDefault(KEY, 12));

        nodeMap.put(KEY, null);
        assertEquals(12, nodeMap.getOrDefault(KEY, 12));

        nodeMap.remove(KEY);
        assertEquals(12, nodeMap.getOrDefault(KEY, 12));
    }

    @Test
    public void testGetBooleanDefaultValue() {
        assertTrue(nodeMap.getOrDefault(KEY, true));
        assertFalse(nodeMap.getOrDefault(KEY, false));

        nodeMap.put(KEY, true);
        assertTrue(nodeMap.getOrDefault(KEY, false));

        nodeMap.put(KEY, null);
        assertTrue(nodeMap.getOrDefault(KEY, true));
        assertFalse(nodeMap.getOrDefault(KEY, false));

        nodeMap.remove(KEY);
        assertTrue(nodeMap.getOrDefault(KEY, true));
        assertFalse(nodeMap.getOrDefault(KEY, false));
    }

    @Test
    public void testGetStringDefaultValue() {
        assertEquals("default", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.put(KEY, "assigned");
        assertEquals("assigned", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.put(KEY, null);
        assertEquals("default", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.remove(KEY);
        assertEquals("default", nodeMap.getOrDefault(KEY, "default"));
    }

    @Test
    public void testClear() {
        nodeMap.put("foo", 1);
        nodeMap.put("bar", 1);
        nodeMap.put("baz", 1);
        nodeMap.clear();
        assertEquals(0, nodeMap.getPropertyNames().count());
        assertFalse(nodeMap.hasProperty("foo"));
        assertFalse(nodeMap.hasProperty("bar"));
        assertFalse(nodeMap.hasProperty("baz"));
    }
}
