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
package com.vaadin.hummingbird.namespace;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;

public class ElementListenersNamespaceTest
        extends AbstractNamespaceTest<ElementListenersNamespace> {
    private static final DomEventListener noOp = () -> {
        // no op
    };

    private ElementListenersNamespace ns = createNamespace();

    @Test
    public void testAddedListenerGetsEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        EventRegistrationHandle handle = ns.add("foo",
                eventCount::incrementAndGet);

        Assert.assertEquals(0, eventCount.get());

        ns.fireEvent("foo");

        Assert.assertEquals(1, eventCount.get());

        handle.remove();

        ns.fireEvent("foo");

        Assert.assertEquals(1, eventCount.get());
    }

    @Test
    public void testEventNameInClientData() {
        Assert.assertFalse(ns.contains("foo"));

        EventRegistrationHandle handle = ns.add("foo", noOp);

        Assert.assertTrue(ns.contains("foo"));

        handle.remove();

        Assert.assertFalse(ns.contains("foo"));
    }
}
