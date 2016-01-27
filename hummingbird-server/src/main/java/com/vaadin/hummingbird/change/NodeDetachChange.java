package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.StateNode;

import elemental.json.JsonObject;

/**
 * Change describing that a node has been detached.
 *
 * @since
 * @author Vaadin Ltd
 */
public class NodeDetachChange extends NodeChange {
    /**
     * Creates a new detach change.
     *
     * @param node
     *            the detached node
     */
    public NodeDetachChange(StateNode node) {
        super(node);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "detach");
    }
}
