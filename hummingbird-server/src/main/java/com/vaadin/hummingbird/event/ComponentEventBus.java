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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;

import elemental.json.JsonValue;

/**
 * An event bus for {@link Component}s.
 * <p>
 * Handles adding and removing event listeners, and firing events of type
 * {@link ComponentEvent}.
 * <p>
 * Events can either be fired manually through
 * {@link #fireEvent(ComponentEvent)} or automatically based on a DOM event
 * (see @{@link DomEvent}). Automatically fired events must have a suitable
 * constructor, as defined in {@link DomEvent}.
 *
 * @author Vaadin
 * @since
 */
public class ComponentEventBus {

    // Package private to enable testing only

    Map<Class<? extends ComponentEvent>, EventRegistrationHandle> domEventRemovers = new HashMap<>();
    Map<Class<? extends ComponentEvent>, List<Consumer<ComponentEvent>>> allEventListeners = new HashMap<>();

    private Component component;

    /**
     * Creates an event bus for the given component.
     *
     * @param component
     *            the component which will be used as a source for all fired
     *            events
     */
    public ComponentEventBus(Component component) {
        this.component = component;
    }

    /**
     * Adds a DOM listener for the given component event if it is mapped to a
     * DOM event and the event is not yet registered.
     *
     * @param eventType
     *            the type of event
     */
    private void addDomTriggerIfNeeded(
            Class<? extends ComponentEvent> eventType) {
        boolean alreadyRegistered = allEventListeners.containsKey(eventType);
        if (alreadyRegistered) {
            return;
        }

        getDomEventType(eventType).ifPresent(e -> addDomTrigger(eventType, e));
    }

    /**
     * Adds a DOM listener of the given type for the given component event.
     * <p>
     * Assumes that no listener exists.
     *
     * @param eventType
     *            the component event type
     * @param domEventType
     *            the DOM event type
     */
    private void addDomTrigger(Class<? extends ComponentEvent> eventType,
            String domEventType) {
        assert eventType != null;
        assert !domEventRemovers.containsKey(eventType);

        if (domEventType == null || domEventType.isEmpty()) {
            throw new IllegalArgumentException(
                    "The DOM event type cannot be null or empty");
        }

        Element element = component.getElement();

        // Register DOM event handler
        LinkedHashMap<String, Class<?>> eventDataExpressions = ComponentEventBusUtil
                .getEventDataExpressions(eventType);
        String[] eventData = new String[eventDataExpressions.size()];
        eventDataExpressions.keySet().toArray(eventData);
        EventRegistrationHandle remover = element.addEventListener(domEventType,
                e -> {
                    handleDomEvent(eventType, e);
                }, eventData);
        domEventRemovers.put(eventType, remover);
    }

    /**
     * Gets the DOM event type which should be mapped to the given component
     * event type.
     *
     * @param eventType
     *            the component event type
     * @return an optional string containing the dom event name or an empty
     *         optional if no mapping is defined
     */
    private Optional<String> getDomEventType(
            Class<? extends ComponentEvent> eventType) {
        com.vaadin.annotations.DomEvent domEvent = eventType
                .getAnnotation(com.vaadin.annotations.DomEvent.class);
        if (domEvent == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(domEvent.value());
        }
    }

    /**
     * Creates a list of data objects which can be passed to the constructor
     * returned by {@link #getEventConstructor(Class)} as parameters 3+.
     *
     * @param domEvent
     *            the DOM event containing the data
     * @param eventType
     *            the component event type
     * @return a list of event data objects in the same order as defined in the
     *         component event constructor
     */
    private List<Object> createEventDataObjects(DomEvent domEvent,
            Class<? extends ComponentEvent> eventType) {
        List<Object> eventDataObjects = new ArrayList<>();

        LinkedHashMap<String, Class<?>> expressions = ComponentEventBusUtil
                .getEventDataExpressions(eventType);
        expressions.forEach((expression, type) -> {
            JsonValue jsonValue = domEvent.getEventData().get(expression);
            if (jsonValue == null) {
                throw new IllegalArgumentException(
                        "The DOM event does not contain the expected event data: "
                                + expression);
            }
            Object value = JsonCodec.decodeAs(jsonValue, type);
            eventDataObjects.add(value);
        });
        return eventDataObjects;
    }

    /**
     * Adds a listener for the given event type.
     *
     * @param eventType
     *            the event type for which to call the listener
     * @param listener
     *            the listener to call when the event occurs
     * @return an object which can be used to remove the event listener
     */
    @SuppressWarnings("unchecked")
    public <T extends ComponentEvent> EventRegistrationHandle addListener(
            Class<T> eventType, Consumer<T> listener) {
        addDomTriggerIfNeeded(eventType);

        List<Consumer<ComponentEvent>> listeners = allEventListeners
                .computeIfAbsent(eventType, t -> new LinkedList<>());
        listeners.add((Consumer<ComponentEvent>) listener);
        return () -> {
            removeListener(eventType, listener);
        };
    }

    /**
     * Removes the given listener for the given event type.
     * <p>
     * Called through the {@link EventRegistrationHandle} returned by
     * {@link #addListener(Class, Consumer)}.
     *
     * @param eventType
     *            the component event type
     * @param listener
     *            the listener to remove
     */
    private <T extends ComponentEvent> void removeListener(Class<T> eventType,
            Consumer<T> listener) {
        assert eventType != null;
        assert listener != null;

        List<Consumer<ComponentEvent>> listeners = allEventListeners
                .get(eventType);
        if (listeners == null) {
            throw new IllegalArgumentException(
                    "No listener of the given type is registered");
        }

        if (!listeners.remove(listener)) {
            throw new IllegalArgumentException(
                    "The given listener is not registered");
        }
        if (listeners.isEmpty()) {
            // No more listeners for this event type
            allEventListeners.remove(eventType);
            getDomEventType(eventType)
                    .ifPresent(e -> unregisterDomEvent(eventType, e));
        }
    }

    /**
     * Removes the DOM listener for the given event type.
     *
     * @param eventType
     *            the component event type
     * @param domEventType
     *            the DOM event type for the component event type
     */
    private void unregisterDomEvent(Class<? extends ComponentEvent> eventType,
            String domEventType) {
        assert eventType != null;
        assert domEventType != null && !domEventType.isEmpty();

        EventRegistrationHandle domEventRemover = domEventRemovers
                .remove(eventType);
        if (domEventRemover != null) {
            domEventRemover.remove();
        } else {
            throw new IllegalArgumentException(
                    "No remover found when unregistering event type "
                            + eventType.getName() + " from DOM event "
                            + domEventType);
        }
    }

    /**
     * Handles a DOM event of the given type by firing a corresponding component
     * event.
     *
     * @param eventType
     *            the component event type which should be fired
     * @param domEvent
     *            the DOM event
     */
    private void handleDomEvent(Class<? extends ComponentEvent> eventType,
            DomEvent domEvent) {
        ComponentEvent e = createEventForDomEvent(eventType, domEvent,
                component);
        fireEvent(e);
    }

    /**
     * Dispatches the event to all listeners registered for the event type.
     *
     * @param event
     *            the event to fire
     */
    public void fireEvent(ComponentEvent event) {
        List<Consumer<ComponentEvent>> listeners = allEventListeners
                .get(event.getClass());
        if (listeners == null) {
            return;
        }
        new ArrayList<>(listeners).forEach(l -> l.accept(event));
    }

    /**
     * Creates a component event object based on the given DOM event.
     *
     * @param eventType
     *            The type of component event to create
     * @param domEvent
     *            The DOM event to get data from
     * @param source
     *            The component which is the source of the event
     * @return an event object of type <code>eventType</code>
     */
    private <T extends ComponentEvent> T createEventForDomEvent(
            Class<T> eventType, DomEvent domEvent, Component source) {
        try {
            Constructor<T> c = ComponentEventBusUtil
                    .getEventConstructor(eventType);
            List<Object> eventData = createEventDataObjects(domEvent,
                    eventType);
            Object[] params = new Object[eventData.size() + 2];
            params[0] = source;
            params[1] = Boolean.TRUE; // From client
            for (int i = 0; i < eventData.size(); i++) {
                params[i + 2] = eventData.get(i);
            }
            return c.newInstance(params);
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | SecurityException e) {
            throw new IllegalArgumentException(
                    "Unable to create an event object of type "
                            + eventType.getName(),
                    e);
        }
    }
}
