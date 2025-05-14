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
package com.vaadin.client.flow;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.client.InitialPropertiesHandler;
import com.vaadin.client.Registry;
import com.vaadin.client.communication.ServerConnector;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.events.EventRemover;

public class StateTreeTest {

    private static class TestServerConnector extends ServerConnector {

        private StateNode node;
        private String key;
        private Object value;

        public TestServerConnector(Registry registry) {
            super(registry);
        }

        @Override
        public void sendNodeSyncMessage(StateNode node, int feature, String key,
                Object value) {
            this.node = node;
            this.key = key;
            this.value = value;
        }

        void clear() {
            this.node = null;
            this.key = null;
            this.value = null;
        }

        void assertMessage(StateNode node, String key, Object value) {
            Assert.assertEquals(node, this.node);
            Assert.assertEquals(key, this.key);
            Assert.assertEquals(value, this.value);
        }
    }

    private InitialPropertiesHandler propertyHandler = Mockito
            .mock(InitialPropertiesHandler.class);

    private StateTree tree = new StateTree(new Registry() {
        {
            set(InitialPropertiesHandler.class, propertyHandler);
            set(ServerConnector.class, new TestServerConnector(this));
        }
    });
    private StateNode node = new StateNode(5, tree);

    private TestServerConnector connector = (TestServerConnector) tree
            .getRegistry().getServerConnector();

    private static class TestVisibilityTree extends StateTree {

        private boolean isVisible;

        public TestVisibilityTree() {
            super(new Registry());
        }

        @Override
        public boolean isVisible(StateNode node) {
            return isVisible;
        }
    }

    @Test
    public void testIdMappings() {

        StateNode nullNode = tree.getNode(node.getId());
        Assert.assertNull(nullNode);

        tree.registerNode(node);

        StateNode foundNode = tree.getNode(node.getId());
        Assert.assertSame(node, foundNode);
    }

    @Test(expected = AssertionError.class)
    public void testRegisterExistingThrows() {
        tree.registerNode(node);
        tree.registerNode(node);
    }

    @Test(expected = AssertionError.class)
    public void testRegisterNullThrows() {
        tree.registerNode(null);
    }

    @Test
    public void testNodeUnregister() {
        tree.registerNode(node);

        Assert.assertFalse(node.isUnregistered());

        AtomicReference<NodeUnregisterEvent> lastEvent = new AtomicReference<>();
        node.addUnregisterListener(event -> {
            Assert.assertNull("Unexpected event fired", lastEvent.get());
            lastEvent.set(event);
        });

        tree.unregisterNode(node);

        NodeUnregisterEvent event = lastEvent.get();
        Assert.assertSame(node, event.getNode());

        Assert.assertTrue(node.isUnregistered());
        Assert.assertNull(tree.getNode(node.getId()));
    }

    @Test
    public void testRemoveUnregisterListener() {
        tree.registerNode(node);

        EventRemover remove = node
                .addUnregisterListener(e -> Assert.fail("Should never run"));

        remove.remove();

        tree.unregisterNode(node);
    }

    @Test(expected = AssertionError.class)
    public void unregisterNonRegisteredNode() {
        tree.unregisterNode(node);
    }

    @Test
    public void unregisterUnregisteredNode() {
        tree.registerNode(node);
        tree.unregisterNode(node);
        // Should run fine up to this point

        try {
            tree.unregisterNode(node);
            Assert.fail("Should have thrown");
        } catch (AssertionError expected) {
            // All is fine
        }
    }

    @Test(expected = AssertionError.class)
    public void testUpdatingTree_triggeringBinder_causesAssertionError() {
        tree.registerNode(node);
        tree.setUpdateInProgress(true);
        Binder.bind(node, null);
    }

    @Test
    public void sendNodePropertySyncToServer_notInitialProperty_propertyIsSent() {
        tree.registerNode(node);
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty property = new MapProperty("foo", map);
        property.setValue("bar");
        connector.clear();

        tree.sendNodePropertySyncToServer(property);

        connector.assertMessage(node, "foo", "bar");
    }

    @Test
    public void sendNodePropertySyncToServer_nodeDetached_propertyNotIsSent() {
        tree.registerNode(node);
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty property = new MapProperty("foo", map);
        property.setValue("bar");
        connector.clear();
        tree.unregisterNode(node);

        tree.sendNodePropertySyncToServer(property);

        connector.assertMessage(null, null, null);
    }

    @Test
    public void sendNodePropertySyncToServer_initialProperty_propertyIsNoSent() {
        tree.registerNode(node);
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty property = new MapProperty("foo", map);
        property.setValue("bar");

        Mockito.when(propertyHandler.handlePropertyUpdate(property))
                .thenReturn(true);

        connector.clear();

        tree.sendNodePropertySyncToServer(property);

        connector.assertMessage(null, null, null);
    }

    @Test
    public void setUpdateInProgress_flushPropertyUpdates() {
        tree.setUpdateInProgress(true);

        Mockito.verify(propertyHandler).flushPropertyUpdates();

        tree.setUpdateInProgress(false);

        Mockito.verify(propertyHandler, Mockito.times(2))
                .flushPropertyUpdates();
    }

    @Test
    public void registerNode_updateIsNotInProgress_noPropertyHandlerCalls() {
        tree.registerNode(node);

        Mockito.verifyNoInteractions(propertyHandler);
    }

    @Test
    public void registerNode_updateIsInProgress_noPropertyHandlerCalls() {
        tree.setUpdateInProgress(true);
        tree.registerNode(node);

        Mockito.verify(propertyHandler).nodeRegistered(node);
    }

    @Test
    public void isVisible_nodeHasNoFeature_nodeIsVisible() {
        Assert.assertTrue(tree.isVisible(node));
    }

    @Test
    public void isVisible_nodeHasFeatureAndVisibleValue_nodeIsVisible() {
        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBLE).setValue(true);
        Assert.assertTrue(tree.isVisible(node));
    }

    @Test
    public void isVisible_nodeHasFeatureAndNoValue_nodeIsVisible() {
        // initialize the feature
        node.getMap(NodeFeatures.ELEMENT_DATA);
        Assert.assertTrue(tree.isVisible(node));
    }

    @Test
    public void isVisible_nodeHasFeatureAndNotVisibleValue_nodeIsNotVisible() {
        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBLE).setValue(false);
        Assert.assertFalse(tree.isVisible(node));
    }

    @Test
    public void isActive_nodeIsVisibleAndNoParent_nodeIsActive() {
        TestVisibilityTree tree = new TestVisibilityTree();

        StateNode stateNode = new StateNode(7, tree);

        tree.isVisible = true;

        Assert.assertTrue(tree.isActive(stateNode));
    }

    @Test
    public void isActive_nodeIsInvisibleAndNoParent_nodeIsActive() {
        TestVisibilityTree tree = new TestVisibilityTree();

        StateNode stateNode = new StateNode(7, tree);

        tree.isVisible = false;

        Assert.assertFalse(tree.isActive(stateNode));
    }

    @Test
    public void isActive_nodeIsVisibleAndVisibleParent_nodeIsActive() {
        TestVisibilityTree tree = new TestVisibilityTree();

        StateNode stateNode = new StateNode(7, tree);

        StateNode parent = new StateNode(8, tree);

        stateNode.setParent(parent);

        tree.isVisible = true;

        Assert.assertTrue(tree.isActive(stateNode));
    }

    @Test
    public void isActive_nodeIsVisibleAndInvisibleParent_nodeIsNotActive() {
        StateNode stateNode = new StateNode(7, tree);

        StateNode parent = new StateNode(8, tree);

        stateNode.setParent(parent);

        TestVisibilityTree tree = new TestVisibilityTree() {
            @Override
            public boolean isVisible(StateNode node) {
                return parent != node;
            }
        };

        Assert.assertFalse(tree.isActive(stateNode));
    }

    @Test
    public void treeHasChildren_prepareForResync_onlyRootRemainsRegistered() {
        tree.registerNode(node);
        tree.prepareForResync();
        Assert.assertFalse(tree.getRootNode().isUnregistered());
        Assert.assertTrue(node.isUnregistered());
    }
}
