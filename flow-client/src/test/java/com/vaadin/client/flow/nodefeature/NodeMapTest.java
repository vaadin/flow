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
package com.vaadin.client.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        map.forEachProperty((p, n) -> fail());
    }

    @Test
    public void testPropertyCreation() {
        MapProperty property = map.getProperty("foo");
        assertEquals("foo", property.getName());
        assertSame(map, property.getMap());

        List<MapProperty> properties = collectProperties();

        assertEquals(Arrays.asList(property), properties);

        MapProperty getAgain = map.getProperty("foo");
        assertSame(property, getAgain);

        assertEquals(properties, collectProperties());
    }

    @Test
    public void testAddPropertyEvent() {
        AtomicReference<MapPropertyAddEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = map
                .addPropertyAddListener(new MapPropertyAddListener() {
                    @Override
                    public void onPropertyAdd(MapPropertyAddEvent event) {
                        assertNull("Got unexpected event",
                                lastEvent.get());

                        lastEvent.set(event);
                    }
                });

        assertNull(lastEvent.get());

        map.getProperty("foo");

        MapPropertyAddEvent event = lastEvent.get();

        assertSame(map, event.getSource());
        assertEquals("foo", event.getProperty().getName());

        lastEvent.set(null);
        map.getProperty("foo");

        assertNull("No new event should have fired", lastEvent.get());

        map.getProperty("bar");

        assertEquals("bar", lastEvent.get().getProperty().getName());

        remover.remove();

        map.getProperty("baz");

        assertEquals("bar", lastEvent.get().getProperty().getName());
    }

    @Test
    public void testReactiveInvalidation() {
        CountingComputation computation = new CountingComputation(
                () -> map.forEachProperty((a, b) -> {
                }));

        Reactive.flush();

        assertEquals(1, computation.getCount());

        map.getProperty("foo");

        assertEquals(1, computation.getCount());

        Reactive.flush();

        assertEquals(2, computation.getCount());
    }

    private List<MapProperty> collectProperties() {
        List<MapProperty> properties = new ArrayList<>();
        map.forEachProperty((p, n) -> properties.add(p));
        return properties;
    }

    @Test
    public void hasPropertyValueForNonExistingProperty() {
        assertFalse(map.hasPropertyValue("foo"));
        // Should not create the property
        map.forEachProperty((property, key) -> {
            fail("There should be no properties");
        });
    }

    @Test
    public void hasPropertyValueForExistingPropertyWithoutValue() {
        map.getProperty("foo");
        assertFalse(map.hasPropertyValue("foo"));
    }

    @Test
    public void hasPropertyValueForExistingPropertyWithValue() {
        map.getProperty("foo").setValue("bar");
        assertTrue(map.hasPropertyValue("foo"));
    }

    @Test
    public void hasPropertyValueAfterRemovingValue() {
        MapProperty p = map.getProperty("foo");
        p.setValue("bar");
        assertTrue(map.hasPropertyValue("foo"));
        p.removeValue();
        assertFalse(map.hasPropertyValue("foo"));
    }

}
