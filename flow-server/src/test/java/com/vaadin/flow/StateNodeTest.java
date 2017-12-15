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

package com.vaadin.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.change.NodeAttachChange;
import com.vaadin.flow.change.NodeChange;
import com.vaadin.flow.change.NodeDetachChange;
import com.vaadin.flow.nodefeature.ElementAttributeMap;
import com.vaadin.flow.nodefeature.ElementChildrenList;
import com.vaadin.flow.nodefeature.ElementClassList;
import com.vaadin.flow.nodefeature.ElementData;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.nodefeature.NodeFeature;
import com.vaadin.flow.shared.Registration;
import com.vaadin.ui.UI;

public class StateNodeTest {

    private static class TestStateNode extends StateNode {
        private int i = -1;

        public TestStateNode() {
            super(ElementChildrenList.class);
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
    public void nodeContainsDefinedFeatures() {
        StateNode node = new StateNode(ElementData.class);

        Assert.assertTrue("Should have feature defined in constructor",
                node.hasFeature(ElementData.class));

        ElementData feature = node.getFeature(ElementData.class);

        Assert.assertNotNull("Existing feature should also be available",
                feature);

        Assert.assertFalse(
                "Should not have a feature that wasn't defined in constructor",
                node.hasFeature(ElementPropertyMap.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getMissingFeatureThrows() {
        StateNode node = new StateNode(ElementData.class);
        node.getFeature(ElementPropertyMap.class);
    }

    @Test
    public void testAttachDetachChangeCollection() {
        StateNode node = createEmptyNode();

        List<NodeChange> changes = new ArrayList<>();
        Consumer<NodeChange> collector = changes::add;

        node.collectChanges(collector);

        Assert.assertTrue("Node should have no changes", changes.isEmpty());

        // Attach node
        setParent(node, createStateTree().getRootNode());

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

        StateNode root = createStateTree().getRootNode();

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

        StateNode root = createStateTree().getRootNode();

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
     *
     * Firefox won't show elements nested more than 200 levels deep, thus makes
     * no sense to test insane depth.
     */
    @Test
    public void recursiveTreeNavigation_resilienceInDepth() {
        TestStateNode childOfRoot = new TestStateNode();
        TestStateNode node = createTree(childOfRoot, 5000);
        StateTree tree = createStateTree();

        setParent(childOfRoot, tree.getRootNode());

        Set<Integer> set = IntStream.range(-1, node.getData() + 1).boxed()
                .collect(Collectors.toSet());
        childOfRoot.visitNodeTree(n -> visit((TestStateNode) n, tree, set));
        Assert.assertTrue(set.isEmpty());
    }

    /**
     * Test for #252: stack overflow exception
     *
     * Firefox won't show elements nested more than 200 levels deep, thus makes
     * no sense to test insane depth.
     */
    @Test
    public void recursiveTreeNavigation_resilienceInSize() {
        TestStateNode childOfRoot = new TestStateNode();
        int count = 300;
        StateNode node = createTree(childOfRoot, count);
        while (node.getParent() != null) {
            node = node.getParent();
            for (int i = 1; i < 50; i++) {
                TestStateNode child = new TestStateNode();
                setParent(child, node);
                child.setData(count);
                count++;
            }
        }
        StateTree tree = createStateTree();

        setParent(childOfRoot, tree.getRootNode());

        Set<Integer> set = IntStream.range(-1, count).boxed()
                .collect(Collectors.toSet());
        childOfRoot.visitNodeTree(n -> visit((TestStateNode) n,
                (StateTree) childOfRoot.getOwner(), set));
        Assert.assertTrue(set.isEmpty());
    }

    @Test
    public void nodeTreeOnAttach_bottomUpTraversing_correctOrder() {
        TestStateNode root = new TestStateNode();
        LinkedList<Integer> data = new LinkedList<>();
        data.add(0);
        root.setData(0);
        int count = 1;

        for (int i = 0; i < 5; i++) {
            TestStateNode childOfRoot = new TestStateNode();
            childOfRoot.setData(count);
            data.add(count);
            if (i % 2 == 0) {
                for (int j = 0; j < 5; j++) {
                    TestStateNode child = new TestStateNode();
                    setParent(child, childOfRoot);
                    child.setData(count);
                    data.add(count);
                    count++;
                }
            }
            setParent(childOfRoot, root);
        }

        root.visitNodeTreeBottomUp(node -> Assert.assertEquals(
                ((Integer) ((TestStateNode) node).getData()),
                data.removeLast()));
    }

    @Test
    public void testAttachListener_onSetParent_listenerTriggered() {
        StateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        Assert.assertFalse(child.isAttached());
        AtomicBoolean triggered = new AtomicBoolean(false);

        child.addAttachListener(() -> triggered.set(true));

        setParent(child, root);

        Assert.assertTrue(triggered.get());
    }

    @Test
    public void testAttachListener_listenerRemoved_listenerNotTriggered() {
        StateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        Assert.assertFalse(child.isAttached());
        AtomicBoolean triggered = new AtomicBoolean(false);

        Registration registrationHandle = child
                .addAttachListener(() -> triggered.set(true));
        registrationHandle.remove();

        setParent(child, root);

        Assert.assertFalse(triggered.get());
    }

    @Test
    public void testDetachListener_onSetParent_listenerTriggered() {
        StateNode root = new RootStateNode();
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
        StateNode root = new RootStateNode();
        TestStateNode child = new TestStateNode();

        setParent(child, root);
        Assert.assertTrue(child.isAttached());

        AtomicBoolean triggered = new AtomicBoolean(false);

        Registration registrationHandle = child
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
        return createTestNode(toString, ElementChildrenList.class);
    }

    @SafeVarargs
    public static StateNode createTestNode(String toString,
            Class<? extends NodeFeature>... features) {
        return new StateNode(features) {
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

            ElementChildrenList children = parent
                    .getFeature(ElementChildrenList.class);
            children.remove(children.indexOf(child));
        } else {
            // Add child
            assert child.getParent() == null;

            ElementChildrenList children = parent
                    .getFeature(ElementChildrenList.class);
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
            node.getFeature(ElementChildrenList.class).add(0, child);
            node = child;
        }
        return node;
    }

    private void visit(TestStateNode node, StateTree tree, Set<Integer> set) {
        Assert.assertEquals(tree, node.getOwner());
        set.remove(node.getData());
    }

    private StateTree createStateTree() {
        StateTree stateTree = new StateTree(new UI(),
                ElementChildrenList.class);
        return stateTree;
    }

    @Test
    public void runWhenAttachedNodeNotAttached() {
        StateTree tree = createStateTree();
        AtomicInteger commandRun = new AtomicInteger(0);
        StateNode n1 = createEmptyNode();
        n1.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });

        Assert.assertEquals(0, commandRun.get());

        setParent(n1, tree.getRootNode());
        Assert.assertEquals(1, commandRun.get());
        setParent(n1, null);
        setParent(n1, tree.getRootNode());
        Assert.assertEquals(1, commandRun.get());
    }

    @Test
    public void runMultipleWhenAttachedNodeNotAttached() {
        StateTree tree = createStateTree();
        AtomicInteger commandRun = new AtomicInteger(0);
        StateNode n1 = createEmptyNode();
        n1.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });
        n1.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });

        Assert.assertEquals(0, commandRun.get());

        setParent(n1, tree.getRootNode());
        Assert.assertEquals(2, commandRun.get());
    }

    @Test
    public void runWhenAttachedNodeAttached() {
        AtomicInteger commandRun = new AtomicInteger(0);
        StateNode n1 = createEmptyNode();
        StateTree tree = createStateTree();
        setParent(n1, tree.getRootNode());
        n1.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });

        Assert.assertEquals(1, commandRun.get());
    }

    @Test
    public void requiredFeatures() {
        StateNode stateNode = new StateNode(
                Arrays.asList(ElementClassList.class, ElementPropertyMap.class),
                ElementAttributeMap.class);

        Assert.assertTrue(stateNode.hasFeature(ElementClassList.class));
        Assert.assertTrue(stateNode.hasFeature(ElementPropertyMap.class));
        Assert.assertTrue(stateNode.hasFeature(ElementAttributeMap.class));

        Assert.assertTrue(stateNode.isReportedFeature(ElementClassList.class));
        Assert.assertTrue(
                stateNode.isReportedFeature(ElementPropertyMap.class));
        Assert.assertFalse(
                stateNode.isReportedFeature(ElementAttributeMap.class));
    }
}
