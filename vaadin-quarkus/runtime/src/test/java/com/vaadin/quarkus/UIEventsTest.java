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
package com.vaadin.quarkus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.router.internal.BeforeLeaveHandler;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.quarkus.context.UIUnderTestContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests that the CDI event forwarding listeners are attached to the UI when
 * Flow fires the {@link UIInitEvent} through the service event bus, and that
 * they survive session serialization.
 */
@QuarkusTest
public class UIEventsTest {

    @Inject
    EventCollector eventCollector;

    private UIUnderTestContext uiUnderTestContext;

    @BeforeEach
    public void setUp() {
        uiUnderTestContext = new UIUnderTestContext();
        uiUnderTestContext.activate();
        eventCollector.getEvents().clear();
    }

    @AfterEach
    public void tearDown() {
        uiUnderTestContext.tearDownAll();
        eventCollector.getEvents().clear();
    }

    @Test
    public void uiInitEventFiredOnEventBus_uiListenersAdded_eventForwardedToCdi() {
        UI ui = uiUnderTestContext.getUi();

        fireUIInitEvent(ui);

        Object listener = getSingleNavigationListener(ui,
                AfterNavigationHandler.class);
        assertSame(listener,
                getSingleNavigationListener(ui, BeforeEnterHandler.class),
                "The same listener instance should observe all navigation events");
        assertSame(listener,
                getSingleNavigationListener(ui, BeforeLeaveHandler.class),
                "The same listener instance should observe all navigation events");

        assertEquals(1, eventCollector.getEvents().size(),
                "UI init event should have been forwarded to CDI");
        assertInstanceOf(UIInitEvent.class,
                eventCollector.getEvents().getFirst());
        assertEquals(1, eventCollector.navigationListenersOnInit,
                "UI listeners should be attached before the CDI event");

        ComponentUtil.fireEvent(ui, new PollEvent(ui, true));

        assertEquals(2, eventCollector.getEvents().size(),
                "Poll event should have been forwarded to CDI");
        assertInstanceOf(PollEvent.class, eventCollector.getEvents().get(1));
    }

    @Test
    public void uiListener_serialized_beanManagerRecoveredFromContainer()
            throws Exception {
        UI ui = uiUnderTestContext.getUi();
        fireUIInitEvent(ui);

        // The listener is attached to the UI, so it is part of the serialized
        // session. The bean manager it uses is not serializable and must be
        // looked up again from the container after deserialization.
        Serializable listener = (Serializable) getSingleNavigationListener(ui,
                AfterNavigationHandler.class);
        Object deserialized = deserialize(serialize(listener));

        eventCollector.getEvents().clear();

        @SuppressWarnings("unchecked")
        ComponentEventListener<PollEvent> pollListener = (ComponentEventListener<PollEvent>) deserialized;
        pollListener.onComponentEvent(new PollEvent(ui, true));

        assertEquals(1, eventCollector.getEvents().size(),
                "Deserialized listener should still forward events to CDI");
        assertInstanceOf(PollEvent.class,
                eventCollector.getEvents().getFirst());
    }

    /**
     * Mimics what Flow does once the UI has been created.
     * <p>
     * Since Flow 25.3 the event is fired through the service event bus and
     * {@link VaadinService#fireUIInitListeners(UI)} is deprecated for removal
     * and no longer called by Flow. The event bus is used reflectively so that
     * the test exercises the real code path also when compiled against Flow
     * 25.2, where the event bus does not exist yet.
     */
    @SuppressWarnings({ "deprecation", "removal" })
    private void fireUIInitEvent(UI ui) {
        VaadinService service = ui.getSession().getService();
        try {
            Object eventBus = VaadinService.class.getMethod("getEventBus")
                    .invoke(service);
            eventBus.getClass().getMethod("fireEvent", EventObject.class)
                    .invoke(eventBus, new UIInitEvent(ui, service));
        } catch (NoSuchMethodException e) {
            // Flow 25.2 and older
            service.fireUIInitListeners(ui);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot fire the UI init event", e);
        }
    }

    private Object getSingleNavigationListener(UI ui, Class<?> handlerType) {
        List<?> listeners = ui.getInternals().getListeners(handlerType);
        assertEquals(1, listeners.size(), "Expecting a single "
                + handlerType.getSimpleName() + " registered on the UI");
        return listeners.getFirst();
    }

    private static byte[] serialize(Serializable object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] data) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data))) {
            return in.readObject();
        }
    }

    @Singleton
    @Unremovable
    public static class EventCollector {

        private final List<EventObject> events = new ArrayList<>();

        private int navigationListenersOnInit;

        void onUIInit(@Observes UIInitEvent event) {
            events.add(event);
            navigationListenersOnInit = event.getUI().getInternals()
                    .getListeners(BeforeEnterHandler.class).size();
        }

        void onPoll(@Observes PollEvent event) {
            events.add(event);
        }

        List<EventObject> getEvents() {
            return events;
        }
    }
}
