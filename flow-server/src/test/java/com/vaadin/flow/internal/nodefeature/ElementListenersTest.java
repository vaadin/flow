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
package com.vaadin.flow.internal.nodefeature;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ElementListenersTest
        extends AbstractNodeFeatureTest<ElementListenerMap> {
    private static final DomEventListener noOp = e -> {
        // no op
    };

    private ElementListenerMap ns = createFeature();

    @Test
    public void testAddedListenerGetsEvent() {

        AtomicInteger eventCount = new AtomicInteger();

        Registration handle = ns.add("foo",
                e -> eventCount.incrementAndGet(), new String[0]);

        Assert.assertEquals(0, eventCount.get());

        ns.fireEvent(createEvent("foo"));

        Assert.assertEquals(1, eventCount.get());

        handle.remove();

        ns.fireEvent(createEvent("foo"));

        Assert.assertEquals(1, eventCount.get());
    }

    private static DomEvent createEvent(String type) {
        return new DomEvent(new Element("fake"), type, Json.createObject());
    }

    @Test
    public void testEventNameInClientData() {
        Assert.assertFalse(ns.contains("foo"));

        Registration handle = ns.add("foo", noOp, new String[0]);

        Assert.assertEquals(0, getExpressions("foo").size());

        handle.remove();

        Assert.assertFalse(ns.contains("foo"));
    }

    @Test
    public void testAddAndRemoveEventData() {
        ns.add("eventType", noOp, new String[] { "data1", "data2" });

        Set<String> expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertFalse(expressions.contains("data3"));

        Registration handle = ns.add("eventType",
                new DomEventListener() {
                    /*
                     * Can't use the existing noOp instance since there would
                     * then not be any listeners left after calling remove()
                     */

                    @Override
                    public void handleEvent(DomEvent event) {
                        // no op
                    }
                }, new String[] { "data3" });

        expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));

        handle.remove();

        expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        // data3 might still be there, but we don't care
    }

    @Test
    public void testEventDataInEvent() {
        AtomicReference<JsonObject> eventDataReference = new AtomicReference<>();
        ns.add("foo", e -> {
            Assert.assertNull(eventDataReference.get());
            eventDataReference.set(e.getEventData());
        }, new String[0]);

        Assert.assertNull(eventDataReference.get());

        JsonObject eventData = Json.createObject();
        eventData.put("baz", true);
        ns.fireEvent(new DomEvent(new Element("element"), "foo", eventData));

        JsonObject capturedJson = eventDataReference.get();
        Assert.assertNotNull(capturedJson);

        Assert.assertEquals(1, capturedJson.keys().length);
        Assert.assertEquals("true", capturedJson.get("baz").toJson());
    }

    private Set<String> getExpressions(String name) {
        return ns.getExpressions(name);
    }
}
