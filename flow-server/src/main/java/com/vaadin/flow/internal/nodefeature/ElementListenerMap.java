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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateNode;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Map of DOM events with server-side listeners. The key set of this map
 * describes the event types for which listeners are present. The values
 * associated with the keys are currently not used.
 *
 * @author Vaadin Ltd
 */
public class ElementListenerMap extends NodeMap {
    private static final String ALWAYS_TRUE_FILTER = "1";

    // Server-side only data
    private Map<String, List<DomEventListenerWrapper>> listeners;

    private static class DomEventListenerWrapper
            implements DomListenerRegistration {
        private final String type;
        private final DomEventListener origin;
        private final ElementListenerMap listenerMap;

        private DisabledUpdateMode mode;
        private Set<String> eventDataExpressions;
        private String filter;

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

            Map<String, Boolean> previous = listenerMap
                    .collectEventExpressions(type);

            if (eventDataExpressions == null) {
                eventDataExpressions = new HashSet<>();
            }
            eventDataExpressions.add(eventData);

            if (!previous.containsKey(eventData)) {
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
            Map<String, Boolean> previous = listenerMap
                    .collectEventExpressions(type);

            this.filter = filter;

            if (!Boolean.TRUE.equals(previous.get(filter))) {
                // Expression was not previously used as a filter
                listenerMap.updateEventSettings(type);
            }

            return this;
        }

        @Override
        public String getFilter() {
            return filter;
        }

        boolean matchesFilter(JsonObject eventData) {
            if (filter == null) {
                // No filter: always matches
                return true;
            }

            if (eventData == null) {
                // No event data: cannot match the filter
                return false;
            }

            return eventData.getBoolean(filter);
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

    private Collection<DomEventListenerWrapper> getWrappers(String eventType) {
        if (listeners == null) {
            return Collections.emptyList();
        }
        List<DomEventListenerWrapper> typeListeners = listeners.get(eventType);
        if (typeListeners == null) {
            return Collections.emptyList();
        }

        return typeListeners;
    }

    // Map<Expression, IsFilter>
    private Map<String, Boolean> collectEventExpressions(String eventType) {
        HashMap<String, Boolean> expressions = new HashMap<>();

        Collection<DomEventListenerWrapper> wrappers = getWrappers(eventType);

        // Pass 1: Collect data expressions as non-filter expressions
        for (DomEventListenerWrapper wrapper : wrappers) {
            if (wrapper.eventDataExpressions != null) {
                wrapper.eventDataExpressions.forEach(expression -> expressions
                        .put(expression, Boolean.FALSE));
            }
        }

        // Pass 2: Collect filters
        boolean hasUnfilteredListener = false;
        boolean hasFilteredListener = false;
        for (DomEventListenerWrapper wrapper : wrappers) {
            String filter = wrapper.getFilter();
            if (filter == null) {
                hasUnfilteredListener = true;
            } else {
                hasFilteredListener = true;
                expressions.put(filter, Boolean.TRUE);
            }
        }

        if (hasFilteredListener && hasUnfilteredListener) {
            /*
             * If there are filters and none match, then client won't send
             * anything to the server.
             *
             * Include a filter that always passes to ensure that unfiltered
             * listeners are still notified.
             */
            expressions.put(ALWAYS_TRUE_FILTER, Boolean.TRUE);
        }

        return expressions;
    }

    private void updateEventSettings(String eventType) {
        Map<String, Boolean> eventSettings = collectEventExpressions(eventType);
        JsonObject eventSettingsJson = JsonUtils.createObject(eventSettings,
                Json::create);

        ConstantPoolKey constantPoolKey = new ConstantPoolKey(
                eventSettingsJson);
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
                    && wrapper.matchesFilter(event.getEventData())) {
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
        return collectEventExpressions(name).keySet();
    }

}
