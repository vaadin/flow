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
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.annotations.EventData;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.util.ReflectTools;

import elemental.json.JsonValue;

public class ComponentEventBus {

    private static final String EVENT_CONSTRUCTOR_DEFINITION = "A DOM event constructor should take (Component, boolean) as the two first parameters, followed by any number of optional parameters marked with @"
            + EventData.class.getSimpleName();
    private static final String NO_SUITABLE_CONSTRUCTOR = "No suitable DOM event constructor found for %s. "
            + EVENT_CONSTRUCTOR_DEFINITION;

    // Package private to enable testing only

    static EventDataCache cache = new EventDataCache();

    Map<Class<? extends ComponentEvent>, EventRegistrationHandle> domEventRemovers = new HashMap<>();
    Map<Class<? extends ComponentEvent>, List<Consumer<ComponentEvent>>> allEventListeners = new HashMap<>();

    private Component component;

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
        LinkedHashMap<String, Class<?>> eventDataExpressions = findEventDataExpressions(
                eventType);
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
     * Gets a map of event data expression (for
     * {@link Element#addEventListener(String, com.vaadin.hummingbird.dom.DomEventListener, String...)}
     * ) to Java type with the same order as the parameters for the event
     * constructor (as returned by {@link #getEventConstructor(Class)}).
     *
     * @param eventType
     *            the component event type
     * @return a map of event expressions, ordered in constructor parameter
     *         order
     */
    private static LinkedHashMap<String, Class<?>> getEventDataExpressions(
            Class<? extends ComponentEvent> eventType) {
        return cache.getDataExpressions(eventType)
                .orElse(findEventDataExpressions(eventType));
    }

    private static LinkedHashMap<String, Class<?>> findEventDataExpressions(
            Class<? extends ComponentEvent> eventType) {
        LinkedHashMap<String, Class<?>> eventDataExpressions = new LinkedHashMap<>();
        Constructor<? extends ComponentEvent> c = getEventConstructor(
                eventType);
        for (int i = 2; i < c.getParameterCount(); i++) {
            Parameter p = c.getParameters()[i];
            EventData eventData = p.getAnnotation(EventData.class);
            if (eventData == null || eventData.value().isEmpty()) {
                throw new IllegalArgumentException("The constructor "
                        + c.getName() + " parameter " + p.getName()
                        + " is missing or has an empty @"
                        + EventData.class.getSimpleName() + " annotation");
            }
            eventDataExpressions.put(eventData.value(),
                    ReflectTools.convertPrimitiveType(p.getType()));
        }

        return cache.setDataExpressions(eventType, eventDataExpressions);
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

        LinkedHashMap<String, Class<?>> expressions = findEventDataExpressions(
                eventType);
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
     * Dispatches the given event to all registered listeners.
     *
     * @param event
     *            the event to send
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
            Constructor<T> c = getEventConstructor(eventType);
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

    private static <T extends ComponentEvent> Constructor<T> getEventConstructor(
            Class<T> eventType) {
        return cache.getEventConstructor(eventType)
                .orElse(findEventConstructor(eventType));
    }

    /**
     * Scans through the given event type class and tries to find a suitable
     * constructor to use for firing DOM Events.
     * <ul>
     * <li>The constructor must take Component, boolean as the two first
     * parameters
     * <li>If there is only one constructor with @{@link EventData} annotations,
     * use that
     * <li>If there is no constructor with @{@link EventData} annotations, use
     * the one which takes (Component, boolean)
     * </ul>
     *
     * @param eventType
     *            the event type
     * @return the constructor to use when creating an event from a DOM event
     * @throws IllegalArgumentException
     *             if no suitable constructor was found
     */
    @SuppressWarnings("unchecked")
    private static <T extends ComponentEvent> Constructor<T> findEventConstructor(
            Class<T> eventType) {
        List<Constructor<T>> constructors = new ArrayList<>();
        for (Constructor<?> c : eventType.getConstructors()) {
            if (isDomEventConstructor(c)) {
                constructors.add((Constructor<T>) c);
            }
        }

        if (constructors.isEmpty()) {
            throw new IllegalArgumentException(String
                    .format(NO_SUITABLE_CONSTRUCTOR, eventType.getName()));
        } else if (constructors.size() == 1) {
            // One expected, one found, all is well
            return cache.setEventConstructor(eventType, constructors.get(0));
        }

        // Multiple constructors - if there is one with @EventData, use that
        Constructor<T> moreThanTwoParamConstructor = null;
        Constructor<T> twoParamConstructor = null;
        for (Constructor<T> c : constructors) {
            if (c.getParameterCount() == 2) {
                // isDomEventConstructor should prevent multiple constructors
                // from being in the list
                assert twoParamConstructor == null;
                twoParamConstructor = c;
            } else if (c.getParameterCount() > 2) {
                if (moreThanTwoParamConstructor == null) {
                    moreThanTwoParamConstructor = c;
                } else {
                    throw new IllegalArgumentException(
                            "Multiple DOM event constructor found for "
                                    + eventType.getName() + ". "
                                    + EVENT_CONSTRUCTOR_DEFINITION);
                }
            }
        }
        if (moreThanTwoParamConstructor != null) {
            return cache.setEventConstructor(eventType,
                    moreThanTwoParamConstructor);
        } else if (twoParamConstructor != null) {
            return cache.setEventConstructor(eventType, twoParamConstructor);
        } else {
            throw new IllegalArgumentException(String
                    .format(NO_SUITABLE_CONSTRUCTOR, eventType.getName()));
        }

    }

    /**
     * Checks if the given constructor can be used when firing a
     * {@link ComponentEvent} based on a {@link DomEvent}.
     *
     * @param constructor
     *            the constructor to check
     * @return <code>true</code> if the constructor can be used,
     *         <code>false</code> otherwise
     */
    private static boolean isDomEventConstructor(Constructor<?> constructor) {
        if (constructor.getParameterCount() < 2) {
            return false;
        }
        if (!Component.class
                .isAssignableFrom(constructor.getParameterTypes()[0])) {
            return false;
        }
        if (constructor.getParameterTypes()[1] != boolean.class) {
            return false;
        }
        for (int param = 2; param < constructor.getParameterCount(); param++) {
            Parameter p = constructor.getParameters()[param];

            if (p.getAnnotation(EventData.class) == null) {
                return false;
            }
        }

        return true;
    }

}
