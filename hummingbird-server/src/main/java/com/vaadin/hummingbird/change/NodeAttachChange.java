package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.StateNode;

import elemental.json.JsonObject;

/**
 * Change describing that a node has been attached.
 *
 * @since
 * @author Vaadin Ltd
 */
public class NodeAttachChange extends NodeChange {

    /**
     * Creates a new attach change.
     *
     * @param node
     *            the attached node
     */
    public NodeAttachChange(StateNode node) {
        super(node);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "attach");
    }
}
