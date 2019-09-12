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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Map of DOM events with server-side listeners. The key set of this map
 * describes the event types for which listeners are present. The values
 * associated with the keys are currently not used.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementListenerMap extends NodeMap {
    /**
     * Dummy filter string that always passes.
     */
    public static final String ALWAYS_TRUE_FILTER = "1";

    private static final EnumSet<DebouncePhase> NO_TIMEOUT_PHASES
            = EnumSet.of(DebouncePhase.LEADING);

    // Server-side only data
    private Map<String, List<DomEventListenerWrapper>> listeners;

    private static class ExpressionSettings implements Serializable {
        private Map<Integer, Set<DebouncePhase>> debounceSettings = new HashMap<>();

        public void addDebouncePhases(int timeout, Set<DebouncePhase> phases) {
            debounceSettings.merge(Integer.valueOf(timeout), phases,
                    (phases1, phases2) -> {
                        EnumSet<DebouncePhase> merge = EnumSet.copyOf(phases1);
                        merge.addAll(phases2);
                        return merge;
                    });
        }

        public JsonValue toJson() {
            if (debounceSettings.isEmpty()) {
                return Json.create(false);
            } else if (debounceSettings.size() == 1
                    && debounceSettings.containsKey(Integer.valueOf(0))) {
                // Shorthand if only debounce is a dummy filter debounce
                return Json.create(true);
            } else {
                // [[timeout1, phase1, phase2, ...], [timeout2, phase1, ...]]
                return debounceSettings.entrySet().stream()
                        .map(entry -> Stream
                                .concat(Stream.of(
                                        Json.create(entry.getKey().intValue())),
                                        entry.getValue().stream()
                                                .map(DebouncePhase::getIdentifier)
                                                .map(Json::create))
                                .collect(JsonUtils.asArray()))
                        .collect(JsonUtils.asArray());
            }

        }
    }

    private static class DomEventListenerWrapper
            implements DomListenerRegistration {
        private final String type;
        private final DomEventListener origin;
        private final ElementListenerMap listenerMap;

        private DisabledUpdateMode mode = DisabledUpdateMode.ONLY_WHEN_ENABLED;
        private Set<String> eventDataExpressions;
        private String filter;

        private int debounceTimeout = 0;
        private EnumSet<DebouncePhase> debouncePhases = NO_TIMEOUT_PHASES;
        private List<SerializableRunnable> unregisterHandlers;

        private DomEventListenerWrapper(ElementListenerMap listenerMap,
                String type, DomEventListener origin) {
            this.listenerMap = listenerMap;
            this.type = type;
            this.origin = origin;
        }

        @Override
        public String getEventType() {
            return type;
        }

        @Override
        public void remove() {
            if (unregisterHandlers != null) {
                unregisterHandlers.forEach(SerializableRunnable::run);
            }

            listenerMap.removeListener(type, this);

            // update settings after removal. If we have listeners of the
            // same type registered, we want to remove settings set by this
            // particular listener from the overall set
            // fixes #5090
            if (listenerMap.listeners != null
                    && listenerMap.listeners.containsKey(type)) {
                listenerMap.updateEventSettings(type);
            }
        }

        @Override
        public DomListenerRegistration addEventData(String eventData) {
            if (eventData == null) {
                throw new IllegalArgumentException(
                        "The event data expression must not be null");
            }

            if (eventDataExpressions == null) {
                eventDataExpressions = Collections.singleton(eventData);
            } else {
                if (eventDataExpressions.size() == 1) {
                    Set<String> oldExpressions = eventDataExpressions;
                    // Don't use no-args or Collection constructors that
                    // allocate for 16 entries
                    eventDataExpressions = new HashSet<>(4);
                    eventDataExpressions.addAll(oldExpressions);
                }
                eventDataExpressions.add(eventData);
            }

            listenerMap.updateEventSettings(type);

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
            this.filter = filter;

            listenerMap.updateEventSettings(type);

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

        @Override
        public DomListenerRegistration debounce(int timeout,
                DebouncePhase firstPhase, DebouncePhase... additionalPhases) {
            if (timeout < 0) {
                throw new IllegalArgumentException(
                        "Timeout cannot be negative");
            }

            debounceTimeout = timeout;

            if (timeout == 0) {
                debouncePhases = NO_TIMEOUT_PHASES;
            } else {
                debouncePhases = EnumSet.of(firstPhase, additionalPhases);
            }

            listenerMap.updateEventSettings(type);

            return this;
        }

        @Override
        public int getDebounceTimeout() {
            return debounceTimeout;
        }

        @Override
        public Set<DebouncePhase> getDebouncePhases() {
            return Collections.unmodifiableSet(debouncePhases);
        }

        public boolean matchesPhase(DebouncePhase phase) {
            return debouncePhases.contains(phase);
        }

        @Override
        public DomListenerRegistration onUnregister(
                SerializableRunnable unregisterHandler) {
            if (unregisterHandlers == null) {
                unregisterHandlers = new ArrayList<>(1);
            }
            unregisterHandlers.add(Objects.requireNonNull(unregisterHandler,
                    "Unregister handler cannot be null"));
            return this;
        }

        private boolean isPropertySynchronized(String propertyName) {
            return eventDataExpressions != null && eventDataExpressions
                    .contains(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN
                            + propertyName);
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

        if (!contains(eventType)) {
            assert listeners == null || !listeners.containsKey(eventType);

            ArrayList<DomEventListenerWrapper> listenerList = new ArrayList<>(
                    1);

            if (listeners == null) {
                listeners = Collections.singletonMap(eventType, listenerList);
            } else {
                if (listeners.size() == 1 && !(listeners instanceof HashMap)) {
                    listeners = new HashMap<>(listeners);
                }

                listeners.put(eventType, listenerList);
            }

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

    private Map<String, ExpressionSettings> collectEventExpressions(
            String eventType) {
        Map<String, ExpressionSettings> expressions = new HashMap<>();
        boolean hasUnfilteredListener = false;
        boolean hasFilteredListener = false;

        Function<String, ExpressionSettings> ensureExpression = expression -> expressions
                .computeIfAbsent(expression, (key -> new ExpressionSettings()));

        Collection<DomEventListenerWrapper> wrappers = getWrappers(eventType);

        for (DomEventListenerWrapper wrapper : wrappers) {
            if (wrapper.eventDataExpressions != null) {
                wrapper.eventDataExpressions.forEach(ensureExpression::apply);
            }

            String filter = wrapper.getFilter();

            int timeout = wrapper.debounceTimeout;
            if (timeout > 0 && filter == null) {
                filter = ALWAYS_TRUE_FILTER;
            }

            if (filter == null) {
                hasUnfilteredListener = true;
            } else {
                hasFilteredListener = true;

                ensureExpression.apply(filter).addDebouncePhases(timeout,
                        wrapper.debouncePhases);
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
            ensureExpression.apply(ALWAYS_TRUE_FILTER).addDebouncePhases(0,
                    NO_TIMEOUT_PHASES);
        }

        return expressions;
    }

    private void updateEventSettings(String eventType) {
            Map<String, ExpressionSettings> eventSettings = collectEventExpressions(
                    eventType);
            JsonObject eventSettingsJson = JsonUtils.createObject(eventSettings,
                    ExpressionSettings::toJson);

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
                if (listeners.size() == 1) {
                    assert listeners.containsKey(eventType);
                    listeners = null;
                } else {
                    listeners.remove(eventType);
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
                    && wrapper.matchesFilter(event.getEventData())
                    && wrapper.matchesPhase(event.getPhase())) {
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

    /**
     * Gets the most permissive update mode for any event registration that is
     * configured to synchronize the given property.
     *
     * @param propertyName
     *            the property name to check, not <code>null</code>
     * @return the most permissive update mode, or <code>null</code> if
     *         synchronization is not configured for the given property
     */
    public DisabledUpdateMode getPropertySynchronizationMode(
            String propertyName) {
        assert propertyName != null;

        if (listeners == null) {
            return null;
        }

        return listeners.values().stream().flatMap(List::stream)
                .filter(wrapper -> wrapper.isPropertySynchronized(propertyName))
                .map(wrapper -> wrapper.mode)
                .reduce(DisabledUpdateMode::mostPermissive).orElse(null);
    }

}
