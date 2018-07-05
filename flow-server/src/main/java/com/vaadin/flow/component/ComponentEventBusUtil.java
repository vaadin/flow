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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;

/**
 * Static helpers and caching functionality for {@link ComponentEventBus}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentEventBusUtil {

    private static final String EVENT_CONSTRUCTOR_DEFINITION = "A DOM event constructor should take (Component, boolean) as the two first parameters, followed by any number of optional parameters marked with @"
            + EventData.class.getSimpleName();

    private static final String NO_SUITABLE_CONSTRUCTOR = "No suitable DOM event constructor found for %s. "
            + EVENT_CONSTRUCTOR_DEFINITION;

    // Package private to enable testing only

    static ReflectionCache<ComponentEvent<?>, EventTypeInfo> cache = new ReflectionCache<>(
            EventTypeInfo::new);

    private static class EventTypeInfo {
        private final LinkedHashMap<String, Class<?>> dataExpressions;
        private final Constructor<? extends ComponentEvent<?>> eventConstructor;

        public EventTypeInfo(Class<? extends ComponentEvent<?>> type) {
            eventConstructor = findEventConstructor(type);
            dataExpressions = findEventDataExpressions(eventConstructor);
        }
    }

    private ComponentEventBusUtil() {
        // Static methods and static/shared cache only
    }

    /**
     * Gets a map of event data expression (for
     * {@link Element#addEventListener(String, com.vaadin.flow.dom.DomEventListener, String...)}
     * ) to Java type, with the same order as the parameters for the event
     * constructor (as returned by {@link #getEventConstructor(Class)}).
     * <p>
     * Caches values and automatically uses the cached values when available.
     *
     * @param eventType
     *            the component event type
     * @return a map of event expressions, ordered in constructor parameter
     *         order
     */
    public static LinkedHashMap<String, Class<?>> getEventDataExpressions(
            Class<? extends ComponentEvent<?>> eventType) {
        return cache.get(eventType).dataExpressions;
    }

    /**
     * Scans the event type and forms a map of event data expression (for
     * {@link Element#addEventListener(String, com.vaadin.flow.dom.DomEventListener, String...)}
     * ) to Java type, with the same order as the parameters for the event
     * constructor (as returned by {@link #getEventConstructor(Class)}).
     *
     * @return a map of event data expressions, in the order defined by the
     *         component event constructor parameters
     */
    private static LinkedHashMap<String, Class<?>> findEventDataExpressions(
            Constructor<? extends ComponentEvent<?>> eventConstructor) {
        LinkedHashMap<String, Class<?>> eventDataExpressions = new LinkedHashMap<>();
        // Parameter 0 is always "Component source"
        // Parameter 1 is always "boolean fromClient"
        for (int i = 2; i < eventConstructor.getParameterCount(); i++) {
            Parameter p = eventConstructor.getParameters()[i];
            EventData eventData = p.getAnnotation(EventData.class);
            if (eventData == null || eventData.value().isEmpty()) {
                // The parameter foo of the constructor Bar(Foo foo) has no
                // @DomEvent, or its value is empty."
                throw new IllegalArgumentException(String.format(
                        "The parameter %s of the constructor %s has no @%s, or the annotation value is empty",
                        p.getName(), eventConstructor.toString(),
                        EventData.class.getSimpleName()));
            }
            eventDataExpressions.put(eventData.value(),p.getType());
        }
        return eventDataExpressions;
    }

    /**
     * Gets the constructor to use for firing a component event, of the given
     * type, based on a DOM event.
     * <p>
     *
     * @param <T>
     *            the event type
     * @param eventType
     *            the event type
     * @return the constructor to use when creating an event from a DOM event
     * @throws IllegalArgumentException
     *             if no suitable constructor was found
     */
    @SuppressWarnings("unchecked")
    public static <T extends ComponentEvent<?>> Constructor<T> getEventConstructor(
            Class<T> eventType) {
        return (Constructor<T>) cache.get(eventType).eventConstructor;
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
    private static <T extends ComponentEvent<?>> Constructor<T> findEventConstructor(
            Class<T> eventType) {
        ReflectTools.checkClassAccessibility(eventType);
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
            return constructors.get(0);
        }

        // Multiple constructors - use the one which has @EventData
        Constructor<T> eventDataConstructor = null;
        for (Constructor<T> c : constructors) {
            if (c.getParameterCount() > 2) {
                if (eventDataConstructor == null) {
                    eventDataConstructor = c;
                } else {
                    throw new IllegalArgumentException(
                            "Multiple DOM event constructors annotated with @"
                                    + EventData.class.getSimpleName()
                                    + " found for " + eventType.getName() + ". "
                                    + EVENT_CONSTRUCTOR_DEFINITION);
                }
            }
        }
        if (eventDataConstructor != null) {
            return eventDataConstructor;
        } else {
            // This should not be possible (there are multiple constructors so
            // one of them must have @EventData)
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
    public static boolean isDomEventConstructor(Constructor<?> constructor) {
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
