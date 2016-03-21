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

package com.vaadin.hummingbird;

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

import com.vaadin.hummingbird.change.ChangeVisitor;
import com.vaadin.hummingbird.change.JsonNodeChange;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeDetachChange;
import com.vaadin.hummingbird.change.StreamResourceChange;
import com.vaadin.hummingbird.namespace.ElementAttributeNamespace;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;
import com.vaadin.hummingbird.namespace.Namespace;
import com.vaadin.tests.util.TestUtil;

public class StateTreeTest {
    private StateTree tree = new StateTree(ElementChildrenNamespace.class);

    private static class TestChangeVisitor implements ChangeVisitor {

        private final List<JsonNodeChange> jsonChanges = new ArrayList<>();
        private final List<StreamResourceChange> resourceChanges = new ArrayList<>();

        @Override
        public void visit(JsonNodeChange change) {
            if (change instanceof ListSpliceChange
                    && ((ListSpliceChange) change)
                            .getNamespace() == ElementChildrenNamespace.class) {
                return;
            } else {
                jsonChanges.add(change);
            }
        }

        @Override
        public void visit(StreamResourceChange change) {
            resourceChanges.add(change);
        }

        List<JsonNodeChange> getJsonChanges() {
            return jsonChanges;
        }

        List<StreamResourceChange> getAddedResources() {
            return resourceChanges;
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

        StateTree anotherTree = new StateTree(ElementChildrenNamespace.class);

        StateNodeTest.setParent(node, anotherTree.getRootNode());
    }

    @Test
    public void testNoRootAttachChange() {
        List<JsonNodeChange> changes = collectChangesExceptChildrenAdd();

        Assert.assertEquals(Collections.emptyList(), changes);
    }

    @Test
    public void testTreeChangeCollection() {
        StateNode node2 = StateNodeTest.createEmptyNode();
        StateNodeTest.setParent(node2, tree.getRootNode());

        List<JsonNodeChange> changes = collectChangesExceptChildrenAdd();

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
    public void testDetachInChanges() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode();
        StateNodeTest.setParent(node2, node1);

        // Clear
        collectChangesExceptChildrenAdd();

        StateNodeTest.setParent(node2, null);

        List<JsonNodeChange> changes = collectChangesExceptChildrenAdd();

        Assert.assertEquals("Should be one change.", 1, changes.size());
        Assert.assertTrue("Should have a detach change",
                changes.get(0) instanceof NodeDetachChange);
    }

    @Test
    public void allValuesAfterReattach() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = new StateNode(ElementDataNamespace.class);

        StateNodeTest.setParent(node2, node1);
        node2.getNamespace(ElementDataNamespace.class).setTag("foo");
        collectChangesExceptChildrenAdd();

        StateNodeTest.setParent(node2, null);
        collectChangesExceptChildrenAdd();

        StateNodeTest.setParent(node2, node1);
        List<JsonNodeChange> changes = collectChangesExceptChildrenAdd();

        Assert.assertEquals("Should be two changes.", 2, changes.size());
        Assert.assertTrue("First change should re-attach the node.",
                changes.get(0) instanceof NodeAttachChange);
        Assert.assertTrue("Second change should re-put the value.",
                changes.get(1) instanceof MapPutChange);

        MapPutChange nodeChange = (MapPutChange) changes.get(1);
        Assert.assertEquals(ElementDataNamespace.class,
                nodeChange.getNamespace());
        Assert.assertEquals("tag", nodeChange.getKey());
        Assert.assertEquals("foo", nodeChange.getValue());
    }

    private List<JsonNodeChange> collectChangesExceptChildrenAdd() {
        TestChangeVisitor visitor = new TestChangeVisitor();
        tree.accept(visitor);
        return visitor.getJsonChanges();
    }

    @Test
    public void testSerializable() {
        @SuppressWarnings("unchecked")
        Class<? extends Namespace>[] namespaces = new Class[] {
                ElementChildrenNamespace.class, ElementDataNamespace.class,
                ElementAttributeNamespace.class,
                ElementPropertyNamespace.class };
        StateTree tree = new StateTree(namespaces);

        StateNode root = tree.getRootNode();
        root.getNamespace(ElementDataNamespace.class).setTag("body");
        StateNode child = new StateNode(namespaces);
        root.getNamespace(ElementChildrenNamespace.class).add(0, child);
        child.getNamespace(ElementDataNamespace.class).setTag("div");

        byte[] serialized = SerializationUtils.serialize(tree);
        StateTree d1 = (StateTree) SerializationUtils.deserialize(serialized);

        Assert.assertNotNull(d1);
    }

    @Test
    public void reattachedNodeRetainsId() throws InterruptedException {
        StateNode child = new StateNode(ElementChildrenNamespace.class);
        StateNode grandChild = new StateNode(ElementChildrenNamespace.class);

        child.getNamespace(ElementChildrenNamespace.class).add(0, grandChild);

        ElementChildrenNamespace children = tree.getRootNode()
                .getNamespace(ElementChildrenNamespace.class);
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
        StateNode child = new StateNode(ElementChildrenNamespace.class);
        StateNode grandChild = new StateNode(ElementChildrenNamespace.class);

        child.getNamespace(ElementChildrenNamespace.class).add(0, grandChild);

        ElementChildrenNamespace children = tree.getRootNode()
                .getNamespace(ElementChildrenNamespace.class);
        children.add(0, child);

        WeakReference<StateNode> childRef = new WeakReference<>(child);
        child = null;

        WeakReference<StateNode> grandChildRef = new WeakReference<>(
                grandChild);
        grandChild = null;

        children.remove(0);

        tree.accept(new NoOpChangeVisitor());

        Assert.assertTrue(TestUtil.isGarbageCollected(childRef));
        Assert.assertTrue(TestUtil.isGarbageCollected(grandChildRef));
    }

}
