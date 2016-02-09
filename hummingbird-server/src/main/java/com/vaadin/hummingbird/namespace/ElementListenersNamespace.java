/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;

/**
 * Namespace recording DOM events with server-side listeners. The key set of
 * this map namespace describes the event types for which listeners are present.
 * The values associated with the keys are currently not used.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementListenersNamespace extends MapNamespace {
    // The most compact single JSON value
    private static final Integer hasListenersValue = Integer.valueOf(1);

    // Server-side only data
    private Map<String, Set<DomEventListener>> listeners = new HashMap<>();

    /**
     * Creates a new element listener namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     *
     */
    public ElementListenersNamespace(StateNode node) {
        super(node);
    }

    /**
     * Adds a listener for an event type.
     *
     * @param eventType
     *            the event type
     * @param listener
     *            the listener to add
     * @return a handle for removing the listener
     */
    public EventRegistrationHandle add(String eventType,
            DomEventListener listener) {
        assert eventType != null;
        assert listener != null;

        Set<DomEventListener> typeListeners = listeners
                .computeIfAbsent(eventType, key -> {
                    // Add to the set that is synchronized with the client
                    put(key, hasListenersValue);

                    // Create a set to store listener instances in
                    return new HashSet<>();
                });
        typeListeners.add(listener);

        return () -> removeListener(eventType, listener);
    }

    private void removeListener(String eventType, DomEventListener listener) {
        Set<DomEventListener> set = listeners.get(eventType);
        if (set != null) {
            set.remove(listener);

            // No more listeners of this type?
            if (set.isEmpty()) {
                listeners.remove(eventType);

                // Remove from the set that is synchronized with the client
                remove(eventType);
            }
        }
    }

    /**
     * Fires an event to all listeners registered for the given type.
     *
     * @param eventType
     *            the event type
     */
    public void fireEvent(String eventType) {
        Set<DomEventListener> typeListeners = listeners.get(eventType);
        if (typeListeners == null) {
            return;
        }

        // Copy to allow concurrent modification
        HashSet<DomEventListener> copy = new HashSet<>(typeListeners);

        copy.forEach(DomEventListener::handleEvent);
    }
}
