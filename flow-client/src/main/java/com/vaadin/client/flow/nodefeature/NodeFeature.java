/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
