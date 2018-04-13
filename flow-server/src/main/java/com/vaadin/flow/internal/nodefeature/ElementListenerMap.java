/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.internal.nodefeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateNode;

import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * Map of DOM events with server-side listeners. The key set of this map
 * describes the event types for which listeners are present. The values
 * associated with the keys are currently not used.
 *
 * @author Vaadin Ltd
 */
public class ElementListenerMap extends NodeMap {
    /**
     * Default filter expression that always passes.
     */
    public static final String DEFAULT_FILTER = "true";

    // Server-side only data
    private Map<String, List<DomEventListenerWrapper>> listeners;

    private static class DomEventListenerWrapper
            implements DomListenerRegistration {
        private final String type;
        private final DomEventListener origin;
        private final ElementListenerMap listenerMap;

        private DisabledUpdateMode mode;
        private Set<String> eventDataExpressions;
        private String filter = DEFAULT_FILTER;

        private DomEventListenerWrapper(ElementListenerMap listenerMap,
                String type, DomEventListener origin) {
            this.listenerMap = listenerMap;
            this.type = type;
            this.origin = origin;
        }

        @Override
        public void remove() {
            listenerMap.removeListener(type, this);
        }

        @Override
        public DomListenerRegistration addEventData(String eventData) {
            if (eventData == null) {
                throw new IllegalArgumentException(
                        "The event data expression must not be null");
            }

            Set<String> previous = listenerMap.collectDataExpressions(type);

            if (eventDataExpressions == null) {
                eventDataExpressions = new HashSet<>();
            }
            eventDataExpressions.add(eventData);

            if (!previous.contains(eventData)) {
                // Update the constant pool reference if the value has
                // changed
                listenerMap.updateEventSettings(type);
            }

            return this;
        }

        @Override
        public DomListenerRegistration setDisabledUpdateMode(
                DisabledUpdateMode disabledUpdateMode) {
            if (disabledUpdateMode == null) {
                throw new IllegalArgumentException(
                        "RPC comunication control mode for disabled element must not be null");
            }

            mode = disabledUpdateMode;
            return this;
        }

        @Override
        public DomListenerRegistration setFilter(String filter) {
            if (filter == null) {
                filter = DEFAULT_FILTER;
            }

            Set<String> previous = listenerMap.collectFilterExpressions(type);
            this.filter = filter;

            Set<String> current = listenerMap.collectFilterExpressions(type);
            if (!previous.equals(current)) {
                listenerMap.updateEventSettings(type);
            }

            return this;
        }

        @Override
        public String getFilter() {
            if (DEFAULT_FILTER.equals(filter)) {
                return null;
            }
            return filter;
        }
    }

    /**
     * Creates a new element listener map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ElementListenerMap(StateNode node) {
        super(node);
    }

    /**
     * Add eventData for an event type.
     *
     * @param eventType
     *            the event type
     * @param listener
     *            the listener to add
     * @return a handle for configuring and removing the listener
     */
    public DomListenerRegistration add(String eventType,
            DomEventListener listener) {
        assert eventType != null;
        assert listener != null;

        if (listeners == null) {
            listeners = new HashMap<>();
        }

        if (!contains(eventType)) {
            assert !listeners.containsKey(eventType);

            listeners.put(eventType, new ArrayList<>());
        }

        DomEventListenerWrapper listenerWrapper = new DomEventListenerWrapper(
                this, eventType, listener);

        listeners.get(eventType).add(listenerWrapper);

        updateEventSettings(eventType);

        return listenerWrapper;
    }

    private Stream<DomEventListenerWrapper> streamWrappers(String eventType) {
        if (listeners == null) {
            return Stream.empty();
        }
        List<DomEventListenerWrapper> typeListeners = listeners.get(eventType);
        if (typeListeners == null) {
            return Stream.empty();
        }

        return typeListeners.stream();
    }

    private Set<String> collectDataExpressions(String eventType) {
        return streamWrappers(eventType)
                .map(wrapper -> wrapper.eventDataExpressions)
                .filter(Objects::nonNull).flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> collectFilterExpressions(String eventType) {
        return streamWrappers(eventType).map(wrapper -> wrapper.filter)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void updateEventSettings(String eventType) {
        Set<String> eventData = collectDataExpressions(eventType);
        Set<String> filters = collectFilterExpressions(eventType);

        JsonArray eventDataJson = JsonUtils.createArray(eventData,
                Json::create);
        JsonArray filtersJson = JsonUtils.createArray(filters, Json::create);

        JsonArray value = JsonUtils.createArray(eventDataJson, filtersJson);

        ConstantPoolKey constantPoolKey = new ConstantPoolKey(value);
        put(eventType, constantPoolKey);
    }

    private void removeListener(String eventType,
            DomEventListenerWrapper wrapper) {
        if (listeners == null) {
            return;
        }
        Collection<DomEventListenerWrapper> listenerList = listeners
                .get(eventType);
        if (listenerList != null) {
            listenerList.remove(wrapper);

            // No more listeners of this type?
            if (listenerList.isEmpty()) {
                listeners.remove(eventType);

                if (listeners.isEmpty()) {
                    listeners = null;
                }

                // Remove from the set that is synchronized with the client
                remove(eventType);
            }
        }
    }

    /**
     * Fires an event to all listeners registered for the given type.
     *
     * @param event
     *            the event to fire
     */
    public void fireEvent(DomEvent event) {
        if (listeners == null) {
            return;
        }
        boolean isElementEnabled = event.getSource().isEnabled();
        List<DomEventListenerWrapper> typeListeners = listeners
                .get(event.getType());
        if (typeListeners == null) {
            return;
        }

        List<DomEventListener> listeners = new ArrayList<>();
        for (DomEventListenerWrapper wrapper : typeListeners) {
            if ((isElementEnabled
                    || DisabledUpdateMode.ALWAYS.equals(wrapper.mode))
                    && event.matchesFilter(wrapper.filter)) {
                listeners.add(wrapper.origin);
            }
        }

        listeners.forEach(listener -> listener.handleEvent(event));
    }

    /**
     * Gets the event data expressions defined for the given event name. This
     * method is currently only provided to facilitate unit testing.
     *
     * @param name
     *            the name of the event, not <code>null</code>
     * @return an unmodifiable set of event data expressions, not
     *         <code>null</code>
     */
    Set<String> getExpressions(String name) {
        assert name != null;
        return collectDataExpressions(name);
    }

}
