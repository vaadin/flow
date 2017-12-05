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
package com.vaadin.flow.nodefeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.flow.ConstantPoolKey;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.util.JsonUtils;
import com.vaadin.shared.Registration;

import elemental.json.Json;

/**
 * Map of DOM events with server-side listeners. The key set of this map
 * describes the event types for which listeners are present. The values
 * associated with the keys are currently not used.
 *
 * @author Vaadin Ltd
 */
public class ElementListenerMap extends NodeMap {
    /*
     * Shared empty serializable set instance to avoid allocating lots of memory
     * for the default case of no event data expressions at all. Cannot easily
     * make the instance immutable while still implementing HashSet. To avoid
     * accidental modification, we instead assert that it's empty when it's
     * used.
     */
    private static final HashSet<String> emptyHashSet = new HashSet<>();

    // Server-side only data
    private Map<String, ArrayList<DomEventListener>> listeners;
    private Map<String, Set<String>> typeToExpressions;

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
     * @param eventDataExpressions
     *            the event data expressions
     * @return a handle for removing the listener
     */
    public Registration add(String eventType,
            DomEventListener listener, String[] eventDataExpressions) {
        assert eventType != null;
        assert listener != null;
        assert eventDataExpressions != null;

        if (listeners == null) {
            listeners = new HashMap<>();
            typeToExpressions = new HashMap<>();
        }

        // Could optimize slightly by integrating the initialization into the
        // main logic, but that would make the code much harder to read
        if (!contains(eventType)) {
            assert !listeners.containsKey(eventType);

            listeners.put(eventType, new ArrayList<>());

            // Make sure the "immutable" instance hasn't accidentally been
            // mutated
            assert emptyHashSet.isEmpty();
            typeToExpressions.put(eventType, emptyHashSet);
            put(eventType, createConstantPoolKey(emptyHashSet));
        }

        listeners.get(eventType).add(listener);

        if (eventDataExpressions.length != 0) {
            Set<String> eventData = new HashSet<>(
                    typeToExpressions.get(eventType));

            if (eventData.addAll(Arrays.asList(eventDataExpressions))) {
                // Update the constant pool reference if the value has changed
                put(eventType, createConstantPoolKey(eventData));

                // Remember value for server-side use
                typeToExpressions.put(eventType, eventData);
            }
        }

        return () -> removeListener(eventType, listener);
    }

    private static ConstantPoolKey createConstantPoolKey(
            Set<String> eventData) {
        return new ConstantPoolKey(eventData.stream().map(Json::create)
                .collect(JsonUtils.asArray()));
    }

    private void removeListener(String eventType, DomEventListener listener) {
        if (listeners == null) {
            return;
        }
        List<DomEventListener> listenerList = listeners.get(eventType);
        if (listenerList != null) {
            listenerList.remove(listener);

            // No more listeners of this type?
            if (listenerList.isEmpty()) {
                listeners.remove(eventType);
                typeToExpressions.remove(eventType);

                if (listeners.isEmpty()) {
                    listeners = null;
                    typeToExpressions = null;
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
        List<DomEventListener> typeListeners = listeners
                .get(event.getType());
        if (typeListeners == null) {
            return;
        }

        // Copy to allow concurrent modification
        List<DomEventListener> copy = new ArrayList<>(typeListeners);

        copy.forEach(l -> l.handleEvent(event));
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
        if (typeToExpressions == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(typeToExpressions.get(name));
        }
    }
}
