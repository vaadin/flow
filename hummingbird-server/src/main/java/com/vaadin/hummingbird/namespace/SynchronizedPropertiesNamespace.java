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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.util.JsonUtil;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Map for storing the data related to property synchronization from the client
 * side to the server.
 *
 * @author Vaadin
 * @since
 */
public class SynchronizedPropertiesNamespace extends MapNamespace {

    public static final String KEY_EVENTS = "events";
    public static final String KEY_PROPERTIES = "properties";

    /**
     * Creates a new namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public SynchronizedPropertiesNamespace(StateNode node) {
        super(node);
    }

    /**
     * Sets the names of the properties to synchronize from the client to the
     * server.
     *
     * @param propertyNames
     *            the names of the properties to synchronize
     */
    public void setSynchronizedProperties(String[] propertyNames) {
        putJson(KEY_PROPERTIES, Arrays.asList(propertyNames).stream()
                .map(Json::create).collect(JsonUtil.asArray()));
    }

    /**
     * Gets the names of the properties to synchronize from the client to the
     * server.
     *
     * @return the names of the properties to synchronize
     */
    public Set<String> getSynchronizedProperties() {
        if (!contains(KEY_PROPERTIES)) {
            return Collections.emptySet();
        }
        return JsonUtil.stream((JsonArray) get(KEY_PROPERTIES))
                .map(JsonValue::asString).collect(Collectors.toSet());

    }

    /**
     * Sets the event types which should trigger synchronization of properties
     * from the client side to the server.
     *
     * @param eventTypes
     *            the event types which should trigger synchronization
     */
    public void setSynchronizedPropertiesEvents(String[] eventTypes) {
        putJson(KEY_EVENTS, Arrays.asList(eventTypes).stream().map(Json::create)
                .collect(JsonUtil.asArray()));

    }

    /**
     * Gets the event types which should trigger synchronization of properties
     * from the client side to the server.
     *
     * @return the event types which should trigger synchronization
     */
    public Set<String> getSynchronizedPropertiesEvents() {
        if (!contains(KEY_EVENTS)) {
            return Collections.emptySet();
        }
        return JsonUtil.stream((JsonArray) get(KEY_EVENTS))
                .map(JsonValue::asString).collect(Collectors.toSet());
    }

}
