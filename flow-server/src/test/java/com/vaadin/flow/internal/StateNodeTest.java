/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeAttachChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.change.NodeDetachChange;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementClassList;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.NodeFeature;
import com.vaadin.flow.shared.Registration;

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

    private static class TestStateTree extends StateTree {

        private Set<StateNode> dirtyNodes;

        public TestStateTree() {
            super(new UI().getInternals(), ElementChildrenList.class);
        }

        @Override
        public void markAsDirty(StateNode node) {
            super.markAsDirty(node);
            if (dirtyNodes == null) {
                dirtyNodes = new HashSet<>();
            }
            dirtyNodes.add(node);
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
    public void attachDetachChangeCollection() {
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
    public void setAsOwnParent() {
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
    public void attachListener_onSetParent_listenerTriggered() {
        StateNode root = new TestStateTree().getRootNode();
        TestStateNode child = new TestStateNode();

        Assert.assertFalse(child.isAttached());
        AtomicBoolean triggered = new AtomicBoolean(false);

        child.addAttachListener(() -> triggered.set(true));

        setParent(child, root);

        Assert.assertTrue(triggered.get());
    }

    @Test
    public void attachListener_listenerRemoved_listenerNotTriggered() {
        StateNode root = new TestStateTree().getRootNode();
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
    public void detachListener_onSetParent_listenerTriggered() {
        StateNode root = new TestStateTree().getRootNode();
        TestStateNode child = new TestStateNode();

        setParent(child, root);
        Assert.assertTrue(child.isAttached());

        AtomicBoolean triggered = new AtomicBoolean(false);

        child.addDetachListener(() -> triggered.set(true));

        setParent(child, null);

        Assert.assertTrue("Detach listener was not triggered.",
                triggered.get());
    }

    @Test
    public void detachListener_listenerRemoved_listenerNotTriggered() {
        StateNode root = new TestStateTree().getRootNode();
        TestStateNode child = new TestStateNode();

        setParent(child, root);
        Assert.assertTrue(child.isAttached());

        AtomicBoolean triggered = new AtomicBoolean(false);

        Registration registrationHandle = child
                .addDetachListener(() -> triggered.set(true));
        registrationHandle.remove();

        setParent(child, null);

        Assert.assertFalse(
                "Detach listener was triggered even though handler was removed.",
                triggered.get());
    }

    @Test
    public void detachListener_removesNode_notUnregisteredTwice() {
        StateTree tree = createStateTree();
        StateNode root = createParentNode("");
        setParent(root, tree.getRootNode());

        TestStateNode child = new TestStateNode();

        setParent(child, root);
        Assert.assertTrue(child.isAttached());

        AtomicBoolean triggered = new AtomicBoolean(false);

        child.addDetachListener(() -> {
            Assert.assertTrue(
                    "Child node should still have a parent and be been seen as attached",
                    child.isAttached());
            Assert.assertFalse("Child node should have been unregistered",
                    tree.hasNode(child));

            child.setParent(null);

            triggered.set(true);
        });

        setParent(child, null);

        Assert.assertTrue("Detach listener was not triggered.",
                triggered.get());
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

            @Override
            public boolean hasFeature(
                    Class<? extends NodeFeature> featureType) {
                // Inform that we don't have ElementData so that PropertyMap
                // doesn't try to initialize an element for our test state node
                if (featureType.isAssignableFrom(ElementData.class)) {
                    return false;
                }
                return super.hasFeature(featureType);
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
        StateTree stateTree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        return stateTree;
    }

    @Test
    public void runWhenAttachedNodeNotAttached() {
        StateTree tree = createStateTree();
        AtomicInteger commandRun = new AtomicInteger(0);
        StateNode node = createEmptyNode();
        node.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });

        Assert.assertEquals(0, commandRun.get());

        setParent(node, tree.getRootNode());
        Assert.assertEquals(1, commandRun.get());
        setParent(node, null);
        setParent(node, tree.getRootNode());
        Assert.assertEquals(1, commandRun.get());
    }

    @Test
    public void runMultipleWhenAttachedNodeNotAttached() {
        StateTree tree = createStateTree();
        AtomicInteger commandRun = new AtomicInteger(0);
        StateNode node = createEmptyNode();
        node.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });
        node.runWhenAttached(ui -> {
            Assert.assertEquals(tree.getUI(), ui);
            commandRun.incrementAndGet();
        });

        Assert.assertEquals(0, commandRun.get());

        setParent(node, tree.getRootNode());
        Assert.assertEquals(2, commandRun.get());
    }

    @Test
    public void runWhenAttachedNodeAttached() {
        AtomicInteger commandRun = new AtomicInteger(0);
        StateNode node = createEmptyNode();
        StateTree tree = createStateTree();
        setParent(node, tree.getRootNode());
        node.runWhenAttached(ui -> {
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

    @Test
    public void collectChanges_initiallyActiveElement_sendOnlyDisalowFeatureChangesWhenInactive() {
        StateNode stateNode = createTestNode("Active node",
                ElementPropertyMap.class, ElementData.class);

        ElementData visibility = stateNode.getFeature(ElementData.class);
        ElementPropertyMap properties = stateNode
                .getFeature(ElementPropertyMap.class);

        TestStateTree tree = new TestStateTree();

        // attach the node to be able to get changes
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0,
                stateNode);

        assertCollectChanges_initiallyVisible(stateNode, properties,
                isVisible -> {
                    visibility.setVisible(isVisible);
                    stateNode.updateActiveState();
                });
    }

    @Test
    public void collectChanges_inactivateViaParent_initiallyActiveElement_sendOnlyDisalowFeatureChangesWhenInactive() {
        StateNode stateNode = createTestNode("Active node",
                ElementPropertyMap.class, ElementData.class);

        StateNode parent = createTestNode("Parent node",
                ElementPropertyMap.class, ElementData.class,
                ElementChildrenList.class);

        parent.getFeature(ElementChildrenList.class).add(0, stateNode);

        ElementData visibility = parent.getFeature(ElementData.class);
        ElementPropertyMap properties = stateNode
                .getFeature(ElementPropertyMap.class);

        TestStateTree tree = new TestStateTree();

        // attach the node to be able to get changes
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0, parent);

        assertCollectChanges_initiallyVisible(stateNode, properties,
                isVisible -> {
                    visibility.setVisible(isVisible);
                    parent.updateActiveState();
                });
    }

    @Test
    public void collectChanges_initiallyInactiveElement_sendOnlyDisalowAndReportedFeatures_sendAllChangesWhenActive() {
        Element element = ElementFactory.createAnchor();

        StateNode stateNode = element.getNode();

        ElementData visibility = stateNode.getFeature(ElementData.class);
        ElementPropertyMap properties = stateNode
                .getFeature(ElementPropertyMap.class);

        TestStateTree tree = new TestStateTree();

        // attach the node to be able to get changes
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0,
                stateNode);

        assertCollectChanges_initiallyInactive(stateNode, properties,
                isVisible -> {
                    visibility.setVisible(isVisible);
                    stateNode.updateActiveState();
                });
    }

    @Test
    public void collectChanges_initiallyInactiveViaParentElement_sendOnlyDisalowAndReportedFeatures_sendAllChangesWhenActive() {
        Element element = ElementFactory.createAnchor();

        StateNode stateNode = element.getNode();

        StateNode parent = createTestNode("Parent node",
                ElementPropertyMap.class, ElementData.class,
                ElementChildrenList.class);

        parent.getFeature(ElementChildrenList.class).add(0, stateNode);

        ElementData visibility = parent.getFeature(ElementData.class);

        ElementPropertyMap properties = stateNode
                .getFeature(ElementPropertyMap.class);

        TestStateTree tree = new TestStateTree();

        // attach the node to be able to get changes
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0, parent);

        assertCollectChanges_initiallyInactive(stateNode, properties,
                isVisible -> {
                    visibility.setVisible(isVisible);
                    parent.updateActiveState();
                });
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           ^         |
     *           |---------+
     *
     *         parent is attached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>c</code> goes to <code>a</code> as a child when attach event is fired for <code>b</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInAttachListener_firstAsParent_lastAsChild() {
        assertAttachDetachEvents(createNodes(), "a", "c", false);
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           |         ^
     *           +---------|
     *
     *         parent is attached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>a</code> goes to <code>c</code> as a child when attach event is fired for <code>b</code>
     *  Only one attach event is expected for <code>a</code> because it had not been yet attached when attach event for
     *  <code>b</code> is fired and it has been detached from <code>parent</code> and attached to <code>c</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInAttachListener_lastAsParent_firstAsChild() {
        assertAttachDetachEvents(createNodes(), "c", "a", true);
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           |    ^
     *           +----|
     *
     *         parent is attached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>a</code> goes to <code>b</code> as a child when attach event is fired for <code>c</code>
     *  Only one attach event is expected for <code>a</code> because it had not been yet attached when attach event for
     *  <code>c</code> is fired and it has been detached from <code>parent</code> and attached to <code>b</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInAttachListener_middleAsParent_firstAsChild() {
        assertAttachDetachEvents(createNodes(), "b", "a", true);
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           ^    |
     *           |----+
     *
     *         parent is attached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>b</code> goes to <code>a</code> as a child when attach event is fired for <code>c</code>
     *  Only one attach event is expected for <code>b</code> because it had not been yet attached when attach event for
     *  <code>c</code> is fired and it has been detached from <code>parent</code> and attached to <code>a/code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInAttachListener_firstAsParent_middleAsChild() {
        assertAttachDetachEvents(createNodes(), "a", "b", true);
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *                ^    |
     *                |----+
     *
     *         parent is attached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>c</code> goes to <code>b</code> as a child when attach event is fired for <code>a</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInAttachListener_middleAsParent_lastAsChild() {
        assertAttachDetachEvents(createNodes(), "b", "c", false);
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *                |    ^
     *                +----|
     *
     *         parent is attached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>b</code> goes to <code>c</code> as a child when attach event is fired for <code>a</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInAttachListener_lastAsParent_middleAsChild() {
        assertAttachDetachEvents(createNodes(), "c", "b", false);
    }

    @Test
    public void detachParent_detachFirstChildOnDetachLast_oneDetachEvent() {
        TestStateTree tree = new TestStateTree();

        StateNode childA = createEmptyNode("a");
        StateNode childB = createEmptyNode("b");

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);
        addChild(parent, childB);

        addChild(tree.getRootNode(), parent);

        AtomicInteger detachEvents = new AtomicInteger();
        childB.addDetachListener(() -> removeFromParent(childA));
        childA.addDetachListener(() -> detachEvents.incrementAndGet());

        removeFromParent(parent);

        Assert.assertEquals(1, detachEvents.get());
    }

    @Test
    public void detachParent_detachLastChildOnDetachFirst_oneDetachEvent() {
        TestStateTree tree = new TestStateTree();

        StateNode childA = createEmptyNode("a");
        StateNode childB = createEmptyNode("b");

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);
        addChild(parent, childB);

        addChild(tree.getRootNode(), parent);

        AtomicInteger detachEvents = new AtomicInteger();
        childA.addDetachListener(() -> removeFromParent(childA));
        childB.addDetachListener(() -> detachEvents.incrementAndGet());

        removeFromParent(parent);

        Assert.assertEquals(1, detachEvents.get());
    }

    @Test
    public void detachParent_appendChildOnDetach_noEvents() {
        TestStateTree tree = new TestStateTree();

        StateNode childA = createEmptyNode("a");

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);

        addChild(tree.getRootNode(), parent);

        AtomicInteger events = new AtomicInteger();
        childA.addDetachListener(() -> {
            StateNode b = createEmptyNode("b");
            b.addAttachListener(events::incrementAndGet);
            b.addDetachListener(events::incrementAndGet);
            addChild(parent, b);
        });

        removeFromParent(parent);
        Assert.assertEquals(0, events.get());
    }

    @Test
    public void detachParent_insertChildAsFirstOnDetach_noEvents() {
        TestStateTree tree = new TestStateTree();

        StateNode child = createEmptyNode("a");

        StateNode parent = createParentNode("parent");

        addChild(parent, child);

        addChild(tree.getRootNode(), parent);

        AtomicInteger events = new AtomicInteger();
        child.addDetachListener(() -> {
            StateNode b = createEmptyNode("b");
            b.addAttachListener(events::incrementAndGet);
            b.addDetachListener(events::incrementAndGet);
            ElementChildrenList list = parent
                    .getFeature(ElementChildrenList.class);
            list.add(0, b);
        });

        removeFromParent(parent);
        Assert.assertEquals(0, events.get());
    }

    @Test
    public void attachParent_detachFirstOnAttachLast_noEvents() {
        TestStateTree tree = new TestStateTree();

        StateNode childA = createEmptyNode("a");
        StateNode childB = createEmptyNode("b");

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);
        addChild(parent, childB);

        AtomicInteger events = new AtomicInteger();
        childB.addAttachListener(() -> {
            removeFromParent(childA);
        });

        childA.addAttachListener(events::incrementAndGet);
        childA.addDetachListener(events::incrementAndGet);

        addChild(tree.getRootNode(), parent);

        // events are fired from right to left, so <code>b</code> had been
        // handled first and <code>a</code> had been detached before attach
        // event has been fired for <code>a</code>. So no events for
        // <code>a</code>
        Assert.assertEquals(0, events.get());
    }

    @Test
    public void attachParent_detachLastOnAttachFirst_attachDetachEvents() {
        TestStateTree tree = new TestStateTree();

        StateNode childA = createEmptyNode("a");
        StateNode childB = createEmptyNode("b");

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);
        addChild(parent, childB);

        childA.addAttachListener(() -> {
            removeFromParent(childB);
        });

        List<Boolean> attachDetachEvents = new ArrayList<>();
        childB.addAttachListener(() -> attachDetachEvents.add(true));
        childB.addDetachListener(() -> attachDetachEvents.add(false));

        addChild(tree.getRootNode(), parent);

        /*
         * Here attach event for <code>b</code> had been fired first since it
         * had been handled first. Then on attach event for <code>a</code> the
         * <code>b</code> has been removed. So we should get also a detach
         * event.
         */
        Assert.assertEquals(2, attachDetachEvents.size());
        Assert.assertTrue(attachDetachEvents.get(0));
        Assert.assertFalse(attachDetachEvents.get(1));
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           ^         |
     *           |---------+
     *
     *         parent is detached,
     *  detach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>c</code> goes to <code>a</code> as a child when detach event is fired for <code>b</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInDetachListener_firstAsParent_lastAsChild() {
        assertDetachAttachEvents(createNodes(), "a", "c");
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           |         ^
     *           +---------|
     *
     *         parent is detached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>a</code> goes to <code>c</code> as a child when detach event is fired for <code>b</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInDetachListener_lastAsParent_firstAsChild() {
        assertDetachAttachEvents(createNodes(), "c", "a");
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           |    ^
     *           +----|
     *
     *         parent is detached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>a</code> goes to <code>b</code> as a child when detach event is fired for <code>c</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInDetachListener_middleAsParent_firstAsChild() {
        assertDetachAttachEvents(createNodes(), "b", "a");
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *           ^    |
     *           |----+
     *
     *         parent is detached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>b</code> goes to <code>a</code> as a child when detach event is fired for <code>c</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInDetachListener_firstAsParent_middleAsChild() {
        assertDetachAttachEvents(createNodes(), "a", "b");
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *                ^    |
     *                |----+
     *
     *         parent is detached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>c</code> goes to <code>b</code> as a child when detach event is fired for <code>a</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInDetachListener_middleAsParent_lastAsChild() {
        assertDetachAttachEvents(createNodes(), "b", "c");
    }

    /**
     * @formatter:off
     *
     *              parent
     *            /   |   \
     *           a    b    c
     *                |    ^
     *                +----|
     *
     *         parent is detached,
     *  attach events are fired from bottom to up, from right, to left:
     *  the order of firing events is : c, b, a, parent
     *
     *  In this test <code>b</code> goes to <code>c</code> as a child when detach event is fired for <code>a</code>
     *
     * @formatter:on
     */
    @Test
    public void modifyNodeTreeInDetachListener_lastAsParent_middleAsChild() {
        assertDetachAttachEvents(createNodes(), "c", "b");
    }

    /**
     * #5316: removeFromTree removes StateTree reference and resets descendant
     * nodes.
     */
    @Test
    public void removeFromTree_nodeAttached_detachedAndDescendantsReset() {
        // given grandParent -> parent -> child
        StateNode grandParent = createParentNode("grandParent");
        StateNode parent = createParentNode("parent");
        addChild(grandParent, parent);
        StateNode child = createEmptyNode("child");
        addChild(parent, child);

        TestStateTree tree = new TestStateTree();
        addChild(tree.getRootNode(), grandParent);

        // when parent is removed from the tree
        parent.removeFromTree();

        // then parent's parent is null
        Assert.assertNull(parent.getParent());

        // then parent and its descendants are reset
        assertNodesReset(parent,child);
    }

    /**
     * #5316: removeFromTree when invoked from a DetachListener removes
     * StateTree reference and resets descendant nodes.
     */
    @Test
    public void removeFromTree_nodeAttachedAndInDetachListener_detachedAndDescendantsReset() {
        // given grandParent -> parent -> child
        StateNode grandParent = createParentNode("grandParent");
        StateNode parent = createParentNode("parent");
        addChild(grandParent, parent);
        StateNode child = createEmptyNode("child");
        addChild(parent, child);

        TestStateTree tree = new TestStateTree();
        addChild(tree.getRootNode(), grandParent);

        // given parent's detach listener removes the node from the tree
        parent.addDetachListener(() -> parent.removeFromTree());

        // when parent is removed from the tree
        parent.setParent(null);

        // then parent and its descendants are reset
        assertNodesReset(parent,child);
    }

    private void assertNodesReset(StateNode... nodes) {
        for (StateNode node : nodes) {
            Assert.assertEquals(-1, node.getId());
            Assert.assertFalse(node.isAttached());
            Assert.assertEquals(NullOwner.get(), node.getOwner());
            node.collectChanges(c -> Assert.fail("No changes expected"));
        }
    }

    private void assertAttachDetachEvents(Map<String, StateNode> nodes,
            String newParent, String child, boolean expectSingleEvent) {
        TestStateTree tree = new TestStateTree();

        // use the order from the list
        StateNode childA = nodes.get("a");
        StateNode childB = nodes.get("b");
        StateNode childC = nodes.get("c");

        // those are the same nodes that above but it's easier to have a
        // dedicate variables for them
        StateNode newParentNode = nodes.remove(newParent);
        StateNode childNode = nodes.remove(child);

        StateNode nodeWithListener = nodes.values().iterator().next();
        nodeWithListener.addAttachListener(() -> {
            addChild(newParentNode, childNode);
        });

        List<Object> attachDetachEvents = new ArrayList<>();
        childNode.addAttachListener(() -> {
            attachDetachEvents.add(childNode.getParent());
        });
        childNode.addDetachListener(() -> attachDetachEvents.add(false));

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);
        addChild(parent, childB);
        addChild(parent, childC);

        addChild(tree.getRootNode(), parent);

        /*
         * Depending on the order of children there should be either only one
         * ATTACH event or 3 events: intermediate ATTACH and DETACH and final
         * ATTACH.
         */
        if (expectSingleEvent) {
            Assert.assertEquals(1, attachDetachEvents.size());
            Assert.assertEquals(newParentNode, attachDetachEvents.get(0));
        } else {
            Assert.assertEquals(3, attachDetachEvents.size());
            Assert.assertEquals(parent, attachDetachEvents.get(0));
            Assert.assertEquals(Boolean.FALSE, attachDetachEvents.get(1));
            Assert.assertEquals(newParentNode, attachDetachEvents.get(2));
        }

        Assert.assertEquals(newParentNode, childNode.getParent());
    }

    private void assertDetachAttachEvents(Map<String, StateNode> nodes,
            String newParent, String child) {
        TestStateTree tree = new TestStateTree();

        // use the order from the list
        StateNode childA = nodes.get("a");
        StateNode childB = nodes.get("b");
        StateNode childC = nodes.get("c");

        // those are the same nodes that above but it's easier to have a
        // dedicate variables for them
        StateNode newParentNode = nodes.remove(newParent);
        StateNode childNode = nodes.remove(child);

        StateNode nodeWithListener = nodes.values().iterator().next();
        nodeWithListener.addDetachListener(() -> {
            addChild(newParentNode, childNode);
        });

        StateNode parent = createParentNode("parent");

        addChild(parent, childA);
        addChild(parent, childB);
        addChild(parent, childC);

        addChild(tree.getRootNode(), parent);

        List<Object> attachDetachEvents = new ArrayList<>();
        childNode.addAttachListener(() -> {
            attachDetachEvents.add(childNode.getParent());
        });
        childNode.addDetachListener(() -> attachDetachEvents.add(false));

        removeFromParent(parent);

        // Only one DETACH event is expected
        Assert.assertEquals(1, attachDetachEvents.size());
        Assert.assertFalse((Boolean) attachDetachEvents.get(0));
    }

    private Map<String, StateNode> createNodes() {
        return Stream
                .of(createParentNode("a"), createParentNode("b"),
                        createParentNode("c"))
                .collect(Collectors.toMap(node -> node.toString(),
                        Function.identity()));
    }

    private void addChild(StateNode parent, StateNode node) {
        removeFromParent(node);
        ElementChildrenList list = parent.getFeature(ElementChildrenList.class);
        list.add(list.size(), node);
    }

    private void removeFromParent(StateNode node) {
        if (node.getParent() == null) {
            return;
        }
        ElementChildrenList list = node.getParent()
                .getFeature(ElementChildrenList.class);
        for (int i = 0; i < list.size(); i++) {
            StateNode child = list.get(i);
            if (node.equals(child)) {
                list.remove(i);
                break;
            }
        }
    }

    private void assertCollectChanges_initiallyInactive(StateNode stateNode,
            ElementPropertyMap properties, Consumer<Boolean> activityUpdater) {

        properties.setProperty("foo", "bar");

        TestStateTree tree = (TestStateTree) stateNode.getOwner();
        tree.dirtyNodes.clear();

        ElementData visibility = stateNode.getFeature(ElementData.class);
        activityUpdater.accept(false);

        // activity updater may modify visibility of the node itself or its
        // ancestor. The number of changes will depend on whether the subject
        // node is visible or not
        boolean visibilityChanged = !visibility.isVisible();

        List<NodeChange> changes = new ArrayList<>();
        stateNode.collectChanges(changes::add);

        if (visibilityChanged) {
            Assert.assertEquals(1, tree.dirtyNodes.size());
            Assert.assertThat(tree.dirtyNodes, CoreMatchers.hasItem(stateNode));
        } else {
            // the target node should be marked as dirty because it's visible
            // but its parent is inactive
            Assert.assertEquals(2, tree.dirtyNodes.size());
            stateNode.visitNodeTree(node -> Assert.assertThat(tree.dirtyNodes,
                    CoreMatchers.hasItem(node)));
        }

        Assert.assertEquals(visibilityChanged ? 3 : 2, changes.size());
        // node is attached event
        Assert.assertThat(changes.get(0),
                CoreMatchers.instanceOf(NodeAttachChange.class));
        // tag update (ElementData is reported feature) and possible active
        // state update
        Optional<MapPutChange> tagFound = changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(chang -> chang.getKey().equals("tag")).findFirst();
        Assert.assertTrue("No tag change found", tagFound.isPresent());
        MapPutChange tagChange = tagFound.get();

        MapPutChange change = (MapPutChange) changes.get(1);
        if (visibilityChanged) {
            Assert.assertThat(changes.get(2),
                    CoreMatchers.instanceOf(MapPutChange.class));
            change = tagChange.equals(change) ? (MapPutChange) changes.get(2)
                    : change;
        }

        Assert.assertEquals(Element.get(stateNode).getTag(),
                tagChange.getValue());

        if (visibilityChanged) {
            Assert.assertEquals(Boolean.FALSE, change.getValue());
        }

        changes.clear();

        // now the node becomes active and should send all values from all
        // features (including values that has not been sent previously).

        activityUpdater.accept(true);

        properties.setProperty("baz", "foo");
        stateNode.collectChanges(changes::add);

        Assert.assertEquals(visibilityChanged ? 3 : 2, changes.size());
        // node is attached event
        // property updates and possible visibility update
        Assert.assertThat(changes.get(1),
                CoreMatchers.instanceOf(MapPutChange.class));

        Optional<MapPutChange> visibilityChange = changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(chang -> chang.getFeature().equals(ElementData.class))
                .findFirst();

        if (visibilityChanged) {
            Assert.assertTrue(visibilityChange.isPresent());
            Assert.assertTrue((Boolean) visibilityChange.get().getValue());
            changes.remove(visibilityChange.get());
        }

        Optional<MapPutChange> fooUpdate = changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(chang -> chang.getKey().equals("foo")).findFirst();

        Assert.assertTrue(fooUpdate.isPresent());
        Assert.assertEquals("bar", fooUpdate.get().getValue());

        changes.remove(fooUpdate.get());

        change = (MapPutChange) changes.get(0);
        Assert.assertEquals("foo", change.getValue());
        Assert.assertEquals("baz", change.getKey());

        // Don't make any changes, check that there are no changes collected
        changes.clear();
        stateNode.collectChanges(changes::add);

        Assert.assertEquals(0, changes.size());
    }

    private void assertCollectChanges_initiallyVisible(StateNode stateNode,
            ElementPropertyMap properties, Consumer<Boolean> activityUpdater) {
        ElementData visibility = stateNode.getFeature(ElementData.class);

        // check that normal flow works as it should (without any inactivity)
        properties.setProperty("foo", "bar");

        List<NodeChange> changes = new ArrayList<>();
        stateNode.collectChanges(changes::add);

        Assert.assertEquals(2, changes.size());
        // node is attached event
        Assert.assertThat(changes.get(0),
                CoreMatchers.instanceOf(NodeAttachChange.class));
        // the property update event
        Assert.assertThat(changes.get(1),
                CoreMatchers.instanceOf(MapPutChange.class));

        changes.clear();

        TestStateTree tree = (TestStateTree) stateNode.getOwner();
        tree.dirtyNodes.clear();

        // now make the node inactive via the VisibiltyData

        activityUpdater.accept(false);

        // now the node becomes inactive and should send only changes for
        // VisibiltyData, but don't loose changes for other features

        properties.setProperty("foo", "baz");

        stateNode.collectChanges(changes::add);

        // activity updater may modify visibility of the node itself or its
        // ancestor. The number of changes will depend on whether the subject
        // node is visible or not
        boolean visibilityChanged = !visibility.isVisible();

        // The only possible change is visibility value change
        Assert.assertEquals(visibilityChanged ? 1 : 0, changes.size());

        MapPutChange change;
        if (visibilityChanged) {
            Assert.assertEquals(1, tree.dirtyNodes.size());
            Assert.assertThat(tree.dirtyNodes, CoreMatchers.hasItem(stateNode));
            Assert.assertThat(changes.get(0),
                    CoreMatchers.instanceOf(MapPutChange.class));
            change = (MapPutChange) changes.get(0);
            Assert.assertEquals(ElementData.class, change.getFeature());
        } else {
            // the target node should be marked as dirty because it's visible
            // but its parent is inactive
            Assert.assertEquals(2, tree.dirtyNodes.size());
            stateNode.visitNodeTree(node -> Assert.assertThat(tree.dirtyNodes,
                    CoreMatchers.hasItem(node)));
        }

        changes.clear();

        // make the node active again
        activityUpdater.accept(true);

        stateNode.collectChanges(changes::add);

        // Two possible changes: probable visibility value change and property
        // update change
        Assert.assertEquals(visibilityChanged ? 2 : 1, changes.size());
        Assert.assertThat(changes.get(0),
                CoreMatchers.instanceOf(MapPutChange.class));
        change = (MapPutChange) changes.get(0);

        MapPutChange propertyChange;

        if (visibilityChanged) {
            MapPutChange visibilityChange = ElementData.class
                    .equals(change.getFeature()) ? change
                            : (MapPutChange) changes.get(1);
            propertyChange = change.equals(visibilityChange)
                    ? (MapPutChange) changes.get(1)
                    : change;
        } else {
            propertyChange = change;
        }

        Assert.assertEquals(ElementPropertyMap.class,
                propertyChange.getFeature());
        Assert.assertEquals("baz", propertyChange.getValue());

        // Don't make any changes, check that there are no changes collected
        changes.clear();
        stateNode.collectChanges(changes::add);

        Assert.assertEquals(0, changes.size());
    }
}
