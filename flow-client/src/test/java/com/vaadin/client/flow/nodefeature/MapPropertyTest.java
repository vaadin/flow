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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.reactive.CountingComputation;
import com.vaadin.client.flow.reactive.Reactive;

import elemental.events.EventRemover;

public class MapPropertyTest {
    private MapProperty property = new MapProperty("foo",
            new NodeMap(0, new StateNode(0, new StateTree(null))));

    @Test
    public void testValue() {
        assertNull(property.getValue());
        assertFalse(property.hasValue());

        property.setValue("bar");

        assertEquals("bar", property.getValue());
        assertTrue(property.hasValue());
    }

    @Test
    public void testChangeListener() {
        AtomicReference<MapPropertyChangeEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = property
                .addChangeListener(new MapPropertyChangeListener() {
                    @Override
                    public void onPropertyChange(MapPropertyChangeEvent event) {
                        assertNull("Got unexpected event",
                                lastEvent.get());
                        lastEvent.set(event);
                    }
                });

        property.setValue("foo");

        MapPropertyChangeEvent event = lastEvent.get();
        assertSame(property, event.getSource());
        assertNull(event.getOldValue());
        assertEquals("foo", event.getNewValue());

        property.setValue("foo");
        assertSame("No new event should have fired", event,
                lastEvent.get());

        lastEvent.set(null);
        property.removeValue();

        MapPropertyChangeEvent removeEvent = lastEvent.get();
        assertNull(removeEvent.getNewValue());

        property.removeValue();
        assertSame("No new event should have fired", removeEvent,
                lastEvent.get());

        lastEvent.set(null);
        property.setValue(null);
        MapPropertyChangeEvent addBackEvent = lastEvent.get();
        assertNull(addBackEvent.getOldValue());

        remover.remove();

        property.setValue("bar");

        assertSame("No new event should have fired", addBackEvent,
                lastEvent.get());
    }

    @Test
    public void testReactive() {
        CountingComputation computation = new CountingComputation(
                () -> property.getValue());

        Reactive.flush();

        assertEquals(1, computation.getCount());

        property.setValue("bar");
        property.setValue("baz");

        assertEquals(1, computation.getCount());

        Reactive.flush();

        assertEquals(2, computation.getCount());
    }

    @Test
    public void testHasValueReactive() {
        CountingComputation computation = new CountingComputation(
                () -> property.hasValue());

        Reactive.flush();

        property.setValue("baz");

        assertEquals(1, computation.getCount());

        Reactive.flush();

        assertEquals(2, computation.getCount());
    }

    @Test
    public void testRemoveValue() {
        property.setValue("foo");
        assertTrue(property.hasValue());

        property.removeValue();

        assertFalse(property.hasValue());
        assertNull(property.getValue());
    }

    @Test
    public void testGetIntDefaultValue() {
        assertEquals(12, property.getValueOrDefault(12));

        property.setValue(24.0); // Server side sets everything as double
        assertEquals(24, property.getValueOrDefault(12));

        property.setValue(null);
        assertEquals(12, property.getValueOrDefault(12));

        property.removeValue();
        assertEquals(12, property.getValueOrDefault(12));
    }

    @Test
    public void testGetBooleanDefaultValue() {
        assertTrue(property.getValueOrDefault(true));
        assertFalse(property.getValueOrDefault(false));

        property.setValue(true);
        assertTrue(property.getValueOrDefault(false));

        property.setValue(null);
        assertTrue(property.getValueOrDefault(true));
        assertFalse(property.getValueOrDefault(false));

        property.removeValue();
        assertTrue(property.getValueOrDefault(true));
        assertFalse(property.getValueOrDefault(false));
    }

    @Test
    public void testGetStringDefaultValue() {
        assertEquals("default", property.getValueOrDefault("default"));

        property.setValue("assigned");
        assertEquals("assigned", property.getValueOrDefault("default"));

        property.setValue(null);
        assertEquals("default", property.getValueOrDefault("default"));

        property.removeValue();
        assertEquals("default", property.getValueOrDefault("default"));
    }

}
