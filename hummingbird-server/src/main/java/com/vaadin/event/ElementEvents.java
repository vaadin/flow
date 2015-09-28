package com.vaadin.event;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import com.vaadin.annotations.EventParameter;
import com.vaadin.annotations.EventType;
import com.vaadin.hummingbird.kernel.DomEventListener;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.ui.Component;

import elemental.json.JsonObject;

public class ElementEvents {

    public static class DOMEventForwarder<E extends EventObject>
            implements DomEventListener {

        private Component component;
        private Class<E> eventType;

        public <T extends EventObject, C extends EventSource & Component> DOMEventForwarder(
                Class<E> eventType, C component) {
            this.eventType = eventType;
            this.component = component;
        }

        @Override
        public void handleEvent(JsonObject eventData) {
            assert eventData != null;

            E eventObject = createEventObject();

            populateEvent(eventObject, eventData);
            ((EventSource) component).fireEvent(eventObject);
        }

        private E createEventObject() {
            for (Constructor<?> c : eventType.getConstructors()) {
                if (c.getParameterCount() == 1 && c.getParameterTypes()[0]
                        .isAssignableFrom(component.getClass())) {
                    try {
                        return eventType.cast(c.newInstance(component));
                    } catch (InstantiationException | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException e) {
                        throw new RuntimeException(
                                "Unable to create event object instanceof of type "
                                        + eventType.getName(),
                                e);
                    }
                }
            }
            throw new RuntimeException(
                    "Unable to create event of type " + eventType.getName()
                            + ". No constructor accepting the event source of type "
                            + component.getClass().getName() + " found.");
        }

        private void populateEvent(E eventObject, JsonObject eventData) {
            assert eventObject != null;
            assert eventData != null;

            for (Field f : getEventParameterFields(eventType)) {
                String value = getDomEventParameterName(f);

                f.setAccessible(true);
                try {
                    Object decodedValue = JsonConverter.fromJson(f.getType(),
                            eventData.get(value));
                    f.set(eventObject, decodedValue);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(
                            "Unable to assign value to field " + f.getName()
                                    + " in event object of type "
                                    + eventType.getName(),
                            e);
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DOMEventForwarder)) {
                return false;
            }

            DOMEventForwarder<?> other = (DOMEventForwarder<?>) obj;
            return component.equals(other.component)
                    && eventType.equals(other.eventType);
        }

    }

    public static <T extends EventObject, C extends EventSource & Component> void addElementListener(
            C component, Class<T> eventType, java.util.EventListener listener) {

        if (!component.hasListeners(eventType)) {
            addForwarder(component, eventType);
        }

        component.addListener(eventType, listener);
    }

    private static <C extends EventSource & Component, E extends EventObject> void addForwarder(
            C component, Class<E> eventType) {
        if (eventType.getEnclosingClass() != null
                && !Modifier.isStatic(eventType.getModifiers())) {
            // Non static inner class
            throw new IllegalArgumentException(
                    "Event classes must be top level classes or static inner classes. "
                            + eventType.getName()
                            + " is a non-static inner class");
        }

        Element element = component.getElement();

        String domEventType = getDomEventType(eventType);
        element.addEventData(domEventType, getDomEventData(eventType));

        element.addEventListener(domEventType,
                new DOMEventForwarder<E>(eventType, component));
    }

    private static String[] getDomEventData(
            Class<? extends EventObject> eventType) {
        assert eventType != null;
        List<String> data = new ArrayList<>();
        for (Field f : getEventParameterFields(eventType)) {
            String eventParameter = getDomEventParameterName(f);
            data.add(eventParameter);
        }
        return data.toArray(new String[data.size()]);
    }

    private static String getDomEventParameterName(Field f) {
        assert f != null;

        EventParameter param = f.getAnnotation(EventParameter.class);
        assert param != null;
        String value = param.value();
        if (value.equals("")) {
            value = f.getName();
        }

        return value;
    }

    private static List<Field> getEventParameterFields(Class<?> eventType) {
        assert eventType != null;

        List<Field> fields = new ArrayList<>();
        // TODO Cache
        while (eventType != Object.class) {
            Arrays.stream(eventType.getDeclaredFields())
                    .filter(f -> f.getAnnotation(EventParameter.class) != null)
                    .forEach(fields::add);
            eventType = eventType.getSuperclass();
        }
        return fields;
    }

    public static String getDomEventType(
            Class<? extends EventObject> eventType) {
        assert eventType != null;

        EventType ann = eventType.getAnnotation(EventType.class);
        if (ann == null) {
            throw new IllegalArgumentException(
                    "Event type " + eventType.getName() + " should have an @"
                            + EventType.class.getSimpleName() + " annotation");
        }
        return ann.value();
    }

    private static <C extends EventSource & Component> void removeForwarder(
            C component, Class<? extends EventObject> eventType) {
        component.getElement().removeEventListener(getDomEventType(eventType),
                new DOMEventForwarder(eventType, component));

    }

    public static <T extends EventObject, C extends EventSource & Component> void removeElementListener(
            C c, Class<T> eventType, java.util.EventListener listener) {
        c.removeListener(eventType, listener);
        if (!c.hasListeners(eventType)) {
            removeForwarder(c, eventType);
        }

    }

}
