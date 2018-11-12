/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.JsonConstants;
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
    public void addedListenerGetsEvent() {

        AtomicInteger eventCount = new AtomicInteger();

        Registration handle = ns.add("foo", e -> eventCount.incrementAndGet());

        Assert.assertEquals(0, eventCount.get());

        ns.fireEvent(createEvent("foo"));

        Assert.assertEquals(1, eventCount.get());

        handle.remove();

        ns.fireEvent(createEvent("foo"));

        Assert.assertEquals(1, eventCount.get());
    }

    @Test
    public void eventNameInClientData() {
        Assert.assertFalse(ns.contains("foo"));

        Registration handle = ns.add("foo", noOp);

        Assert.assertEquals(0, getExpressions("foo").size());

        handle.remove();

        Assert.assertFalse(ns.contains("foo"));
    }

    @Test
    public void addAndRemoveEventData() {
        ns.add("eventType", noOp).addEventData("data1").addEventData("data2");

        Set<String> expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertFalse(expressions.contains("data3"));

        Registration handle = ns.add("eventType", new DomEventListener() {
            /*
             * Can't use the existing noOp instance since there would then not
             * be any listeners left after calling remove()
             */

            @Override
            public void handleEvent(DomEvent event) {
                // no op
            }
        }).addEventData("data3");

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
    public void eventDataInEvent() {
        AtomicReference<JsonObject> eventDataReference = new AtomicReference<>();
        ns.add("foo", e -> {
            Assert.assertNull(eventDataReference.get());
            eventDataReference.set(e.getEventData());
        });

        Assert.assertNull(eventDataReference.get());

        JsonObject eventData = Json.createObject();
        eventData.put("baz", true);
        ns.fireEvent(new DomEvent(new Element("element"), "foo", eventData));

        JsonObject capturedJson = eventDataReference.get();
        Assert.assertNotNull(capturedJson);

        Assert.assertEquals(1, capturedJson.keys().length);
        Assert.assertEquals("true", capturedJson.get("baz").toJson());
    }

    @Test
    public void disabledElement_listenerDoesntReceiveEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet());

        Assert.assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");
        event.getSource().setEnabled(false);
        ns.fireEvent(event);
        Assert.assertEquals(0, eventCount.get());
    }

    @Test
    public void implicitlyDisabledElement_listenerDoesntReceiveEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet());

        Assert.assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");

        Element parent = new Element("parent");
        parent.appendChild(event.getSource());
        parent.setEnabled(false);

        ns.fireEvent(event);
        Assert.assertEquals(0, eventCount.get());
    }

    @Test
    public void disabledElement_listenerWithAlwaysUpdateModeReceivesEvent() {
        AtomicInteger eventCount = new AtomicInteger();

        ns.add("foo", e -> eventCount.incrementAndGet())
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        Assert.assertEquals(0, eventCount.get());
        DomEvent event = createEvent("foo");
        event.getSource().setEnabled(false);
        ns.fireEvent(event);
        Assert.assertEquals(1, eventCount.get());
    }

    @Test
    public void serializable() {
        ns.add("click", noOp).addEventData("eventdata");

        ElementListenerMap roundtrip = SerializationUtils.roundtrip(ns);

        Set<String> expressions = roundtrip.getExpressions("click");
        Assert.assertEquals(Collections.singleton("eventdata"), expressions);
    }

    @Test
    public void synchronized_hasExpressionToken() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        Assert.assertEquals(Collections.emptySet(), getExpressions("foo"));

        registration.synchronizeProperties();

        Assert.assertEquals(
                Collections.singleton(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN),
                getExpressions("foo"));
    }

    // Helper for accessing package private API from other tests
    public static Set<String> getExpressions(
            ElementListenerMap elementListenerMap, String eventName) {
        return elementListenerMap.getExpressions(eventName);
    }

    private Set<String> getExpressions(String name) {
        return getExpressions(ns, name);
    }

    private static DomEvent createEvent(String type) {
        return new DomEvent(new Element("fake"), type, Json.createObject());
    }
}
