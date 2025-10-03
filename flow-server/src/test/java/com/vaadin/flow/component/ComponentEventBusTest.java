/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.component.internal.KeyboardEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

public class ComponentEventBusTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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

    @Tag("button")
    private class TestButton extends Component implements ClickNotifier {

    }

    private void fireDomEvent(Component component, String domEvent,
            JsonNode eventData) {
        Element e = component.getElement();
        e.getNode().getFeature(ElementListenerMap.class).fireEvent(
                new com.vaadin.flow.dom.DomEvent(e, domEvent, eventData));

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
        AtomicReference<BigDecimal> dataValueInEvent = new AtomicReference<>(
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
        fireDomEvent(c, "dom-event", createMinimalEventData());
    }

    @Test
    public void mappedDomEvent_fire_missingData_shouldFail() {
        TestComponent c = new TestComponent();
        EventTracker<MappedToDomEvent> eventListener = new EventTracker<>();
        c.addListener(MappedToDomEvent.class, eventListener);

        // Missing primitive boolean data should cause event creation to fail
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            fireDomEvent(c, "dom-event", createData("event.someData", 2));
        });

        // Event should not have been called due to the failure
        eventListener.assertEventNotCalled();
    }

    @Test
    public void mappedDomEventWithElementEventData_clientReportsElement_mapsElement() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        component.getElement().appendChild(child.getElement());
        ui.add(component);

        final EventTracker<MappedToDomEventWithElementData> listener = new EventTracker<>();
        component.addListener(MappedToDomEventWithElementData.class, listener);

        fireDomEvent(component, "dom-event", createStateNodeIdData("element",
                child.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertEquals(child.getElement(),
                listener.getEvent().getElement());
    }

    @Test
    public void mappedDomEventWithComponentEventData_clientReportsTypeComponent_mapsComponent() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        component.getElement().appendChild(child.getElement());
        ui.add(component);

        final EventTracker<MappedDomEventWithComponentData> listener = new EventTracker<>();
        component.addListener(MappedDomEventWithComponentData.class, listener);

        fireDomEvent(component, "dom-event", createStateNodeIdData("component",
                child.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertEquals(child, listener.getEvent().getComponent());
    }

    @Test
    public void mappedDomEventWithComponentEventData_clientReportsConcreteComponents_mapsComponents() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        final RouterLink routerLink = new RouterLink();
        component.getElement().appendChild(child.getElement());
        child.getElement().appendChild(routerLink.getElement());
        ui.add(component);

        final EventTracker<MappedDomEventWithRouterLinkData> listener = new EventTracker<>();
        component.addListener(MappedDomEventWithRouterLinkData.class, listener);

        fireDomEvent(component, "dom-event",
                createStateNodeIdData("component",
                        child.getElement().getNode().getId(), "router.link",
                        routerLink.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertEquals(routerLink, listener.getEvent().getRouterLink());
        Assert.assertEquals(child, listener.getEvent().getComponent());
    }

    @Test
    public void mappedDomEventWithComponentEventData_clientReportsMissingComponent_mapsComponentAndNull() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        final RouterLink routerLink = new RouterLink();
        component.getElement().appendChild(child.getElement());
        child.getElement().appendChild(routerLink.getElement());
        ui.add(component);

        final EventTracker<MappedDomEventWithRouterLinkData> listener = new EventTracker<>();
        component.addListener(MappedDomEventWithRouterLinkData.class, listener);

        // null data will be used if event data is missing
        fireDomEvent(component, "dom-event", createStateNodeIdData("component",
                routerLink.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertNull(listener.getEvent().getRouterLink());
        Assert.assertEquals(routerLink, listener.getEvent().getComponent());

        listener.reset();

        fireDomEvent(component, "dom-event", createStateNodeIdData(
                "router.link", routerLink.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertEquals(routerLink, listener.getEvent().getRouterLink());
        Assert.assertNull(listener.getEvent().getComponent());

        listener.reset();
    }

    @Test
    public void mappedDomEventWithComponentEventData_clientReportsSiblingComponentToEventSource_mapsComponents() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        final RouterLink routerLink = new RouterLink();
        ui.add(component, child, routerLink);

        final EventTracker<MappedDomEventWithRouterLinkData> listener = new EventTracker<>();
        component.addListener(MappedDomEventWithRouterLinkData.class, listener);

        fireDomEvent(component, "dom-event",
                createStateNodeIdData("component",
                        component.getElement().getNode().getId(), "router.link",
                        routerLink.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertEquals(routerLink, listener.getEvent().getRouterLink());
        Assert.assertEquals(component, listener.getEvent().getComponent());
    }

    @Test
    public void mappedDomEventWithComponentEventData_clientReportsElementMissing_returnsNull() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        component.getElement().appendChild(child.getElement());
        ui.add(component);

        final EventTracker<MappedDomEventWithComponentData> listener = new EventTracker<>();
        component.addListener(MappedDomEventWithComponentData.class, listener);

        fireDomEvent(component, "dom-event",
                createStateNodeIdData("component", -1));
        listener.assertEventCalled(component, true);
        Assert.assertNull(listener.getEvent().getComponent());
    }

    @Test
    public void mappedDomEventWithComponentEventData_clientReportsMissingNodeIdReported_returnsNull() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent child = new TestComponent();
        component.getElement().appendChild(child.getElement());
        ui.add(component);

        final EventTracker<MappedDomEventWithComponentData> listener = new EventTracker<>();
        component.addListener(MappedDomEventWithComponentData.class, listener);

        fireDomEvent(component, "dom-event",
                createStateNodeIdData("component", 999999999));
        listener.assertEventCalled(component, true);
        Assert.assertNull(listener.getEvent().getComponent());
    }

    @Test
    public void mappedDomEventWithElementOrComponentEventData_clientReportsStateNodeForInvisibleComponent_returnsNull() {
        final MockUI ui = new MockUI();
        final TestComponent component = new TestComponent();
        final TestComponent invisible = new TestComponent();
        component.getElement().appendChild(invisible.getElement());
        ui.add(component);

        final EventTracker<MappedToDomEventWithElementData> listener = new EventTracker<>();
        component.addListener(MappedToDomEventWithElementData.class, listener);

        invisible.setVisible(false);

        fireDomEvent(component, "dom-event", createStateNodeIdData("element",
                invisible.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        Assert.assertNull(
                "Invisible elements/components should not be reported",
                listener.getEvent().getElement());
        listener.reset();

        final EventTracker<MappedDomEventWithComponentData> second = new EventTracker<>();
        component.addListener(MappedDomEventWithComponentData.class, second);

        fireDomEvent(component, "dom-event", createStateNodeIdData("component",
                invisible.getElement().getNode().getId()));
        listener.assertEventCalled(component, true);
        second.assertEventCalled(component, true);
        Assert.assertNull(
                "Invisible elements/components should not be reported",
                listener.getEvent().getElement());
        Assert.assertNull(
                "Invisible elements/components should not be reported",
                second.getEvent().getComponent());
    }

    private JsonNode createStateNodeIdData(String key, int value) {
        return createData(JsonConstants.MAP_STATE_NODE_EVENT_DATA + key, value);
    }

    private JsonNode createStateNodeIdData(String key, int value, String key2,
            int value2) {
        return createData(JsonConstants.MAP_STATE_NODE_EVENT_DATA + key, value,
                JsonConstants.MAP_STATE_NODE_EVENT_DATA + key2, value2);
    }

    private JsonNode createData(String key, Object value) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.set(key, JacksonCodec.encodeWithoutTypeInfo(value));
        return data;
    }

    private JsonNode createData(String key, Object value, String key2,
            Object value2) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.set(key, JacksonCodec.encodeWithoutTypeInfo(value));
        data.set(key2, JacksonCodec.encodeWithoutTypeInfo(value2));
        return data;
    }

    private JsonNode createCompleteEventData(int someData, String moreData) {
        ObjectNode data = JacksonUtils.createObjectNode();
        data.set("event.someData",
                JacksonCodec.encodeWithoutTypeInfo(someData));
        data.set("event.moreData",
                JacksonCodec.encodeWithoutTypeInfo(moreData));
        data.set("event.primitiveBoolean",
                JacksonCodec.encodeWithoutTypeInfo(false));
        data.set("event.objectBoolean",
                JacksonCodec.encodeWithoutTypeInfo(null));
        return data;
    }

    private JsonNode createMinimalEventData() {
        return createCompleteEventData(0, "");
    }

    @Test
    public void domEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        Registration remover = component.addListener(MappedToDomEvent.class,
                eventTracker);
        remover.remove();

        JsonNode eventData = createCompleteEventData(42, "1");
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

        JsonNode eventData = createCompleteEventData(42, "1");
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventCalled(component, true);
        MappedToDomEvent event = eventTracker.getEvent();
        Assert.assertEquals(42, event.getSomeData());
        Assert.assertEquals("1", event.getMoreData());
        Assert.assertFalse(event.getPrimitiveBoolean());
        Assert.assertNull(event.getObjectBoolean());
    }

    @Test
    public void domEvent_fireServerEvent() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        component.addListener(MappedToDomEvent.class, eventTracker);

        JsonNode eventData = createCompleteEventData(42, "1");
        fireDomEvent(component, "dom-event", eventData);

        eventTracker.assertEventCalled(component, true);
        MappedToDomEvent event = eventTracker.getEvent();
        Assert.assertEquals(42, event.getSomeData());
        Assert.assertEquals("1", event.getMoreData());
        Assert.assertFalse(event.getPrimitiveBoolean());
        Assert.assertNull(event.getObjectBoolean());
    }

    @Test
    public void nonDomEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<ServerEvent> eventTracker = new EventTracker<>();
        Registration remover = component.addListener(ServerEvent.class,
                eventTracker);
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
    public void domEvent_addListenerWithDomListenerConsumer() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        component.getEventBus().addListener(MappedToDomEvent.class,
                eventTracker, domRegistration -> domRegistration.debounce(200));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonDomEvent_addListenerWithDomListenerConsumer_throws() {
        TestComponent component = new TestComponent();
        EventTracker<ServerEvent> eventTracker = new EventTracker<>();
        component.getEventBus().addListener(ServerEvent.class, eventTracker,
                domRegistration -> domRegistration.debounce(200));
    }

    private int calls = 0;

    @Test
    public void domEvent_addSameListenerTwice() {
        TestComponent component = new TestComponent();

        ComponentEventListener<MappedToDomEvent> listener = e -> calls++;

        Registration reg1 = component.addListener(MappedToDomEvent.class,
                listener);
        Registration reg2 = component.addListener(MappedToDomEvent.class,
                listener);

        Assert.assertEquals(1,
                component.getEventBus().componentEventData.size());
        Assert.assertEquals(2, component.getEventBus().componentEventData
                .get(MappedToDomEvent.class).size());

        fireDomEvent(component, "dom-event", createMinimalEventData());
        Assert.assertEquals(2, calls);

        reg1.remove();
        Assert.assertEquals(1,
                component.getEventBus().componentEventData.size());
        Assert.assertEquals(1, component.getEventBus().componentEventData
                .get(MappedToDomEvent.class).size());

        fireDomEvent(component, "dom-event", createMinimalEventData());

        Assert.assertEquals(3, calls);

        reg2.remove();
        Assert.assertEquals(0,
                component.getEventBus().componentEventData.size());

        fireDomEvent(component, "dom-event", createMinimalEventData());
        Assert.assertEquals(3, calls);
    }

    @Test
    public void multipleEventsForSameDomEvent_removeListener() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        EventTracker<MappedToDomNoDataEvent> eventTracker2 = new EventTracker<>();

        Registration remover = component.addListener(MappedToDomEvent.class,
                eventTracker);
        Registration remover2 = component
                .addListener(MappedToDomNoDataEvent.class, eventTracker2);
        remover.remove();

        JsonNode eventData = createCompleteEventData(42, "1");
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

        JsonNode eventData = createCompleteEventData(42, "19");
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

        JsonNode eventData = createCompleteEventData(42, "19");
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

        Registration remover = component.addListener(MappedToDomEvent.class,
                eventTracker);
        Registration remover2 = component.addListener(MappedToDomEvent.class,
                eventTracker2);
        remover.remove();

        JsonNode eventData = createCompleteEventData(42, "19");
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

    @Test(expected = IllegalArgumentException.class)
    public void getListeners_nullEventType_throws() {
        new ComponentEventBus(new TestComponent()).getListeners(null);
    }

    @Test
    public void getListeners_eventType_listenersCollection() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        Registration remover = component.addListener(MappedToDomEvent.class,
                eventTracker);
        Collection<?> listeners = component
                .getListeners(MappedToDomEvent.class);
        Assert.assertEquals(1, listeners.size());
        remover.remove();
    }

    @Test
    public void getListeners_subclassOfEventType_listenersCollection() {
        TestComponent component = new TestComponent();
        EventTracker<KeyPressEvent> eventTracker = new EventTracker<>();
        EventTracker<KeyUpEvent> eventTracker2 = new EventTracker<>();
        Registration remover = component.addListener(KeyPressEvent.class,
                eventTracker);
        Registration remover2 = component.addListener(KeyUpEvent.class,
                eventTracker2);
        Collection<?> listeners = component.getListeners(KeyboardEvent.class);
        Assert.assertEquals(2, listeners.size());
        remover.remove();
        remover2.remove();
    }

    @Test
    public void getListeners_notExistingEventType_emptyListenersCollection() {
        TestComponent component = new TestComponent();
        EventTracker<MappedToDomEvent> eventTracker = new EventTracker<>();
        Registration remover = component.addListener(MappedToDomEvent.class,
                eventTracker);
        Collection<?> listeners = component.getListeners(ServerEvent.class);
        Assert.assertTrue(listeners.isEmpty());
        remover.remove();
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

    @Test
    public void eventUnregisterListener_insideListener() {
        TestComponent c = new TestComponent();
        c.addListener(ServerEvent.class, e -> {
            e.unregisterListener();
        });
        Assert.assertTrue(c.hasListener(ServerEvent.class));
        c.fireEvent(new ServerEvent(c, new BigDecimal(0)));
        Assert.assertFalse(c.hasListener(ServerEvent.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void eventUnregisterListener_insideListenerTwiceThrows() {
        TestComponent c = new TestComponent();
        c.addListener(ServerEvent.class, e -> {
            e.unregisterListener();
            e.unregisterListener();
        });
        c.fireEvent(new ServerEvent(c, new BigDecimal(0)));
    }

    @Test(expected = IllegalStateException.class)
    public void eventUnregisterListener_outsideListenerTwiceThrows() {
        TestComponent c = new TestComponent();
        AtomicReference<ServerEvent> storedEvent = new AtomicReference<>();
        c.addListener(ServerEvent.class, e -> {
            storedEvent.set(e);
        });
        c.fireEvent(new ServerEvent(c, new BigDecimal(0)));
        storedEvent.get().unregisterListener();
    }

    @Test // #7826
    public void addListener_eventDataExpressionsPresent_constantPoolKeyNotCreatedAfterEachExpression() {
        final TestButton button = new TestButton();
        try (MockedStatic<MessageDigestUtil> util = Mockito
                .mockStatic(MessageDigestUtil.class)) {
            util.when(() -> MessageDigestUtil.sha256(Mockito.anyString()))
                    .thenReturn(
                            new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, });
            button.addClickListener(event -> {
            });
            util.verifyNoInteractions();
        }
    }

    @Test
    public void addListener_nullListener_failFast() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("component event listener cannot be null");

        final TestButton button = new TestButton();
        button.addListener(ServerEvent.class, null);
    }
}
