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

package com.vaadin.flow.internal;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
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
import com.vaadin.flow.internal.nodefeature.PushConfigurationMap.PushConfigurationParametersMap;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.TestUtil;

public class StateTreeTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private StateTree tree = new UI().getInternals().getStateTree();

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

    public static class CollectableNode extends StateNode {

        public CollectableNode() {
            super(ElementData.class, ElementChildrenList.class);
        }

        @Override
        public void collectChanges(Consumer<NodeChange> collector) {
            collector.accept(new NodeChange(this) {
                @Override
                protected void populateJson(ObjectNode json,
                        ConstantPool constantPool) {
                }
            });
        }
    }

    @Test
    public void rootNodeState() {
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
    public void rootNode_setStateNodeAsParent_throws() {
        tree.getRootNode().setParent(new StateNode());
    }

    @Test
    public void rootNode_setNullAsParent_nodeIsDetached() {
        AtomicInteger detachCount = new AtomicInteger();
        Assert.assertTrue(tree.hasNode(tree.getRootNode()));
        tree.getRootNode()
                .addDetachListener(() -> detachCount.incrementAndGet());
        tree.getRootNode().setParent(null);
        Assert.assertEquals(1, detachCount.get());
        Assert.assertFalse(tree.getRootNode().isAttached());

        Assert.assertFalse(tree.hasNode(tree.getRootNode()));
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

        StateTree anotherTree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);

        StateNodeTest.setParent(node, anotherTree.getRootNode());
    }

    @Test
    public void moveNodeToOtherRoot_removeFromTree_doesNotThrow() {
        StateNode node = StateNodeTest.createEmptyNode();

        StateNodeTest.setParent(node, tree.getRootNode());

        node.removeFromTree();

        StateTree anotherTree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);

        StateNodeTest.setParent(node, anotherTree.getRootNode());
    }

    @Test
    public void testNoRootAttachChange() {
        List<NodeChange> changes = collectChangesExceptChildrenAddRemove();

        for (NodeChange change : changes) {
            if (change instanceof NodeFeatureChange) {
                Class<? extends NodeFeature> feature = ((NodeFeatureChange) change)
                        .getFeature();
                Assert.assertNotEquals(ElementChildrenList.class, feature);
            } else if (change instanceof NodeAttachChange) {
                StateNode node = ((NodeAttachChange) change).getNode();
                Assert.assertNotEquals(tree.getRootNode(), node);
            }
        }

    }

    @Test
    public void testTreeChangeCollection() {
        StateNode node2 = StateNodeTest.createEmptyNode();
        StateNodeTest.setParent(node2, tree.getRootNode());

        List<NodeChange> changes = collectChangesExceptChildrenAddRemove();

        List<NodeChange> notChildrenChanges = new ArrayList<>();
        for (NodeChange change : changes) {
            if (change instanceof NodeFeatureChange) {
                Class<? extends NodeFeature> feature = ((NodeFeatureChange) change)
                        .getFeature();
                Assert.assertNotEquals(ElementChildrenList.class, feature);
            } else {
                notChildrenChanges.add(change);
            }
        }

        Assert.assertEquals(2, notChildrenChanges.size());
        NodeAttachChange nodeChange = (NodeAttachChange) notChildrenChanges
                .get(0);
        // The first node is not in the "hierarchy" tree but is the Push
        // config node
        Assert.assertTrue(nodeChange.getNode()
                .hasFeature(PushConfigurationParametersMap.class));

        NodeAttachChange attachChange = (NodeAttachChange) notChildrenChanges
                .get(1);
        Assert.assertSame(node2, attachChange.getNode());
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

        HashSet<StateNode> dirty = initialDirty.stream().filter(
                node -> !node.hasFeature(PushConfigurationParametersMap.class))
                .collect(Collectors.toCollection(HashSet::new));
        Assert.assertEquals("Both nodes should initially be empty",
                new HashSet<>(Arrays.asList(node1, node2)), dirty);

        tree.collectChanges(change -> {
        });

        node2.markAsDirty();

        Set<StateNode> collectAfterOneMarked = tree.collectDirtyNodes();
        Assert.assertTrue("Marked node should be in collect result",
                collectAfterOneMarked.contains(node2));
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

        Object[] dirty = tree.collectDirtyNodes().stream().filter(
                node -> !node.hasFeature(PushConfigurationParametersMap.class))
                .toArray();
        Assert.assertArrayEquals(expected.toArray(), dirty);

        tree.collectChanges(change -> {
        });

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

        Assert.assertEquals("Should be three changes.", 2, changes.size());
        Assert.assertTrue("First change should re-attach the node.",
                changes.get(0) instanceof NodeAttachChange);
        Assert.assertTrue("Second change should put the tag or payload value.",
                changes.get(1) instanceof MapPutChange);

        Optional<MapPutChange> tagFound = changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(chang -> chang.getKey().equals("tag")).findFirst();
        Assert.assertTrue("No tag change found", tagFound.isPresent());
        MapPutChange nodeChange = tagFound.get();
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
        StateTree tree = new StateTree(new UI().getInternals(), features);

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

        tree.beforeClientResponse(rootNode, context -> results.add(0));
        tree.beforeClientResponse(rootNode, context -> results.add(1));
        tree.beforeClientResponse(rootNode, context -> results.add(2));

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
    public void beforeClientResponse_initiallyAttachedToOneUI_executedWithAnother_executionDoesNotHappen() {
        StateTree initialTree = new UI().getInternals().getStateTree();

        StateNode child = new StateNode(ElementChildrenList.class);
        StateNodeTest.setParent(child, initialTree.getRootNode());

        AtomicBoolean isExecuted = new AtomicBoolean();
        initialTree.beforeClientResponse(child,
                context -> isExecuted.set(true));

        child.removeFromTree();

        StateNodeTest.setParent(child, tree.getRootNode());
        tree.runExecutionsBeforeClientResponse();

        Assert.assertFalse(isExecuted.get());
    }

    @Test
    public void beforeClientResponse_initiallyNotAttached_executedWithUI_executionRun() {
        StateTree someTree = new UI().getInternals().getStateTree();

        StateNode child = new StateNode(ElementChildrenList.class);

        AtomicBoolean isExecuted = new AtomicBoolean();
        someTree.beforeClientResponse(child, context -> isExecuted.set(true));

        StateNodeTest.setParent(child, tree.getRootNode());
        tree.runExecutionsBeforeClientResponse();

        Assert.assertTrue(isExecuted.get());
    }

    @Test
    public void beforeClientResponse_withInnerRunnables() {
        StateNode rootNode = new AttachableNode(true);

        List<Integer> results = new ArrayList<>();

        tree.beforeClientResponse(rootNode, context -> results.add(0));
        tree.beforeClientResponse(rootNode, context -> {
            results.add(1);
            tree.beforeClientResponse(rootNode, context2 -> results.add(3));
            tree.beforeClientResponse(rootNode, context2 -> results.add(4));
        });
        tree.beforeClientResponse(rootNode, context -> results.add(2));

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

        tree.beforeClientResponse(emptyNode, context -> results.add(0));
        tree.beforeClientResponse(rootNode, context -> results.add(1));
        tree.beforeClientResponse(emptyNode, context -> results.add(2));
        tree.beforeClientResponse(rootNode, context -> results.add(3));

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

        tree.beforeClientResponse(emptyNode1, context -> {
            results.add(0);
            StateNodeTest.setParent(emptyNode2, rootNode);
        });
        tree.beforeClientResponse(rootNode, context -> {
            results.add(1);
            StateNodeTest.setParent(emptyNode1, rootNode);
        });
        tree.beforeClientResponse(emptyNode2, context -> results.add(2));
        tree.beforeClientResponse(rootNode, context -> results.add(3));

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
    public void beforeClientResponse_failingExecutionWithNullErrorHandler_NoNPE() {
        thrown.expect(IllegalStateException.class);
        thrown.reportMissingExceptionWithMessage(
                "Failure should be thrown again for no errorhandler");

        StateNode rootNode = tree.getRootNode();
        tree.beforeClientResponse(rootNode, context -> {
            throw new IllegalStateException("Throw before client response");
        });

        Assert.assertNull(tree.getUI().getSession());

        VaadinSession mockSession = Mockito.mock(VaadinSession.class);
        Mockito.when(mockSession.getErrorHandler()).thenReturn(null);

        try {
            tree.getUI().getInternals().setSession(mockSession);
            tree.beforeClientResponse(rootNode, context -> {
                throw new IllegalStateException("Throw before client response");
            });

            tree.runExecutionsBeforeClientResponse();
        } finally {
            tree.getUI().getInternals().setSession(null);
        }
    }

    @Test
    public void beforeClientResponse_failingExecutionWithNullSession_NoNPE() {
        thrown.expect(IllegalStateException.class);
        thrown.reportMissingExceptionWithMessage(
                "Failure should be thrown again for no errorhandler");

        StateNode rootNode = tree.getRootNode();
        tree.beforeClientResponse(rootNode, context -> {
            throw new IllegalStateException("Throw before client response");
        });

        Assert.assertNull(tree.getUI().getSession());

        tree.beforeClientResponse(rootNode, context -> {
            throw new IllegalStateException("Throw before client response");
        });

        tree.runExecutionsBeforeClientResponse();
    }

    @Test
    public void beforeClientResponse_nodeGarbageCollectedDespiteClosure()
            throws InterruptedException {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode("node2");

        StateNodeTest.setParent(node2, node1);

        class CapturingConsumer
                implements SerializableConsumer<ExecutionContext> {
            private final Object captured;

            public CapturingConsumer(Object captured) {
                this.captured = captured;
            }

            @Override
            public void accept(ExecutionContext t) {
                // no-op
            }
        }

        tree.beforeClientResponse(node2, new CapturingConsumer(node2));

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

    @Test
    public void collectChanges_parentIsInactive_childrenAreCollected() {
        StateNode node1 = new CollectableNode();
        StateNode node2 = new CollectableNode();
        StateNode node3 = new CollectableNode();

        node1.setTree(tree);
        node2.setTree(tree);
        node3.setTree(tree);

        node1.getFeature(ElementChildrenList.class).add(0, node2);
        node2.getFeature(ElementChildrenList.class).add(0, node3);

        // cleanup the current dirty nodes
        tree.collectChanges(node -> {
        });

        node1.getFeature(ElementData.class).setVisible(false);

        List<StateNode> collectedNodes = new ArrayList<>(3);

        tree.collectChanges(change -> collectedNodes.add(change.getNode()));

        Assert.assertEquals(3, collectedNodes.size());
        Assert.assertTrue(collectedNodes.contains(node1));
        Assert.assertTrue(collectedNodes.contains(node2));
        Assert.assertTrue(collectedNodes.contains(node3));
    }

    @Test
    public void prepareForResync_nodeHasAttachAndDetachListeners_treeIsDirtyAndListenersAreCalled() {
        StateNode node1 = tree.getRootNode();
        StateNode node2 = StateNodeTest.createEmptyNode("node2");
        StateNodeTest.setParent(node2, node1);

        AtomicInteger attachCount = new AtomicInteger();
        node2.addAttachListener(attachCount::incrementAndGet);
        AtomicInteger detachCount = new AtomicInteger();
        node2.addDetachListener(detachCount::incrementAndGet);

        tree.collectChanges(c -> {
        });
        Assert.assertEquals(0, tree.collectDirtyNodes().size());
        Assert.assertTrue(node2.isClientSideInitialized());
        Assert.assertTrue(node2.isAttached());

        tree.getRootNode().prepareForResync();

        Assert.assertFalse(node2.isClientSideInitialized());
        Assert.assertTrue(node2.isAttached());
        Assert.assertEquals(1, attachCount.get());
        Assert.assertEquals(1, detachCount.get());

        Assert.assertEquals(3, tree.collectDirtyNodes().size());

        Set<StateNode> dirtyNodes = new HashSet<>(tree.collectDirtyNodes());
        Assert.assertTrue(dirtyNodes.remove(node1));
        Assert.assertTrue(dirtyNodes.remove(node2));

        StateNode remaining = dirtyNodes.iterator().next();
        // The remaining node is not in the "hierarchy" tree but is the Push
        // config node
        Assert.assertTrue(
                remaining.hasFeature(PushConfigurationParametersMap.class));

        tree.collectChanges(change -> {
        });
        Assert.assertTrue(node2.isClientSideInitialized());

        // Make sure detach listener is called when a resynced node is
        // eventually detached
        // In practice checks that node2.hasBeenAttached = true
        node2.setParent(null);
        Assert.assertEquals("Detach listener was not called on final detach", 2,
                detachCount.get());
    }

    @Test
    public void pendingJavascriptExecutionForInitiallyInvisibleNode() {
        UI ui = new UI();
        VaadinSession mockSession = Mockito.mock(VaadinSession.class);
        ui.getInternals().setSession(mockSession);
        StateTree initialTree = ui.getInternals().getStateTree();

        Element element = ElementFactory.createAnchor();
        element.setVisible(false);
        ui.getElement().appendChild(element);

        element.executeJs("js");
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertEquals(0,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        // Pending execution removed when node is detached
        element.removeFromParent();
        initialTree.collectChanges(nodeChange -> {
        });
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertFalse("Pending JS executions are not removed on detach",
                ui.getInternals().isDirty());
    }

    @Test
    public void pendingJavascriptExecutionForVisibleAndInvisibleNode() {
        UI ui = new UI();
        VaadinSession mockSession = Mockito.mock(VaadinSession.class);
        ui.getInternals().setSession(mockSession);
        StateTree initialTree = ui.getInternals().getStateTree();

        Element element = ElementFactory.createAnchor();
        ui.getElement().appendChild(element);

        // Check that execution will be dumped for visible node
        element.executeJs("js");
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        // Check that execution will not be dumped for invisible node
        element.setVisible(false);
        element.executeJs("js");
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertEquals(0,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        // Check that execution will be dumped once the visibility changes to
        // true
        element.setVisible(true);
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());
    }

    @Test
    public void pendingJavascriptExecutionForVisibleAndInvisibleParentNode() {
        UI ui = new UI();
        VaadinSession mockSession = Mockito.mock(VaadinSession.class);
        ui.getInternals().setSession(mockSession);
        StateTree initialTree = ui.getInternals().getStateTree();

        Element element = ElementFactory.createAnchor();
        Element parentElement = ElementFactory.createDiv();
        ui.getElement().appendChild(parentElement);
        parentElement.appendChild(element);

        // Check that execution will not be dumped when parent node is invisible
        parentElement.setVisible(false);
        element.executeJs("js");
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertEquals(0,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        // Check that execution will be dumped once the parent node visibility
        // changes to true
        parentElement.setVisible(true);
        initialTree.runExecutionsBeforeClientResponse();
        Assert.assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());
    }
}
