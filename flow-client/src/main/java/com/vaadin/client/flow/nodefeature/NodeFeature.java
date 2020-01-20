/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.client.flow.nodefeature;

import java.util.function.Function;

import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.StateNode;

import elemental.json.JsonValue;

/**
 * Holder of the actual data in a state node. The state node data is isolated
 * into different features of related data.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class NodeFeature {

    private final int id;
    private final StateNode node;

    /**
     * Creates a new feature.
     *
     * @param id
     *            the id of the feature
     * @param node
     *            the node that the feature belongs to
     */
    public NodeFeature(int id, StateNode node) {
        this.id = id;
        this.node = node;
    }

    /**
     * Gets the id of this feature.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the node of this feature.
     *
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Gets a JSON object representing the contents of this feature. Only
     * intended for debugging purposes.
     *
     * @return a JSON representation
     */
    public abstract JsonValue getDebugJson();

    /**
     * Convert the feature values into a {@link JsonValue} using provided
     * {@code converter} for the values stored in the feature (i.e. primitive
     * types, StateNodes).
     * 
     * @param converter
     *            converter to convert values stored in the feature
     * @return resulting converted value
     */
    public abstract JsonValue convert(Function<Object, JsonValue> converter);

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
