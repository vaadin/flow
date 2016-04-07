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
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventData;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.ComponentTest.TestComponent;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ComponentEventBusTest {

    @DomEvent("the-dom-event")
    public static class ComponentEventWithDomEvent extends ComponentEvent {

        @EventData("event.someData")
        private int someData;

        public ComponentEventWithDomEvent(Component source) {
            super(source);
        }

        public int getSomeData() {
            return someData;
        }
    }

    public static class TestComponentWithDomEvent extends TestComponent {

        public EventRegistrationHandle addEventWithDomEventListener(
                Consumer<ComponentEventWithDomEvent> listener) {
            return super.addListener(ComponentEventWithDomEvent.class,
                    listener);
        }
    }

    public static class ServerComponentEvent extends ComponentEvent {

        private BigDecimal someValue;

        public ServerComponentEvent(Component source, BigDecimal someValue) {
            super(source);
            this.someValue = someValue;
        }

        public BigDecimal getSomeValue() {
            return someValue;
        }

    }

    public static class TestComponentWithServerEvent extends TestComponent {
        public EventRegistrationHandle addServerEventListener(
                Consumer<ServerComponentEvent> listener) {
            return super.addListener(ServerComponentEvent.class, listener);
        }

        @Override
        protected void fireEvent(ComponentEvent event) {
            super.fireEvent(event);
        }
    }

    @Test
    public void eventBasedOnDomEvent() {
        AtomicInteger eventHandlerCalled = new AtomicInteger(0);
        AtomicInteger dataValueInEvent = new AtomicInteger(-1);
        TestComponentWithDomEvent component = new TestComponentWithDomEvent();
        component.addEventWithDomEventListener(e -> {
            eventHandlerCalled.incrementAndGet();
            dataValueInEvent.set(e.getSomeData());
        });

        JsonObject eventData = Json.createObject();
        eventData.put("event.someData", 42);
        component.getElement().getNode()
                .getNamespace(ElementListenersNamespace.class)
                .fireEvent(new com.vaadin.hummingbird.dom.DomEvent(
                        component.getElement(), "the-dom-event", eventData));

        Assert.assertEquals(1, eventHandlerCalled.get());
        Assert.assertEquals(42, dataValueInEvent.get());
    }

    @Test
    public void serverEvent() {
        AtomicInteger eventHandlerCalled = new AtomicInteger(0);
        AtomicReference<BigDecimal> dataValueInEvent = new AtomicReference<BigDecimal>(
                new BigDecimal(0));

        TestComponentWithServerEvent component = new TestComponentWithServerEvent();
        component.addServerEventListener(e -> {
            eventHandlerCalled.incrementAndGet();
            dataValueInEvent.set(e.getSomeValue());
        });

        component.fireEvent(
                new ServerComponentEvent(component, new BigDecimal("12.22")));

        Assert.assertEquals(1, eventHandlerCalled.get());
        Assert.assertEquals(new BigDecimal("12.22"), dataValueInEvent.get());
    }
}
