/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.Registry;
import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.reactive.CountingComputation;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;

import elemental.events.EventRemover;

public class MapPropertyTest {
    private MapProperty property = new MapProperty("foo",
            new NodeMap(0, new StateNode(0, new StateTree(null))));

    private static class TestTree extends StateTree {

        private MapProperty sentProperty;

        private boolean isActive = true;

        public TestTree() {
            super(new Registry());
        }

        @Override
        public void sendNodePropertySyncToServer(MapProperty property) {
            sentProperty = property;
        }

        @Override
        public boolean isActive(StateNode node) {
            return isActive;
        }
    }

    @Test
    public void testValue() {
        Assert.assertNull(property.getValue());
        Assert.assertFalse(property.hasValue());

        property.setValue("bar");

        Assert.assertEquals("bar", property.getValue());
        Assert.assertTrue(property.hasValue());
    }

    @Test
    public void testChangeListener() {
        AtomicReference<MapPropertyChangeEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = property.addChangeListener(event -> {
            Assert.assertNull("Got unexpected event", lastEvent.get());
            lastEvent.set(event);
        });

        property.setValue("foo");

        MapPropertyChangeEvent event = lastEvent.get();
        Assert.assertSame(property, event.getSource());
        Assert.assertNull(event.getOldValue());
        Assert.assertEquals("foo", event.getNewValue());

        property.setValue("foo");
        Assert.assertSame("No new event should have fired", event,
                lastEvent.get());

        lastEvent.set(null);
        property.removeValue();

        MapPropertyChangeEvent removeEvent = lastEvent.get();
        Assert.assertNull(removeEvent.getNewValue());

        property.removeValue();
        Assert.assertSame("No new event should have fired", removeEvent,
                lastEvent.get());

        lastEvent.set(null);
        property.setValue(null);
        MapPropertyChangeEvent addBackEvent = lastEvent.get();
        Assert.assertNull(addBackEvent.getOldValue());

        remover.remove();

        property.setValue("bar");

        Assert.assertSame("No new event should have fired", addBackEvent,
                lastEvent.get());
    }

    @Test
    public void testReactive() {
        CountingComputation computation = new CountingComputation(
                () -> property.getValue());

        Reactive.flush();

        Assert.assertEquals(1, computation.getCount());

        property.setValue("bar");
        property.setValue("baz");

        Assert.assertEquals(1, computation.getCount());

        Reactive.flush();

        Assert.assertEquals(2, computation.getCount());
    }

    @Test
    public void testHasValueReactive() {
        CountingComputation computation = new CountingComputation(
                () -> property.hasValue());

        Reactive.flush();

        property.setValue("baz");

        Assert.assertEquals(1, computation.getCount());

        Reactive.flush();

        Assert.assertEquals(2, computation.getCount());
    }

    @Test
    public void testRemoveValue() {
        property.setValue("foo");
        Assert.assertTrue(property.hasValue());

        property.removeValue();

        Assert.assertFalse(property.hasValue());
        Assert.assertNull(property.getValue());
    }

    @Test
    public void testGetIntDefaultValue() {
        Assert.assertEquals(12, property.getValueOrDefault(12));

        property.setValue(24.0); // Server side sets everything as double
        Assert.assertEquals(24, property.getValueOrDefault(12));

        property.setValue(null);
        Assert.assertEquals(12, property.getValueOrDefault(12));

        property.removeValue();
        Assert.assertEquals(12, property.getValueOrDefault(12));
    }

    @Test
    public void testGetBooleanDefaultValue() {
        Assert.assertTrue(property.getValueOrDefault(true));
        Assert.assertFalse(property.getValueOrDefault(false));

        property.setValue(true);
        Assert.assertTrue(property.getValueOrDefault(false));

        property.setValue(null);
        Assert.assertTrue(property.getValueOrDefault(true));
        Assert.assertFalse(property.getValueOrDefault(false));

        property.removeValue();
        Assert.assertTrue(property.getValueOrDefault(true));
        Assert.assertFalse(property.getValueOrDefault(false));
    }

    @Test
    public void testGetStringDefaultValue() {
        Assert.assertEquals("default", property.getValueOrDefault("default"));

        property.setValue("assigned");
        Assert.assertEquals("assigned", property.getValueOrDefault("default"));

        property.setValue(null);
        Assert.assertEquals("default", property.getValueOrDefault("default"));

        property.removeValue();
        Assert.assertEquals("default", property.getValueOrDefault("default"));
    }

    @Test
    public void syncToServer_nodeIsActive_propertyIsSent() {
        TestTree tree = new TestTree();

        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.syncToServer("bar");

        Assert.assertEquals(property, tree.sentProperty);
    }

    @Test
    public void syncToServer_nodeIsInactive_propertyIsNotSent_eventIsFiredAndFlushed() {
        TestTree tree = new TestTree();

        tree.isActive = false;

        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        AtomicReference<MapPropertyChangeEvent> event = new AtomicReference<MapPropertyChangeEvent>();
        property.addChangeListener(event::set);

        AtomicBoolean flushListener = new AtomicBoolean();
        Reactive.addFlushListener(() -> flushListener.set(true));

        property.syncToServer("bar");

        Assert.assertNull(tree.sentProperty);

        Assert.assertNotNull(event.get());

        MapPropertyChangeEvent propertyChangeEvent = event.get();

        Assert.assertNull(propertyChangeEvent.getNewValue());
        Assert.assertTrue(flushListener.get());
    }

    @Test
    public void setValue_updateFromServerIsNoCompleted_syncToServerDoesntUpdateValue() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.setValue("bar");

        property.syncToServer("baz");

        Assert.assertEquals("bar", property.getValue());
    }

    @Test
    public void setValue_updateFromServerIsApplied_syncToServerUpdatesValue() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.setValue("bar");

        Reactive.flush();

        property.syncToServer("baz");

        Assert.assertEquals("baz", property.getValue());
    }

    @Test
    public void setValue_updateFromServerIsAppliedViaSyncToServer_syncToServerUpdatesValue() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.setValue("bar");

        property.syncToServer("bar");

        property.syncToServer("baz");

        Assert.assertEquals("baz", property.getValue());
    }

    @Test
    public void removeValue_updateFromServerIsNoCompleted_syncToServerDoesntUpdateValue() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.setValue("bar");

        property.removeValue();

        property.syncToServer("baz");

        Assert.assertNull(property.getValue());
    }

    @Test
    public void removeValue_updateFromServerIsApplied_syncToServerUpdatesValue() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.setValue("bar");

        property.removeValue();

        Reactive.flush();

        property.syncToServer("baz");

        Assert.assertEquals("baz", property.getValue());
    }

    @Test
    public void removeValue_updateFromServerIsAppliedViaSyncToServer_syncToServerUpdatesValue() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(7, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.setValue("bar");

        property.removeValue();

        property.syncToServer(null);

        property.syncToServer("baz");

        Assert.assertEquals("baz", property.getValue());
    }

    @Test
    public void syncToServer_propertyHasNoValue_propertyIsSync() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(11, tree);

        MapProperty property = node.getMap(NodeFeatures.ELEMENT_PROPERTIES)
                .getProperty("foo");

        property.syncToServer(null);
        Assert.assertEquals(property, tree.sentProperty);
    }

    @Test
    public void setValue_alwaysUpdateValue_eventIsAlwaysFired() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(13, tree);

        MapProperty property = new MapProperty("foo",
                node.getMap(NodeFeatures.ELEMENT_PROPERTIES), true);

        AtomicReference<MapPropertyChangeEvent> capture = new AtomicReference<>();
        property.addChangeListener(capture::set);

        property.setValue("bar");

        Assert.assertNotNull(capture.get());
        // reset
        capture.set(null);

        // set the same value again
        property.setValue("foo");
        Assert.assertNotNull(capture.get());
    }

    @Test
    public void setValue_defaultUpdateValueStrategy_eventIsFiredOnlyOnce() {
        TestTree tree = new TestTree();
        StateNode node = new StateNode(13, tree);

        MapProperty property = new MapProperty("foo",
                node.getMap(NodeFeatures.ELEMENT_PROPERTIES), false);

        AtomicReference<MapPropertyChangeEvent> capture = new AtomicReference<>();
        property.addChangeListener(capture::set);

        property.setValue("bar");

        Assert.assertNotNull(capture.get());
        // reset
        capture.set(null);

        // set the same value again
        property.setValue("bar");
        Assert.assertNull(capture.get());
    }
}
