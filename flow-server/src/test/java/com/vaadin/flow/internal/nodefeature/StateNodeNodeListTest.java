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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.ListRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StateNodeNodeListTest
        extends AbstractNodeFeatureTest<ElementChildrenList> {
    private NodeList<StateNode> nodeList = createFeature();

    @Test
    public void testAddingAndRemoving() {
        StateNode value1 = StateNodeTest.createEmptyNode("value1");
        StateNode value2 = StateNodeTest.createEmptyNode("value2");

        nodeList.add(value1);

        Assertions.assertEquals(1, nodeList.size());
        Assertions.assertSame(value1, nodeList.get(0));

        List<NodeChange> firstAddChanges = collectChanges(nodeList);
        Assertions.assertEquals(1, firstAddChanges.size());
        ListAddChange<?> firstAddChange = (ListAddChange<?>) firstAddChanges
                .get(0);
        Assertions.assertEquals(0, firstAddChange.getIndex());
        Assertions.assertEquals(Arrays.asList(value1),
                firstAddChange.getNewItems());

        nodeList.add(0, value2);
        Assertions.assertEquals(2, nodeList.size());
        Assertions.assertSame(value2, nodeList.get(0));
        Assertions.assertSame(value1, nodeList.get(1));

        List<NodeChange> secondAddChanges = collectChanges(nodeList);
        Assertions.assertEquals(1, secondAddChanges.size());
        ListAddChange<?> secondAddChange = (ListAddChange<?>) secondAddChanges
                .get(0);
        Assertions.assertEquals(0, secondAddChange.getIndex());
        Assertions.assertEquals(Arrays.asList(value2),
                secondAddChange.getNewItems());

        StateNode removedItem = nodeList.remove(0);

        Assertions.assertEquals(1, nodeList.size());
        Assertions.assertSame(value1, nodeList.get(0));
        Assertions.assertSame(value2, removedItem);

        List<NodeChange> removeChanges = collectChanges(nodeList);
        Assertions.assertEquals(1, removeChanges.size());
        ListRemoveChange<?> removeChange = (ListRemoveChange<?>) removeChanges
                .get(0);
        Assertions.assertEquals(0, removeChange.getIndex());
    }

    @Test
    public void testChangesAfterReset() {
        StateNode value1 = StateNodeTest.createEmptyNode("value1");
        StateNode value2 = StateNodeTest.createEmptyNode("value2");

        nodeList.add(value1);
        nodeList.add(value2);

        nodeList.getNode().clearChanges();

        nodeList.generateChangesFromEmpty();

        List<NodeChange> changes = collectChanges(nodeList);

        Assertions.assertEquals(1, changes.size());
        ListAddChange<?> change = (ListAddChange<?>) changes.get(0);
        Assertions.assertEquals(0, change.getIndex());
        Assertions.assertEquals(Arrays.asList(value1, value2),
                change.getNewItems());
    }

    @Test
    public void testAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        Assertions.assertNull(child.getParent());

        nodeList.add(child);

        Assertions.assertSame(nodeList.getNode(), child.getParent());

        nodeList.remove(0);

        Assertions.assertNull(child.getParent());
    }

    @Test
    public void testIndexOf() {
        StateNode one = StateNodeTest.createEmptyNode("one");
        StateNode two = StateNodeTest.createEmptyNode("two");
        StateNode three = StateNodeTest.createEmptyNode("three");

        nodeList.add(one);
        nodeList.add(two);
        Assertions.assertEquals(0, nodeList.indexOf(one));
        Assertions.assertEquals(1, nodeList.indexOf(two));
        Assertions.assertEquals(-1, nodeList.indexOf(three));
    }

    @Test
    public void testClear() {
        StateNode one = StateNodeTest.createEmptyNode("one");
        StateNode two = StateNodeTest.createEmptyNode("two");

        nodeList.add(one);
        nodeList.add(two);
        Assertions.assertEquals(2, nodeList.size());
        nodeList.clear();
        Assertions.assertEquals(0, nodeList.size());
    }

    @Test
    public void nullNotAllowed() {
        assertThrows(AssertionError.class, () -> {
            nodeList.add(null);
        });
    }

    @Test
    public void testSerializable() {
        StateNode one = StateNodeTest.createTestNode("one",
                ElementClassList.class);
        one.getFeature(ElementClassList.class).add("foo");
        one.getFeature(ElementClassList.class).add("bar");
        StateNode two = StateNodeTest.createTestNode("two",
                ElementClassList.class);
        two.getFeature(ElementClassList.class).add("baz");
        nodeList.add(one);
        nodeList.add(two);

        List<StateNode> values = new ArrayList<>();
        int size = nodeList.size();
        for (int i = 0; i < size; i++) {
            values.add(nodeList.get(i));
        }

        NodeList<StateNode> copy = SerializationUtils
                .deserialize(SerializationUtils.serialize(nodeList));

        Assertions.assertNotSame(nodeList, copy);

        Assertions.assertEquals(values.size(), copy.size());
        for (int i = 0; i < size; i++) {
            assertNodeEquals(values.get(i), copy.get(i));
        }
        // Also verify that original value wasn't changed by the serialization
        Assertions.assertEquals(values.size(), nodeList.size());
        for (int i = 0; i < size; i++) {
            assertNodeEquals(values.get(i), nodeList.get(i));
        }

    }
}
