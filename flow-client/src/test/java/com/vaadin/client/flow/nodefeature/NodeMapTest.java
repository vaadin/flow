/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.reactive.CountingComputation;
import com.vaadin.client.flow.reactive.Reactive;

import elemental.events.EventRemover;

public class NodeMapTest {

    private final NodeMap map = new NodeMap(0,
            new StateNode(0, new StateTree(null)));

    @Test
    public void testNewMapEmpty() {
        map.forEachProperty((p, n) -> Assert.fail());
    }

    @Test
    public void testPropertyCreation() {
        MapProperty property = map.getProperty("foo");
        Assert.assertEquals("foo", property.getName());
        Assert.assertSame(map, property.getMap());

        List<MapProperty> properties = collectProperties();

        Assert.assertEquals(Arrays.asList(property), properties);

        MapProperty getAgain = map.getProperty("foo");
        Assert.assertSame(property, getAgain);

        Assert.assertEquals(properties, collectProperties());
    }

    @Test
    public void testAddPropertyEvent() {
        AtomicReference<MapPropertyAddEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = map
                .addPropertyAddListener(event -> {
                    Assert.assertNull("Got unexpected event", lastEvent.get());
                    lastEvent.set(event);
                });

        Assert.assertNull(lastEvent.get());

        map.getProperty("foo");

        MapPropertyAddEvent event = lastEvent.get();

        Assert.assertSame(map, event.getSource());
        Assert.assertEquals("foo", event.getProperty().getName());

        lastEvent.set(null);
        map.getProperty("foo");

        Assert.assertNull("No new event should have fired", lastEvent.get());

        map.getProperty("bar");

        Assert.assertEquals("bar", lastEvent.get().getProperty().getName());

        remover.remove();

        map.getProperty("baz");

        Assert.assertEquals("bar", lastEvent.get().getProperty().getName());
    }

    @Test
    public void testReactiveInvalidation() {
        CountingComputation computation = new CountingComputation(
                () -> map.forEachProperty((a, b) -> {
                }));

        Reactive.flush();

        Assert.assertEquals(1, computation.getCount());

        map.getProperty("foo");

        Assert.assertEquals(1, computation.getCount());

        Reactive.flush();

        Assert.assertEquals(2, computation.getCount());
    }

    private List<MapProperty> collectProperties() {
        List<MapProperty> properties = new ArrayList<>();
        map.forEachProperty((p, n) -> properties.add(p));
        return properties;
    }

    @Test
    public void hasPropertyValueForNonExistingProperty() {
        Assert.assertFalse(map.hasPropertyValue("foo"));
        // Should not create the property
        map.forEachProperty((property, key) -> {
            Assert.fail("There should be no properties");
        });
    }

    @Test
    public void hasPropertyValueForExistingPropertyWithoutValue() {
        map.getProperty("foo");
        Assert.assertFalse(map.hasPropertyValue("foo"));
    }

    @Test
    public void hasPropertyValueForExistingPropertyWithValue() {
        map.getProperty("foo").setValue("bar");
        Assert.assertTrue(map.hasPropertyValue("foo"));
    }

    @Test
    public void hasPropertyValueAfterRemovingValue() {
        MapProperty p = map.getProperty("foo");
        p.setValue("bar");
        Assert.assertTrue(map.hasPropertyValue("foo"));
        p.removeValue();
        Assert.assertFalse(map.hasPropertyValue("foo"));
    }

}
