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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        assertEquals(0, list.length());
    }

    @Test
    public void testSplice() {
        list.splice(0, 0, JsCollections.array("1", "2", "3"));

        assertEquals(3, list.length());
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
        assertEquals("3", list.get(2));

        list.splice(0, 2);

        assertEquals(1, list.length());
        assertEquals("3", list.get(0));
    }

    @Test
    public void testSpliceEvents() {
        AtomicReference<ListSpliceEvent> lastEvent = new AtomicReference<>();

        EventRemover remover = list.addSpliceListener(new ListSpliceListener() {
            @Override
            public void onSplice(ListSpliceEvent event) {
                assertNull("Got unexpected event", lastEvent.get());
                lastEvent.set(event);
            }
        });

        list.splice(0, 0, JsCollections.array("1", "2", "3"));

        ListSpliceEvent addEvent = lastEvent.get();
        assertSame(list, addEvent.getSource());
        assertEquals(0, addEvent.getIndex());
        assertEquals(0, addEvent.getRemove().length());
        assertEquals(Arrays.asList("1", "2", "3"),
                JreJsArray.asList(addEvent.getAdd()));

        lastEvent.set(null);

        list.splice(1, 2);
        ListSpliceEvent removeEvent = lastEvent.get();
        assertEquals(1, removeEvent.getIndex());
        assertEquals(0, removeEvent.getAdd().length());

        JsArray<?> removed = removeEvent.getRemove();
        assertEquals(2, removed.length());
        assertEquals("2", removed.get(0));
        assertEquals("3", removed.get(1));

        remover.remove();

        list.splice(0, 0, JsCollections.array("1", "2", "3"));
        assertSame("No new event should have been fired", removeEvent,
                lastEvent.get());
    }

    @Test
    public void testReactive() {
        CountingComputation computation = new CountingComputation(
                () -> list.length());

        Reactive.flush();

        assertEquals(1, computation.getCount());

        list.add(0, "1");

        assertEquals(1, computation.getCount());

        Reactive.flush();
        assertEquals(2, computation.getCount());

        list.get(0);

        Reactive.flush();
        assertEquals("Get should not trigger recompute", 2,
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

        assertArrayEquals(new String[] { "baz", "foo", "bar" },
                forEachList.toArray());
    }
}
