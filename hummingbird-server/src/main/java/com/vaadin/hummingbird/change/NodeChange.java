package com.vaadin.hummingbird.change;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Base class describing a change to a state node.
 *
 * @since
 * @author Vaadin Ltd
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
     * @return a json representation of this change
     */
    public JsonObject toJson() {
        JsonObject json = Json.createObject();

        json.put("node", node.getId());

        populateJson(json);

        return json;
    }

    /**
     * Overridden by subclasses to populate a JSON object when serializing.
     *
     * @param json
     *            the json object to populate
     */
    protected abstract void populateJson(JsonObject json);
}
