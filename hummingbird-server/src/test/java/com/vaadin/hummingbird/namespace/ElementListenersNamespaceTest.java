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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.util.JsonStream;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class ElementListenersNamespaceTest
        extends AbstractNamespaceTest<ElementListenersNamespace> {
    private static final DomEventListener noOp = e -> {
        // no op
    };

    private ElementListenersNamespace ns = createNamespace();

    @Test
    public void testAddedListenerGetsEvent() {

        AtomicInteger eventCount = new AtomicInteger();

        EventRegistrationHandle handle = ns.add("foo",
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

        EventRegistrationHandle handle = ns.add("foo", noOp, new String[0]);

        Assert.assertEquals(0, getExpressions("foo").size());

        handle.remove();

        Assert.assertFalse(ns.contains("foo"));
    }

    @Test
    public void testAddAndRemoveEventData() {
        ns.add("foo", noOp, new String[] { "foo", "bar" });

        Set<String> expressions = getExpressions("foo");
        Assert.assertTrue(expressions.contains("foo"));
        Assert.assertTrue(expressions.contains("bar"));
        Assert.assertFalse(expressions.contains("baz"));

        EventRegistrationHandle handle = ns.add("foo", new DomEventListener() {
            @Override
            public void handleEvent(DomEvent event) {
                // no op here as well, but a distinct instance
            }
        }, new String[] { "baz" });

        expressions = getExpressions("foo");
        Assert.assertTrue(expressions.contains("foo"));
        Assert.assertTrue(expressions.contains("bar"));
        Assert.assertTrue(expressions.contains("baz"));

        handle.remove();

        expressions = getExpressions("foo");
        Assert.assertTrue(expressions.contains("foo"));
        Assert.assertTrue(expressions.contains("bar"));
        // baz might still be there, but we don't care
    }

    @Test
    public void testEventDataInEvent() {
        AtomicReference<JsonObject> eventDataReference = new AtomicReference<>();
        ns.add("foo", e -> {
            Assert.assertNull(eventDataReference.get());
            eventDataReference.set(e.getEventData());
        } , new String[0]);

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
        JsonArray json = (JsonArray) ns.get(name);
        if (json == null) {
            return null;
        }
        return JsonStream.stream(json).map(JsonValue::asString)
                .collect(Collectors.toSet());
    }
}
