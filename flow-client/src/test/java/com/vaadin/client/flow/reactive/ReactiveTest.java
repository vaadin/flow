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
package com.vaadin.client.flow.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import elemental.events.EventRemover;

public class ReactiveTest {
    private AtomicInteger count = new AtomicInteger();

    @Test
    public void testFlushListenersRemoved() {
        Reactive.addFlushListener(count::incrementAndGet);
        Reactive.addFlushListener(count::incrementAndGet);

        Assert.assertEquals(0, count.get());

        Reactive.flush();

        Assert.assertEquals(2, count.get());

        Reactive.flush();

        Assert.assertEquals("Flush listeners are removed after each flush", 2,
                count.get());
    }

    @Test
    public void addFlushListenerDuringFlush() {
        Reactive.addFlushListener(
                () -> Reactive.addFlushListener(count::incrementAndGet));

        Reactive.flush();

        Assert.assertEquals(
                "Listener added during flush is run in the same flush", 1,
                count.get());

        Reactive.flush();

        Assert.assertEquals("Listener is not run again", 1, count.get());
    }

    @Test
    public void testCollectEvents() {
        TestReactiveEventRouter router = new TestReactiveEventRouter();

        EventRemover eventRemover = Reactive
                .addEventCollector(e -> count.incrementAndGet());

        Assert.assertEquals(0, count.get());

        router.invalidate();

        Assert.assertEquals("Event should trigger collector", 1, count.get());

        router.invalidate();

        Assert.assertEquals("Event should still trigger collector", 2,
                count.get());

        eventRemover.remove();

        router.invalidate();

        Assert.assertEquals("Event should no longer trigger collector", 2,
                count.get());
    }

    @Test
    public void testPostFlushListenerInvokedDuringFlush() {
        AtomicInteger invokeCount = new AtomicInteger();
        Reactive.addPostFlushListener(invokeCount::incrementAndGet);

        Assert.assertEquals(0, invokeCount.get());

        Reactive.flush();

        Assert.assertEquals(1, invokeCount.get());
    }

    @Test
    public void testPostFlushListenerRemovedAfterFlush() {
        AtomicInteger invokeCount = new AtomicInteger();
        Reactive.addPostFlushListener(invokeCount::incrementAndGet);

        Reactive.flush();
        Assert.assertEquals(1, invokeCount.get());

        Reactive.flush();
        Assert.assertEquals(1, invokeCount.get());
    }

    @Test
    public void testPostFlushListenerInvokedInAddOrder() {
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            final int iFinal = i;
            Reactive.addPostFlushListener(
                    () -> order.add(Integer.valueOf(iFinal)));
        }

        Reactive.flush();

        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(Integer.valueOf(i), order.get(i));
        }
    }

    @Test
    public void testPostFlushListenerInvokedAfterRegularFlushListener() {
        List<String> order = new ArrayList<>();

        Reactive.addPostFlushListener(() -> order.add("postFlush"));
        Reactive.addFlushListener(() -> order.add("flush"));

        Assert.assertEquals(Collections.emptyList(), order);

        Reactive.flush();

        Assert.assertEquals(Arrays.asList("flush", "postFlush"), order);
    }

    @Test
    public void testNewFlushListenerInvokedBeforeNextPostListener() {
        List<String> order = new ArrayList<>();

        Reactive.addPostFlushListener(() -> order.add("postFlush1"));
        Reactive.addPostFlushListener(
                () -> Reactive.addFlushListener(() -> order.add("flush2")));
        Reactive.addPostFlushListener(() -> order.add("postFlush2"));
        Reactive.addFlushListener(() -> order.add("flush1"));

        Reactive.flush();

        Assert.assertEquals(
                Arrays.asList("flush1", "postFlush1", "flush2", "postFlush2"),
                order);
    }

    @Test
    public void flushRunning_newFlushIsIgnored() {
        List<String> order = new ArrayList<>();

        Reactive.addPostFlushListener(() -> order.add("postFlush"));
        Reactive.addFlushListener(() -> order.add("flush"));
        Reactive.addFlushListener(() -> {
            Reactive.flush();
            order.add("flush2");
        });

        Assert.assertEquals(Collections.emptyList(), order);

        Reactive.flush();

        Assert.assertEquals(Arrays.asList("flush", "flush2", "postFlush"), order);
    }
}
