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
package com.vaadin.hummingbird.nodefeature;

import com.vaadin.hummingbird.StateNode;

/**
 * Methods which are published as event callbacks on the client side.
 *
 * @author Vaadin Ltd
 *
 */
public class ElementServerEventHandlers extends SerializableNodeList<String> {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public ElementServerEventHandlers(StateNode node) {
        super(node);
    }

    /**
     * Add a listener event name to be bound on the client-side.
     * 
     * @param eventName
     *            Name of the Event
     */
    public void addServerEvent(String eventName) {
        add(eventName);
    }
}
