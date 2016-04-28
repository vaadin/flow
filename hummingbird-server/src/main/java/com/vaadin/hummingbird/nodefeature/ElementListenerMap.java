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
package com.vaadin.hummingbird.nodefeature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.DomEvent;
import com.vaadin.hummingbird.dom.DomEventListener;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.util.JsonUtil;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Map of DOM events with server-side listeners. The key set of this map
 * describes the event types for which listeners are present. The values
 * associated with the keys are currently not used.
 *
 * @author Vaadin Ltd
 */
public class ElementListenerMap extends NodeMap {
    // Server-side only data
    private HashMap<String, ArrayList<DomEventListener>> listeners = new HashMap<>();

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
     * Adds a listener for an event type.
     *
     * @param eventType
     *            the event type
     * @param listener
     *            the listener to add
     * @param eventDataExpressions
     *            the event data expressions
     * @return a handle for removing the listener
     */
    public EventRegistrationHandle add(String eventType,
            DomEventListener listener, String[] eventDataExpressions) {
        assert eventType != null;
        assert listener != null;
        assert eventDataExpressions != null;

        // Could optimize slightly by integrating the initialization into the
        // main logic, but that would make the code much harder to read
        if (!contains(eventType)) {
            assert !listeners.containsKey(eventType);

            listeners.put(eventType, new ArrayList<>());
            put(eventType, Json.createArray());
        }

        listeners.get(eventType).add(listener);

        if (eventDataExpressions.length != 0) {
            JsonArray eventDataJson = (JsonArray) get(eventType);

            Set<String> eventData = JsonUtil.stream(eventDataJson)
                    .map(JsonValue::asString).collect(Collectors.toSet());

            if (eventData.addAll(Arrays.asList(eventDataExpressions))) {
                // Send full new event data to the client if the set changed

                put(eventType, eventData.stream().map(Json::create)
                        .collect(JsonUtil.asArray()));
            }
        }

        return () -> removeListener(eventType, listener);
    }

    private void removeListener(String eventType, DomEventListener listener) {
        ArrayList<DomEventListener> listenerList = listeners.get(eventType);
        if (listenerList != null) {
            listenerList.remove(listener);

            // No more listeners of this type?
            if (listenerList.isEmpty()) {
                listeners.remove(eventType);

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
        ArrayList<DomEventListener> typeListeners = listeners
                .get(event.getType());
        if (typeListeners == null) {
            return;
        }

        // Copy to allow concurrent modification
        ArrayList<DomEventListener> copy = new ArrayList<>(typeListeners);

        copy.forEach(l -> l.handleEvent(event));
    }
}
