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

package com.vaadin.flow.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.ListRemoveChange;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeAttachChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.change.NodeDetachChange;
import com.vaadin.flow.internal.change.NodeFeatureChange;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.tests.util.TestUtil;

public class StateTreeTest {
    private StateTree tree = new StateTree(new UI(), ElementChildrenList.class);

    public static class AttachableNode extends StateNode {

        private boolean attached;

        public AttachableNode() {
        }

        public AttachableNode(boolean attached) {
            this.attached = attached;
        }

        public void setAttached(boolean attached) {
            this.attached = attached;
        }

        @Override
        public boolean isAttached() {
            return attached;
        }
    }

    @Test
    public void testRootNodeState() {
        StateNode rootNode = tree.getRootNode();

        Assert.assertNull("Root node should have no parent",
                rootNode.getParent());

        Assert.assertTrue("Root node should always be attached",
                rootNode.isAttached());

        Assert.assertEquals("Root node should always have the same id", 1,
                rootNode.getId());

        Assert.assertSame(tree, rootNode.getOwner());
    }

    @Test(expected = IllegalStateException.class)
    public void testRootNode_setParent_throws() {
        tree.getRootNode().setParent(new StateNode());
    }

    @Test
    public void attachedNodeIsAttached() {
        StateNode node = StateNodeTest.createEmptyNode();

        Assert.assertFalse("New node should not be attached",
                node.isAttached());

        StateNodeTest.setParent(node, tree.getRootNode());

        Assert.assertTrue("Node with parent set should be attached",
                node.isAttached());

        StateNodeTest.setParent(node, null);

        Assert.assertFalse("Node without parent should not be attached",
                node.isAttached());
    }

    @Test(expected = IllegalStateException.class)
    public void moveNodeToOtherRoot_throws() {
        StateNode node = StateNodeTest.createEmptyNode();

        StateNodeTest.setParent(node, tree.getRootNode());

        StateNodeTest.setParent(node, null);

        StateTree anotherTree = new StateTree(new UI(),
                ElementChildrenList.class);

        StateNodeTest.setParent(node, anotherTree.getRootNode());
    }

    @Test
    public void testNoRootAttachChange() {
        List<NodeChange> changes = collectChangesExceptChildrenAddRemove();

        Assert.assertEquals(Collections.emptyList(), changes);
    }

    @Test
    public void testTreeChangeCollection() {
        StateNode node2 = StateNodeTest.createEmptyNode();
        StateNodeTest.setParent(node2, tree.getRootNode());

        List<NodeChange> changes = collectChangesExceptChildrenAddRemove();

        Assert.assertEquals(1, changes.size());
        NodeAttachChange nodeChange = (NodeAttachChange) changes.get(0);
        Assert.assertSame(node2, nodeChange.getNode());
    }

    @Test
    public void testDirtyNodeCollection() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode("node2");

        StateNodeTest.setParent(node2, node1);

        NodeOwner owner = node1.getOwner();

        Assert.assertSame("Both nodes should have the same owner", owner,
                node2.getOwner());

        Set<StateNode> initialDirty = tree.collectDirtyNodes();
        Assert.assertEquals("Both nodes should initially be empty",
                new HashSet<>(Arrays.asList(node1, node2)), initialDirty);

        Set<StateNode> emptyCollection = tree.collectDirtyNodes();
        Assert.assertTrue("Dirty nodes should be empty after collection",
                emptyCollection.isEmpty());

        node2.markAsDirty();

        Set<StateNode> collectAfterOneMarked = tree.collectDirtyNodes();
        Assert.assertEquals("Marked node should be in collect result",
                Collections.singleton(node2), collectAfterOneMarked);
    }

    @Test
    public void testDirtyNodeCollectionOrder() {
        StateNode rootNode = tree.getRootNode();
        List<StateNode> nodes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            StateNode node = StateNodeTest.createEmptyNode("node" + i);
            nodes.add(node);
            StateNodeTest.setParent(node, rootNode);
        }

        nodes.forEach(StateNode::markAsDirty);
        List<StateNode> expected = new ArrayList<>();
        expected.add(rootNode);
        expected.addAll(nodes);

        Assert.assertArrayEquals(expected.toArray(),
                tree.collectDirtyNodes().toArray());

        nodes.forEach(StateNode::markAsDirty);
        expected = new ArrayList<>(nodes);
        Assert.assertArrayEquals(expected.toArray(),
                tree.collectDirtyNodes().toArray());
    }

    @Test
    public void testDetachInChanges() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode();
        StateNodeTest.setParent(node2, node1);

        // Clear
        collectChangesExceptChildrenAddRemove();

        StateNodeTest.setParent(node2, null);

        List<NodeChange> changes = collectChangesExceptChildrenAddRemove();

        Assert.assertEquals("Should be one change.", 1, changes.size());
        Assert.assertTrue("Should have a detach change",
                changes.get(0) instanceof NodeDetachChange);
    }

    @Test
    public void allValuesAfterReattach() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = new StateNode(ElementData.class);

        StateNodeTest.setParent(node2, node1);
        node2.getFeature(ElementData.class).setTag("foo");
        collectChangesExceptChildrenAddRemove();

        StateNodeTest.setParent(node2, null);
        collectChangesExceptChildrenAddRemove();

        StateNodeTest.setParent(node2, node1);
        List<NodeChange> changes = collectChangesExceptChildrenAddRemove();

        Assert.assertEquals("Should be two changes.", 2, changes.size());
        Assert.assertTrue("First change should re-attach the node.",
                changes.get(0) instanceof NodeAttachChange);
        Assert.assertTrue("Second change should re-put the value.",
                changes.get(1) instanceof MapPutChange);

        MapPutChange nodeChange = (MapPutChange) changes.get(1);
        Assert.assertEquals(ElementData.class, nodeChange.getFeature());
        Assert.assertEquals("tag", nodeChange.getKey());
        Assert.assertEquals("foo", nodeChange.getValue());
    }

    private List<NodeChange> collectChangesExceptChildrenAddRemove() {
        List<NodeChange> changes = new ArrayList<>();
        tree.collectChanges(change -> {
            if ((change instanceof ListAddChange
                    || change instanceof ListRemoveChange)
                    && ((NodeFeatureChange) change)
                            .getFeature() == ElementChildrenList.class) {
                return;
            } else {
                changes.add(change);
            }
        });
        return changes;
    }

    @Test
    public void testSerializable() {
        @SuppressWarnings("unchecked")
        Class<? extends NodeFeature>[] features = new Class[] {
                ElementChildrenList.class, ElementData.class,
                ElementAttributeMap.class, ElementPropertyMap.class };
        StateTree tree = new StateTree(new UI(), features);

        StateNode root = tree.getRootNode();
        root.getFeature(ElementData.class).setTag("body");
        StateNode child = new StateNode(features);
        root.getFeature(ElementChildrenList.class).add(0, child);
        child.getFeature(ElementData.class).setTag(Tag.DIV);

        byte[] serialized = SerializationUtils.serialize(tree);
        StateTree d1 = (StateTree) SerializationUtils.deserialize(serialized);

        Assert.assertNotNull(d1);
    }

    @Test
    public void reattachedNodeRetainsId() throws InterruptedException {
        StateNode child = new StateNode(ElementChildrenList.class);
        StateNode grandChild = new StateNode(ElementChildrenList.class);

        child.getFeature(ElementChildrenList.class).add(0, grandChild);

        ElementChildrenList children = tree.getRootNode()
                .getFeature(ElementChildrenList.class);
        children.add(0, child);

        int childId = child.getId();
        int grandChildId = grandChild.getId();

        Assert.assertTrue(child.isAttached());
        Assert.assertTrue(grandChild.isAttached());

        Assert.assertSame(child, tree.getNodeById(childId));
        Assert.assertSame(grandChild, tree.getNodeById(grandChildId));

        children.remove(0);

        Assert.assertFalse(child.isAttached());
        Assert.assertFalse(grandChild.isAttached());

        Assert.assertNull(tree.getNodeById(childId));
        Assert.assertNull(tree.getNodeById(grandChildId));

        children.add(0, child);

        Assert.assertTrue(child.isAttached());
        Assert.assertTrue(grandChild.isAttached());

        Assert.assertEquals(childId, child.getId());
        Assert.assertEquals(grandChildId, grandChild.getId());

        Assert.assertSame(child, tree.getNodeById(childId));
        Assert.assertSame(grandChild, tree.getNodeById(grandChildId));
    }

    @Test
    public void detachedNodeGarbageCollected() throws InterruptedException {
        StateNode child = new StateNode(ElementChildrenList.class);
        StateNode grandChild = new StateNode(ElementChildrenList.class);

        child.getFeature(ElementChildrenList.class).add(0, grandChild);

        ElementChildrenList children = tree.getRootNode()
                .getFeature(ElementChildrenList.class);
        children.add(0, child);

        WeakReference<StateNode> childRef = new WeakReference<>(child);
        child = null;

        WeakReference<StateNode> grandChildRef = new WeakReference<>(
                grandChild);
        grandChild = null;

        children.remove(0);

        tree.collectChanges(c -> {
            // nop
        });

        Assert.assertTrue(TestUtil.isGarbageCollected(childRef));
        Assert.assertTrue(TestUtil.isGarbageCollected(grandChildRef));
    }

    @Test
    public void beforeClientResponse_regularOrder() {
        StateNode rootNode = new AttachableNode(true);

        List<Integer> results = new ArrayList<>();

        tree.beforeClientResponse(rootNode, () -> results.add(0));
        tree.beforeClientResponse(rootNode, () -> results.add(1));
        tree.beforeClientResponse(rootNode, () -> results.add(2));

        tree.runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 3 results in the list",
                results.size() == 3);

        for (int i = 0; i < results.size(); i++) {
            Assert.assertEquals(
                    "The result at index '" + i + "' should be " + i, i,
                    results.get(i).intValue());
        }
    }

    @Test
    public void beforeClientResponse_withInnerRunnables() {
        StateNode rootNode = new AttachableNode(true);

        List<Integer> results = new ArrayList<>();

        tree.beforeClientResponse(rootNode, () -> results.add(0));
        tree.beforeClientResponse(rootNode, () -> {
            results.add(1);
            tree.beforeClientResponse(rootNode, () -> results.add(3));
            tree.beforeClientResponse(rootNode, () -> results.add(4));
        });
        tree.beforeClientResponse(rootNode, () -> results.add(2));

        tree.runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 5 results in the list",
                results.size() == 5);

        for (int i = 0; i < results.size(); i++) {
            Assert.assertEquals(
                    "The result at index '" + i + "' should be " + i, i,
                    results.get(i).intValue());
        }
    }

    @Test
    public void beforeClientResponse_withUnattachedNodes() {
        StateNode rootNode = new AttachableNode(true);
        StateNode emptyNode = new AttachableNode();

        List<Integer> results = new ArrayList<>();

        tree.beforeClientResponse(emptyNode, () -> results.add(0));
        tree.beforeClientResponse(rootNode, () -> results.add(1));
        tree.beforeClientResponse(emptyNode, () -> results.add(2));
        tree.beforeClientResponse(rootNode, () -> results.add(3));

        tree.runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 2 results in the list",
                results.size() == 2);

        Assert.assertEquals("The result at index '0' should be " + 1, 1,
                results.get(0).intValue());
        Assert.assertEquals("The result at index '1' should be " + 3, 3,
                results.get(1).intValue());
    }

    @Test
    public void beforeClientResponse_withAttachedNodesDuringExecution() {
        StateNode rootNode = tree.getRootNode();
        StateNode emptyNode1 = StateNodeTest.createEmptyNode("node1");
        StateNode emptyNode2 = StateNodeTest.createEmptyNode("node2");

        List<Integer> results = new ArrayList<>();

        tree.beforeClientResponse(emptyNode1, () -> {
            results.add(0);
            StateNodeTest.setParent(emptyNode2, rootNode);
        });
        tree.beforeClientResponse(rootNode, () -> {
            results.add(1);
            StateNodeTest.setParent(emptyNode1, rootNode);
        });
        tree.beforeClientResponse(emptyNode2, () -> results.add(2));
        tree.beforeClientResponse(rootNode, () -> results.add(3));

        tree.runExecutionsBeforeClientResponse();
        Assert.assertTrue("There should be 4 results in the list",
                results.size() == 4);

        Assert.assertEquals("The result at index '0' should be 1", 1,
                results.get(0).intValue());
        Assert.assertEquals("The result at index '1' should be 3", 3,
                results.get(1).intValue());
        Assert.assertEquals("The result at index '2' should be 0", 0,
                results.get(2).intValue());
        Assert.assertEquals("The result at index '3' should be 2", 2,
                results.get(3).intValue());
    }

    @Test
    public void beforeClientResponse_nodeGarbageCollectedDespiteClosure()
            throws InterruptedException {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode("node2");

        StateNodeTest.setParent(node2, node1);

        class CapturingRunnable implements Runnable {
            private final Object captured;

            public CapturingRunnable(Object captured) {
                this.captured = captured;
            }

            @Override
            public void run() {
                // nop
            }
        }

        tree.beforeClientResponse(node2, new CapturingRunnable(node2));

        StateNodeTest.setParent(node2, null);

        WeakReference<StateNode> ref = new WeakReference<>(node2);
        node2 = null;

        // Collect to release from list of detached nodes
        tree.collectChanges(value -> {
            // nop
        });

        Assert.assertTrue(TestUtil.isGarbageCollected(ref));
    }

    @Test
    public void collectChanges_updateActiveState() {
        StateNode node1 = Mockito.mock(StateNode.class);
        StateNode node2 = Mockito.mock(StateNode.class);

        Mockito.when(node1.getOwner()).thenReturn(tree);
        Mockito.when(node2.getOwner()).thenReturn(tree);

        tree.markAsDirty(node1);
        tree.markAsDirty(node2);

        tree.collectChanges(node -> {
        });

        Mockito.verify(node1).updateActiveState();
        Mockito.verify(node2).updateActiveState();
    }

}
