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
package com.vaadin.flow.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.shared.Registration;

import elemental.json.Json;
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
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentEventBus implements Serializable {

    /**
     * Pairs a component-level listener for its DOM listener registration, if
     * the event-type is annotated with {@link DomEvent}.
     */
    private static class ListenerWrapper<T extends ComponentEvent<?>>
            implements Serializable {
        private ComponentEventListener<T> listener;
        private DomListenerRegistration domRegistration;

        public ListenerWrapper(ComponentEventListener<T> listener) {
            this.listener = listener;
        }

    }

    // Package private to enable testing only
    HashMap<Class<? extends ComponentEvent<?>>, ArrayList<ListenerWrapper<?>>> componentEventData = new HashMap<>(
            2);

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
     * Adds a listener for the given event type.
     *
     * @param <T>
     *            the event type
     * @param eventType
     *            the event type for which to call the listener
     * @param listener
     *            the listener to call when the event occurs
     * @return an object which can be used to remove the event listener
     */
    public <T extends ComponentEvent<?>> Registration addListener(
            Class<T> eventType, ComponentEventListener<T> listener) {
        return addListenerInternal(eventType, listener, null);
    }

    /**
     * Adds a listener for the given event type, and customizes the
     * corresponding DOM event listener with the given consumer. This allows
     * overriding eg. the debounce settings defined in the {@link DomEvent}
     * annotation.
     * <p>
     * Note that customizing the DOM event listener works only for event types
     * which are annotated with {@link DomEvent}. Use
     * {@link #addListener(Class, ComponentEventListener)} for other listeners,
     * or if you don't need to customize the DOM listener.
     *
     * @param <T>
     *            the event type
     * @param eventType
     *            the event type for which to call the listener, must be
     *            annotated with {@link DomEvent}
     * @param listener
     *            the listener to call when the event occurs
     * @param domListenerConsumer
     *            a consumer to customize the behavior of the DOM event
     *            listener, not {@code null}
     * @return an object which can be used to remove the event listener
     * @throws IllegalArgumentException
     *             if the event type is not annotated with {@link DomEvent}
     */
    public <T extends ComponentEvent<?>> Registration addListener(
            Class<T> eventType, ComponentEventListener<T> listener,
            Consumer<DomListenerRegistration> domListenerConsumer) {
        Objects.requireNonNull(domListenerConsumer,
                "DOM listener consumer cannot be null");
        return addListenerInternal(eventType, listener, domListenerConsumer);
    }

    private <T extends ComponentEvent<?>> Registration addListenerInternal(
            Class<T> eventType, ComponentEventListener<T> listener,
            Consumer<DomListenerRegistration> domListenerConsumer) {

        ListenerWrapper<T> wrapper = new ListenerWrapper<>(listener);

        boolean isDomEvent = addDomTriggerIfNeeded(eventType, wrapper);

        if (domListenerConsumer != null) {
            if (!isDomEvent) {
                throw new IllegalArgumentException(String.format(
                        "DomListenerConsumer can be used only for DOM events. The given event type %s is not annotated with %s.",
                        eventType.getSimpleName(),
                        DomEvent.class.getSimpleName()));
            }
            domListenerConsumer.accept(wrapper.domRegistration);
        }

        componentEventData.computeIfAbsent(eventType, t -> new ArrayList<>(1))
                .add(wrapper);

        return () -> removeListener(eventType, wrapper);
    }

    /**
     * Checks if there is at least one listener registered for the given event
     * type.
     *
     * @param eventType
     *            the component event type
     * @return <code>true</code> if at least one listener is registered,
     *         <code>false</code> otherwise
     */
    @SuppressWarnings("rawtypes")
    public boolean hasListener(Class<? extends ComponentEvent> eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        return componentEventData.containsKey(eventType);
    }

    /**
     * Dispatches the event to all listeners registered for the event type.
     *
     * @param event
     *            the event to fire
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void fireEvent(ComponentEvent event) {
        Class<? extends ComponentEvent> eventType = event.getClass();
        if (!hasListener(eventType)) {
            return;
        }

        // Copy the list to avoid ConcurrentModificationException
        for (ListenerWrapper wrapper : new ArrayList<>(
                componentEventData.get(event.getClass()))) {
            fireEventForListener(event, wrapper);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ComponentEvent<?>> void fireEventForListener(T event,
            ListenerWrapper<T> wrapper) {
        Class<T> eventType = (Class<T>) event.getClass();
        event.setUnregisterListenerCommand(() -> {
            removeListener(eventType, wrapper);
        });
        wrapper.listener.onComponentEvent(event);
        event.setUnregisterListenerCommand(null);
    }

    /**
     * Adds a DOM listener for the given component event if it is mapped to a
     * DOM event.
     *
     * @param eventType
     *            the type of event
     * @param wrapper
     *            the listener that is being registered
     * @return {@code true} if a DOM-trigger was added (the event is annotated
     *         with {@link DomEvent}), {@code false} otherwise.
     */
    private <T extends ComponentEvent<?>> boolean addDomTriggerIfNeeded(
            Class<T> eventType, ListenerWrapper<T> wrapper) {
        return AnnotationReader
                .getAnnotationFor(eventType,
                        com.vaadin.flow.component.DomEvent.class)
                .map(annotation -> {
                    addDomTrigger(eventType, annotation, wrapper);
                    return true;
                }).orElse(false);
    }

    /**
     * Adds a DOM listener of the given type for the given component event and
     * annotation.
     *
     * @param eventType
     *            the component event type
     * @param annotation
     *            annotation with event configuration
     * @param wrapper
     *            the listener that is being registered
     */
    private <T extends ComponentEvent<?>> void addDomTrigger(Class<T> eventType,
            com.vaadin.flow.component.DomEvent annotation,
            ListenerWrapper<T> wrapper) {
        assert eventType != null;
        assert annotation != null;

        String domEventType = annotation.value();
        DisabledUpdateMode mode = annotation.allowUpdates();
        String filter = annotation.filter();
        DebounceSettings debounce = annotation.debounce();
        int debounceTimeout = debounce.timeout();

        if (domEventType == null || domEventType.isEmpty()) {
            throw new IllegalArgumentException(
                    "The DOM event type cannot be null or empty");
        }

        Element element = component.getElement();

        // Register DOM event handler
        DomListenerRegistration registration = element.addEventListener(
                domEventType,
                event -> handleDomEvent(eventType, event, wrapper));

        wrapper.domRegistration = registration;

        registration.setDisabledUpdateMode(mode);

        LinkedHashMap<String, Class<?>> eventDataExpressions = ComponentEventBusUtil
                .getEventDataExpressions(eventType);
        eventDataExpressions.keySet().forEach(registration::addEventData);

        if (!"".equals(filter)) {
            registration.setFilter(filter);
        }

        if (debounceTimeout != 0) {
            DebouncePhase[] phases = debounce.phases();
            if (phases.length == 0) {
                throw new IllegalStateException(
                        "There must be at least one debounce phase");
            }

            DebouncePhase[] rest = new DebouncePhase[phases.length - 1];
            System.arraycopy(phases, 1, rest, 0, rest.length);

            registration.debounce(debounceTimeout, phases[0], rest);
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
            Class<? extends ComponentEvent<?>> eventType) {
        List<Object> eventDataObjects = new ArrayList<>();

        LinkedHashMap<String, Class<?>> expressions = ComponentEventBusUtil
                .getEventDataExpressions(eventType);
        expressions.forEach((expression, type) -> {
            JsonValue jsonValue = domEvent.getEventData().get(expression);
            if (jsonValue == null) {
                jsonValue = Json.createNull();
            }
            Object value = JsonCodec.decodeAs(jsonValue, type);
            eventDataObjects.add(value);
        });
        return eventDataObjects;
    }

    /**
     * Removes the given listener for the given event type.
     * <p>
     * Called through the {@link Registration} returned by
     * {@link #addListener(Class, ComponentEventListener)}.
     *
     * @param eventType
     *            the component event type
     * @param wrapper
     *            the listener to remove
     */
    private <T extends ComponentEvent<?>> void removeListener(
            Class<T> eventType, ListenerWrapper<T> wrapper) {
        assert eventType != null;
        assert wrapper != null;
        assert wrapper.listener != null;

        ArrayList<ListenerWrapper<?>> eventData = componentEventData
                .get(eventType);
        if (eventData == null) {
            throw new IllegalArgumentException(
                    "No listener of the given type is registered");
        }

        if (!eventData.remove(wrapper)) {
            throw new IllegalArgumentException(
                    "The given listener is not registered");
        }

        if (wrapper.domRegistration != null) {
            wrapper.domRegistration.remove();
        }

        if (eventData.isEmpty()) {
            componentEventData.remove(eventType);
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
     * @param wrapper
     *            the component event listener to call when the DOM event is
     *            fired
     */
    private <T extends ComponentEvent<?>> void handleDomEvent(
            Class<T> eventType, DomEvent domEvent, ListenerWrapper<T> wrapper) {
        T event = createEventForDomEvent(eventType, domEvent, component);
        fireEventForListener(event, wrapper);
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
    private <T extends ComponentEvent<?>> T createEventForDomEvent(
            Class<T> eventType, DomEvent domEvent, Component source) {
        try {
            Constructor<T> c = ComponentEventBusUtil
                    .getEventConstructor(eventType);
            // Make sure that the source component type is ok
            if (!c.getParameterTypes()[0].isAssignableFrom(source.getClass())) {
                Class<?> definedSourceType = c.getParameterTypes()[0];
                throw new IllegalArgumentException(String.format(
                        "The event type %s define the source type to be %s, which is not compatible with the used source of type %s",
                        eventType.getName(), definedSourceType.getName(),
                        source.getClass().getName()));
            }

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
