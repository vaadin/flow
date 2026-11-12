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
package com.vaadin.flow.component.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBusUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.shared.JsonConstants;

/**
 * What a component event type annotated with {@link DomEvent} is fired with:
 * the constructor that creates it from a DOM event and the {@link EventData} of
 * its parameters. Computed from the event type on creation and never cached
 * here: {@link ComponentEventBusUtil} caches it for the application, and the
 * build creates it without a cache, so that a class it loads with a class
 * loader it closes afterwards is not kept by a static map.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.4
 */
public final class DomEventInfo {

    private static final String EVENT_CONSTRUCTOR_DEFINITION = "A DOM event constructor should take (Component, boolean) as the two first parameters, followed by any number of optional parameters marked with @"
            + EventData.class.getSimpleName();

    private static final String NO_SUITABLE_CONSTRUCTOR = "No suitable DOM event constructor found for %s. "
            + EVENT_CONSTRUCTOR_DEFINITION;

    private final LinkedHashMap<String, Class<?>> dataExpressions;
    private final List<String> listenerEventData;
    private final List<String> expressionEventData;
    private final List<String> bundledEventData;
    private final Constructor<? extends ComponentEvent<?>> eventConstructor;

    /**
     * Scans the given event type.
     *
     * @param type
     *            the component event type, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the event type has no suitable DOM event constructor
     */
    public DomEventInfo(Class<? extends ComponentEvent<?>> type) {
        eventConstructor = findEventConstructor(type);
        dataExpressions = findEventDataExpressions(eventConstructor);
        listenerEventData = toListenerEventData(dataExpressions);
        bundledEventData = BundledEventData.selectBundled(type,
                listenerEventData);
        // Keeps sharing the list when nothing is put into the bundle
        expressionEventData = bundledEventData.isEmpty() ? listenerEventData
                : listenerEventData.stream().filter(
                        eventData -> !bundledEventData.contains(eventData))
                        .toList();
    }

    /**
     * Gets the event data expressions mapped to the Java type of the
     * constructor parameter they are passed as, in parameter order.
     *
     * @return the event data expressions, not <code>null</code>
     */
    public LinkedHashMap<String, Class<?>> getDataExpressions() {
        return dataExpressions;
    }

    /**
     * Gets the event data to add to a DOM listener registration, with
     * expressions for components and elements mapped like
     * {@link DomListenerRegistration#addEventDataElement(String)} does.
     *
     * @return an unmodifiable list, not <code>null</code>
     */
    public List<String> getListenerEventData() {
        return listenerEventData;
    }

    /**
     * Gets the event data of {@link #getListenerEventData()} that the client
     * gets as it is, which is all of it except {@link #getBundledEventData()}.
     *
     * @return an unmodifiable list, not <code>null</code>
     */
    public List<String> getExpressionEventData() {
        return expressionEventData;
    }

    /**
     * Gets the event data of {@link #getListenerEventData()} that the build
     * puts into the bundle as functions, see {@link BundledEventData}.
     *
     * @return an unmodifiable list, empty if there are none
     */
    public List<String> getBundledEventData() {
        return bundledEventData;
    }

    /**
     * Gets the constructor to use when creating the event from a DOM event.
     *
     * @return the constructor, not <code>null</code>
     */
    public Constructor<? extends ComponentEvent<?>> getEventConstructor() {
        return eventConstructor;
    }

    private static List<String> toListenerEventData(
            LinkedHashMap<String, Class<?>> dataExpressions) {
        LinkedHashSet<String> eventData = new LinkedHashSet<>();
        dataExpressions.forEach((expression, type) -> {
            if (Component.class.isAssignableFrom(type)
                    || type == Element.class) {
                // Same mapping as
                // DomListenerRegistration.addEventDataElement
                eventData.add("event.target".equals(expression)
                        ? JsonConstants.MAP_STATE_NODE_EVENT_DATA
                        : (JsonConstants.MAP_STATE_NODE_EVENT_DATA
                                + expression));
            } else {
                eventData.add(expression);
            }
        });
        return List.copyOf(eventData);
    }

    /**
     * Scans the event type and forms a map of event data expression (for
     * {@link com.vaadin.flow.dom.DomListenerRegistration#addEventData(String)}
     * ) to Java type, with the same order as the parameters for the event
     * constructor (see {@link #getEventConstructor()}).
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
            eventDataExpressions.put(eventData.value(), p.getType());
        }
        return eventDataExpressions;
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
            if (ComponentEventBusUtil.isDomEventConstructor(c)) {
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
                            "More than one DOM event constructors annotated with @"
                                    + EventData.class.getSimpleName()
                                    + " found for " + eventType.getName()
                                    + ". There can be only one constructor with @"
                                    + EventData.class.getSimpleName()
                                    + " annotations present.");
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
}
