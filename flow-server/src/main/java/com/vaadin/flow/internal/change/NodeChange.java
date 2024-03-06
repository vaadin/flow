/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.change;

import java.io.Serializable;

import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Base class describing a change to a state node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class NodeChange implements Serializable {
    private final StateNode node;

    /**
     * Creates a new change for the given node.
     *
     * @param node
     *            the changed node
     */
    public NodeChange(StateNode node) {
        this.node = node;
    }

    /**
     * Gets the changed node.
     *
     * @return the node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Serializes this change to JSON.
     *
     * @param constantPool
     *            the constant pool to use for serializing constant pool
     *            references
     *
     * @return a json representation of this change
     */
    public JsonObject toJson(ConstantPool constantPool) {
        JsonObject json = Json.createObject();

        json.put(JsonConstants.CHANGE_NODE, node.getId());

        populateJson(json, constantPool);

        return json;
    }

    /**
     * Overridden by subclasses to populate a JSON object when serializing.
     *
     * @param json
     *            the json object to populate
     * @param constantPool
     *            the constant pool to use for serializing constant pool
     *            references
     */
    protected abstract void populateJson(JsonObject json,
            ConstantPool constantPool);
}
