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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.change.NodeDetachChange;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;
import com.vaadin.hummingbird.namespace.Namespace;

public class StateNodeTest {

    private static class TestStateNode extends StateNode {
        private int i = -1;

        public TestStateNode() {
            super(ElementChildrenNamespace.class);
        }

        public void setData(int data) {
            i = data;
        }

        public int getData() {
            return i;
        }

        @Override
        public String toString() {
            return Integer.toString(getData());
        }
    }

    private static class RootStateNode extends TestStateNode {

        @Override
        public boolean isAttached() {
            return true;
        }
    }

    @Test
    public void newNodeState() {
        StateNode node = createEmptyNode();

        NodeOwner owner = node.getOwner();

        Assert.assertNotNull("New node should have an owner", owner);

        Assert.assertEquals("New node shold have unassigned id", -1,
                node.getId());

        Assert.assertFalse("Node should not be attached", node.isAttached());
    }

    @Test
    public void nodeContainsDefinedNamespaces() {
        StateNode node = new StateNode(ElementDataNamespace.class);

        Assert.assertTrue("Should have namespace defined in constructor",
                node.hasNamespace(ElementDataNamespace.class));

        ElementDataNamespace namespace = node
                .getNamespace(ElementDataNamespace.class);

        Assert.assertNotNull("Existing namespace should also be available",
                namespace);

        Assert.assertFalse(
                "Should not have namespace that wasn't defined in constructor",
                node.hasNamespace(ElementPropertyNamespace.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getMissingNamespaceThrows() {
        StateNode node = new StateNode(ElementDataNamespace.class);
        node.getNamespace(ElementPropertyNamespace.class);
    }

    @Test
    public void testAttachDetachChangeCollection() {
        StateNode node = createEmptyNode();

        List<NodeChange> changes = new ArrayList<>();
        Consumer<NodeChange> collector = changes::add;

        node.collectChanges(collector);

        Assert.assertTrue("Node should have no changes", changes.isEmpty());

        // Attach node
        setParent(node,
                new StateTree(ElementChildrenNamespace.class).getRootNode());

        node.collectChanges(collector);

        Assert.assertEquals("Should have 1 change", 1, changes.size());
        Assert.assertTrue("Should have attach change",
                changes.get(0) instanceof NodeAttachChange);
        changes.clear();

        node.collectChanges(collector);
        Assert.assertTrue("Node should have no changes", changes.isEmpty());

        // Detach node
        setParent(node, null);

        node.collectChanges(collector);
        Assert.assertEquals("Should have 1 change", 1, changes.size());
        Assert.assertTrue("Should have detach change",
                changes.get(0) instanceof NodeDetachChange);
        changes.clear();
    }

    @Test
    public void appendChildBeforeParent() {
        StateNode parent = createParentNode("parent");
        StateNode child = createParentNode("child");
        StateNode grandchild = createEmptyNode("grandchild");

        StateNode root = new StateTree(ElementChildrenNamespace.class)
                .getRootNode();

        setParent(grandchild, child);
        setParent(child, parent);
        setParent(parent, root);

        Assert.assertNotEquals(-1, parent.getId());
        Assert.assertNotEquals(-1, child.getId());
        Assert.assertNotEquals(-1, grandchild.getId());
    }

    @Test
    public void appendParentBeforeChild() {
        StateNode parent = createParentNode("parent");
        StateNode child = createParentNode("child");
        StateNode grandchild = createEmptyNode("grandchild");

        StateNode root = new StateTree(ElementChildrenNamespace.class)
                .getRootNode();

        setParent(parent, root);
        setParent(child, parent);
        setParent(grandchild, child);

        Assert.assertNotEquals(-1, parent.getId());
        Assert.assertNotEquals(-1, child.getId());
        Assert.assertNotEquals(-1, grandchild.getId());
    }

    @Test(expected = IllegalStateException.class)
    public void setChildAsParent() {
        StateNode parent = createParentNode("parent");
        StateNode child = createParentNode("child");

        setParent(child, parent);
        setParent(parent, child);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetAsOwnParent() {
        StateNode parent = createParentNode("parent");

        setParent(parent, parent);
    }

    /**
     * Test for #252: stack overflow exception
     */
    @Test
    public void recursiveTreeNavigation_resilienceInDepth() {
        TestStateNode root = new TestStateNode();
        TestStateNode node = createTree(root, 20000);
        StateTree tree = new StateTree(ElementChildrenNamespace.class);
        root.setTree(tree);
        Set<Integer> set = IntStream.range(-1, node.getData() + 1).boxed()
                .collect(Collectors.toSet());
        root.visitNodeTree(n -> visit((TestStateNode) n, tree, set), false);
        Assert.assertTrue(set.isEmpty());
    }

    /**
     * Test for #252: stack overflow exception
     */
    @Test
    public void recursiveTreeNavigation_resilienceInSize() {
        TestStateNode root = new TestStateNode();
        int count = 3000;
        StateNode node = createTree(root, count);
        while (node.getParent() != null) {
            node = node.getParent();
            for (int i = 1; i < 50; i++) {
                TestStateNode child = new TestStateNode();
                node.getNamespace(ElementChildrenNamespace.class).add(i, child);
                child.setData(count);
                count++;
            }
        }
        root.setTree(new StateTree(ElementChildrenNamespace.class));
        Set<Integer> set = IntStream.range(-1, count).boxed()
                .collect(Collectors.toSet());
        root.visitNodeTree(
                n -> visit((TestStateNode) n, (StateTree) root.getOwner(), set),
                false);
        Assert.assertTrue(set.isEmpty());
    }

    @Test
    public void testAttachListener_onSetParent_listenerTriggered() {
        TestStateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        Assert.assertFalse(child.isAttached());
        AtomicBoolean triggered = new AtomicBoolean(false);

        child.addAttachListener(() -> triggered.set(true));

        setParent(child, root);

        Assert.assertTrue(triggered.get());
    }

    @Test
    public void testAttachListener_listenerRemoved_listenerNotTriggered() {
        TestStateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        Assert.assertFalse(child.isAttached());
        AtomicBoolean triggered = new AtomicBoolean(false);

        EventRegistrationHandle registrationHandle = child
                .addAttachListener(() -> triggered.set(true));
        registrationHandle.remove();

        setParent(child, root);

        Assert.assertFalse(triggered.get());
    }

    @Test
    public void testDetachListener_onSetParent_listenerTriggered() {
        TestStateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        setParent(child, root);
        Assert.assertTrue(child.isAttached());

        AtomicBoolean triggered = new AtomicBoolean(false);

        child.addDetachListener(() -> triggered.set(true));

        setParent(child, null);

        Assert.assertTrue(triggered.get());
    }

    @Test
    public void testDetachListener_listenerRemoved_listenerNotTriggered() {
        TestStateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        setParent(child, root);
        Assert.assertTrue(child.isAttached());

        AtomicBoolean triggered = new AtomicBoolean(false);

        EventRegistrationHandle registrationHandle = child
                .addDetachListener(() -> triggered.set(true));
        registrationHandle.remove();

        Assert.assertFalse(triggered.get());
    }

    public static StateNode createEmptyNode() {
        return createEmptyNode("Empty node");
    }

    public static StateNode createEmptyNode(String toString) {
        return createTestNode(toString);
    }

    public static StateNode createParentNode(String toString) {
        return createTestNode(toString, ElementChildrenNamespace.class);
    }

    @SafeVarargs
    public static StateNode createTestNode(String toString,
            Class<? extends Namespace>... namespaces) {
        return new StateNode(namespaces) {
            @Override
            public String toString() {
                if (toString != null) {
                    return toString;
                } else {
                    return super.toString();
                }
            }
        };
    }

    public static void setParent(StateNode child, StateNode parent) {
        if (parent == null) {
            // Remove child
            parent = child.getParent();

            ElementChildrenNamespace children = parent
                    .getNamespace(ElementChildrenNamespace.class);
            children.remove(children.indexOf(child));
        } else {
            // Add child
            assert child.getParent() == null;

            ElementChildrenNamespace children = parent
                    .getNamespace(ElementChildrenNamespace.class);
            children.add(children.size(), child);
        }
    }

    /**
     * Creates a tree with given {@code root} and {@code depth}. The resulting
     * tree is just a chain.
     * <p>
     * Returns the leaf (last) node.
     */
    private TestStateNode createTree(TestStateNode root, int depth) {
        TestStateNode node = root;
        for (int i = 0; i < depth; i++) {
            TestStateNode child = new TestStateNode();
            child.setData(i);
            node.getNamespace(ElementChildrenNamespace.class).add(0, child);
            node = child;
        }
        return node;
    }

    private void visit(TestStateNode node, StateTree tree, Set<Integer> set) {
        Assert.assertEquals(tree, node.getOwner());
        set.remove(node.getData());
    }

}
