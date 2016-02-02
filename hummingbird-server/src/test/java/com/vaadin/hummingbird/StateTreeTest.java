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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.MapPutChange;
import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.change.NodeDetachChange;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;

public class StateTreeTest {
    private StateTree tree = new StateTree();

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
    public void testStateTreeAdoptNode() {
        StateNode node = StateNodeTest.createEmptyNode();

        NodeOwner originalOwner = node.getOwner();
        tree.adoptNodes(originalOwner);

        Assert.assertFalse("Original owner should no longer konw the node",
                originalOwner.getNodes().contains(node));
        Assert.assertTrue("Tree should know the node",
                tree.getNodes().contains(node));

        Assert.assertSame("Node owner should be the tree", tree,
                node.getOwner());

        Assert.assertEquals("Node should have an id", 2, node.getId());

        StateNode byId = tree.getNodeById(node.getId());

        Assert.assertSame("Node should be findable by id", node, byId);
    }

    @Test
    public void attachedNodeIsAttached() {
        StateNode node = StateNodeTest.createEmptyNode();

        Assert.assertFalse("New node should not be attached",
                node.isAttached());

        node.setParent(tree.getRootNode());

        Assert.assertTrue("Node with parent set should be attached",
                node.isAttached());

        node.setParent(null);

        Assert.assertFalse("Node without parent should not be attached",
                node.isAttached());
    }

    @Test(expected = IllegalStateException.class)
    public void moveNodeToOtherRoot_throws() {
        StateNode node = StateNodeTest.createEmptyNode();

        node.setParent(tree.getRootNode());

        node.setParent(null);

        StateTree anotherTree = new StateTree();

        node.setParent(anotherTree.getRootNode());
    }

    @Test
    public void testNoRootAttachChange() {
        List<NodeChange> changes = collectChanges();

        Assert.assertEquals(Collections.emptyList(), changes);
    }

    @Test
    public void testTreeChangeCollection() {
        StateNode node2 = StateNodeTest.createEmptyNode();
        node2.setParent(tree.getRootNode());

        List<NodeChange> changes = collectChanges();

        Assert.assertEquals(1, changes.size());
        NodeAttachChange nodeChange = (NodeAttachChange) changes.get(0);
        Assert.assertSame(node2, nodeChange.getNode());
    }

    @Test
    public void testDirtyNodeCollection() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode("node2");

        node2.setParent(node1);

        NodeOwner owner = node1.getOwner();

        Assert.assertSame("Both nodes should have the same owner", owner,
                node2.getOwner());

        Set<StateNode> initialDirty = owner.collectDirtyNodes();
        Assert.assertEquals("Both nodes should initially be empty",
                new HashSet<>(Arrays.asList(node1, node2)), initialDirty);

        Set<StateNode> emptyCollection = owner.collectDirtyNodes();
        Assert.assertTrue("Dirty nodes should be empty after collection",
                emptyCollection.isEmpty());

        node2.markAsDirty();

        Set<StateNode> collectAfterOneMarked = owner.collectDirtyNodes();
        Assert.assertEquals("Marked node should be in collect result",
                Collections.singleton(node2), collectAfterOneMarked);
    }

    @Test
    public void testDetachInChanges() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode();
        node2.setParent(node1);

        // Clear
        collectChanges();

        node2.setParent(null);

        List<NodeChange> changes = collectChanges();

        Assert.assertEquals("Should be one change.", 1, changes.size());
        Assert.assertTrue("Should have a detach change",
                changes.get(0) instanceof NodeDetachChange);
    }

    @Test
    public void allValuesAfterReattach() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = new StateNode(ElementDataNamespace.class);

        node2.setParent(node1);
        node2.getNamespace(ElementDataNamespace.class).setTag("foo");
        collectChanges();

        node2.setParent(null);
        collectChanges();

        node2.setParent(node1);
        List<NodeChange> changes = collectChanges();

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

    private List<NodeChange> collectChanges() {
        ArrayList<NodeChange> changes = new ArrayList<>();
        tree.collectChanges(changes::add);
        return changes;
    }
}
