/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.BaseJsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.MapRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.server.Command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// Using ElementStylePropertyMap since it closely maps to the underlying map
class NodeMapTest extends AbstractNodeFeatureTest<ElementStylePropertyMap> {
    private static final String KEY = "key";
    private ElementStylePropertyMap nodeMap = createFeature();

    private static class AlwaysProduceChangeMap extends NodeMap {

        AlwaysProduceChangeMap(StateNode node) {
            super(node);
        }

        @Override
        protected boolean producePutChange(String key, boolean hadValueEarlier,
                Serializable newValue) {
            return true;
        }

    }

    private static class NeverProduceChangeMap extends NodeMap {

        NeverProduceChangeMap(StateNode node) {
            super(node);
        }

        @Override
        protected boolean producePutChange(String key, boolean hadValueEarlier,
                Serializable newValue) {
            return false;
        }

    }

    @Test
    void testBasicFunctionality() {
        assertFalse(nodeMap.contains(KEY));
        assertNull(nodeMap.get(KEY));

        nodeMap.put(KEY, "value");
        assertTrue(nodeMap.contains(KEY));
        assertEquals("value", nodeMap.get(KEY));

        nodeMap.remove(KEY);
        assertFalse(nodeMap.contains(KEY));
        assertNull(nodeMap.get(KEY));
    }

    @Test
    void testCollectChange() {
        List<NodeChange> initialChanges = collectChanges(nodeMap);
        assertEquals(0, initialChanges.size());

        nodeMap.put(KEY, "value");
        List<NodeChange> putChanges = collectChanges(nodeMap);
        assertEquals(1, putChanges.size());

        MapPutChange putChange = (MapPutChange) putChanges.get(0);
        assertEquals(KEY, putChange.getKey());
        assertEquals("value", putChange.getValue());

        nodeMap.put(KEY, null);
        List<NodeChange> putNullChanges = collectChanges(nodeMap);
        assertEquals(1, putNullChanges.size());

        MapPutChange putNullChange = (MapPutChange) putNullChanges.get(0);
        assertEquals(KEY, putNullChange.getKey());
        assertEquals(null, putNullChange.getValue());

        nodeMap.remove(KEY);

        List<NodeChange> removeChanges = collectChanges(nodeMap);
        assertEquals(1, removeChanges.size());

        MapRemoveChange removeChange = (MapRemoveChange) removeChanges.get(0);
        assertEquals(KEY, removeChange.getKey());
    }

    @Test
    void testCollectChange_withSignalBinding() {
        // Signal and Registration instances are irrelevant in this test.
        nodeMap.put(KEY, new NodeMap.SignalBinding(null, null, "value", null));
        List<NodeChange> putChanges = collectChanges(nodeMap);

        assertEquals(1, putChanges.size());
        MapPutChange putChange = (MapPutChange) putChanges.get(0);
        assertEquals(KEY, putChange.getKey());
        assertEquals("value", putChange.getValue());
    }

    @Test
    void testNoChangeEvent() {
        nodeMap.put(KEY, "value", false);
        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
        nodeMap.put(KEY, "value", true);
        changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
        nodeMap.put(KEY, "bar", true);
        changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
    }

    @Test
    void testNoChangeOverwritesOldChanges() {
        nodeMap.put(KEY, "value", true);
        nodeMap.put(KEY, "foobar", false);
        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());

        nodeMap.put(KEY, "urk");
        changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
        assertEquals("urk", ((MapPutChange) changes.get(0)).getValue());

    }

    @Test
    void testCoalescePutRemove() {
        nodeMap.put(KEY, "value");
        nodeMap.remove(KEY);

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    void testCoalesceDoublePut() {
        nodeMap.put(KEY, "value1");
        nodeMap.put(KEY, "value2");

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
        assertEquals("value2", ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    void testCoalescePutSame() {
        nodeMap.put(KEY, "value");
        collectChanges(nodeMap);

        nodeMap.put(KEY, "otherValue");
        nodeMap.put(KEY, "value");
        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    void testCoalesceRemovePut() {
        nodeMap.put(KEY, "value");
        collectChanges(nodeMap);

        nodeMap.remove(KEY);
        nodeMap.put(KEY, "value");

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    void testResetChanges() {
        nodeMap.put(KEY, "value");
        collectChanges(nodeMap);

        nodeMap.generateChangesFromEmpty();

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(1, changes.size());
        assertEquals("value", ((MapPutChange) changes.get(0)).getValue());
    }

    @Test
    void testCoalesceRemoveReset() {
        nodeMap.put(KEY, "value");
        collectChanges(nodeMap);

        nodeMap.generateChangesFromEmpty();
        nodeMap.remove(KEY);

        List<NodeChange> changes = collectChanges(nodeMap);
        assertEquals(0, changes.size());
    }

    @Test
    void testNullKeyThrows() {
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
        assertTrue(threw, name + " should throw AssertionError");
    }

    @Test
    void testPutAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        assertNull(child.getParent());

        nodeMap.put(KEY, child);

        assertSame(nodeMap.getNode(), child.getParent());

        nodeMap.put(KEY, "foo");

        assertNull(child.getParent());
    }

    @Test
    void testRemoveDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        nodeMap.put(KEY, child);

        assertSame(nodeMap.getNode(), child.getParent());

        nodeMap.remove(KEY);

        assertNull(child.getParent());
    }

    @Test
    void testSerializable() {
        nodeMap.put("string", "bar");
        nodeMap.put("null", null);
        nodeMap.put("boolean", Boolean.TRUE);
        nodeMap.put("number", Double.valueOf(5));

        nodeMap.put("jsonString", JacksonUtils.writeValue("bar"));
        nodeMap.put("jsonNull", JacksonUtils.nullNode());
        nodeMap.put("jsonBoolean", JacksonUtils.writeValue(true));
        nodeMap.put("jsonNumber", JacksonUtils.writeValue(5));
        nodeMap.put("jsonObject", JacksonUtils.createObjectNode());
        nodeMap.put("jsonArray", JacksonUtils.createArrayNode());

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
                BaseJsonNode originalValue = (BaseJsonNode) nodeMap.get(key);
                BaseJsonNode copyValue = (BaseJsonNode) copy.get(key);

                assertEquals(originalValue.toString(), copyValue.toString());
            } else {
                assertEquals(nodeMap.get(key), copy.get(key));
            }

            // Verify original was not touched
            assertSame(values.get(key), nodeMap.get(key));
        });
    }

    @Test
    void testGetIntDefaultValue() {
        assertEquals(12, nodeMap.getOrDefault(KEY, 12));

        nodeMap.put(KEY, 24);
        assertEquals(24, nodeMap.getOrDefault(KEY, 12));

        nodeMap.put(KEY, null);
        assertEquals(12, nodeMap.getOrDefault(KEY, 12));

        nodeMap.remove(KEY);
        assertEquals(12, nodeMap.getOrDefault(KEY, 12));
    }

    @Test
    void testGetBooleanDefaultValue() {
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
    void testGetStringDefaultValue() {
        assertEquals("default", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.put(KEY, "assigned");
        assertEquals("assigned", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.put(KEY, null);
        assertEquals("default", nodeMap.getOrDefault(KEY, "default"));

        nodeMap.remove(KEY);
        assertEquals("default", nodeMap.getOrDefault(KEY, "default"));
    }

    @Test
    void testClear() {
        nodeMap.put("foo", 1);
        nodeMap.put("bar", 1);
        nodeMap.put("baz", 1);
        nodeMap.clear();
        assertEquals(0, nodeMap.getPropertyNames().count());
        assertFalse(nodeMap.hasProperty("foo"));
        assertFalse(nodeMap.hasProperty("bar"));
        assertFalse(nodeMap.hasProperty("baz"));
    }

    @Test
    void put_sameValue_hasNoEffect() {
        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        StateNode child = new StateNode();

        AtomicBoolean listenerIsCalled = new AtomicBoolean();
        child.addAttachListener(() -> {
            assertFalse(listenerIsCalled.get());
            listenerIsCalled.set(true);
        });

        nodeMap.put("foo", child);

        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(child.getParent());

        assertTrue(listenerIsCalled.get());

        // The attach listener is not called one more time
        nodeMap.put("foo", child);
    }

    @Test
    void put_replaceSingleValue_stillUseSingleValue() {
        nodeMap.put("foo", "bar");

        assertTrue(nodeMap.usesSingleMap());

        nodeMap.put("foo", "baz");

        assertTrue(nodeMap.usesSingleMap());
    }

    @Test
    void streamSingleNullValue() {
        nodeMap.put("foo", null);

        assertTrue(nodeMap.usesSingleMap());

        nodeMap.forEachChild(child -> {
            fail("Should not happen, but forEachChild shouldn't explode either");
        });
    }

    @Test
    void collectChanges_sameValue_alwaysCollect_allValueChangesCollected() {
        StateNode node = new StateNode(ElementPropertyMap.class);
        AlwaysProduceChangeMap map = new AlwaysProduceChangeMap(node);

        assertChangeCollected(map);
        // change the same property one more time: it still should be collected
        assertChangeCollected(map);
    }

    @Test
    void put_sameValue_alwaysProduceChange_nodeIsDirty() {
        UI ui = new UI();
        StateNode node = new StateNode(ElementPropertyMap.class);
        StateTree tree = ui.getInternals().getStateTree();
        tree.getRootNode().getFeature(ElementChildrenList.class).add(node);
        AlwaysProduceChangeMap map = new AlwaysProduceChangeMap(node);

        // clear dirty nodes
        tree.collectChanges(change -> {
        });

        map.put("foo", "bar");

        Set<StateNode> nodes = tree.collectDirtyNodes();
        assertTrue(nodes.contains(node));

        // clear dirty nodes
        tree.collectChanges(change -> {
        });

        assertTrue(tree.collectDirtyNodes().isEmpty());

        // set once again the same value
        map.put("foo", "bar");

        nodes = tree.collectDirtyNodes();
        assertTrue(nodes.contains(node));
    }

    @Test
    void put_sameValue_neverProduceChange_nodeIsNotDirty() {
        UI ui = new UI();
        StateNode node = new StateNode(ElementPropertyMap.class);
        StateTree tree = ui.getInternals().getStateTree();
        tree.getRootNode().getFeature(ElementChildrenList.class).add(node);
        NeverProduceChangeMap map = new NeverProduceChangeMap(node);

        // clear dirty nodes
        tree.collectChanges(change -> {
        });

        map.put("foo", "bar");

        Set<StateNode> nodes = tree.collectDirtyNodes();
        assertFalse(nodes.contains(node));

        // clear dirty nodes
        tree.collectChanges(change -> {
        });

        assertTrue(tree.collectDirtyNodes().isEmpty());

        // set another value
        map.put("foo", "baz");

        nodes = tree.collectDirtyNodes();
        assertFalse(nodes.contains(node));
    }

    @Test
    void collectChanges_sameValue_neverCollect_noValueChanges() {
        StateNode node = new StateNode(ElementPropertyMap.class);
        NeverProduceChangeMap map = new NeverProduceChangeMap(node);

        assertChangeIsNotCollected(map, "bar");
        // change the same property one more time to another value: it still
        // should not be collected
        assertChangeIsNotCollected(map, "baz");
    }

    private void assertChangeIsNotCollected(NeverProduceChangeMap map,
            String value) {
        map.put("foo", value);

        List<NodeChange> changes = new ArrayList<>();
        map.collectChanges(changes::add);

        assertTrue(changes.isEmpty());
    }

    private void assertChangeCollected(AlwaysProduceChangeMap map) {
        map.put("foo", "bar");

        List<NodeChange> changes = new ArrayList<>();
        map.collectChanges(changes::add);

        assertEquals(1, changes.size());
        assertEquals(MapPutChange.class, changes.get(0).getClass());
    }
}
