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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.vaadin.annotations.EventData;
import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;

public class ComponentEventBus {

    // FIXME Initially null
    private Map<String, EventRegistrationHandle> domEventRemovers = new HashMap<>();
    private Map<String, List<Class<? extends ComponentEvent>>> domEventToComponentEventTypes = new HashMap<>();
    private Map<Class<? extends ComponentEvent>, List<Consumer<ComponentEvent>>> allEventListeners = new HashMap<>();
    private Component component;

    public ComponentEventBus(Component component) {
        this.component = component;
    }

    // TODO Support other elements
    private void addDomTriggerIfNeeded(
            Class<? extends ComponentEvent> eventType) {
        String domEventType = getDomEventType(eventType);
        if (domEventType == null) {
            // Server side only event, not connected to a DOM event
            return;
        }
        String[] eventDataExpressions = getEventDataExpressions(eventType);
        Element element = component.getElement();
        EventRegistrationHandle remover = element.addEventListener(domEventType,
                this::handleDomEvent, eventDataExpressions);
        domEventRemovers.put(domEventType, remover);

        List<Class<? extends ComponentEvent>> forwardedEvents = domEventToComponentEventTypes
                .computeIfAbsent(domEventType, t -> new LinkedList<>());
        if (!forwardedEvents.contains(eventType)) {
            forwardedEvents.add(eventType);
        } else {
            throw new IllegalStateException("Event " + domEventType
                    + " is already forwarded to " + eventType.getSimpleName());
        }
    }

    private String getDomEventType(Class<? extends ComponentEvent> eventType) {
        com.vaadin.annotations.DomEvent domEvent = eventType
                .getAnnotation(com.vaadin.annotations.DomEvent.class);
        if (domEvent != null) {
            return domEvent.value();
        } else {
            return null;
        }
    }

    private static String[] getEventDataExpressions(
            Class<? extends ComponentEvent> eventType) {
        Collection<String> expressions = getEventDataFields(eventType).keySet();
        return expressions.toArray(new String[expressions.size()]);
    }

    public static Map<String, Field> getEventDataFields(
            Class<? extends ComponentEvent> eventType) {
        Map<String, Field> eventData = new HashMap<>();
        // TODO Cache
        for (Field f : eventType.getDeclaredFields()) {
            EventData data = f.getAnnotation(EventData.class);
            if (data != null) {
                eventData.put(data.value(), f);
            }
        }
        return eventData;
    }

    @SuppressWarnings("unchecked")
    public <T extends ComponentEvent> void addListener(Class<T> eventType,
            Consumer<T> listener) {
        addDomTriggerIfNeeded(eventType);

        List<Consumer<ComponentEvent>> listeners = allEventListeners
                .computeIfAbsent(eventType, t -> new LinkedList<>());
        listeners.add((Consumer<ComponentEvent>) listener);
    }

    public <T extends ComponentEvent> void removeListener(Class<T> eventType,
            Consumer<T> listener) {
        List<Consumer<ComponentEvent>> listeners = allEventListeners
                .get(eventType);
        if (allEventListeners == null) {
            throw new IllegalArgumentException(
                    "The given listener is not registered");
        }

        if (!listeners.remove(listener)) {
            throw new IllegalArgumentException(
                    "The given listener is not registered");
        }
        if (listeners.isEmpty()) {
            unregistedComponentEventType(eventType);
        }
    }

    private void unregistedComponentEventType(
            Class<? extends ComponentEvent> eventType) {
        allEventListeners.remove(eventType);
        String domEventType = getDomEventType(eventType);
        if (domEventType != null) {
            List<Class<? extends ComponentEvent>> componentTypesForDomEvent = domEventToComponentEventTypes
                    .get(domEventType);
            boolean removed = componentTypesForDomEvent.remove(eventType);
            if (!removed) {
                getLogger().warning("Event type " + eventType.getName()
                        + " when unregistering from dom event to component event mapping");
            }
            if (componentTypesForDomEvent.isEmpty()) {
                componentTypesForDomEvent.remove(eventType);
                EventRegistrationHandle domEventRemover = domEventRemovers
                        .remove(domEventType);
                if (domEventRemover != null) {
                    domEventRemover.remove();
                } else {
                    getLogger().warning("Event type " + eventType.getName()
                            + " when unregistering from dom event to component event mapping");
                }
            }
        }

    }

    public void fireEvent(ComponentEvent event) {
        List<Consumer<ComponentEvent>> listeners = allEventListeners
                .get(event.getClass());
        new ArrayList<>(listeners).forEach(l -> l.accept(event));
    }

    private void handleDomEvent(DomEvent domEvent) {
        List<Class<? extends ComponentEvent>> mappedEventTypes = domEventToComponentEventTypes
                .get(domEvent.getType());
        if (mappedEventTypes == null) {
            getLogger().warning("Received an " + domEvent.getType()
                    + " DOM event but no mapping is defined for it");
            return;
        }

        mappedEventTypes.forEach(componentEventType -> {
            ComponentEvent e = createEvent(componentEventType, domEvent);
            fireEvent(e);
        });

    }

    private ComponentEvent createEvent(
            Class<? extends ComponentEvent> mappedEventType,
            DomEvent domEvent) {
        ComponentEvent event = createEventInstance(mappedEventType, component);
        event.initFromDomEvent(domEvent);
        return event;
    }

    private <T extends ComponentEvent> T createEventInstance(Class<T> eventType,
            Component source) {
        try {
            return (T) eventType.getConstructors()[0].newInstance(source);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | SecurityException e) {
            // FIXME
            throw new RuntimeException(e);
        }

    }

    private static final Logger getLogger() {
        return Logger.getLogger(ComponentEventBus.class.getName());
    }

}
