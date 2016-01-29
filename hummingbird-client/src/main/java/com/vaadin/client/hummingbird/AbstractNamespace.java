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
package com.vaadin.client.hummingbird;

import com.vaadin.client.WidgetUtil;

import elemental.json.JsonValue;

/**
 * Holder of the actual data in a state node. The state node data is isolated
 * into different namespaces of related data.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class AbstractNamespace {

    private final int id;
    private final StateNode node;

    /**
     * Creates a new namespace
     *
     * @param id
     *            the id of the namespace
     * @param node
     *            the node that the namespace belongs to
     */
    public AbstractNamespace(int id, StateNode node) {
        this.id = id;
        this.node = node;
    }

    /**
     * Gets the id of this namespace.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the node of this namespace.
     *
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Gets a JSON object representing the contents of this namespace. Only
     * intended for debugging purposes.
     *
     * @return a JSON representation
     */
    public abstract JsonValue getDebugJson();

    /**
     * Helper for getting a JSON representation of a child value.
     *
     * @param value
     *            the child value
     * @return the JSON representation
     */
    protected JsonValue getAsDebugJson(Object value) {
        if (value instanceof StateNode) {
            StateNode child = (StateNode) value;
            return child.getDebugJson();
        } else {
            return WidgetUtil.crazyJsoCast(value);
        }
    }
}
