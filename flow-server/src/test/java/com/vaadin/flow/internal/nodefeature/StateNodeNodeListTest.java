/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateNodeTest;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.ListRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;

public class StateNodeNodeListTest
        extends AbstractNodeFeatureTest<ElementChildrenList> {
    private NodeList<StateNode> nodeList = createFeature();

    @Test
    public void testAddingAndRemoving() {
        StateNode value1 = StateNodeTest.createEmptyNode("value1");
        StateNode value2 = StateNodeTest.createEmptyNode("value2");

        nodeList.add(value1);

        Assert.assertEquals(1, nodeList.size());
        Assert.assertSame(value1, nodeList.get(0));

        List<NodeChange> firstAddChanges = collectChanges(nodeList);
        Assert.assertEquals(1, firstAddChanges.size());
        ListAddChange<?> firstAddChange = (ListAddChange<?>) firstAddChanges
                .get(0);
        Assert.assertEquals(0, firstAddChange.getIndex());
        Assert.assertEquals(Arrays.asList(value1),
                firstAddChange.getNewItems());

        nodeList.add(0, value2);
        Assert.assertEquals(2, nodeList.size());
        Assert.assertSame(value2, nodeList.get(0));
        Assert.assertSame(value1, nodeList.get(1));

        List<NodeChange> secondAddChanges = collectChanges(nodeList);
        Assert.assertEquals(1, secondAddChanges.size());
        ListAddChange<?> secondAddChange = (ListAddChange<?>) secondAddChanges
                .get(0);
        Assert.assertEquals(0, secondAddChange.getIndex());
        Assert.assertEquals(Arrays.asList(value2),
                secondAddChange.getNewItems());

        StateNode removedItem = nodeList.remove(0);

        Assert.assertEquals(1, nodeList.size());
        Assert.assertSame(value1, nodeList.get(0));
        Assert.assertSame(value2, removedItem);

        List<NodeChange> removeChanges = collectChanges(nodeList);
        Assert.assertEquals(1, removeChanges.size());
        ListRemoveChange<?> removeChange = (ListRemoveChange<?>) removeChanges
                .get(0);
        Assert.assertEquals(0, removeChange.getIndex());
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

        Assert.assertEquals(1, changes.size());
        ListAddChange<?> change = (ListAddChange<?>) changes.get(0);
        Assert.assertEquals(0, change.getIndex());
        Assert.assertEquals(Arrays.asList(value1, value2),
                change.getNewItems());
    }

    @Test
    public void testAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        Assert.assertNull(child.getParent());

        nodeList.add(child);

        Assert.assertSame(nodeList.getNode(), child.getParent());

        nodeList.remove(0);

        Assert.assertNull(child.getParent());
    }

    @Test
    public void testIndexOf() {
        StateNode one = StateNodeTest.createEmptyNode("one");
        StateNode two = StateNodeTest.createEmptyNode("two");
        StateNode three = StateNodeTest.createEmptyNode("three");

        nodeList.add(one);
        nodeList.add(two);
        Assert.assertEquals(0, nodeList.indexOf(one));
        Assert.assertEquals(1, nodeList.indexOf(two));
        Assert.assertEquals(-1, nodeList.indexOf(three));
    }

    @Test
    public void testClear() {
        StateNode one = StateNodeTest.createEmptyNode("one");
        StateNode two = StateNodeTest.createEmptyNode("two");

        nodeList.add(one);
        nodeList.add(two);
        Assert.assertEquals(2, nodeList.size());
        nodeList.clear();
        Assert.assertEquals(0, nodeList.size());
    }

    @Test(expected = AssertionError.class)
    public void nullNotAllowed() {
        nodeList.add(null);
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

        Assert.assertNotSame(nodeList, copy);

        Assert.assertEquals(values.size(), copy.size());
        for (int i = 0; i < size; i++) {
            assertNodeEquals(values.get(i), copy.get(i));
        }
        // Also verify that original value wasn't changed by the serialization
        Assert.assertEquals(values.size(), nodeList.size());
        for (int i = 0; i < size; i++) {
            assertNodeEquals(values.get(i), nodeList.get(i));
        }

    }
}
