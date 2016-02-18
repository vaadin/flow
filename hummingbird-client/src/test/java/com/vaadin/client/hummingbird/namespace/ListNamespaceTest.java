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
package com.vaadin.client.hummingbird.namespace;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.hummingbird.StateTree;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.reactive.CountingComputation;
import com.vaadin.client.hummingbird.reactive.Reactive;

import elemental.events.EventRemover;

@SuppressWarnings("deprecation")
public class ListNamespaceTest {
    private ListNamespace namespace = new StateTree(null).getRootNode()
            .getListNamespace(0);

    @Test
    public void testInitialEmpty() {
        Assert.assertEquals(0, namespace.length());
    }

    @Test
    public void testSplice() {
        namespace.splice(0, 0, "1", "2", "3");

        Assert.assertEquals(3, namespace.length());
        Assert.assertEquals("1", namespace.get(0));
        Assert.assertEquals("2", namespace.get(1));
        Assert.assertEquals("3", namespace.get(2));

        namespace.splice(0, 2);

        Assert.assertEquals(1, namespace.length());
        Assert.assertEquals("3", namespace.get(0));
    }

    @Test
    public void testSpliceEvents() {
        AtomicReference<ListSpliceEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = namespace
                .addSpliceListener(new ListSpliceListener() {
                    @Override
                    public void onSplice(ListSpliceEvent event) {
                        Assert.assertNull("Got unexpected event",
                                lastEvent.get());
                        lastEvent.set(event);
                    }
                });

        namespace.splice(0, 0, "1", "2", "3");

        ListSpliceEvent addEvent = lastEvent.get();
        Assert.assertSame(namespace, addEvent.getSource());
        Assert.assertEquals(0, addEvent.getIndex());
        Assert.assertEquals(0, addEvent.getRemove().length());
        Assert.assertEquals(Arrays.asList("1", "2", "3"),
                Arrays.asList(addEvent.getAdd()));

        lastEvent.set(null);

        namespace.splice(1, 2);
        ListSpliceEvent removeEvent = lastEvent.get();
        Assert.assertEquals(1, removeEvent.getIndex());
        Assert.assertEquals(0, removeEvent.getAdd().length);

        JsArray<Object> removed = removeEvent.getRemove();
        Assert.assertEquals(2, removed.length());
        Assert.assertEquals("2", removed.get(0));
        Assert.assertEquals("3", removed.get(1));

        remover.remove();

        namespace.splice(0, 0, "1", "2", "3");
        Assert.assertSame("No new event should have been fired", removeEvent,
                lastEvent.get());
    }

    @Test
    public void testReactive() {
        CountingComputation computation = new CountingComputation(
                () -> namespace.length());

        Reactive.flush();

        Assert.assertEquals(1, computation.getCount());

        namespace.splice(0, 0, "1");

        Assert.assertEquals(1, computation.getCount());

        Reactive.flush();
        Assert.assertEquals(2, computation.getCount());

        namespace.get(0);

        Reactive.flush();
        Assert.assertEquals("Get should not trigger recompute", 2,
                computation.getCount());
    }

}
