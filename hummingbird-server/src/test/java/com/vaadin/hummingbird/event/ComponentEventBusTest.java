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
package com.vaadin.hummingbird.event;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.ComponentTest.TestComponent;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ComponentEventBusTest {

    private static class EventTracker<T extends ComponentEvent<?>>
            implements ComponentEventListener<T> {
        private AtomicInteger eventHandlerCalled = new AtomicInteger(0);
        private AtomicReference<T> eventObject = new AtomicReference<>(null);

        @Override
        public void onComponentEvent(T e) {
            eventHandlerCalled.incrementAndGet();
            Assert.assertNull(
                    "Event object must be explicitly set to null before firing an event",
                    eventObject.get());
            eventObject.set(e);

        }

        public int getCalls() {
            return eventHandlerCalled.get();
        }

        public T getEvent() {
            return eventObject.get();
        }

        public void reset() {
            eventObject.set(null);
            eventHandlerCalled.set(0);
        }

        public void assertEventCalled(TestComponent source,
                boolean fromClient) {
            Assert.assertEquals(1, getCalls());
            Assert.assertEquals(source, getEvent().getSource());
            Assert.assertEquals(fromClient, getEvent().isFromClient());

        }

        public void assertEventNotCalled() {
            Assert.assertEquals(0, getCalls());
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }

            @SuppressWarnings("unchecked")
            EventTracker<T> o = (EventTracker<T>) obj;
            return eventHandlerCalled.get() == o.eventHandlerCalled.get()
                    && eventObject.equals(o.eventObject);
        }
    }

    private void fireDomEvent(Component component, String domEvent,
            JsonObject eventData) {
        Element e = component.getElement();
        e.getNode().getNamespace(ElementListenersNamespace.class)
                .fireEvent(new com.vaadin.hummingbird.dom.DomEvent(e, domEvent,
                        eventData));

    }

    @Test
    public void mappedDomEvent_fire() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();

        component.addListener(MappedToDomEvent.class, eventTracker);
        component.getEventBus().fireEvent(new MappedToDomEvent(component));

        eventTracker.assertEventCalled(component, false);
        Assert.assertEquals(32, eventTracker.getEvent().getSomeData());
        Assert.assertEquals("Default constructor",
                eventTracker.getEvent().getMoreData());

        eventTracker.reset();
        component.getEventBus()
                .fireEvent(new MappedToDomEvent(component, true));

        eventTracker.assertEventCalled(component, true);
        Assert.assertEquals(12, eventTracker.getEvent().getSomeData());
        Assert.assertEquals("Two arg constructor",
                eventTracker.getEvent().getMoreData());
    }

    @Test
    public void serverEvent_fire() {
        AtomicInteger eventHandlerCalled = new AtomicInteger(0);
        AtomicReference<BigDecimal> dataValueInEvent = new AtomicReference<BigDecimal>(
                new BigDecimal(0));

        TestComponentWithServerEvent component = new TestComponentWithServerEvent();
        component.addServerEventListener(e -> {
            eventHandlerCalled.incrementAndGet();
            dataValueInEvent.set(e.getSomeValue());
        });

        component
                .fireEvent(new ServerEvent(component, new BigDecimal("12.22")));

        Assert.assertEquals(1, eventHandlerCalled.get());
        Assert.assertEquals(new BigDecimal("12.22"), dataValueInEvent.get());
    }

    @Test
    public void serverNoDataEvent_fire() {
        TestComponent c = new TestComponent();
        EventTracker<ServerNoDataEvent> eventTracker = new EventTracker<>();
        c.addListener(ServerNoDataEvent.class, eventTracker);
        c.fireEvent(new ServerNoDataEvent(c, false));
        eventTracker.assertEventCalled(c, false);
        Assert.assertFalse(eventTracker.getEvent().isFromClient());
    }

    @Test
    public void serverNoDataEvent_fire_noListeners() {
        TestComponent c = new TestComponent();
        c.fireEvent(new ServerNoDataEvent(c, false));
    }

    @Test
    public void mappedDomEvent_fire_noListeners() {
        TestComponent c = new TestComponent();
        fireDomEvent(c, "dom-event", Json.createObject());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mappedDomEvent_fire_missingData() {
        TestComponent c = new TestComponent();
        EventTracker<MappedToDomEvent> eventListener = new EventTracker<>();
        c.addListener(MappedToDomEvent.class, eventListener);
        fireDomEvent(c, "dom-event", createData("event.someData", 2));
    }

    private JsonObject createData(String key, Object value) {
        JsonObject data = Json.createObject();
        data.put(key, JsonCodec.encodeWithoutTypeInfo(value));
        return data;
    }

    private JsonObject createData(String key, Object value, String key2,
            Object value2) {
        JsonObject data = Json.createObject();
        data.put(key, JsonCodec.encodeWithoutTypeInfo(value));
        data.put(key2, JsonCodec.encodeWithoutTypeInfo(value2));
        return data;
    }

    @Test
    public void domEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        EventRegistrationHandle remover = component
                .addListener(MappedToDomEvent.class, eventTracker);
        remover.remove();

        JsonObject eventData = createData("event.someData", 42,
                "event.moreData", 1);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventNotCalled();
        assertNoListeners(component.getEventBus());
    }

    private void assertNoListeners(ComponentEventBus eventBus) {
        Assert.assertTrue(eventBus.componentEventData.isEmpty());
    }

    @Test
    public void domEvent_fireClientEvent() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        component.addListener(MappedToDomEvent.class, eventTracker);

        JsonObject eventData = createData("event.someData", 42,
                "event.moreData", 1);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventCalled(component, true);
        MappedToDomEvent event = eventTracker.getEvent();
        Assert.assertEquals(42, event.getSomeData());
        Assert.assertEquals("1", event.getMoreData());
    }

    @Test
    public void domEvent_fireServerEvent() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        component.addListener(MappedToDomEvent.class, eventTracker);

        JsonObject eventData = Json.createObject();
        eventData.put("event.someData", 42);
        eventData.put("event.moreData", 1);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventCalled(component, true);
        MappedToDomEvent event = eventTracker.getEvent();
        Assert.assertEquals(42, event.getSomeData());
        Assert.assertEquals("1", event.getMoreData());
    }

    @Test
    public void nonDomEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<ServerEvent> eventTracker = new EventTracker<>();
        EventRegistrationHandle remover = component
                .addListener(ServerEvent.class, eventTracker);
        remover.remove();

        component.fireEvent(new ServerEvent(component, new BigDecimal("12.2")));

        eventTracker.assertEventNotCalled();
        assertNoListeners(component.getEventBus());
    }

    @Test
    public void nonDomEvent_fireEvent() {
        TestComponent component = new TestComponent();
        EventTracker<ServerEvent> eventTracker = new EventTracker<>();
        component.addListener(ServerEvent.class, eventTracker);

        component.fireEvent(new ServerEvent(component, new BigDecimal("12.2")));

        eventTracker.assertEventCalled(component, false);
    }

    @Test
    public void multipleEventsForSameDomEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        EventTracker<MappedToDomNoDataEvent> eventTracker2 = new EventTracker<>();

        EventRegistrationHandle remover = component
                .addListener(MappedToDomEvent.class, eventTracker);
        EventRegistrationHandle remover2 = component
                .addListener(MappedToDomNoDataEvent.class, eventTracker2);
        remover.remove();

        JsonObject eventData = createData("event.someData", 42,
                "event.moreData", 1);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventNotCalled();
        eventTracker2.assertEventCalled(component, true);
        remover2.remove();

        assertNoListeners(component.getEventBus());
    }

    @Test
    public void multipleEventsForSameDomEvent_fireEvent() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        EventTracker<MappedToDomNoDataEvent> eventTracker2 = new EventTracker<>();

        component.addListener(MappedToDomEvent.class, eventTracker);
        component.addListener(MappedToDomNoDataEvent.class, eventTracker2);

        JsonObject eventData = createData("event.someData", 42,
                "event.moreData", 19);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventCalled(component, true);
        Assert.assertEquals("19", eventTracker.getEvent().getMoreData());
        Assert.assertEquals(42, eventTracker.getEvent().getSomeData());
        Assert.assertEquals(component, eventTracker.getEvent().getSource());

        eventTracker2.assertEventCalled(component, true);
    }

    @Test
    public void multipleListenersForSameEvent_fireEvent() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        EventTracker<MappedToDomEvent> eventTracker2 = new EventTracker<>();

        component.addListener(MappedToDomEvent.class, eventTracker);
        component.addListener(MappedToDomEvent.class, eventTracker2);

        JsonObject eventData = createData("event.someData", 42,
                "event.moreData", 19);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventCalled(component, true);
        eventTracker2.assertEventCalled(component, true);
        Assert.assertEquals("19", eventTracker.getEvent().getMoreData());
        Assert.assertEquals("19", eventTracker2.getEvent().getMoreData());
        Assert.assertEquals(42, eventTracker.getEvent().getSomeData());
        Assert.assertEquals(42, eventTracker2.getEvent().getSomeData());
        Assert.assertEquals(component, eventTracker.getEvent().getSource());
        Assert.assertEquals(component, eventTracker2.getEvent().getSource());
    }

    @Test
    public void multipleListenersForSameEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        EventTracker<MappedToDomEvent> eventTracker2 = new EventTracker<>();

        EventRegistrationHandle remover = component
                .addListener(MappedToDomEvent.class, eventTracker);
        EventRegistrationHandle remover2 = component
                .addListener(MappedToDomEvent.class, eventTracker2);
        remover.remove();

        JsonObject eventData = createData("event.someData", 42,
                "event.moreData", 19);
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventNotCalled();
        eventTracker2.assertEventCalled(component, true);
        Assert.assertEquals("19", eventTracker2.getEvent().getMoreData());
        Assert.assertEquals(42, eventTracker2.getEvent().getSomeData());
        Assert.assertEquals(component, eventTracker2.getEvent().getSource());
        remover2.remove();
        assertNoListeners(component.getEventBus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidEventConstructor_addListener() {
        TestComponent c = new TestComponent();
        c.addListener(InvalidMappedToDomEvent.class, e -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidEventDataInConstructor_addListener() {
        TestComponent c = new TestComponent();
        c.addListener(MappedToDomInvalidEventData.class, e -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void multipleEventDataConstructors_addListener() {
        TestComponent c = new TestComponent();
        c.addListener(MappedToDomEventMultipleConstructors.class, e -> {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasListeners_nullEventType_throws() {
        new ComponentEventBus(new TestComponent()).hasListener(null);
    }

    @Test
    public void testFireEvent_noListeners_eventBusNotCreated() {
        AtomicInteger eventBusCreated = new AtomicInteger();
        TestComponent c = new TestComponent() {
            @Override
            public ComponentEventBus getEventBus() {
                eventBusCreated.incrementAndGet();
                return super.getEventBus();
            }
        };
        c.fireEvent(new ServerEvent(c, new BigDecimal(0)));

        Assert.assertEquals(0, eventBusCreated.get());
    }

}
