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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
import elemental.json.JsonObject;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

public class ElementListenersTest
        extends AbstractNodeFeatureTest<ElementListenerMap> {
    private static final DomEventListener noOp = e -> {
        // no op
    };

    private ElementListenerMap ns;

    @Before
    public void init() {
        ns = createFeature();
    }

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
        // due to fix to #5090, data3 won't be present after removal
        Assert.assertFalse(expressions.contains("data3"));
    }
    
    @Test
    public void settingsAreOnlyUpdated_should_ListenersSharingTheTypeOfRemovedListenerExist() {
        ns = spy(createFeature());
        DomEventListener del1 = event -> {};
        DomEventListener del2 = event -> {};
        DomEventListener del3 = event -> {};
        Registration handle1 = ns.add("eventType", del1).addEventData("data1");
        Registration handle2 = ns.add("eventType", del2).addEventData("data2");
        Registration handle3 = ns.add("eventTypeOther", del3)
                .addEventData("data3");
        Mockito.reset(ns);

        Set<String> expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        Assert.assertTrue(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));

        handle1.remove();

        Mockito.verify(ns, times(1)).put(eq("eventType"),
                any(Serializable.class));

        expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));

        handle2.remove();
        // updating settings does not take place a second time
        Mockito.verify(ns, times(1)).put(eq("eventType"),
                any(Serializable.class));

        expressions = getExpressions("eventType");
        expressions.addAll(getExpressions("eventTypeOther"));

        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertFalse(expressions.contains("data2"));
        Assert.assertTrue(expressions.contains("data3"));
    }

    @Test
    public void addingRemovingAndAddingListenerOfTheSameType() {
        DomEventListener del1 = event -> {};
        DomEventListener del2 = event -> {};
        Registration handle = ns.add("eventType", del1).addEventData("data1");

        Set<String> expressions = getExpressions("eventType");
        Assert.assertTrue(expressions.contains("data1"));

        handle.remove();
        expressions = getExpressions("eventType");
        Assert.assertFalse(expressions.contains("data1"));

        // re-add a listener for "eventType", using different eventData
        handle = ns.add("eventType", del2).addEventData("data2");
        expressions = getExpressions("eventType");
        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertTrue(expressions.contains("data2"));

        handle.remove();
        expressions = getExpressions("eventType");
        Assert.assertFalse(expressions.contains("data1"));
        Assert.assertFalse(expressions.contains("data2"));
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
    public void synchronizeProperty_hasSynchronizedProperty() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        Assert.assertNull(ns.getPropertySynchronizationMode("name"));

        registration.synchronizeProperty("anotherName");

        Assert.assertNull(ns.getPropertySynchronizationMode("name"));

        registration.synchronizeProperty("name");

        Assert.assertSame(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_alwaysMode() {
        DomListenerRegistration registration = ns.add("foo", noOp)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        registration.synchronizeProperty("name");

        Assert.assertSame(DisabledUpdateMode.ALWAYS,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_bothModes() {
        DomListenerRegistration registration1 = ns.add("foo", noOp)
                .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        registration1.synchronizeProperty("name");

        DomListenerRegistration registration2 = ns.add("foo", noOp);
        registration2.synchronizeProperty("name");

        Assert.assertSame(DisabledUpdateMode.ALWAYS,
                ns.getPropertySynchronizationMode("name"));
    }

    @Test
    public void synchronizeProperty_hasExpressionToken() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        Assert.assertEquals(Collections.emptySet(), getExpressions("foo"));

        registration.synchronizeProperty("name");

        Assert.assertEquals(
                Collections.singleton(
                        JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "name"),
                getExpressions("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void synchronizeProperty_nullArgument_illegalArgumentException() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        registration.synchronizeProperty(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void synchronizeProperty_emptyArgument_illegalArgumentException() {
        DomListenerRegistration registration = ns.add("foo", noOp);

        registration.synchronizeProperty("");
    }

    // Helper for accessing package private API from other tests
    public static Set<String> getExpressions(
            ElementListenerMap elementListenerMap, String eventName) {
        return new HashSet<>(elementListenerMap.getExpressions(eventName));
    }

    private Set<String> getExpressions(String name) {
        return getExpressions(ns, name);
    }

    private static DomEvent createEvent(String type) {
        return new DomEvent(new Element("fake"), type, Json.createObject());
    }
}
