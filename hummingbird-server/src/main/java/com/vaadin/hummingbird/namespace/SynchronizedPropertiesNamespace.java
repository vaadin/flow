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

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.util.JsonUtil;

import elemental.json.Json;

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

    public void setSynchronizedProperties(String[] propertyNames) {
        putJson(KEY_PROPERTIES, Arrays.asList(propertyNames).stream()
                .map(Json::create).collect(JsonUtil.asArray()));
    }

    public void setSynchronizedPropertiesEvents(String[] eventTypes) {
        putJson(KEY_EVENTS, Arrays.asList(eventTypes).stream().map(Json::create)
                .collect(JsonUtil.asArray()));

    }

}
