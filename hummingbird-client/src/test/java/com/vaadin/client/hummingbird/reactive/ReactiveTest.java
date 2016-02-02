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
package com.vaadin.client.hummingbird.reactive;

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
}
