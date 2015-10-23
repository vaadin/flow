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
import com.vaadin.ui.HasElement;

import elemental.json.JsonObject;

public class ElementEvents {

    public static class DOMEventForwarder<E extends EventObject>
            implements DomEventListener {

        private Class<E> eventType;
        private Element element;
        private HasEventRouter hasEventRouter;

        public <T extends EventObject> DOMEventForwarder(Class<E> eventType,
                Element element, HasEventRouter hasEventRouter) {
            this.eventType = eventType;
            this.element = element;
            this.hasEventRouter = hasEventRouter;
        }

        @Override
        public void handleEvent(JsonObject eventData) {
            assert eventData != null;

            E eventObject = createEventObject();

            populateEvent(eventObject, eventData);
            hasEventRouter.getEventRouter().fireEvent(eventObject);
        }

        private E createEventObject() {
            for (Constructor<?> c : eventType.getConstructors()) {
                if (c.getParameterCount() == 1 && c.getParameterTypes()[0]
                        .isAssignableFrom(hasEventRouter.getClass())) {
                    try {
                        return eventType.cast(c.newInstance(hasEventRouter));
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
                            + hasEventRouter.getClass().getName() + " found.");
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
            return eventType.equals(other.eventType)
                    && hasEventRouter.equals(other.hasEventRouter)
                    && element.equals(other.element);
        }

    }

    /**
     * Adds a listener for the event specified by {@code eventType}, mapped to a
     * DOM event using the {@link EventType} annotation
     *
     * @param hasElementAndEvents
     * @param eventType
     * @param listener
     */
    public static <T extends EventObject, HasElementAndEvents extends HasEventRouter & HasElement> void addElementListener(
            HasElementAndEvents hasElementAndEvents, Class<T> eventType,
            java.util.EventListener listener) {
        addElementListener(hasElementAndEvents.getElement(),
                hasElementAndEvents, eventType, listener);
    }

    /**
     * Adds a listener for the event specified by {@code eventType}, mapped to a
     * DOM event using the {@link EventType} annotation
     *
     * @param hasElementAndEvents
     * @param eventType
     * @param listener
     */
    public static <T extends EventObject> void addElementListener(
            Element element, HasEventRouter hasEventRouter, Class<T> eventType,
            java.util.EventListener listener) {

        if (!hasEventRouter.getEventRouter().hasListeners(eventType)) {
            addForwarder(element, hasEventRouter, eventType);
        }

        hasEventRouter.getEventRouter().addListener(eventType, listener);
    }

    private static <C extends HasEventRouter & HasElement, E extends EventObject> void addForwarder(
            Element element, HasEventRouter hasEventRouter,
            Class<E> eventType) {
        if (eventType.getEnclosingClass() != null
                && !Modifier.isStatic(eventType.getModifiers())) {
            // Non static inner class
            throw new IllegalArgumentException(
                    "Event classes must be top level classes or static inner classes. "
                            + eventType.getName()
                            + " is a non-static inner class");
        }

        String domEventType = getDomEventType(eventType);
        element.addEventData(domEventType, getDomEventData(eventType));

        element.addEventListener(domEventType,
                new DOMEventForwarder<E>(eventType, element, hasEventRouter));
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

    private static <HasElementAndEvents extends HasEventRouter & HasElement> void removeForwarder(
            HasElementAndEvents hasElementAndEvents,
            Class<? extends EventObject> eventType) {
        hasElementAndEvents.getElement().removeEventListener(
                getDomEventType(eventType), new DOMEventForwarder(eventType,
                        hasElementAndEvents.getElement(), hasElementAndEvents));

    }

    public static <T extends EventObject, HasElementAndEvents extends HasEventRouter & HasElement> void removeElementListener(
            HasElementAndEvents hasElementAndEvents, Class<T> eventType,
            java.util.EventListener listener) {
        hasElementAndEvents.getEventRouter().removeListener(eventType,
                listener);
        if (!hasElementAndEvents.getEventRouter().hasListeners(eventType)) {
            removeForwarder(hasElementAndEvents, eventType);
        }

    }

}
