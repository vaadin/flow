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
package com.vaadin.client.hummingbird;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.hummingbird.reactive.CountingComputation;
import com.vaadin.client.hummingbird.reactive.Reactive;

import elemental.events.EventRemover;

public class MapNamespaceTest {

    private final MapNamespace namespace = new MapNamespace(0,
            new StateNode(0, new StateTree()));

    @Test
    public void testNewNamespaceEmpty() {
        namespace.forEachProperty((p, n) -> Assert.fail());
    }

    @Test
    public void testPropertyCreation() {
        MapProperty property = namespace.getProperty("foo");
        Assert.assertEquals("foo", property.getName());
        Assert.assertSame(namespace, property.getNamespace());

        List<MapProperty> properties = collectProperties();

        Assert.assertEquals(Arrays.asList(property), properties);

        MapProperty getAgain = namespace.getProperty("foo");
        Assert.assertSame(property, getAgain);

        Assert.assertEquals(properties, collectProperties());
    }

    @Test
    public void testAddPropertyEvent() {
        AtomicReference<PropertyAddEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = namespace
                .addPropertyAddListener(new PropertyAddListener() {
                    @Override
                    public void onPropertyAdd(PropertyAddEvent event) {
                        Assert.assertNull("Got unexpected event",
                                lastEvent.get());

                        lastEvent.set(event);
                    }
                });

        Assert.assertNull(lastEvent.get());

        namespace.getProperty("foo");

        PropertyAddEvent event = lastEvent.get();

        Assert.assertSame(namespace, event.getSource());
        Assert.assertEquals("foo", event.getProperty().getName());

        lastEvent.set(null);
        namespace.getProperty("foo");

        Assert.assertNull("No new event should have fired", lastEvent.get());

        namespace.getProperty("bar");

        Assert.assertEquals("bar", lastEvent.get().getProperty().getName());

        remover.remove();

        namespace.getProperty("baz");

        Assert.assertEquals("bar", lastEvent.get().getProperty().getName());
    }

    @Test
    public void testReactiveInvalidation() {
        CountingComputation computation = new CountingComputation(
                () -> namespace.forEachProperty((a, b) -> {
                }));

        Reactive.flush();

        Assert.assertEquals(1, computation.getCount());

        namespace.getProperty("foo");

        Assert.assertEquals(1, computation.getCount());

        Reactive.flush();

        Assert.assertEquals(2, computation.getCount());
    }

    private List<MapProperty> collectProperties() {
        List<MapProperty> properties = new ArrayList<>();
        namespace.forEachProperty((p, n) -> properties.add(p));
        return properties;
    }
}
