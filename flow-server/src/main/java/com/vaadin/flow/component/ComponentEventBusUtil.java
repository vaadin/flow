/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.component.internal.DomEventInfo;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.ReflectionCache;

/**
 * Static helpers and caching functionality for {@link ComponentEventBus}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ComponentEventBusUtil {

    // Package private to enable testing only

    static ReflectionCache<ComponentEvent<?>, DomEventInfo> cache = new ReflectionCache<>(
            DomEventInfo::new);

    private ComponentEventBusUtil() {
        // Static methods and static/shared cache only
    }

    /**
     * Gets a map of event data expression (for
     * {@link com.vaadin.flow.dom.DomListenerRegistration#addEventData(String)}
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
        return cache.get(eventType).getDataExpressions();
    }

    /**
     * Gets the event data expressions to add to a DOM listener registration for
     * the given event type, with expressions for components and elements mapped
     * like {@link DomListenerRegistration#addEventDataElement(String)} does.
     * <p>
     * The same unmodifiable list is returned for every call so that all
     * listeners of the event type can share it, see
     * {@link com.vaadin.flow.internal.nodefeature.ElementListenerMap#addEventData(DomListenerRegistration, List)}.
     *
     * @param eventType
     *            the component event type
     * @return an unmodifiable list of event data expressions for a DOM listener
     *         registration
     * @since 25.4
     */
    public static List<String> getListenerEventData(
            Class<? extends ComponentEvent<?>> eventType) {
        return cache.get(eventType).getListenerEventData();
    }

    /**
     * Gets the event data of {@link #getListenerEventData(Class)} that the
     * client gets as it is, which is all of it except what
     * {@link #getBundledEventData(Class)} returns. The same unmodifiable list
     * is returned for every call.
     *
     * @param eventType
     *            the component event type
     * @return an unmodifiable list of event data expressions
     */
    static List<String> getExpressionEventData(
            Class<? extends ComponentEvent<?>> eventType) {
        return cache.get(eventType).getExpressionEventData();
    }

    /**
     * Gets the event data of {@link #getListenerEventData(Class)} that the
     * build puts into the bundle as functions, see {@link BundledEventData}.
     * The same unmodifiable list is returned for every call.
     *
     * @param eventType
     *            the component event type
     * @return an unmodifiable list of event data expressions, empty if there
     *         are none
     */
    static List<String> getBundledEventData(
            Class<? extends ComponentEvent<?>> eventType) {
        return cache.get(eventType).getBundledEventData();
    }

    /**
     * Gets the constructor to use for firing a component event, of the given
     * type, based on a DOM event.
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
        return (Constructor<T>) cache.get(eventType).getEventConstructor();
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
