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

import com.vaadin.client.flow.StateTree;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.jre.JreJsArray;
import com.vaadin.client.flow.reactive.CountingComputation;
import com.vaadin.client.flow.reactive.Reactive;

import elemental.events.EventRemover;

@SuppressWarnings("deprecation")
public class NodeListTest {
    private NodeList list = new StateTree(null).getRootNode().getList(0);

    @Test
    public void testInitialEmpty() {
        Assert.assertEquals(0, list.length());
    }

    @Test
    public void testSplice() {
        list.splice(0, 0, JsCollections.array("1", "2", "3"));

        Assert.assertEquals(3, list.length());
        Assert.assertEquals("1", list.get(0));
        Assert.assertEquals("2", list.get(1));
        Assert.assertEquals("3", list.get(2));

        list.splice(0, 2);

        Assert.assertEquals(1, list.length());
        Assert.assertEquals("3", list.get(0));
    }

    @Test
    public void testSpliceEvents() {
        AtomicReference<ListSpliceEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = list.addSpliceListener(event -> {
            Assert.assertNull("Got unexpected event", lastEvent.get());
            lastEvent.set(event);
        });

        list.splice(0, 0, JsCollections.array("1", "2", "3"));

        ListSpliceEvent addEvent = lastEvent.get();
        Assert.assertSame(list, addEvent.getSource());
        Assert.assertEquals(0, addEvent.getIndex());
        Assert.assertEquals(0, addEvent.getRemove().length());
        Assert.assertEquals(Arrays.asList("1", "2", "3"),
                JreJsArray.asList(addEvent.getAdd()));

        lastEvent.set(null);

        list.splice(1, 2);
        ListSpliceEvent removeEvent = lastEvent.get();
        Assert.assertEquals(1, removeEvent.getIndex());
        Assert.assertEquals(0, removeEvent.getAdd().length());

        JsArray<?> removed = removeEvent.getRemove();
        Assert.assertEquals(2, removed.length());
        Assert.assertEquals("2", removed.get(0));
        Assert.assertEquals("3", removed.get(1));

        remover.remove();

        list.splice(0, 0, JsCollections.array("1", "2", "3"));
        Assert.assertSame("No new event should have been fired", removeEvent,
                lastEvent.get());
    }

    @Test
    public void testReactive() {
        CountingComputation computation = new CountingComputation(
                () -> list.length());

        Reactive.flush();

        Assert.assertEquals(1, computation.getCount());

        list.add(0, "1");

        Assert.assertEquals(1, computation.getCount());

        Reactive.flush();
        Assert.assertEquals(2, computation.getCount());

        list.get(0);

        Reactive.flush();
        Assert.assertEquals("Get should not trigger recompute", 2,
                computation.getCount());
    }

    @Test
    public void testForEach() {
        list.add(0, "foo");
        list.add(1, "bar");
        list.add(0, "baz");

        List<String> forEachList = new ArrayList<>();
        list.forEach(item -> {
            forEachList.add((String) item);
        });

        Assert.assertArrayEquals(new String[] { "baz", "foo", "bar" },
                forEachList.toArray());
    }
}
